package com.tersesystems.blacklite;

import com.tersesystems.blacklite.archive.Archiver;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** A simple rifter that writes directly to the live repository. */
public class BlockingEntryWriter extends AbstractEntryWriter {

  protected final ScheduledExecutorService scheduler;

  public BlockingEntryWriter(
      StatusReporter statusReporter, EntryStoreConfig config, Archiver archiver, String name)
      throws SQLException {
    super(statusReporter, config, archiver, name);

    this.scheduler =
        Executors.newSingleThreadScheduledExecutor(
            r1 -> {
              Thread t1 = new Thread(r1);
              t1.setDaemon(true);
              t1.setName(name + "-scheduler-thread");
              return t1;
            });

    scheduler.scheduleAtFixedRate(
        () -> CompletableFuture.runAsync(archiveTask, executor),
        1, // wait one second.
        1, // run every second
        TimeUnit.SECONDS);
  }

  @Override
  public void write(long epochSeconds, int nanos, int level, byte[] content) {
    try {
      entryStore.insert(epochSeconds, nanos, level, content);
      entryStore.executeBatch();
    } catch (SQLException e) {
      statusReporter.addError("write", e);
    }
  }

  @Override
  public void close() throws Exception {
    super.close();

    // Shut down the scheduler.
    scheduler.shutdown();
    scheduler.awaitTermination(1L, TimeUnit.SECONDS);
  }
}
