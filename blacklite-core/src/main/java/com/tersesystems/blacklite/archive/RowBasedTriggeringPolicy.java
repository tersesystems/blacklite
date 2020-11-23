package com.tersesystems.blacklite.archive;

import com.tersesystems.blacklite.Statements;
import java.sql.*;

public class RowBasedTriggeringPolicy implements TriggeringPolicy {

  private long maximumNumRows = Long.MAX_VALUE;

  @Override
  public boolean isTriggered(Connection conn) {
    boolean result = false;
    try (PreparedStatement st = conn.prepareStatement(statements().archiveNumRows())) {
      try (ResultSet rs = st.executeQuery()) {
        if (rs.next()) {
          final long size = rs.getLong(1);
          result = size > getMaximumNumRows();
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return result;
  }

  private Statements statements() {
    return Statements.instance();
  }

  public long getMaximumNumRows() {
    return maximumNumRows;
  }

  public void setMaximumNumRows(long maximumNumRows) {
    this.maximumNumRows = maximumNumRows;
  }
}
