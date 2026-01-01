#!/usr/bin/env python3
"""CLI tool for reading blacklite database entries."""

import os.path
import json
import time
import click
from sqlite_utils import Database

from .database import create_codec


@click.command()
@click.argument("dbpath", default="./tests/fixtures/uncompressed.db")
def main(dbpath):
    """Read data from blacklite database using appropriate codec.

    Args:
        dbpath: Path to the blacklite database file
    """
    if not os.path.exists(dbpath):
        raise IOError(f'No database found at {dbpath}')

    db = Database(dbpath)
    codec = create_codec(db)

    epoch_time = int(time.time())
    for row in db["entries"].rows_where("epoch_secs < ? limit 1", [epoch_time]):
        print_row(codec, row)


def print_row(codec, row):
    """Print a database row with decoded content.

    Args:
        codec: Codec instance for decoding content
        row: Database row dict
    """
    epoch_secs = row['epoch_secs']
    level = row['level']
    content = row['content']
    json_content = json.loads(codec.decode(content))
    print("epoch_secs =", epoch_secs, "level =", level, "message =", json_content['message'])


if __name__ == '__main__':
    main()
