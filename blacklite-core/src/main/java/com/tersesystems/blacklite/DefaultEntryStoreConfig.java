package com.tersesystems.blacklite;

import java.util.Properties;
import org.sqlite.SQLiteConfig;

public class DefaultEntryStoreConfig implements EntryStoreConfig {

  private String url = "jdbc:sqlite:blacklite.db";
  private Properties properties = liveConfig().toProperties();
  private long batchInsertSize = 1000;

  @Override
  public String getUrl() {
    return this.url;
  }

  @Override
  public void setUrl(String url) {
    this.url = url;
  }

  @Override
  public Properties getProperties() {
    return this.properties;
  }

  @Override
  public void setProperties(Properties properties) {
    this.properties = properties;
  }

  @Override
  public long getBatchInsertSize() {
    return this.batchInsertSize;
  }

  @Override
  public void setBatchInsertSize(long batchInsertSize) {
    this.batchInsertSize = batchInsertSize;
  }

  SQLiteConfig liveConfig() {
    // https://github.com/xerial/sqlite-jdbc/blob/master/Usage.md#configure-connections
    // https://phiresky.github.io/blog/2020/sqlite-performance-tuning/
    // http://sqlite.1065341.n5.nabble.com/In-memory-only-WAL-file-td101283.html
    // https://stackoverflow.com/questions/1711631/improve-insert-per-second-performance-of-sqlite
    // https://sqlite.org/pragma.html#pragma_journal_mode
    // https://wiki.mozilla.org/Performance/Avoid_SQLite_In_Your_Next_Firefox_Feature
    // https://www.deconstructconf.com/2019/dan-luu-files

    SQLiteConfig config = new SQLiteConfig();
    // config.setApplicationId(APPLICATION_ID);
    // config.setUserVersion(LIVE_DB);
    config.setPageSize(4096);

    // Mandate UTF8 for general hygiene
    config.setEncoding(SQLiteConfig.Encoding.UTF8);

    // WAL makes it much easier to read and write without seeing locks...
    config.setJournalMode(SQLiteConfig.JournalMode.WAL);

    // https://www.sqlite.org/pragma.html#pragma_wal_autocheckpoint
    // PRAGMA wal_autocheckpoint=N to ensure that size doesn't grow too large?
    // https://groups.google.com/g/sqlite_users/c/hhy4Zk89uG4/m/kDYpcmVPNskJ

    // the default is 1000
    //  wal_autocheckpoint=4096

    // Changes the maximum number of database disk pages that SQLite will hold
    // in memory at once per open database file.
    config.setCacheSize(409600);
    //
    // config.setSynchronous(SQLiteConfig.SynchronousMode.OFF);

    // TEMP_STORE_DIRECTORY can be placed to something that is using tmpfs so we can use swap?
    // config.setTempStore(SQLiteConfig.TempStore.FILE);
    config.setTempStore(SQLiteConfig.TempStore.MEMORY);
    // config.setTempStoreDirectory("/tmp/blacklite/");

    // 256 MB of memory mapping
    config.setPragma(SQLiteConfig.Pragma.MMAP_SIZE, "268435500");

    return config;
  }

  @Override
  public String toString() {
    return "DefaultEntryStoreConfig{"
        + "url='"
        + url
        + '\''
        + ", properties="
        + properties
        + ", batchInsertSize="
        + batchInsertSize
        + '}';
  }
}
