import pytest
import os
import tempfile
from click.testing import CliRunner
from sqlite_utils import Database
from blacklite_tools.cli_compress import main

FIXTURES_DIR = os.path.join(os.path.dirname(__file__), "..", "fixtures")


class TestCompressIntegration:
    def test_compress_without_dictionary(self, tmp_path):
        """Test basic compression without dictionary."""
        runner = CliRunner()
        source_db = os.path.join(FIXTURES_DIR, "uncompressed.db")
        dest_db = tmp_path / "compressed_output.db"

        result = runner.invoke(main, [source_db, str(dest_db)])

        assert result.exit_code == 0
        assert os.path.exists(dest_db)

        # Verify database has entries
        db = Database(dest_db)
        count = db.execute("SELECT COUNT(*) FROM entries").fetchone()[0]
        assert count == 100

        # Verify no dictionary table
        assert not db["zstd_dicts"].exists()

    def test_compress_with_dictionary(self, tmp_path):
        """Test compression with dictionary training."""
        runner = CliRunner()
        source_db = os.path.join(FIXTURES_DIR, "uncompressed.db")
        dest_db = tmp_path / "compressed_dict_output.db"

        result = runner.invoke(main, [source_db, str(dest_db), "--dict"])

        assert result.exit_code == 0
        assert os.path.exists(dest_db)

        # Verify database has entries
        db = Database(dest_db)
        count = db.execute("SELECT COUNT(*) FROM entries").fetchone()[0]
        assert count == 100

        # Verify dictionary table exists
        assert db["zstd_dicts"].exists()
        dict_count = db.execute("SELECT COUNT(*) FROM zstd_dicts").fetchone()[0]
        assert dict_count == 1

    def test_compress_output_is_smaller(self, tmp_path):
        """Test that compressed database is smaller than uncompressed."""
        runner = CliRunner()
        source_db = os.path.join(FIXTURES_DIR, "uncompressed.db")
        dest_db = tmp_path / "compressed_output.db"

        result = runner.invoke(main, [source_db, str(dest_db)])

        assert result.exit_code == 0

        # Compare total content size
        source = Database(source_db)
        dest = Database(dest_db)

        source_size = sum(len(row[0]) for row in
                         source.execute("SELECT content FROM entries"))
        dest_size = sum(len(row[0]) for row in
                       dest.execute("SELECT content FROM entries"))

        # Compressed should be smaller
        assert dest_size < source_size

    def test_compress_missing_source_raises_error(self, tmp_path):
        """Test that compressing a missing database raises an error."""
        runner = CliRunner()
        dest_db = tmp_path / "output.db"

        result = runner.invoke(main, ["/nonexistent/source.db", str(dest_db)])

        assert result.exit_code != 0
