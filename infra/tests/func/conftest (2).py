# coding=utf-8
from __future__ import unicode_literals

from ConfigParser import SafeConfigParser
import os
import random
import string

import py
import pytest

import yatest.common


FUNC_TESTS_DIR = 'infra/nanny/instancectl/tests'
INSTANCECTL = 'infra/nanny/instancectl/bin/instancectl'
SD_BIN = 'infra/nanny/instancectl/sd_bin/sd_bin'
RESOURCES_DIR = 'resources'
PORTO_SOCKET = '/run/portod.socket'

def pytest_configure(config):
    config.addinivalue_line(
        "markers", "run_now: mark test to run now"
    )

def is_porto_enabled():
    """
    Some ugly way to determine if porto is enabled.
    """
    return os.path.exists(PORTO_SOCKET) and os.getenv('RUN_PORTO_TESTS')


@pytest.fixture
def cwd():
    str_path = yatest.common.ram_drive_path()
    path = py.path.local(str_path)
    subdir = ''.join(random.choice(string.ascii_uppercase + string.digits) for _ in range(10))
    path.mkdir(subdir)
    return path.join(subdir)


@pytest.fixture
def ctl_environment(cwd):
    port = '1543'
    env = {
        'BSCONFIG_DBTOP': cwd.dirname,
        'BSCONFIG_IDIR': cwd.strpath,
        'BSCONFIG_IHOST': 'localhost',
        'BSCONFIG_INAME': 'localhost:{}'.format(port),
        'BSCONFIG_IPORT': port,
        'BSCONFIG_ITAGS': 'portopowered a_ctype_fakectype',
        'BSCONFIG_REQUIRES_SHARDS': '',
        'BSCONFIG_SHARDDIR': cwd.dirname,
        'BSCONFIG_SHARDNAME': 'shard',
        'BSCONFIG_SHARDREPLICA': '1',
    }
    return env


@pytest.fixture
def ctl(cwd, request):
    """
    Copies built binary and resources for the test to current directory.
    """
    # Копируем ресурсы для теста
    if 'resources' in request.funcargnames:
        resources_dir = request.getfixturevalue('resources')
    else:
        resources_dir = 'resources'
    # Don't know how to get test path in arcadia so will use stupid workaround
    # test module usually looks like __tests__.arguments_evaluation.test_evaluation
    test_module = request.function.__module__.split('.')

    resources_path_segments = [FUNC_TESTS_DIR] + test_module[1:-1] + [resources_dir]
    resources_path = os.path.join(*resources_path_segments)

    resources = py.path.local(yatest.common.source_path(resources_path))

    for item in resources.listdir():
        item.copy(cwd.join(item.basename))

    # Copy built instancectl
    copied_ctl = cwd.join('loop-httpsearch')
    ctl_path = py.path.local(yatest.common.binary_path(INSTANCECTL))
    ctl_path.copy(copied_ctl)
    copied_ctl.chmod(0744)

    return copied_ctl


@pytest.fixture
def sd_bin(cwd):
    # Copy built service discovery bin
    copied_sd_bin = cwd.join('sd_bin')
    sd_bin_path = py.path.local(yatest.common.binary_path(SD_BIN))
    sd_bin_path.copy(copied_sd_bin)
    copied_sd_bin.chmod(0744)

    return copied_sd_bin


if is_porto_enabled():
    use_porto = [True, False]
else:
    use_porto = [False]


@pytest.fixture(autouse=True, params=use_porto)
def patch_loop_conf(ctl, cwd):
    """
    Патчит loop.conf под разные варианты запуска (с porto из без него)
    """
    loop_conf = cwd.join('loop.conf')
    if not loop_conf.exists():
        return
    parser = SafeConfigParser()
    parser.read(loop_conf.strpath)
    if not parser.has_section(b'defaults'):
        parser.add_section(b'defaults')
    parser.set(b'defaults', b'use_porto', str(use_porto))
    with open(loop_conf.strpath, 'w') as fd:
        parser.write(fd)


porto_required = pytest.mark.skipif(not is_porto_enabled(), reason='requires porto')
