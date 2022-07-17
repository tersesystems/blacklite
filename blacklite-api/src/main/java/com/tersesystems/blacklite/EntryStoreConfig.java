package com.tersesystems.blacklite;

import java.util.Properties;

public interface EntryStoreConfig {
  String getFile();

  void setFile(String file);

  Properties getProperties();

  void setProperties(Properties properties);

  int getBatchInsertSize();

  void setBatchInsertSize(int batchInsertSize);

  boolean isTracing();

  void setTracing(boolean tracing);
}
