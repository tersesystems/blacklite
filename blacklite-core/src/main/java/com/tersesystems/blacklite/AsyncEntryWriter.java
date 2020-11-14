package com.tersesystems.blacklite;

import com.tersesystems.blacklite.archive.Archiver;
import org.jctools.queues.MessagePassingQueue;
import org.jctools.queues.MpscUnboundedXaddArrayQueue;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

public class AsyncEntryWriter extends AbstractEntryWriter {

  private final MessagePassingQueue<Entry> queue;

  public AsyncEntryWriter(
      StatusReporter statusReporter, EntryStoreConfig config, Archiver archiver, String name)
      throws SQLException {
    super(statusReporter, config, archiver, name);

    // https://vmlens.com/articles/scale/scalability_queue/
    // https://twitter.com/forked_franz/status/1228773808317792256?lang=en
    // XXX should talk to @forked_franz about ideal chunk size?
    queue = new MpscUnboundedXaddArrayQueue<>(4096);

    executor.execute(
        () -> {
          final AtomicLong inserts = new AtomicLong(0);

          MessagePassingQueue.WaitStrategy waitStrategy =
              idleCounter -> {
                try {
                  // Nothing happening right now, try a batch
                  if (inserts.get() > 0) {
                    entryStore.executeBatch();
                    inserts.set(0);
                  }

                  if (idleCounter > 10) {
                    archiveTask.run();
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
      statusReporter.addError("Could not accept entry!");
    }
  }
}
