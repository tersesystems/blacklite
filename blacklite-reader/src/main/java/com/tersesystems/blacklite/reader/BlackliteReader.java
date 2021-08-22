package com.tersesystems.blacklite.reader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.TimeZone;
import java.util.stream.Stream;

import com.tersesystems.blacklite.StatusReporter;
import com.tersesystems.blacklite.codec.Codec;
import com.tersesystems.blacklite.codec.identity.IdentityCodec;
import com.tersesystems.blacklite.codec.zstd.*;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import static java.lang.String.format;

/**
 * Runs the command line application, using command line options to run parse out dates, set up a
 * database connection, and execute a QueryBuilder.
 */
@Command(
    name = "blacklite-reader",
    version = "1.0",
    description = "Outputs content from blacklite database")
public class BlackliteReader implements Runnable {

  @Parameters(paramLabel = "FILE", description = "one or more files to read")
  File file;

  @Option(
      names = {"--charset"},
      paramLabel = "CHARSET",
      defaultValue = "utf8",
      description = "Charset (default: ${DEFAULT-VALUE})")
  Charset charset;

  // after string and after TSE are mutually exclusive
  @ArgGroup AfterTime afterTime;

  static class AfterTime {
    @Option(
        names = {"-a", "--after"},
        paramLabel = "AFTER",
        description = "Only render entries after the given date")
    String afterString;

    @Option(
        names = {"-s", "--start"},
        paramLabel = "START",
        description = "Only render entries after the start of given epoch second")
    long afterEpoch;
  }

  @ArgGroup BeforeTime beforeTime;

  // before string and before TSE are mutually exclusive
  static class BeforeTime {
    @Option(
        names = {"-b", "--before"},
        paramLabel = "BEFORE",
        description = "Only render entries before the given date")
    String beforeString;

    @Option(
        names = {"-e", "--end"},
        paramLabel = "END",
        description = "Only render entries before the given epoch second")
    long beforeEpoch;
  }

  @Option(
      names = {"-v", "--verbose"},
      paramLabel = "VERBOSE",
      description = "Print verbose logging")
  boolean verbose;

  @Option(
      names = {"-w", "--where"},
      paramLabel = "WHERE",
      description = "Custom SQL WHERE clause")
  String whereString;

  @Option(
      names = {"-t", "--timezone"},
      defaultValue = "UTC",
      description = "Use the given timezone for before/after dates")
  TimeZone timezone;

  @Option(
    names = {"-d", "--dictionary"},
    description = "Use the given zstandard dictionary")
  String dictPath;

  @Option(
      names = {"-V", "--version"},
      versionHelp = true,
      description = "display version info")
  boolean versionInfoRequested;

  @Option(
      names = {"-h", "--help"},
      usageHelp = true,
      description = "display this help message")
  boolean usageHelpRequested;

  // this example implements Callable, so parsing, error handling and handling user
  // requests for usage help or version help can be done with one line of code.
  public static void main(String... args) {
    final CommandLine commandLine = new CommandLine(new BlackliteReader());
    if (commandLine.isUsageHelpRequested()) {
      commandLine.usage(System.out);
      return;
    } else if (commandLine.isVersionHelpRequested()) {
      commandLine.printVersionHelp(System.out);
      return;
    }
    System.exit(commandLine.execute(args));
  }

  public void run() {
    StatusReporter statusReporter = StatusReporter.DEFAULT;
    if (! file.exists()) {
      throw new IllegalArgumentException("File not found: " + file);
    }

    if (! file.canRead()) {
      throw new IllegalArgumentException("Cannot read file: " + file);
    }

    try (Connection c = Database.createConnection(file)) {
      Codec codec;
      if (isCompressed(c)) {
        ZstdDictRepository dictRepo = dictPath != null ?
          explicitDictionary(new File(dictPath)) :
          zstdDictFromDB(file);
        codec = zstdDictCodec(statusReporter, dictRepo);
      } else {
        codec = identityCodec();
      }
      QueryBuilder qb = createQueryBuilder(codec);

      Stream<LogEntry> entryStream = qb.execute(c, verbose);
      entryStream
        .map(LogEntry::getContent)
        .map(content -> charset.decode(ByteBuffer.wrap(content)))
        .forEach(System.out::println);
    } catch (SQLException e) {
      statusReporter.addError("Cannot complete query", e);
    }
  }

