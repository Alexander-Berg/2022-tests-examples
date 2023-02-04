import datetime
import time
import typing as tp
from os import path

from billing.hot.tests.lib.state import state
from billing.hot.tests.lib.templates import loader
from billing.hot.tests.lib.state import contract
from billing.hot.tests.lib.rand import rand
from billing.hot.tests.lib.util.util import merge


class Balance:
    def __init__(self, account: str, analytic: dict[str, tp.Any]) -> None:
        self.account = account
        self.analytic = analytic

    def render(self, client_id: str, ts: int, namespace: str) -> loader.RenderedTemplate:
        balance = {
            'dt': ts,
            'loc': {
                'client_id': client_id,
                'type': self.account,
                'namespace': namespace,
            },
        }

        for field, value in self.analytic.items():
            balance['loc'].setdefault(field, value)

        return balance


class AccountsReadBatchRenderer:
    def __init__(self, balances_by_namespace: dict[str, tp.Sequence[Balance]]) -> None:
        self.balances_by_namespace = {
            namespace: {
                balance.account: balance for balance in balances_by_namespace[namespace]
            }
            for namespace in balances_by_namespace
        }

    def render_read_balances_request(
        self, st: state.PipelineState, ts: int, accounts: list[str], namespace: str
    ) -> loader.RenderedTemplate:
        namespace_balances = self.balances_by_namespace.get(namespace)
        if not namespace_balances:
            raise ValueError(f'not balances with namespace={namespace} found.')

        return {
            'balances': [
                self.render_balance(
                    namespace_balances, account, str(st.client_id), ts, namespace
                )
                for account in accounts
            ]
        }

    @staticmethod
    def render_balance(
        balances: dict[str, Balance],
        account: str,
        client_id: str,
        ts: int,
        namespace: str,
    ) -> loader.RenderedTemplate:
        balance = balances.get(account)
        if not balance:
            raise ValueError(f'balance with type={account} not found')

        return balance.render(client_id, ts, namespace)


class AccountsWriteBatchRenderer:
    def __init__(self, template_dir) -> None:
        self.loader = loader.TemplateLoader(template_dir)

    def fill_info(
        self,
        st: state.PipelineState,
        extend_info: dict = None,
        namespace: str = "taxi",
        extended_params: dict = None,
        contract_states: dict = None
    ) -> dict:
        extended_params = extended_params or {}
        extend_info = extend_info or {}

        info = self.loader.load(path.join('write_batch', 'info.json'))
        time_format = '%Y-%m-%dT%H:%M:%S+00:00'
        date = datetime.datetime.now().strftime(time_format)
        info.update({
            'client_id': str(st.client_id),
            'event_time': date,
            'transaction_id': f'payout/{namespace}/{st.client_id}/{date}',
        })
        info = merge(info, extend_info)
        info['tariffer_payload'] = merge(info['tariffer_payload'], extended_params)
        info['tariffer_payload']['side_batch_info']['tariffer_payload']['dry_run'] = \
            extended_params.get('dry_run', True)
        if contract_states:
            info['tariffer_payload']['contract_states'] = contract_states
        return info

    def fill_invoice(self, st: state.PipelineState, extended_params: dict = {}) -> dict:
        invoice = self.loader.load(path.join('write_batch', 'invoice.json'))
        invoice.update({
            'id': rand.int64(),
            'external_id': st.external_id,
        })
        invoice = merge(invoice, extended_params)
        return invoice

    def fill_event(self, st: state.PipelineState, extended_params: dict = {}) -> dict:
        event = self.loader.load(path.join('write_batch', 'event.json'))
        event['loc'].update({
            'client_id': str(st.client_id),
            'contract_id': str(st.get_contract(contract.ServiceContract).id),
        })
        event['dt'] = round(time.mktime(datetime.datetime.now().timetuple()))
        event['loc'] = merge(event['loc'], extended_params)
        event = merge(event, extended_params)
        return event

    def fill_contract_state(self, st: state.PipelineState, contract_id: int = -1,
                            extended_params: dict = {}, invoices: list[dict] = []) -> dict:
        if contract_id == -1:
            contract_id = str(st.get_contract(contract.ServiceContract).id)
        contract_state = self.loader.load(path.join('write_batch', 'contract_state.json'))
        contract_state = merge(contract_state, extended_params)
        if invoices:
            contract_state['invoices'] = invoices
        return {contract_id: contract_state}

    def render_write_batch_request(self, st: state.PipelineState, extended_params: dict = {},
                                   info: dict = {}, events: dict = {}):
        request = self.loader.load(path.join('write_batch', 'basic.json'))
        request.update({
            'external_id': st.external_id,
            'dt': round(time.mktime(datetime.datetime.now().timetuple()))
        })
        request = merge(request, extended_params)
        if info:
            request['info'] = info
        if events:
            request['events'] = events
        return request
