# Blacklite Encryption Feature Design

**Date:** 2026-01-01
**Status:** Design Phase

## Overview

Add symmetric encryption capability to Blacklite log entries using a codec wrapper pattern. Each database will have a unique AES key that can optionally be encrypted with a public key, requiring the corresponding private key for decryption at runtime.

## Goals

1. Encrypt log content at rest using AES-GCM-256
2. Support per-database unique encryption keys
3. Optionally wrap encryption keys with public key cryptography (RSA-OAEP)
4. Work transparently with existing codecs (especially ZStandard dictionary compression)
5. Maintain zero external dependencies (Java 8 standard library only)
6. Provide clear error messages when private keys are missing

## Architecture

### Codec Wrapper Pattern

The encryption feature uses a **wrapper codec** that composes with any existing codec:

```
Write Pipeline: plaintext → InnerCodec.encode() → EncryptionCodec.encode() → SQLite
Read Pipeline:  SQLite → EncryptionCodec.decode() → InnerCodec.decode() → plaintext
```

Example with ZStandard:
```
plaintext → ZStdDictCodec (compress) → EncryptionCodec (encrypt) → encrypted blob → SQLite
```

### Key Components

#### 1. EncryptionCodec

Main codec implementation that wraps an inner codec and adds encryption layer.

```java
public class EncryptionCodec implements Codec {
  private Codec innerCodec;
  private EncryptionKeyStore keyStore;
  private SymmetricEncryption encryption;

  @Override
  public byte[] encode(byte[] unencoded) throws CodecException {
    byte[] innerEncoded = innerCodec.encode(unencoded);
    return encryption.encrypt(innerEncoded, keyStore.getSymmetricKey());
  }

  @Override
  public byte[] decode(byte[] encoded) throws CodecException {
    byte[] decrypted = encryption.decrypt(encoded, keyStore.getSymmetricKey());
    return innerCodec.decode(decrypted);
  }
}
```

#### 2. SymmetricEncryption Interface

Strategy pattern for different symmetric encryption algorithms.

```java
interface SymmetricEncryption {
  String getAlgorithm(); // "AES-GCM-256", "ChaCha20-Poly1305", etc.
  byte[] encrypt(byte[] plaintext, byte[] key) throws EncryptionException;
  byte[] decrypt(byte[] ciphertext, byte[] key) throws EncryptionException;
}
```

**Initial Implementation:** `AesGcmEncryption`
- AES-256-GCM with random 12-byte IVs
- IV prepended to ciphertext
- Authenticated encryption (AEAD) - detects tampering

#### 3. EncryptionKeyStore

Manages symmetric key lifecycle: generation, storage, retrieval.

**Key Generation:**
```java
private byte[] generateSymmetricKey(String algorithm) {
  int keySize = 256; // AES-256
  byte[] key = new byte[keySize / 8];

  SecureRandom secureRandom = new SecureRandom();
  // Force seeding to ensure entropy pool is ready
  secureRandom.nextBytes(new byte[1]);
  secureRandom.nextBytes(key);

  return key;
}
```

**Storage:** Keys stored in SQLite metadata table (see below).

#### 4. Metadata Table

Each database contains encryption metadata:

```sql
CREATE TABLE blacklite_encryption_metadata (
  id INTEGER PRIMARY KEY,
  algorithm TEXT NOT NULL,              -- e.g., "AES-GCM-256"
  encrypted_key BLOB NOT NULL,          -- Symmetric key (plaintext or wrapped)
  key_encryption_algorithm TEXT,        -- NULL or "RSA-OAEP-2048"
  key_encryption_comment TEXT,          -- User-provided description
  created_at INTEGER NOT NULL
);
```

**Metadata Fields:**
- `algorithm`: Symmetric encryption algorithm used for content
- `encrypted_key`: The AES key, either plaintext or wrapped with public key
- `key_encryption_algorithm`: NULL if key is stored plaintext, otherwise describes wrapping algorithm
- `key_encryption_comment`: User-provided comment (e.g., "Production logs - requires ops team private key")
- `created_at`: Timestamp when key was generated

#### 5. AsymmetricKeyWrapper Interface

Strategy for wrapping symmetric keys with public key cryptography.

```java
interface AsymmetricKeyWrapper {
  String getAlgorithm(); // "RSA-OAEP-2048", "ECIES-P256", etc.
  byte[] wrap(byte[] symmetricKey, PublicKey publicKey);
  byte[] unwrap(byte[] wrappedKey, PrivateKey privateKey);
}
```

