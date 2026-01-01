# Python Tools for Blacklite

Python utilities for reading and managing Blacklite SQLite databases with zstandard compression support.

## Installation

This project uses [uv](https://docs.astral.sh/uv/) for dependency management. Install uv first:

```bash
# macOS/Linux
curl -LsSf https://astral.sh/uv/install.sh | sh

# Windows
powershell -c "irm https://astral.sh/uv/install.ps1 | iex"

# Or via pip
pip install uv
```

Then sync dependencies:

```bash
cd scripts/python
uv sync
```

For development (includes testing tools):

```bash
uv sync --dev
```

## Project Structure

```
scripts/python/
├── blacklite_tools/          # Core package
│   ├── codecs.py             # Codec classes (Identity, Zstandard)
│   ├── database.py           # Database utilities
│   ├── cli_reader.py         # Reader CLI
│   ├── cli_compress.py       # Compress CLI
│   └── cli_decompress.py     # Decompress CLI
├── reader.py                 # Reader script wrapper
├── zstd-compress.py          # Compress script wrapper
├── zstd-decompress.py        # Decompress script wrapper
└── tests/                    # Unit and integration tests
```

## Usage

All commands can be run with `uv run`:

### Reading Databases

Read and display entries from a blacklite database. Automatically detects and handles compression:

```bash
# Read from default path
uv run python reader.py

# Read from specific database
uv run python reader.py /path/to/blacklite.db

# Or use the installed command
uv run blacklite-reader /path/to/blacklite.db
```

The reader automatically detects:
- Uncompressed databases
- Zstd-compressed databases
- Dictionary-compressed databases

### Compressing Databases

Convert an uncompressed database to zstandard-compressed format:

```bash
# Basic compression
uv run python zstd-compress.py source.db compressed.db

# With dictionary training (better compression)
uv run python zstd-compress.py --dict source.db compressed_dict.db

# Or use the installed command
uv run blacklite-compress --dict source.db compressed.db
```

Dictionary compression typically achieves 4x better compression ratios for repetitive log data.

### Decompressing Databases

Convert a compressed database back to uncompressed format:

```bash
# Decompress (auto-detects dictionary)
uv run python zstd-decompress.py compressed.db uncompressed.db

# Or use the installed command
uv run blacklite-decompress compressed.db uncompressed.db
```

The decompressor automatically detects and uses dictionaries if present in the source database.

## Development

### Running Tests

Run all tests:

```bash
cd scripts/python
uv run pytest
```

Run specific test suites:

```bash
# Unit tests only
uv run pytest tests/unit/ -v

# Integration tests only
uv run pytest tests/integration/ -v

# With coverage
uv run pytest --cov=blacklite_tools --cov-report=html
```

### Regenerating Test Fixtures

Test fixtures are pre-generated and committed to the repository. To regenerate:

```bash
cd scripts/python
uv run python tests/fixtures/generate_fixtures.py
```

This creates three test databases:
- `uncompressed.db` - Plain SQLite database
- `compressed.db` - Zstd-compressed content
- `compressed_dict.db` - Dictionary-compressed content

### Code Organization

The codebase follows a layered architecture:

1. **Codec Layer** (`codecs.py`): Handles compression/decompression
   - `IdentityCodec`: No-op codec for uncompressed data
   - `ZstandardCodec`: Zstandard compression with optional dictionary

2. **Database Layer** (`database.py`): SQLite interactions and codec selection
   - `create_codec()`: Auto-detect and create appropriate codec
   - `create_dictionary()`: Train compression dictionaries from data
   - Helper functions for database introspection

3. **CLI Layer** (`cli_*.py`): Command-line interfaces
   - Click-based argument parsing
   - High-level workflow orchestration
   - User-facing error handling

4. **Script Wrappers**: Thin wrappers for backward compatibility
   - Original script names remain executable
   - Import and invoke CLI modules

## Technical Details

### Compression

- Uses [python-zstandard](https://github.com/indygreg/python-zstandard) library
- Dictionary training samples up to 10,000 entries by default
- Target dictionary size: 10 MB
- Compression happens via SQLite UDFs for efficient batch processing

### Database Schema

**Entries table:**
```sql
CREATE TABLE entries (
    epoch_secs LONG,
    nanos INTEGER,
    level INTEGER,
    content BLOB
)
```

**Dictionary table** (when using --dict):
```sql
CREATE TABLE zstd_dicts (
    dict_id LONG NOT NULL PRIMARY KEY,
    dict_bytes BLOB NOT NULL
)
```

### Architecture Notes

- **UDF Approach**: Compression/decompression uses SQLite User-Defined Functions
  - Registered via `db.register_function()`
  - Allows efficient batch processing in SQL queries
  - Better performance than row-by-row Python loops

- **Single-pass Processing**: Compress/decompress operations use `INSERT INTO ... SELECT` with UDFs
  - Leverages SQLite's query optimizer
  - Minimal memory footprint
  - Transactional integrity

## Dependencies

- **sqlite-utils** (≥3.0): High-level SQLite library
- **zstandard** (≥0.22.0): Zstandard compression
- **click** (≥8.0): CLI framework
- **pytest** (≥7.0, dev): Testing framework
- **pytest-cov** (≥4.0, dev): Coverage reporting

## License

Same as parent Blacklite project.
