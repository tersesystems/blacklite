package com.tersesystems.blacklite.codec.zstd;

import com.tersesystems.blacklite.StatusReporter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 10, time = 1)
@Measurement(iterations = 20, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class ZstdDictCodecBenchmark {

  byte[] content;
  private ZStdDictCodec codec;

  @Setup
  public void setUp() throws IOException {
    ZstdDictFileRepository repo = new ZstdDictFileRepository();
    repo.setFile("/home/wsargent/work/blacklite/blacklite-benchmarks/src/jmh/resources/dictionary");

    this.codec = new ZStdDictCodec();
    this.codec.setRepository(repo);
    this.codec.initialize(StatusReporter.DEFAULT);

    String path =
        "/home/wsargent/work/blacklite/blacklite-benchmarks/src/jmh/resources/message.json";
    File file = new File(path);
    this.content = Files.readAllBytes(file.toPath());
  }

  // Dell XPS Laptop running Elementary 5.1.7
  // Built on Ubuntu 18.04.4 LTS
  // Intel® Core™ i7-9750H CPU @ 2.60GHz × 6

  // use htop, make sure you don't have any hung elementary processes (that perl one)
  // and you don't have discord/1password/slack running.
  // Make sure laptop is cool so it won't suffer thermal throttling, and is plugged in.

  @Benchmark
  public void testWrite() {
    codec.encode(content);
  }
}
