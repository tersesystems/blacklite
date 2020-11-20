package com.tersesystems.blacklite;

import java.util.Properties;

public interface EntryStoreConfig {
  String getFile();

  void setFile(String file);

  Properties getProperties();

  void setProperties(Properties properties);

  long getBatchInsertSize();

  void setBatchInsertSize(long batchInsertSize);
}
