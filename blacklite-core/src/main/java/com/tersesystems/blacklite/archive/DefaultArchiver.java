package com.tersesystems.blacklite.archive;

import com.tersesystems.blacklite.EntryStore;
import com.tersesystems.blacklite.Statements;
import com.tersesystems.blacklite.StatusReporter;
import com.tersesystems.blacklite.codec.Codec;
import com.tersesystems.blacklite.codec.identity.IdentityCodec;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Objects;
import java.util.Properties;
import org.sqlite.Function;
import org.sqlite.JDBC;
import org.sqlite.SQLiteConfig;

public class DefaultArchiver implements Archiver {

  private String file;
  private Properties properties = archiveSqliteConfig().toProperties();

  // Maximum number of rows that is allowed in the main live database. */
  private long archiveAfterRows = 10000;

  private Codec codec = new IdentityCodec();
  private EntryStore entryStore;
  private StatusReporter statusReporter;

  private RollingStrategy rollingStrategy;
  private TriggeringPolicy triggeringPolicy;

  public String getFile() {
    return file;
  }

  public void setFile(String file) {
    this.file = file;
  }

  public Properties getProperties() {
    return properties;
  }

  public void setProperties(Properties properties) {
    this.properties = properties;
  }

  public long getArchiveAfterRows() {
    return archiveAfterRows;
  }

  public void setArchiveAfterRows(long archiveAfterRows) {
    this.archiveAfterRows = archiveAfterRows;
  }

  @Override
  public Codec getCodec() {
    return codec;
  }

  @Override
  public void setCodec(Codec codec) {
    this.codec = codec;
  }

  @Override
  public EntryStore getEntryStore() {
    return entryStore;
  }

  @Override
  public void setEntryStore(EntryStore entryStore) {
    this.entryStore = entryStore;
  }

  @Override
  public boolean shouldArchive() {
    try {
      return shouldArchive(entryStore.getConnection());
    } catch (SQLException e) {
      statusReporter.addError(e.getMessage(), e);
      return false;
    }
  }

  @Override
  public int archive() {
    try {
      return archive(entryStore.getConnection());
    } catch (SQLException e) {
      statusReporter.addError(e.getMessage(), e);
      return 0;
    }
  }

  @Override
  public void close() {
    codec.close();
  }

  /** Returns true if archiving should happen, otherwise false. */
  boolean shouldArchive(Connection conn) throws SQLException {
    long numRows = numRows(conn);
    return numRows > getArchiveAfterRows();
  }

