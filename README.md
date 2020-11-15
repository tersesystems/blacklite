# Blacklite

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

In addition, providing data in SQLite format means you can leverage tools built using SQLite:

* [sqlite-utils](https://sqlite-utils.readthedocs.io/en/stable/): Read and process SQLite files from command line
* [Datasette](https://docs.datasette.io/en/stable/): Exposing SQLite files as web applications
* [Observable HQ](https://observablehq.com/@mbostock/sqlite): Using SQLite data in visualization notebooks

More details on [querying SQLite using Python and sqlite3 here](SQLITE.md).

## Installation

### Gradle

Add the following resolver:

```
repositories {
    maven {
        url  "https://dl.bintray.com/tersesystems/maven" 
    }
}
```

And then add the libraries and codecs that you want:

```
implementation 'com.tersesystems.blacklite:blacklite-logback:<latestVersion>'
implementation 'com.tersesystems.blacklite:blacklite-codec-zstd:<latestVersion>'
```

or 

```
implementation 'com.tersesystems.blacklite:blacklite-log4j2:<latestVersion>'
implementation 'com.tersesystems.blacklite:blacklite-codec-zstd:<latestVersion>'
```

### Maven

Add the `tersesystems-maven` repository to `settings.xml`:

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<settings xsi:schemaLocation='http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd'
          xmlns='http://maven.apache.org/SETTINGS/1.0.0' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>

    <profiles>
        <profile>
            <repositories>
                <repository>
                    <snapshots>
                        <enabled>false</enabled>
                    </snapshots>
                    <id>bintray-tersesystems-maven</id>
                    <name>bintray</name>
                    <url>https://dl.bintray.com/tersesystems/maven</url>
                </repository>
            </repositories>
            <id>bintray</id>
        </profile>
    </profiles>
    <activeProfiles>
        <activeProfile>bintray</activeProfile>
    </activeProfiles>
</settings>
```

and then add the libraries:

```
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

### SBT

SBT installation is fairly straightforward.

```sbt
resolvers += Resolver.bintrayRepo("tersesystems", "maven")
libraryDependencies += "com.tersesystems.blacklite" % "blacklite-logback" % "<latestVersion>"
libraryDependencies += "com.tersesystems.blacklite" % "blacklite-codec-zstd" % "<latestVersion>"
```

## Configuration

Configuration is straightforward.  The archiver can be a bit complicated, but it works much the same way that rolling file appenders do.

The archiver has a `archiveAfterRows` property that is the maximum number of rows in the live database.  When the maximum number of rows 
is reached, the oldest rows will be moved into the sqlite.db archive specified by the `file` property.

The archive file will be rolled over when the triggering policy is matched.  In the case of the `ArchiveRowsTriggeringPolicy`, 
this is the maximum number of rows in the archive database -- after that, the archive database will be renamed according to
the rolling strategy and another archive file will be created.

The archiver can take a codec.  If using dictionary compression, it's `ZStdDictCodec` and the dictionary must be defined 
in a repository.

There are two repositories for dictionaries: `FileRepository` which points directly to a zstandard dictionary on the
filesystem, and `SqliteRepository` which keeps dictionaries in an sqlite database.

```
<ZStdDictCodec>
    <level>3</level>
    <sampleSize>102400000</sampleSize>
    <dictSize>10485760</dictSize>
    <!-- <FileRepository file="${sys:java.io.tmpdir}/blacklite/dictionary"/> -->
    <SqliteRepository url="jdbc:sqlite:${sys:java.io.tmpdir}/blacklite/dict.db"/>
</ZStdDictCodec>
```

### Logback

The logback appender uses [JCTools](https://jctools.github.io/JCTools/) internally as an asynchronous queue.  This means you don't need to use an `AsyncAppender` on top.

You should always use a `shutdownHook` to allow Logback to drain the queue before exiting.
 
```xml
<configuration>

    <shutdownHook class="ch.qos.logback.core.hook.DelayingShutdownHook">
        <delay>1000</delay>
    </shutdownHook>

    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%date{H:mm:ss.SSS} [%-5level] %logger{15} - %message%ex%n</pattern>
        </encoder>
    </appender>

    <appender name="FILE" class="ch.qos.logback.core.FileAppender">
        <file>/tmp/blacklite/entries.json</file>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        </encoder>
    </appender>

    <appender name="BLACKLITE" class="com.tersesystems.blacklite.logback.BlackliteAppender">
        <url>jdbc:sqlite:/tmp/blacklite/live.db</url>

        <archiver class="com.tersesystems.blacklite.archive.DefaultArchiver">
            <file>/tmp/blacklite/archive.db</file>
            <archiveAfterRows>10000</archiveAfterRows>

            <codec class="com.tersesystems.blacklite.codec.zstd.ZStdCodec">
                <level>9</level>
            </codec>

            <rollingStrategy class="com.tersesystems.blacklite.logback.TimeBasedRollingStrategy">
                <fileNamePattern>/tmp/blacklite/archive.%d{yyyy-MM-dd-hh-mm.SSS}.db</fileNamePattern>
                <maxHistory>20</maxHistory>
            </rollingStrategy>
            
            <triggeringPolicy class="com.tersesystems.blacklite.archive.ArchiveRowsTriggeringPolicy">
                <maximumNumRows>500000</maximumNumRows>
            </triggeringPolicy>
        </archiver>

        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        </encoder>
    </appender>

    <root level="TRACE">
        <appender-ref ref="BLACKLITE"/>
        <appender-ref ref="FILE"/>
    </root>

</configuration>
```

### Log4J 2

Log4J 2 uses a blocking appender, so it should be wrapped behind an `Async` appender:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="INFO" packages="com.tersesystems.blacklite.log4j2,com.tersesystems.blacklite.log4j2.zstd">
    <appenders>
        <Blacklite name="Blacklite">
            <url>jdbc:sqlite:/${sys:java.io.tmpdir}/blacklite/log4j.db</url>

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
                <ArchiveRowsTriggeringPolicy>
                    <maximumNumRows>100000</maximumNumRows>
                </ArchiveRowsTriggeringPolicy>
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

### Reader

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

You can run it from the command line using the JAR:

```
java -jar blacklite-reader.jar
```

but it's probably easier to wrap it in a bash script.

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

Using my local laptop:

* Dell XPS Laptop running Elementary 5.1.7 
* Built on Ubuntu 18.04.4 LTS 
* Intel® Core™ i7-9750H CPU @ 2.60GHz × 6
* NVMe M2 SSD

``` 
Benchmark                                     Mode  Cnt       Score       Error  Units
DefaultEntryStoreBenchmark.benchmark         thrpt   20  803206.197 ± 54816.464  ops/s
AsyncEntryWriterBenchmark.benchmark           avgt   10      11.920 ±     0.099  ns/op
BlockingEntryWriterBenchmark.benchmark        avgt   10       1.057 ±     0.010  ns/op
codec.zstd.ZstdCodecBenchmark.benchmark       avgt   20    4431.193 ±    17.608  ns/op
codec.zstd.ZstdDictCodecBenchmark.testWrite   avgt   20    3079.556 ±   226.191  ns/op
```

Note that the blocking entry writer is faster than the async entry writer, but after that point you're 
still writing to SQLite from the main thread, which is between 1-3k nanoseconds if you're unlucky enough
to hit a batch commit.  The async entry writer will offload that write onto a background thread so your
critical HTTP response rendering thread is never impacted.

Note also that benchmarking can vary considerably depending on your hardware, and in particular IO
writes may be different given a 250 MB/sec SSD available to a cloud instance.  If you're writing more 
log entries than your IO throughput then memory mapping can offload some of it, but at some point you
will saturate something and God's Own Backpressure will be applied.

### Compression

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
