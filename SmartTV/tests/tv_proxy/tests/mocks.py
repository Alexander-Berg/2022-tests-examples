import logging
from abc import ABCMeta, abstractmethod

from smarttv.alice.tv_proxy.proxy.pq_delivery import MessageWriter, LogBrokerResult

logger = logging.getLogger(__name__)


class SaasSearchClient(object):
    # noinspection PyMethodMayBeStatic
    def search(self, _):
        return set()


class Strategy(object):
    __metaclass__ = ABCMeta

    @abstractmethod
    def get_result(self):
        pass

    def add_name(self, result):
        result['strategy'] = type(self)
        return result


class AlwaysSuccessfulStrategy(Strategy):
    def get_result(self):
        return self.add_name({'written': True})


class FailOnFirstNStrategy(Strategy):
    def __init__(self, n):
        self.n = n
        self.current = 0

    def get_result(self):
        logger.debug('current=%d, n=%d', self.current, self.n)
        written = not bool(self.current < self.n)
        self.current += 1

        return self.add_name({'written': written})


class FatalErrorStrategy(Strategy):
    def get_result(self):
        return self.add_name({'written': False, 'user_error': True})


class MockWriter(MessageWriter):
    def __init__(self, strategy):
        super(MockWriter, self).__init__()
        self.strategy = strategy

    def is_ready(self):
        return True

    def _write_json(self, msg):
        return LogBrokerResult(self.strategy.get_result())


always_successful_writer = MockWriter(AlwaysSuccessfulStrategy())
fatal_error_writer = MockWriter(FatalErrorStrategy())
saas_search_client = SaasSearchClient()