  protected QueryBuilder createQueryBuilder(Codec codec) {
    QueryBuilder qb = new QueryBuilder(codec);

    DateParser dateParser = new DateParser(timezone);
    if (beforeTime != null) {
      Instant before;
      final String beforeString = beforeTime.beforeString;
      if (beforeString != null) {
        before =
          dateParser
            .parse(beforeString)
            .orElseThrow(
              () -> new IllegalStateException("Cannot parse before string: " + beforeString));
      } else {
        before = Instant.ofEpochSecond(beforeTime.beforeEpoch);
      }
      qb.addBefore(before);
    }

    if (afterTime != null) {
      final String afterString = afterTime.afterString;
      Instant after;
      if (afterString != null) {
        after =
          dateParser
            .parse(afterString)
            .orElseThrow(
              () -> new IllegalStateException("Cannot parse after string: " + afterString));
      } else {
        after = Instant.ofEpochSecond(afterTime.afterEpoch);
      }
      qb.addAfter(after);
    }

    if (whereString != null) {
      qb.addWhere(whereString);
    }
    return qb;
  }

  protected boolean isCompressed(Connection c) throws SQLException {
    try (PreparedStatement ps = c.prepareStatement("SELECT content FROM entries LIMIT 1")) {
      try (ResultSet resultSet = ps.executeQuery()) {
        if (resultSet.next()) {
          final byte[] contentBytes = resultSet.getBytes(1);
          return ZStdUtils.isFrame(contentBytes);
        }
      }
    }
    return false;
  }

  protected ZstdDictRepository explicitDictionary(File dictFile) {
    if (! dictFile.exists()) {
      String msg = format("Dictionary %s does not exist!", dictFile);
      throw new IllegalStateException(msg);
    }

    if (! dictFile.canRead()) {
      String msg = format("Dictionary %s cannot be read!", dictFile);
      throw new IllegalStateException(msg);
    }

    if (isDatabase(dictFile)) {
      return zstdDictFromDB(dictFile);
    } else {
      try {
        if (ZStdUtils.isDictionary(dictFile)) {
          return zstdDictFromFile(dictFile);
        } else {
          String msg = format("Dictionary %s must be an sqlite db or a zstd dictionary!", dictFile);
          throw new IllegalStateException(msg);
        }
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  protected boolean isDatabase(File dictFile) {
    try (InputStream inputStream = Files.newInputStream(dictFile.toPath())) {
      // https://sqlite.org/fileformat.html
      final byte[] bytes = inputStream.readNBytes(15);
      final byte[] magicBytes = "SQLite format 3".getBytes(StandardCharsets.UTF_8);
      return Arrays.equals(bytes, magicBytes);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  protected Codec identityCodec() {
    return new IdentityCodec();
  }

  protected Codec zstdDictCodec(StatusReporter statusReporter, ZstdDictRepository dictRepo) {
    final ZStdDictCodec zstdDictCodec = new ZStdDictCodec();
    zstdDictCodec.setRepository(dictRepo);
    zstdDictCodec.initialize(statusReporter);
    return zstdDictCodec;
  }

  protected ZStdDictSqliteRepository zstdDictFromDB(File dbFile) {
    final ZStdDictSqliteRepository dictRepo = new ZStdDictSqliteRepository();
    dictRepo.setFile(dbFile.getAbsolutePath());
    dictRepo.initialize();
    return dictRepo;
  }

  protected ZstdDictFileRepository zstdDictFromFile(File dictFile) {
    final ZstdDictFileRepository dictRepo = new ZstdDictFileRepository();
    dictRepo.setFile(dictFile.getAbsolutePath());
    dictRepo.initialize();
    return dictRepo;
  }

}
