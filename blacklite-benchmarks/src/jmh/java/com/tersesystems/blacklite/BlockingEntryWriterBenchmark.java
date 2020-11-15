package com.tersesystems.blacklite;

import com.tersesystems.blacklite.archive.Archiver;
import com.tersesystems.blacklite.archive.DefaultArchiver;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class BlockingEntryWriterBenchmark {

  Instant now = Instant.now();
  byte[] content = "Hello World!".getBytes();
  int level = 5000;
  private BlockingEntryWriter blacklite;

  static class FakeEntryStore implements EntryStore {
    @Override
    public void insert(long epochSecond, int nanos, int level, byte[] content)
        throws SQLException {}

    @Override
    public void vacuum() throws SQLException {}

    @Override
    public void executeBatch() throws SQLException {
      return;
    }

    @Override
    public void commit() throws SQLException {}

    @Override
    public void initialize() throws SQLException {}

    @Override
    public Connection getConnection() {
      return null;
    }

    @Override
    public void close() throws Exception {}
  }

  @Setup
  public void setUp() throws Exception {
    EntryStoreConfig config = new DefaultEntryStoreConfig();
    final FakeEntryStore fakeEntryStore = new FakeEntryStore();
    Archiver archiver =
        new DefaultArchiver() {
          public boolean shouldArchive() {
            return false;
          }

          public int archive() {
            return 0;
          }
        };
    this.blacklite =
        new BlockingEntryWriter(StatusReporter.DEFAULT, config, archiver, "rifter-appender") {
          @Override
          protected EntryStore createEntryStore(EntryStoreConfig config) throws SQLException {
            return fakeEntryStore;
          }
        };
  }

  @TearDown
  public void tearDown() throws Exception {
    blacklite.close();
  }

  @Benchmark
  public void benchmark() throws SQLException {
    blacklite.write(now.getEpochSecond(), now.getNano(), level, content);
  }
}
