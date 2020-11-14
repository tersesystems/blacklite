package com.tersesystems.blacklite.reader;

/**
 * Consumes results of the command runner.
 *
 * <p>Breaking out the interface here makes it easier to set up shims for tests.
 */
public interface ResultConsumer {
  void print(byte[] content);

  void count(long rowCount);
}
