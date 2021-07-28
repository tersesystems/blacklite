/// usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.5.0
//DEPS com.tersesystems.blacklite:blacklite-reader:1.0.1
//DEPS com.tersesystems.blacklite:blacklite-core:1.0.1

import java.io.File;
import java.nio.charset.Charset;
import java.sql.*;
import java.time.Instant;
import java.util.TimeZone;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import com.tersesystems.blacklite.reader.*;
import com.tersesystems.blacklite.archiver.*;

import java.nio.*;

import java.lang.Runnable;

@Command(
    name = "reader",
    mixinStandardHelpOptions = true,
    version = "reader 0.1",
    description = "reader made with jbang")
class reader implements Runnable {

  @Parameters(paramLabel = "FILE", description = "one or more files to read")
  File file;

  @Option(
      names = {"--charset"},
      paramLabel = "CHARSET",
      defaultValue = "utf8",
      description = "Charset (default: ${DEFAULT-VALUE})")
  Charset charset;

  public static void main(String... args) {
    final CommandLine commandLine = new CommandLine(new reader());
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

    try (Connection c = Database.createConnection(file)) {
      qb.execute(c, false).forEach(entry ->
        System.out.println(charset.decode(ByteBuffer.wrap(entry.getContent())))
      );
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
