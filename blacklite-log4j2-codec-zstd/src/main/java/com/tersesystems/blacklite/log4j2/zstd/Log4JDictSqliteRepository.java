package com.tersesystems.blacklite.log4j2.zstd;

import com.tersesystems.blacklite.codec.zstd.ZStdDictSqliteRepository;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

@Plugin(name = "SqliteRepository", category = Core.CATEGORY_NAME, printObject = true)
public class Log4JDictSqliteRepository extends ZStdDictSqliteRepository {
  Log4JDictSqliteRepository(String url) {
    setFile(url);
  }

  @PluginFactory
  public static Log4JDictSqliteRepository createRepository(@PluginAttribute("url") String url) {
    return new Log4JDictSqliteRepository(url);
  }
}
