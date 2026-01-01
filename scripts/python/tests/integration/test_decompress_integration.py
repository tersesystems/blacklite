import pytest
import os
import json
from click.testing import CliRunner
from sqlite_utils import Database
from blacklite_tools.cli_decompress import main

FIXTURES_DIR = os.path.join(os.path.dirname(__file__), "..", "fixtures")


class TestDecompressIntegration:
    def test_decompress_standard_compressed_database(self, tmp_path):
        """Test decompressing a standard zstd-compressed database."""
        runner = CliRunner()
        source_db = os.path.join(FIXTURES_DIR, "compressed.db")
        dest_db = tmp_path / "decompressed_output.db"

        result = runner.invoke(main, [source_db, str(dest_db)])

        assert result.exit_code == 0
        assert os.path.exists(dest_db)

        # Verify database has entries
        db = Database(dest_db)
        count = db.execute("SELECT COUNT(*) FROM entries").fetchone()[0]
        assert count == 100

        # Verify content is decompressed (can parse as JSON)
        row = db.execute("SELECT content FROM entries LIMIT 1").fetchone()
        content = json.loads(row[0])
        assert "message" in content
        assert "Test log message" in content["message"]

    def test_decompress_dictionary_compressed_database(self, tmp_path):
        """Test decompressing a dictionary-compressed database."""
        runner = CliRunner()
        source_db = os.path.join(FIXTURES_DIR, "compressed_dict.db")
        dest_db = tmp_path / "decompressed_dict_output.db"

        result = runner.invoke(main, [source_db, str(dest_db)])

        assert result.exit_code == 0
        assert os.path.exists(dest_db)

        # Verify database has entries
        db = Database(dest_db)
        count = db.execute("SELECT COUNT(*) FROM entries").fetchone()[0]
        assert count == 100

        # Verify content is decompressed
        row = db.execute("SELECT content FROM entries LIMIT 1").fetchone()
        content = json.loads(row[0])
        assert "message" in content

    def test_decompress_output_is_larger(self, tmp_path):
        """Test that decompressed database is larger than compressed."""
        runner = CliRunner()
        source_db = os.path.join(FIXTURES_DIR, "compressed.db")
        dest_db = tmp_path / "decompressed_output.db"

        result = runner.invoke(main, [source_db, str(dest_db)])

        assert result.exit_code == 0

        # Compare total content size
        source = Database(source_db)
        dest = Database(dest_db)

        source_size = sum(len(row[0]) for row in
                         source.execute("SELECT content FROM entries"))
        dest_size = sum(len(row[0]) for row in
                       dest.execute("SELECT content FROM entries"))

        # Decompressed should be larger
        assert dest_size > source_size

    def test_decompress_missing_source_raises_error(self, tmp_path):
        """Test that decompressing a missing database raises an error."""
        runner = CliRunner()
        dest_db = tmp_path / "output.db"

        result = runner.invoke(main, ["/nonexistent/source.db", str(dest_db)])

        assert result.exit_code != 0

    def test_round_trip_compress_decompress(self, tmp_path):
        """Test that compress -> decompress preserves data."""
        from blacklite_tools.cli_compress import main as compress_main

        runner = CliRunner()
        source_db = os.path.join(FIXTURES_DIR, "uncompressed.db")
        compressed_db = tmp_path / "compressed.db"
        decompressed_db = tmp_path / "decompressed.db"

        # Compress
        result = runner.invoke(compress_main, [source_db, str(compressed_db)])
        assert result.exit_code == 0

        # Decompress
        result = runner.invoke(main, [str(compressed_db), str(decompressed_db)])
        assert result.exit_code == 0

        # Compare original and round-trip data
        source = Database(source_db)
        dest = Database(decompressed_db)

        source_rows = list(source.execute("SELECT epoch_secs, nanos, level, content FROM entries ORDER BY epoch_secs"))
        dest_rows = list(dest.execute("SELECT epoch_secs, nanos, level, content FROM entries ORDER BY epoch_secs"))

        assert len(source_rows) == len(dest_rows)
        assert source_rows == dest_rows
