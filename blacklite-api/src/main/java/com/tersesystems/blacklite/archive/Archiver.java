package com.tersesystems.blacklite.archive;

import com.tersesystems.blacklite.EntryStore;
import com.tersesystems.blacklite.StatusReporter;
import com.tersesystems.blacklite.codec.Codec;
import java.sql.SQLException;

/** The archive policy determines when old data is archived. */
public interface Archiver extends AutoCloseable {

  Codec getCodec();

  void setCodec(Codec codec);

  EntryStore getEntryStore();

  void setEntryStore(EntryStore entryStore);

  String getFile();

  int archive();

  void close() throws Exception;

  void initialize(StatusReporter statusReporter) throws SQLException;
}
