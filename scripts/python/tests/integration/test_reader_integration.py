import pytest
import os
from click.testing import CliRunner
from blacklite_tools.cli_reader import main

FIXTURES_DIR = os.path.join(os.path.dirname(__file__), "..", "fixtures")


class TestReaderIntegration:
    def test_read_uncompressed_database(self):
        """Test reading from an uncompressed database."""
        runner = CliRunner()
        db_path = os.path.join(FIXTURES_DIR, "uncompressed.db")

        result = runner.invoke(main, [db_path])

        assert result.exit_code == 0
        assert "epoch_secs" in result.output
        assert "level" in result.output
        assert "message" in result.output
        assert "Test log message" in result.output

    def test_read_compressed_database(self):
        """Test reading from a zstd-compressed database."""
        runner = CliRunner()
        db_path = os.path.join(FIXTURES_DIR, "compressed.db")

        result = runner.invoke(main, [db_path])

        assert result.exit_code == 0
        assert "epoch_secs" in result.output
        assert "Test log message" in result.output

    def test_read_compressed_dict_database(self):
        """Test reading from a dictionary-compressed database."""
        runner = CliRunner()
        db_path = os.path.join(FIXTURES_DIR, "compressed_dict.db")

        result = runner.invoke(main, [db_path])

        assert result.exit_code == 0
        assert "epoch_secs" in result.output
        assert "Test log message" in result.output

    def test_read_missing_database_raises_error(self):
        """Test that reading a missing database raises an error."""
        runner = CliRunner()

        result = runner.invoke(main, ["/nonexistent/path.db"])

        assert result.exit_code != 0
        assert "No database found" in str(result.exception) or "No database found" in result.output
