package com.tersesystems.blacklite.codec.zstd;

import com.tersesystems.blacklite.codec.CodecException;

public abstract class ZStdCodecException extends CodecException {

  public ZStdCodecException(String message) {
    super(message);
  }

  public ZStdCodecException(String message, Throwable cause) {
    super(message, cause);
  }

  public ZStdCodecException(Throwable cause) {
    super(cause);
  }

  public ZStdCodecException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
