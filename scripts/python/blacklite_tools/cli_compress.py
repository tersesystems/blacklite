#!/usr/bin/env python3
"""CLI tool for compressing blacklite databases with zstandard."""

import os.path
import sqlite3
import click
import zstandard as zstd
from sqlite_utils import Database

from .database import create_dictionary


@click.command()
@click.argument("source")
@click.argument("dest")
@click.option("-d", "--dict/--no-dict", default=False,
              help="Create and use zstandard dictionary")
def main(source, dest, dict):
    """Compress a blacklite database with zstandard compression.

    Args:
        source: Source database path
        dest: Destination database path
        dict: Whether to train and use a compression dictionary
    """
    if not os.path.exists(source):
        raise IOError(f'No database found at {source}')

    # Enable callback tracebacks for better UDF error messages
    sqlite3.enable_callback_tracebacks(True)

    db = Database(source)

    initialize_dest(dest)
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
        """UDF for compressing content."""
        if isinstance(s, str):
            return cctx.compress(bytes(s, "utf8"))
        return cctx.compress(s)

    db.register_function(compress)

    db.execute("""
        INSERT INTO blacklite_zstd.entries (epoch_secs, nanos, level, content)
        SELECT epoch_secs, nanos, level, compress(content) FROM entries
    """)

    db.execute("COMMIT")


def initialize_dest(dest):
    """Initialize destination database with schema.

    Args:
        dest: Destination database path
    """
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


if __name__ == '__main__':
    main()
