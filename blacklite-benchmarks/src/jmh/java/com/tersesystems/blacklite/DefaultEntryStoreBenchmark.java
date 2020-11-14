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
@Fork(1)
@State(Scope.Benchmark)
public class DefaultEntryStoreBenchmark {

  private DefaultEntryStore repository;

  Instant now = Instant.now();
  byte[] content = "Hello World!".getBytes();
  int level = 5000;
  Path tempDirectoryPath;
  Connection conn;

  @Setup
  public void setUp() throws Exception {
    // conn = Sqlite.connect("jdbc:sqlite::memory:");
    tempDirectoryPath = Files.createTempDirectory("blacklite");
    String url = "jdbc:sqlite:" + tempDirectoryPath.resolve("test.db").toAbsolutePath().toString();
    EntryStoreConfig config = new DefaultEntryStoreConfig();
    config.setUrl(url);
    repository = new DefaultEntryStore(config);
    repository.initialize();
  }

  @TearDown
  public void tearDown() throws Exception {
    repository.close();
    Files.deleteIfExists(tempDirectoryPath.resolve("test.db"));
  }

  @Benchmark
  public void benchmark() throws SQLException {
    repository.insert(now.getEpochSecond(), now.getNano(), level, content);
    repository.commit();
  }
}
