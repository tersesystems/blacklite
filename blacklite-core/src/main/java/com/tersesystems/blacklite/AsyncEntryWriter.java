package com.tersesystems.blacklite;

import com.tersesystems.blacklite.archive.ArchiveResult;
import com.tersesystems.blacklite.archive.Archiver;
import org.jctools.queues.MessagePassingQueue;
import org.jctools.queues.MpscGrowableArrayQueue;

import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * An asynchronous entry writer.
 * <p>
 * This entry writer uses an unbounded queue and will
 * create an internal thread "$NAME-executor-thread" to drain entry objects off the queue.
 * <p>
 * The entry writer will insert items to the entry store as it receives them, calling executeBatch()
 * when inserts reach the batch entry size.
 * <p>
 * When idle, the queue will call flush() on the entry store, and call the archive task every second.
 * <p>
 * The queue is unbounded because when an archiver is active, the backlog can
 * get very large, but will drain extremely quickly once archiver has completed.
 * <p>
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

    this.queue = new MpscGrowableArrayQueue<>(config.getMaxCapacity());

    this.executor =
      Executors.newSingleThreadExecutor(
        r1 -> {
          Thread t1 = new Thread(r1);
          t1.setDaemon(true);
          t1.setName(name + "-executor-thread");
          return t1;
        });

    executor.execute(new Consumer());
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
    executor.execute(new ClosingConsumer());

    // Give the executor a second to commit and close out cleanly...
    if (!executor.awaitTermination(1000L, TimeUnit.SECONDS)) {
      statusReporter.addError("Timeout exceeded when closing executor!");
    }
  }

  private final class Consumer implements Runnable {
    private long inserts = 0;
    private long lastRun = System.currentTimeMillis();

    @Override
    public void run() {
      // called when there are no elements in the queue.
      MessagePassingQueue.WaitStrategy onIdle =
        idleCounter -> {
          // flush any outstanding inserts if there's nothing in the queue
          // This means that batchInsertSize is more of a highwater mark:
          // "you MUST commit now after this number of inserts" etc
          try {
            commit();
          } catch (SQLException e) {
            statusReporter.addError(e.getMessage(), e);
          }
          archive();

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
            inserts = inserts + 1;
            // Always flush on batch insert size, even if we've never been idle.
            if (inserts >= batchInsertSize) {
              commit();

            }
            archive();
          } catch (SQLException ex) {
            statusReporter.addError(ex.getMessage(), ex);
          }
        };
      queue.drain(consumer, onIdle, () -> (acceptingWrites() || !queue.isEmpty()));
      if (tracing) {
        statusReporter.addInfo("AsyncEntryWriter: queue no longer accepting writes");
      }
    }

    private void commit() throws SQLException {
      final long i = inserts;
      if (i > 0) {
        if (tracing) {
          final int size = queue.size();
          statusReporter.addInfo("AsyncEntryWriter: queue size = " + size + ", committing " + i);
        }
        entryStore.executeBatch();
        entryStore.commit();
        inserts = 0;
      }
    }

    private void archive() {
      if (lastRun < System.currentTimeMillis() - 1000) {
        if (!archiving) {
          if (tracing) {
            statusReporter.addInfo("AsyncEntryWriter: archive lastRun " + lastRun);
          }
          ArchiveResult result = archiveTask.run(entryStore.getConnection());
          if (result instanceof ArchiveResult.Failure) {
            final Exception e = ((ArchiveResult.Failure) result).getException();
            statusReporter.addError("AsyncEntryWriter: Archive task returned failure: ", e);
          }
          archiving = false;
          lastRun = System.currentTimeMillis();
        }
      }
    }
  }

  private final class ClosingConsumer implements Runnable {

    @Override
    public void run() {
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
    }
  }
}
