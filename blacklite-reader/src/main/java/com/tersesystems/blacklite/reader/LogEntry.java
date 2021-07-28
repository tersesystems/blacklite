package com.tersesystems.blacklite.reader;

public class LogEntry {

  private long epochSecs;
  private int nanos;
  private byte[] content;
  private int level;

  public LogEntry() {
  }

  /**
   * Writes values to this log entry without a new allocation.
   */
  public LogEntry write(long epochSecs, int nanos, int level, byte[] content) {
    this.epochSecs = epochSecs;
    this.nanos = nanos;
    this.level = level;
    this.content = content;
    return this;
  }

  /**
   * Allocates a new LogEntry and writes the current values to it.  Note that
   * you get a new byte array for the content as well.
   *
   * Useful for immutable APIs / multi thread usage.
   */
  public LogEntry copy() {
    LogEntry newInstance = new LogEntry();
    byte[] newContent = new byte[content.length];
    System.arraycopy(content, 0, newContent, 0, content.length);
    return newInstance.write(epochSecs, nanos, level, newContent);
  }

  public long getEpochSecs() {
    return epochSecs;
  }

  public int getNanos() {
    return nanos;
  }

  public int getLevel() {
    return level;
  }

  public byte[] getContent() {
    return content;
  }
}
