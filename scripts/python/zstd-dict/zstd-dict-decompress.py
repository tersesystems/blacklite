#!/usr/bin/env python3

import sqlite3
from sqlite_utils import Database
import zstandard as zstd

#
# This script takes in a database that have entries
# compressed with zstandard compression and outputs
# an uncompressed database.
#

source = "./blacklite-zstd-dict.db"

dest = "./blacklite-decompress.db"


def initialize():
    cdb = Database(dest)
    cdb.execute("""CREATE TABLE IF NOT EXISTS entries (
      epoch_secs LONG,
      nanos INTEGER,
      level INTEGER,
      content BLOB)
    """)
    cdb.execute("""CREATE VIEW IF NOT EXISTS entries_view AS
      SELECT datetime(epoch_secs, 'unixepoch', 'utc') as timestamp_utc,
      datetime(epoch_secs, 'unixepoch', 'localtime') as timestamp_local,
      nanos, level, content
      FROM entries
    """)

initialize()

def tracer(sql, params):
    print("SQL: {} - params: {}".format(sql, params))

# https://sqlite-utils.datasette.io/en/stable/python-api.html
db = Database(source, tracer=tracer)

# If there are errors in the user defined function, this is the
# only way to get the actual error and not
# "user-defined function raised exception"
sqlite3.enable_callback_tracebacks(True)

dict_row = db.execute("select dict_bytes from zstd_dicts LIMIT 1").fetchone()
dict = zstd.ZstdCompressionDict(dict_row[0])
dctx = zstd.ZstdDecompressor(dict_data = dict)

@db.register_function
def decode(s):
    return dctx.decompress(s)

try:
    db.attach("decompress", dest)
    db.execute("""
    insert into decompress.entries (epoch_secs, nanos, level, content)
    select epoch_secs, nanos, level, decode(content) from entries
    """)

    db.execute("COMMIT")
except Exception as e:
    print("Unexpected error:", e)
