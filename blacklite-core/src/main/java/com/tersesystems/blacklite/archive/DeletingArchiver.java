package com.tersesystems.blacklite.archive;

import com.tersesystems.blacklite.StatusReporter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import org.sqlite.JDBC;
import org.sqlite.SQLiteConfig;

/**
 * This class deletes the oldest rows when the maximum number of rows exceeds the archiveAfterRow
 * property.
 */
public class DeletingArchiver extends AbstractArchiver {

  @Override
  public void initialize(StatusReporter statusReporter) throws SQLException {
    this.statusReporter = statusReporter;
  }

  @Override
  public ArchiveResult archive(Connection conn) {
    try {
      if (shouldArchive(conn)) {
        return new ArchiveResult.Success(delete(conn));
      } else {
        return ArchiveResult.NoOp.instance;
      }
    } catch (Exception e) {
      return new ArchiveResult.Failure(e);
    }
  }


  int delete(Connection conn) throws SQLException {
    // This is the number of rows to leave in the live database (not archived or encoded)
    final long archiveAfterRows = getArchiveAfterRows();

    long maxRowId = findMaxRowId(conn);
    long rowId = maxRowId - archiveAfterRows;

    int deleted;
    boolean success = false;
    try {
      deleted = deleteFromLive(conn, rowId);
      success = true;
    } finally {
      if (success) conn.commit();
      else conn.rollback();
    }
    return deleted;
  }

  @Override
  public void close() throws Exception {}
}
