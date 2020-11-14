package com.tersesystems.blacklite.reader;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;

public class DateParserTest {
  TimeZone utc = TimeZone.getTimeZone(ZoneOffset.UTC);
  TimeZone pst = TimeZone.getTimeZone("PST");

  @Test
  public void testParseAbsolute() {
    DateParser dateParser = new DateParser(utc);
    final Optional<Instant> optActual = dateParser.parse("11/9/2020 5 am");
    Instant actual = optActual.get();
    final Instant expected = Instant.parse("2020-11-09T05:00:00.000Z");
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void testParseTimezone() {
    DateParser dateParser = new DateParser(pst);
    final Optional<Instant> optActual = dateParser.parse("11/9/2020 5 am");
    Instant actual = optActual.get();
    final Instant expected = Instant.parse("2020-11-09T13:00:00Z"); // 7 or 8 hours?
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void testParseRelative() {
    DateParser dateParser = new DateParser(utc);
    final Optional<Instant> optActual = dateParser.parse("five seconds ago");
    Instant actual = optActual.get();
    assertThat(actual).isBeforeOrEqualTo(Instant.now().minusSeconds(5));
  }

  @Test
  public void testParseFailure() {
    DateParser dateParser = new DateParser(utc);
    final Optional<Instant> optActual = dateParser.parse("herp derp");
    assertThat(optActual).isEmpty();
  }
}
