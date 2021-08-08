package com.tersesystems.blacklite.codec.zstd;

import com.tersesystems.blacklite.codec.CodecException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * The zstd trainer codec uses zstd without compression, but also trains the message set using a
 * dictionary. When the trainer has enough sample messages, the hook is called.
 */
public class ZStdDictTrainer {
  private final com.github.luben.zstd.ZstdDictTrainer trainer;
  private final AtomicBoolean trainDictionary;
  private final Consumer<byte[]> completionHook;
  private ExecutorService executor;

  public ZStdDictTrainer(int sampleSize, int dictSize, Consumer<byte[]> completionHook)
      throws CodecException {
    trainDictionary = new AtomicBoolean(true);
    this.trainer = new com.github.luben.zstd.ZstdDictTrainer(sampleSize, dictSize);
    this.completionHook = completionHook;
  }

  // Can take about 16 seconds to train on laptop, and will eat
  // the core while it's doing that.  Any way to signal to the
  // application that this is expected behavior?
  public void train(byte[] src) throws CodecException {
    // Already trained up
    if (!trainDictionary.get()) {
      return;
    }

    // Adding this sample to this trainer didn't fill the dictionary.
    if (this.trainer.addSample(src)) {
      return;
    }

    // If we got to this point, we want to run a CPU bound task to train the
    // dictionary without slowing down processing.
    if (trainDictionary.getAndSet(false)) {
      this.executor =
          Executors.newSingleThreadExecutor(
              r -> {
                Thread thread = new Thread(r);
                // if the JVM exits when we're training, then bail.
                thread.setDaemon(true);
                thread.setName("zstd-training-executor");
                return thread;
              });
      CompletableFuture.supplyAsync(trainer::trainSamples, executor)
          .thenAccept(completionHook)
          .thenRun(this::close);
    }
  }

  private void close() {
    // The actual training takes 16 seconds
    if (executor != null && !executor.isShutdown()) {
      executor.shutdown();
    }
  }
}
