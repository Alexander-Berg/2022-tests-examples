from yatest.common import source_path
import pytest

import subprocess

import library.python.resource

import maps.analyzer.pylibs.envkit as envkit


def test_resource_file():
    with envkit.resource_file('/config.development.json') as fname:
        with open(fname, 'rb') as f:
            assert f.read() == library.python.resource.find('/config.development.json')


def test_fs_file():
    with envkit.resource_file(source_path('maps/analyzer/pylibs/envkit/tests/data/config.development.json')) as fname:
        with open(fname, 'rb') as f:
            assert f.read() == library.python.resource.find('/config.development.json')

    with pytest.raises(ValueError):
        with envkit.resource_file(source_path('maps/analyzer/pylibs/envkit/tests/data/config.development.json'), use_fs=False) as fname:
            pass


def test_resource_tool():
    with envkit.resource_tool('/hello_world') as tool:
        p = subprocess.Popen([tool], stdout=subprocess.PIPE)
        p.wait()
        assert p.stdout.read() == b'Hello, world!\n'
