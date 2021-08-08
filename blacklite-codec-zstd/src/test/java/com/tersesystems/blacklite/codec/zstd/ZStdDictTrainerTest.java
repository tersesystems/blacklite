package com.tersesystems.blacklite.codec.zstd;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.luben.zstd.Zstd;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

public class ZStdDictTrainerTest {

  // This test can take up to 30 seconds to run.
  @Test
  public void testDictionary() throws Exception {
    // then the sample size is 26000 bytes.
    int sampleSize = 100_000 * 1024;

    // Dictionary size can be up to 10 MeB in bytes.  This is the output from training.
    int dictSize = 10485760;

    AtomicReference<byte[]> ref = new AtomicReference<>();
    // Run through enough that we get dictionary compression.
    CountDownLatch latch = new CountDownLatch(1);
    Consumer<byte[]> consumer =
        f -> {
          ref.set(f);
          latch.countDown();
        };

    // https://github.com/luben/zstd-jni/blob/master/src/test/scala/ZstdDict.scala
    ZStdDictTrainer trainer = new ZStdDictTrainer(sampleSize, dictSize, consumer);
    for (int i = 0; i < sampleSize; i++) {
      byte[] input =
          ("Message " + i + " at " + System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8);

      trainer.train(input);
    }
    latch.await();

    byte[] dict = requireNonNull(ref.get());
    long dictionaryId = Zstd.getDictIdFromDict(dict);

    // Check we have a dictionary at the end of this.
    assertThat(dictionaryId).isGreaterThan(-1);
  }
}