**Initial Implementation:** `RsaOaepKeyWrapper`
- RSA-OAEP with SHA-256
- 2048-bit or 4096-bit key support
- Uses Java 8 standard library (javax.crypto)

**Future Extensions:** Interface is designed to support ECIES or other algorithms when Java 8 compatibility is dropped or via optional dependencies.

#### 6. Private Key Provider Chain

Chain of responsibility pattern for locating private keys at runtime.

```java
interface PrivateKeyProvider {
  Optional<PrivateKey> getPrivateKey(String comment);
}
```

**Provider Priority Order:**
1. **ProgrammaticKeyProvider** - Keys set via API (`EntryStore.setPrivateKey()`)
2. **EnvironmentVariableProvider** - `BLACKLITE_PRIVATE_KEY_FILE` env var
3. **ConfigurationFileProvider** - `privateKeyFile` appender config property
4. **SystemPropertyProvider** - `blacklite.privateKey.file` system property

**Fail-Fast Behavior:**

During `EntryStore.initialize()`:
```java
EncryptionMetadata metadata = readMetadataTable();
if (metadata.keyEncryptionAlgorithm != null) {
  Optional<PrivateKey> privateKey = providerChain.getPrivateKey(metadata.comment);
  if (!privateKey.isPresent()) {
    throw EncryptionException.missingPrivateKey(
      metadata.keyEncryptionAlgorithm,
      metadata.comment
    );
  }
  symmetricKey = keyWrapper.unwrap(metadata.encryptedKey, privateKey.get());
}
```

This fails immediately on database open if a required private key is missing, rather than failing later during read operations.

## Integration with ZStandard Dictionary Compression

### Data Flow with Both Compression and Encryption

```
Write: plaintext → ZStdDictCodec.encode() → compressed → EncryptionCodec.encode() → encrypted → SQLite
Read:  SQLite → encrypted → EncryptionCodec.decode() → compressed → ZStdDictCodec.decode() → plaintext
```

### Dictionary Encryption

The ZStandard dictionary is trained on **plaintext** (before encryption) for optimal compression, then the dictionary itself is **encrypted at rest**.

**Enhanced ZStdDictSqliteRepository:**

```java
public class ZStdDictSqliteRepository implements ZstdDictRepository {
  private Optional<EncryptionCodec> encryptionCodec = Optional.empty();

  public void save(byte[] dictBytes) {
    byte[] toStore = dictBytes;
    if (encryptionCodec.isPresent()) {
      toStore = encryptionCodec.get().getEncryption()
                  .encrypt(dictBytes, encryptionCodec.get().getSymmetricKey());
    }
    // Store in blacklite_zstd_dict table
  }

  public Optional<ZStdDict> mostRecent() {
    byte[] stored = // read from blacklite_zstd_dict table
    if (encryptionCodec.isPresent()) {
      stored = encryptionCodec.get().getEncryption()
                 .decrypt(stored, encryptionCodec.get().getSymmetricKey());
    }
    return Optional.of(new ZStdDict(dictId, stored));
  }
}
```

**Configuration Example:**

```xml
<codec class="com.tersesystems.blacklite.codec.encryption.EncryptionCodec">
  <innerCodec class="com.tersesystems.blacklite.codec.zstd.ZStdDictCodec">
    <repository class="com.tersesystems.blacklite.codec.zstd.ZStdDictSqliteRepository"/>
    <level>3</level>
  </innerCodec>
  <encryption class="com.tersesystems.blacklite.codec.encryption.AesGcmEncryption"/>
  <publicKeyFile>/path/to/public.pem</publicKeyFile>
  <publicKeyComment>Production deployment - requires ops private key</publicKeyComment>
</codec>
```

## Archiving with Encryption

### Archive Strategy

Archive databases **inherit** the encryption metadata from the main database. This means:
- Same symmetric key is used for both main and archive databases
- Metadata table is copied during archive creation
- No re-encryption overhead during archiving
- Archives have same access requirements as main database

### Implementation

The `RollingArchiver` already uses SQLite custom functions with codec.encode():

