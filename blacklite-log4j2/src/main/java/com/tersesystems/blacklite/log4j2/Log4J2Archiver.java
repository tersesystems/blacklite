package com.tersesystems.blacklite.log4j2;

import static java.util.Objects.requireNonNull;

import com.tersesystems.blacklite.archive.RollingArchiver;
import com.tersesystems.blacklite.archive.RollingStrategy;
import com.tersesystems.blacklite.archive.TriggeringPolicy;
import com.tersesystems.blacklite.codec.Codec;
import com.tersesystems.blacklite.codec.identity.IdentityCodec;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

@Plugin(name = "Archiver", category = Core.CATEGORY_NAME, printObject = true)
public class Log4J2Archiver extends RollingArchiver {

  Log4J2Archiver(
      String file,
      long maximumNumRows,
      Codec codec,
      RollingStrategy rollingStrategy,
      TriggeringPolicy triggeringPolicy) {
    setFile(file);
    setArchiveAfterRows(maximumNumRows);
    setCodec(codec == null ? new IdentityCodec() : codec);

    setRollingStrategy(requireNonNull(rollingStrategy, "Null rollingStrategy"));
    setTriggeringPolicy(requireNonNull(triggeringPolicy, "Null triggeringPolicy"));
  }

  @PluginFactory
  public static Log4J2Archiver createArchiver(
      @PluginAttribute("file") @Required(message = "No file provided for Archiver") String file,
      @PluginAttribute(value = "maximumNumRows", defaultInt = 10000) long maximumNumRows,
      @PluginElement("codec") Codec codec,
      @PluginElement("rollingStrategy") RollingStrategy rollingStrategy,
      @PluginElement("triggeringPolicy") TriggeringPolicy triggeringPolicy) {
    return new Log4J2Archiver(file, maximumNumRows, codec, rollingStrategy, triggeringPolicy);
  }
}
