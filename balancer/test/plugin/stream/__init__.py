# -*- coding: utf-8 -*-
import pytest

from balancer.test.util import settings
from balancer.test.util import multiscope
from balancer.test.util.context import ManagerFixture

from balancer.test.util.stream import StreamManager


pytest_plugins = [
    'balancer.test.plugin.settings',
    'balancer.test.plugin.resource',
    'balancer.test.plugin.fs',
    'balancer.test.plugin.process',
]


__OPENSSL = settings.Tool(
    tool_file_name='openssl',
    yatest_option_name='openssl',
    yatest_path='contrib/libs/openssl/apps/openssl',
    system_file_name='openssl',
)


@pytest.fixture(scope='session')
def openssl_path(test_tools):
    return test_tools.get_tool(__OPENSSL)


@multiscope.fixture(pytest_fixtures=['openssl_path'])
def stream_manager(resource_manager, fs_manager, openssl_path, process_manager):
    return StreamManager(resource_manager, fs_manager, openssl_path, process_manager)


MANAGERS = [ManagerFixture('stream', 'stream_manager')]
