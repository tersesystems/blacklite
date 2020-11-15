package com.tersesystems.blacklite.logback;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) throws InterruptedException, IOException {
    Files.createDirectories(Paths.get("/tmp/blacklite-logback"));

    System.out.println("START " + Instant.now());
    Thread.sleep(1000L);

    for (int f = 0; f < 1000; f++) {
      // System.out.println("Going to sleep for 100L, f = " + f);
      Thread.sleep(10L);
      // System.out.println("Okay I'm back");
      for (int i = 0; i < 1000; i++) {
        logger.debug("debugging is fun!!! {}", Instant.now());
      }
    }

    //    System.out.println("Sleeping for 1000L " + Instant.now());
    System.out.println("ALL DONE " + Instant.now());
    // Thread.sleep(100000L);
  }
}
