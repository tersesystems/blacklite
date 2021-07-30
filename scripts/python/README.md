
## Python support with sqlite-utils

Extracting content is simple and easy using [sqlite-utils](https://sqlite-utils.readthedocs.io/en/stable/):

```bash
sudo apt install python3-pip
sudo pip3 install sqlite-utils
sudo pip3 install zstandard
```

And then to read the compressed data:

```python
```

This produces:

```bash
❱ ./reader.py
epoch_secs =  1603055625 level =  10000 message =  debugging is fun!!! 2020-10-18T21:13:45.317090Z
```

You can also use SQL custom functions, which allows you to use SQLite's JSON functions natively:

```python

```
