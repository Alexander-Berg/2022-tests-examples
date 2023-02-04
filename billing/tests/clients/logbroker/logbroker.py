import typing as tp

import kikimr.public.sdk.python.persqueue.grpc_pq_streaming_api as pqlib
import tvm2
from kikimr.public.sdk.python.persqueue import auth

from billing.hot.tests.clients.logbroker import consumer
from billing.hot.tests.clients.logbroker import producer
from billing.hot.tests.config import config


class LogBrokerAPI:
    def __init__(self, tvm_client: tvm2.TVM2, cfg: config.LBConfig):
        self.api = pqlib.PQStreamingAPI(cfg.endpoint, cfg.port)
        self.config = cfg
        self.credentials_provider = self._create_credentials_provider(tvm_client)

        self.consumers: dict[str, consumer.Consumer] = {}
        for name, c in cfg.consumers.items():
            self.consumers[name] = consumer.Consumer(self, c)

        self.producers: dict[str, producer.Producer] = {}
        for name, p in cfg.producers.items():
            self.producers[name] = producer.Producer(self, p)

    def _create_credentials_provider(self, tvm_client: tvm2.TVM2) -> tp.Optional[auth.TVMCredentialsProvider]:
        if self.config.tvm_id and tvm_client:
            return auth.TVMCredentialsProvider(tvm_client, self.config.tvm_id)

        return None

    def start(self):
        self.api.start().result(timeout=5)
        for c in self.consumers.values():
            c.connect()
        for p in self.producers.values():
            p.connect()

    def stop(self):
        for c in self.consumers.values():
            c.close()
        for p in self.producers.values():
            p.close()
        self.api.stop()
