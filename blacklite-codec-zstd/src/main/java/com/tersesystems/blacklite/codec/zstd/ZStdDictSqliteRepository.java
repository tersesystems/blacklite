package com.tersesystems.blacklite.codec.zstd;

import static com.tersesystems.blacklite.DefaultEntryStore.APPLICATION_ID;

import com.github.luben.zstd.Zstd;
import com.tersesystems.blacklite.codec.CodecException;
import java.sql.*;
import java.util.Objects;
import java.util.Optional;
import org.sqlite.JDBC;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConnection;

/** Repository that can write out dictionaries and codec specific info. */
public class ZStdDictSqliteRepository implements ZstdDictRepository {

  public static int CODEC_DB_ID = 3;
  private SQLiteConnection conn;

  private String file;

  public void initialize() throws CodecException {
    Objects.requireNonNull(file, "null file");
    try {
      String url = (file.startsWith("jdbc:sqlite:")) ? file : "jdbc:sqlite:" + file;
      this.conn = JDBC.createConnection(url, sqliteConfig().toProperties());
      this.conn.setAutoCommit(false);
      createDDL();
    } catch (SQLException e) {
      throw new CodecException(e);
    }
  }

  public String getFile() {
    return file;
  }

  public void setFile(String file) {
    this.file = file;
  }

  @Override
  public Optional<ZstdDict> lookup(long id) {
    try {
      String queryStatement = Dictionaries.queryStatement();
      try (PreparedStatement statement = conn.prepareStatement(queryStatement)) {
        statement.setLong(1, id);
        return dictFromStatement(statement);
      }
    } catch (SQLException ex) {
      throw new CodecException(ex);
    }
  }

  @Override
  public Optional<ZstdDict> mostRecent() {
    try {
      // XXX should read from database first, if empty then look for file and
      // read that in and save it to database
      String queryStatement = Dictionaries.firstStatement();
      try (PreparedStatement statement = conn.prepareStatement(queryStatement)) {
        return dictFromStatement(statement);
      }
    } catch (SQLException ex) {
      throw new CodecException(ex);
    }
  }

  @Override
  public void save(byte[] dictBytes) {
    long dictId = Zstd.getDictIdFromDict(dictBytes);
    try {
      String insertStatement = Dictionaries.insertStatement();
      try (PreparedStatement statement = conn.prepareStatement(insertStatement)) {
        int adder = 1;
        statement.setLong(adder++, dictId);
        statement.setBytes(adder, dictBytes);
        statement.executeUpdate();
      }
      conn.commit();
    } catch (SQLException ex) {
      ex.printStackTrace();
    }
  }

  @Override
  public void close() throws SQLException {
    this.conn.close();
  }

  protected SQLiteConfig sqliteConfig() {
    SQLiteConfig config = new SQLiteConfig();
    config.setApplicationId(APPLICATION_ID);
    config.setUserVersion(CODEC_DB_ID);
    return config;
  }

  private Optional<ZstdDict> dictFromStatement(PreparedStatement statement) throws SQLException {
    try (ResultSet rs = statement.executeQuery()) {
      if (rs.next()) {
        long id1 = rs.getLong(1);
        byte[] bytes = rs.getBytes(2);
        ZstdDict dict = new ZstdDict(id1, bytes);
        return Optional.of(dict);
      } else {
        return Optional.empty();
      }
    }
  }

  protected void createDDL() {
    try {
      String createStatements = Dictionaries.createStatement();
      try (Statement stmt = conn.createStatement()) {
        stmt.executeUpdate(createStatements);
      }
      conn.commit();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public static final class Dictionaries {
    public static String createStatement() {
      return "CREATE TABLE IF NOT EXISTS zstd_dicts (" // keep the dictionary in database
          + "dict_id LONG NOT NULL PRIMARY KEY," // the id, always 32 bit random
          + "dict_bytes BLOB NOT NULL)"; // the zstd dictionary itself
    }

    public static String insertStatement() {
      return "INSERT INTO zstd_dicts(dict_id, dict_bytes)\n" + "values(?, ?)";
    }

    public static String queryStatement() {
      return "SELECT dict_id, dict_bytes FROM zstd_dicts WHERE dict_id = ?";
    }

    public static String firstStatement() {
      return "SELECT dict_id, dict_bytes FROM zstd_dicts LIMIT 1";
    }
  }
}
