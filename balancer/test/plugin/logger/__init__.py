# -*- coding: utf-8 -*-
import pytest
import logging

from balancer.test.util import multiscope
from balancer.test.util.context import ManagerFixture


pytest_plugins = [
    'balancer.test.plugin.fs',
]


MAIN_LOG = 'tests.log'
LOG_FORMAT = '%(asctime)s %(levelname)-6s (%(module)s) %(message)s'


@pytest.fixture(scope='session')
def logger(session_fs_manager):
    log_file = session_fs_manager.create_file(MAIN_LOG)
    formatter = logging.Formatter(LOG_FORMAT)
    file_handler = logging.FileHandler(log_file)
    file_handler.setFormatter(formatter)
    session_logger = logging.getLogger()
    session_logger.addHandler(file_handler)
    session_logger.setLevel(logging.INFO)
    return session_logger


class LoggerContext(object):
    @property
    def logger(self):
        return self.manager.logger


MANAGERS = [ManagerFixture('logger', 'logger', multiscope.FixtureType.PYTEST)]
CONTEXTS = [LoggerContext]
