package com.tersesystems.blacklite.codec.encryption;

import com.tersesystems.blacklite.codec.CodecException;

public class EncryptionException extends CodecException {

  public EncryptionException(String message) {
    super(message);
  }

  public EncryptionException(String message, Throwable cause) {
    super(message, cause);
  }

  public static EncryptionException missingPrivateKey(String algorithm, String comment) {
    return new EncryptionException(
        "Database requires private key for decryption.\n"
            + "  Algorithm: " + algorithm + "\n"
            + "  Comment: " + comment + "\n"
            + "Provide private key via:\n"
            + "  - EntryStore.setPrivateKey(PrivateKey)\n"
            + "  - BLACKLITE_PRIVATE_KEY_FILE environment variable\n"
            + "  - privateKeyFile configuration property\n"
            + "  - blacklite.privateKey.file system property");
  }

  public static EncryptionException decryptionFailed(Throwable cause) {
    return new EncryptionException(
        "Failed to decrypt content. Possible causes:\n"
            + "  - Wrong private key provided\n"
            + "  - Data corruption detected\n"
            + "  - Data tampering detected (authentication failure)\n"
            + "  - Incompatible encryption algorithm",
        cause);
  }

  public static EncryptionException keyGenerationFailed(Throwable cause) {
    return new EncryptionException(
        "Failed to generate encryption key. Ensure system has sufficient entropy.", cause);
  }

  public static EncryptionException invalidKeyFile(String path, Throwable cause) {
    return new EncryptionException(
        "Failed to read key file: " + path + "\nEnsure file is valid PEM or PKCS8 format.", cause);
  }
}
