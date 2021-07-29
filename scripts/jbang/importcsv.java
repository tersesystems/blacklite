///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.5.0
//DEPS com.tersesystems.blacklite:blacklite-reader:1.0.1
//DEPS com.tersesystems.blacklite:blacklite-core:1.0.1
//DEPS com.opencsv:opencsv:5.5.1

import com.tersesystems.blacklite.*;
import com.tersesystems.blacklite.reader.*;
import com.tersesystems.blacklite.archive.*;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import com.opencsv.*;
import com.opencsv.exceptions.CsvValidationException;

import java.time.Instant;
import java.time.format.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.*;

import java.util.concurrent.Callable;

@Command(name = "importcsv", mixinStandardHelpOptions = true, version = "0.1",
        description = "import from CSV")
class importcsv implements Callable<Integer> {

  @Parameters(paramLabel = "FILE", description = "the database to import into")
  String file;

  @Parameters(paramLabel = "CSV", description = "CSV files to import")
  String csv;

  @Parameters(paramLabel = "level", description = "Level Field", defaultValue = "level")
  String levelField;

  @Parameters(paramLabel = "timestamp", description = "Timestamp Field", defaultValue = "timestamp_utc")
  String timestampField;

  @Parameters(paramLabel = "content", description = "Content Field", defaultValue = "content")
  String contentField;

  public static void main(String... args) {
    int exitCode = new CommandLine(new importcsv()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Integer call() throws Exception {
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_INSTANT;

    EntryStoreConfig config = new DefaultEntryStoreConfig();
    config.setFile(file);
    config.setBatchInsertSize(1); // don't batch inserts here.

    Archiver archiver = new NoOpArchiver();
    BlockingEntryWriter writer = new BlockingEntryWriter(StatusReporter.DEFAULT, config, archiver, "blacklite-writer");
    try {
      try (CSVReaderHeaderAware reader = new CSVReaderHeaderAware(new FileReader(csv))) {
        Map<String, String> values;

        while ((values = reader.readMap()) != null) {
          Instant ts = Instant.from(dateTimeFormatter.parse(values.get(timestampField)));
          long epochSeconds = ts.getEpochSecond();
          int nanos = ts.getNano();
          int level = Integer.parseInt(values.get(levelField));
          String content = values.get(contentField);
          byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

          writer.write(epochSeconds, nanos, level, bytes);
        }
      }
    } finally {
      writer.close();
    }
    return 0;
  }

}
