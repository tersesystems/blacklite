package com.tersesystems.blacklite.log4j2;

import com.tersesystems.blacklite.*;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import com.tersesystems.blacklite.archive.Archiver;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.time.Instant;

@Plugin(
    name = "Blacklite",
    category = Core.CATEGORY_NAME,
    elementType = Appender.ELEMENT_TYPE,
    printObject = true)
public class Log4JBlackliteAppender extends AbstractAppender {

  private final EntryWriter entryWriter;

  Log4JBlackliteAppender(
      String name,
      Filter filter,
      Layout<? extends Serializable> layout,
      boolean ignoreExceptions,
      Property[] properties,
      Archiver archiver,
      EntryStoreConfig config)
      throws SQLException {
    super(name, filter, layout, ignoreExceptions, properties);
    if (layout == null) {
      throw new IllegalStateException("Null layout");
    }
    StatusReporter statusReporter = new Log4JStatusReporter(this.getHandler());
    this.entryWriter = new AsyncEntryWriter(statusReporter, config, archiver, name);
  }

  @PluginFactory
  public static Log4JBlackliteAppender createAppender(
      @PluginAttribute("name") @Required(message = "No name provided for BlackliteAppender")
          final String name,
      @PluginAttribute("file") final String file,
      @PluginAttribute(value = "batchInsertSize", defaultInt = DefaultEntryStoreConfig.BATCH_INSERT_SIZE) final int batchInsertSize,
      @PluginAttribute(value = "maxCapacity", defaultInt = DefaultEntryStoreConfig.MAX_CAPACITY) final int maxCapacity,
      @PluginElement("archiver") final Archiver archiver,
      @PluginElement("layout") final Layout<? extends Serializable> layout,
      @PluginElement("filter") final Filter filter)
      throws SQLException {
    if (name == null) {
      LOGGER.error("No name provided for BlackliteAppender");
      return null;
    }

    if (file == null) {
      LOGGER.error("No file provided for BlackliteAppender");
      return null;
    }

    EntryStoreConfig config = new DefaultEntryStoreConfig();

    config.setFile(file);
    config.setBatchInsertSize(batchInsertSize);
    config.setMaxCapacity(maxCapacity);
    //config.setProperties(additionalProperties);
    LOGGER.info("Connecting with config " + config);

    boolean ignoreExceptions = true;
    Property[] properties = {};

    return new Log4JBlackliteAppender(
        name, filter, layout, ignoreExceptions, properties, archiver, config);
  }

  @Override
  public void append(final LogEvent event) {
    Layout<? extends Serializable> layout = getLayout();

    Instant instant = event.getInstant();
    int level = event.getLevel().intLevel();
    byte[] entry = layout.toByteArray(event);
    long epochSeconds = instant.getEpochSecond();
    int nanos = instant.getNanoOfSecond();
    entryWriter.write(epochSeconds, nanos, level, entry);
  }

  @Override
  public boolean stop(final long timeout, final TimeUnit timeUnit) {
    setStopping();
    final boolean stopped = super.stop(timeout, timeUnit, false);
    if (entryWriter != null) {
      try {
        entryWriter.close();
      } catch (Exception e) {
        this.getHandler().error("Cannot close", e);
      }
    }
    setStopped();
    return stopped;
  }

  static class Log4JStatusReporter implements StatusReporter {

    private final ErrorHandler handler;

    Log4JStatusReporter(ErrorHandler handler) {
      this.handler = handler;
    }

    @Override
    public void addInfo(String msg) {
      LOGGER.info(msg);
    }

    @Override
    public void addInfo(String msg, Throwable ex) {
      LOGGER.info(msg, ex);
    }

    @Override
    public void addWarn(String msg) {
      LOGGER.warn(msg);
    }

    @Override
    public void addWarn(String msg, Throwable ex) {
      LOGGER.warn(msg, ex);
    }

    @Override
    public void addError(String msg) {
      handler.error(msg);
    }

    @Override
    public void addError(String msg, Throwable ex) {
      handler.error(msg, ex);
    }
  }
}
