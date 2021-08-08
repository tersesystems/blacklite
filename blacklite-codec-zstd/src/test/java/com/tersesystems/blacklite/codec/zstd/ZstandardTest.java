package com.tersesystems.blacklite.codec.zstd;

import com.github.luben.zstd.Zstd;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class ZstandardTest {

  @Test
  public void testCompressed() {
    byte[] src = "Hello World".getBytes(StandardCharsets.UTF_8);
    byte[] dst = new byte[100];
    Zstd.compressByteArray(dst, 0, dst.length, src, 0, src.length, 3);
    assertThat(ZStdUtils.isFrame(dst)).isTrue();
  }

}
