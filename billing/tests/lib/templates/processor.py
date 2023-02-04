import typing as tp
from abc import ABC, abstractmethod
from os import path

from billing.hot.tests.lib.date import formatted
from billing.hot.tests.lib.rand import rand
from billing.hot.tests.lib.templates import loader
from billing.hot.tests.lib.util.util import deep_update
from billing.hot.tests.lib.state import contract as contr

if tp.TYPE_CHECKING:
    from billing.hot.tests.lib.state import state


class ProcessorLoader:
    def __init__(self, template_dir: str) -> None:
        self.loader = loader.TemplateLoader(template_dir)

    def load_actotron_act_rows_request(self, name: str) -> loader.RenderedTemplate:
        return self.loader.load(path.join('actotron_act_rows', name))

    def load_taxi_request(self, name: str) -> loader.RenderedTemplate:
        return self.loader.load(path.join('taxi', name))

    def load_oplata_request(self, name: str) -> loader.RenderedTemplate:
        return self.loader.load(path.join('oplata', name))

    def load_bnpl_request(self, name: str) -> loader.RenderedTemplate:
        return self.loader.load(path.join('bnpl', name))

    def load_bnpl_income_request(self, name: str) -> loader.RenderedTemplate:
        return self.loader.load(path.join('bnpl_income', name))

    def load_taxi_light_request(self, name: str) -> loader.RenderedTemplate:
        return self.loader.load(path.join('taxi_light', name))

    def load_trust_request(self, name: str) -> loader.RenderedTemplate:
        return self.loader.load(path.join('trust', name))

    def load_request(self, folder: str, name: str) -> loader.RenderedTemplate:
        return self.loader.load(path.join(folder, name))


class ProcessorActotronActRowsRendererMixin:
    @staticmethod
    def render_acts_request(
        st: 'state.PipelineState',
        contract_type: type,
        request: dict,
        namespace: str,
        act_sum_positive: float,
        act_sum_negative: float,
        act_sum_wo_vat_positive: float,
        act_sum_wo_vat_negative: float,
    ) -> loader.RenderedTemplate:
        request['namespace'] = namespace

        act_row_id = rand.hex(16)
        st.add_transactions([act_row_id])

        event: dict[str, tp.Any] = request['event']['obj']
        event.update({
            'act_row_id': act_row_id,
            'client_id': st.client_id,
            'act_sum': act_sum_positive + act_sum_negative,
            'contract_id': st.get_contract(contract_type).id,
            'act_start_dt': formatted.shifted_date_iso(hours=-4),
            'act_finish_dt': formatted.shifted_date_iso(hours=-3),
        })
        sum_components: dict[str, tp.Any] = event['act_sum_components']
        sum_components.update({
            'act_sum_positive': act_sum_positive,
            'act_sum_negative': act_sum_negative,
            'act_sum_wo_vat_positive': act_sum_wo_vat_positive,
            'act_sum_wo_vat_negative': act_sum_wo_vat_negative,
        })

        return request


class BaseProcessorRenderer(ABC):
    def __init__(self, lder: ProcessorLoader) -> None:
        self.loader = lder

    @abstractmethod
    def render_request(self, sender_state: 'state.ExtendedPipelineState') -> dict:
        raise NotImplementedError

    @staticmethod
    def _render_payout_request(
        st: 'state.PipelineState',
        request: loader.RenderedTemplate,
        extended_params: dict = None,
    ) -> loader.RenderedTemplate:
        event = request['event']
        t_id = rand.int64()
        st.add_transactions([t_id])
        event.update({
            "client_id": st.client_id,
            "event_time": formatted.date_iso(),
            "transaction_id": str(t_id),
        })
        if extended_params:
            event.update(extended_params)
        return request


