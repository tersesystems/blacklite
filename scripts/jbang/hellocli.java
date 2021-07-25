///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.5.0
//DEPS com.tersesystems.blacklite:blacklite-reader:1.0.1
//DEPS com.tersesystems.blacklite:blacklite-codec-zstd:1.0.1

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

import java.lang.Runnable;

@Command(name = "hellocli",
        mixinStandardHelpOptions = true,
        version = "hellocli 0.1",
        description = "hellocli made with jbang")
class hellocli implements Runnable {

    @Parameters(paramLabel = "FILE", description = "one or more files to read")
    File file;

    @Option(
        names = {"--charset"},
        paramLabel = "CHARSET",
        defaultValue = "utf8",
        description = "Charset (default: ${DEFAULT-VALUE})")
    Charset charset;

    @Option(
        names = {"-c", "--count"},
        paramLabel = "COUNT",
        description = "Return a count of entries")
    boolean count;

    public static void main(String... args) {
        final CommandLine commandLine = new CommandLine(new hellocli());
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

        if (count) {
          qb.addCount(count);
        }

        execute(qb);
      }

    public void execute(QueryBuilder qb) {
        try (Connection c = Database.createConnection(file)) {
            ResultConsumer consumer = createConsumer();
            qb.execute(c, consumer);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    ResultConsumer createConsumer() {
        return new PrintStreamResultConsumer(System.out, charset);
    }
}
