package com.tersesystems.blacklite.codec.encryption;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EncryptionKeyStoreTest {

  private Connection connection;
  private EncryptionMetadataRepository repository;

  @BeforeEach
  public void setUp() throws Exception {
    connection = DriverManager.getConnection("jdbc:sqlite::memory:");
    repository = new EncryptionMetadataRepository(connection);
    repository.initialize();
  }

  @AfterEach
  public void tearDown() throws Exception {
    connection.close();
  }

  @Test
  public void testGenerateSymmetricKey() {
    EncryptionKeyStore keyStore = new EncryptionKeyStore(repository, "AES-GCM-256");

    byte[] key = keyStore.generateSymmetricKey();

    assertThat(key).hasSize(32); // 256 bits = 32 bytes
  }

  @Test
  public void testGeneratedKeysAreUnique() {
    EncryptionKeyStore keyStore = new EncryptionKeyStore(repository, "AES-GCM-256");

    byte[] key1 = keyStore.generateSymmetricKey();
    byte[] key2 = keyStore.generateSymmetricKey();

    assertThat(key1).isNotEqualTo(key2);
  }

  @Test
  public void testInitializeWithoutPublicKey() throws Exception {
    EncryptionKeyStore keyStore = new EncryptionKeyStore(repository, "AES-GCM-256");

    keyStore.initialize(null, null);

    byte[] key = keyStore.getSymmetricKey();
    assertThat(key).hasSize(32);

    // Verify metadata saved
    EncryptionMetadata metadata = repository.get().orElseThrow(() -> new RuntimeException("No metadata found"));
    assertThat(metadata.getAlgorithm()).isEqualTo("AES-GCM-256");
    assertThat(metadata.isKeyWrapped()).isFalse();
  }

  @Test
  public void testInitializeWithPublicKey() throws Exception {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(2048, new SecureRandom());
    KeyPair keyPair = keyGen.generateKeyPair();

    EncryptionKeyStore keyStore = new EncryptionKeyStore(repository, "AES-GCM-256");

    keyStore.initialize(keyPair.getPublic(), "Test comment");

    // Verify metadata saved with key wrapping
    EncryptionMetadata metadata = repository.get().orElseThrow(() -> new RuntimeException("No metadata found"));
    assertThat(metadata.getAlgorithm()).isEqualTo("AES-GCM-256");
    assertThat(metadata.isKeyWrapped()).isTrue();
    assertThat(metadata.getKeyEncryptionAlgorithm()).contains("RSA-OAEP");
    assertThat(metadata.getKeyEncryptionComment()).isEqualTo("Test comment");
  }

  @Test
  public void testLoadExistingKey() throws Exception {
    // First store: create key
    EncryptionKeyStore keyStore1 = new EncryptionKeyStore(repository, "AES-GCM-256");
    keyStore1.initialize(null, null);
    byte[] originalKey = keyStore1.getSymmetricKey();

    // Second store: load existing key
    EncryptionKeyStore keyStore2 = new EncryptionKeyStore(repository, "AES-GCM-256");
    keyStore2.initialize(null, null);
    byte[] loadedKey = keyStore2.getSymmetricKey();

    assertThat(loadedKey).isEqualTo(originalKey);
  }

  @Test
  public void testLoadWrappedKey() throws Exception {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(2048, new SecureRandom());
    KeyPair keyPair = keyGen.generateKeyPair();

    // First store: create and wrap key
    EncryptionKeyStore keyStore1 = new EncryptionKeyStore(repository, "AES-GCM-256");
    keyStore1.initialize(keyPair.getPublic(), "Test");
    byte[] originalKey = keyStore1.getSymmetricKey();

    // Second store: load and unwrap key
    EncryptionKeyStore keyStore2 = new EncryptionKeyStore(repository, "AES-GCM-256");
    keyStore2.setPrivateKey(keyPair.getPrivate());
    keyStore2.initialize(null, null);
    byte[] unwrappedKey = keyStore2.getSymmetricKey();

    assertThat(unwrappedKey).isEqualTo(originalKey);
  }
}
