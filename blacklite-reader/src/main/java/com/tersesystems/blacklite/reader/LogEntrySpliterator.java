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

  public LogEntrySpliterator(final ResultSet resultSet) {
    super(Long.MAX_VALUE,Spliterator.ORDERED);
    this.resultSet = resultSet;
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
      long epochSecs = resultSet.getLong(EPOCH_SECS);
      int nanos = resultSet.getInt(NANOS);
      int level = resultSet.getInt(LEVEL);
      byte[] bytes = resultSet.getBytes(CONTENT);

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
}
