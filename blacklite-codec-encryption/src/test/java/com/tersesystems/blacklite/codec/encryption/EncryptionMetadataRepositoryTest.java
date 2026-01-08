package com.tersesystems.blacklite.codec.encryption;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EncryptionMetadataRepositoryTest {

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
  public void testInitializeCreatesTable() throws Exception {
    // Should not throw
    repository.initialize();
  }

  @Test
  public void testSaveAndRetrieve() throws Exception {
    byte[] key = new byte[] {1, 2, 3, 4};
    EncryptionMetadata metadata =
        new EncryptionMetadata(0, "AES-GCM-256", key, "RSA-OAEP-2048", "Test comment", 123456L);

    repository.save(metadata);

    Optional<EncryptionMetadata> retrieved = repository.get();
    assertThat(retrieved).isPresent();
    assertThat(retrieved.get().getAlgorithm()).isEqualTo("AES-GCM-256");
    assertThat(retrieved.get().getEncryptedKey()).isEqualTo(key);
    assertThat(retrieved.get().getKeyEncryptionAlgorithm()).isEqualTo("RSA-OAEP-2048");
    assertThat(retrieved.get().getKeyEncryptionComment()).isEqualTo("Test comment");
    assertThat(retrieved.get().getCreatedAt()).isEqualTo(123456L);
  }

  @Test
  public void testGetWhenEmpty() throws Exception {
    Optional<EncryptionMetadata> result = repository.get();
    assertThat(result).isEmpty();
  }

  @Test
  public void testSaveNullKeyEncryption() throws Exception {
    byte[] key = new byte[] {5, 6, 7, 8};
    EncryptionMetadata metadata = new EncryptionMetadata(0, "AES-GCM-256", key, null, null, 789L);

    repository.save(metadata);

    Optional<EncryptionMetadata> retrieved = repository.get();
    assertThat(retrieved).isPresent();
    assertThat(retrieved.get().getKeyEncryptionAlgorithm()).isNull();
    assertThat(retrieved.get().getKeyEncryptionComment()).isNull();
    assertThat(retrieved.get().isKeyWrapped()).isFalse();
  }
}
