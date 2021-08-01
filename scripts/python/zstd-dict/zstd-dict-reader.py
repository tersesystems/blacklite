#!/usr/bin/env python3

import os.path
from sqlite_utils import Database
import time
import json
import zstandard as zstd


source = "../data/blacklite_zstd_dict.db"
if not os.path.exists(source):
    raise f'"No source database found at {source}'

db = Database(source)

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
content = json.loads(row[2])

print("ts = ", ts, "message = ", message, "content = ", content)
