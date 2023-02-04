# -*- coding: utf-8 -*-
import pytest

from balancer.test.util import multiscope
from balancer.test.util.context import ManagerFixture
from balancer.test.util.port import StaticPortManager, YatestPortManager


pytest_plugins = [
    'balancer.test.plugin.settings',
]


def pytest_addoption(parser):
    debug = parser.getgroup('debug')
    debug.addoption('-P', '--start_port', dest='start_port', default=None, type=int,
                    help='use static port manager with specified start port')


@pytest.fixture(scope='session')
def port_manager(request, settings):
    start_port = settings.get_param('start_port')
    if start_port is not None:
        return StaticPortManager(int(start_port))
    else:
        port_manager = YatestPortManager()
        request.addfinalizer(port_manager.release)
        return port_manager


MANAGERS = [ManagerFixture('port', 'port_manager', multiscope.FixtureType.PYTEST)]
