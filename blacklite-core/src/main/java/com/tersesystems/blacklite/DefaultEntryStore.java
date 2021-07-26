package com.tersesystems.blacklite;

import static java.util.Objects.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import org.sqlite.JDBC;

/**
 * The live repository covers access to an SQLite database that has new entries that have not been
 * encoded yet. As the database fills up, the oldest entries are removed after they are successfully
 * archived.
 *
 * <p>This provides a buffer to make quick queries with plain SQL, and gives some flexibility to
 * ensure that the sqlite database is as fast with writes as possible, i.e. memory mapped at the
 * very least, potentially with a WAL mapped to tmpfs or even no journaling at all, with a database
 * small enough to fit entirely in memory.
 */
public class DefaultEntryStore implements EntryStore {

  private final Connection conn;
  private final String url;
  private final Statements statements;
  private PreparedStatement insertStatement;

  private long totalInserts;
  private long totalBytes;

  public DefaultEntryStore(EntryStoreConfig config) throws SQLException {
    String fileString = requireNonNull(config.getFile(), "Null file");
    Path path = Paths.get(fileString);
    this.url = "jdbc:sqlite:" + path.toAbsolutePath();
    if (!JDBC.isValidURL(this.url)) {
      throw new IllegalArgumentException("Invalid URL " + config.getFile());
    }
    createParentDirectories(path);
    this.conn = JDBC.createConnection(this.url, config.getProperties());
    statements = Statements.instance();
  }

  private void createParentDirectories(Path path) throws SQLException {
    final Path parentDir = path.getParent();
    if (parentDir != null && !Files.exists(parentDir)) {
      try {
        Files.createDirectories(parentDir);
      } catch (IOException e) {
        throw new SQLException(e);
      }
    }
  }

  @Override
  public void initialize() throws SQLException {
    try (Statement stmt = conn.createStatement()) {
      stmt.execute(statements.createEntriesTable());
      stmt.execute(statements.createEntriesView());
    }
    this.insertStatement = conn.prepareStatement(statements.insert());

    // Set to transaction mode after setting up DDL.
    conn.setAutoCommit(false);
  }

  @Override
  public String getUrl() {
    return url;
  }

  @Override
  public void insert(long epochSecond, int nanos, int level, byte[] content) throws SQLException {
    int adder = 1;
    insertStatement.setLong(adder++, epochSecond);
    insertStatement.setInt(adder++, nanos);
    insertStatement.setLong(adder++, level);
    insertStatement.setBytes(adder, content);
    totalBytes = totalBytes + content.length;
    insertStatement.addBatch();
    totalInserts++;
  }

  @Override
  public void executeBatch() throws SQLException {
    insertStatement.executeBatch();
  }

  @Override
  public void commit() throws SQLException {
    conn.commit();
  }

  public long getTotalInserts() {
    return totalInserts;
  }

  @Override
  public void close() throws Exception {
    try {
      executeBatch();
      commit();
    } finally {
      insertStatement.close();
      vacuum();
      conn.close();
    }
  }

  /**
   * Vacuums the database.
   *
   * Note that this will change the autocommit setting while excution update is being
   * called!
   */
  @Override
  public void vacuum() throws SQLException {
    // Do a full vacuum of the live repository.  This
    // should be fairly fast as it's deliberately size constrained.

    // 1) maintain an in-memory operation-queue, so you can copy the DB when idle,
    // vacuum as long as necessary on the copy, and then switch to the vacuumed copy
    // after replaying the queue.
    // (SQLite allows only a single writer, so statement-replay is safe, unlike
    // concurrent-writer databases in some cases since you can't recreate the DB's
    // row-visibility logic)
    // https://news.ycombinator.com/item?id=23521079

    // Use dbstat to find out what fraction of the pages in a database are sequential
    // if there's a significant degree of fragmentation, then vacuum.
    // https://www.sqlite.org/dbstat.html

    conn.setAutoCommit(true);
    try (Statement stmt = conn.createStatement()) {
      stmt.executeUpdate("VACUUM");
    }
    conn.setAutoCommit(false);
  }
}