```java
// In RollingArchiver.archive()
Function codecFunction = new Function() {
  @Override
  protected void xFunc() throws SQLException {
    // codec is EncryptionCodec wrapping ZStdDictCodec
    result(codec.encode(value_blob(0)));
  }
};
Function.create(conn, "encode", codecFunction);

// Archive query - transparently applies compression + encryption
String sql = "INSERT INTO archive.entries " +
             "SELECT epoch_secs, nanos, level, encode(content) FROM main.entries " +
             "WHERE epoch_secs < ?";
```

**Archive Initialization:**

When creating a new archive database:
1. Create archive database schema
2. Copy `blacklite_encryption_metadata` table from main database
3. Copy `blacklite_zstd_dict` table if using ZStd (already encrypted)
4. Perform archive query using same codec instance

This ensures archives use the same encryption key and can be decrypted with the same private key as the main database.

## Reader Tool Integration

The `blacklite-reader` CLI tool will transparently handle encrypted databases.

### Command-Line Interface

```bash
# Unencrypted database (works as before)
blacklite-reader query logs.db --since "yesterday"

# Encrypted database - private key required
blacklite-reader query logs.db --since "yesterday" --private-key /path/to/private.pem

# Or via environment variable
export BLACKLITE_PRIVATE_KEY_FILE=/path/to/private.pem
blacklite-reader query logs.db --since "yesterday"
```

### Initialization Logic

```java
public class Reader {
  public void initialize(String dbPath, Optional<String> privateKeyFile) {
    EncryptionMetadata metadata = readMetadataTable(dbPath);

    if (metadata.keyEncryptionAlgorithm != null) {
      // Database is encrypted - need private key
      PrivateKey privateKey = loadPrivateKeyViaProviderChain(privateKeyFile);
      byte[] symmetricKey = unwrapKey(metadata.encryptedKey, privateKey);

      // Build codec chain: Encryption(ZStd(...))
      codec = new EncryptionCodec(new ZStdDictCodec(...), symmetricKey);
    } else {
      // Not encrypted, just use ZStd if present
      codec = new ZStdDictCodec(...);
    }
  }
}
```

### Error Messages

If user attempts to read encrypted database without private key:

```
Error: Database is encrypted and requires a private key

Database Encryption Details:
  Symmetric Algorithm: AES-GCM-256
  Key Encryption: RSA-OAEP-2048
  Comment: Production logs - requires ops team private key

Provide private key using:
  --private-key /path/to/key.pem
  or set BLACKLITE_PRIVATE_KEY_FILE environment variable
```

## Module Structure

### New Module: blacklite-codec-encryption

```
blacklite-codec-encryption/
├── src/main/java/com/tersesystems/blacklite/codec/encryption/
│   ├── EncryptionCodec.java                  // Main codec wrapper
│   ├── EncryptionException.java              // Exception type
│   ├── SymmetricEncryption.java              // Strategy interface
│   ├── AesGcmEncryption.java                 // AES-GCM-256 implementation
│   ├── EncryptionKeyStore.java               // Key lifecycle management
│   ├── EncryptionMetadata.java               // Metadata model
│   ├── EncryptionMetadataRepository.java     // SQLite metadata operations
│   ├── AsymmetricKeyWrapper.java             // Key wrapping interface
│   ├── RsaOaepKeyWrapper.java                // RSA-OAEP implementation
│   ├── PrivateKeyProvider.java               // Provider chain interface
│   ├── PrivateKeyProviderChain.java          // Chain of responsibility impl
│   ├── ProgrammaticKeyProvider.java          // API-based provider
│   ├── EnvironmentVariableProvider.java      // Env var provider
│   ├── ConfigurationFileProvider.java        // Config file provider
│   ├── SystemPropertyProvider.java           // System property provider
│   └── PemKeyReader.java                     // PEM/PKCS8 key file reader
└── src/test/java/...                         // Test classes
```

### Dependencies

```gradle
dependencies {
    implementation project(':blacklite-api')
    // No external dependencies - Java 8 standard library only

    testImplementation 'org.junit.jupiter:junit-jupiter'
    testImplementation 'org.assertj:assertj-core'
    testImplementation 'org.awaitility:awaitility'
}
```

### Configuration Examples

#### Logback Configuration

