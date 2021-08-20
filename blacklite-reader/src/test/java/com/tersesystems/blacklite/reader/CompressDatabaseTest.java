package com.tersesystems.blacklite.reader;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CompressDatabaseTest {

  @Test
  public void testCompressDatabase() throws IOException {
    Path tempDir = Files.createTempDirectory("blacklite");

    final String source = getClass().getResource("/blacklite.db").getFile();
    if (! Files.exists(Paths.get(source))) {
      throw new IllegalStateException("Does not exist: " + source);
    }
    CompressDatabase.main(source, tempDir.resolve("compressed.db").toString());
  }
}
