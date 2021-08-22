package com.tersesystems.blacklite.reader;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

import static com.tersesystems.blacklite.reader.Database.Entries.*;

/**
 * A log entry spliterator.  This is not thread safe.
 */
public class LogEntrySpliterator extends Spliterators.AbstractSpliterator<LogEntry> {

  private final ResultSet resultSet;
  private final LogEntry logEntry = new LogEntry();

  public LogEntrySpliterator(final ResultSet resultSet) throws SQLException {
    super(Long.MAX_VALUE,Spliterator.ORDERED);
    if (! resultSet.isClosed()) {
      this.resultSet = resultSet;
    } else {
      throw new SQLException("Closed resultset!");
    }
  }

  @Override
  public boolean tryAdvance(Consumer<? super LogEntry> action) {
    if (next()) {
      LogEntry logEntry = setLogEntry(resultSet);
      action.accept(logEntry);
      return true;
    } else {
      return false;
    }
  }

  private LogEntry setLogEntry(ResultSet resultSet) {
    try {
      long epochSecs = resultSet.getLong(1);
      int nanos = resultSet.getInt(2);
      int level = resultSet.getInt(3);
      byte[] bytes = resultSet.getBytes(4);

      return logEntry.set(epochSecs, nanos, level, bytes);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private boolean next() {
    try {
      return resultSet.next();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  // XXX should be a way to close the resultset / prepared statement here
}
