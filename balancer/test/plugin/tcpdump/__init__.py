# -*- coding: utf-8 -*-
import re
import sys
import pytest

from balancer.test.util import multiscope
from balancer.test.util.context import ManagerFixture

from balancer.test.util.tcpdump import TcpdumpManager, tcpdump_prefix
from balancer.test.util.process import call


pytest_plugins = [
    'balancer.test.plugin.settings',
    'balancer.test.plugin.fs',
    'balancer.test.plugin.resource',
    'balancer.test.plugin.process',
]


__DEV = re.compile(r'\d+\.([^\s]+).*')


@pytest.fixture(scope='session')
def __skip_tcpdump(settings, logger):
    result = None
    try:
        result = call(tcpdump_prefix(settings) + ['-D'], logger)
    except OSError:
        return True
    logger.info('tcpdump stdout: {}'.format(result.stdout))
    logger.info('tcpdump stderr: {}'.format(result.stderr))
    devices = list()
    for line in result.stdout.splitlines():
        devices.append(__DEV.match(line).groups()[0])
    for dev in devices:
        # ubuntu 16.04 fix
        if dev.startswith('lo '):
            return False
    iface = "lo"
    # OS X could run tcpdump on lo0 without sudo
    if sys.platform == "darwin":
        iface = "lo0"

    return iface not in devices


@multiscope.fixture(pytest_fixtures=['logger', 'settings', '__skip_tcpdump'])
def tcpdump_manager(resource_manager, logger, fs_manager, settings, __skip_tcpdump):
    return TcpdumpManager(resource_manager, logger, fs_manager, settings, skip=__skip_tcpdump)


MANAGERS = [ManagerFixture('tcpdump', 'tcpdump_manager')]
