package com.tersesystems.blacklite;

import java.sql.Connection;
import java.sql.SQLException;

public interface EntryStore extends AutoCloseable {

  int APPLICATION_ID = 0xB1AC3117;

  void insert(long epochSecond, int nanos, int level, byte[] content) throws SQLException;

  Connection getConnection();

  void vacuum() throws SQLException;

  void executeBatch() throws SQLException;

  void commit() throws SQLException;

  void initialize() throws SQLException;

  String getUrl();
}
