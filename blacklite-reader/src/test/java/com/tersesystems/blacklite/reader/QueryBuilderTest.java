package com.tersesystems.blacklite.reader;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;

import com.tersesystems.blacklite.DefaultEntryStore;
import com.tersesystems.blacklite.DefaultEntryStoreConfig;
import com.tersesystems.blacklite.EntryStore;
import com.tersesystems.blacklite.EntryStoreConfig;
import com.tersesystems.blacklite.codec.identity.IdentityCodec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

public class QueryBuilderTest {

  Path archivePath = Paths.get(System.getProperty("java.io.tmpdir"), "blacklite.db");
  String archiveFile = archivePath.toString();

  @BeforeEach
  public void initialize() throws Exception {
    EntryStoreConfig config = new DefaultEntryStoreConfig();
    config.setFile(archivePath.toString());
    config.setBatchInsertSize(1); // don't batch inserts here.
    EntryStore entryStore = new DefaultEntryStore(config);
    try (entryStore) {
      entryStore.initialize();
    }
  }

  @AfterEach
  public void cleanUp() throws IOException {
    Files.deleteIfExists(archivePath);
  }

  @Test
  public void testBefore() {
    final TestBlackliteReader runner = new TestBlackliteReader();
    final CommandLine commandLine = new CommandLine(runner);
    commandLine.execute("-b", "five seconds ago", archiveFile);

    final QueryBuilder actual = runner.createQueryBuilder(new IdentityCodec());
    assertThat(actual.getBefore()).isBeforeOrEqualTo(Instant.now().minusSeconds(5));
  }

  @Test
  public void testEnd() {
    final TestBlackliteReader runner = new TestBlackliteReader();
    final CommandLine commandLine = new CommandLine(runner);
    commandLine.execute("--end", "0", archiveFile);

    final QueryBuilder actual = runner.createQueryBuilder(new IdentityCodec());
    assertThat(actual.getBefore()).isEqualTo(Instant.ofEpochSecond(0));
  }

  @Test
  public void testAfter() {
    final TestBlackliteReader runner = new TestBlackliteReader();
    final CommandLine commandLine = new CommandLine(runner);
    commandLine.execute("-a", "five seconds ago", archiveFile);

    final QueryBuilder actual = runner.createQueryBuilder(new IdentityCodec());
    assertThat(actual.getAfter()).isBeforeOrEqualTo(Instant.now().minusSeconds(5));
  }

  @Test
  public void testStartEpoch() {
    final TestBlackliteReader runner = new TestBlackliteReader();
    final CommandLine commandLine = new CommandLine(runner);
    commandLine.execute("--start", "0", archiveFile);

    final QueryBuilder actual = runner.createQueryBuilder(new IdentityCodec());
    assertThat(actual.getAfter()).isEqualTo(Instant.ofEpochSecond(0));
  }

  @Test
  public void testExclusiveAfter() {
    final TestBlackliteReader runner = new TestBlackliteReader();
    final CommandLine commandLine = new CommandLine(runner);
    int ret = commandLine.execute("-a", "five seconds ago", "--start", "0", archiveFile);

    assertThat(ret).isEqualTo(2);
  }

  @Test
  public void testExclusiveBefore() {
    final TestBlackliteReader runner = new TestBlackliteReader();
    final CommandLine commandLine = new CommandLine(runner);
    int ret = commandLine.execute("-b", "five seconds ago", "--end", "0", archiveFile);

    assertThat(ret).isEqualTo(2);
  }

  @Test
  public void testRangeEpochSeconds() {
    final TestBlackliteReader runner = new TestBlackliteReader();
    final CommandLine commandLine = new CommandLine(runner);
    int ret = commandLine.execute("--start", "0", "--end", "100000", archiveFile);

    assertThat(ret).isEqualTo(0);
  }

  @Test
  public void testRangeRelative() {
    final TestBlackliteReader runner = new TestBlackliteReader();
    final CommandLine commandLine = new CommandLine(runner);
    int ret =
        commandLine.execute(
            "--after", "10 minutes ago", "--before", "five minutes ago", archiveFile);

    assertThat(ret).isEqualTo(0);
  }

  @Test
  public void testWhere() {
    final TestBlackliteReader runner = new TestBlackliteReader();
    final CommandLine commandLine = new CommandLine(runner);
    commandLine.execute("-w", "level > 9000", archiveFile);

    final QueryBuilder actual = runner.createQueryBuilder(new IdentityCodec());
    assertThat(actual.getWhere()).isEqualTo("level > 9000");
  }

  @Test
  public void testSQL() {
    final TestBlackliteReader runner = new TestBlackliteReader();
    final CommandLine commandLine = new CommandLine(runner);
    commandLine.execute("-s", "0", "-e", "1000", "-w", "level > 9000", archiveFile);

    final QueryBuilder actual = runner.createQueryBuilder(new IdentityCodec());
    final String sql = actual.createSQL();
    assertThat(sql)
        .isEqualTo(
            "SELECT epoch_secs, nanos, level, decode(content) FROM entries WHERE epoch_secs < ?  AND epoch_secs > ?  AND level > 9000");
  }

  static class TestBlackliteReader extends BlackliteReader {

  }
}