class ProcessorBnplRenderer(BaseProcessorRenderer, ProcessorActotronActRowsRendererMixin):
    def render_request(self, sender_state: 'state.ExtendedPipelineState') -> dict:
        event = sender_state.event_params or {}
        if sender_state.endpoint == 'cashless':
            return self.render_cashless_request(
                sender_state=sender_state,
                contract_type=contr.BnplContract,
                request=sender_state.template,
                transaction_amount=event.get('transaction_amount'),
                total_commission=event.get('total_commission'),
            )
        if sender_state.endpoint == 'payout':
            return self.render_payout_request(
                st=sender_state,
                request=self.loader.load_bnpl_request(sender_state.template),
                extended_params=event
            )
        if sender_state.endpoint == 'acts':
            return self.render_acts_request(
                st=sender_state,
                contract_type=contr.BnplContract,
                request=self.loader.load_actotron_act_rows_request(name=sender_state.template),
                namespace=sender_state.namespace,
                act_sum_positive=event.get('act_sum_positive', 0),
                act_sum_negative=event.get('act_sum_negative', 0),
                act_sum_wo_vat_positive=event.get('act_sum_wo_vat_positive', 0),
                act_sum_wo_vat_negative=event.get('act_sum_wo_vat_negative', 0)
            )

        raise LookupError(f'unknown endpoint for ProcessorBnplRenderer: {sender_state.endpoint}')

    def render_payout_request(
        self,
        st: 'state.PipelineState',
        request: loader.RenderedTemplateOrTemplatePath,
        extended_params: tp.Optional[dict] = None,
    ) -> loader.RenderedTemplate:
        if isinstance(request, str):
            request = self.loader.load_bnpl_request(request)
        return self._render_payout_request(st, request, extended_params)

    def render_cashless_request(
        self,
        sender_state: 'state.PipelineState',
        contract_type: type,
        request: loader.RenderedTemplateOrTemplatePath,
        transaction_amount: tp.Optional[float] = None,
        total_commission: tp.Optional[float] = None,
    ):
        if isinstance(request, str):
            request = self.loader.load_bnpl_request(request)
        event: dict[str, tp.Any] = request['event']

        self._update_event(sender_state, event, contract_type)

        if transaction_amount:
            event['transaction_amount'] = transaction_amount

        if total_commission:
            event['aquiring_commission'] = total_commission // 2
            event['service_commission'] = total_commission - event['aquiring_commission']

        return request

    @staticmethod
    def _update_event(
        sender_state: 'state.PipelineState',
        event: dict[str, tp.Any],
        contract_type: type,
    ) -> None:
        event.update({
            'billing_client_id': sender_state.client_id,
            'billing_contract_id': sender_state.get_contract(contract_type).id,
            'order_creation_dt': formatted.shifted_date_iso(hours=-4),
            'transaction_dt': formatted.shifted_date_iso(hours=-3),
            'transaction_id': f'transaction_id-{sender_state.order_uid}-{event["transaction_type"]}',
        })


class ProcessorBnplIncomeRenderer(BaseProcessorRenderer):
    def render_request(self, sender_state: 'state.ExtendedPipelineState') -> dict:
        event = sender_state.event_params or {}
        if sender_state.endpoint == 'commission':
            return self.render_commission_request(
                st=sender_state,
                contract_type=contr.BnplIncomeContract,
                request=sender_state.template,
                transaction_type=event.get('transaction_type', 'payment'),
                transaction_amount=event.get('transaction_amount')
            )
        raise LookupError(f'unknown endpoint for ProcessorBnplIncomeRenderer: {sender_state.endpoint}')

    def render_commission_request(
        self,
        st: 'state.PipelineState',
        contract_type: type,
        request: loader.RenderedTemplateOrTemplatePath = 'commission.json',
        transaction_type: str = 'payment',
        transaction_amount: tp.Optional[float] = None,
    ) -> loader.RenderedTemplate:
        if isinstance(request, str):
            request = self.loader.load_bnpl_income_request(request)
        event: dict[str, tp.Any] = request['event']

        self._update_event(st, event, contract_type)

        event['transaction_type'] = transaction_type

        if transaction_amount:
            event['transaction_amount'] = transaction_amount

        return request

    @staticmethod
    def _update_event(
        sender_state: 'state.PipelineState',
        event: dict[str, tp.Any],
        contract_type: type,
    ) -> None:
        event.update({
            'billing_client_id': sender_state.client_id,
            'billing_contract_id': sender_state.get_contract(contract_type).id,
            'transaction_dt': formatted.shifted_date_iso(hours=-3),
            'transaction_id': f'transaction_id-{sender_state.order_uid}-{event["transaction_type"]}',
        })


