package com.tersesystems.blacklite;

import java.util.ResourceBundle;

public final class Statements {

  // Don't allow bundle to be defined statically, as it messes up class init if
  // those class resources aren't found.
  private final ResourceBundle bundle =
      ResourceBundle.getBundle(this.getClass().getPackage().getName() + ".resources");

  private static class SingletonHolder {
    static final Statements INSTANCE = new Statements();
  }

  public static Statements instance() {
    return SingletonHolder.INSTANCE;
  }

  public String createEntriesTable() {
    return bundle.getString("entries.create.statement");
  }

  public String selectDatabaseSize() {
    return bundle.getString("entries.dbsize.statement");
  }

  public String insert() {
    return bundle.getString("entries.insert.statement");
  }

  public String selectMaxRowId() {
    return bundle.getString("entries.maxrow.statement");
  }

  public String oldest() {
    return bundle.getString("entries.oldest.statement");
  }

  public String databaseSizeStatement() {
    return bundle.getString("entries.dbsize.statement");
  }

  public String numRows() {
    return bundle.getString("entries.numrows.statement");
  }

  public String archiveNumRows() {
    return bundle.getString("entries.archive.numrows.statement");
  }

  public String createEntriesView() {
    return bundle.getString("entries_view.create.statement");
  }

  public String archive() {
    return bundle.getString("entries.archive.statement");
  }

  public String deleteLessThanRowId() {
    return bundle.getString("entries.deletelessthan.statement");
  }

  public String attachFormat() {
    return bundle.getString("entries.attach.statement");
  }

  public String detach() {
    return bundle.getString("entries.detach.statement");
  }
}
