package com.tersesystems.blacklite;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import org.assertj.db.type.Source;
import org.assertj.db.type.Table;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DefaultEntryStoreTest {

  String url;
  EntryStore repo;

  @BeforeEach
  public void beforeEach() throws IOException, SQLException {
    Path tmpDir = Files.createTempDirectory("livedb");
    String filePath = tmpDir.resolve("livedb.db").toAbsolutePath().toString();
    url = "jdbc:sqlite:" + filePath;

    EntryStoreConfig config = new DefaultEntryStoreConfig();
    config.setUrl(url);
    repo = new DefaultEntryStore(config);
    repo.initialize();
  }

  @AfterEach
  public void afterEach() throws Exception {
    repo.close();
  }

  @Test
  public void testInsert() throws Exception {
    Instant instant = Instant.ofEpochMilli(1234);
    int level = 5000;
    byte[] content = "This is a test".getBytes();
    repo.insert(instant.getEpochSecond(), instant.getNano(), level, content);
    repo.executeBatch();
    repo.commit();

    Source source = new Source(url, null, null);
    Table table = new Table(source, "entries");
    org.assertj.db.api.Assertions.assertThat(table)
        .row(0)
        .value()
        .isEqualTo(1)
        .value()
        .isEqualTo(234000000)
        .value()
        .isEqualTo(level)
        .value()
        .isEqualTo("This is a test".getBytes());
  }

  @Test
  public void testMaxRowId() throws SQLException {
    Instant instant = Instant.now();
    int level = 5000;
    byte[] content = "This is a test".getBytes();
    repo.insert(instant.getEpochSecond(), instant.getNano(), level, content);
    repo.insert(instant.getEpochSecond(), instant.getNano(), level, content);
    repo.insert(instant.getEpochSecond(), instant.getNano(), level, content);
    repo.executeBatch();

    final long maxRowId = getMaxRow();
    assertThat(maxRowId).isEqualTo(3);
  }

  long getMaxRow() throws SQLException {
    final Connection connection = repo.getConnection();
    try (Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT MAX(_rowid_) FROM entries"); ) {
      rs.next();
      return rs.getLong(1);
    }
  }
}
