package com.tersesystems.blacklite.archive;

import java.sql.Connection;

public interface TriggeringPolicy {

  /**
   * @param conn the database connection.
   * @return true if the rollover strategy should be executed, false otherwise.
   */
  boolean isTriggered(Connection conn);
}
