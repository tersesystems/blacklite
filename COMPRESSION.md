# Compression

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
