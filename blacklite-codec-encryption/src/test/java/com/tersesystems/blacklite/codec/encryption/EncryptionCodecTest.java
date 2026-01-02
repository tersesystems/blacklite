package com.tersesystems.blacklite.codec.encryption;

import static org.assertj.core.api.Assertions.assertThat;

import com.tersesystems.blacklite.StatusReporter;
import com.tersesystems.blacklite.codec.Codec;
import com.tersesystems.blacklite.codec.identity.IdentityCodec;
import java.sql.Connection;
import java.sql.DriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EncryptionCodecTest {

  private Connection connection;
  private EncryptionCodec codec;

  @BeforeEach
  public void setUp() throws Exception {
    connection = DriverManager.getConnection("jdbc:sqlite::memory:");
    EncryptionMetadataRepository repository = new EncryptionMetadataRepository(connection);
    repository.initialize();

    codec = new EncryptionCodec();
    codec.setConnection(connection);
    codec.setInnerCodec(new IdentityCodec());
    codec.setEncryption(new AesGcmEncryption());
  }

  @AfterEach
  public void tearDown() throws Exception {
    codec.close();
    connection.close();
  }

  @Test
  public void testGetName() {
    assertThat(codec.getName()).isEqualTo("encryption");
  }

  @Test
  public void testEncryptDecryptRoundtrip() throws Exception {
    codec.initialize(StatusReporter.DEFAULT);

    byte[] plaintext = "Hello, World!".getBytes("UTF-8");
    byte[] encrypted = codec.encode(plaintext);
    byte[] decrypted = codec.decode(encrypted);

    assertThat(decrypted).isEqualTo(plaintext);
  }

  @Test
  public void testDifferentPlaintextProducesDifferentCiphertext() throws Exception {
    codec.initialize(StatusReporter.DEFAULT);

    byte[] plaintext1 = "Message 1".getBytes("UTF-8");
    byte[] plaintext2 = "Message 2".getBytes("UTF-8");

    byte[] encrypted1 = codec.encode(plaintext1);
    byte[] encrypted2 = codec.encode(plaintext2);

    assertThat(encrypted1).isNotEqualTo(encrypted2);
  }

  @Test
  public void testSamePlaintextProducesDifferentCiphertext() throws Exception {
    codec.initialize(StatusReporter.DEFAULT);

    byte[] plaintext = "Same message".getBytes("UTF-8");

    byte[] encrypted1 = codec.encode(plaintext);
    byte[] encrypted2 = codec.encode(plaintext);

    // Due to random IV, same plaintext produces different ciphertext
    assertThat(encrypted1).isNotEqualTo(encrypted2);

    // But both decrypt correctly
    assertThat(codec.decode(encrypted1)).isEqualTo(plaintext);
    assertThat(codec.decode(encrypted2)).isEqualTo(plaintext);
  }

  @Test
  public void testNullInput() throws Exception {
    codec.initialize(StatusReporter.DEFAULT);

    byte[] encrypted = codec.encode(null);
    assertThat(encrypted).isNull();
  }

  @Test
  public void testEmptyInput() throws Exception {
    codec.initialize(StatusReporter.DEFAULT);

    byte[] plaintext = new byte[0];
    byte[] encrypted = codec.encode(plaintext);
    byte[] decrypted = codec.decode(encrypted);

    assertThat(decrypted).isEmpty();
  }

  @Test
  public void testReloadUsingSameKey() throws Exception {
    codec.initialize(StatusReporter.DEFAULT);

    byte[] plaintext = "Persistent message".getBytes("UTF-8");
    byte[] encrypted = codec.encode(plaintext);

    // Create new codec instance with same connection
    EncryptionCodec codec2 = new EncryptionCodec();
    codec2.setConnection(connection);
    codec2.setInnerCodec(new IdentityCodec());
    codec2.setEncryption(new AesGcmEncryption());
    codec2.initialize(StatusReporter.DEFAULT);

    // Should be able to decrypt with reloaded key
    byte[] decrypted = codec2.decode(encrypted);
    assertThat(decrypted).isEqualTo(plaintext);

    codec2.close();
  }
}
