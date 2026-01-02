package com.tersesystems.blacklite.codec.encryption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class AesGcmEncryptionTest {

  @Test
  public void testAlgorithmName() {
    AesGcmEncryption encryption = new AesGcmEncryption();
    assertThat(encryption.getAlgorithm()).isEqualTo("AES-GCM-256");
  }

  @Test
  public void testEncryptDecryptRoundtrip() throws Exception {
    AesGcmEncryption encryption = new AesGcmEncryption();

    byte[] key = new byte[32]; // 256-bit key
    new SecureRandom().nextBytes(key);

    byte[] plaintext = "Hello, World!".getBytes("UTF-8");

    byte[] ciphertext = encryption.encrypt(plaintext, key);
    byte[] decrypted = encryption.decrypt(ciphertext, key);

    assertThat(decrypted).isEqualTo(plaintext);
  }

  @Test
  public void testDifferentKeysProduceDifferentCiphertext() throws Exception {
    AesGcmEncryption encryption = new AesGcmEncryption();

    byte[] key1 = new byte[32];
    byte[] key2 = new byte[32];
    new SecureRandom().nextBytes(key1);
    new SecureRandom().nextBytes(key2);

    byte[] plaintext = "Same plaintext".getBytes("UTF-8");

    byte[] ciphertext1 = encryption.encrypt(plaintext, key1);
    byte[] ciphertext2 = encryption.encrypt(plaintext, key2);

    assertThat(ciphertext1).isNotEqualTo(ciphertext2);
  }

  @Test
  public void testIvUniqueness() throws Exception {
    AesGcmEncryption encryption = new AesGcmEncryption();

    byte[] key = new byte[32];
    new SecureRandom().nextBytes(key);

    byte[] plaintext = "Same plaintext".getBytes("UTF-8");

    byte[] ciphertext1 = encryption.encrypt(plaintext, key);
    byte[] ciphertext2 = encryption.encrypt(plaintext, key);

    // Same plaintext + same key should produce different ciphertext due to random IV
    assertThat(ciphertext1).isNotEqualTo(ciphertext2);

    // But both should decrypt correctly
    assertThat(encryption.decrypt(ciphertext1, key)).isEqualTo(plaintext);
    assertThat(encryption.decrypt(ciphertext2, key)).isEqualTo(plaintext);
  }

  @Test
  public void testWrongKeyFailsDecryption() throws Exception {
    AesGcmEncryption encryption = new AesGcmEncryption();

    byte[] correctKey = new byte[32];
    byte[] wrongKey = new byte[32];
    new SecureRandom().nextBytes(correctKey);
    new SecureRandom().nextBytes(wrongKey);

    byte[] plaintext = "Secret message".getBytes("UTF-8");
    byte[] ciphertext = encryption.encrypt(plaintext, correctKey);

    assertThatThrownBy(() -> encryption.decrypt(ciphertext, wrongKey))
        .isInstanceOf(EncryptionException.class);
  }

  @Test
  public void testTamperingDetected() throws Exception {
    AesGcmEncryption encryption = new AesGcmEncryption();

    byte[] key = new byte[32];
    new SecureRandom().nextBytes(key);

    byte[] plaintext = "Important data".getBytes("UTF-8");
    byte[] ciphertext = encryption.encrypt(plaintext, key);

    // Tamper with ciphertext (flip a bit in the middle)
    byte[] tampered = Arrays.copyOf(ciphertext, ciphertext.length);
    tampered[ciphertext.length / 2] ^= 0x01;

    assertThatThrownBy(() -> encryption.decrypt(tampered, key))
        .isInstanceOf(EncryptionException.class)
        .hasMessageContaining("tampering");
  }

  @Test
  public void testNullPlaintext() throws Exception {
    AesGcmEncryption encryption = new AesGcmEncryption();
    byte[] key = new byte[32];

    byte[] result = encryption.encrypt(null, key);
    assertThat(result).isNull();
  }

  @Test
  public void testEmptyPlaintext() throws Exception {
    AesGcmEncryption encryption = new AesGcmEncryption();

    byte[] key = new byte[32];
    new SecureRandom().nextBytes(key);

    byte[] plaintext = new byte[0];
    byte[] ciphertext = encryption.encrypt(plaintext, key);
    byte[] decrypted = encryption.decrypt(ciphertext, key);

    assertThat(decrypted).isEmpty();
  }
}
