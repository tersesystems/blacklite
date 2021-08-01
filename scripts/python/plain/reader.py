#!/usr/bin/env python3

from sqlite_utils import Database
import time
import json
import os.path

dbpath = "./blacklite.db"
if not os.path.exists(dbpath):
    raise f'"No database found at {dbpath}'

db = Database(dbpath)

epoch_time = int(time.time())

for row in db["entries"].rows_where("epoch_secs < ? limit 1", [epoch_time]):
    epoch_secs = row['epoch_secs']
    level = row['level']
    content = json.loads(row['content'])
    print("epoch_secs = ", epoch_secs, "level = ", level, "message = ", content['message'])
