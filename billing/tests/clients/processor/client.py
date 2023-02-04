import typing as tp
from urllib import parse

from billing.hot.tests.clients import base
from billing.hot.tests.config import config
from billing.hot.tests.lib.state import state
from billing.hot.tests.lib.templates import processor as renderer


class Client:
    def __init__(self, base_client: base.BaseClient, cfg: config.ProcessorConfig) -> None:
        self.process_url = parse.urljoin(cfg.url, cfg.handlers.process)
        self.loader = renderer.ProcessorLoader(cfg.template_dir)
        self.base_client = base_client
        self.tvm_id = cfg.tvm_id

    def process(self, st: state.PipelineState, request: dict) -> tp.AsyncContextManager:
        return self.base_client.post(
            self.process_url,
            st,
            dst_tvm_id=self.tvm_id,
            body=request,
        )

    def do(self, event_type: str, *args, **kwargs):
        return getattr(self, event_type)(*args, **kwargs)
