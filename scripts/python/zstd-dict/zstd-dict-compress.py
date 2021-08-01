#!/usr/bin/env python3

import sqlite3

import os.path
from sqlite_utils import Database
import zstandard as zstd

#
# This script takes in a blacklite database and compresses the
# content to zstandard encoded content using dictionary compression.
#
# The output is a new blacklite database containing the
# compressed content.
#

source = "../data/blacklite.db"
dest = "./blacklite-zstd-dict.db"

if not os.path.exists(source):
    raise f'"No source database found at {source}'


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


def tracer(sql, params):
    print("SQL: {} - params: {}".format(sql, params))


def content(row):
    return bytes(row["content"], encoding='utf8')


def create_dictionary(db: Database):
    # make this parse through the entire entries table and make
    # a dictionary from it...
    dict_size = 10485760
    samples = list(map(content, (db.query("SELECT content from entries limit 10000"))))
    # https://python-zstandard.readthedocs.io/en/latest/compressor.html
    return zstd.train_dictionary(dict_size, samples)


initialize()

# If there are errors in the user defined function, this is the
# only way to get the actual error and not
# "user-defined function raised exception"
sqlite3.enable_callback_tracebacks(True)

# https://sqlite-utils.datasette.io/en/stable/python-api.html
db = Database(source) # tracer=tracer

zstd_dict = create_dictionary(db)
cctx = zstd.ZstdCompressor(dict_data=zstd_dict)


@db.register_function
def compress(s):
    return cctx.compress(bytes(s, encoding='utf8'))


try:
    db.attach("blacklite_zstd_dict", "blacklite-zstd-dict.db")
    db.execute("""
     CREATE TABLE IF NOT EXISTS blacklite_zstd_dict.zstd_dicts (
        dict_id LONG NOT NULL PRIMARY KEY,
        dict_bytes BLOB NOT NULL)
    """)

    db.execute("INSERT INTO blacklite_zstd_dict.zstd_dicts VALUES (?, ?)", parameters = [
      zstd_dict.dict_id(),
      zstd_dict.as_bytes()
    ])

    db.execute("""
    insert into blacklite_zstd_dict.entries (epoch_secs, nanos, level, content)
    select epoch_secs, nanos, level, compress(content) from entries
    """)

    db.execute("COMMIT")
except Exception as e:
    print("Unexpected error:", e)
