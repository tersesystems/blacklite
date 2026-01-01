"""Codec classes for encoding/decoding blacklite database content."""

from typing import ByteString
import zstandard as zstd


class Codec:
    """Base codec interface."""

    def decode(self, data: ByteString) -> ByteString:
        """Decode bytes to bytes."""
        ...


class IdentityCodec(Codec):
    """Pass-through codec that returns data unchanged."""

    def decode(self, data: ByteString) -> ByteString:
        """Return input unchanged."""
        return data


class ZstandardCodec(Codec):
    """Zstandard compression codec."""

    def __init__(self, dctx: zstd.ZstdDecompressor):
        """Initialize with a ZstdDecompressor context.

        Args:
            dctx: ZstdDecompressor instance, optionally configured with dictionary
        """
        self.dctx = dctx

    def decode(self, data: ByteString) -> ByteString:
        """Decompress zstandard-compressed data.

        Args:
            data: Compressed bytes

        Returns:
            Decompressed bytes

        Raises:
            zstd.ZstdError: If data is not valid zstd compressed data
        """
        return self.dctx.decompress(data)
