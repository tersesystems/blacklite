package com.tersesystems.blacklite.reader;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class QueryBuilder {

  private String whereString;
  private Instant before;
  private Instant after;
  private int boundParams = 0;

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

  public Stream<LogEntry> execute(Connection c, boolean verbose) throws SQLException {
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

      final ResultSet rs = ps.executeQuery();
      final LogEntrySpliterator logEntrySpliterator = new LogEntrySpliterator(rs);
      return StreamSupport.stream(logEntrySpliterator, false);
    }
  }

  String createSQL() {
    StringBuilder sb = new StringBuilder();
    sb.append("SELECT epoch_secs, nanos, level, content FROM entries");

    if (boundParams > 0 || whereString != null) {
      sb.append(" WHERE ");
    }

    if (before != null) {
      sb.append("epoch_secs < ? ");
      if (boundParams > 1) {
        sb.append(" AND ");
      }
    }

    if (after != null) {
      sb.append("epoch_secs > ? ");
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
