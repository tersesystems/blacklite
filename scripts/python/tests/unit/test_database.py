import pytest
from sqlite_utils import Database
from blacklite_tools.database import has_zstd_dictionary, is_compressed, create_codec, create_dictionary
from blacklite_tools.codecs import IdentityCodec, ZstandardCodec
import tempfile
import os
import zstandard as zstd


class TestDatabaseUtilities:
    def test_has_zstd_dictionary_returns_true_when_table_exists(self):
        with tempfile.NamedTemporaryFile(delete=False, suffix='.db') as f:
            db_path = f.name

        try:
            db = Database(db_path)
            db.execute("CREATE TABLE zstd_dicts (dict_id LONG, dict_bytes BLOB)")
            assert has_zstd_dictionary(db) is True
        finally:
            os.unlink(db_path)

    def test_has_zstd_dictionary_returns_false_when_table_missing(self):
        with tempfile.NamedTemporaryFile(delete=False, suffix='.db') as f:
            db_path = f.name

        try:
            db = Database(db_path)
            assert has_zstd_dictionary(db) is False
        finally:
            os.unlink(db_path)

    def test_is_compressed_returns_true_for_compressed_data(self):
        with tempfile.NamedTemporaryFile(delete=False, suffix='.db') as f:
            db_path = f.name

        try:
            db = Database(db_path)
            db.execute("CREATE TABLE entries (content BLOB)")

            # Insert compressed data
            cctx = zstd.ZstdCompressor()
            compressed = cctx.compress(b"test content")
            db.execute("INSERT INTO entries (content) VALUES (?)", [compressed])

            assert is_compressed(db) is True
        finally:
            os.unlink(db_path)

    def test_is_compressed_returns_false_for_uncompressed_data(self):
        with tempfile.NamedTemporaryFile(delete=False, suffix='.db') as f:
            db_path = f.name

        try:
            db = Database(db_path)
            db.execute("CREATE TABLE entries (content BLOB)")
            db.execute("INSERT INTO entries (content) VALUES (?)", [b"plain text"])

            assert is_compressed(db) is False
        finally:
            os.unlink(db_path)

    def test_is_compressed_returns_false_for_empty_table(self):
        with tempfile.NamedTemporaryFile(delete=False, suffix='.db') as f:
            db_path = f.name

        try:
            db = Database(db_path)
            db.execute("CREATE TABLE entries (content BLOB)")

            assert is_compressed(db) is False
        finally:
            os.unlink(db_path)

    def test_create_codec_returns_identity_for_uncompressed(self):
        with tempfile.NamedTemporaryFile(delete=False, suffix='.db') as f:
            db_path = f.name

        try:
            db = Database(db_path)
            db.execute("CREATE TABLE entries (content BLOB)")
            db.execute("INSERT INTO entries (content) VALUES (?)", [b"plain"])

            codec = create_codec(db)
            assert isinstance(codec, IdentityCodec)
        finally:
            os.unlink(db_path)

    def test_create_codec_returns_zstd_for_compressed(self):
        with tempfile.NamedTemporaryFile(delete=False, suffix='.db') as f:
            db_path = f.name

        try:
            db = Database(db_path)
            db.execute("CREATE TABLE entries (content BLOB)")

            cctx = zstd.ZstdCompressor()
            compressed = cctx.compress(b"test")
            db.execute("INSERT INTO entries (content) VALUES (?)", [compressed])

            codec = create_codec(db)
            assert isinstance(codec, ZstandardCodec)
        finally:
            os.unlink(db_path)

    def test_create_codec_returns_zstd_with_dict_when_dict_exists(self):
        with tempfile.NamedTemporaryFile(delete=False, suffix='.db') as f:
            db_path = f.name

        try:
            db = Database(db_path)
            db.execute("CREATE TABLE entries (content BLOB)")
            db.execute("CREATE TABLE zstd_dicts (dict_id LONG, dict_bytes BLOB)")

            # Create and store dictionary
            samples = [b"test " * 100 for _ in range(100)]
            dict_data = zstd.train_dictionary(1024, samples)
            db.execute("INSERT INTO zstd_dicts VALUES (?, ?)",
                      [dict_data.dict_id(), dict_data.as_bytes()])

            # Insert compressed data with dictionary
            cctx = zstd.ZstdCompressor(dict_data=dict_data)
            compressed = cctx.compress(b"test data")
            db.execute("INSERT INTO entries (content) VALUES (?)", [compressed])

            codec = create_codec(db)
            assert isinstance(codec, ZstandardCodec)
            # Verify it can decode dict-compressed content
            assert codec.decode(compressed) == b"test data"
        finally:
            os.unlink(db_path)

    def test_create_dictionary_trains_from_database_samples(self):
        with tempfile.NamedTemporaryFile(delete=False, suffix='.db') as f:
            db_path = f.name

        try:
            db = Database(db_path)
            db.execute("CREATE TABLE entries (content TEXT)")

            # Insert sample data
            for i in range(100):
                db.execute("INSERT INTO entries (content) VALUES (?)",
                          [f"Sample log entry number {i} with repeated patterns"])

            dict_data = create_dictionary(db)

            # Verify it's a valid ZstdCompressionDict
            assert isinstance(dict_data, zstd.ZstdCompressionDict)
            assert dict_data.dict_id() > 0

            # Verify it can be used for compression
            cctx = zstd.ZstdCompressor(dict_data=dict_data)
            # Use a longer string to ensure compression benefit
            test_string = b"Sample log entry number 42 with repeated patterns" * 10
            compressed = cctx.compress(test_string)
            assert len(compressed) < len(test_string)
        finally:
            os.unlink(db_path)
