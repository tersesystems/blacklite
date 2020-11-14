package com.tersesystems.blacklite.reader;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;

/** Result consumer that writes to a printstream. */
public class PrintStreamResultConsumer implements ResultConsumer {
  private final PrintStream out;
  private final Charset charset;

  public PrintStreamResultConsumer(PrintStream out, Charset charset) {
    this.out = out;
    this.charset = charset;
  }

  @Override
  public void print(byte[] content) {
    if (charset != null) {
      out.print(new String(content, charset));
    } else {
      try {
        out.write(content); // write just sends bytes without modification
      } catch (IOException e) {
        e.printStackTrace(); // should use out here?
      }
    }
  }

  @Override
  public void count(long rowCount) {
    out.println(rowCount);
  }
}
