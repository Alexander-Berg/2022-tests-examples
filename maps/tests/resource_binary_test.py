import subprocess

import pytest

from maps.infra.sandbox import get_binary


VALID_BINARY_NAME = 'binary'
INVALID_BINARY_NAME = 'bad_binary'


def test_unexistent_resource_name():
    with pytest.raises(RuntimeError, match='No such resource'):
        with get_binary('unexistent_name') as binary:
            subprocess.run(binary, check=True)


def test_bad_binary():
    with pytest.raises(OSError, match=r'\[Errno 8\] Exec format error'):
        with get_binary(INVALID_BINARY_NAME) as binary:
            subprocess.run(binary, check=True)


def test_success():
    with get_binary(VALID_BINARY_NAME) as binary:
        process = subprocess.run(binary, check=True, capture_output=True)
    assert process.stdout == b'Hello, world!\n'