class ProcessorTaxiLightRenderer(BaseProcessorRenderer):
    def render_request(self, sender_state: 'state.ExtendedPipelineState') -> dict:
        event = sender_state.event_params or {}
        if sender_state.endpoint == 'payout':
            return self.render_payout_request(
                st=sender_state,
                request=sender_state.template,
                operation_type=event.get('operation_type', 'INSERT_NETTING'),
                amount=event.get('amount')
            )
        raise LookupError(f'unknown endpoint for ProcessorTaxiLightRenderer: {sender_state.endpoint}')

    def render_payout_request(
        self,
        st: 'state.PipelineState',
        request: loader.RenderedTemplateOrTemplatePath,
        operation_type: str = 'INSERT_NETTING',
        amount: tp.Optional[float] = None,
    ) -> loader.RenderedTemplate:
        if isinstance(request, str):
            request = self.loader.load_taxi_light_request(request)
        event = request['event']
        t_id = rand.int64()
        st.add_transactions([t_id])
        event.update({
            "id": t_id,
            "transaction_dt": formatted.shifted_date_iso(hours=-2),
            "client_id": str(st.client_id),
            "contract_id": st.get_contract(contr.ServiceContract).id,
            "invoice_external_id": st.external_id,
            "operation_type": operation_type,
        })
        if amount is not None:
            event["amount"] = amount
        return request


class ProcessorTrustRenderer(BaseProcessorRenderer):
    def render_request(self, sender_state: 'state.ExtendedPipelineState') -> dict:
        request = self.loader.load_trust_request(sender_state.template)

        request.update({
            'namespace': sender_state.namespace,
            'endpoint': sender_state.endpoint
        })

        self._update_event(request['event'], sender_state)
        return request

    def _update_event(self, event: dict[str, tp.Any], st: 'state.ExtendedPipelineState') -> None:
        dt = formatted.shifted_date_iso(hours=-4)

        event.update({
            'dt': dt,
            'postauth_dt': dt,
            'payment_method_id': st.payment_method_id or event['payment_method_id'],
            'currency': st.event_currency,
            'service_id': st.service_id,
        })

        products_params = st.products_params or [{}]
        for params in products_params:
            params['partner_id'] = st.client_id
            params['service_id'] = st.service_id

        event['products'] = self.generate_products(products_params)

        for row in st.rows:
            row['dt'] = dt
        for refund in st.refunds:
            refund['dt'] = refund['payment_dt'] = dt

        event['rows'] = self.generate_rows(st.rows)
        if st.refunds:
            event['refunds'] = self.generate_refunds(st.refunds)

    @classmethod
    def generate_rows(cls, rows_params: list[dict]) -> list[dict]:
        return [
            deep_update(cls.generate_row(row_id=params.get('row_id')), params)
            for params in rows_params
        ]

    @classmethod
    def generate_refunds(cls, refunds_params: list[dict]) -> list[dict]:
        return [
            deep_update(cls.generate_refund(rows_params=params.get('rows', [])), params)
            for params in refunds_params
        ]

    @classmethod
    def generate_row(cls, row_id: tp.Optional[int]) -> dict:
        return {
            "id": row_id or rand.int64(),
            "fiscal_nds": "nds_none",
            "fiscal_inn": "",
            "fiscal_title": "test_fiscal_title",
            "price": 1200.15,
            "order": {
                "contract_id": None,
                "commission_category": None,
                "update_dt": "2021-10-05T14:55:57+00:00",
                "service_product_id": 786020877,
                "price": None,
                "service_order_id_number": 168366440,
                "start_dt_utc": None,
                "service_product_external_id": "1626979513690-DTQA22CMFK-5877",
                "clid": None,
                "service_order_id": "168366439",
                "text": None,
                "start_dt_offset": None,
                "service_id": 638,
                "dt": "2021-10-05T14:55:57+00:00",
                "passport_id": 4011632014,
                "region_id": None
            },
            "amount": 1200.15,
            "fiscal_item_code": "",
            "fiscal_agent_type": "",
            "cancel_dt": None,
            "quantity": 0.0
        }

    @classmethod
    def generate_refund(cls, rows_params: list[dict]) -> dict:
        return {
            "resp_desc": None,
            "terminal_id": None,
            "trust_refund_id": "615c678a5b095cb6e55af28e",
            "currency": "RUB",
            "rows": cls.generate_rows(rows_params=rows_params),
            "is_reversal": 0,
            "payment_dt": "2021-10-05T14:56:12+00:00",
            "cancel_dt": None,
            "resp_dt": None,
            "description": "cancel payment",
            "dt": "2021-10-05T14:56:10+00:00",
            "refund_to": "paysys",
            "type": "REFUND",
            "amount": 1200.15,
            "resp_code": "success",
            "service_id": 638,
            "passport_id": 4011632014
        }

    @classmethod
    def generate_products(cls, products_params: list[dict]) -> list[dict]:
        return [
            deep_update(cls.generate_product(product_id=params.get('row_id')), params)
            for params in products_params
        ]

    @classmethod
    def generate_product(cls, product_id: tp.Optional[int]) -> dict:
        return {
            "id": product_id or 786020877,
            "partner_id": 1351796565,
            "product_type": "app",
            "name": "TestTestAppProduct",
            "package_name": None,
            "fiscal_title": "test_fiscal_title",
            "single_purchase": None,
            "subs_period": None,
            "parent_id": None,
            "active_until_dt": None,
            "service_fee": None,
            "fiscal_nds": "nds_20_120",
            "hidden": None,
            "external_id": "638076723262902886",
            "subs_trial_period": None,
            "inapp_name": None,
            "service_id": 638
        }


