package com.tersesystems.blacklite.log4j2;

import com.tersesystems.blacklite.archive.Archiver;
import com.tersesystems.blacklite.archive.DeletingArchiver;
import com.tersesystems.blacklite.archive.TriggeringPolicy;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

@Plugin(name = "DeletingArchiver", category = Core.CATEGORY_NAME, printObject = true)
public class Log4J2DeletingArchiver extends DeletingArchiver implements Archiver {

  Log4J2DeletingArchiver(long archiveAfterRows, TriggeringPolicy triggeringPolicy) {
    setArchiveAfterRows(archiveAfterRows);
    setTriggeringPolicy(triggeringPolicy);
  }

  @PluginFactory
  public static Log4J2DeletingArchiver createArchiver(
    @PluginAttribute(value = "archiveAfterRows", defaultInt = 10000) long archiveAfterRows,
    @PluginElement("triggeringPolicy") TriggeringPolicy triggeringPolicy
  ) {
    return new Log4J2DeletingArchiver(archiveAfterRows, triggeringPolicy);
  }
}
