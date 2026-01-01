"""Database utility functions for blacklite SQLite databases."""

from sqlite_utils import Database
import zstandard as zstd
from typing import List

from .codecs import Codec, IdentityCodec, ZstandardCodec


def has_zstd_dictionary(db: Database) -> bool:
    """Check if database has a zstd_dicts table.

    Args:
        db: Database instance

    Returns:
        True if zstd_dicts table exists, False otherwise
    """
    return db["zstd_dicts"].exists()


def is_compressed(db: Database) -> bool:
    """Check if database entries are zstd compressed.

    Attempts to decompress the first entry's content to detect compression.

    Args:
        db: Database instance

    Returns:
        True if content is compressed, False otherwise
    """
    row = db.execute("SELECT content FROM entries LIMIT 1").fetchone()
    if row is None:
        return False

    content = row[0]
    try:
        zstd.decompress(content)
        return True
    except zstd.ZstdError:
        return False


def create_codec(db: Database) -> Codec:
    """Create appropriate codec based on database content.

    Detects whether database uses dictionary compression, standard compression,
    or no compression, and returns the appropriate codec.

    Args:
        db: Database instance

    Returns:
        Codec instance configured for the database
    """
    if has_zstd_dictionary(db):
        return _create_zstd_dict_codec(db)
    elif is_compressed(db):
        return _create_zstd_codec()
    else:
        return _create_identity_codec()


def _create_identity_codec() -> IdentityCodec:
    """Create identity (pass-through) codec."""
    return IdentityCodec()


def _create_zstd_codec() -> ZstandardCodec:
    """Create standard zstandard codec."""
    dctx = zstd.ZstdDecompressor()
    return ZstandardCodec(dctx)


def _create_zstd_dict_codec(db: Database) -> ZstandardCodec:
    """Create zstandard codec with dictionary from database.

    Args:
        db: Database instance with zstd_dicts table

    Returns:
        ZstandardCodec configured with dictionary
    """
    dict_row = db.execute("SELECT dict_bytes FROM zstd_dicts LIMIT 1").fetchone()
    dict_data = zstd.ZstdCompressionDict(dict_row[0])
    dctx = zstd.ZstdDecompressor(dict_data=dict_data)
    return ZstandardCodec(dctx)


def create_dictionary(db: Database, limit: int = 10000, dict_size: int = 10485760) -> zstd.ZstdCompressionDict:
    """Train a zstandard compression dictionary from database content.

    Args:
        db: Database instance
        limit: Maximum number of samples to use for training
        dict_size: Target dictionary size in bytes

    Returns:
        Trained ZstdCompressionDict
    """
    def content_bytes(row):
        content = row["content"]
        if isinstance(content, bytes):
            return content
        return bytes(content, encoding='utf8')

    samples = list(map(content_bytes,
                      db.query(f"SELECT content FROM entries LIMIT {limit}")))
    return zstd.train_dictionary(dict_size, samples)
