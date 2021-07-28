///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.5.0
//DEPS com.tersesystems.blacklite:blacklite-reader:1.0.1
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

@Command(name = "compressdb", mixinStandardHelpOptions = true, version = "0.1",
  description = "use zstd compression")
class compressdb implements Callable<Integer> {

  @Parameters(paramLabel = "SOURCE", description = "the source database")
  String source;

  @Parameters(paramLabel = "DEST", description = "the destination database")
  String dest;

  public void run() {
    QueryBuilder qb = new QueryBuilder();

    try (Connection c = Database.createConnection(file)) {
      ResultConsumer consumer = new ResultConsumer() {
        @Override
        public void accept(ResultSet resultSet) {
          long epochSeconds = ts.getEpochSecond();
          int nanos = ts.getNano();
          int level = Integer.parseInt(values.get(levelField));
          String content = values.get(contentField);
          byte[] bytes = resultSet.getBytes("content");

          // Write to new database
        }
      };
      qb.execute(c, consumer);
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

}
