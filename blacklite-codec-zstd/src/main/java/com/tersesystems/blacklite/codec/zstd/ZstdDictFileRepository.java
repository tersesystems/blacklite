package com.tersesystems.blacklite.codec.zstd;

import com.github.luben.zstd.Zstd;
import com.tersesystems.blacklite.codec.CodecException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;

public class ZstdDictFileRepository implements ZstdDictRepository {
  private String file;

  public String getFile() {
    return file;
  }

  public void setFile(String file) {
    this.file = file;
  }

  @Override
  public Optional<ZStdDict> lookup(long id) {
    return mostRecent().filter(dict -> dict.getId() == id);
  }

  @Override
  public Optional<ZStdDict> mostRecent() {
    return readFromFile(Paths.get(getFile()))
        .map(bytes -> new ZStdDict(Zstd.getDictIdFromDict(bytes), bytes));
  }

  @Override
  public void save(byte[] dictBytes) {
    writeToFile(Paths.get(getFile()), dictBytes);
  }

  @Override
  public void initialize() throws CodecException {
    Objects.requireNonNull(getFile(), "Null file");
  }

  private void writeToFile(Path path, byte[] dictBytes) {
    try {
      Files.write(path, dictBytes);
    } catch (IOException e) {
      throw new CodecException(e);
    }
  }

  /**
   * Utility for reading bytes from a file.
   *
   * @return the bytes of the file.
   */
  private Optional<byte[]> readFromFile(Path path) {
    try {
      if (!Files.exists(path)) {
        return Optional.empty();
      }
      byte[] dictBytes = Files.readAllBytes(path);
      return Optional.of(dictBytes);
    } catch (IOException e) {
      throw new CodecException(e);
    }
  }

  @Override
  public void close() throws Exception {
    // do nothing
  }
}
