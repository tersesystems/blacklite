package com.tersesystems.blacklite;

import com.tersesystems.blacklite.archive.ArchiveResult;
import com.tersesystems.blacklite.archive.Archiver;
import org.jctools.queues.MessagePassingQueue;
import org.jctools.queues.MpscUnboundedXaddArrayQueue;

import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * An asynchronous entry writer.
 *
 * This entry writer uses an unbounded queue and will
 * create an internal thread "$NAME-executor-thread" to drain entry objects off the queue.
 *
 * The entry writer will insert items to the entry store as it receives them, calling executeBatch()
 * when inserts reach the batch entry size.
 *
 * When idle, the queue will call flush() on the entry store, and call the archive task every
 * 1_000_000_000 nanoseconds.
 *
 * The queue is unbounded because when an archiver is active, the backlog can
 * get very large, but will drain extremely quickly once archiver has completed.
 *
 * Jn SQLite you can only ever have one thread writing to the database at once,
 * and so we need to buffer _everything_ in the queue until the archive has
 * completed and then drain the queue in batched commits.
 */
public class AsyncEntryWriter extends AbstractEntryWriter {

  protected final ExecutorService executor;

  private final MessagePassingQueue<Entry> queue;

  private final boolean tracing;

  private boolean archiving = false;

  public AsyncEntryWriter(
      StatusReporter statusReporter, EntryStoreConfig config, Archiver archiver, String name)
      throws SQLException {
    super(statusReporter, config, archiver, name);

    this.tracing = config.getTracing();

    // https://vmlens.com/articles/scale/scalability_queue/
    // https://twitter.com/forked_franz/status/1228773808317792256?lang=en
    // XXX should talk to @forked_franz about ideal chunk size?
    this.queue = new MpscUnboundedXaddArrayQueue<>(4096);
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
          final AtomicLong lastRun = new AtomicLong(System.currentTimeMillis());

          // called when there are no elements in the queue.
          MessagePassingQueue.WaitStrategy onIdle =
              idleCounter -> {
                // flush any outstanding inserts if there's nothing in the queue
                // This means that batchInsertSize is more of a highwater mark:
                // "you MUST commit now after this number of inserts" etc
                try {
                  commit(inserts);
                } catch (SQLException e) {
                  statusReporter.addError(e.getMessage(), e);
                }
                archive(lastRun);

                // We only want to run the idle loop so often, so we'll sleep it.
                // parkNanos is going to park for a microsecond at minimum, maybe even more.
                // http://www.rationaljava.com/2015/10/measuring-microsecond-in-java.html
                // https://hazelcast.com/blog/locksupport-parknanos-under-the-hood-and-the-curious-case-of-parking/
                LockSupport.parkNanos(1000);
                return idleCounter + 1;
              };

          MessagePassingQueue.Consumer<Entry> consumer =
              e -> {
                try {
                  entryStore.insert(e.epochSecond, e.nanos, e.level, e.content);

                  // Always flush on batch insert size, even if we've never been idle.
                  if (inserts.incrementAndGet() >= batchInsertSize) {
                    commit(inserts);
                  }
                  archive(lastRun);
                } catch (SQLException ex) {
                  statusReporter.addError(ex.getMessage(), ex);
                }
              };
          queue.drain(consumer, onIdle, () -> (acceptingWrites() || !queue.isEmpty()));
          if (tracing) {
            statusReporter.addInfo("AsyncEntryWriter: queue no longer accepting writes");
          }
        });
  }

  private void commit(AtomicLong inserts) throws SQLException {
    final long i = inserts.get();
    if (i > 0) {
      if (tracing) {
        final int size = queue.size();
        statusReporter.addInfo("AsyncEntryWriter: queue size = " + size + ", committing " + i);
      }
      entryStore.executeBatch();
      entryStore.commit();
      inserts.set(0);
    }
  }

  private void archive(AtomicLong lastRun) {
    if (lastRun.get() < System.currentTimeMillis() - 1000) {
      if (! archiving) {
        if (tracing) {
          statusReporter.addInfo("AsyncEntryWriter: archive lastRun " + lastRun.get());
        }
        ArchiveResult result = archiveTask.run(entryStore.getConnection());
        if (result instanceof ArchiveResult.Failure) {
          final Exception e = ((ArchiveResult.Failure) result).getException();
          statusReporter.addError("AsyncEntryWriter: Archive task returned failure: ", e);
        }
        archiving = false;
        lastRun.set(System.currentTimeMillis());
      }
    }
  }

  @Override
  public void write(long epochSeconds, int nanos, int level, byte[] content) {
    if (!queue.relaxedOffer(new Entry(-1, epochSeconds, nanos, level, content))) {
      statusReporter.addError("AsyncEntryWriter: Could not accept entry!");
    }
  }

  @Override
  public void close() throws Exception {
    // Reject any additional inserts into the queue.
    // This will set acceptingWrites() to false and exit the main queue.drain.
    enabled.set(false);

    statusReporter.addInfo("AsyncEntryWriter: close");

    // this job will only execute after the queue.drain exits, leaving the queue empty.
    executor.submit(
        () -> {
          try {
            if (tracing) {
              statusReporter.addInfo("AsyncEntryWriter: close job executing");
            }
            // Check if there's anything still in there (shouldn't be, but
            // we'll check anyway)
            int entries = queue.drain(e -> {
              try {
                entryStore.insert(e.epochSecond, e.nanos, e.level, e.content);
              } catch (SQLException ex) {
                statusReporter.addError(ex.getMessage(), ex);
              }
            });
            if (tracing) {
              statusReporter.addInfo("AsyncEntryWriter: closed queue contained " + entries + " entries");
            }
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
    if (!executor.awaitTermination(1000L, TimeUnit.SECONDS)) {
      statusReporter.addError("Timeout exceeded when closing executor!");
    }
  }
}
