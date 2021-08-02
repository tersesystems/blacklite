package com.tersesystems.blacklite;

import com.tersesystems.blacklite.archive.Archiver;
import java.sql.SQLException;
import java.util.concurrent.atomic.LongAdder;

/**
 * A simple blocking writer that writes directly to the live repository.
 *
 * This is useful in situations like bulk loads, testing, and when it's
 * not acceptable to lose log entries in processing.
 */
public class BlockingEntryWriter extends AbstractEntryWriter {

  private final LongAdder adder = new LongAdder();

  public BlockingEntryWriter(
      StatusReporter statusReporter, EntryStoreConfig config, Archiver archiver, String name)
      throws SQLException {
    super(statusReporter, config, archiver, name);
  }

  @Override
  public void write(long epochSeconds, int nanos, int level, byte[] content) {
    try {
      adder.increment();
      entryStore.insert(epochSeconds, nanos, level, content);
      entryStore.executeBatch();

      // If you're unlucky enough to hit the insert, then your thread gets the
      // commit to the store.
      if (adder.longValue() % batchInsertSize == 0) {
        entryStore.commit();
      }
    } catch (SQLException e) {
      statusReporter.addError("write", e);
    }
  }

  @Override
  public void close() throws Exception {
    enabled.set(false);
  }
}
