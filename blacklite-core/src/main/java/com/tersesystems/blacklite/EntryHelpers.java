package com.tersesystems.blacklite;

import java.time.Instant;

/** */
public final class EntryHelpers {

  private EntryHelpers() {}

  public static EntryHelpers instance() {
    return LazyHolder.INSTANCE;
  }

  /**
   * Gets the epoch second portion from the milliseconds from epoch.
   *
   * @param epochMilli the milliseconds since epoch.
   * @return the number of seconds since the epoch.
   */
  public long epochSecondFromMillis(long epochMilli) {
    return Math.floorDiv(epochMilli, (long) 1000);
  }

  /**
   * Gets the nanosecond portion from the the milliseconds from epoch.
   *
   * @param epochMilli the milliseconds since epoch.
   * @return the number of nanoseconds inside the epoch milli.
   */
  public int nanosFromMillis(long epochMilli) {
    return (int) Math.floorMod(epochMilli, (long) 1000) * 1_000_000;
  }

  /**
   * Creates an instant from an entry timestamp, a wrapper around Instant.ofEpochSecond.
   *
   * <p>NOTE: This does an allocation of {@code new Instant()}.
   *
   * @param epochSecond the seconds since epoch.
   * @param nanos the nanoseconds inside the instant.
   * @return the Instant with the given seconds.
   */
  public Instant instantFromTimestamp(long epochSecond, int nanos) {
    return Instant.ofEpochSecond(epochSecond, nanos);
  }

  private static class LazyHolder {
    static final EntryHelpers INSTANCE = new EntryHelpers();
  }
}
