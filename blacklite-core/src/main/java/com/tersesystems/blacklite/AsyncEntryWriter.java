package com.tersesystems.blacklite;

import com.tersesystems.blacklite.archive.ArchiveResult;
import com.tersesystems.blacklite.archive.Archiver;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import org.jctools.queues.MessagePassingQueue;
import org.jctools.queues.MpscUnboundedXaddArrayQueue;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

public class AsyncEntryWriter extends AbstractEntryWriter {

  protected final ExecutorService executor;

  private final MessagePassingQueue<Entry> queue;

  private final boolean tracing = false;

  public AsyncEntryWriter(
      StatusReporter statusReporter, EntryStoreConfig config, Archiver archiver, String name)
      throws SQLException {
    super(statusReporter, config, archiver, name);

    // https://vmlens.com/articles/scale/scalability_queue/
    // https://twitter.com/forked_franz/status/1228773808317792256?lang=en
    // XXX should talk to @forked_franz about ideal chunk size?
    // with archiving especially, this can get very large.
    queue = new MpscUnboundedXaddArrayQueue<>(4096);
    // queue = new MpscArrayQueue<>(65536);

    this.executor =
        Executors.newSingleThreadExecutor(
            r1 -> {
              Thread t1 = new Thread(r1);
              t1.setDaemon(true);
              t1.setName(name + "-executor-thread");
              return t1;
            });

    executor.execute(
        () -> {
          final AtomicLong inserts = new AtomicLong(0);
          final AtomicLong lastRun = new AtomicLong(System.nanoTime());
          MessagePassingQueue.WaitStrategy waitStrategy =
              idleCounter -> {
                try {
                  // Nothing happening right now, try a batch commit
                  if (inserts.get() > 0) {
                    if (tracing) {
                      statusReporter.addInfo(
                        "AsyncEntryWriter: idle counter "
                          + idleCounter
                          + ", committing "
                          + inserts.get());
                    }
                    entryStore.executeBatch();
                    entryStore.commit();
                    inserts.set(0);
                  }

                  // If it's been idle for a while, run the archive task.
                  // XXX this should also run at set intervals if a deadline is exceeded.
                  if (lastRun.get() < System.nanoTime() - 1_000_000_000) {
                    ArchiveResult result = archiveTask.run(entryStore.getConnection());
                    if (result instanceof ArchiveResult.Failure) {
                      final Exception e = ((ArchiveResult.Failure) result).getException();
                      statusReporter.addError("AsyncEntryWriter: Archive task returned failure: ", e);
                    }
                    lastRun.set(System.nanoTime());
                    return 0;
                  }

                  // parkNanos is going to park for a microsecond at minimum, maybe even more.
                  // http://www.rationaljava.com/2015/10/measuring-microsecond-in-java.html
                  // https://hazelcast.com/blog/locksupport-parknanos-under-the-hood-and-the-curious-case-of-parking/
                  LockSupport.parkNanos(1000);
                } catch (SQLException e) {
                  statusReporter.addError(e.getMessage(), e);
                }
                return idleCounter + 1;
              };

          MessagePassingQueue.Consumer<Entry> consumer =
              e -> {
                try {
                  entryStore.insert(e.epochSecond, e.nanos, e.level, e.content);

                  // Always check to see if we should be committing here...
                  if (inserts.incrementAndGet() > batchInsertSize) {
                    entryStore.executeBatch();
                    inserts.set(0);
                  }
                } catch (SQLException ex) {
                  statusReporter.addError(ex.getMessage(), ex);
                }
              };
          queue.drain(consumer, waitStrategy, () -> (acceptingWrites() || !queue.isEmpty()));
        });
  }

  @Override
  public void write(long epochSeconds, int nanos, int level, byte[] content) {
    if (!queue.relaxedOffer(new Entry(-1, epochSeconds, nanos, level, content))) {
      statusReporter.addError("AsyncEntryWriter: Could not accept entry!");
    }
  }

  @Override
  public void close() throws Exception {
    // Reject any additional inserts.
    enabled.set(false);

    statusReporter.addInfo("AsyncEntryWriter: close");

    executor.submit(
        () -> {
          try {
            // Clean out the queue.
            entryStore.executeBatch();
            entryStore.commit();

            // run the archive before we close the entry store (as that will close out the connection)
            archiveTask.run(entryStore.getConnection());
            archiveTask.close();
            statusReporter.addInfo("AsyncEntryWriter: Archive task closed");

            // CLose out the entry store.
            entryStore.close();
            statusReporter.addInfo("AsyncEntryWriter: Entry store closed");
          } catch (Exception e) {
            statusReporter.addError("AsyncEntryWriter: Shutdown", e);
          } finally {
            executor.shutdown();
          }
        });

    // Give the executor a second to commit and close out cleanly...
    if (!executor.awaitTermination(1L, TimeUnit.SECONDS)) {
      statusReporter.addError("Timeout exceeded when closing executor!");
    }
  }
}
