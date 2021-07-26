///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.5.0
//DEPS com.tersesystems.blacklite:blacklite-reader:1.0.1
//DEPS com.tersesystems.blacklite:blacklite-codec-zstd:1.0.1
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

@Command(name = "import", mixinStandardHelpOptions = true, version = "0.1",
        description = "import from JSON")
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

  DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_INSTANT;

  private final EntryStoreConfig config = new DefaultEntryStoreConfig();

  public static void main(String... args) {
    int exitCode = new CommandLine(new importcsv()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Integer call() throws Exception {
    config.setFile(file);
    Archiver archiver = new NoOpArchiver();
    String name = "blacklite";

    // StatusReporter statusReporter, EntryStoreConfig config, Archiver archiver, String name)
    BlockingEntryWriter writer = new BlockingEntryWriter(StatusReporter.DEFAULT, config, archiver, name);
    try {
      try (CSVReaderHeaderAware reader = getReader(csv)) {
        Map<String, String> values;

        while ((values = reader.readMap()) != null) {
          Instant ts = parseInstant(values.get(timestampField));
          long epochSeconds = ts.getEpochSecond();
          int nanos = ts.getNano();
          int level = Integer.parseInt(values.get(levelField));
          String content = values.get(contentField);

          writer.write(epochSeconds, nanos, level, content.getBytes(StandardCharsets.UTF_8));
        }
      }
    } finally {
      writer.close();
    }
    return 0;
  }

  public Instant parseInstant(String dateString) {
    return Instant.from(dateTimeFormatter.parse(dateString));
  }

  public CSVReaderHeaderAware getReader(String csv) throws IOException {
    return new CSVReaderHeaderAware(new FileReader(csv));
  }

}
