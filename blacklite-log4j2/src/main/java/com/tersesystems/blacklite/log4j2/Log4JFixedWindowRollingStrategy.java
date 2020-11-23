package com.tersesystems.blacklite.log4j2;

import com.tersesystems.blacklite.archive.FileArchiver;
import com.tersesystems.blacklite.archive.RollingStrategy;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.appender.rolling.AbstractRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.PatternProcessor;
import org.apache.logging.log4j.core.appender.rolling.RollingFileManager;
import org.apache.logging.log4j.core.appender.rolling.RolloverDescription;
import org.apache.logging.log4j.core.appender.rolling.action.Action;
import org.apache.logging.log4j.core.appender.rolling.action.FileRenameAction;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.pattern.NotANumber;
import org.apache.logging.log4j.status.StatusLogger;

@Plugin(name = "FixedWindowRollingStrategy", category = Core.CATEGORY_NAME, printObject = true)
public class Log4JFixedWindowRollingStrategy extends AbstractRolloverStrategy
    implements RollingStrategy {

  protected static final Logger LOGGER = StatusLogger.getLogger();

  private final int maxIndex;
  private final int minIndex;
  private final boolean useMax;
  private final PatternProcessor patternProcessor;

  Log4JFixedWindowRollingStrategy(
      int max, int min, String filePattern, StrSubstitutor strSubstitutor) {
    super(strSubstitutor);
    this.maxIndex = max;
    this.minIndex = min;
    this.useMax = false;
    this.patternProcessor = new PatternProcessor(filePattern);
  }

  @PluginFactory
  public static Log4JFixedWindowRollingStrategy createRollingStrategy(
      @PluginAttribute("max") final int max,
      @PluginAttribute("min") final int min,
      @PluginAttribute("filePattern") final String filePattern) {
    final StrSubstitutor nonNullStrSubstitutor = new StrSubstitutor();
    return new Log4JFixedWindowRollingStrategy(max, min, filePattern, nonNullStrSubstitutor);
  }

  @Override
  public void rollover(FileArchiver archiver) {
    int fileIndex;
    final StringBuilder buf = new StringBuilder(255);
    if (minIndex == Integer.MIN_VALUE) {
      final String pattern = patternProcessor.getPattern();
      patternProcessor.formatFileName(strSubstitutor, buf, NotANumber.NAN);
      final String fileName = archiver.getFile();
      final SortedMap<Integer, Path> eligibleFiles =
          getEligibleFiles(fileName, buf.toString(), pattern, true);
      fileIndex = eligibleFiles.size() > 0 ? eligibleFiles.lastKey() + 1 : 1;
      patternProcessor.formatFileName(strSubstitutor, buf, fileIndex);
    } else {
      if (maxIndex < 0) {
        return;
      }
      final long startNanos = System.nanoTime();
      fileIndex = purge(minIndex, maxIndex, archiver);
      if (fileIndex < 0) {
        return;
      }
      patternProcessor.formatFileName(strSubstitutor, buf, fileIndex);
      if (LOGGER.isTraceEnabled()) {
        final double durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
        LOGGER.trace("purge() took {} milliseconds", durationMillis);
      }
    }

    final String currentFileName = archiver.getFile();
    String renameTo = buf.toString();
    if (currentFileName.equals(renameTo)) {
      LOGGER.warn("Attempt to rename file {} to itself will be ignored", currentFileName);
      return;
    }

    final FileRenameAction renameAction =
        new FileRenameAction(new File(currentFileName), new File(renameTo), false);
    renameAction.execute();
  }

  private int purge(final int lowIndex, final int highIndex, FileArchiver archiver) {
    return useMax
        ? purgeAscending(lowIndex, highIndex, archiver)
        : purgeDescending(lowIndex, highIndex, archiver);
  }

  protected int purgeAscending(final int lowIndex, final int highIndex, FileArchiver archiver) {
    final SortedMap<Integer, Path> eligibleFiles = getEligibleFiles(archiver, true);
    final int maxFiles = highIndex - lowIndex + 1;

    boolean renameFiles = !eligibleFiles.isEmpty() && eligibleFiles.lastKey() >= maxIndex;
    while (eligibleFiles.size() >= maxFiles) {
      try {
        LOGGER.debug("Eligible files: {}", eligibleFiles);
        final Integer key = eligibleFiles.firstKey();
        LOGGER.debug("Deleting {}", eligibleFiles.get(key).toFile().getAbsolutePath());
        Files.delete(eligibleFiles.get(key));
        eligibleFiles.remove(key);
        renameFiles = true;
      } catch (final IOException ioe) {
        LOGGER.error("Unable to delete {}, {}", eligibleFiles.firstKey(), ioe.getMessage(), ioe);
        break;
      }
    }
    if (renameFiles) {
      final StringBuilder buf = new StringBuilder();
      for (final Map.Entry<Integer, Path> entry : eligibleFiles.entrySet()) {
        buf.setLength(0);
        // LOG4J2-531: directory scan & rollover must use same format
        patternProcessor.formatFileName(strSubstitutor, buf, entry.getKey() - 1);
        final String currentName = entry.getValue().toFile().getName();
        String renameTo = buf.toString();
        final int suffixLength = suffixLength(renameTo);
        if (suffixLength > 0 && suffixLength(currentName) == 0) {
          renameTo = renameTo.substring(0, renameTo.length() - suffixLength);
        }
        final Action action =
            new FileRenameAction(entry.getValue().toFile(), new File(renameTo), true);
        try {
          LOGGER.debug("DefaultRolloverStrategy.purgeAscending executing {}", action);
          if (!action.execute()) {
            return -1;
          }
        } catch (final Exception ex) {
          LOGGER.warn("Exception during purge in RollingFileAppender", ex);
          return -1;
        }
      }
    }

    return eligibleFiles.size() > 0
        ? (eligibleFiles.lastKey() < highIndex ? eligibleFiles.lastKey() + 1 : highIndex)
        : lowIndex;
  }

  protected SortedMap<Integer, Path> getEligibleFiles(
      final FileArchiver archiver, final boolean isAscending) {
    final StringBuilder buf = new StringBuilder();
    final String pattern = patternProcessor.getPattern();
    patternProcessor.formatFileName(strSubstitutor, buf, NotANumber.NAN);
    final String fileName = archiver.getFile();
    return getEligibleFiles(fileName, buf.toString(), pattern, isAscending);
  }

  private int purgeDescending(
      final int lowIndex, final int highIndex, final FileArchiver archiver) {
    // Retrieve the files in descending order, so the highest key will be first.
    final SortedMap<Integer, Path> eligibleFiles = getEligibleFiles(archiver, false);
    final int maxFiles = highIndex - lowIndex + 1;
    LOGGER.debug("Eligible files: {}", eligibleFiles);
    while (eligibleFiles.size() >= maxFiles) {
      try {
        final Integer key = eligibleFiles.firstKey();
        Files.delete(eligibleFiles.get(key));
        eligibleFiles.remove(key);
      } catch (final IOException ioe) {
        LOGGER.error("Unable to delete {}, {}", eligibleFiles.firstKey(), ioe.getMessage(), ioe);
        break;
      }
    }
    final StringBuilder buf = new StringBuilder();
    for (final Map.Entry<Integer, Path> entry : eligibleFiles.entrySet()) {
      buf.setLength(0);
      // LOG4J2-531: directory scan & rollover must use same format
      patternProcessor.formatFileName(strSubstitutor, buf, entry.getKey() + 1);
      final String currentName = entry.getValue().toFile().getName();
      String renameTo = buf.toString();
      final int suffixLength = suffixLength(renameTo);
      if (suffixLength > 0 && suffixLength(currentName) == 0) {
        renameTo = renameTo.substring(0, renameTo.length() - suffixLength);
      }
      final Action action =
          new FileRenameAction(entry.getValue().toFile(), new File(renameTo), true);
      try {
        LOGGER.debug("DefaultRolloverStrategy.purgeDescending executing {}", action);
        if (!action.execute()) {
          return -1;
        }
      } catch (final Exception ex) {
        LOGGER.warn("Exception during purge in RollingFileAppender", ex);
        return -1;
      }
    }

    return lowIndex;
  }

  @Override
  public RolloverDescription rollover(RollingFileManager manager) throws SecurityException {
    // RollingFileManager looks for an outputstream, so we can't use it here.
    // Instead purge* from the DefaultRolloverStrategy (can't call it directly, it's private)
    return null;
  }

  @Override
  public String toString() {
    return "FixedWindowRollingStrategy(min=" + minIndex + ", max=" + maxIndex + ")";
  }
}
