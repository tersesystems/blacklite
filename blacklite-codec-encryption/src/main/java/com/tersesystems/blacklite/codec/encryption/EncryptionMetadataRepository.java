package com.tersesystems.blacklite.codec.encryption;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/** Repository for storing and retrieving encryption metadata from SQLite. */
public class EncryptionMetadataRepository {

  private static final String TABLE_NAME = "blacklite_encryption_metadata";

  private final Connection connection;

  public EncryptionMetadataRepository(Connection connection) {
    this.connection = connection;
  }

  /** Initialize the metadata table. */
  public void initialize() throws SQLException {
    String createTable =
        "CREATE TABLE IF NOT EXISTS "
            + TABLE_NAME
            + " ("
            + "  id INTEGER PRIMARY KEY,"
            + "  algorithm TEXT NOT NULL,"
            + "  encrypted_key BLOB NOT NULL,"
            + "  key_encryption_algorithm TEXT,"
            + "  key_encryption_comment TEXT,"
            + "  created_at INTEGER NOT NULL"
            + ")";

    try (Statement stmt = connection.createStatement()) {
      stmt.execute(createTable);
    }
  }

  /** Save encryption metadata. */
  public void save(EncryptionMetadata metadata) throws SQLException {
    String insert =
        "INSERT INTO "
            + TABLE_NAME
            + " (algorithm, encrypted_key, key_encryption_algorithm, key_encryption_comment, created_at) "
            + "VALUES (?, ?, ?, ?, ?)";

    try (PreparedStatement stmt = connection.prepareStatement(insert)) {
      stmt.setString(1, metadata.getAlgorithm());
      stmt.setBytes(2, metadata.getEncryptedKey());
      stmt.setString(3, metadata.getKeyEncryptionAlgorithm());
      stmt.setString(4, metadata.getKeyEncryptionComment());
      stmt.setLong(5, metadata.getCreatedAt());
      stmt.executeUpdate();
    }
  }

  /** Get encryption metadata (returns the first row). */
  public Optional<EncryptionMetadata> get() throws SQLException {
    String query = "SELECT * FROM " + TABLE_NAME + " LIMIT 1";

    try (Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(query)) {

      if (rs.next()) {
        EncryptionMetadata metadata =
            new EncryptionMetadata(
                rs.getInt("id"),
                rs.getString("algorithm"),
                rs.getBytes("encrypted_key"),
                rs.getString("key_encryption_algorithm"),
                rs.getString("key_encryption_comment"),
                rs.getLong("created_at"));
        return Optional.of(metadata);
      }

      return Optional.empty();
    }
  }
}
