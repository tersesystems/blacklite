package com.tersesystems.blacklite.codec.zstd;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.tersesystems.blacklite.StatusReporter;
import java.io.File;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;

public class ZStdDictCodecTest {

  @Test
  public void testBytesWithCodec() throws Exception {
    // https://github.com/luben/zstd-jni/blob/master/src/test/scala/Zstd.scala
    ClassLoader classLoader = getClass().getClassLoader();
    File file = new File(classLoader.getResource("message.json").getFile());
    final byte[] bytes = Files.readAllBytes(file.toPath());
    ZStdDictSqliteRepository repo = new ZStdDictSqliteRepository();
    repo.setFile("jdbc:sqlite:");
    repo.initialize();
    //      ZstdDictSqliteRepository dictRepository =
    //          new ZstdDictSqliteRepository(StatusReporter.DEFAULT, repo, directoryPath);

    // Path directoryPath = Files.createTempDirectory("blacklite");
    // final Path dictionaryPath = directoryPath.resolve("dictionary");
    // ZstdDictFileRepository dictRepository = new ZstdDictFileRepository();
    ZstdDictCodec codec = new ZstdDictCodec();
    codec.setRepository(repo);
    codec.initialize(StatusReporter.DEFAULT);

    byte[] compressed = codec.encode(bytes);
    byte[] decompressed = codec.decode(compressed);

    assertThat(decompressed.length).isEqualTo(bytes.length);
  }
}
