import typing as tp

from billing.hot.tests.clients import base
from billing.hot.tests.clients.processor.client import Client as ProcessorClient
from billing.hot.tests.config import config
from billing.hot.tests.lib.state import state, contract
from billing.hot.tests.lib.templates import processor as renderer


class Client(ProcessorClient):
    def __init__(self, base_client: base.BaseClient, cfg: config.ProcessorConfig):
        super(Client, self).__init__(base_client, cfg)
        self.renderer = renderer.ProcessorBnplIncomeRenderer(self.loader)

    def commission_payment(
        self,
        st: state.PipelineState,
        body: tp.Union[str, dict[str, tp.Any]] = 'commission.json',
        transaction_type='payment',
        transaction_amount=None,
    ) -> tp.AsyncContextManager:
        request = self.renderer.render_commission_request(
            st=st,
            contract_type=contract.BnplIncomeContract,
            request=body,
            transaction_type=transaction_type,
            transaction_amount=transaction_amount,
        )

        return self.process(st, request)
