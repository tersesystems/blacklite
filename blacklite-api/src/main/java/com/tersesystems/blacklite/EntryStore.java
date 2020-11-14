package com.tersesystems.blacklite;

import java.sql.Connection;
import java.sql.SQLException;

public interface EntryStore extends AutoCloseable {
  void insert(long epochSecond, int nanos, int level, byte[] content) throws SQLException;

  void vacuum() throws SQLException;

  void executeBatch() throws SQLException;

  void commit() throws SQLException;

  void initialize() throws SQLException;

  Connection getConnection();
}
