package com.tersesystems.blacklite.codec.zstd;

import com.tersesystems.blacklite.StatusReporter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 10, time = 1)
@Measurement(iterations = 20, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class ZstdCodecBenchmark {

  byte[] content;
  private ZStdCodec codec;

  @Setup
  public void setUp() throws SQLException, IOException {
    File file =
        new File(
            "/home/wsargent/work/blacklite/blacklite-benchmarks/src/jmh/resources/message.json");
    this.content = Files.readAllBytes(file.toPath());
    this.codec = new ZStdCodec();
    this.codec.setLevel(3);
    this.codec.initialize(StatusReporter.DEFAULT);
  }

  // Dell XPS Laptop running Elementary 5.1.7
  // Built on Ubuntu 18.04.4 LTS
  // Intel® Core™ i7-9750H CPU @ 2.60GHz × 6

  // codec.compress.ZstdCodecBenchmark.benchmark  avgt   20  41088.071 ± 4270.751  ns/op

  @Benchmark
  public void benchmark() {
    codec.encode(content);
  }
}
