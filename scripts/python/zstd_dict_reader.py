#!/usr/bin/env python3

from sqlite_utils import Database
import time
import json
import zstandard as zstd

db = Database("../data/blacklite_zstd_dict.db")

epoch_time = int(time.time())

dict_row = db.execute("select dict_bytes from zstd_dicts LIMIT 1").fetchone()
dict = zstd.ZstdCompressionDict(dict_row[0])
dctx = zstd.ZstdDecompressor(dict_data = dict)

for row in db["entries"].rows_where("epoch_secs < ? limit 1", [epoch_time]):
    epoch_secs = row['epoch_secs']
    level = row['level']
    content = json.loads(dctx.decompress(row['content']))
    print("epoch_secs = ", epoch_secs, "level = ", level, "message = ", content['message'])
