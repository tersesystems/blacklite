package com.tersesystems.blacklite;

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
  public static int APPLICATION_ID = 0xF1F70000;

  private final Connection conn;
  private PreparedStatement insertStatement;

  private long totalInserts;
  private long totalBytes;

  public DefaultEntryStore(EntryStoreConfig config) throws SQLException {
    this.conn = JDBC.createConnection(config.getUrl(), config.getProperties());
  }

  @Override
  public void initialize() throws SQLException {
    try (Statement stmt = conn.createStatement()) {
      stmt.execute(Statements.createEntriesTable());
      stmt.execute(Statements.createEntriesView());
    }
    this.insertStatement = conn.prepareStatement(Statements.insert());

    // Set to transaction mode after setting up DDL.
    conn.setAutoCommit(false);
  }

  @Override
  public Connection getConnection() {
    return conn;
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
    insertStatement.close();
    vacuum();
    conn.close();
  }

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
  }
}
