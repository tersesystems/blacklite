package com.tersesystems.blacklite.reader;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

class QueryBuilder {

  private String whereString;
  private Instant before;
  private Instant after;
  private int boundParams = 0;
  private boolean count;

  public void addBefore(Instant before) {
    this.boundParams += 1;
    this.before = before;
  }

  public void addAfter(Instant after) {
    this.boundParams += 1;
    this.after = after;
  }

  public void addWhere(String whereString) {
    this.whereString = whereString.trim();
  }

  public void addCount(boolean count) {
    this.count = count;
  }

  public void execute(Connection c, ResultConsumer consumer, boolean verbose) throws SQLException {
    final String statement = createSQL();

    if (verbose) {
      verboseExecute(statement);
    }

    try (PreparedStatement ps = c.prepareStatement(statement)) {
      int adder = 1;
      if (before != null) {
        ps.setLong(adder++, before.getEpochSecond());
      }

      if (after != null) {
        ps.setLong(adder, after.getEpochSecond());
      }

      try (final ResultSet rs = ps.executeQuery()) {
        if (count) {
          if (rs.next()) {
            consumer.count(rs.getLong(1));
          }
        }

        while (rs.next()) {
          final byte[] content = rs.getBytes("content");
          consumer.print(content);
        }
      }
    }
  }

  String createSQL() {
    StringBuilder sb = new StringBuilder();
    if (count) {
      // SQLite does a full table scan for count, so use MAX(_rowid_) instead
      sb.append("SELECT MAX(_rowid_) FROM entries");
    } else {
      sb.append("SELECT content FROM entries");
    }

    if (boundParams > 0 || whereString != null) {
      sb.append(" WHERE ");
    }

    if (before != null) {
      sb.append(" timestamp < ? ");
      if (boundParams > 1) {
        sb.append(" AND ");
      }
    }

    if (after != null) {
      sb.append(" timestamp > ? ");
      if (whereString != null && !whereString.isEmpty()) {
        sb.append(" AND ");
      }
    }

    if (whereString != null && !whereString.isEmpty()) {
      sb.append(whereString);
    }

    return sb.toString();
  }

  public void verboseExecute(String statement) {
    verbosePrint("QueryBuilder statement: " + statement);
    verbosePrint(
        "QueryBuilder before: "
            + before
            + ((before != null) ? " / " + before.getEpochSecond() : ""));
    verbosePrint(
        "QueryBuilder after: " + after + ((after != null) ? " / " + after.getEpochSecond() : ""));
    verbosePrint("QueryBuilder where: " + whereString);
  }

  public void verbosePrint(String s) {
    System.err.println(s);
  }

  boolean getCount() {
    return count;
  }

  Instant getBefore() {
    return before;
  }

  Instant getAfter() {
    return after;
  }

  String getWhere() {
    return whereString;
  }
}
