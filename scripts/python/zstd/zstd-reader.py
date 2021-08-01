#!/usr/bin/env python3


import os.path
from sqlite_utils import Database
import time
import json
import zstandard as zstd
import os.path

dbpath = "./blacklite-zstd.db"
if not os.path.exists(dbpath):
    raise f'"No database found at {dbpath}'

db = Database(dbpath)

epoch_time = int(time.time())

dctx = zstd.ZstdDecompressor()

for row in db["entries"].rows_where("epoch_secs < ? limit 1", [epoch_time]):
    epoch_secs = row['epoch_secs']
    level = row['level']
    content = json.loads(dctx.decompress(row['content']))
    print("epoch_secs = ", epoch_secs, "level = ", level, "message = ", content['message'])
