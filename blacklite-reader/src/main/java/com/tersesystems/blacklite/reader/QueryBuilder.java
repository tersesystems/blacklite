package com.tersesystems.blacklite.reader;

import com.tersesystems.blacklite.codec.Codec;
import org.sqlite.Function;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class QueryBuilder {

  private final Codec codec;
  private String whereString;
  private Instant before;
  private Instant after;
  private int boundParams = 0;
  private boolean count;

  public QueryBuilder(Codec codec) {
    this.codec = codec;
  }

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

  public long executeCount(Connection c, boolean verbose) throws SQLException {
    final String statement = createCountSQL();

    if (verbose) {
      verboseExecute(statement);
    }

    PreparedStatement ps = c.prepareStatement(statement);
    int adder = 1;
    if (before != null) {
      ps.setLong(adder++, before.getEpochSecond());
    }

    if (after != null) {
      ps.setLong(adder, after.getEpochSecond());
    }

    try (ResultSet rs = ps.executeQuery()) {
      rs.next();
      return rs.getLong(1);
    }
  }

  public Stream<LogEntry> execute(Connection c, boolean verbose) throws SQLException {
    final String statement = createSQL();

    if (verbose) {
      verboseExecute(statement);
    }

    registerCodec(c);
    PreparedStatement ps = c.prepareStatement(statement);
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

  public void registerCodec(Connection c) throws SQLException {
    Function codecFunction =
      new Function() {
        @Override
        protected void xFunc() throws SQLException {
          result(codec.decode(value_blob(0)));
        }
      };
    // Register the codec as a custom SQLite function
    Function.create(c, "decode", codecFunction);
  }

  public String createCountSQL() {
    StringBuilder sb = new StringBuilder();
    sb.append("SELECT COUNT(*) FROM entries");

    return createParameters(sb);
  }

  public String createSQL() {
    StringBuilder sb = new StringBuilder();
    sb.append("SELECT epoch_secs, nanos, level, decode(content) FROM entries");

    return createParameters(sb);
  }

  public String createParameters(StringBuilder sb) {
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

  public Instant getBefore() {
    return before;
  }

  public Instant getAfter() {
    return after;
  }

  public String getWhere() {
    return whereString;
  }

  public void setCount(boolean count) {
    this.count = count;
  }

  public boolean isCount() {
    return this.count;
  }

}
