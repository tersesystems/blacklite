package com.tersesystems.blacklite.codec.identity;

import com.tersesystems.blacklite.StatusReporter;
import com.tersesystems.blacklite.codec.Codec;
import com.tersesystems.blacklite.codec.CodecException;

public class IdentityCodec implements Codec {
  public static final String NAME = "identity";

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public void initialize(StatusReporter statusReporter) {}

  @Override
  public byte[] encode(byte[] sourceBytes) throws CodecException {
    return sourceBytes;
  }

  @Override
  public byte[] decode(byte[] sourceBytes) throws CodecException {
    return sourceBytes;
  }
}
