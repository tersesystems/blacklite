package com.tersesystems.blacklite.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import com.tersesystems.blacklite.*;
import com.tersesystems.blacklite.archive.Archiver;
import com.tersesystems.blacklite.archive.DeletingArchiver;
import com.tersesystems.blacklite.archive.TriggeringPolicy;
import java.util.Properties;

/** A logback appender using blacklite as a backend. */
public class BlackliteAppender extends UnsynchronizedAppenderBase<ILoggingEvent>
    implements EntryStoreConfig, LoggerContextListener {
  private final EntryStoreConfig config = new DefaultEntryStoreConfig();

  private Encoder<ILoggingEvent> encoder;
  private EntryWriter entryWriter;
  private Archiver archiver;

  private TriggeringPolicy triggeringPolicy;

  public Encoder<ILoggingEvent> getEncoder() {
    return encoder;
  }

  public void setEncoder(Encoder<ILoggingEvent> encoder) {
    this.encoder = encoder;
  }

  public synchronized void start() {
    if (isStarted()) {
      return;
    }

    try {
      StatusReporter statusReporter = new LogbackStatusReporter(this);

      addInfo("Connecting with config " + config);
      if (config.getFile() == null || config.getFile().isEmpty()) {
        addError("Required 'file' configuration setting is missing!");
        return;
      }

      if (this.archiver == null) {
        this.archiver = new DeletingArchiver();
      }
      this.entryWriter = new AsyncEntryWriter(statusReporter, config, archiver, name);

      super.start();
    } catch (Exception e) {
      addError("Cannot start appender!", e);
      throw new IllegalStateException(e);
    }
  }

  @Override
  public synchronized void stop() {
    if (!isStarted()) {
      return;
    }

    close();
    super.stop();
  }

  @Override
  protected void append(ILoggingEvent event) {
    EntryHelpers helpers = EntryHelpers.instance();
    long epochMillis = event.getTimeStamp();

    long epochSeconds = helpers.epochSecondFromMillis(epochMillis);
    int nanos = helpers.nanosFromMillis(epochMillis);
    int level = event.getLevel().toInt();
    byte[] encode = encoder.encode(event);
    entryWriter.write(epochSeconds, nanos, level, encode);
  }

  @Override
  public String getFile() {
    return config.getFile();
  }

  @Override
  public void setFile(String file) {
    config.setFile(file);
  }

  @Override
  public Properties getProperties() {
    return config.getProperties();
  }

  @Override
  public void setProperties(Properties properties) {
    config.setProperties(properties);
  }

  @Override
  public long getBatchInsertSize() {
    return config.getBatchInsertSize();
  }

  @Override
  public void setBatchInsertSize(long batchInsertSize) {
    config.setBatchInsertSize(batchInsertSize);
  }

  public Archiver getArchiver() {
    return archiver;
  }

  public void setArchiver(Archiver archiver) {
    this.archiver = archiver;
  }

  void close() {
    try {
      addInfo("Closing entryWriter " + entryWriter);
      entryWriter.close();
      addInfo("Closed entryWriter " + entryWriter);
      archiver = null;
      entryWriter = null;
    } catch (Exception e) {
      addError("Stopping threads", e);
    }
  }

  @Override
  public boolean isResetResistant() {
    return false;
  }

  @Override
  public void onStart(LoggerContext context) {
    addInfo("onStart");
  }

  @Override
  public void onReset(LoggerContext context) {
    addInfo("onReset() method called [" + this + "]");
    close();
  }

  @Override
  public void onStop(LoggerContext context) {
    addInfo("onStop");
  }

  @Override
  public void onLevelChange(Logger logger, Level level) {}
}
