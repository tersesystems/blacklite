package com.tersesystems.blacklite.archive;

public interface RollingStrategy {

  /** Roll over an archive file. */
  void rollover(FileArchiver archiver);
}
