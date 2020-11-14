package com.tersesystems.blacklite.archive;

import com.tersesystems.blacklite.Statements;
import java.sql.*;

public class ArchiveRowsTriggeringPolicy implements TriggeringPolicy {

  private long maximumNumRows = Long.MAX_VALUE;

  @Override
  public boolean isTriggered(Connection conn) {
    boolean result = false;
    try (PreparedStatement st = conn.prepareStatement(Statements.archiveNumRows())) {
      try (ResultSet rs = st.executeQuery()) {
        if (rs.next()) {
          final long size = rs.getLong(1);
          result = size > getMaximumNumRows();
          System.out.printf(
              "isTriggered: size = %s, max = %s, result = %s\n", size, getMaximumNumRows(), result);
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return result;
  }

  public long getMaximumNumRows() {
    return maximumNumRows;
  }

  public void setMaximumNumRows(long maximumNumRows) {
    this.maximumNumRows = maximumNumRows;
  }
}