```xml
<appender name="BLACKLITE" class="com.tersesystems.blacklite.logback.BlackliteAppender">
  <codec class="com.tersesystems.blacklite.codec.encryption.EncryptionCodec">
    <innerCodec class="com.tersesystems.blacklite.codec.zstd.ZStdDictCodec">
      <repository class="com.tersesystems.blacklite.codec.zstd.ZStdDictSqliteRepository"/>
      <level>3</level>
    </innerCodec>
    <encryption class="com.tersesystems.blacklite.codec.encryption.AesGcmEncryption"/>
    <publicKeyFile>/etc/blacklite/public.pem</publicKeyFile>
    <publicKeyComment>Production logs - contact ops for private key</publicKeyComment>
  </codec>

  <entryStore class="com.tersesystems.blacklite.DefaultEntryStore">
    <file>/var/log/app/blacklite.db</file>
  </entryStore>
</appender>
```

#### Log4J 2 Configuration

```xml
<Blacklite name="BLACKLITE">
  <Codec type="Encryption">
    <InnerCodec type="ZStdDict">
      <Repository type="SqliteDict"/>
      <Level>3</Level>
    </InnerCodec>
    <Encryption type="AesGcm"/>
    <PublicKeyFile>/etc/blacklite/public.pem</PublicKeyFile>
    <PublicKeyComment>Production logs - contact ops for private key</PublicKeyComment>
  </Codec>

  <EntryStore type="Default">
    <File>/var/log/app/blacklite.db</File>
  </EntryStore>
</Blacklite>
```

## Testing Strategy

### Unit Tests

1. **AesGcmEncryptionTest**
   - Verify encrypt/decrypt roundtrip with random data
   - Verify IV uniqueness across operations
   - Verify authentication tag prevents tampering
   - Verify different keys produce different ciphertext

2. **EncryptionKeyStoreTest**
   - Verify SecureRandom key generation produces unique keys
   - Verify metadata storage and retrieval
   - Verify key wrapping with public key
   - Verify key unwrapping with private key

3. **RsaOaepKeyWrapperTest**
   - Verify wrap/unwrap roundtrip
   - Verify different RSA keys produce different wrapped keys
   - Verify wrong private key fails to unwrap

4. **PrivateKeyProviderChainTest**
   - Verify provider priority order
   - Verify fallback behavior
   - Verify environment variable provider
   - Verify config file provider

### Integration Tests

1. **EncryptionCodecTest**
   - Full codec chain: plaintext → ZStd → AES → SQLite → AES → ZStd → plaintext
   - Verify with IdentityCodec (no compression)
   - Verify with ZStdDictCodec (with compression)

2. **EncryptedDictionaryTest**
   - Verify ZStd dictionary training on plaintext
   - Verify dictionary encryption at rest
   - Verify dictionary decryption on load
   - Verify compression ratios are maintained

3. **EncryptedArchiveTest**
   - Verify archiving with encryption
   - Verify metadata inheritance from main to archive
   - Verify archive can be read with same private key
   - Verify archive with ZStd + encryption

4. **ReaderToolEncryptionTest**
   - End-to-end test with blacklite-reader CLI
   - Verify reading encrypted database with private key
   - Verify error message when private key missing
   - Verify environment variable support

### Security Tests

1. **Key Isolation Test**
   - Verify different databases get different AES keys
   - Verify no key reuse across databases

2. **IV Uniqueness Test**
   - Verify IVs are unique per encryption operation
   - Verify same plaintext produces different ciphertext

3. **Authentication Test**
   - Verify tampering detection (modify ciphertext)
   - Verify wrong key detection

4. **Key Erasure Test**
   - Verify sensitive byte arrays are zeroed after use
   - Verify keys cleared on codec close

## Error Handling

### EncryptionException

```java
public class EncryptionException extends CodecException {

  // Database requires private key but none found
  public static EncryptionException missingPrivateKey(
      String algorithm, String comment) {
    return new EncryptionException(
      "Database requires private key for decryption.\n" +
      "  Algorithm: " + algorithm + "\n" +
      "  Comment: " + comment + "\n" +
      "Provide private key via:\n" +
      "  - EntryStore.setPrivateKey(PrivateKey)\n" +
      "  - BLACKLITE_PRIVATE_KEY_FILE environment variable\n" +
      "  - privateKeyFile configuration property\n" +
      "  - blacklite.privateKey.file system property"
    );
  }

  // Decryption failed (wrong key, corruption, tampering)
  public static EncryptionException decryptionFailed(Throwable cause) {
    return new EncryptionException(
      "Failed to decrypt content. Possible causes:\n" +
      "  - Wrong private key provided\n" +
      "  - Data corruption detected\n" +
      "  - Data tampering detected (authentication failure)\n" +
      "  - Incompatible encryption algorithm",
      cause
    );
  }

  // Key generation failed
  public static EncryptionException keyGenerationFailed(Throwable cause) {
    return new EncryptionException(
      "Failed to generate encryption key. " +
      "Ensure system has sufficient entropy.",
      cause
    );
  }

  // Invalid key file
  public static EncryptionException invalidKeyFile(String path, Throwable cause) {
    return new EncryptionException(
      "Failed to read key file: " + path + "\n" +
      "Ensure file is valid PEM or PKCS8 format.",
      cause
    );
  }
}
```

