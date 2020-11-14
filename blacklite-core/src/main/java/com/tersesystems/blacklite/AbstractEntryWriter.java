package com.tersesystems.blacklite;

import com.tersesystems.blacklite.archive.Archiver;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** The main rifter action. */
public abstract class AbstractEntryWriter implements EntryWriter {

  protected final ExecutorService executor;
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

    this.executor =
        Executors.newSingleThreadExecutor(
            r1 -> {
              Thread t1 = new Thread(r1);
              t1.setDaemon(true);
              t1.setName(name + "-executor-thread");
              return t1;
            });

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

  @Override
  public void close() throws Exception {
    // Reject any additional inserts.
    enabled.set(false);

    // Force a commit.
    CompletableFuture.runAsync(
            () -> {
              try {
                entryStore.executeBatch();
              } catch (SQLException e) {
                statusReporter.addError(e.getMessage(), e);
              }
            },
            executor)
        .get();

    // Run the archive task before shutdown.
    CompletableFuture.runAsync(archiveTask, executor).get();

    // Shutdown the executor
    executor.shutdown();
    boolean shutdownBeforeTimeout = executor.awaitTermination(100L, TimeUnit.SECONDS);
    if (!shutdownBeforeTimeout) {
      // THIS IS BAD but I'm not sure we can do anything about it...
      statusReporter.addWarn("Executor timed out before shutdown!");
    }

    // Finally, close everything out.
    entryStore.close();
    archiveTask.close();
  }

  protected static class ArchiveTask implements Runnable {
    private final Archiver archiver;

    public ArchiveTask(Archiver archiver) {
      this.archiver = archiver;
    }

    @Override
    public void run() {
      if (archiver.shouldArchive()) {
        archiver.archive();
      }
    }

    public void close() {
      archiver.close();
    }
  }
}
