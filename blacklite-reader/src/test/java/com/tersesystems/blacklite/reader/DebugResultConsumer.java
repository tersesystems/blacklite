package com.tersesystems.blacklite.reader;

class DebugResultConsumer implements ResultConsumer {
  long actualCount = 0;
  byte[] actualContent = null;

  @Override
  public void print(byte[] content) {
    actualContent = content;
  }

  @Override
  public void count(long rowCount) {
    actualCount = rowCount;
  }
}
;
