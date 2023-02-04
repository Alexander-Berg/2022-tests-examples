import logging
import typing as tp

from billing.library.python.calculator.models.personal_account import ServiceCode
from billing.library.python.calculator.values import PersonType, PaymentMethodID

from billing.hot.tests.clients.yt import client as yt_client
from billing.hot.tests.lib.state import state, contract as contr
from billing.hot.tests.lib.templates import loader
from billing.hot.tests.lib.templates import yt as yt_template

logger = logging.getLogger(__name__)


class Builder:
    def __init__(
        self, yt: yt_client.Client, yt_renderer: yt_template.YtRenderer, st: state.PipelineState = None,
    ) -> None:
        self._state = st or state.PipelineState.generate()
        self.yt = yt
        self.yt_renderer = yt_renderer

    def with_firm(self, firm_id: int) -> None:
        self._state.with_firm(firm_id)

    def with_person_type(self, person_type: PersonType) -> None:
        self._state.with_person_type(person_type)

    def with_service_code(self, service_code: ServiceCode) -> None:
        self._state.with_service_code(service_code)

    def with_client_id(self, client_id: int) -> None:
        self._state.with_client_id(client_id)

    def with_withholding_commissions_from_payments(self, checkmark: bool) -> None:
        self._state.with_withholding_commissions_from_payments(checkmark)

    def fill_contracts(self, contracts: list = None, dry_run: bool = False,
                       namespace: str = 'taxi', filter: str = 'Client', migrate: bool = True) -> None:
        if not contracts:
            contracts = [contr.ServiceContract.generate()]
        for contract in contracts:
            self.try_fill_contract(contract, migrate=migrate)
        if migrate:
            self.write_clients_migrated(dry_run, namespace, filter)

    def try_fill_contract(self, contract, migrate=True) -> None:
        if not isinstance(contract, contr.Contract):
            assert issubclass(contract, contr.Contract), "contract must be either contr.Contract instance or subclass"
            contract = contract.generate()

        if not self._state.try_add_contract(contract):
            return
        if migrate:
            self._write_contract(contract)

    def built_state(self) -> state.PipelineState:
        logger.info('state: %s', self._state)
        return self._state

    def write_clients_migrated_old(self, client_id: int = None) -> None:
        if not client_id:
            client_id = self._state.client_id
        self.yt.insert_rows('clients_migrated', [{'client_id': client_id, 'from_dt': '2021-01-01T00:00:00Z'}])

    def write_clients_migrated(
        self,
        dry_run: bool = False,
        namespace: str = 'taxi',
        filter: str = 'Client',
    ):
        object_id = self._state.client_id if filter == 'Client' else self._state.firm_id
        migration_info = {
            'object_id': object_id,
            'from_dt': '2021-01-02T00:00:00Z',
            'dry_run': int(dry_run),
            'namespace': namespace,
            'filter': filter,
        }
        logger.info(f'builder: migration info {migration_info}')
        self.yt.insert_rows('clients_migrated_new', [migration_info])

    def _write_clients_contracts(self, client_id: int, contract_id: int) -> None:
        self.yt.insert_rows('contract_client_idx', [{'client_id': client_id, 'id': contract_id}], atomicity='none')

    def _write_clients_accounts(self, client_id: int, account_id: int) -> None:
        self.yt.insert_rows('personal_account_client_idx', [{'client_id': client_id, 'id': account_id}])

    def _write_accounts(self, contract_id: int, account: loader.RenderedTemplateOrTemplatePath = 'basic.json') -> None:
        account = self.yt_renderer.render_account(self._state, contract_id, account)
        self.yt.insert_rows('personal_accounts', [{
            'id': account['id'],
            'client_id': account['person']['client_id'],
            'obj': account,
            'contract_id': account['contract_id'],
            'version': 0,
            "_rest": {
                "classname": "PersonalAccount"
            }
        }])
        logger.info(f"builder: personal account {account}")
        self._write_clients_accounts(account['person']['client_id'], account['id'])

    def _write_firm(self, firm: loader.RenderedTemplateOrTemplatePath = 'basic.json') -> None:
        firm = self.yt_renderer.render_firm(self._state, firm)
        self.yt.insert_rows('firm_tax', [firm], atomicity='full')
        logger.info(f'builder: yt firm {firm}')

    def _write_contract(
        self,
        contract,
        contract_model: loader.RenderedTemplateOrTemplatePath = 'basic.json',
    ) -> None:
        contract = self._render_contract(contract, contract_model)
        self.yt.insert_rows('contracts', [{
            "client_id": contract["client_id"],
            "id": contract["id"],
            "obj": contract,
            "version": contract["version_id"],
            "_rest": {
                "classname": "Contract",
            }
        }], atomicity='none')
        logger.info(f"builder: yt contract {contract}")
        self._write_clients_contracts(contract["client_id"], contract["id"])
        self._write_accounts(contract["id"])
        self._write_firm()
        self._write_iso_currency_rate(contract['currency_iso_code'])

    def _write_iso_currency_rate(self, currency: str) -> None:
        rates = self.yt_renderer.render_iso_currency_rates(currencies=list({'RUB', 'USD', currency}))

        self.yt.insert_rows('iso_currency_rate', rates, atomicity='none')

        logger.info(f"builder: yt rates {rates}")

    def _render_contract(
        self, contract: contr.Contract, contract_model: loader.RenderedTemplateOrTemplatePath = 'basic.json'
    ) -> dict:
        return self.yt_renderer.render_contract(self._state, contract.id, contract_model, contract.params())


