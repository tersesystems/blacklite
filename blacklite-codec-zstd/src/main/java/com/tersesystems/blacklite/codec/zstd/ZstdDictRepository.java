package com.tersesystems.blacklite.codec.zstd;

import com.tersesystems.blacklite.codec.CodecException;
import java.util.Optional;

public interface ZstdDictRepository extends AutoCloseable {
  Optional<ZStdDict> lookup(long id);

  Optional<ZStdDict> mostRecent();

  void save(byte[] dictBytes);

  void initialize() throws CodecException;
}
