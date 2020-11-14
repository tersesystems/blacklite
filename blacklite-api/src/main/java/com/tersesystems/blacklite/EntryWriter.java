package com.tersesystems.blacklite;

public interface EntryWriter extends AutoCloseable {

  void write(long epochSeconds, int nanos, int level, byte[] content);
}
