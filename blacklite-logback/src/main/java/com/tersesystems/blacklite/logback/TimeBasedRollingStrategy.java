package com.tersesystems.blacklite.logback;

import static ch.qos.logback.core.CoreConstants.UNBOUNDED_TOTAL_SIZE_CAP;
import static ch.qos.logback.core.CoreConstants.UNBOUND_HISTORY;

import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.rolling.helper.*;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.util.FileSize;
import com.tersesystems.blacklite.archive.Archiver;
import com.tersesystems.blacklite.archive.RollingStrategy;
import java.util.Date;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TimeBasedRollingStrategy extends ContextAwareBase
    implements RollingStrategy, LifeCycle {
  static final String FNP_NOT_SET =
      "The FileNamePattern option must be set before using TimeBasedRollingPolicy.";

  FileNamePattern fileNamePattern;

  private String fileNamePatternStr;

  private final RenameUtil renameUtil = new RenameUtil();
  Future<?> cleanUpFuture;

  private int maxHistory = UNBOUND_HISTORY;

  private Archiver archiver;
  private ArchiveRemover archiveRemover;

  boolean cleanHistoryOnStart = false;
  private FileSize totalSizeCap = new FileSize(UNBOUNDED_TOTAL_SIZE_CAP);
  private volatile boolean started;

  @Override
  public void start() {
    renameUtil.setContext(this.context);

    fileNamePattern = getFilenamePattern();

    addInfo("Will use the pattern " + fileNamePattern + " for the active file");

    if (maxHistory != UNBOUND_HISTORY) {
      archiveRemover = getArchiveRemover();
      if (cleanHistoryOnStart) {
        addInfo("Cleaning on start up");
        Date now = new Date(getCurrentTime());
        cleanUpFuture = archiveRemover.cleanAsynchronously(now);
      }
    } else if (!isUnboundedTotalSizeCap()) {
      addWarn(
          "'maxHistory' is not set, ignoring 'totalSizeCap' option with value ["
              + totalSizeCap
              + "]");
    }
    started = true;
  }

  private FileNamePattern getFilenamePattern() {
    if (fileNamePatternStr != null) {
      return new FileNamePattern(fileNamePatternStr, this.context);
    } else {
      addWarn(FNP_NOT_SET);
      addWarn(CoreConstants.SEE_FNP_NOT_SET);
      throw new IllegalStateException(FNP_NOT_SET + CoreConstants.SEE_FNP_NOT_SET);
    }
  }

  @Override
  public void stop() {
    if (!started) return;
    waitForAsynchronousJobToStop(cleanUpFuture);
    started = false;
  }

  @Override
  public boolean isStarted() {
    return started;
  }

  @Override
  public void rollover(Archiver archiver) {
    String elapsedPeriodsFileName = getElapsedPeriodsFileName();
    if (archiver != null) {
      renameUtil.rename(archiver.getFile(), elapsedPeriodsFileName);
    }

    if (archiveRemover != null) {
      Date now = new Date(getCurrentTime());
      this.cleanUpFuture = archiveRemover.cleanAsynchronously(now);
    }
  }

  public int getMaxHistory() {
    return maxHistory;
  }

  public void setMaxHistory(int maxHistory) {
    this.maxHistory = maxHistory;
  }

  public boolean isCleanHistoryOnStart() {
    return cleanHistoryOnStart;
  }

  public void setCleanHistoryOnStart(boolean cleanHistoryOnStart) {
    this.cleanHistoryOnStart = cleanHistoryOnStart;
  }

  public void setTotalSizeCap(FileSize totalSizeCap) {
    addInfo("setting totalSizeCap to " + totalSizeCap.toString());
    this.totalSizeCap = totalSizeCap;
  }

  public void setParent(Archiver archiver) {
    this.archiver = archiver;
  }

  public String getParentsRawFileProperty() {
    return archiver.getFile();
  }

  public void setFileNamePattern(String fileNamePatternStr) {
    this.fileNamePatternStr = fileNamePatternStr;
  }

  public long getCurrentTime() {
    return System.currentTimeMillis();
  }

  protected boolean isUnboundedTotalSizeCap() {
    return totalSizeCap.getSize() == UNBOUNDED_TOTAL_SIZE_CAP;
  }

  private void waitForAsynchronousJobToStop(Future<?> aFuture) {
    if (aFuture != null) {
      try {
        aFuture.get(30, TimeUnit.SECONDS);
      } catch (TimeoutException e) {
        addError("Timeout while waiting for clean-up job to finish", e);
      } catch (Exception e) {
        addError("Unexpected exception while waiting for clean-up job to finish", e);
      }
    }
  }

  private String getElapsedPeriodsFileName() {
    return fileNamePattern.convert(new Date(getCurrentTime()));
  }

  protected ArchiveRemover getArchiveRemover() {
    DateTokenConverter<Object> dtc = fileNamePattern.getPrimaryDateTokenConverter();
    RollingCalendar rc = new RollingCalendar(dtc.getDatePattern());
    ArchiveRemover archiveRemover = new TimeBasedArchiveRemover(fileNamePattern, rc);
    archiveRemover.setContext(this.context);
    archiveRemover.setMaxHistory(maxHistory);
    archiveRemover.setTotalSizeCap(totalSizeCap.getSize());
    return archiveRemover;
  }
}
