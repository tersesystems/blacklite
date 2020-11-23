package com.tersesystems.blacklite.reader;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

public class BlackliteReaderTest {

  private String archiveFile = "/some/random/file";

  @Test
  public void testCount() {
    final TestBlackliteReader runner = new TestBlackliteReader();
    final CommandLine commandLine = new CommandLine(runner);
    commandLine.execute("-c", archiveFile);

    final QueryBuilder actual = runner.actualQueryBuilder;
    assertThat(actual.getCount()).isTrue();
  }

  @Test
  public void testBefore() {
    final TestBlackliteReader runner = new TestBlackliteReader();
    final CommandLine commandLine = new CommandLine(runner);
    commandLine.execute("-b", "five seconds ago", archiveFile);

    final QueryBuilder actual = runner.actualQueryBuilder;
    assertThat(actual.getBefore()).isBeforeOrEqualTo(Instant.now().minusSeconds(5));
  }

  @Test
  public void testEnd() {
    final TestBlackliteReader runner = new TestBlackliteReader();
    final CommandLine commandLine = new CommandLine(runner);
    commandLine.execute("--end", "0", archiveFile);

    final QueryBuilder actual = runner.actualQueryBuilder;
    assertThat(actual.getBefore()).isEqualTo(Instant.ofEpochSecond(0));
  }

  @Test
  public void testAfter() {
    final TestBlackliteReader runner = new TestBlackliteReader();
    final CommandLine commandLine = new CommandLine(runner);
    commandLine.execute("-a", "five seconds ago", archiveFile);

    final QueryBuilder actual = runner.actualQueryBuilder;
    assertThat(actual.getAfter()).isBeforeOrEqualTo(Instant.now().minusSeconds(5));
  }

  @Test
  public void testStartEpoch() {
    final TestBlackliteReader runner = new TestBlackliteReader();
    final CommandLine commandLine = new CommandLine(runner);
    commandLine.execute("--start", "0", archiveFile);

    final QueryBuilder actual = runner.actualQueryBuilder;
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

    final QueryBuilder actual = runner.actualQueryBuilder;
    assertThat(actual.getWhere()).isEqualTo("level > 9000");
  }

  @Test
  public void testSQL() {
    final TestBlackliteReader runner = new TestBlackliteReader();
    final CommandLine commandLine = new CommandLine(runner);
    commandLine.execute("-s", "0", "-e", "1000", "-w", "level > 9000", archiveFile);

    final QueryBuilder actual = runner.actualQueryBuilder;
    final String sql = actual.createSQL();
    assertThat(sql)
        .isEqualTo(
            "SELECT content FROM entries WHERE  timestamp < ?  AND  timestamp > ?  AND level > 9000");
  }

  @Test
  public void testCountSQL() {
    final TestBlackliteReader runner = new TestBlackliteReader();
    final CommandLine commandLine = new CommandLine(runner);
    commandLine.execute("-c", "-s", "0", "-e", "1000", "-w", "level > 9000", archiveFile);

    final QueryBuilder actual = runner.actualQueryBuilder;
    final String sql = actual.createSQL();
    assertThat(sql)
        .isEqualTo(
            "SELECT COUNT(*) FROM entries WHERE  timestamp < ?  AND  timestamp > ?  AND level > 9000");
  }

  static class TestBlackliteReader extends BlackliteReader {
    QueryBuilder actualQueryBuilder;

    @Override
    public void execute(QueryBuilder qb) {
      actualQueryBuilder = qb;
    }

    @Override
    ResultConsumer createConsumer() {
      return new DebugResultConsumer();
    }
  }
}
