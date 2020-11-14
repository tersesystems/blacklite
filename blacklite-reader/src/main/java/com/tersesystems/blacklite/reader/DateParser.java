package com.tersesystems.blacklite.reader;

import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

/** A date parser wrapping natural language date parsing using Natty. */
public class DateParser {

  private final TimeZone timezone;

  public DateParser(TimeZone timezone) {
    this.timezone = timezone;
  }

  public Optional<Instant> parse(String dateString) {
    Parser parser = new Parser(timezone);
    final List<DateGroup> groups = parser.parse(dateString);
    for (DateGroup group : groups) {
      List<java.util.Date> dates = group.getDates();
      if (dates.isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(dates.get(0).toInstant());
    }
    // Natty doesn't return an error status or throw on parser failure
    return Optional.empty();
  }
}
