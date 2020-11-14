package com.tersesystems.blacklite.archive;

import java.sql.Connection;

public interface TriggeringPolicy {

  /** */
  boolean isTriggered(Connection conn);
}
