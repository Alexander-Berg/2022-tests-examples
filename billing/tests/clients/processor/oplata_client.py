import typing as tp

from billing.hot.tests.clients import base
from billing.hot.tests.clients.processor.client import Client as ProcessorClient
from billing.hot.tests.config import config
from billing.hot.tests.lib.state import state, contract
from billing.hot.tests.lib.templates import processor as renderer


class Client(ProcessorClient):
    def __init__(self, base_client: base.BaseClient, cfg: config.ProcessorConfig) -> None:
        super(Client, self).__init__(base_client, cfg)
        self.renderer = renderer.ProcessorOplataRenderer(self.loader)

    def cashless_payment(
        self,
        st: state.PipelineState,
        body: tp.Union[str, dict[str, tp.Any]] = 'cashless-payment.json',
        order_price: float = None,
        commission: float = None,
        item_by_card: float = None,
        item_by_promocode: float = None,
    ) -> tp.AsyncContextManager:
        request = self.renderer.render_cashless_payment_request(
            st,
            contract.OplataContract,
            body,
            order_price,
            commission,
            item_by_card,
            item_by_promocode,
        )
        return self.process(st, request)

    def cashless_refund(
        self,
        st: state.PipelineState,
        body: tp.Union[str, dict[str, tp.Any]] = 'cashless-refund.json',
        original_order_price: float = None,
        refund_price: float = None,
        item_by_card: float = None,
        item_by_promocode: float = None,
    ) -> tp.AsyncContextManager:
        request = self.renderer.render_cashless_refund_request(
            st,
            contract.OplataContract,
            body,
            original_order_price,
            refund_price,
            item_by_card,
            item_by_promocode,
        )
        return self.process(st, request)

    def payout(self,
               st: state.PipelineState,
               body: tp.Union[str, dict[str, tp.Any]] = 'payout.json',
               extended_params: dict = None,
               ) -> tp.AsyncContextManager:
        request = self.renderer.render_payout_request(
            st,
            body,
            extended_params,
        )
        return self.process(st, request)
