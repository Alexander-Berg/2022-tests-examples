import os
import sys
import tempfile
from io import StringIO

import pytest
import aiohttp
from aiohttp.test_utils import TestServer

from infra.yp_drcp.server import make_test_app


@pytest.fixture
async def drcu():
    app = await make_test_app()
    async with TestServer(app) as server:
        yield server


@pytest.fixture
async def ping_server():
    app = aiohttp.web.Application()

    async def ping(request: aiohttp.web.Request):
        return aiohttp.web.Response(text='Ok')

    app.add_routes([aiohttp.web.get('/ping', ping)])

    async with TestServer(app) as server:
        yield server


@pytest.fixture
async def stdout():
    io = StringIO()
    old_stdout, sys.stdout = sys.stdout, io
    try:
        yield io
    finally:
        sys.stdout = old_stdout


@pytest.fixture
def tempdir():
    with tempfile.TemporaryDirectory() as tmpdir:
        yield tmpdir


@pytest.fixture
async def installed_resource():
    with tempfile.TemporaryDirectory() as tmpdir, tempfile.TemporaryDirectory() as tmpdir2:
        resource_dir = os.path.join(tmpdir, 'old')
        old_resource_dir = os.path.join(tmpdir, 'older')
        os.mkdir(resource_dir)
        os.mkdir(old_resource_dir)
        resource_path = os.path.join(resource_dir, 'data')
        old_resource_path = os.path.join(old_resource_dir, 'data')
        dest_path = os.path.join(tmpdir2, 'symlink')
        with open(resource_path, 'wb'):
            pass
        with open(old_resource_path, 'wb'):
            pass
        os.symlink(resource_path, dest_path)
        state = {
            'revision': 'old',
            'symlink': dest_path,
            'path': resource_dir,
            'base_path': resource_path,
            'http_action': None,
            'exec_action': None,
            'last_check_time': None,
            'in_progress': False,
            'ready': True,
            'error': False,
            'revs': [
                {
                    'revision': 'old',
                    'path': resource_dir,
                    'base_path': resource_path,
                    'dirty': False,
                    'urls': ['raw:'],
                },
                {
                    'revision': 'older',
                    'path': old_resource_dir,
                    'base_path': old_resource_path,
                    'mark': 'test0',
                    'dirty': False,
                    'urls': ['raw:_'],
                },
            ]
        }
        info = {
            'id': 'test_resource',
            'revision': 'old',
            'urls': ['raw:'],
            'mark': 'test1',
            'storage_options': {
                'http_action': None,
                'exec_action': None,
                'box_ref': 'test_box',
                'destination': dest_path,
                'storage_dir': tmpdir,
                'cached_revisions_count': 2,
                'verification': {
                    'checksum': 'EMPTY:',
                    'check_period_ms': 1,
                }
            },
        }
        yield state, info


@pytest.fixture
async def uninstalled_resource():
    with tempfile.TemporaryDirectory() as tmpdir, tempfile.TemporaryDirectory() as tmpdir2:
        state = {}
        info = {
            'id': 'test_resource2',
            'revision': 'old',
            'urls': ['raw:'],
            'mark': 'test2',
            'storage_options': {
                'http_action': None,
                'exec_action': None,
                'box_ref': 'test_box',
                'destination': os.path.join(tmpdir2, 'resource'),
                'storage_dir': tmpdir,
                'verification': {
                    'checksum': 'SHA256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
                    'check_period_ms': 1,
                }
            },
        }
        yield state, info
