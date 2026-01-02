package com.tersesystems.blacklite.codec.encryption;

/** Model for encryption metadata stored in database. */
public class EncryptionMetadata {

  private final int id;
  private final String algorithm;
  private final byte[] encryptedKey;
  private final String keyEncryptionAlgorithm;
  private final String keyEncryptionComment;
  private final long createdAt;

  public EncryptionMetadata(
      int id,
      String algorithm,
      byte[] encryptedKey,
      String keyEncryptionAlgorithm,
      String keyEncryptionComment,
      long createdAt) {
    this.id = id;
    this.algorithm = algorithm;
    this.encryptedKey = encryptedKey;
    this.keyEncryptionAlgorithm = keyEncryptionAlgorithm;
    this.keyEncryptionComment = keyEncryptionComment;
    this.createdAt = createdAt;
  }

  public int getId() {
    return id;
  }

  public String getAlgorithm() {
    return algorithm;
  }

  public byte[] getEncryptedKey() {
    return encryptedKey;
  }

  public String getKeyEncryptionAlgorithm() {
    return keyEncryptionAlgorithm;
  }

  public String getKeyEncryptionComment() {
    return keyEncryptionComment;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public boolean isKeyWrapped() {
    return keyEncryptionAlgorithm != null;
  }
}
