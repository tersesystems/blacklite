#!/usr/bin/env python3

import sqlite3
from sqlite_utils import Database
import click
import zstandard as zstd
import os.path

@click.command()
@click.argument("source")
@click.argument("dest")
@click.option("-d", "--dict/--no-dict", default=False, help="Create and use zstandard dictionary")
def main(source, dest, dict):
    if not os.path.exists(source):
        raise IOError(f'"No database found at {source}')

    # XXX old versions of sqlite_utils do not have "attach" and
    # so will error out.  How to establish right version?

    db = Database(source)
    # If there are errors in the user defined function, this is the
    # only way to get the actual error and not
    # "user-defined function raised exception"
    sqlite3.enable_callback_tracebacks(True)

    initialize(dest)
    db.attach("blacklite_zstd", dest)

    cctx = None
    if dict:
        zstd_dict = create_dictionary(db)
        cctx = zstd.ZstdCompressor(dict_data=zstd_dict)

        db.execute("""
             CREATE TABLE IF NOT EXISTS blacklite_zstd.zstd_dicts (
                dict_id LONG NOT NULL PRIMARY KEY,
                dict_bytes BLOB NOT NULL)
            """)
        db.execute("INSERT INTO blacklite_zstd.zstd_dicts VALUES (?, ?)", [
            zstd_dict.dict_id(),
            zstd_dict.as_bytes()
        ])
    else:
        cctx = zstd.ZstdCompressor()

    def compress(s):
        return cctx.compress(bytes(s, "utf8"))

    db.register_function(compress)

    db.execute("""
        insert into blacklite_zstd.entries (epoch_secs, nanos, level, content)
        select epoch_secs, nanos, level, compress(content) from entries
        """)

    db.execute("COMMIT")


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


def create_dictionary(database: Database):
    # make this parse through the entire entries table and make
    # a dictionary from it...

    def content_bytes(row):
        return bytes(row["content"], encoding='utf8')

    dict_size = 10485760
    samples = list(map(content_bytes, (database.query("SELECT content from entries limit 10000"))))
    # https://python-zstandard.readthedocs.io/en/latest/compressor.html
    return zstd.train_dictionary(dict_size, samples)


if __name__ == '__main__':
    main()

