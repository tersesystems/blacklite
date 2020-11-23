package com.tersesystems.blacklite.log4j2;

import com.tersesystems.blacklite.archive.RowBasedTriggeringPolicy;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

@Plugin(name = "RowBasedTriggeringPolicy", category = Core.CATEGORY_NAME, printObject = true)
public class Log4JRowBasedTriggeringPolicy extends RowBasedTriggeringPolicy {

  public Log4JRowBasedTriggeringPolicy(long maximumNumRows) {
    setMaximumNumRows(maximumNumRows);
  }

  @PluginFactory
  public static Log4JRowBasedTriggeringPolicy createTriggeringPolicy(
      @PluginAttribute("maximumNumRows") final long maximumNumRows) {
    return new Log4JRowBasedTriggeringPolicy(maximumNumRows);
  }
}
