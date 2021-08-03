package com.tersesystems.blacklite.logback;

import static ch.qos.logback.core.CoreConstants.CODES_URL;

import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.rolling.RolloverFailure;
import ch.qos.logback.core.rolling.helper.FileNamePattern;
import ch.qos.logback.core.rolling.helper.IntegerTokenConverter;
import ch.qos.logback.core.rolling.helper.RenameUtil;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.LifeCycle;
import com.tersesystems.blacklite.archive.FileArchiver;
import com.tersesystems.blacklite.archive.RollingStrategy;
import java.io.File;

public class FixedWindowRollingStrategy extends ContextAwareBase
    implements RollingStrategy, LifeCycle {
  static final String FNP_NOT_SET =
      "The \"FileNamePattern\" property must be set before using FixedWindowRollingPolicy. ";
  static final String SEE_PARENT_FN_NOT_SET =
      "Please refer to " + CODES_URL + "#fwrp_parentFileName_not_set";

  /** It's almost always a bad idea to have a large window size, say over 20. */
  private static final int MAX_WINDOW_SIZE = 20;

  FileNamePattern fileNamePattern;
  // fileNamePatternStr is always slashified, see setter
  protected String fileNamePatternStr;
  int maxIndex;
  int minIndex;

  RenameUtil util = new RenameUtil();

  private volatile boolean started;

  public void setFileNamePattern(String fnp) {
    fileNamePatternStr = fnp;
  }

  public String getFileNamePattern() {
    return fileNamePatternStr;
  }

  public int getMaxIndex() {
    return maxIndex;
  }

  public int getMinIndex() {
    return minIndex;
  }

  public void setMaxIndex(int maxIndex) {
    this.maxIndex = maxIndex;
  }

  public void setMinIndex(int minIndex) {
    this.minIndex = minIndex;
  }

  @Override
  public void start() {
    util.setContext(this.context);

    // find out period from the filename pattern
    if (fileNamePatternStr != null) {
      fileNamePattern = new FileNamePattern(fileNamePatternStr, this.context);
    } else {
      addWarn(FNP_NOT_SET);
      addWarn(CoreConstants.SEE_FNP_NOT_SET);
      throw new IllegalStateException(FNP_NOT_SET + CoreConstants.SEE_FNP_NOT_SET);
    }

    if (maxIndex < minIndex) {
      addWarn("MaxIndex (" + maxIndex + ") cannot be smaller than MinIndex (" + minIndex + ").");
      addWarn("Setting maxIndex to equal minIndex.");
      maxIndex = minIndex;
    }

    final int maxWindowSize = getMaxWindowSize();
    if ((maxIndex - minIndex) > maxWindowSize) {
      addWarn("Large window sizes are not allowed.");
      maxIndex = minIndex + maxWindowSize;
      addWarn("MaxIndex reduced to " + maxIndex);
    }

    IntegerTokenConverter itc = fileNamePattern.getIntegerTokenConverter();

    if (itc == null) {
      throw new IllegalStateException(
          "FileNamePattern ["
              + fileNamePattern.getPattern()
              + "] does not contain a valid IntegerToken");
    }

    addInfo("filename pattern = " + fileNamePattern);
    started = true;
  }

  public int getMaxWindowSize() {
    return MAX_WINDOW_SIZE;
  }

  @Override
  public void rollover(FileArchiver archiver) throws RolloverFailure {
    addInfo(String.format("rollover: enter minIndex = %d maxIndex = %d", minIndex, maxIndex));

    // Inside this method it is guaranteed that the hereto active log file is
    // closed.
    // If maxIndex <= 0, then there is no file renaming to be done.
    if (maxIndex >= 0) {
      // Delete the oldest file, to keep Windows happy.
      File file = new File(fileNamePattern.convertInt(maxIndex));

      if (file.exists()) {
        addInfo("rollover: deleting oldest file " + file);
        file.delete();
      }

      // Map {(maxIndex - 1), ..., minIndex} to {maxIndex, ..., minIndex+1}
      for (int i = maxIndex - 1; i >= minIndex; i--) {
        String toRenameStr = fileNamePattern.convertInt(i);
        File toRename = new File(toRenameStr);
        // no point in trying to rename an  non-existent file
        if (toRename.exists()) {
          final String s = fileNamePattern.convertInt(i + 1);
          addInfo(String.format("rollover: renaming %s to %s", toRenameStr, s));
          util.rename(toRenameStr, s);
        } else {
          addInfo("rollover: Skipping roll-over for non-existent file " + toRenameStr);
        }
      }

      String minFilename = fileNamePattern.convertInt(minIndex);
      addInfo(
          String.format("rollover: finally renaming %s to %s", archiver.getFile(), minFilename));
      util.rename(archiver.getFile(), minFilename);
    }
  }

  @Override
  public void stop() {
    started = false;
  }

  @Override
  public boolean isStarted() {
    return started;
  }
}
