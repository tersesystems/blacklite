package com.tersesystems.blacklite;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 10, time = 1)
@Measurement(iterations = 20, time = 1)
@Fork(3)
@State(Scope.Benchmark)
public class DefaultEntryStoreBenchmark {

  private DefaultEntryStore repository;

  Instant now = Instant.now();
  byte[] content = "Hello World!".getBytes();
  int level = 5000;
  Path tempDirectoryPath;
  Connection conn;

  private int inserts = 0;

  @Setup
  public void setUp() throws Exception {
    tempDirectoryPath = Files.createTempDirectory("blacklite");
    String file = tempDirectoryPath.resolve("test.db").toAbsolutePath().toString();
    EntryStoreConfig config = new DefaultEntryStoreConfig();
    config.setFile(file);
    repository = new DefaultEntryStore(config);
    repository.initialize();
  }

  @TearDown
  public void tearDown() throws Exception {
    repository.close();
    Files.deleteIfExists(tempDirectoryPath.resolve("test.db"));
  }

  @Benchmark
  public void insertAndBatchCommit() throws SQLException {
    repository.insert(now.getEpochSecond(), now.getNano(), level, content);
    inserts += 1;
    if (inserts == 1000) {
      repository.executeBatch();
      repository.commit();
      inserts = 0;
    }
  }

  @Benchmark
  public void insertAndCommit() throws SQLException {
    repository.insert(now.getEpochSecond(), now.getNano(), level, content);
    repository.executeBatch();
    repository.commit();
  }
}
