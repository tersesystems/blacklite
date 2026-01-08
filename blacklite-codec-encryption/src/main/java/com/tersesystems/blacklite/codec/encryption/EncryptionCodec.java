package com.tersesystems.blacklite.codec.encryption;

import com.tersesystems.blacklite.StatusReporter;
import com.tersesystems.blacklite.codec.Codec;
import com.tersesystems.blacklite.codec.CodecException;
import java.sql.Connection;

/**
 * Encryption codec that wraps another codec and adds encryption.
 *
 * <p>Composition pattern: plaintext → innerCodec → encryption → ciphertext
 */
public class EncryptionCodec implements Codec {

  private Codec innerCodec;
  private SymmetricEncryption encryption;
  private EncryptionKeyStore keyStore;
  private Connection connection;

  @Override
  public String getName() {
    return "encryption";
  }

  @Override
  public void initialize(StatusReporter statusReporter) {
    if (connection == null) {
      throw new IllegalStateException("Connection not set");
    }
    if (innerCodec == null) {
      throw new IllegalStateException("Inner codec not set");
    }
    if (encryption == null) {
      throw new IllegalStateException("Encryption not set");
    }

    try {
      // Initialize inner codec
      innerCodec.initialize(statusReporter);

      // Initialize key store
      EncryptionMetadataRepository repository = new EncryptionMetadataRepository(connection);
      repository.initialize();

      keyStore = new EncryptionKeyStore(repository, encryption.getAlgorithm());
      keyStore.initialize(null, null); // No public key wrapping for now

    } catch (Exception e) {
      statusReporter.addError("Failed to initialize EncryptionCodec", e);
      throw new RuntimeException("Failed to initialize EncryptionCodec", e);
    }
  }

  @Override
  public byte[] encode(byte[] unencoded) throws CodecException {
    if (unencoded == null) {
      return null;
    }

    try {
      // First apply inner codec (e.g., compression)
      byte[] innerEncoded = innerCodec.encode(unencoded);

      // Then encrypt
      return encryption.encrypt(innerEncoded, keyStore.getSymmetricKey());
    } catch (Exception e) {
      throw new CodecException("Encryption failed", e);
    }
  }

  @Override
  public byte[] decode(byte[] encoded) throws CodecException {
    if (encoded == null) {
      return null;
    }

    try {
      // First decrypt
      byte[] decrypted = encryption.decrypt(encoded, keyStore.getSymmetricKey());

      // Then apply inner codec (e.g., decompression)
      return innerCodec.decode(decrypted);
    } catch (Exception e) {
      throw new CodecException("Decryption failed", e);
    }
  }

  @Override
  public void close() {
    try {
      if (innerCodec != null) {
        innerCodec.close();
      }
      if (keyStore != null) {
        keyStore.close();
      }
    } catch (Exception e) {
      // Log but don't throw
    }
  }

  // Setters for configuration

  public void setInnerCodec(Codec innerCodec) {
    this.innerCodec = innerCodec;
  }

  public void setEncryption(SymmetricEncryption encryption) {
    this.encryption = encryption;
  }

  public void setConnection(Connection connection) {
    this.connection = connection;
  }

  public EncryptionKeyStore getKeyStore() {
    return keyStore;
  }
}