## Security Considerations

### Key Material Handling

1. **Secure Generation**
   - Use `SecureRandom` for all key and IV generation
   - Force entropy pool seeding before key generation
   - Generate 256-bit keys for AES-256

2. **Key Erasure**
   - Zero sensitive byte arrays after use: `Arrays.fill(key, (byte)0)`
   - Clear keys on codec close
   - Avoid string representations of keys (no logging)

3. **IV Management**
   - Generate fresh random 12-byte IV for each encryption operation
   - Prepend IV to ciphertext (standard AES-GCM practice)
   - Never reuse IVs with the same key

### Authentication and Integrity

1. **Authenticated Encryption**
   - AES-GCM provides AEAD (Authenticated Encryption with Associated Data)
   - Authentication tag prevents tampering
   - Decryption fails if ciphertext is modified

2. **No Key Rotation**
   - Archive logging data is immutable
   - Each database has a single key for its lifetime
   - Key rotation not applicable to write-once log archives

### Private Key Storage

1. **Out of Scope**
   - Private key storage/management is user responsibility
   - Recommend hardware security modules (HSMs) for production
   - Support standard key formats (PEM, PKCS8)

2. **Provider Chain Flexibility**
   - Programmatic API allows integration with key management systems
   - Environment variables support secrets management tools
   - Multiple provider options for different deployment scenarios

## Future Enhancements

### Potential Extensions (Not in Initial Implementation)

1. **Additional Symmetric Algorithms**
   - ChaCha20-Poly1305 (via `ChaCha20Poly1305Encryption` implementation)
   - AES-CBC with HMAC (for compatibility scenarios)

2. **Additional Key Wrapping Algorithms**
   - ECIES with P-256/P-384 (when Java 8 compatibility dropped or via BouncyCastle)
   - Age encryption (via external library)

3. **Key Management Features**
   - Key rotation for active (non-archive) databases
   - Multiple key support for phased rotation
   - Key derivation from passwords (PBKDF2)

4. **Reader Tool Enhancements**
   - SSH agent integration for private keys
   - Interactive private key password prompt
   - Hardware security module (HSM) support

## Implementation Notes

### Thread Safety

- `EncryptionCodec` instances are thread-safe
- `SecureRandom` is thread-safe
- Symmetric key is read-only after initialization
- Each encryption operation gets its own IV

### Performance Considerations

1. **Encryption Overhead**
   - AES-GCM is hardware-accelerated on modern CPUs (AES-NI)
   - Typical overhead: ~100-500 ns per operation
   - Negligible compared to SQLite write time (~2 μs/op)

2. **Key Unwrapping**
   - RSA operations are expensive (~1-10 ms)
   - Only happens once during initialization
   - Not in write path (hot path uses symmetric key only)

3. **Memory Usage**
   - Symmetric keys: 32 bytes (AES-256)
   - IV overhead: 12 bytes per log entry
   - Authentication tag: 16 bytes per log entry
   - Total overhead: ~28 bytes per encrypted entry

### Compatibility

- **Java 8+** required (javax.crypto.Cipher, SecureRandom)
- **SQLite 3.x** (no special requirements)
- **PEM format** for public/private keys (standard)
- **Backward compatible** - existing databases without encryption continue to work

## Summary

This design adds robust encryption to Blacklite using:

1. **Codec wrapper pattern** - Composable encryption layer
2. **Per-database unique keys** - Generated with SecureRandom
3. **Optional public key wrapping** - RSA-OAEP for secure key distribution
4. **Transparent ZStd integration** - Dictionary compression + encryption
5. **Clear error handling** - Fail-fast with actionable messages
6. **Zero external dependencies** - Pure Java 8 standard library

The implementation maintains Blacklite's design philosophy of minimal dependencies, high performance, and operational simplicity while adding enterprise-grade encryption capability.
