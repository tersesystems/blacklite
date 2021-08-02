# Reader

There's a command line blacklite reader that can return the contents given parameters.

You need to download the JAR, using [search.maven.org](https://search.maven.org/artifact/com.tersesystems.blacklite/blacklite-reader).

You can run it from the command line using the JAR:

```
export VERSION=1.0.1
wget https://repo1.maven.org/maven2/com/tersesystems/blacklite/blacklite-reader/$VERSION/blacklite-reader-$VERSION-all.jar
java -jar blacklite-reader-$VERSION-all.jar $*;
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

Or you can use one of the python scripts to create a decompressed database.
