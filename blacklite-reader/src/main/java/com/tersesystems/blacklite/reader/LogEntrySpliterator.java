package com.tersesystems.blacklite.reader;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

/**
 * A log entry spliterator.  This is not thread safe.
 */
public class LogEntrySpliterator extends Spliterators.AbstractSpliterator<LogEntry> {

  private final ResultSet resultSet;
  private final LogEntry logEntry = new LogEntry();

  public LogEntrySpliterator(final ResultSet resultSet) {
    super(Long.MAX_VALUE,Spliterator.ORDERED);
    this.resultSet = resultSet;
  }

  @Override
  public boolean tryAdvance(Consumer<? super LogEntry> action) {
    if (next()) {
      LogEntry logEntry = createLogEntry(resultSet);
      action.accept(logEntry);
      return true;
    } else {
      return false;
    }
  }

  private LogEntry createLogEntry(ResultSet resultSet) {
    try {
      long epochSecs = resultSet.getLong("epoch_secs");
      int nanos = resultSet.getInt("nanos");
      int level = resultSet.getInt("level");
      byte[] bytes = resultSet.getBytes("content");

      return logEntry.write(epochSecs, nanos, level, bytes);
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
}
