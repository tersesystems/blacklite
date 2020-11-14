package com.tersesystems.blacklite;

import java.util.Properties;

public interface EntryStoreConfig {
  String getUrl();

  void setUrl(String url);

  Properties getProperties();

  void setProperties(Properties properties);

  long getBatchInsertSize();

  void setBatchInsertSize(long batchInsertSize);
}