class ExtendedBuilder(Builder):
    def __init__(self, yt: yt_client.Client, yt_renderer: yt_template.YtRenderer) -> None:
        super().__init__(yt, yt_renderer)
        self.yt = yt
        self.yt_renderer = yt_renderer

        self._state = state.ExtendedPipelineState.generate()

    def with_namespace(self, namespace: str) -> None:
        self._state.with_namespace(namespace)

    def with_endpoint(self, endpoint: str) -> None:
        self._state.with_endpoint(endpoint)

    def with_template(self, template: str) -> None:
        self._state.with_template(template)

    def with_payment_method_id(self, payment_method_id: PaymentMethodID) -> None:
        self._state.with_payment_method_id(payment_method_id)

    def with_event_currency(self, event_currency: str) -> None:
        self._state.with_event_currency(event_currency)

    def with_contract_params(self, contract_params: dict) -> None:
        self._state.with_contract_params(contract_params)

    def with_service_id(self, service_id: int) -> None:
        self._state.with_service_id(service_id)

    def with_rows(self, rows: list[dict[str, tp.Any]]) -> None:
        self._state.with_rows(rows)

    def with_refunds(self, refunds: list[dict[str, tp.Any]]) -> None:
        self._state.with_refunds(refunds)

    def with_products_params(self, products_params: list[dict[str, tp.Any]]) -> None:
        self._state.with_products_params(products_params)

    def with_event_params(self, event_params: dict[str, tp.Any]) -> None:
        self._state.with_event_params(event_params)

    def built_state(self) -> state.ExtendedPipelineState:
        logger.info('state: %s', self._state)
        return self._state

    def _render_contract(
        self, contract: contr.Contract, contract_model: loader.RenderedTemplateOrTemplatePath = 'basic.json'
    ) -> dict:
        return self.yt_renderer.render_contract(
            self._state, contract.id, contract_model, contract.params(extended_params=self._state.contract_params)
        )

    def clear(self):
        logger.info('clear state: %s', self._state)
        """
        метод для очистки созданного, сейчас удаляем только договоры, т.к все остальное без договора не мешает в тесте,
        но занимает время на удалении, а пересечения по id исключительно редки т.к в рецептах база пуста каждый запуск
        """
        self.yt.delete_rows('contracts', [{'id': c.id} for c in self._state.contracts.values()], atomicity='none')
