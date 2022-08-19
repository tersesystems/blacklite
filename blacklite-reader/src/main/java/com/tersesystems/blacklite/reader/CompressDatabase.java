///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.5.0
//DEPS com.tersesystems.blacklite:blacklite-reader:1.0.1
//DEPS com.opencsv:opencsv:5.5.1

package com.tersesystems.blacklite.reader;

import com.tersesystems.blacklite.*;
import com.tersesystems.blacklite.codec.Codec;
import com.tersesystems.blacklite.codec.zstd.ZStdDictSqliteRepository;
import com.tersesystems.blacklite.codec.zstd.ZStdUtils;
import com.tersesystems.blacklite.codec.zstd.ZStdDictCodec;
import org.sqlite.Function;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Command(name = "CompressDatabase", mixinStandardHelpOptions = true, version = "0.1",
  description = "use zstd compression")
public class CompressDatabase implements Runnable {

  StatusReporter reporter = StatusReporter.DEFAULT;

  @Parameters(paramLabel = "SOURCE", description = "the source database")
  Path source;

  @Parameters(paramLabel = "DEST", description = "the destination database")
  Path dest;

  public static void main(String... args) {
    final CommandLine commandLine = new CommandLine(new CompressDatabase());
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
    try {
      initializeCompressDatabase();

      try (Connection c = Database.createConnection(source.toFile())) {
        ZStdDictSqliteRepository repo = createRepository(c);

        ZStdDictCodec codec = new ZStdDictCodec();
        codec.setRepository(repo);
        codec.initialize(reporter);
        registerCodecFunction(c, codec);

        try (Statement statement = c.createStatement()) {
          String attachName = "blacklite_zstd";
          final String attachStatement = getAttachStatement(dest.toAbsolutePath(), attachName);
          System.out.println("attachStatement = " + attachStatement);
          statement.executeUpdate(attachStatement);

          String sql = getBulkInsertStatement(attachName);
          statement.executeUpdate(sql);
        }
      }
    } catch (Exception e) {
      reporter.addError("Could not compress database", e);
    }
  }

  private ZStdDictSqliteRepository createRepository(Connection c) throws SQLException {
    int sampleSize = 10000;
    int dictSize = 10485760;

    final byte[] dictBytes;
    try (Statement stmt = c.createStatement()) {
      try (ResultSet rs = stmt.executeQuery("SELECT epoch_secs, nanos, level, content FROM entries")) {
        LogEntrySpliterator logEntrySpliterator = new LogEntrySpliterator(rs);
        Stream<LogEntry> stream = StreamSupport.stream(logEntrySpliterator, false);
        dictBytes = ZStdUtils.trainDictionary(sampleSize, dictSize, stream.map(LogEntry::getContent));
      }
    }
    final ZStdDictSqliteRepository repo = new ZStdDictSqliteRepository();
    repo.setFile(dest.toAbsolutePath().toString());
    repo.initialize();
    repo.save(dictBytes);

    return repo;
  }

  private void initializeCompressDatabase() throws Exception {
    EntryStoreConfig config = new DefaultEntryStoreConfig();
    config.setFile(dest.toAbsolutePath().toString());
    config.setBatchInsertSize(1); // don't batch inserts here.
    try (EntryStore entryStore = new DefaultEntryStore(config)) {
      entryStore.initialize();
    }

    try (ZStdDictSqliteRepository repository = new ZStdDictSqliteRepository()) {
      repository.setFile(dest.toFile().getAbsolutePath());
      repository.initialize();
    }
  }

  private String getBulkInsertStatement(String attachName) {
    String sql = "insert into %s.entries (epoch_secs, nanos, level, content)\n" +
      "select epoch_secs, nanos, level, encode(content) from entries";

    return String.format(sql, attachName);
  }

  private String getAttachStatement(Path destPath, String attachName) {
    return String.format("ATTACH DATABASE \"%s\" AS %s", destPath.toString(), attachName);
  }

  private void registerCodecFunction(Connection c, Codec codec) throws SQLException {
    Function codecFunction =
      new Function() {
        @Override
        protected void xFunc() throws SQLException {
          result(codec.encode(value_blob(0)));
        }
      };
    // Register the codec as a custom SQLite function
    Function.create(c, "encode", codecFunction);
  }

}
