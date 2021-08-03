package com.tersesystems.blacklite.codec.zstd;

import com.tersesystems.blacklite.codec.CodecException;

public abstract class ZstdCodecException extends CodecException {

  public ZstdCodecException(String message) {
    super(message);
  }

  public ZstdCodecException(String message, Throwable cause) {
    super(message, cause);
  }

  public ZstdCodecException(Throwable cause) {
    super(cause);
  }

  public ZstdCodecException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
