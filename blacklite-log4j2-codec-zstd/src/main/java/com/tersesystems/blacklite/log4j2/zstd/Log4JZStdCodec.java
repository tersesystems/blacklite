package com.tersesystems.blacklite.log4j2.zstd;

import com.tersesystems.blacklite.codec.zstd.ZStdCodec;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

@Plugin(name = "ZStdCodec", category = Core.CATEGORY_NAME, printObject = true)
public class Log4JZStdCodec extends ZStdCodec {

  public Log4JZStdCodec(int level) {
    setLevel(level);
  }

  @PluginFactory
  public static Log4JZStdCodec createCodec(
          @PluginAttribute(value = "level", defaultInt = 3) int level) {
    return new Log4JZStdCodec(level);
  }
}
