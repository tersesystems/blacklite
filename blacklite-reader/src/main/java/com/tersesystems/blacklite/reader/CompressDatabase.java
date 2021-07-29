///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.5.0
//DEPS com.tersesystems.blacklite:blacklite-reader:1.0.1
//DEPS com.opencsv:opencsv:5.5.1

package com.tersesystems.blacklite.reader;

import com.tersesystems.blacklite.BlockingEntryWriter;
import com.tersesystems.blacklite.DefaultEntryStoreConfig;
import com.tersesystems.blacklite.EntryStoreConfig;
import com.tersesystems.blacklite.StatusReporter;
import com.tersesystems.blacklite.archive.Archiver;
import com.tersesystems.blacklite.archive.NoOpArchiver;
import com.tersesystems.blacklite.codec.Codec;
import com.tersesystems.blacklite.codec.zstd.ZStdCodec;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.stream.Stream;

@Command(name = "CompressDatabase", mixinStandardHelpOptions = true, version = "0.1",
  description = "use zstd compression")
public class CompressDatabase implements Runnable {

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
    QueryBuilder qb = new QueryBuilder();

    EntryStoreConfig config = new DefaultEntryStoreConfig();
    config.setFile(dest.toAbsolutePath().toString());
    config.setBatchInsertSize(1); // don't batch inserts here.

    Codec codec = new ZStdCodec();
    Archiver archiver = new NoOpArchiver();
    try (Connection c = Database.createConnection(dest.toFile())) {
      BlockingEntryWriter writer = new BlockingEntryWriter(StatusReporter.DEFAULT, config, archiver, "blacklite-writer");
      try {
        final Stream<LogEntry> entries = qb.execute(c, false);
        entries.forEach(
          e -> {
            byte[] compressed = codec.encode(e.getContent());
            writer.write(e.getEpochSecs(), e.getNanos(), e.getLevel(), compressed);
          });
      } finally {
        writer.close();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


}
