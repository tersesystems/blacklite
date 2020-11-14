package com.tersesystems.blacklite;

import java.util.ResourceBundle;

public final class Statements {
  private static final ResourceBundle bundle =
      ResourceBundle.getBundle(Statements.class.getPackage().getName() + ".resources");

  public static String createEntriesTable() {
    return bundle.getString("entries.create.statement");
  }

  public static String selectDatabaseSize() {
    return bundle.getString("entries.dbsize.statement");
  }

  public static String insert() {
    return bundle.getString("entries.insert.statement");
  }

  public static String selectMaxRowId() {
    return bundle.getString("entries.maxrow.statement");
  }

  public static String oldest() {
    return bundle.getString("entries.oldest.statement");
  }

  public static String databaseSizeStatement() {
    return bundle.getString("entries.dbsize.statement");
  }

  public static String numRows() {
    return bundle.getString("entries.numrows.statement");
  }

  public static String archiveNumRows() {
    return bundle.getString("entries.archive.numrows.statement");
  }

  public static String createEntriesView() {
    return bundle.getString("entries_view.create.statement");
  }

  public static String archive() {
    return bundle.getString("entries.archive.statement");
  }

  public static String deleteLessThanRowId() {
    return bundle.getString("entries.deletelessthan.statement");
  }

  public static String attachFormat() {
    return bundle.getString("entries.attach.statement");
  }

  public static String detach() {
    return bundle.getString("entries.detach.statement");
  }
}
