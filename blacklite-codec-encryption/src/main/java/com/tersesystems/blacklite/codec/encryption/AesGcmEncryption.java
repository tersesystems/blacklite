package com.tersesystems.blacklite.codec.encryption;

import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES-256-GCM symmetric encryption implementation.
 *
 * <p>Uses 12-byte random IVs (prepended to ciphertext) and 128-bit authentication tags.
 */
public class AesGcmEncryption implements SymmetricEncryption {

  private static final String ALGORITHM = "AES/GCM/NoPadding";
  private static final int IV_SIZE = 12; // 12 bytes recommended for GCM
  private static final int TAG_SIZE = 128; // 128-bit authentication tag

  private final SecureRandom secureRandom;

  public AesGcmEncryption() {
    this.secureRandom = new SecureRandom();
    // Force seeding
    secureRandom.nextBytes(new byte[1]);
  }

  @Override
  public String getAlgorithm() {
    return "AES-GCM-256";
  }

  @Override
  public byte[] encrypt(byte[] plaintext, byte[] key) throws EncryptionException {
    if (plaintext == null) {
      return null;
    }

    try {
      // Generate random IV
      byte[] iv = new byte[IV_SIZE];
      secureRandom.nextBytes(iv);

      // Initialize cipher
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
      GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_SIZE, iv);
      cipher.init(Cipher.ENCRYPT_MODE, keySpec, parameterSpec);

      // Encrypt
      byte[] encrypted = cipher.doFinal(plaintext);

      // Prepend IV to ciphertext: [IV][encrypted+tag]
      byte[] result = new byte[IV_SIZE + encrypted.length];
      System.arraycopy(iv, 0, result, 0, IV_SIZE);
      System.arraycopy(encrypted, 0, result, IV_SIZE, encrypted.length);

      return result;
    } catch (Exception e) {
      throw new EncryptionException("Encryption failed", e);
    }
  }

  @Override
  public byte[] decrypt(byte[] ciphertext, byte[] key) throws EncryptionException {
    if (ciphertext == null) {
      return null;
    }

    try {
      // Extract IV from ciphertext
      byte[] iv = new byte[IV_SIZE];
      System.arraycopy(ciphertext, 0, iv, 0, IV_SIZE);

      // Extract encrypted data
      byte[] encrypted = new byte[ciphertext.length - IV_SIZE];
      System.arraycopy(ciphertext, IV_SIZE, encrypted, 0, encrypted.length);

      // Initialize cipher
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
      GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_SIZE, iv);
      cipher.init(Cipher.DECRYPT_MODE, keySpec, parameterSpec);

      // Decrypt (authentication happens automatically in GCM mode)
      return cipher.doFinal(encrypted);
    } catch (javax.crypto.AEADBadTagException e) {
      throw EncryptionException.decryptionFailed(e);
    } catch (Exception e) {
      throw new EncryptionException("Decryption failed", e);
    }
  }
}
