package com.tersesystems.blacklite.codec.encryption;

/** Strategy interface for symmetric encryption algorithms. */
public interface SymmetricEncryption {

  /**
   * @return The algorithm identifier (e.g., "AES-GCM-256", "ChaCha20-Poly1305")
   */
  String getAlgorithm();

  /**
   * Encrypts plaintext with the given key.
   *
   * @param plaintext the data to encrypt
   * @param key the symmetric encryption key
   * @return ciphertext (typically IV + encrypted data + auth tag)
   * @throws EncryptionException if encryption fails
   */
  byte[] encrypt(byte[] plaintext, byte[] key) throws EncryptionException;

  /**
   * Decrypts ciphertext with the given key.
   *
   * @param ciphertext the encrypted data
   * @param key the symmetric encryption key
   * @return plaintext
   * @throws EncryptionException if decryption fails or authentication fails
   */
  byte[] decrypt(byte[] ciphertext, byte[] key) throws EncryptionException;
}
