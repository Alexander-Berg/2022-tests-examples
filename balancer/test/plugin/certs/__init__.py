# -*- coding: utf-8 -*-
import os

from balancer.test.util import multiscope

from balancer.test.util import settings
from balancer.test.util.certs import Certs
from balancer.test.util.context import ManagerFixture


pytest_plugins = [
    'balancer.test.plugin.fs',
]


def __setup_certs(fs_manager):
    src = settings.get_data(
        py_path=os.path.abspath(os.path.join(os.path.dirname(__file__), 'data')),
        ya_path='balancer/test/plugin/certs/data',
    )
    dst = fs_manager.get_unique_name('certs')
    fs_manager.copy(src, dst)
    return Certs(dst)


@multiscope.fixture
def certs(fs_manager):
    return __setup_certs(fs_manager)


class CertsContext(object):
    @property
    def certs(self):
        """
        Directory with SSL certs

        :rtype: FileSystemManager
        """
        return self.manager.certs


MANAGERS = [ManagerFixture('certs', 'certs')]
CONTEXTS = [CertsContext]
