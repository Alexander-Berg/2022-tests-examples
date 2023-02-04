import typing as tp

from billing.hot.tests.clients import base
from billing.hot.tests.clients.processor.client import Client as ProcessorClient
from billing.hot.tests.config import config
from billing.hot.tests.lib.state import state, contract
from billing.hot.tests.lib.templates import processor as renderer


class Client(ProcessorClient):
    def __init__(self, base_client: base.BaseClient, cfg: config.ProcessorConfig) -> None:
        super(Client, self).__init__(base_client, cfg)
        self.renderer = renderer.ProcessorBnplRenderer(self.loader)

    def _cashless(
            self,
            st: state.PipelineState,
            body: tp.Union[str, dict[str, tp.Any]] = 'cashless-payment.json',
            transaction_amount: tp.Optional[float] = None,
            total_commission: tp.Optional[float] = None,
    ) -> tp.AsyncContextManager:
        request = self.renderer.render_cashless_request(
            sender_state=st,
            contract_type=contract.BnplContract,
            request=body,
            transaction_amount=transaction_amount,
            total_commission=total_commission,
        )
        return self.process(st, request)

    def cashless_payment(
            self,
            st: state.PipelineState,
            body: tp.Union[str, dict[str, tp.Any]] = 'cashless-payment.json',
            transaction_amount: tp.Optional[float] = None,
            total_commission: tp.Optional[float] = None,
    ) -> tp.AsyncContextManager:
        return self._cashless(st, body, transaction_amount, total_commission)

    def cashless_refund(
            self,
            st: state.PipelineState,
            body: tp.Union[str, dict[str, tp.Any]] = 'cashless-refund.json',
            transaction_amount: tp.Optional[float] = None,
            total_commission: tp.Optional[float] = None,
    ) -> tp.AsyncContextManager:
        return self._cashless(st, body, transaction_amount, total_commission)

    def payout(self,
               st: state.PipelineState,
               body: tp.Union[str, dict[str, tp.Any]] = 'payout.json',
               extended_params: tp.Optional[dict] = None,
               ) -> tp.AsyncContextManager:
        request = self.renderer.render_payout_request(st, body, extended_params)
        return self.process(st, request)