class ProcessorOplataRenderer(BaseProcessorRenderer):
    def render_request(self, sender_state: 'state.ExtendedPipelineState') -> dict:
        event = sender_state.event_params or {}

        if sender_state.endpoint == 'cashless':
            event_type = event.get('type')

            if event_type == 'payment':
                return self.render_cashless_payment_request(
                    sender_state=sender_state,
                    contract_type=contr.OplataContract,
                    request=sender_state.template,
                    order_price=event.get('order_price'),
                    commission=event.get('commission'),
                    item_by_card=event.get('item_by_card'),
                    item_by_promocode=event.get('item_by_promocode'),
                )

            if event_type == 'refund':
                return self.render_cashless_refund_request(
                    sender_state=sender_state,
                    contract_type=contr.OplataContract,
                    request=sender_state.template,
                    original_order_price=event.get('original_order_price'),
                    refund_price=event.get('refund_price'),
                    item_by_card=event.get('item_by_card'),
                    item_by_promocode=event.get('item_by_promocode'),
                )

            raise LookupError(f'unknown "cashless" endpoint event type in ProcessorOplataRenderer: {event_type}')

        if sender_state.endpoint == 'payout':
            return self.render_payout_request(
                st=sender_state,
                request=sender_state.template,
                extended_params=event,
            )

        raise LookupError(f'unknown endpoint for ProcessorOplataRenderer: {sender_state.endpoint}')

    def render_payout_request(
        self,
        st: 'state.PipelineState',
        request: loader.RenderedTemplateOrTemplatePath,
        extended_params: dict = None,
    ) -> loader.RenderedTemplate:
        if isinstance(request, str):
            request = self.loader.load_oplata_request(request)
        return self._render_payout_request(st, request, extended_params)

    def render_cashless_payment_request(
        self,
        sender_state: 'state.PipelineState',
        contract_type: type,
        request: loader.RenderedTemplateOrTemplatePath,
        order_price: float = None,
        commission: float = None,
        item_by_card: float = None,
        item_by_promocode: float = None,
    ) -> loader.RenderedTemplate:
        if isinstance(request, str):
            request = self.loader.load_oplata_request(request)
        event: dict[str, tp.Any] = request['event']

        self._update_event_merchant(sender_state, event['merchant'], contract_type)
        self._update_event_order(
            sender_state,
            event['order'],
            price=order_price,
            commission=commission,
            item_by_card=item_by_card,
            item_by_promocode=item_by_promocode,
        )
        self._update_event_transaction(event['transaction'])

        return request

    def render_cashless_refund_request(
        self,
        sender_state: 'state.PipelineState',
        contract_type: type,
        request: loader.RenderedTemplateOrTemplatePath,
        original_order_price: float = None,
        refund_price: float = None,
        item_by_card: float = None,
        item_by_promocode: float = None,
    ) -> loader.RenderedTemplate:
        if isinstance(request, str):
            request = self.loader.load_oplata_request(request)
        event: dict[str, tp.Any] = request['event']

        self._update_event_merchant(sender_state, event['merchant'], contract_type)
        self._update_event_order(
            sender_state,
            event['original_order'],
            price=original_order_price,
            item_by_card=item_by_card,
            item_by_promocode=item_by_promocode,
        ),
        self._update_event_order(
            sender_state,
            event['refund'],
            refund=True,
            price=refund_price,
            item_by_card=item_by_card,
            item_by_promocode=item_by_promocode,
        )
        self._update_event_transaction(event['original_order_transaction'])

        return request

    @staticmethod
    def _update_event_merchant(
        sender_state: 'state.PipelineState',
        merchant: dict[str, tp.Any],
        contract_type: type,
    ) -> None:
        merchant.update({
            'client_id': sender_state.client_id,
            'contract_id': sender_state.get_contract(contract_type).id,
        })

    @staticmethod
    def _update_event_order(
        st: 'state.PipelineState',
        order: dict[str, tp.Any],
        refund: bool = False,
        price: float = None,
        commission: float = None,
        item_by_card: float = None,
        item_by_promocode: float = None,
    ) -> None:
        uid = st.refund_uid if refund else st.order_uid
        order_id = st.refund_id if refund else st.refund_id

        order.update({
            'uid': uid,
            'order_id': order_id,
            'created': formatted.shifted_date_iso(hours=-4),
            'updated': formatted.shifted_date_iso(hours=-3),
            'held_at': formatted.shifted_date_iso(hours=-3),
            'pay_status_updated_at': formatted.shifted_date_iso(hours=-2),
            'closed': formatted.shifted_date_iso(hours=-1),
        })

        if price is not None:
            order['price'] = price

        if commission is not None:
            order['commission'] = commission

        for item in order.get('items', []):
            without_markup = item_by_card is None and item_by_promocode is None
            item.update({
                'total_price': price,
                'markup': None if without_markup else {
                    'card': item_by_card,
                    'virtual::new_promocode': item_by_promocode,
                }
            })

    @staticmethod
    def _update_event_transaction(transaction: dict[str, tp.Any]) -> None:
        transaction.update({
            'created': formatted.shifted_date_iso(hours=-4),
            'updated': formatted.shifted_date_iso(hours=-3),
        })


