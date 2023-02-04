from concurrent import futures
import json
import logging
import typing as tp

from billing.hot.tests.config import config
from billing.hot.tests.clients.logbroker import logbroker  # noqa: F401

from kikimr.public.sdk.python.persqueue import grpc_pq_streaming_api as pqlib
from kikimr.public.sdk.python.persqueue import errors as pqerrors

logger = logging.getLogger(__name__)


class Producer:
    def __init__(self, lb: 'logbroker.LogBrokerAPI', cfg: config.LBProducerConfig) -> None:
        self.config = cfg
        self.lb = lb
        self.producer = None
        self.max_seq_no: tp.Optional[int] = None

    def connect(self) -> None:
        configurator = pqlib.ProducerConfigurator(self.config.topic, self.config.source_id)

        self.producer = self.lb.api.create_producer(configurator, credentials_provider=self.lb.credentials_provider)

        start_result = self.producer.start().result(timeout=5)
        if isinstance(start_result, pqerrors.SessionFailureResult):
            raise RuntimeError(f'error occurred on start of producer: {start_result}')

        if not start_result.HasField("init"):
            raise RuntimeError(f'bad producer start result from server: {start_result}')
        self.max_seq_no = start_result.init.max_seq_no

    def write(self, message: dict) -> None:
        self.max_seq_no += 1
        byte_message = bytes(json.dumps(message), encoding='ascii')
        try:
            result = self.producer.write(self.max_seq_no, byte_message).result(timeout=2)
        except futures.TimeoutError as e:
            raise RuntimeError('timed out to write message') from e
        logger.info("written message %s", message)

        if not result.HasField('ack'):
            raise RuntimeError(f'failed to write message: {result}')

    def close(self) -> None:
        self.producer.stop()
