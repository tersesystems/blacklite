package com.tersesystems.blacklite.codec.zstd;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdCompressCtx;
import com.github.luben.zstd.ZstdDecompressCtx;
import com.tersesystems.blacklite.StatusReporter;
import com.tersesystems.blacklite.codec.Codec;

public class ZStdCodec implements Codec, AutoCloseable {

  private final ZstdCompressCtx compressCtx = new ZstdCompressCtx();
  private final ZstdDecompressCtx decompressCtx = new ZstdDecompressCtx();
  private int level = 3;

  @Override
  public String getName() {
    return "zstd";
  }

  @Override
  public void initialize(StatusReporter statusReporter) {
    this.compressCtx.setLevel(getLevel());
  }

  public byte[] encode(byte[] bytes) {
    if (bytes == null) return null;
    return compressCtx.compress(bytes);
  }

  public byte[] decode(byte[] compressed) {
    if (compressed == null) return null;
    int i = (int) Zstd.decompressedSize(compressed);
    return decompressCtx.decompress(compressed, i);
  }

  @Override
  public void close() {
    compressCtx.close();
    decompressCtx.close();
  }

  public int getLevel() {
    return level;
  }

  public void setLevel(int level) {
    this.level = level;
  }
}
