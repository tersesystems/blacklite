package com.tersesystems.blacklite.codec.encryption;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Optional;

/** Manages encryption key lifecycle: generation, storage, retrieval. */
public class EncryptionKeyStore implements AutoCloseable {

  private final EncryptionMetadataRepository repository;
  private final String algorithm;
  private final SecureRandom secureRandom;
  private final AsymmetricKeyWrapper keyWrapper;

  private byte[] symmetricKey;
  private PrivateKey privateKey;

  public EncryptionKeyStore(EncryptionMetadataRepository repository, String algorithm) {
    this.repository = repository;
    this.algorithm = algorithm;
    this.secureRandom = new SecureRandom();
    // Force seeding
    this.secureRandom.nextBytes(new byte[1]);
    this.keyWrapper = new RsaOaepKeyWrapper();
  }

  /**
   * Initialize the key store. Creates new key or loads existing.
   *
   * @param publicKey optional public key for wrapping
   * @param comment optional comment for metadata
   */
  public void initialize(PublicKey publicKey, String comment) throws Exception {
    Optional<EncryptionMetadata> existing = repository.get();

    if (existing.isPresent()) {
      // Load existing key
      loadKey(existing.get());
    } else {
      // Generate new key
      createNewKey(publicKey, comment);
    }
  }

  /** Generate a new symmetric key. */
  public byte[] generateSymmetricKey() {
    int keySize = getKeySizeForAlgorithm(algorithm);
    byte[] key = new byte[keySize / 8];
    secureRandom.nextBytes(key);
    return key;
  }

  /** Set private key for unwrapping. */
  public void setPrivateKey(PrivateKey privateKey) {
    this.privateKey = privateKey;
  }

  /** Get the symmetric key. */
  public byte[] getSymmetricKey() {
    if (symmetricKey == null) {
      throw new IllegalStateException("Key store not initialized");
    }
    return symmetricKey;
  }

  @Override
  public void close() {
    // Zero out sensitive key material
    if (symmetricKey != null) {
      Arrays.fill(symmetricKey, (byte) 0);
    }
  }

  private void createNewKey(PublicKey publicKey, String comment) throws Exception {
    symmetricKey = generateSymmetricKey();
    long timestamp = System.currentTimeMillis();

    if (publicKey != null) {
      // Wrap key with public key
      byte[] wrappedKey = keyWrapper.wrap(symmetricKey, publicKey);
      EncryptionMetadata metadata =
          new EncryptionMetadata(
              0, algorithm, wrappedKey, keyWrapper.getAlgorithm(), comment, timestamp);
      repository.save(metadata);
    } else {
      // Store unwrapped key
      EncryptionMetadata metadata =
          new EncryptionMetadata(0, algorithm, symmetricKey, null, null, timestamp);
      repository.save(metadata);
    }
  }

  private void loadKey(EncryptionMetadata metadata) throws Exception {
    if (metadata.isKeyWrapped()) {
      // Need private key to unwrap
      if (privateKey == null) {
        throw EncryptionException.missingPrivateKey(
            metadata.getKeyEncryptionAlgorithm(), metadata.getKeyEncryptionComment());
      }
      symmetricKey = keyWrapper.unwrap(metadata.getEncryptedKey(), privateKey);
    } else {
      // Key stored unwrapped
      symmetricKey = metadata.getEncryptedKey();
    }
  }

  private int getKeySizeForAlgorithm(String algorithm) {
    if (algorithm.contains("256")) {
      return 256;
    } else if (algorithm.contains("128")) {
      return 128;
    }
    return 256; // default to 256-bit
  }
}
