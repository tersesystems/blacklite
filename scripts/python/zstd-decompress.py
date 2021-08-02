#!/usr/bin/env python3

import os.path
import sqlite3
from sqlite_utils import Database
import zstandard as zstd
import click

#
# This script takes in a database that have entries
# compressed with zstandard compression and outputs
# an uncompressed database.
#

@click.command()
@click.argument("source")
@click.argument("dest")
def main(source, dest):
    if not os.path.exists(source):
        raise IOError(f'"No source database found at {source}')

    try:
        # https://sqlite-utils.datasette.io/en/stable/python-api.html
        db = Database(source, tracer=tracer)

        # If there are errors in the user defined function, this is the
        # only way to get the actual error and not
        # "user-defined function raised exception"
        sqlite3.enable_callback_tracebacks(True)

        dctx = None
        if db["zstd_dicts"].exists():
            dict_row = db.execute("select dict_bytes from zstd_dicts LIMIT 1").fetchone()
            if dict_row is None:
                raise Exception("No dictionary found in zstd_dicts table!")
            else:
                dict = zstd.ZstdCompressionDict(dict_row[0])
                dctx = zstd.ZstdDecompressor(dict_data = dict)
        else:
            dctx = zstd.ZstdDecompressor()

        @db.register_function
        def decompress(s):
            return dctx.decompress(s)

        initialize(dest)

        db.attach("decompress", dest)
        db.execute("""
        insert into decompress.entries (epoch_secs, nanos, level, content)
        select epoch_secs, nanos, level, decompress(content) from entries
        """)

        db.execute("COMMIT")
    except Exception as e:
        print("Unexpected error:", e)

def initialize(dest):
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


if __name__ == '__main__':
    main()

