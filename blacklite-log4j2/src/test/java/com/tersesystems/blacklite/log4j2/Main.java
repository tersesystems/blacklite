package com.tersesystems.blacklite.log4j2;

import java.time.Instant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {
  private static final Logger logger = LogManager.getLogger(Main.class);

  public static void main(String[] args) throws InterruptedException {
    System.out.println("START " + Instant.now());
    Thread.sleep(1000L);

    for (int f = 0; f < 1000; f++) {
      // System.out.println("Going to sleep for 100L, f = " + f);
      Thread.sleep(100L);
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
