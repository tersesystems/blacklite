package com.tersesystems.blacklite.reader;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class BlackliteReaderTest {

    @Test
    public void testCompressedDict() {
      final Path rootDir = Paths.get(System.getProperty("user.dir"));
      final Path relative = Paths.get( "src", "test", "resources", "blacklite-zstd-dict.db");
      final Path path = rootDir.resolve(relative);
      String[] args = { path.toString() };
      final CommandLine commandLine = new CommandLine(new BlackliteReader());
      assertThat(commandLine.execute(args)).isEqualTo(0);
    }

  @Test
  public void testCompressedDictWithExternal() {
    final Path rootDir = Paths.get(System.getProperty("user.dir"));
    final Path dictPath = Paths.get( "src", "test", "resources", "zstd-dict");
    final Path relative = Paths.get( "src", "test", "resources", "blacklite-zstd-dict.db");
    final Path path = rootDir.resolve(relative);
    String[] args = { "-d", rootDir.resolve(dictPath).toString(), path.toString() };
    final CommandLine commandLine = new CommandLine(new BlackliteReader());
    assertThat(commandLine.execute(args)).isEqualTo(0);
  }

  @Test
  public void testPlain() {
    final Path rootDir = Paths.get(System.getProperty("user.dir"));
    final Path relative = Paths.get( "src", "test", "resources", "blacklite.db");
    final Path path = rootDir.resolve(relative);
    String[] args = { path.toString() };
    final CommandLine commandLine = new CommandLine(new BlackliteReader());
    assertThat(commandLine.execute(args)).isEqualTo(0);
  }

  @Test
  public void testCompressed() {
    final Path rootDir = Paths.get(System.getProperty("user.dir"));
    final Path relative = Paths.get( "src", "test", "resources", "blacklite-zstd.db");
    final Path path = rootDir.resolve(relative);
    String[] args = { path.toString() };
    final CommandLine commandLine = new CommandLine(new BlackliteReader());
    assertThat(commandLine.execute(args)).isEqualTo(0);
  }

}
