#!/usr/bin/env python3

from sqlite_utils import Database
import zstandard as zstd

def initialize():
    cdb = Database("./blacklite-zstd.db")
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
db = Database("../data/blacklite.db", tracer=tracer)
cctx = zstd.ZstdCompressor()

@db.register_function
def compress(s):
    return cctx.compress(s)

try:
    db.attach("blacklite_zstd", "blacklite-zstd.db")
    db.execute("""
    insert into blacklite_zstd.entries (epoch_secs, nanos, level, content) select epoch_secs, nanos, level, compress(content) from entries
    """)

    db.execute("COMMIT")
except Exception as e:
    print("Unexpected error:", e)
