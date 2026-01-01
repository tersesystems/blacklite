#!/usr/bin/env python3
"""Generate test fixture databases for integration tests."""

import os
import json
import time
from sqlite_utils import Database
import zstandard as zstd

FIXTURES_DIR = os.path.dirname(__file__)


def create_uncompressed_db():
    """Create uncompressed test database."""
    db_path = os.path.join(FIXTURES_DIR, "uncompressed.db")
    if os.path.exists(db_path):
        os.unlink(db_path)

    db = Database(db_path)
    db.execute("""CREATE TABLE entries (
        epoch_secs LONG,
        nanos INTEGER,
        level INTEGER,
        content BLOB)
    """)

    # Insert 100 sample log entries
    epoch = int(time.time()) - 3600  # 1 hour ago
    for i in range(100):
        content = json.dumps({
            "message": f"Test log message number {i}",
            "logger": "test.logger",
            "thread": "main"
        })
        db.execute(
            "INSERT INTO entries (epoch_secs, nanos, level, content) VALUES (?, ?, ?, ?)",
            [epoch + i, i * 1000000, 20, content.encode('utf8')]
        )

    db.execute("COMMIT")
    print(f"Created {db_path}")
    return db_path


def create_compressed_db():
    """Create zstd compressed test database."""
    db_path = os.path.join(FIXTURES_DIR, "compressed.db")
    if os.path.exists(db_path):
        os.unlink(db_path)

    db = Database(db_path)
    db.execute("""CREATE TABLE entries (
        epoch_secs LONG,
        nanos INTEGER,
        level INTEGER,
        content BLOB)
    """)

    cctx = zstd.ZstdCompressor()

    # Insert 100 compressed sample log entries
    epoch = int(time.time()) - 3600
    for i in range(100):
        content = json.dumps({
            "message": f"Test log message number {i}",
            "logger": "test.logger",
            "thread": "main"
        })
        compressed = cctx.compress(content.encode('utf8'))
        db.execute(
            "INSERT INTO entries (epoch_secs, nanos, level, content) VALUES (?, ?, ?, ?)",
            [epoch + i, i * 1000000, 20, compressed]
        )

    db.execute("COMMIT")
    print(f"Created {db_path}")
    return db_path


def create_compressed_dict_db():
    """Create dictionary-compressed test database."""
    db_path = os.path.join(FIXTURES_DIR, "compressed_dict.db")
    if os.path.exists(db_path):
        os.unlink(db_path)

    db = Database(db_path)

    # Create tables
    db.execute("""CREATE TABLE entries (
        epoch_secs LONG,
        nanos INTEGER,
        level INTEGER,
        content BLOB)
    """)
    db.execute("""CREATE TABLE zstd_dicts (
        dict_id LONG NOT NULL PRIMARY KEY,
        dict_bytes BLOB NOT NULL)
    """)

    # Train dictionary from sample data
    samples = []
    for i in range(1000):
        content = json.dumps({
            "message": f"Test log message number {i}",
            "logger": "test.logger",
            "thread": "main"
        })
        samples.append(content.encode('utf8'))

    dict_data = zstd.train_dictionary(1024 * 10, samples)

    # Store dictionary
    db.execute(
        "INSERT INTO zstd_dicts (dict_id, dict_bytes) VALUES (?, ?)",
        [dict_data.dict_id(), dict_data.as_bytes()]
    )

    # Insert compressed entries using dictionary
    cctx = zstd.ZstdCompressor(dict_data=dict_data)
    epoch = int(time.time()) - 3600
    for i in range(100):
        content = json.dumps({
            "message": f"Test log message number {i}",
            "logger": "test.logger",
            "thread": "main"
        })
        compressed = cctx.compress(content.encode('utf8'))
        db.execute(
            "INSERT INTO entries (epoch_secs, nanos, level, content) VALUES (?, ?, ?, ?)",
            [epoch + i, i * 1000000, 20, compressed]
        )

    db.execute("COMMIT")
    print(f"Created {db_path}")
    return db_path


if __name__ == '__main__':
    print("Generating test fixtures...")
    create_uncompressed_db()
    create_compressed_db()
    create_compressed_dict_db()
    print("Done!")
