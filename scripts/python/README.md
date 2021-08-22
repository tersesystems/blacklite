
## Python support with sqlite-utils

Ideally use virtualenv to segment your environment:

```bash
# or whatever director you have blacklite installed in
cd blacklite
# see virtualenv docs for installation
sudo easy_install virtualenv
. venv/bin/activate
```

Extracting content is simple and easy using [sqlite-utils](https://sqlite-utils.readthedocs.io/en/stable/):

```bash
# or pipx etc
pip install sqlite-utils zstandard click
```

## ZStandard Reader

Prints out the first matching line from the database.  Works with compressed/uncompressed databases.

```
./reader.py ./blacklite.db
```

## ZStandard Compress

Converts a database containing raw content to a database containing zstandard compressed content.

```
./zstd-compress.py ../data/blacklite.db ./blacklite-zstd.db
```

You can specify a zstandard dictionary which can compress entries further:

```
./zstd-compress.py --dict ../data/blacklite.db ./blacklite-zstd-dict.db
```

## ZStandard Decompress

You can decompress a database into an uncompressed one:

```
./zstd-decompress.py ./blacklite-zstd.db ./blacklite-decompressed.db
```

This will also work transparently with databases using a dictionary:

```
./zstd-decompress.py ./blacklite-zstd-dict.db ./blacklite-decompressed.db
```

