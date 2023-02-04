import typing as tp

from billing.hot.tests.clients import base
from billing.hot.tests.clients.processor.client import Client as ProcessorClient
from billing.hot.tests.config import config
from billing.hot.tests.lib.state import state

if tp.TYPE_CHECKING:
    from billing.hot.tests.lib.templates.processor import BaseProcessorRenderer


class Client(ProcessorClient):
    def __init__(
        self,
        base_client: base.BaseClient,
        cfg: config.ProcessorConfig,
        renderer_cls: tp.Type['BaseProcessorRenderer']
    ) -> None:
        super(Client, self).__init__(base_client, cfg)
        self.renderer = renderer_cls(self.loader)

    def post(self, st: state.ExtendedPipelineState) -> tp.AsyncContextManager:
        request = self.renderer.render_request(sender_state=st)

        return self.process(st, request)
