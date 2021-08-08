package com.tersesystems.blacklite.reader;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

public class CompressDatabaseTest {

  @Test
  public void testCompressDatabase() {
    final String source = getClass().getResource("/blacklite.db").getFile();
    if (! Files.exists(Paths.get(source))) {
      throw new IllegalStateException("Does not exist: " + source);
    }
    CompressDatabase.main(source, "dest.db");
  }
}
