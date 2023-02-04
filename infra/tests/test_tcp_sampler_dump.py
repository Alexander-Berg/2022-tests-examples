import unittest.mock as mock
import time


class MockOsStat(object):
    def __init__(self, st_mtime, st_size):
        self.st_mtime = st_mtime
        self.st_size = st_size


def ok_mock(*args, **kwargs):
    return MockOsStat(time.time() - 10, 100)


def crit_mock(*args, **kwargs):
    return MockOsStat(time.time() - 25 * 60 * 60, 100)


def crit_mock_size(*args, **kwargs):
    return MockOsStat(time.time() - 10, 0)


def test_ok(manifest):
    ok_data = {"events": [{"description": "OK", "service": "tcp_sampler_dump", "status": "OK"}]}
    with mock.patch('os.stat', side_effect=ok_mock):
        result = manifest.execute('tcp_sampler_dump')
    assert result == ok_data


def test_crit(manifest):
    crit_data = {"events": [{"description": "dump timestamp is too old", "service": "tcp_sampler_dump", "status": "CRIT"}]}
    with mock.patch('os.stat', side_effect=crit_mock):
        result = manifest.execute('tcp_sampler_dump')
    assert result == crit_data


def test_crit_size(manifest):
    crit_data = {"events": [{"description": "dump size is too small", "service": "tcp_sampler_dump", "status": "CRIT"}]}
    with mock.patch('os.stat', side_effect=crit_mock_size):
        result = manifest.execute('tcp_sampler_dump')
    assert result == crit_data
