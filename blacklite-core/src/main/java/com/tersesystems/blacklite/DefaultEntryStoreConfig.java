package com.tersesystems.blacklite;

import static com.tersesystems.blacklite.DefaultEntryStore.APPLICATION_ID;

import java.util.Properties;
import org.sqlite.SQLiteConfig;

public class DefaultEntryStoreConfig implements EntryStoreConfig {
  public static final int MAX_CAPACITY = 1048576;
  public static final int BATCH_INSERT_SIZE = 1000;

  private static final Properties defaults = liveConfig().toProperties();

  private String file;
  private Properties properties = new Properties(defaults);
  private int batchInsertSize = BATCH_INSERT_SIZE;
  private boolean tracing = false;

  private int maxCapacity = MAX_CAPACITY;

  @Override
  public String getFile() {
    return this.file;
  }

  @Override
  public void setFile(String file) {
    this.file = file;
  }

  @Override
  public Properties getProperties() {
    return this.properties;
  }

  @Override
  public void setProperties(Properties properties) {
    Properties merged = new Properties();
    merged.putAll(defaults);
    merged.putAll(properties);
    this.properties = merged;
  }

  @Override
  public int getBatchInsertSize() {
    return this.batchInsertSize;
  }

  @Override
  public void setBatchInsertSize(int batchInsertSize) {
    this.batchInsertSize = batchInsertSize;
  }

  @Override
  public int getMaxCapacity() {
    return this.maxCapacity;
  }

  @Override
  public void setMaxCapacity(int maxCapacity) {
    this.maxCapacity = maxCapacity;
  }

  @Override
  public boolean getTracing() {
    return this.tracing;
  }

  @Override
  public void setTracing(boolean tracing) {
    this.tracing = tracing;
  }

  private static SQLiteConfig liveConfig() {
    // https://github.com/xerial/sqlite-jdbc/blob/master/Usage.md#configure-connections
    // https://phiresky.github.io/blog/2020/sqlite-performance-tuning/
    // http://sqlite.1065341.n5.nabble.com/In-memory-only-WAL-file-td101283.html
    // https://stackoverflow.com/questions/1711631/improve-insert-per-second-performance-of-sqlite
    // https://sqlite.org/pragma.html#pragma_journal_mode
    // https://wiki.mozilla.org/Performance/Avoid_SQLite_In_Your_Next_Firefox_Feature
    // https://www.deconstructconf.com/2019/dan-luu-files

    SQLiteConfig config = new SQLiteConfig();
    config.setApplicationId(APPLICATION_ID);
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
        + "file='"
        + file
        + '\''
        + ", properties="
        + properties
        + ", batchInsertSize="
        + batchInsertSize
        + '}';
  }
}
