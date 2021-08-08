package com.tersesystems.blacklite.codec.zstd;

import com.tersesystems.blacklite.codec.CodecException;

public class ZStdDict {

  private final long id;
  private final byte[] dictBytes;

  public ZStdDict(long dictId, byte[] dictBytes) throws CodecException {
    if (dictId == 0) {
      throw new CodecException("No dictionary found from bytes!");
    }
    this.id = dictId;
    this.dictBytes = dictBytes;
  }

  public long getId() {
    return id;
  }

  public byte[] getBytes() {
    return dictBytes;
  }

  @Override
  public String toString() {
    String bytesString = (dictBytes == null) ? "null" : "" + dictBytes.length;
    return "ZStandardDictionary{id=" + id + ",bytes=" + bytesString + "}";
  }
}
