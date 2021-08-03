package com.tersesystems.blacklite.reader;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.sql.*;
import java.time.Instant;
import java.util.TimeZone;
import java.util.stream.Stream;

import com.tersesystems.blacklite.StatusReporter;
import com.tersesystems.blacklite.codec.Codec;
import com.tersesystems.blacklite.codec.zstd.ZStdDictSqliteRepository;
import com.tersesystems.blacklite.codec.zstd.ZstdDictCodec;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

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

  @Option(
      names = {"--binary"},
      paramLabel = "BINARY",
      description = "Renders content as raw BLOB binary")
  boolean binary;

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
    try (Connection c = Database.createConnection(file)) {
      Codec codec = defaultCodec(statusReporter);
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

      Stream<LogEntry> entryStream = qb.execute(c, verbose);
      entryStream
        .map(LogEntry::getContent)
        .map(content -> charset.decode(ByteBuffer.wrap(content)))
        .forEach(System.out::println);
    } catch (SQLException e) {
      statusReporter.addError("Cannot complete query", e);
    }
  }

  protected Codec defaultCodec(StatusReporter statusReporter) {
    final ZStdDictSqliteRepository dictRepo = new ZStdDictSqliteRepository();
    dictRepo.setFile(file.getAbsolutePath());
    dictRepo.initialize();
    final ZstdDictCodec zstdDictCodec = new ZstdDictCodec();
    zstdDictCodec.setRepository(dictRepo);
    zstdDictCodec.initialize(statusReporter);
    return zstdDictCodec;
  }

}
