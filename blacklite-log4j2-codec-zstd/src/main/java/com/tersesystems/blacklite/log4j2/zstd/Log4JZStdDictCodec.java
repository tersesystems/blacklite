package com.tersesystems.blacklite.log4j2.zstd;

import com.tersesystems.blacklite.codec.zstd.ZstdDictCodec;
import com.tersesystems.blacklite.codec.zstd.ZstdDictRepository;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

@Plugin(name = "ZStdDictCodec", category = Core.CATEGORY_NAME, printObject = true)
public class Log4JZStdDictCodec extends ZstdDictCodec {

  Log4JZStdDictCodec(int level, int sampleSize, int dictSize, ZstdDictRepository repository) {
    setLevel(level);
    setSampleSize(sampleSize);
    setDictSize(dictSize);
    setRepository(repository);
  }

  @PluginFactory
  public static Log4JZStdDictCodec createCodec(
      @PluginAttribute(value = "level", defaultInt = 3) int level,
      @PluginAttribute(value = "sampleSize", defaultInt = 100_000 * 1024) int sampleSize,
      @PluginAttribute(value = "dictSize", defaultInt = 10485760) int dictSize,
      @PluginElement("repository") ZstdDictRepository repository) {
    return new Log4JZStdDictCodec(level, sampleSize, dictSize, repository);
  }
}
