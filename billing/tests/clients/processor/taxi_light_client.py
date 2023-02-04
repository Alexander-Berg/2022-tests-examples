import typing as tp

from billing.hot.tests.clients import base
from billing.hot.tests.clients.processor.client import Client as ProcessorClient
from billing.hot.tests.config import config
from billing.hot.tests.lib.state import state
from billing.hot.tests.lib.templates import processor as renderer


class Client(ProcessorClient):
    def __init__(self, base_client: base.BaseClient, cfg: config.ProcessorConfig):
        super(Client, self).__init__(base_client, cfg)
        self.renderer = renderer.ProcessorTaxiLightRenderer(self.loader)

    def payout(
        self,
        st: state.PipelineState,
        body: tp.Union[str, dict[str, tp.Any]] = 'payout.json',
        operation_type='INSERT_NETTING',
        amount=None,
    ) -> tp.AsyncContextManager:
        request = self.renderer.render_payout_request(
            st=st,
            request=body,
            operation_type=operation_type,
            amount=amount,
        )

        return self.process(st, request)
