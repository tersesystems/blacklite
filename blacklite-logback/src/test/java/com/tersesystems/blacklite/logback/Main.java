package com.tersesystems.blacklite.logback;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) throws InterruptedException, IOException {
    final Path dir = Paths.get("/home/wsargent/blacklite-logback");
    Files.createDirectories(dir);

    System.out.println("START " + Instant.now());

    Random random = new Random();
    Runnable runnable = () -> {
      try {
        Thread.sleep(100L);
        for (int f = 0; f < 100000; f++) {
          //System.out.println("Going to sleep for 10L, f = " + f);
          Thread.sleep(10L);
          //System.out.println("Okay I'm back");
          // this will all log within the same millisecond, so we'll
          // add a nanosecond instant so that we can see what's going on.
          for (int i = 0; i < Math.abs(random.nextInt()) % 100000; i++) {
            logger.debug("debugging is fun!!! {}", Instant.now());
          }
        }
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    };
    Thread t1 = new Thread(runnable);
    Thread t2 = new Thread(runnable);
    Thread t3 = new Thread(runnable);

    t1.start();
    t2.start();
    t3.start();
  }
}