class ProcessorActotronActRowsRenderer(BaseProcessorRenderer, ProcessorActotronActRowsRendererMixin):
    def render_request(self, sender_state: 'state.ExtendedPipelineState') -> dict:
        pass

    def render_acts_request(
        self,
        st: 'state.PipelineState',
        contract_type: type,
        request: loader.RenderedTemplateOrTemplatePath,
        namespace: str,
        act_sum_positive: float,
        act_sum_negative: float,
        act_sum_wo_vat_positive: float,
        act_sum_wo_vat_negative: float,
    ) -> loader.RenderedTemplate:
        if isinstance(request, str):
            request = self.loader.load_actotron_act_rows_request(request)

        return super().render_acts_request(
            st=st,
            contract_type=contract_type,
            request=request,
            namespace=namespace,
            act_sum_positive=act_sum_positive,
            act_sum_negative=act_sum_negative,
            act_sum_wo_vat_positive=act_sum_wo_vat_positive,
            act_sum_wo_vat_negative=act_sum_wo_vat_negative
        )


class ProcessorTaxiRenderer(BaseProcessorRenderer):
    def render_request(self, sender_state: 'state.ExtendedPipelineState') -> dict:
        pass

    def render_stream_request(
        self,
        st: 'state.PipelineState',
        contract_type: type,  # subclass of lib.contract.Contract
        request: loader.RenderedTemplateOrTemplatePath,
        extended_params: dict = None,
    ) -> loader.RenderedTemplate:
        contract = st.get_contract(contract_type)
        if isinstance(request, str):
            request = self.loader.load_taxi_request(request)
        event: dict[str, tp.Any] = request['event']
        amount = extended_params.get('amount') or event.get('amount')
        event['payload']['amount_details']['base_amount'] = str(abs(amount))
        t_id = rand.int64()
        st.add_transactions([t_id])
        event.update({
            "amount": amount,
            "client_id": st.client_id,
            "contract_id": contract.id,
            "due": formatted.shifted_date_iso(hours=-3),
            "event_time": formatted.shifted_date_iso(seconds=-5),
            "invoice_date": formatted.shifted_date_iso(hours=-2),
            "service_transaction_id": st.service_transaction_id,
            "transaction_id": t_id,
            "transaction_time": formatted.shifted_date_iso(hours=-2),
        })
        if extended_params:
            event.update(extended_params)
        return request

    def render_revenue_request(
        self,
        st: 'state.PipelineState',
        request: loader.RenderedTemplateOrTemplatePath,
        extended_params: dict = None,
    ) -> loader.RenderedTemplate:
        if isinstance(request, str):
            request = self.loader.load_taxi_request(request)
        event: dict[str, tp.Any] = request['event']
        amount = extended_params.get('amount') or event.get('amount')
        event['payload']['amount_details']['base_amount'] = amount
        event['payload']['contract_id'] = st.get_contract(contr.ServiceContract).id
        t_id = rand.int64()
        st.add_transactions([t_id])
        event.update({
            "amount": amount,
            "client_id": st.client_id,
            "contract_id": st.get_contract(contr.ServiceContract).id,
            "due": formatted.shifted_date_iso(hours=-3),
            "event_time": formatted.shifted_date_iso(seconds=-5),
            "invoice_date": formatted.shifted_date_iso(hours=-2),
            "orig_transaction_id": t_id,
            "service_transaction_id": st.service_transaction_id,
            "transaction_id": t_id,
            "transaction_time": formatted.shifted_date_iso(hours=-2),
        })
        if extended_params:
            event.update(extended_params)
        return request

    def render_payout_request(
        self,
        st: 'state.PipelineState',
        request: loader.RenderedTemplateOrTemplatePath,
        extended_params: dict = None,
    ) -> loader.RenderedTemplate:
        if isinstance(request, str):
            request = self.loader.load_taxi_request(request)
        return self._render_payout_request(st, request, extended_params)

    def render_fuel_hold_request(
        self,
        st: 'state.PipelineState',
        request: loader.RenderedTemplateOrTemplatePath,
        extended_params: dict = None,
    ) -> loader.RenderedTemplate:
        if isinstance(request, str):
            request = self.loader.load_taxi_request(request)
        event = request['event']
        amount = extended_params.get('amount') or event.get('amount')
        t_id = rand.int64()
        st.add_transactions([t_id])
        event.update({
            "amount": amount,
            "dt": formatted.shifted_date_iso(seconds=-5),
            "id": rand.int64(),
            "client_id": st.client_id,
            "contract_id": st.get_contract(contr.ServiceContract).id,
            "invoice_eid": rand.uuid(),
            "partner_id": st.client_id,
            "person_id": st.person_id,
            "transaction_id": str(t_id),
            "transaction_dt": formatted.shifted_date_iso(hours=-1),
            "total_sum": amount,
        })
        if extended_params:
            event.update(extended_params)
        return request

    def render_transfer_init_request(
        self,
        sender_state: 'state.PipelineState',
        receiver_state: 'state.PipelineState',
        request: loader.RenderedTemplateOrTemplatePath,
        extended_params: dict = None,
    ) -> loader.RenderedTemplate:
        if isinstance(request, str):
            request = self.loader.load_taxi_request(request)
        event = request['event']
        transaction_id = rand.uuid()
        sender_state.add_transactions([transaction_id])
        event.update({
            "event_time": formatted.date_iso(),
            "recipient_billing_client_id": receiver_state.client_id,
            "recipient_billing_contract_id": receiver_state.get_contract(contr.TransferContract).id,
            "sender_billing_client_id": sender_state.client_id,
            "sender_billing_contract_id": sender_state.get_contract(contr.ServiceContract).id,
            "transaction_id": transaction_id,
        })
        if extended_params:
            event.update(extended_params)
        return request

    def render_transfer_cancel_request(
        self,
        sender_state: 'state.PipelineState',
        transaction_id: str,
        request: loader.RenderedTemplateOrTemplatePath,
        extended_params: dict = None,
    ) -> loader.RenderedTemplate:
        if isinstance(request, str):
            request = self.loader.load_taxi_request(request)
        event = request['event']
        event.update({
            "sender_billing_client_id": sender_state.client_id,
            "sender_billing_contract_id": sender_state.get_contract(contr.ServiceContract).id,
            "transaction_id": transaction_id,
        })
        if extended_params:
            event.update(extended_params)
        return request
