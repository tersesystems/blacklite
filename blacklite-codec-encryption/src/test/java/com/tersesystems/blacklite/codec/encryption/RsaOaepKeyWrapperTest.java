package com.tersesystems.blacklite.codec.encryption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RsaOaepKeyWrapperTest {

  private KeyPair keyPair;
  private RsaOaepKeyWrapper wrapper;

  @BeforeEach
  public void setUp() throws Exception {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(2048, new SecureRandom());
    keyPair = keyGen.generateKeyPair();
    wrapper = new RsaOaepKeyWrapper();
  }

  @Test
  public void testAlgorithmName() {
    assertThat(wrapper.getAlgorithm()).contains("RSA-OAEP");
  }

  @Test
  public void testWrapUnwrapRoundtrip() throws Exception {
    byte[] symmetricKey = new byte[32]; // 256-bit AES key
    new SecureRandom().nextBytes(symmetricKey);

    byte[] wrapped = wrapper.wrap(symmetricKey, keyPair.getPublic());
    byte[] unwrapped = wrapper.unwrap(wrapped, keyPair.getPrivate());

    assertThat(unwrapped).isEqualTo(symmetricKey);
  }

  @Test
  public void testDifferentPublicKeysProduceDifferentWrappedKeys() throws Exception {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(2048, new SecureRandom());
    KeyPair keyPair2 = keyGen.generateKeyPair();

    byte[] symmetricKey = new byte[32];
    new SecureRandom().nextBytes(symmetricKey);

    byte[] wrapped1 = wrapper.wrap(symmetricKey, keyPair.getPublic());
    byte[] wrapped2 = wrapper.wrap(symmetricKey, keyPair2.getPublic());

    assertThat(wrapped1).isNotEqualTo(wrapped2);
  }

  @Test
  public void testWrongPrivateKeyFailsUnwrap() throws Exception {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(2048, new SecureRandom());
    KeyPair wrongKeyPair = keyGen.generateKeyPair();

    byte[] symmetricKey = new byte[32];
    new SecureRandom().nextBytes(symmetricKey);

    byte[] wrapped = wrapper.wrap(symmetricKey, keyPair.getPublic());

    assertThatThrownBy(() -> wrapper.unwrap(wrapped, wrongKeyPair.getPrivate()))
        .isInstanceOf(EncryptionException.class);
  }
}
