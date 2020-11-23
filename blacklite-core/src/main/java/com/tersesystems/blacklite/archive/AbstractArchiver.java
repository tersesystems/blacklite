package com.tersesystems.blacklite.archive;

import com.tersesystems.blacklite.EntryStore;
import com.tersesystems.blacklite.Statements;
import com.tersesystems.blacklite.StatusReporter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class AbstractArchiver implements Archiver {

  private EntryStore entryStore;

  private TriggeringPolicy triggeringPolicy;

  protected StatusReporter statusReporter;

  // Maximum number of rows that is allowed in the main live database. */
  private long archiveAfterRows = 10000;

  public long getArchiveAfterRows() {
    return archiveAfterRows;
  }

  public void setArchiveAfterRows(long archiveAfterRows) {
    this.archiveAfterRows = archiveAfterRows;
  }

  @Override
  public EntryStore getEntryStore() {
    return entryStore;
  }

  public TriggeringPolicy getTriggeringPolicy() {
    return triggeringPolicy;
  }

  public void setTriggeringPolicy(TriggeringPolicy triggeringPolicy) {
    this.triggeringPolicy = triggeringPolicy;
  }

  @Override
  public void setEntryStore(EntryStore entryStore) {
    this.entryStore = entryStore;
  }

  protected int deleteFromLive(Connection conn, long rowId) throws SQLException {
    int deleted = 0;
    try (PreparedStatement deleteStatement =
        conn.prepareStatement(statements().deleteLessThanRowId())) {
      deleteStatement.setLong(1, rowId);
      deleted = deleteStatement.executeUpdate();
    }
    return deleted;
  }

  long findMaxRowId(Connection conn) throws SQLException {
    try (PreparedStatement ps = conn.prepareStatement(statements().selectMaxRowId())) {
      final ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        return rs.getLong(1);
      } else {
        throw new IllegalStateException("No row id?!?");
      }
    }
  }

  /** Returns true if archiving should happen, otherwise false. */
  boolean shouldArchive(Connection conn) throws SQLException {
    // XXX should this be triggering policy
    final long numRows = numRows(conn);
    final long archiveAfterRows = getArchiveAfterRows();
    boolean result = numRows > archiveAfterRows;
    return result;
  }

  long numRows(Connection conn) throws SQLException {
    try (PreparedStatement ps = conn.prepareStatement(statements().numRows())) {
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        return rs.getLong(1);
      } else {
        throw new IllegalStateException("No number of rows?");
      }
    }
  }

  protected Statements statements() {
    return Statements.instance();
  }
}