  long numRows(Connection conn) throws SQLException {
    try (PreparedStatement ps = conn.prepareStatement(Statements.numRows())) {
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        return rs.getLong(1);
      } else {
        throw new IllegalStateException("No number of rows?");
      }
    }
  }

  long findMaxRowId(Connection conn) throws SQLException {
    try (PreparedStatement ps = conn.prepareStatement(Statements.selectMaxRowId())) {
      final ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        return rs.getLong(1);
      } else {
        throw new IllegalStateException("No row id?!?");
      }
    }
  }

  @Override
  public void initialize(StatusReporter statusReporter) throws SQLException {
    Objects.requireNonNull(codec, "Null codec");
    codec.initialize(statusReporter);

    // Slight cheat here for benchmarking with a null entry store...
    final Connection connection = entryStore.getConnection();
    if (connection != null) {
      Function codecFunction =
          new Function() {
            @Override
            protected void xFunc() throws SQLException {
              result(codec.encode(value_blob(0)));
            }
          };
      // Register the codec as a custom SQLite function
      Function.create(connection, "encode", codecFunction);
    }

    this.statusReporter = statusReporter;
  }

  /**
   * Reads from the oldest entries from the "live" database, and writes the batch into the "archive"
   * database. Once the batch has been committed, the original entries are removed from the "live"
   * database.
   *
   * <p>Because the archive database is append-only and inserts in batches, this keeps the archive
   * database down to near flat-file level efficiency.
   */
  int archive(Connection conn) throws SQLException {
    // XXX Better logic that can be driven by configuration here.

    // This is the number of rows to leave in the live database (not archived or encoded)
    final long archiveAfterRows = getArchiveAfterRows();

    long maxRowId = findMaxRowId(conn);
    long rowId = maxRowId - archiveAfterRows;

    String file = getFile();
    if (file == null) {
      statusReporter.addError("archive: No file found in archiver!");
      System.exit(-1);
      return 0;
    }

    final Path archivePath = Paths.get(getFile());

    // Create the archive database if it doesn't already exist
    String archiveUrl = "jdbc:sqlite:" + archivePath.toString();
    try (Connection archiveConn = JDBC.createConnection(archiveUrl, getProperties())) {
      try (Statement stmt = archiveConn.createStatement()) {
        stmt.execute(Statements.createEntriesTable());
        stmt.execute(Statements.createEntriesView());
      }
    }

    int inserted = 0;
    boolean success = false;
    try {
      try (Statement st = conn.createStatement()) {
        String attach = String.format(Statements.attachFormat(), archivePath.toString());
        st.execute(attach);
      }

      // Insert from LIVE to ARCHIVE using custom SQL encode function here.
      // Delete from LIVE using the same critera.
      try (PreparedStatement insertStatement = conn.prepareStatement(Statements.archive())) {
        insertStatement.setLong(1, rowId);
        inserted = insertStatement.executeUpdate();
      }

      int deleted = 0;
      try (PreparedStatement deleteStatement =
          conn.prepareStatement(Statements.deleteLessThanRowId())) {
        deleteStatement.setLong(1, rowId);
        deleted = deleteStatement.executeUpdate();
      }
      // System.out.printf("inserted = %s, deleted = %s, maxRowId = %s, rowId = %s%n", inserted,
      // deleted, maxRowId, rowId);

      if (inserted != deleted) {
        String msg =
            String.format("Inserted rows %s does not match deleted rows %s", inserted, deleted);
        throw new IllegalStateException(msg);
      }

      // Transactions will be atomic across databases, but only if the main database
      // is neither in WAL mode, or a :memory: database.
      // https://stackoverflow.com/questions/27224104/sqlite-using-one-file-vs-many-files

      if (triggeringPolicy != null && rollingStrategy != null) {
        if (triggeringPolicy.isTriggered(conn)) {
          // XXX should add an option to index timestamp/level columns on rollover
          rollingStrategy.rollover(this);
        }
      }
      success = true;
    } finally {
      if (success) conn.commit();
      else conn.rollback();

      try (Statement st = conn.createStatement()) {
        st.execute(Statements.detach());
      }
    }

    return inserted;
  }

  SQLiteConfig archiveSqliteConfig() {
    SQLiteConfig config = new SQLiteConfig();
    config.setPageSize(4096);
    config.setEncoding(SQLiteConfig.Encoding.UTF8);
    return config;
  }

  public RollingStrategy getRollingStrategy() {
    return rollingStrategy;
  }

  public void setRollingStrategy(RollingStrategy rollingStrategy) {
    this.rollingStrategy = rollingStrategy;
  }

  public TriggeringPolicy getTriggeringPolicy() {
    return triggeringPolicy;
  }

  public void setTriggeringPolicy(TriggeringPolicy triggeringPolicy) {
    this.triggeringPolicy = triggeringPolicy;
  }

  @Override
  public String toString() {
    return "DefaultArchiver{"
        + "file='"
        + file
        + '\''
        + ", properties="
        + properties
        + ", maximumNumRows="
        + archiveAfterRows
        + ", codec="
        + codec
        + ", entryStore="
        + entryStore
        + ", statusReporter="
        + statusReporter
        + ", rollingStrategy="
        + rollingStrategy
        + ", triggeringPolicy="
        + triggeringPolicy
        + '}';
  }
}
