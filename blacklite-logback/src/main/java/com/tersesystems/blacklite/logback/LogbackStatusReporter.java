package com.tersesystems.blacklite.logback;

import ch.qos.logback.core.spi.ContextAware;
import com.tersesystems.blacklite.StatusReporter;

public class LogbackStatusReporter implements StatusReporter {

  private final ContextAware parent;

  public LogbackStatusReporter(ContextAware parent) {
    this.parent = parent;
  }

  @Override
  public void addError(String message, Throwable e) {
    parent.addError(message, e);
  }

  @Override
  public void addInfo(String message) {
    parent.addInfo(message);
  }

  @Override
  public void addInfo(String msg, Throwable ex) {
    parent.addInfo(msg, ex);
  }

  @Override
  public void addWarn(String msg) {
    parent.addWarn(msg);
  }

  @Override
  public void addWarn(String msg, Throwable ex) {
    parent.addWarn(msg, ex);
  }

  @Override
  public void addError(String msg) {
    parent.addError(msg);
  }
}
