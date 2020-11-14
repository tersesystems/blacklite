package com.tersesystems.blacklite;

import java.util.Arrays;
import java.util.Objects;

public class Entry {
  public final long rowId;
  public long epochSecond;
  public int nanos;
  public int level;
  public byte[] content;

  public Entry(long rowId, long epochSecond, int nanos, int level, byte[] content) {
    this.rowId = rowId;
    this.epochSecond = epochSecond;
    this.nanos = nanos;
    this.level = level;
    this.content = content;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Entry entry = (Entry) o;
    return epochSecond == entry.epochSecond
        && nanos == entry.nanos
        && level == entry.level
        && Arrays.equals(content, entry.content);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(epochSecond, nanos, level);
    result = 31 * result + Arrays.hashCode(content);
    return result;
  }

  @Override
  public String toString() {
    return "Entry{"
        + "rowId="
        + rowId
        + ", epochSecond="
        + epochSecond
        + ", nanos="
        + nanos
        + ", level="
        + level
        + ", content="
        + bytesToHex(content)
        + '}';
  }

  private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

  private static String bytesToHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = HEX_ARRAY[v >>> 4];
      hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
    }
    return new String(hexChars);
  }
}
