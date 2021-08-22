package com.tersesystems.blacklite.codec.zstd;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdDictTrainer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.stream.Stream;

public final class ZStdUtils {

  // 0xFD2FB528 little endian = Array(28, B5, 2F, FD)
  private static final int ZSTD_MAGIC_NUMBER = Zstd.magicNumber();

  // 0xEC30A437 little endian = Array(37, A4, 30, EC)
  private static final byte[] ZSTD_DICT_MAGIC_NUMBER = { 0x37, (byte) 0xA4, 0x30, (byte) 0xEC };

  private static int magicNumber(byte[] array) {
    return (array[3] << 24) | ((array[2] & 0xff) << 16) | ((array[1] & 0xff) << 8) | (array[0] & 0xff);
  }

  public static boolean isFrame(byte[] frame) {
    if (frame == null || frame.length < 4) return false;
    // https://github.com/facebook/zstd/blob/master/doc/zstd_compression_format.md#zstandard-frames
    return ZSTD_MAGIC_NUMBER == magicNumber(frame);
  }

  public static byte[] trainDictionary(int sampleSize, int dictSize, Stream<byte[]> samples) {
    ZstdDictTrainer trainer = new ZstdDictTrainer(sampleSize, dictSize);
    samples.forEach(trainer::addSample);
    return trainer.trainSamples();
  }

  public static boolean isDictionary(File dictFile) throws IOException {
    try (InputStream in = Files.newInputStream(dictFile.toPath())) {
      final byte[] bytes = in.readNBytes(4);

      return Arrays.equals(bytes, ZSTD_DICT_MAGIC_NUMBER);
    }
  }

}
