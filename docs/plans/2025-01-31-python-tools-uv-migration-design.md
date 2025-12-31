# Python Tools: uv Migration and Comprehensive Testing

**Date:** 2025-01-31
**Status:** Approved

## Overview

Migrate the Python scripts in `scripts/python/` to use `uv` for dependency management and add comprehensive test coverage (unit + integration tests). Keep the existing UDF approach for zstandard compression/decompression.

## Current State

Three Python scripts with embedded logic:
- `reader.py` - Reads and decompresses blacklite database entries
- `zstd-compress.py` - Compresses databases with optional dictionary training
- `zstd-decompress.py` - Decompresses databases

Current setup uses manual pip/virtualenv, `sqlite_utils`, `zstandard`, and `click` libraries.

## Design

### 1. uv Migration Strategy

**Approach:**
- Add `pyproject.toml` in `scripts/python/` for project definition
- Use uv for dependency management (replaces virtualenv + pip)
- Keep existing script structure for backward compatibility
- Add lockfile (`uv.lock`) for reproducible builds

**Dependencies:**
- `sqlite-utils` - database operations
- `zstandard` - compression
- `click` - CLI interface
- `pytest` (dev) - testing framework
- `pytest-cov` (dev) - coverage reporting

**Benefits:**
- Faster dependency resolution
- Automatic virtual environment management
- Reproducible builds with lockfile
- `uv run` for direct script execution

### 2. Comprehensive Testing Strategy

**Test Organization:**
```
tests/
├── unit/              # Unit tests for functions/classes
├── integration/       # End-to-end workflow tests
└── fixtures/          # Sample databases
```

**Unit Tests:**

1. **test_codecs.py** - Codec classes:
   - `IdentityCodec.decode()` returns unchanged input
   - `ZstandardCodec.decode()` decompresses correctly
   - Edge cases: empty input, corrupted data

2. **test_database.py** - Utility functions:
   - `create_codec()` detection logic
   - `has_zstd_dictionary()` table checks
   - `is_compressed()` detection
   - `create_dictionary()` training

3. **test_cli.py** - CLI argument parsing:
   - Click command validation
   - Defaults and options

**Integration Tests:**

1. **test_reader_integration.py**:
   - Read uncompressed database
   - Read zstd-compressed database
   - Read dict-compressed database
   - Error handling (missing DB, corrupt data)

2. **test_compress_integration.py**:
   - Compress without dictionary
   - Compress with --dict flag
   - Verify dictionary table creation
   - Round-trip: compress → decompress → verify

3. **test_decompress_integration.py**:
   - Decompress standard zstd
   - Decompress dict-based zstd
   - Round-trip verification

**Test Fixtures:**
- `fixtures/uncompressed.db` - ~100 sample log entries
- `fixtures/compressed.db` - Same data, zstd compressed
- `fixtures/compressed_dict.db` - Same data, zstd with dictionary
- `fixtures/generate_fixtures.py` - Regenerate test databases

### 3. Project Structure

**New structure:**
```
scripts/python/
├── pyproject.toml
├── uv.lock
├── README.md
├── blacklite_tools/           # Python package
│   ├── __init__.py
│   ├── codecs.py              # Codec classes
│   ├── database.py            # Database utilities
│   ├── cli_reader.py          # Reader CLI logic
│   ├── cli_compress.py        # Compress CLI logic
│   └── cli_decompress.py      # Decompress CLI logic
├── reader.py                  # Thin wrapper
├── zstd-compress.py           # Thin wrapper
├── zstd-decompress.py         # Thin wrapper
└── tests/
    ├── unit/
    ├── integration/
    └── fixtures/
```

**Refactoring Benefits:**
- Importable modules for unit testing
- Original scripts remain backward compatible
- Separation of concerns (CLI vs logic vs DB ops)
- Easier mocking for tests

### 4. Configuration Details

**pyproject.toml:**
```toml
[project]
name = "blacklite-tools"
version = "0.1.0"
requires-python = ">=3.8"
dependencies = ["sqlite-utils>=3.0", "zstandard>=0.22.0", "click>=8.0"]

[project.optional-dependencies]
dev = ["pytest>=7.0", "pytest-cov>=4.0"]

[project.scripts]
blacklite-reader = "blacklite_tools.cli_reader:main"
blacklite-compress = "blacklite_tools.cli_compress:main"
blacklite-decompress = "blacklite_tools.cli_decompress:main"
```

**README Updates:**
- Replace virtualenv instructions with uv
- `uv sync` for dependencies
- `uv run pytest` for testing
- `uv run reader.py` examples
- Document package structure

**Backward Compatibility:**
- Original scripts remain executable: `./reader.py`
- Also available as installed commands: `uv run blacklite-reader`

## Implementation Plan

1. Create `blacklite_tools/` package structure
2. Extract and refactor code into modules
3. Set up `pyproject.toml` and initialize uv
4. Write unit tests
5. Generate test fixtures
6. Write integration tests
7. Update README with uv instructions
8. Verify all tests pass

## Success Criteria

- All existing functionality works unchanged
- Comprehensive test coverage (unit + integration)
- uv-based dependency management
- Updated documentation
- All tests passing
