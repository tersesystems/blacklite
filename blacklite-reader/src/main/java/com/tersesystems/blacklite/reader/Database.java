package com.tersesystems.blacklite.reader;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** A wrapper class for connecting to sqlite database. */
public class Database {

  public static final class Entries {

    public static final String EPOCH_SECS = "epoch_secs";
    public static final String NANOS = "nanos";
    public static final String LEVEL = "level";
    public static final String CONTENT = "content";
  }

  public static Connection createConnection(File file) throws SQLException {
    String url = "jdbc:sqlite:" + file.getAbsolutePath();
    return DriverManager.getConnection(url);
  }
}
