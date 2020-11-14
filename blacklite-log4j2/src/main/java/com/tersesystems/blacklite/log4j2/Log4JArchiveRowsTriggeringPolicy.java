package com.tersesystems.blacklite.log4j2;

import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

@Plugin(name = "ArchiveRowsTriggeringPolicy", category = Core.CATEGORY_NAME, printObject = true)
public class Log4JArchiveRowsTriggeringPolicy
    extends com.tersesystems.blacklite.archive.ArchiveRowsTriggeringPolicy {

  public Log4JArchiveRowsTriggeringPolicy(long maximumNumRows) {
    setMaximumNumRows(maximumNumRows);
  }

  @PluginFactory
  public static Log4JArchiveRowsTriggeringPolicy createTriggeringPolicy(
      @PluginAttribute("maximumNumRows") final long maximumNumRows) {
    return new Log4JArchiveRowsTriggeringPolicy(maximumNumRows);
  }
}
