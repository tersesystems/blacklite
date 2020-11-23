package com.tersesystems.blacklite.archive;

import com.tersesystems.blacklite.EntryStore;
import com.tersesystems.blacklite.StatusReporter;
import java.sql.SQLException;

/** The archive policy determines when old data is archived. */
public interface Archiver extends AutoCloseable {

  EntryStore getEntryStore();

  void setEntryStore(EntryStore entryStore);

  int archive();

  void close() throws Exception;

  void initialize(StatusReporter statusReporter) throws SQLException;
}
