# -*- coding: utf-8 -*-
from balancer.test.util import multiscope
from balancer.test.util.context import ManagerFixture

from balancer.test.util.sync import SharedManager


pytest_plugins = [
    'balancer.test.plugin.resource',
    'balancer.test.plugin.fs',
]


@multiscope.fixture
def shared_manager(resource_manager, fs_manager):
    return SharedManager(resource_manager, fs_manager)


MANAGERS = [ManagerFixture('shared', 'shared_manager')]
