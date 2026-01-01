package com.tersesystems.blacklite.codec.encryption;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class EncryptionExceptionTest {

  @Test
  public void testMissingPrivateKeyMessage() {
    EncryptionException ex = EncryptionException.missingPrivateKey("RSA-OAEP-2048", "Production logs");

    assertThat(ex.getMessage())
        .contains("Database requires private key")
        .contains("RSA-OAEP-2048")
        .contains("Production logs")
        .contains("BLACKLITE_PRIVATE_KEY_FILE");
  }

  @Test
  public void testDecryptionFailedMessage() {
    Exception cause = new RuntimeException("Invalid key");
    EncryptionException ex = EncryptionException.decryptionFailed(cause);

    assertThat(ex.getMessage())
        .contains("Failed to decrypt")
        .contains("Wrong private key");
    assertThat(ex.getCause()).isEqualTo(cause);
  }

  @Test
  public void testKeyGenerationFailedMessage() {
    Exception cause = new RuntimeException("Low entropy");
    EncryptionException ex = EncryptionException.keyGenerationFailed(cause);

    assertThat(ex.getMessage())
        .contains("Failed to generate encryption key")
        .contains("entropy");
    assertThat(ex.getCause()).isEqualTo(cause);
  }

  @Test
  public void testInvalidKeyFileMessage() {
    Exception cause = new RuntimeException("Parse error");
    EncryptionException ex = EncryptionException.invalidKeyFile("/path/to/key.pem", cause);

    assertThat(ex.getMessage())
        .contains("/path/to/key.pem")
        .contains("PEM or PKCS8");
    assertThat(ex.getCause()).isEqualTo(cause);
  }
}
