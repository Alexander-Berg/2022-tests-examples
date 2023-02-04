from urllib import parse
import typing as tp

from billing.hot.tests.clients import base
from billing.hot.tests.config import config
from billing.hot.tests.lib.state.state import PipelineState


class Client:
    def __init__(
        self,
        base_client: base.BaseClient,
        cfg: config.DiodConfig,
    ) -> None:
        self.base_client = base_client
        self.cfg = cfg
        self.tvm_id = cfg.tvm_id

    def read_keys_batch(self, st: PipelineState, namespace: str, keys: list[str]) -> tp.AsyncContextManager:
        return self.base_client.get(
            parse.urljoin(self.cfg.url, self.cfg.handlers.batch),
            st,
            params={'namespace': namespace, 'key': keys},
            headers={'X-Service-ID': self.cfg.service_id},
            dst_tvm_id=self.cfg.tvm_id,
        )

    def update_keys_batch(self, st: PipelineState, keys: list[dict]) -> tp.AsyncContextManager:
        return self.base_client.post(
            parse.urljoin(self.cfg.url, self.cfg.handlers.batch),
            st,
            body={'items': keys},
            headers={'X-Service-ID': self.cfg.service_id},
            dst_tvm_id=self.cfg.tvm_id,
        )
