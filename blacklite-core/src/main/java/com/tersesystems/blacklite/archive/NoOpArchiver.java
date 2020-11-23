package com.tersesystems.blacklite.archive;

import com.tersesystems.blacklite.EntryStore;
import com.tersesystems.blacklite.StatusReporter;
import java.sql.SQLException;

public class NoOpArchiver implements Archiver {
  private EntryStore entryStore;

  @Override
  public EntryStore getEntryStore() {
    return this.entryStore;
  }

  @Override
  public void setEntryStore(EntryStore entryStore) {
    this.entryStore = entryStore;
  }

  @Override
  public int archive() {
    return 0;
  }

  @Override
  public void close() throws Exception {}

  @Override
  public void initialize(StatusReporter statusReporter) throws SQLException {}
}
