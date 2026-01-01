import pytest
from blacklite_tools.codecs import IdentityCodec, ZstandardCodec
import zstandard as zstd


class TestIdentityCodec:
    def test_decode_returns_input_unchanged(self):
        codec = IdentityCodec()
        input_data = b"test data"
        assert codec.decode(input_data) == input_data

    def test_decode_empty_input(self):
        codec = IdentityCodec()
        assert codec.decode(b"") == b""


class TestZstandardCodec:
    def test_decode_decompresses_data(self):
        # Compress some data
        original = b"test data for compression"
        cctx = zstd.ZstdCompressor()
        compressed = cctx.compress(original)

        # Test decompression
        dctx = zstd.ZstdDecompressor()
        codec = ZstandardCodec(dctx)
        assert codec.decode(compressed) == original

    def test_decode_with_dictionary(self):
        # Create dictionary with larger samples
        samples = [b"test data " * 100 for _ in range(100)]
        dict_data = zstd.train_dictionary(1024, samples)

        # Compress with dictionary
        original = b"test data sample content"
        cctx = zstd.ZstdCompressor(dict_data=dict_data)
        compressed = cctx.compress(original)

        # Decompress with dictionary
        dctx = zstd.ZstdDecompressor(dict_data=dict_data)
        codec = ZstandardCodec(dctx)
        assert codec.decode(compressed) == original

    def test_decode_invalid_data_raises_error(self):
        dctx = zstd.ZstdDecompressor()
        codec = ZstandardCodec(dctx)

        with pytest.raises(zstd.ZstdError):
            codec.decode(b"not compressed data")
