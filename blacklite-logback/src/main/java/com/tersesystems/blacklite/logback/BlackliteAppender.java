package com.tersesystems.blacklite.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import com.tersesystems.blacklite.*;
import com.tersesystems.blacklite.archive.Archiver;
import com.tersesystems.blacklite.archive.DefaultArchiver;
import java.util.Properties;

/** A logback appender using blacklite as a backend. */
public class BlackliteAppender extends UnsynchronizedAppenderBase<ILoggingEvent>
    implements EntryStoreConfig {
  private final EntryStoreConfig config = new DefaultEntryStoreConfig();

  private Encoder<ILoggingEvent> encoder;
  private EntryWriter entryWriter;
  private Archiver archiver = new DefaultArchiver();

  public Encoder<ILoggingEvent> getEncoder() {
    return encoder;
  }

  public void setEncoder(Encoder<ILoggingEvent> encoder) {
    this.encoder = encoder;
  }

  public void start() {
    try {
      StatusReporter statusReporter = new LogbackStatusReporter(this);
      this.entryWriter = new AsyncEntryWriter(statusReporter, config, archiver, name);

      super.start();
    } catch (Exception e) {
      addError("Cannot start appender!", e);
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void stop() {
    super.stop();
    try {
      entryWriter.close();
    } catch (Exception e) {
      addError("Stopping threads", e);
    }
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
  public String getUrl() {
    return config.getUrl();
  }

  @Override
  public void setUrl(String url) {
    config.setUrl(url);
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
}
