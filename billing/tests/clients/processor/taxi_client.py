import typing as tp

from billing.hot.tests.clients import base
from billing.hot.tests.clients.processor.client import Client as ProcessorClient
from billing.hot.tests.config import config
from billing.hot.tests.lib.state import state, contract
from billing.hot.tests.lib.templates import processor as renderer


class Client(ProcessorClient):
    def __init__(self, base_client: base.BaseClient, cfg: config.ProcessorConfig) -> None:
        super(Client, self).__init__(base_client, cfg)
        self.renderer = renderer.ProcessorTaxiRenderer(self.loader)

    def cashless(
        self,
        st: state.PipelineState,
        body: tp.Union[str, dict[str, tp.Any]] = 'cashless.json',
        extended_params: dict = None,
    ) -> tp.AsyncContextManager:
        request = self.renderer.render_stream_request(
            st,
            contract.ServiceContract,
            body,
            extended_params,
        )
        return self.process(st, request)

    def cashless_refunds(
        self,
        st: state.PipelineState,
        body: tp.Union[str, dict[str, tp.Any]] = 'cashless.json',
        extended_params: dict = None,
    ) -> tp.AsyncContextManager:
        extended_params = extended_params or {}
        extended_params['transaction_type'] = 'refund'
        request = self.renderer.render_stream_request(
            st,
            contract.ServiceContract,
            body,
            extended_params,
        )
        return self.process(st, request)

    def subvention(
        self,
        st: state.PipelineState,
        body: tp.Union[str, dict[str, tp.Any]] = 'subvention.json',
        extended_params: dict = None,
    ) -> tp.AsyncContextManager:
        request = self.renderer.render_stream_request(
            st,
            contract.SubventionContract,
            body,
            extended_params,
        )
        return self.process(st, request)

    def subvention_refunds(
        self,
        st: state.PipelineState,
        body: tp.Union[str, dict[str, tp.Any]] = 'subvention.json',
        extended_params: dict = None,
    ) -> tp.AsyncContextManager:
        extended_params = extended_params or {}
        extended_params['transaction_type'] = 'refund'
        request = self.renderer.render_stream_request(
            st,
            contract.SubventionContract,
            body,
            extended_params,
        )
        return self.process(st, request)

    def revenue(
        self,
        st: state.PipelineState,
        body: tp.Union[str, dict[str, tp.Any]] = 'revenue.json',
        extended_params: dict = None,
    ) -> tp.AsyncContextManager:
        request = self.renderer.render_revenue_request(
            st,
            body,
            extended_params,
        )
        return self.process(st, request)

    commissions = revenue

    def revenue_refunds(
        self,
        st: state.PipelineState,
        body: tp.Union[str, dict[str, tp.Any]] = 'revenue.json',
        extended_params: dict = None,
    ) -> tp.AsyncContextManager:
        extended_params = extended_params or {}
        extended_params['transaction_type'] = 'refund'
        request = self.renderer.render_revenue_request(
            st,
            body,
            extended_params,
        )
        return self.process(st, request)

    commissions_refunds = revenue_refunds

    def promocodes(
        self,
        st: state.PipelineState,
        body: tp.Union[str, dict[str, tp.Any]] = 'revenue.json',
        extended_params: dict = None,
    ) -> tp.AsyncContextManager:
        extended_params = extended_params or {}
        extended_params['product'] = 'coupon'
        return self.revenue(st, body, extended_params)

    def promocodes_refunds(
        self,
        st: state.PipelineState,
        body: tp.Union[str, dict[str, tp.Any]] = 'revenue.json',
        extended_params: dict = None,
    ) -> tp.AsyncContextManager:
        extended_params = extended_params or {}
        extended_params.update({
            'product': 'subvention',
            'aggregation_sign': -1,
        })
        return self.revenue_refunds(st, body, extended_params)

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

    def logistics(
        self,
        st: state.PipelineState,
        body: tp.Union[str, dict[str, tp.Any]] = 'subvention.json',
        extended_params: dict = None,
    ) -> tp.AsyncContextManager:
        extended_params = extended_params or {}
        extended_params.update({
            'contract_id': st.get_contract(contract.LogisticsContract).id,
            'service_id': 719,
            'product': 'delivery_park_b2b_logistics_payment',
        })
        return self.subvention(st, body, extended_params)

    def logistics_refunds(
        self,
        st: state.PipelineState,
        body: tp.Union[str, dict[str, tp.Any]] = 'subvention.json',
        extended_params: dict = None,
    ) -> tp.AsyncContextManager:
        extended_params = extended_params or {}
        extended_params.update({
            'product': 'delivery_park_b2b_logistics_payment',
            'contract_id': st.get_contract(contract.LogisticsContract).id,
            'service_id': 719,
            'transaction_type': 'refund',
            'aggregation_sign': -1,
        })
        return self.subvention_refunds(st, body, extended_params)

    def corporate(
        self,
        st: state.PipelineState,
        body: tp.Union[str, dict[str, tp.Any]] = 'subvention.json',
        extended_params: dict = None,
    ) -> tp.AsyncContextManager:
        extended_params = extended_params or {}
        extended_params.update({
            'contract_id': st.get_contract(contract.CorporateContract).id,
            'service_id': 651,
            'product': 'park_b2b_trip_payment',
        })
        return self.subvention(st, body, extended_params)

    def corporate_refunds(
        self,
        st: state.PipelineState,
        body: tp.Union[str, dict[str, tp.Any]] = 'subvention.json',
        extended_params: dict = None,
    ) -> tp.AsyncContextManager:
        extended_params = extended_params or {}
        extended_params.update({
            'contract_id': st.get_contract(contract.CorporateContract).id,
            'service_id': 651,
            'product': 'park_b2b_trip_payment',
            'transaction_type': 'refund',
            'aggregation_sign': -1,
        })
        return self.subvention_refunds(st, body, extended_params)

    def fuel_hold(
        self,
        st: state.PipelineState,
        body: tp.Union[str, dict[str, tp.Any]] = 'fuel_hold.json',
        extended_params: dict = None,
    ) -> tp.AsyncContextManager:
        request = self.renderer.render_fuel_hold_request(
            st,
            body,
            extended_params,
        )
        return self.process(st, request)

    def fuel_hold_refunds(
        self,
        st: state.PipelineState,
        body: tp.Union[str, dict[str, tp.Any]] = 'fuel_hold.json',
        extended_params: dict = None,
    ) -> tp.AsyncContextManager:
        extended_params = extended_params or {}
        extended_params.update({
            'payment_type': 'deposit',
            'paysys_type_cc': 'fuel_hold',
            'transaction_type': 'refund',
        })
        request = self.renderer.render_fuel_hold_request(
            st,
            body,
            extended_params,
        )
        return self.process(st, request)

    def transfer_init(
        self,
        sender_state: state.PipelineState,
        receiver_state: state.PipelineState,
        body: tp.Union[str, dict[str, tp.Any]] = 'transfer-init.json',
        extended_params: dict = None,
    ) -> tp.AsyncContextManager:
        request = self.renderer.render_transfer_init_request(
            sender_state,
            receiver_state,
            body,
            extended_params,
        )
        return self.process(sender_state, request)

    def transfer_cancel(
        self,
        sender_state: state.PipelineState,
        transaction_id: str,
        body: tp.Union[str, dict[str, tp.Any]] = 'transfer-cancel.json',
        extended_params: dict = None,
    ) -> tp.AsyncContextManager:
        request = self.renderer.render_transfer_cancel_request(
            sender_state,
            transaction_id,
            body,
            extended_params,
        )
        return self.process(sender_state, request)
