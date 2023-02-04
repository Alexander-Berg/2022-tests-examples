# -*- coding: utf-8 -*-
from balancer.test.util import multiscope
from balancer.test.util.context import ManagerFixture

from balancer.test.util.server import ConfigManager


pytest_plugins = [
    'balancer.test.plugin.port',
    'balancer.test.plugin.fs',
]


@multiscope.fixture(pytest_fixtures=['port_manager'])
def config_manager(port_manager, fs_manager):
    return ConfigManager(port_manager, fs_manager)


MANAGERS = [ManagerFixture('config', 'config_manager')]
