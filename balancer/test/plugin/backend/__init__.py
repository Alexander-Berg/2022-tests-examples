# -*- coding: utf-8 -*-
from collections import namedtuple

from balancer.test.util import multiscope
from balancer.test.util.context import ManagerFixture

from balancer.test.util.backend import BackendManager


pytest_plugins = [
    'balancer.test.plugin.resource',
    'balancer.test.plugin.logger',
    'balancer.test.plugin.server',
    'balancer.test.plugin.stream',
    'balancer.test.plugin.process',
]


FakeConfig = namedtuple('FakeConfig', ['port'])
FakeBackend = namedtuple('FakeBackend', ['server_config'])


class BackendContext(object):
    def __store_backend(self, name, backend):
        self.manager.config.add_server(name, backend.server_config)
        if not hasattr(self.state, name):
            self.state.register(name)
        setattr(self.state, name, backend)  # FIXME: do not need to store backend in state
        setattr(self, name, backend)

    def start_backend(self, backend_config, name='backend', state=None, listen_queue=64, host=None, port=None, family=None):
        """
        :param Config handler_config: backend handler config
        :param str name: backend name

        :rtype: PythonBackend
        """
        backend = self.manager.backend.start(backend_config, state=state, listen_queue=listen_queue, host=host, port=port, family=family)
        print backend
        self.__store_backend(name, backend)
        return backend

    def start_fake_backend(self, name='backend'):
        """
        :param str name: fake backend name

        :rtype: FakeBackend
        """
        config = FakeConfig(self.manager.port.get_port())
        backend = FakeBackend(config)
        self.__store_backend(name, backend)
        return backend


@multiscope.fixture(pytest_fixtures=['logger', 'openssl_path'])
def backend_manager(resource_manager, logger, config_manager, openssl_path, process_manager, stream_manager):
    return BackendManager(logger, process_manager, resource_manager, config_manager, stream_manager, openssl_path)


MANAGERS = [ManagerFixture('backend', 'backend_manager')]
CONTEXTS = [BackendContext]
