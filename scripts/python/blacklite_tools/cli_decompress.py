#!/usr/bin/env python3
"""CLI tool for decompressing blacklite databases."""

import os.path
import sqlite3
import click
import zstandard as zstd
from sqlite_utils import Database


@click.command()
@click.argument("source")
@click.argument("dest")
def main(source, dest):
    """Decompress a zstandard-compressed blacklite database.

    Automatically detects and uses dictionary if present in source database.

    Args:
        source: Source database path (compressed)
        dest: Destination database path (uncompressed)
    """
    if not os.path.exists(source):
        raise IOError(f'No source database found at {source}')

    try:
        db = Database(source)

        # Enable callback tracebacks for better UDF error messages
        sqlite3.enable_callback_tracebacks(True)

        dctx = None
        if db["zstd_dicts"].exists():
            dict_row = db.execute("SELECT dict_bytes FROM zstd_dicts LIMIT 1").fetchone()
            if dict_row is None:
                raise Exception("No dictionary found in zstd_dicts table!")
            else:
                dict_data = zstd.ZstdCompressionDict(dict_row[0])
                dctx = zstd.ZstdDecompressor(dict_data=dict_data)
        else:
            dctx = zstd.ZstdDecompressor()

        @db.register_function
        def decompress(s):
            """UDF for decompressing content."""
            return dctx.decompress(s)

        initialize_dest(dest)

        db.attach("decompress", dest)
        db.execute("""
            INSERT INTO decompress.entries (epoch_secs, nanos, level, content)
            SELECT epoch_secs, nanos, level, decompress(content) FROM entries
        """)

        db.execute("COMMIT")
    except Exception as e:
        print("Unexpected error:", e)
        raise


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
