# -*- coding: utf-8 -*-


from tests.base import BalanceTest


from butils import logger
log = logger.get_logger()

class StrLog:
    def __str__(self):
        log.error('StrLog.__str__')
        return 'StrLog'

class TestLoggingLock(BalanceTest):
    def test_lock(self):
        log.error('Test %s', StrLog())
