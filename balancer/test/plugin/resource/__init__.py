# -*- coding: utf-8 -*-
from balancer.test.util import multiscope
from balancer.test.util.context import ManagerFixture

from balancer.test.util.resource import AbstractResourceManager


pytest_plugins = [
]


class PytestResourceManager(AbstractResourceManager):
    def __init__(self, request):
        super(PytestResourceManager, self).__init__()
        request.addfinalizer(self._finish_all)


@multiscope.fixture(pytest_fixtures=['request'])
def resource_manager(request):
    return PytestResourceManager(request)


MANAGERS = [ManagerFixture('resource', 'resource_manager')]
