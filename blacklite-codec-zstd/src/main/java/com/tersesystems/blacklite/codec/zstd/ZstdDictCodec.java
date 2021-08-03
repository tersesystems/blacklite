package com.tersesystems.blacklite.codec.zstd;

import com.github.luben.zstd.*;
import com.tersesystems.blacklite.StatusReporter;
import com.tersesystems.blacklite.codec.Codec;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * ZStandard compression with dictionary compression training.
 */
public class ZstdDictCodec implements Codec {

  // The sample size is the sum of the individual samples,
  // i.e. if you have 1000 messages that are all 26 bytes each,
  // then the sample size is 26000 bytes.
  private int sampleSize = 100_000 * 1024;

  // Dictionary size can be up to 10 MeB in bytes.  This is the output from training.
  private int dictSize = 10485760;

  private int level = 3;

  private final ZstdCompressCtx compressCtx = new ZstdCompressCtx();
  private final ZstdDecompressCtx decompressCtx = new ZstdDecompressCtx();

  private ZstdDictRepository repository;
  private ZstdDictTrainer trainer;
  private StatusReporter statusReporter;

  public ZstdDictCodec() {}

  @Override
  public void initialize(StatusReporter statusReporter) {
    Objects.requireNonNull(repository, "Null repository");
    repository.initialize();

    this.statusReporter = statusReporter;
    this.compressCtx.setLevel(level);
    Optional<ZstdDict> maybe = repository.mostRecent();
    if (maybe.isPresent()) {
      byte[] dict = maybe.get().getBytes();
      trainer = null;
      compressCtx.loadDict(dict);
      decompressCtx.loadDict(dict);
    } else {
      Consumer<byte[]> consumer =
          dbytes -> {
            repository.save(dbytes);
            trainer = null;
            compressCtx.loadDict(dbytes);
            decompressCtx.loadDict(dbytes);
          };
      this.trainer = new ZstdDictTrainer(sampleSize, dictSize, consumer);
    }
  }

  public byte[] encode(byte[] bytes) {
    if (bytes == null) return null;

    if (trainer != null) {
      trainer.train(bytes);
    }
    return compressCtx.compress(bytes);
  }

  public byte[] decode(byte[] compressed) {
    if (compressed == null) return null;
    // XXX if it's not compressed at all then return the original
    int i = (int) Zstd.decompressedSize(compressed);
    final long dictIdFromDict = Zstd.getDictIdFromDict(compressed);
    if (dictIdFromDict == 0) {
      return decompressCtx.decompress(compressed, i);
    } else {
      // XXX should cache this so we don't have to do look up repeatedly
      final Optional<ZstdDict> lookup = repository.lookup(dictIdFromDict);
      if (lookup.isEmpty()) {
        throw new NoDictionaryFoundException("No dictionary found for dictId", dictIdFromDict);
      }
      final ZstdDict zstdDict = lookup.get();
      return Zstd.decompress(compressed, zstdDict.getBytes(), i);
    }
  }

  @Override
  public String toString() {
    return "ZStandardCodec{" + "level=" + level + ", repo=" + repository + '}';
  }

  @Override
  public void close() {
    try {
      compressCtx.close();
      decompressCtx.close();
      repository.close();
    } catch (Exception e) {
      statusReporter.addError(e.getMessage(), e);
    }
  }

  @Override
  public String getName() {
    return "zstddict";
  }

  public int getLevel() {
    return level;
  }

  public void setLevel(int level) {
    this.level = level;
  }

  public int getDictSize() {
    return dictSize;
  }

  public void setDictSize(int dictSize) {
    this.dictSize = dictSize;
  }

  public int getSampleSize() {
    return sampleSize;
  }

  public void setSampleSize(int sampleSize) {
    this.sampleSize = sampleSize;
  }

  public ZstdDictRepository getRepository() {
    return this.repository;
  }

  public void setRepository(ZstdDictRepository dictRepository) {
    this.repository = dictRepository;
  }
}
