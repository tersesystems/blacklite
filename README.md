# Blacklite

Blacklite is an appender that writes to a [SQLite](https://www.sqlite.org/index.html) database
.  It combines all the advantages of writing to a database with the speed and control of passing
 around a flat file.

* Uses SQLite [memory mapping](https://www.sqlite.org/mmap.html) and batch inserts for maximum
 throughput with minimum latency.
* Broad support given [Library of Congress approved storage format](https://sqlite.org/locrsf.html
).
* Built-in archiving and rollover based on number of rows.
* Automatic ZStandard [dictionary compression](http://fileformats.archiveteam.org/wiki/Zstandard_dictionary) for 4x disk space savings in archives.
* Database reader to search logs from command line by "natural language" date ranges and 
[JSON](https://www.sqlite.org/json1.html).

Blacklite is designed for ubiquitous logging and "logging-aware" applications: 

* You want to search through logs in a given date ranges and level.
* You want to analyze number and types of messages produced by the logging framework.
* You want an always-available buffer debugging/tracing that can be queried and "dumped" on application error.

In addition, providing data in SQLite format means you can leverage tools built using SQLite:

* [sqlite-utils](https://sqlite-utils.readthedocs.io/en/stable/): Read and process SQLite files from command line
* [Datasette](https://docs.datasette.io/en/stable/): Exposing SQLite files as web applications
* [Observable HQ](https://observablehq.com/@mbostock/sqlite): Using SQLite data in visualization notebooks

For more details on how to query in SQLite, please see the [SQLite page](SQLITE.md).

## Installation

XXX TODO

## Configuration

XXX TODO

## Benchmarks

Using my local laptop;

Dell XPS Laptop running Elementary 5.1.7 / Built on Ubuntu 18.04.4 LTS / Intel® Core™ i7-9750H CPU
 @ 2.60GHz × 6 / NVMe M2 SSD

``` 
Benchmark                                     Mode  Cnt       Score       Error  Units
DefaultEntryStoreBenchmark.benchmark         thrpt   20  803206.197 ± 54816.464  ops/s
AsyncEntryWriterBenchmark.benchmark           avgt   10      11.920 ±     0.099  ns/op
BlockingEntryWriterBenchmark.benchmark        avgt   10       1.057 ±     0.010  ns/op
codec.zstd.ZstdCodecBenchmark.benchmark       avgt   20    4431.193 ±    17.608  ns/op
codec.zstd.ZstdDictCodecBenchmark.testWrite   avgt   20    3079.556 ±   226.191  ns/op
```

Note that benchmarking can vary considerably depending on your hardware, and in particular IO
 writes may be different given a 250 MB/sec SSD available to a cloud instance.

## Archiving

Blacklite comes with an archiver which ensures that older data is removed and archive databases are a reasonable size.

### Triggering Policy

There is a triggering policy when a given number of rows is exceeded.
 
### Rolling Strategy

Archiving uses a fixed window rolling strategy, which renames older archive databases and will delete the oldest archives.

### Compression

The archiver can use a codec, which handles compression and decompression of archived content.  This can be used 

Blacklite provides a codec for [zstandard](https://facebook.github.io/zstd/), using the [zstd-jni](https://github.com/luben/zstd-jni) library. which is extremely fast and can be tweaked to be
 competitive with LZ4 using "negative" compression levels like "-4".  

In addition, the archiver also includes a [dictionary compression](https://facebook.github.io/zstd/#small-data) option.  If a dictionary is found, then the archiver will write the compressed
 content to the archive file.  If no dictionary is found, the archiver will feed a dictionary using the incoming log entries, then switch over to dictionary compression once the dictionary has been
  trained.
  
Using a dictionary provides both speed and size improvements.  An entry that is typically 185 bytes with JSON can shrink down to as few as 32 bytes.  This adds up extremely quickly when you start
 working with larger log files.
 
This is all very abstract, so here's a real life example using 2,001,000 log entries with the logstash logback encoder writing out JSON.

For the unencoded content:

```
❱ ls -lh rifter.json
-rw-rw-r-- 1 wsargent wsargent 431M Oct 18 14:14 rifter.json
```

Compare with the encoded SQLite database using dictionary compression:

```
❱ ls -lh archive.db
-rw-rw-r-- 1 wsargent wsargent 177M Oct 18 14:14 archive.db
```

But still have the same number of records:

```
❱ sqlite3 archive.db  "select count(*) from entries"
2001000
❱ wc rifter.json
  2001000   6002000 451212069 rifter.json
```
