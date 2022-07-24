package com.tersesystems.blacklite.log4j2;

import com.tersesystems.blacklite.archive.Archiver;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.Core;

import com.tersesystems.blacklite.archive.NoOpArchiver;

@Plugin(name = "NoOpArchiver", category = Core.CATEGORY_NAME, printObject = true)
public class Log4J2NoOpArchiver extends NoOpArchiver implements Archiver {

  Log4J2NoOpArchiver() {
  }

  @PluginFactory
  public static NoOpArchiver createArchiver() {
    return new Log4J2NoOpArchiver();
  }
}
