# -*- coding: utf-8 -*-

from balancer.test.util import multiscope
from balancer.test.util.context import ManagerFixture
from balancer.test.util.dnsfake import DnsFakeManager


@multiscope.fixture
def dnsfake_manager():
    return DnsFakeManager()


MANAGERS = [ManagerFixture('dnsfake', 'dnsfake_manager')]
