import collections
import logging
import threading
from concurrent import futures

from kikimr.public.sdk.python.persqueue import errors as pqerrors
from kikimr.public.sdk.python.persqueue import grpc_pq_streaming_api as pqlib

from billing.hot.tests.clients.logbroker import logbroker  # noqa: F401
from billing.hot.tests.config import config
from billing.hot.tests.lib.date import timestamp

logger = logging.getLogger(__name__)


class Consumer:
    def __init__(self, lb: 'logbroker.LogBrokerAPI', cfg: config.LBConsumerConfig) -> None:
        self.config = cfg
        self.lb = lb
        self.consumer = None
        self.now_ts_ms = timestamp.now_dt_ms()

        self._lock = threading.RLock()
        self._futures = collections.deque()

    def connect(self) -> None:
        self.now_ts_ms = timestamp.now_dt_ms()
        configurator = pqlib.ConsumerConfigurator(
            self.config.topic, self.config.reader,
            max_time_lag_ms=300_000,
            read_timestamp_ms=self.now_ts_ms,
            commits_disabled=True,
        )

        self.consumer = self.lb.api.create_consumer(configurator, credentials_provider=self.lb.credentials_provider)

        start_result = self.consumer.start().result(timeout=5)
        if isinstance(start_result, pqerrors.SessionFailureResult):
            raise RuntimeError(f'error occurred on start of consumer: {start_result}')

        if not start_result.HasField("init"):
            raise RuntimeError(f'bad consumer start result from server: {start_result}')

    def _get_next_read_future(self):
        with self._lock:
            if self._futures:
                return self._futures.popleft()
            return self.consumer.next_event()

    def _set_last_read_future(self, f):
        with self._lock:
            return self._futures.append(f)

    def read(self) -> (list, bool):
        """returns messages and bool if found any message before filtering"""
        f = self._get_next_read_future()
        try:
            result = f.result(timeout=3)
        except futures.TimeoutError:
            self._set_last_read_future(f)
            return [], False

        messages = []
        assert result.type == pqlib.ConsumerMessageType.MSG_DATA
        for batch in result.message.data.message_batch:
            for message in batch.message:
                if message.meta.write_time_ms < self.now_ts_ms:
                    continue
                messages.append(message)
                logger.info("read message %s", message.data)
        return messages, True

    def close(self) -> None:
        self.consumer.stop()
