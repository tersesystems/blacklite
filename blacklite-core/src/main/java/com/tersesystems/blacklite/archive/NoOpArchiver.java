package com.tersesystems.blacklite.archive;

import com.tersesystems.blacklite.EntryStore;
import com.tersesystems.blacklite.StatusReporter;
import com.tersesystems.blacklite.codec.Codec;
import java.sql.SQLException;

public class NoOpArchiver implements Archiver {
  @Override
  public Codec getCodec() {
    return null;
  }

  @Override
  public void setCodec(Codec codec) {}

  @Override
  public EntryStore getEntryStore() {
    return null;
  }

  @Override
  public void setEntryStore(EntryStore entryStore) {}

  @Override
  public String getFile() {
    return null;
  }

  @Override
  public int archive() {
    return 0;
  }

  @Override
  public void close() {}

  @Override
  public void initialize(StatusReporter statusReporter) throws SQLException {}
}
