package com.tersesystems.blacklite;

import com.tersesystems.blacklite.archive.ArchiveResult;
import com.tersesystems.blacklite.archive.Archiver;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An abstract writer.
 *
 * This writer does not depend on a particular logging framework and so can be used
 * as a stable base for logging functionality.
 */
public abstract class AbstractEntryWriter implements EntryWriter {
  protected final ArchiveTask archiveTask;
  protected final StatusReporter statusReporter;
  protected final long batchInsertSize;
  protected final AtomicBoolean enabled = new AtomicBoolean(true);

  protected EntryStore entryStore;

  public AbstractEntryWriter(
      StatusReporter statusReporter, EntryStoreConfig config, Archiver archiver, String name)
      throws SQLException {
    Objects.requireNonNull(statusReporter, "Null statusReporter");
    Objects.requireNonNull(config, "Null config");
    Objects.requireNonNull(name, "Null name");
    Objects.requireNonNull(archiver, "Null archiver");

    this.statusReporter = statusReporter;
    this.batchInsertSize = config.getBatchInsertSize();

    this.entryStore = createEntryStore(config);
    archiver.setEntryStore(entryStore);

    entryStore.initialize();
    archiver.initialize(statusReporter);

    this.archiveTask = new ArchiveTask(archiver);
  }

  protected EntryStore createEntryStore(EntryStoreConfig config) throws SQLException {
    return new DefaultEntryStore(config);
  }

  protected boolean acceptingWrites() {
    return enabled.get();
  }

  protected static class ArchiveTask {
    private final Archiver archiver;

    public ArchiveTask(Archiver archiver) {
      this.archiver = archiver;
    }

    public ArchiveResult run(Connection conn) {
      return archiver.archive(conn);
    }

    public void close() throws Exception {
      archiver.close();
    }
  }
}
