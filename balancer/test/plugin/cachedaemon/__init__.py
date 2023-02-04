# -*- coding: utf-8 -*-
import pytest
import os

from balancer.test.util import settings
from balancer.test.util import multiscope
from balancer.test.util.context import ManagerFixture

from balancer.test.util.cachedaemon import CacheDaemonManager


# FIXME
pytest_plugins = [
    'balancer.test.plugin.resource',
    'balancer.test.plugin.logger',
    'balancer.test.plugin.server',
    'balancer.test.plugin.stream',
    'balancer.test.plugin.process',
    'balancer.test.plugin.fs',
    'balancer.test.plugin.settings',
]


__CACHEDAEMON = settings.Tool(
    pytest_option_name='cachedaemon',
    tool_file_name='cached',
    yatest_option_name='cachedaemon',
    yatest_path='web/daemons/cached/cached',
)


def pytest_addoption(parser):
    parser.addoption('--cachedaemon', dest=__CACHEDAEMON.pytest_option_name,
                     default=None, help='path to cachedaemon executable')


@pytest.fixture(scope='session')
def cachedaemon_path(test_tools):
    path = test_tools.get_tool(__CACHEDAEMON)

    if not os.access(path, os.X_OK):
        raise OSError('[Errno 13] Cachedaemon executable is not executable (check file permissions)')

    return path


@multiscope.fixture(pytest_fixtures=['cachedaemon_path', 'logger', 'request'])
def cachedaemon_manager(cachedaemon_path, resource_manager, logger, fs_manager, connection_manager, config_manager, request):
    return CacheDaemonManager(
        resource_manager=resource_manager,
        logger=logger,
        fs_manager=fs_manager,
        http_connection_manager=connection_manager.http,
        config_manager=config_manager,
        cachedaemon_path=cachedaemon_path,
    )


MANAGERS = [ManagerFixture('cachedaemon', 'cachedaemon_manager')]
