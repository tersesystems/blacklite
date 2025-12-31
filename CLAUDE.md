# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Blacklite is a high-performance SQLite appender for Java logging frameworks (Logback and Log4J 2) that provides queryable log buffering with optional compression and archiving. It uses an asynchronous queue-based architecture to achieve ~25-60 ns/op for queue insertion and ~2 μs/op for SQLite writes.

## Build System

This is a multi-module Gradle project using **Java 8** compatibility (JDK 8+ required).

### Common Commands

```bash
# Build all modules
./gradlew build

# Run all tests
./gradlew test

# Run tests for a specific module
./gradlew :blacklite-core:test
./gradlew :blacklite-logback:test

# Clean build artifacts
./gradlew clean

# Publish to local Maven repository
./gradlew publishToMavenLocal

# Format code with Spotless
./gradlew spotlessApply
```

### Module Structure

- **blacklite-api**: Core interfaces and contracts (`EntryStore`, `EntryWriter`, `Archiver`, `Codec`)
- **blacklite-core**: Core implementation with SQLite integration, async queue, and archiving logic
- **blacklite-logback**: Logback appender implementation
- **blacklite-log4j2**: Log4J 2 appender implementation
- **blacklite-codec-zstd**: ZStandard compression codec with dictionary training
- **blacklite-log4j2-codec-zstd**: Log4J 2 specific ZStandard codec wrappers
- **blacklite-reader**: Command-line tool for reading/querying Blacklite databases
- **blacklite-benchmarks**: JMH benchmarks

## Architecture

### Core Components

1. **Entry Pipeline**: Logging events → `EntryWriter` → Async Queue (JCTools MpscGrowableArrayQueue) → `EntryStore` → SQLite

2. **Entry Storage**:
   - Table schema: `(epoch_secs LONG, nanos INTEGER, level INTEGER, content BLOB)`
   - No indexes or autoincrement to maximize write speed
   - Uses batched inserts with manual commit control
   - Configured with memory mapping and WAL mode for performance

3. **Entry Writers**:
   - `AsyncEntryWriter`: Uses unbounded JCTools queue with single-threaded consumer
   - `BlockingEntryWriter`: Synchronous variant for testing
   - Consumer drains queue, batches inserts, commits on idle or when batch size reached

4. **Archiving System**:
   - `NoOpArchiver`: No archiving (testing/simple use cases)
   - `DeletingArchiver`: Deletes oldest entries when row limit reached
   - `RollingArchiver`: Moves old entries to separate archive databases with optional compression
   - Archiver runs periodically (every 1 second) during idle queue drain
   - Uses SQLite custom functions to apply codec during archive copy

5. **Compression**:
   - `IdentityCodec`: No compression
   - `ZStdCodec`: ZStandard compression with configurable levels
   - `ZStdDictCodec`: Dictionary-based ZStandard compression (4x space savings typical)
   - Dictionary training happens automatically from incoming content if not present

### Key Design Patterns

- **Single-threaded SQLite writes**: Only one connection writes at a time (SQLite constraint)
- **Queue buffering during archiving**: Queue accumulates entries while archive operation holds connection
- **Batch commits**: Default batch size is 10,000 rows, but commits happen on idle too
- **Custom SQLite functions**: Codec compression registered as SQLite function `encode()` for use in archive queries

### Threading Model

- Main application threads → `EntryWriter.write()` (non-blocking queue offer)
- `{name}-executor-thread` → Single consumer thread that:
  1. Drains queue
  2. Batches inserts
  3. Commits on idle or batch size
  4. Runs archiver every 1 second when idle
  5. Uses `LockSupport.parkNanos(1000)` for idle loop timing

### Configuration Notes

- **Logback**: Configure via `logback.xml` with `BlackliteAppender`
- **Log4J 2**: Configure via `log4j2.xml` with `Blacklite` appender
- Both support encoder/layout configuration for the `content` BLOB
- Always use shutdown hooks to drain queue before JVM exit

## Testing

- Uses JUnit Jupiter (JUnit 5)
- Test dependencies: AssertJ, Awaitility, AssertJ-DB
- Tests typically create temporary databases and verify queue draining, commits, and archiving

## Performance Considerations

- tmpfs filesystem recommended for non-persistent ring buffer use cases
- SQLite VACUUM runs on close (can be slow, uses row-visibility replay technique)
- Dictionary compression training requires sample data (configurable sample size)
- Bounded queue capacity (default 1,048,576) prevents OOM if filesystem fails

## Reader Tool

The `blacklite-reader` module produces a standalone JAR for querying databases:
- Supports natural language date ranges via Natty library
- Transparently handles compressed content
- Automatically loads dictionaries from database if present
- Can filter by time ranges or custom SQL WHERE clauses
