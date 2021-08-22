package com.tersesystems.blacklite.reader;

public class LogEntry {

  private long epochSecs;
  private int nanos;
  private byte[] content;
  private int level;

  public LogEntry() {
  }

  /**
   * Set values to this log entry without a new allocation.
   *
   * @param epochSecs the seconds from epoch
   * @param nanos the nanoseconds in second
   * @param level the logging level
   * @param content the content of the entry
   * @return the same instance of log entry with new settings.
   */
  public LogEntry set(long epochSecs, int nanos, int level, byte[] content) {
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
   *
   * @return a new instance of log entry with the same data.
   */
  public LogEntry copy() {
    LogEntry newInstance = new LogEntry();
    byte[] newContent = new byte[content.length];
    System.arraycopy(content, 0, newContent, 0, content.length);
    return newInstance.set(epochSecs, nanos, level, newContent);
  }

  /**
   * @return the timestamp represented as the number of seconds since epoch.
   */
  public long getEpochSecs() {
    return epochSecs;
  }

  /**
   * @return the nanosecond portion of the time, within the second.
   */
  public int getNanos() {
    return nanos;
  }

  /**
   * @return the logging level.
   */
  public int getLevel() {
    return level;
  }

  /**
   * @return the content of the entry.
   */
  public byte[] getContent() {
    return content;
  }
}
