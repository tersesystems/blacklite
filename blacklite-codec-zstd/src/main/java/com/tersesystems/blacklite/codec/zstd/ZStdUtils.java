package com.tersesystems.blacklite.codec.zstd;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdDictTrainer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Stream;

public final class ZStdUtils {

  private static final int ZSTD_MAGIC_NUMBER = Zstd.magicNumber();

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

}
