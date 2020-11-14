package com.tersesystems.blacklite.log4j2.zstd;

import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

@Plugin(name = "FileRepository", category = Core.CATEGORY_NAME, printObject = true)
public class Log4JDictFileRepository
    extends com.tersesystems.blacklite.codec.zstd.ZstdDictFileRepository {

  Log4JDictFileRepository(String file) {
    setFile(file);
  }

  @PluginFactory
  public static Log4JDictFileRepository createRepository(@PluginAttribute("file") String file) {
    return new Log4JDictFileRepository(file);
  }
}
