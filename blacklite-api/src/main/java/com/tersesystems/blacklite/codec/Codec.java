package com.tersesystems.blacklite.codec;

import com.tersesystems.blacklite.StatusReporter;

/** Interface for converting between data formats, typically a text format and a binary format. */
public interface Codec extends AutoCloseable {

  /** @return The key that is used in the codec registry. */
  String getName();

  void initialize(StatusReporter statusReporter);

  /**
   * Encodes content.
   *
   * @param unencoded the bytes containing unencoded data.
   * @return the size of the encoded data.
   * @throws CodecException
   */
  byte[] encode(byte[] unencoded) throws CodecException;

  /**
   * Decodes content from sourceBytes bytes to destBytes bytes.
   *
   * @param encoded the bytes containing encoded data.
   * @return the bytes containing the decoded data.
   * @throws CodecException
   */
  byte[] decode(byte[] encoded) throws CodecException;

  default void close() {}
}
