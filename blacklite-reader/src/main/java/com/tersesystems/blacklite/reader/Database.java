package com.tersesystems.blacklite.reader;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** A wrapper class for connecting to sqlite database. */
public class Database {

  public static Connection createConnection(File file) throws SQLException {
    String url = "jdbc:sqlite:" + file.getAbsolutePath();
    return DriverManager.getConnection(url);
  }
}
