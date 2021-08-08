package com.tersesystems.blacklite.codec.zstd;

public class NoDictionaryFoundException extends ZStdCodecException {

  private long dictId;

  public long getDictionaryId() {
    return this.dictId;
  }

  public NoDictionaryFoundException(String message, long dictId) {
    super(message);
    this.dictId = dictId;
  }

  public NoDictionaryFoundException(String message, Throwable cause, long dictId) {
    super(message, cause);
    this.dictId = dictId;
  }
}
