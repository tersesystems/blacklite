## A Quick Guide to SQLite


## SQLite Tables

The table structure is as follows:

```
CREATE TABLE IF NOT EXISTS entries (
  epoch_secs LONG, // number of seconds since epoch
  nanos INTEGER,  // nanoseconds in the second
  level INTEGER,  // numeric level of logging
  content BLOB    // raw bytes from logging framework encoder / layout
);
```

There are no indices on any columns, for speed of inserts.  You are free to add an index if you
expect to be querying on a regular basis:

```
CREATE INDEX IF NOT EXISTS epoch_secs_idx ON entries (epoch_secs);
CREATE INDEX IF NOT EXISTS level_idx ON entries (level);
```

Also note that if you are using structured logging, you can [index using expressions](https://www.sqlite.org/expridx.html), including over JSON:

```
CREATE INDEX message_idx ON entries (json_extract(content, '$.message') COLLATE NOCASE);
```

Because the database shows the timestamp as seconds since epoch, a view is added that displays
 the timestamp in both UTC and local time.

```
CREATE VIEW IF NOT EXISTS entries_view AS
  SELECT datetime(epoch_secs, 'unixepoch', 'utc') as timestamp_utc,
  datetime(epoch_secs, 'unixepoch', 'localtime') as timestamp_local,
  nanos, level, content
  FROM entries
```

## Querying

The easiest way to query is to use [DB Browser for SQLite](https://sqlitebrowser.org/).

The command line option works fine:

```
sqlite3 archive.db
```

And from there you can do individual queries (the `identity` codec is used here so there's no
 encoding):

```sqlite-psql
SELECT _rowid_, datetime(epoch_secs, 'unixepoch', 'localtime'), nanos, content FROM entries
WHERE content IS NOT NULL ORDER BY epoch_secs ASC LIMIT 0, 100;
```

```
6940072|2020-10-10 19:08:58|679000000|{"@timestamp":"2020-10-10T19:08:58.679-07:00","@version":"1","message":"debugging is fun!!! 2020-10-11T02:08:58.679486Z","logger_name":"com.tersesystems.rifter.logback.Main","thread_name":"main","level":"WARN","level_value":30000}
```

You can select from a specific rowid:

```
SELECT _rowid_,* from entries where _rowid_ = 6940072;
```

and get back:

```
6940072|1602382138|679000000|30000|{"@timestamp":"2020-10-10T19:08:58.679-07:00","@version":"1","message":"debugging is fun!!! 2020-10-11T02:08:58.679486Z","logger_name":"com.tersesystems.rifter.logback.Main","thread_name":"main","level":"WARN","level_value":30000}
|
```

If you are using structured logging, you can query the content directly using the [JSON
 extensions](https://www.sqlite.org/json1.html):

```
sqlite3 live.db "SELECT * FROM entries_view WHERE json_extract(content, '$.message') LIKE
 'warning%' LIMIT 1"
```

You can also select an individual row using the rowid, an implicit autoincrementing primary key
 that SQLite adds automatically:

```
SELECT _rowid_,* from entries where _rowid_ = 6940072;
```

If you have a query that you want to go over multiple SQLite files, you should use the [ATTACH
 DATABASE](https://www.sqlite.org/lang_attach.html) command and then query over all the databases at once.

### Views

There is an `entries_view` that turns the epoch seconds into a real date.

XXX I don't have epoch nanoseconds working here :-/

### JSON Support

https://www.sqlite.org/json1.html

The CLI also lets you do `json_extract` on various logs:

```
sqlite> SELECT json_extract(content,'$.message') FROM entries WHERE _rowid_ = 6940072;
debugging is fun!!! 2020-10-11T02:08:58.679486Z
```

and you can search through messages:

```
sqlite> SELECT _rowid_,* FROM entries WHERE json_extract(content,'$.message') LIKE '%08:58.679486Z';
6940072|1602382138|679000000|30000|{"@timestamp":"2020-10-10T19:08:58.679-07:00","@version":"1","message":"debugging is fun!!! 2020-10-11T02:08:58.679486Z","logger_name":"com.tersesystems.rifter.logback.Main","thread_name":"main","level":"WARN","level_value":30000}
|
6940073|1602382138|679000000|30000|{"@timestamp":"2020-10-10T19:08:58.679-07:00","@version":"1","message":"debugging is fun!!! 2020-10-11T02:08:58.679486Z","logger_name":"com.tersesystems.rifter.logback.Main","thread_name":"main","level":"WARN","level_value":30000}
|
sqlite>
```

Note we get back two rows here.  This is because the nanotime resolution is not absolute, and so
 it's possible for two different statements to show up in the same nanosecond.  If you're
  spitting out logs, you may want to include a unique flake id or counter so that you can
   distinguish them.

You can also query from the command line:

```
sqlite3 ./rifter.db "SELECT json_extract(content,'$.message') FROM entries WHERE _rowid_ = 6940072"
debugging is fun!!! 2020-10-11T02:08:58.679486Z
```

or you can extract the JSON directly to stdout and pass it through `jq`:

```
❱ sqlite3 ./rifter.db  "SELECT content FROM entries WHERE content IS NOT NULL LIMIT 1" | jq
{
  "@timestamp": "2020-10-10T19:08:58.679-07:00",
  "@version": "1",
  "message": "debugging is fun!!! 2020-10-11T02:08:58.679444Z",
  "logger_name": "com.tersesystems.rifter.logback.Main",
  "thread_name": "main",
  "level": "WARN",
  "level_value": 30000
}
```

## Encoded Content

For compressed content, blobs can be written out as files on the filesystem and then decompressed.

```
sqlite3 rifter.db "SELECT writefile('1.zst', content) FROM entries WHERE _rowid_ = 1"
```

If you look at the file with `vi`, you'll see plain text.  This is because vi is smart enough to
 autodecode for you. Don't be deceived, it's actually still zstandard:

```
❱ hexdump 1.zst
0000000 b528 fd2f e720 059d 7200 268b 3022 568b
0000010 0301 966d 5525 8f8e 1a94 5e28 0523 e1fc
0000020 b8a4 4e43 b0ee cb33 f054 30fc 4080 889d
0000030 5507 c4d5 9d9e 9ec7 c186 0101 f7c2 1f8f
0000040 107e 58c2 cc2a 47a3 0ac7 c2d2 3983 f35a
0000050 e492 7119 3532 d9e4 cd0d 792d d826 203c
0000060 0d8b adc9 ef35 09e8 800b 2849 a865 5144
0000070 e894 9244 4976 ae8b c735 90c1 b9ef 6eb1
0000080 4786 df57 c6c7 e4b5 7167 d574 ba5d 1428
0000090 0d2b aee8 1580 0c50 28b5 cca3 0cf7 cd90
00000a0 e866 e2e6 010e 0008 0132 1c38 e712 c5b8
00000b0 c54a 8210 9285 1403 34ea 7770
00000bc
```

And you can decrypt it as follows:

```
❱ zstd -d 1.zst
1.zst               : 231 bytes
```

## Python support with sqlite-utils

Extracting content is simple and easy using [sqlite-utils](https://sqlite-utils.readthedocs.io/en
/stable/):

```bash
sudo apt install python3-pip
sudo pip3 install sqlite-utils
sudo pip3 install zstandard
```

And then to read the compressed data:

```python
#!/usr/bin/env python3

from sqlite_utils import Database
import time
import json
import zstandard as zstd

db = Database("archive.db")

epoch_time = int(time.time())

dict_row = db.execute("select dict_bytes from zstd_dicts LIMIT 1").fetchone()
dict = zstd.ZstdCompressionDict(dict_row[0])
dctx = zstd.ZstdDecompressor(dict_data = dict)

for row in db["entries"].rows_where("epoch_secs < ? limit 1", [epoch_time]):
    epoch_secs = row['epoch_secs']
    level = row['level']
    content = json.loads(dctx.decompress(row['content']))
    print("epoch_secs = ", epoch_secs, "level = ", level, "message = ", content['message'])
```

This produces:

```
❱ ./reader.py
epoch_secs =  1603055625 level =  10000 message =  debugging is fun!!! 2020-10-18T21:13:45.317090Z
```

You can also use SQL custom functions, which allows you to use SQLite's JSON functions natively:

```python
#!/usr/bin/env python3

from sqlite_utils import Database
import time
import json
import zstandard as zstd

db = Database("archive.db")

epoch_time = int(time.time())

dict_row = db.execute("select dict_bytes from zstd_dicts LIMIT 1").fetchone()
dict = zstd.ZstdCompressionDict(dict_row[0])
dctx = zstd.ZstdDecompressor(dict_data = dict)

@db.register_function
def decode(s):
    return dctx.decompress(s)

sql = """select
timestamp_utc,
json_extract(decode(content),'$.message') as message,
decode(content) as decoded
from entries_view limit 1
"""

row = db.execute(sql).fetchone()

ts = row[0]
message = row[1]
decoded = row[2]
print("ts = ", ts, "message = ", message, "decoded = ", decoded)
```
