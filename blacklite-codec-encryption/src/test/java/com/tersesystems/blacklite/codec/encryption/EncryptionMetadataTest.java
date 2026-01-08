package com.tersesystems.blacklite.codec.encryption;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class EncryptionMetadataTest {

  @Test
  public void testConstructorAndGetters() {
    byte[] key = new byte[] {1, 2, 3};
    EncryptionMetadata metadata =
        new EncryptionMetadata(1, "AES-GCM-256", key, "RSA-OAEP-2048", "Production logs", 123456L);

    assertThat(metadata.getId()).isEqualTo(1);
    assertThat(metadata.getAlgorithm()).isEqualTo("AES-GCM-256");
    assertThat(metadata.getEncryptedKey()).isEqualTo(key);
    assertThat(metadata.getKeyEncryptionAlgorithm()).isEqualTo("RSA-OAEP-2048");
    assertThat(metadata.getKeyEncryptionComment()).isEqualTo("Production logs");
    assertThat(metadata.getCreatedAt()).isEqualTo(123456L);
  }

  @Test
  public void testNullKeyEncryption() {
    byte[] key = new byte[] {1, 2, 3};
    EncryptionMetadata metadata =
        new EncryptionMetadata(1, "AES-GCM-256", key, null, null, 123456L);

    assertThat(metadata.getKeyEncryptionAlgorithm()).isNull();
    assertThat(metadata.getKeyEncryptionComment()).isNull();
    assertThat(metadata.isKeyWrapped()).isFalse();
  }

  @Test
  public void testKeyWrappedDetection() {
    byte[] key = new byte[] {1, 2, 3};
    EncryptionMetadata wrapped =
        new EncryptionMetadata(1, "AES-GCM-256", key, "RSA-OAEP-2048", "Test", 123456L);
    EncryptionMetadata unwrapped =
        new EncryptionMetadata(1, "AES-GCM-256", key, null, null, 123456L);

    assertThat(wrapped.isKeyWrapped()).isTrue();
    assertThat(unwrapped.isKeyWrapped()).isFalse();
  }

  @Test
  public void testEncryptedKeyDefensivelyCopied() {
    byte[] key = new byte[] {1, 2, 3, 4};
    EncryptionMetadata metadata =
        new EncryptionMetadata(1, "AES-GCM-256", key, null, null, 123456L);

    // Mutate original array
    key[0] = 99;

    // Metadata should be unchanged
    assertThat(metadata.getEncryptedKey()[0]).isEqualTo((byte) 1);

    // Mutate returned array
    byte[] retrieved = metadata.getEncryptedKey();
    retrieved[1] = 88;

    // Subsequent retrieval should be unchanged
    assertThat(metadata.getEncryptedKey()[1]).isEqualTo((byte) 2);
  }
}
