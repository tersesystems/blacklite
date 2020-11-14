package com.tersesystems.blacklite.codec.zstd;

import com.tersesystems.blacklite.codec.CodecException;
import java.util.Optional;

public interface ZstdDictRepository extends AutoCloseable {
  Optional<ZstdDict> lookup(long id);

  Optional<ZstdDict> mostRecent();

  void save(byte[] dictBytes);

  void initialize() throws CodecException;
}
