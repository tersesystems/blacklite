package com.tersesystems.blacklite;

import com.tersesystems.blacklite.archive.Archiver;
import com.tersesystems.blacklite.archive.NoOpArchiver;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(3)
@State(Scope.Benchmark)
public class AsyncEntryWriterBenchmark {

  Instant now = Instant.now();
  byte[] content = "Hello World!".getBytes();
  int level = 5000;
  private AsyncEntryWriter writer;

  static class FakeEntryStore implements EntryStore {
    @Override
    public void insert(long epochSecond, int nanos, int level, byte[] content)
        throws SQLException {}

    @Override
    public Connection getConnection() {
      return null;
    }

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
    public String getUrl() {
      return null;
    }

    @Override
    public void close() throws Exception {}
  }

  @Setup
  public void setUp() throws Exception {
    EntryStoreConfig config = new DefaultEntryStoreConfig();
    final FakeEntryStore fakeEntryStore = new FakeEntryStore();
    Archiver archiver = new NoOpArchiver();
    this.writer =
        new AsyncEntryWriter(StatusReporter.DEFAULT, config, archiver, "blacklite-appender") {
          @Override
          protected EntryStore createEntryStore(EntryStoreConfig config) throws SQLException {
            return fakeEntryStore;
          }
        };
  }

  @TearDown
  public void tearDown() throws Exception {
    writer.close();
  }

  // Dell XPS Laptop running Elementary 5.1.7
  // Built on Ubuntu 18.04.4 LTS
  // Intel® Core™ i7-9750H CPU @ 2.60GHz × 6

  // QueueBenchmark.benchmark             avgt   10    11.781 ±  2.563  ns/op

  @Benchmark
  public void benchmark() throws SQLException {
    writer.write(now.getEpochSecond(), now.getNano(), level, content);
  }
}
