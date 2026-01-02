package com.tersesystems.blacklite.codec.encryption;

import java.security.PrivateKey;
import java.security.PublicKey;
import javax.crypto.Cipher;

/** RSA-OAEP key wrapping implementation. */
public class RsaOaepKeyWrapper implements AsymmetricKeyWrapper {

  private static final String ALGORITHM = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

  @Override
  public String getAlgorithm() {
    return "RSA-OAEP-SHA256";
  }

  @Override
  public byte[] wrap(byte[] symmetricKey, PublicKey publicKey) throws EncryptionException {
    try {
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.WRAP_MODE, publicKey);
      return cipher.wrap(new javax.crypto.spec.SecretKeySpec(symmetricKey, "AES"));
    } catch (Exception e) {
      throw new EncryptionException("Failed to wrap symmetric key", e);
    }
  }

  @Override
  public byte[] unwrap(byte[] wrappedKey, PrivateKey privateKey) throws EncryptionException {
    try {
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.UNWRAP_MODE, privateKey);
      javax.crypto.SecretKey unwrapped =
          (javax.crypto.SecretKey) cipher.unwrap(wrappedKey, "AES", Cipher.SECRET_KEY);
      return unwrapped.getEncoded();
    } catch (Exception e) {
      throw new EncryptionException("Failed to unwrap symmetric key", e);
    }
  }
}
