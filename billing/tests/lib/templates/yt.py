from datetime import datetime, timedelta
from os import path
from itertools import permutations

from billing.hot.tests.lib.date import formatted, timestamp
from billing.hot.tests.lib.rand import rand
from billing.hot.tests.lib.state import state
from billing.hot.tests.lib.templates import loader
from billing.hot.tests.lib.util import util


class YtLoader:
    def __init__(self, template_dir: str) -> None:
        self.loader = loader.TemplateLoader(template_dir)

    def load_contracts(self, name: str = 'basic.json') -> loader.RenderedTemplate:
        return self.loader.load(path.join('contracts', name))

    def load_accounts(self, name: str = 'basic.json') -> loader.RenderedTemplate:
        return self.loader.load(path.join('accounts', name))

    def load_firm(self, name: str = 'basic.json') -> loader.RenderedTemplate:
        return self.loader.load(path.join('firms', name))


class YtRenderer:
    def __init__(self, lder: YtLoader) -> None:
        self.loader = lder

    def render_contract(
        self,
        st: state.PipelineState,
        contract_id: int,
        contract: loader.RenderedTemplateOrTemplatePath = 'basic.json',
        extended_params: dict = None
    ) -> loader.RenderedTemplate:
        if isinstance(contract, str):
            contract = self.loader.load_contracts(name=contract)
        contract.update(
            {
                'client_id': st.client_id,
                'external_id': st.external_id,
                'id': contract_id,
                'passport_id': st.passport_id,
                'person_id': st.person_id,
                'person_type': st.person_type.value if st.person_type else None,
                'update_dt': contract['update_dt'] or formatted.shifted_date_iso_seconds(hours=-6),
            }
        )

        collateral = contract['collaterals']['0']
        checkmark = st.withholding_commissions_from_payments or False
        collateral.update({
            'firm': st.firm_id,
            'withholding_commissions_from_payments': int(checkmark),
        })
        if extended_params:
            contract = util.merge(contract, extended_params)
        return contract

    def render_account(
        self, st: state.PipelineState, contract_id: int,
        account: loader.RenderedTemplateOrTemplatePath = 'basic.json',
    ) -> loader.RenderedTemplate:
        if isinstance(account, str):
            account = self.loader.load_accounts(name=account)
        account.update(
            {
                'contract_id': contract_id,
                'dt': formatted.shifted_date_iso_seconds(years=-1),
                'external_id': st.external_id,
                'id': rand.int32(),
                'passport_id': st.passport_id,
                'realdt': formatted.shifted_date_iso_seconds(years=-1),
                'receipt_dt': formatted.shifted_date_iso_seconds(years=-2),
                'receipt_dt_1c': formatted.shifted_date_iso_seconds(years=-2),
                'turn_on_dt': formatted.shifted_date_iso_seconds(years=-2),
                'service_code': st.service_code.value if st.service_code else None,
            }
        )
        person = account['person']
        person.update(
            {
                'account': person['account'] or str(rand.int32()),
                'client_id': st.client_id,
                'dt': formatted.shifted_date_iso_seconds(years=2),
                'id': st.person_id,
                'passport_id': st.passport_id,
            }
        )
        return account

    def render_firm(
        self, st: state.PipelineState, firm: loader.RenderedTemplateOrTemplatePath = 'basic.json',
    ) -> loader.RenderedTemplate:
        if isinstance(firm, str):
            firm = self.loader.load_firm(name=firm)
        firm.update(
            {
                'id': st.firm_id,
            }
        )
        return firm

    @classmethod
    def render_iso_currency_rates(cls, currencies: list[str]) -> list[loader.RenderedTemplate]:
        dt = timestamp.now_dt_second(date=datetime.now() - timedelta(days=7))
        rates = []

        for from_cur, to_cur in permutations(currencies, 2):
            row_id = rand.int32()
            rates.append({
                "id": row_id,
                "src_cc": "tcmb",
                "iso_currency_from": from_cur,
                "iso_currency_to": to_cur,
                "dt": dt,
                "obj": {
                    "dt": dt,
                    "id": row_id,
                    "iso_currency_from": from_cur,
                    "iso_currency_to": to_cur,
                    "rate_from": "10",
                    "rate_to": "1",
                    "src_cc": "tcmb",
                    "version_id": 0
                },
                "_rest": {
                    "classname": "IsoCurrencyRate",
                    "version": 1
                }
            })

        return rates
