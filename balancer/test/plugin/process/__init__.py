# -*- coding: utf-8 -*-
from balancer.test.util import multiscope
from balancer.test.util.context import ManagerFixture

from balancer.test.util.process import ProcessManager


pytest_plugins = [
    'balancer.test.plugin.resource',
    'balancer.test.plugin.fs',
    'balancer.test.plugin.logger',
]


@multiscope.fixture(pytest_fixtures=['logger'])
def process_manager(resource_manager, logger, fs_manager):
    return ProcessManager(resource_manager, logger, fs_manager)


class ProcessContext(object):
    def call(self, cmd, text=None):
        return self.manager.process.call(cmd, text)


MANAGERS = [ManagerFixture('process', 'process_manager')]
CONTEXTS = [ProcessContext]
