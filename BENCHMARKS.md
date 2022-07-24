# Benchmarks

Benchmarks are notoriously unreliable, but this should give a general idea of effectiveness.

Benchmarks are provided with some caveats. There are a number of [presentations](https://www.cs.utexas.edu/~jaya/slides/apsys17-sqlite-slides.pdf) on the complexities of benchmarking SQLite.  Benchmarking can vary considerably depending on your hardware, and in particular IO writes may be different given a 250 MB/sec SSD available to a cloud instance.  If you're writing more log entries than your IO throughput then memory mapping can offload some of it, but at some point you will saturate something and God's Own Backpressure will be applied.

## Setup

All benchmarks were run on a Dell XPS 15 running Linux.

```
OpenJDK 64-Bit Server VM Corretto-11.0.9.11.1 (build 11.0.9+11-LTS, mixed mode)
```

The JDK is Corretto 17:

```
sdk install java 17.0.3.6.1-amzn
```

The JMH options are:

```
args = ['-prof', 'gc', '-rf', 'json']
```

## Throughput

The default entry store benchmark shows a rough indication of throughput using the SQLite appender, using an temporary directory on `tmpfs` (so no `fsync` or IO).

The `insertAndBatchCommit` benchmark creates batches of 1000 and commits the batch.  The `insertAndCommit` benchmark inserts and commits one at a time.

## Latency

The `AsyncEntryWriterBenchmark` shows an indication of the time needed to insert an element into the queue.  We care about the latency here as we want a logging operation to move off the executing thread as soon as possible.

## Results

```bash
./gradlew clean blacklite-benchmarks:jmh
LOGDATE=$(date +%Y%m%dT%H%M%S)
mv log4j/jmh-result.json results/17.0.3.6.1-amzn/$LOGDATE.json
```

* [20220723T225439.json](https://jmh.morethan.io/?source=https://raw.githubusercontent.com/tersesystems/blacklite/main/blacklite-benchmarks/results/17.0.3.6.1-amzn/20220723T225439.json)
