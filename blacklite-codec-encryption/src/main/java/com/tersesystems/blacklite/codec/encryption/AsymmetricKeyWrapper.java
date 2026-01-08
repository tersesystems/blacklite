package com.tersesystems.blacklite.codec.encryption;

import java.security.PrivateKey;
import java.security.PublicKey;

/** Strategy interface for wrapping symmetric keys with asymmetric encryption. */
public interface AsymmetricKeyWrapper {

  /**
   * @return The algorithm identifier (e.g., "RSA-OAEP-2048", "ECIES-P256")
   */
  String getAlgorithm();

  /**
   * Wrap (encrypt) a symmetric key with a public key.
   *
   * @param symmetricKey the symmetric key to wrap
   * @param publicKey the public key
   * @return the wrapped symmetric key
   * @throws EncryptionException if wrapping fails
   */
  byte[] wrap(byte[] symmetricKey, PublicKey publicKey) throws EncryptionException;

  /**
   * Unwrap (decrypt) a symmetric key with a private key.
   *
   * @param wrappedKey the wrapped symmetric key
   * @param privateKey the private key
   * @return the unwrapped symmetric key
   * @throws EncryptionException if unwrapping fails
   */
  byte[] unwrap(byte[] wrappedKey, PrivateKey privateKey) throws EncryptionException;
}
