# Blacklite

<!---freshmark shields
output = [
	link(shield('Maven artifact', 'mavenCentral', 'tersesystems:blacklite', 'blue'), 'https://search.maven.org/search?q=blacklite'),
	link(shield('Latest version', 'latest', '{{latestVersion}}', 'blue'), 'https://github.com/tersesystems/blacklite/releases/latest'),
	link(shield('License Apache-2.0', 'license', 'Apache-2.0', 'blue'), 'https://www.tldrlegal.com/l/apache2'),
	'',
	link(image('Travis CI', 'https://travis-ci.org/tersesystems/blacklite.svg?branch=master'), 'https://travis-ci.org/tersesystems/blacklite')
	].join('\n')
-->
[![Bintray](https://img.shields.io/badge/bintray-tersesystems%3Ablacklite-blue.svg)](https://bintray.com/tersesystems/maven/blacklite/view)
[![Latest version](https://img.shields.io/badge/latest-1.0.1-blue.svg)](https://github.com/tersesystems/blacklite/releases/latest)
[![License Apache-2.0](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](https://www.tldrlegal.com/l/apache2)

[![Travis CI](https://travis-ci.org/tersesystems/blacklite.svg?branch=master)](https://travis-ci.org/tersesystems/blacklite)
<!---freshmark /shields -->

Blacklite is an appender that writes to a [SQLite](https://www.sqlite.org/index.html) database.
It combines all the advantages of writing to a database with the speed and control of passing
around a flat file.  Blacklite supports both [Logback](http://logback.qos.ch/) and
[Log4J 2](https://logging.apache.org/log4j/2.x/).

* SQLite file means [total compatibility](https://sqlite.org/locrsf.html) and support over all platforms.
* Uses [memory mapping](https://www.sqlite.org/mmap.html), batch inserts for maximum
 throughput with minimum latency.  No indexes, no autoincrement fields.
* Built-in archiving and rollover based on number of rows.
* Automatic ZStandard dictionary training and compression for 4x disk space savings in archives.
* `blacklite-core` module allows direct entry writing with no logging framework needed.
* Database reader to search logs from command line by "natural language" date ranges.

Blacklite is designed for ubiquitous logging and "logging-aware" applications:

* You want to search through logs in a given date ranges and level.
* You want to analyze number and types of messages produced by the logging framework.
* You want an always-available buffer debugging/tracing that can be queried and "dumped" on application error.

Practically speaking, with some decent hardware you can budget around 800 debugging statements per 1 ms request.  Using [conditional logging](https://tersesystems.github.io/blindsight/usage/conditional.html), you can turn on debugging in production and get a complete picture of what a single request is doing.  See [terse-logback-showcase](https://github.com/tersesystems/terse-logback-showcase) for a live demonstration.

In addition, providing data in SQLite format means you can leverage tools built using SQLite:

* [sqlite-utils](https://sqlite-utils.readthedocs.io/en/stable/): Read and process SQLite files from command line
* [Datasette](https://docs.datasette.io/en/stable/): Exposing SQLite files as web applications
* [Observable HQ](https://observablehq.com/@mbostock/sqlite): Using SQLite data in visualization notebooks

More details on [querying SQLite using Python and sqlite3 here](SQLITE.md).

Blog post [here](https://tersesystems.com/blog/2020/11/26/queryable-logging-with-blacklite/).

## Installation

### Gradle

Add the following resolver:

```
repositories {
    mavenCentral()
}
```

And then add the libraries and codecs that you want.

For logback:

```
implementation 'com.tersesystems.blacklite:blacklite-logback:<latestVersion>'
implementation 'com.tersesystems.blacklite:blacklite-codec-zstd:<latestVersion>'
```

or for log4j:

```
implementation 'com.tersesystems.blacklite:blacklite-log4j2:<latestVersion>'
implementation 'com.tersesystems.blacklite:blacklite-log4j2-codec-zstd:<latestVersion>'
```

### Maven

For logback:

```xml
<dependency>
  <groupId>com.tersesystems.blacklite</groupId>
  <artifactId>blacklite-logback</artifactId>
  <version>$latestVersion</version>
</dependency>

<dependency>
  <groupId>com.tersesystems.blacklite</groupId>
  <artifactId>blacklite-codec-zstd</artifactId>
  <version>$latestVersion</version>
</dependency>
```

or log4j:

```xml
<dependency>
  <groupId>com.tersesystems.blacklite</groupId>
  <artifactId>blacklite-log4j</artifactId>
  <version>$latestVersion</version>
</dependency>

<dependency>
  <groupId>com.tersesystems.blacklite</groupId>
  <artifactId>blacklite-log4j2-codec-zstd</artifactId>
  <version>$latestVersion</version>
</dependency>
```

### SBT

SBT installation is fairly straightforward.

```sbt
libraryDependencies += "com.tersesystems.blacklite" % "blacklite-logback" % "<latestVersion>"
libraryDependencies += "com.tersesystems.blacklite" % "blacklite-codec-zstd" % "<latestVersion>"

// or log4j
//libraryDependencies += "com.tersesystems.blacklite" % "blacklite-log4j" % "<latestVersion>"
//libraryDependencies += "com.tersesystems.blacklite" % "blacklite-log4j2-codec-zstd" % "<latestVersion>"
```

## Configuration

### Logback

The logback appender uses [JCTools](https://jctools.github.io/JCTools/) internally as an asynchronous queue.  This means you don't need to use an `AsyncAppender` on top.

You should always use a `shutdownHook` to allow Logback to drain the queue before exiting.

The appender consists of a `file` property, and an `encoder` which encodes the bytes written to the `content` field in an entry.

If not defined, the default archiver is the `DeletingArchiver` set to `10000` rows.

```xml
<configuration>
    <shutdownHook class="ch.qos.logback.core.hook.DelayingShutdownHook">
        <delay>1000</delay>
    </shutdownHook>

    <appender name="BLACKLITE" class="com.tersesystems.blacklite.logback.BlackliteAppender">
        <file>logs/live.db</file>

        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        </encoder>
    </appender>

    <root level="TRACE">
        <appender-ref ref="BLACKLITE"/>
    </root>
</configuration>
```

#### Deleting Archiver

The deleting archiver will delete the oldest entries in the database when the highwater mark is reached.

Note that the database file size may be notably larger than the number of rows after deletion, because SQLite will reuse pages after deletion.  You can run `VACUUM` at regular intervals to recover space.

The maximum number of rows in the table is set using the `archiveAfterRows` property. There is no facility for unbounded growth, but you can set this number to `Long.MaxValue` which is 2<sup>63</sup>-1.

```xml
<archiver class="com.tersesystems.blacklite.archive.DeletingArchiver">
    <archiveAfterRows>10000</archiveAfterRows>
</archiver>
```

#### Rolling Archiver

The rolling archiver can be a bit complicated, but it works much the same way that rolling file appenders do.

The archiver has a `archiveAfterRows` property that is the maximum number of rows in the live database.  When there are more rows, then archiving takes place.

The rolling archiver will keep older log entries by moving them into other sqlite databases. When the maximum number of rows is reached, the oldest rows will be moved into the archive specified by the `file` property.  A codec compression can be
applied when rows are moved into the archive to save on disk space.

The archive file will be rolled over when the triggering policy is matched.  In the case of the `RowBasedTriggeringPolicy`,
this is the maximum number of rows in the archive database -- after that, the archive database will be renamed according to
the rolling strategy and another archive file will be created.

```xml
<archiver class="com.tersesystems.blacklite.archive.RollingArchiver">
    <file>/tmp/blacklite/archive.db</file>
    <archiveAfterRows>10000</archiveAfterRows>

    <codec class="com.tersesystems.blacklite.codec.zstd.ZStdCodec">
        <level>9</level>
    </codec>

    <triggeringPolicy class="com.tersesystems.blacklite.archive.RowBasedTriggeringPolicy">
        <maximumNumRows>500000</maximumNumRows>
    </triggeringPolicy>

    <rollingStrategy class="com.tersesystems.blacklite.logback.TimeBasedRollingStrategy">
        <fileNamePattern>logs/archive.%d{yyyyMMdd'T'hhmm,utc}.db</fileNamePattern>
        <maxHistory>20</maxHistory>
    </rollingStrategy>

</archiver>
```

##### Codec

The rolling archiver can take a codec that compresses the content of the bytes produced by the encoder.  This can be [very effective](COMPRESSION.md).

```xml
<codec class="com.tersesystems.blacklite.codec.zstd.ZStdCodec">
    <level>9</level>
</codec>
```

If using dictionary compression, it's `ZStdDictCodec` and the dictionary must be defined in a repository.

There are two repositories for dictionaries: `ZstdDictFileRepository` which points directly to a zstandard
 dictionary on the filesystem, and `SqliteRepository` which keeps dictionaries in an sqlite database.

Blacklite will automatically train a dictionary from the incoming content if it does not exist.  You can
tweak the dictionary parameters, but the defaults work fine.

```xml
<codec class="com.tersesystems.blacklite.codec.zstd.ZstdDictCodec">
<level>9</level>
  <repository class="com.tersesystems.blacklite.codec.zstd.ZstdDictFileRepository">
    <file>logs/dictionary</file>
  </repository>
</codec>
```

You can also specify a SQLite database containing dictionaries, using the zstandard dictionary ids as a lookup.  This lets you use multiple dictionaries.

```xml
<repository class="com.tersesystems.blacklite.codec.zstd.ZStdDictSqliteRepository">
  <file>logs/dictionary.db</file>
</repository>
```

Be aware that if you use a zstandard dictionary, you must have it available to read the logs.  If you lose it, the logs will be unreadable!

##### Triggering Policy

There is one triggering policy, using the maximum number of rows in the archive.

```xml
<triggeringPolicy class="com.tersesystems.blacklite.archive.RowBasedTriggeringPolicy">
    <maximumNumRows>500000</maximumNumRows>
</triggeringPolicy>
```

##### Rolling Strategies

Fixed Window Rolling Strategy will set up a number of SQLite archive databases, using `%i` to indicate the index.

```xml
<rollingStrategy class="com.tersesystems.blacklite.logback.FixedWindowRollingStrategy">
  <fileNamePattern>logs/archive.%i.db</fileNamePattern>
  <minIndex>1</minIndex>
  <maxIndex>10</maxIndex>
</rollingStrategy>
```

Time Based Rolling Strategy uses a date system, which will roll over renaming the file to the given date.

```xml
<rollingStrategy class="com.tersesystems.blacklite.logback.TimeBasedRollingStrategy">
  <fileNamePattern>logs/archive.%d{yyyyMMdd'T'hhmm,utc}.db</fileNamePattern>
  <maxHistory>20</maxHistory>
  <totalSizeCap>10M</totalSizeCap>
  <cleanHistoryOnStartup>true</cleanHistoryOnStartup>
</rollingStrategy>
```

### Log4J 2

Log4J 2 uses a blocking appender, so it should be wrapped behind an `Async` appender:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="INFO" packages="com.tersesystems.blacklite.log4j2,com.tersesystems.blacklite.log4j2.zstd">
    <appenders>
        <Blacklite name="Blacklite">
            <file>/${sys:java.io.tmpdir}/blacklite/log4j.db</file>

            <!-- https://mvnrepository.com/artifact/com.vlkan.log4j2/log4j2-logstash-layout -->
            <LogstashLayout dateTimeFormatPattern="yyyy-MM-dd'T'HH:mm:ss.SSSSSSZZZ"
                            eventTemplateUri="classpath:LogstashJsonEventLayoutV1.json"
                            prettyPrintEnabled="false"/>

            <Archiver file="/tmp/blacklite/archive.db" archiveAfterRows="10000">
                <ZStdDictCodec>
                    <level>3</level>
                    <sampleSize>102400000</sampleSize>
                    <dictSize>10485760</dictSize>
                    <!-- <FileRepository file="${sys:java.io.tmpdir}/blacklite/dictionary"/> -->
                    <SqliteRepository url="jdbc:sqlite:${sys:java.io.tmpdir}/blacklite/dict.db"/>
                </ZStdDictCodec>

                <FixedWindowRollingStrategy
                        min="1"
                        max="5"
                        filePattern="${sys:java.io.tmpdir}/blacklite/archive-%i.db"/>
                <RowBasedTriggeringPolicy>
                    <maximumNumRows>100000</maximumNumRows>
                </RowBasedTriggeringPolicy>
            </Archiver>
        </Blacklite>

        <Async name="AsyncBlacklite">
            <AppenderRef ref="Blacklite"/>
            <JCToolsBlockingQueue/>
        </Async>
    </appenders>
    <Loggers>
        <Root level="DEBUG">
            <AppenderRef ref="AsyncBlacklite"/>
        </Root>
    </Loggers>
</Configuration>
```

It is broadly similar to the Logback system.

### Reader

There's a command line blacklite reader that can return the contents given parameters.

You can run it from the command line using the JAR:

```
export BLACKLITE_VERSION=0.1.0-SNAPSHOT
java -jar $HOME/.m2/repository/com/tersesystems/blacklite/blacklite-reader/$BLACKLITE_VERSION/blacklite-reader-$BLACKLITE_VERSION-all.jar $*;
```

but it's probably easier to wrap it in a bash script, like `blacklite-reader`.

```
$ blacklite-reader live.db
```

which then renders the contents:

```json
{"@timestamp":"2020-11-18T19:46:58.111-08:00","@version":"1","message":"Module execution: 2042ms","logger_name":"com.google.inject.internal.util.Stopwatch","thread_name":"main","level":"DEBUG","level_value":10000,"application.home":"/home/wsargent/work/memalloctest/target/universal/stage"}
```

You can also render the count:

```
$ blacklite-reader -c live.db
126
```

The blacklite reader has a number of command line options:

```
Usage: blacklite-reader [-chvV] [--binary] [--charset=CHARSET] [-t=<timezone>]
                        [-w=WHERE] [-a=AFTER | -s=START] [-b=BEFORE | -e=END]
                        FILE
Outputs content from blacklite database
      FILE                one or more files to read
  -a, --after=AFTER       Only render entries after the given date
  -b, --before=BEFORE     Only render entries before the given date
      --binary            Renders content as raw BLOB binary
  -c, --count             Return a count of entries
      --charset=CHARSET   Charset (default: utf8)
  -e, --end=END           Only render entries before the given epoch second
  -h, --help              display this help message
  -s, --start=START       Only render entries after the start of given epoch
                            second
  -t, --timezone=<timezone>
                          Use the given timezone for before/after dates
  -v, --verbose           Print verbose logging
  -V, --version           display version info
  -w, --where=WHERE       Custom SQL WHERE clause
```

You can use absolute or relative date processing, i.e. "five seconds ago" using [Natty](https://github.com/joestelmach/natty/blob/master/src/test/java/com/joestelmach/natty/DateTest.java):

```
./blacklite-reader \
  --after="2020-11-03 19:22:09" \
  --before="2020-11-03 19:22:11" \
  --timezone=PST \
  /tmp/blacklite/archive.2020-11-03-07-22.669.db
```

You can also extract data using the `binary` flag and redirect to a file, which you can decompress later.

```bash
./blacklite-reader --binary /tmp/blacklite/archive.db > zarchive.zst
zstd -d zarchive.zst
```

## Benchmarks

Benchmarks are provided with some caveats. There are a number of [presentations](https://www.cs.utexas.edu/~jaya/slides/apsys17-sqlite-slides.pdf) on the complexities of benchmarking SQLite.  Benchmarking can vary considerably depending on your hardware, and in particular IO writes may be different given a 250 MB/sec SSD available to a cloud instance.  If you're writing more log entries than your IO throughput then memory mapping can offload some of it, but at some point you will saturate something and God's Own Backpressure will be applied.

### Platform

Using my local laptop:

* Dell XPS Laptop running Elementary 5.1.7
* Built on Ubuntu 18.04.4 LTS
* Intel® Core™ i7-9750H CPU @ 2.60GHz × 6
* NVMe M2 SSD

### Throughput

The first question is how many log entries per second can be written to SQLite, in total.  This happens in a background thread, so the throughput is more significant than the latency here.

```
Benchmark                                     Mode  Cnt       Score       Error  Units
DefaultEntryStoreBenchmark.benchmark         thrpt   20  803206.197 ± 54816.464  ops/s
```

The error margin is large because commits are batched and don't happen on every write.  For comparison, a file appender with `immediateFlush=false` is roughly [~1789 ops/ms](https://github.com/wsargent/slf4j-benchmark#throughput-benchmarks).

### Latency

The next question is how much latency does logging add to your main thread?

```
Benchmark                                     Mode  Cnt       Score       Error  Units
AsyncEntryWriterBenchmark.benchmark           avgt   10      11.920 ±     0.099  ns/op
BlockingEntryWriterBenchmark.benchmark        avgt   10       1.057 ±     0.010  ns/op
```

The async entry writer takes 11 nanoseconds to add the entry to the queue so that the background thread can write it.  The blocking entry writer takes 1 nanosecond, but then the SQLite write itself must be added onto that, which is roughly between 1k - 3k nanoseconds, depending on batch commits.

Finally, there's the cost of archiving data using a zstandard codec.

```
Benchmark                                     Mode  Cnt       Score       Error  Units
codec.zstd.ZstdCodecBenchmark.benchmark       avgt   20    4431.193 ±    17.608  ns/op
codec.zstd.ZstdDictCodecBenchmark.testWrite   avgt   20    3079.556 ±   226.191  ns/op
```

Compressing a log entry with ZStandard with a compression level of `3` takes around 4 microseconds.  Once a dictionary is trained, it takes 3 microseconds and yields much better [compression results](COMPRESSION.md).

### Improvements using tmpfs

You can improve Blacklite performance even further if you are willing to use a `tmpfs` filesystem as a backing store.  This is a tactic used by [Alluxio](https://github.com/Alluxio/alluxio/blob/master/core/server/worker/src/main/java/alluxio/worker/block/meta/StorageTier.java#L141), for example.

The easiest thing to do is to set up `/var/log` as [tmpfs](
https://forums.gentoo.org/viewtopic-t-371889-start-0-postdays-0-postorder-asc-highlight-tmpfs.html?sid=13bc57e79de631391821d1869615eb45) and go from there.

Using a `tmpfs` filesystem does not require that you constrain your logs to the amount of memory you have, but it does mean that the logs will be removed when the server shuts down.  To get around this, you can [run some scripts on shutdown](https://web.archive.org/web/20200809170437/https://debian-administration.org/article/661/A_transient_/var/log) to transfer the log files.
