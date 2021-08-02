#!/usr/bin/env python3

from sqlite_utils import Database
import time
import json
import os.path
import click
import zstandard as zstd
from typing import ByteString


@click.command()
@click.argument("dbpath", default="../data/blacklite.db")
def main(dbpath):
    """Reads data from blacklite database using codec."""
    if not os.path.exists(dbpath):
        raise IOError(f'"No database found at {dbpath}')

    db = Database(dbpath)
    codec = create_codec(db)

    epoch_time = int(time.time())
    for row in db["entries"].rows_where("epoch_secs < ? limit 1", [epoch_time]):
        print_row(codec, row)


def print_row(codec, row):
    epoch_secs = row['epoch_secs']
    level = row['level']
    content = row['content']
    json_content = json.loads(codec.decode(content))
    print("epoch_secs = ", epoch_secs, "level = ", level, "message = ", json_content['message'])


def create_codec(db: Database):
    if has_zstd_dictionary(db):
        return create_zstd_dict_codec(db)
    elif is_compressed(db):
        return create_zstd_codec()
    else:
        return create_identity_codec()


def create_identity_codec():
    return IdentityCodec()


def create_zstd_codec():
    dctx = zstd.ZstdDecompressor()
    return ZstandardCodec(dctx)


def create_zstd_dict_codec(db: Database):
    dict_row = db.execute("select dict_bytes from zstd_dicts LIMIT 1").fetchone()
    dict = zstd.ZstdCompressionDict(dict_row[0])
    dctx = zstd.ZstdDecompressor(dict_data = dict)
    return ZstandardCodec(dctx)


def has_zstd_dictionary(db: Database):
    if db["zstd_dicts"].exists():
        return True
    else:
        return False


def is_compressed(db: Database):
    row = db.execute("SELECT content from entries LIMIT 1").fetchone()
    if row is None:
        return False
    else:
        content = row[0]
        try:
            zstd.decompress(content)
            return True
        except zstd.ZstdError as e:
            return False



class Codec:
    def decode(self, bytes: ByteString): ...


class IdentityCodec(Codec):

    def __init__(self):
        pass

    def decode(self, b: ByteString):
        return b


class ZstandardCodec(Codec):
    dctx = None

    def __init__(self, dctx: zstd.ZstdDecompressor):
        self.dctx = dctx

    def decode(self, b: ByteString):
        return self.dctx.decompress(b)


if __name__ == '__main__':
    main()

