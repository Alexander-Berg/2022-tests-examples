import typing as tp
from copy import deepcopy
from dataclasses import dataclass

from billing.library.python.calculator.models.personal_account import ServiceCode
from billing.library.python.calculator.values import PaymentMethodID
from billing.hot.tests.config.config import BaseConfig
from billing.hot.tests.lib.state import contract as contr
from billing.hot.tests.lib.schema import extract
from billing.hot.tests.lib.templates import processor as renderer


@dataclass
class TrustTestCaseEvent(BaseConfig):
    payment_method_id: tp.Optional[PaymentMethodID]
    currency: str
    rows: list[dict[str, tp.Any]]
    refunds: list[dict[str, tp.Any]]
    products: tp.Optional[list[dict[str, tp.Any]]]

    @classmethod
    def from_dict(cls, dct: dict[str, tp.Any]) -> 'TrustTestCaseEvent':
        payment_method_id = PaymentMethodID.from_code(dct['payment_method_id']) if 'payment_method_id' in dct else None
        return cls(
            payment_method_id=payment_method_id,
            currency=dct.get('currency') or 'RUB',
            products=dct.get('products'),
            rows=dct.get('rows', []),
            refunds=dct.get('refunds', [])
        )


@dataclass
class ProcessorTestCaseInput(BaseConfig):
    template: str
    contract_type: tp.Type[contr.Contract]
    renderer_type: tp.Type[renderer.BaseProcessorRenderer]
    client_id: tp.Optional[int]
    service_id: int
    person_type: tp.Optional[str]
    firm_id: tp.Optional[int]
    dry_run: tp.Optional[bool]
    contract: tp.Optional[dict]
    service_code: tp.Optional[ServiceCode]
    trust_event: TrustTestCaseEvent
    event_params: tp.Optional[dict[str, tp.Any]]

    @staticmethod
    def _get_trust_event(dct: dict[str, tp.Any]) -> tp.Optional[TrustTestCaseEvent]:
        return TrustTestCaseEvent.from_dict(dct.get('trust_event')) if 'trust_event' in dct else None

    @classmethod
    def from_dict(cls, dct: dict[str, tp.Any]) -> 'ProcessorTestCaseInput':
        return cls(
            template=dct.get('template'),
            contract=dct.get('contract'),
            client_id=dct.get('client_id'),
            service_id=dct.get('service_id'),
            person_type=dct.get('person_type'),
            firm_id=dct.get('firm_id'),
            dry_run=dct.get('dry_run'),
            contract_type=getattr(contr, dct.get('contract_type')),
            renderer_type=getattr(renderer, dct.get('renderer_type')),
            trust_event=cls._get_trust_event(dct),
            event_params=dct.get('event'),
            service_code=ServiceCode(dct.get('service_code')) if 'service_code' in dct else None,
        )


@dataclass
class ExpectedResponse(BaseConfig):
    status: tp.Optional[int]
    data: tp.Optional[tp.Union[dict, list]]


@dataclass
class TestCaseExpected(BaseConfig):
    response: ExpectedResponse


@dataclass
class AccountsTestCaseInput(BaseConfig):
    accounts: list[str]


@dataclass
class ProcessorTestCaseData(BaseConfig):
    input: ProcessorTestCaseInput
    expected: TestCaseExpected


@dataclass
class AccountsTestCaseData(BaseConfig):
    input: AccountsTestCaseInput
    expected: TestCaseExpected


@dataclass
class ForeachCaseGenerator(BaseConfig):
    paths: list[list[str]]
    values: list[list[tp.Any]]

    @classmethod
    def from_dict(cls, dct: dict[str, tp.Any]) -> 'ForeachCaseGenerator':
        return cls(
            paths=[path.split('.') for path in dct.get('paths', [])],
            values=dct.get('values', [])
        )

    def _get_update_dict(self, val: tp.Any, path: list[str]) -> tp.Union[dict, list]:
        """
        >>> self._get_update_dict(5, ['path', 'to', '[0]', 'key'])
        ... {'path': {'to': [{'key': 5}]}}
        >>> self._get_update_dict(5, ['path', 'to', '0', 'key'])
        ... {'path': {'to': {'0': {'key': 5}}}}
        """
        if not path:
            return val
        if path[0][0] == '[' and path[0][-1] == ']':  # list item
            return [self._get_update_dict(val, path[1:])]
        return {path[0]: self._get_update_dict(val, path[1:])}

    @classmethod
    def _deep_update(cls, target: dict, update_data: dict) -> dict:
        for key, value in update_data.items():
            if isinstance(value, dict):
                target[key] = cls._deep_update(target.get(key, {}), value)
            else:
                target[key] = value
        return target

    def _update_case(self, case_dict: dict, values: list[tp.Any]) -> dict:
        upd_dict = None
        for value, path in zip(values, self.paths):
            upd_dict = self._deep_update(case_dict, self._get_update_dict(value, path))
        return upd_dict

    def generate_cases(self, case_dict: dict) -> list['TestCase']:
        return [TestCase.from_dict(self._update_case(deepcopy(case_dict), vals)) for vals in self.values]


@dataclass
class TestCase(BaseConfig):
    namespace: str
    endpoint: str
    processor: ProcessorTestCaseData
    accounts: tp.Optional[AccountsTestCaseData]
    description: tp.Optional[str]

    @classmethod
    def from_dict(cls, dct: dict[str, tp.Any]) -> 'TestCase':
        return cls(
            namespace=dct.get("namespace"),
            endpoint=dct.get('endpoint'),
            description=dct.get('description'),
            processor=ProcessorTestCaseData.from_dict(dct.get('processor')),
            accounts=AccountsTestCaseData.from_dict(dct.get('accounts')) if dct.get('accounts') else None,
        )


@dataclass
class ObfuscationConfig(BaseConfig):
    obfuscate_term: str
    processor_result_paths: list[str]
    accounts_balance_paths: list[str]
    accounts_event_paths: list[str]
    act_rows_paths: tp.Optional[list[str]]
    acted_events_paths: tp.Optional[list[str]]


@dataclass
class SystemTestBundleConfig(BaseConfig):
    processor_input: ProcessorTestCaseInput
    obfuscation: ObfuscationConfig
    accounts: list[str]
    act_o_tron_loc_attrs: list[str]
    loc_kwargs: dict[str, tp.Any]
    endpoint: str
    extract_external_ids_func: tp.Callable[[dict[str, tp.Any]], list[tp.Union[str, int]]]

    @classmethod
    def from_dict(cls, dct: dict[str, tp.Any]):
        return cls(
            processor_input=ProcessorTestCaseInput.from_dict(dct=dct.get('processor_input')),
            obfuscation=ObfuscationConfig.from_dict(dct=dct.get('obfuscation')),
            accounts=dct.get('accounts'),
            act_o_tron_loc_attrs=dct.get('act_o_tron_loc_attrs'),
            loc_kwargs=dct.get('loc_kwargs'),
            endpoint=dct.get('endpoint'),
            extract_external_ids_func=getattr(extract, dct.get('extract_external_ids_func'))
        )


@dataclass
class TestConfig(BaseConfig):
    namespace: str
    system: tp.Optional[SystemTestBundleConfig]
    cases: tp.Optional[dict[str, list[TestCase]]]  # endpoint: list[TestCase]

    @classmethod
    def _get_cases_dict(cls, dct: tp.Optional[dict[str, tp.Any]]) -> tp.Optional[dict[str, list[TestCase]]]:
        if 'cases' not in dct:
            return None
        namespace = dct.get('namespace')
        cases_dict = {}
        for endpoint, cases in dct['cases'].items():
            cases_dict[endpoint] = []
            for case in cases:
                case['namespace'] = namespace
                case['endpoint'] = endpoint
                if 'foreach' not in case:
                    cases_dict[endpoint].append(TestCase.from_dict(case))
                else:
                    generator = ForeachCaseGenerator.from_dict(case.get('foreach'))
                    cases_dict[endpoint] += generator.generate_cases(case_dict=case)
        return cases_dict

    @classmethod
    def _get_system_test_bundle(cls, dct: dict[str, tp.Any]) -> tp.Optional[SystemTestBundleConfig]:
        if 'system_test_bundle' in dct:
            return SystemTestBundleConfig.from_dict(dct=dct.get('system_test_bundle'))
        return None

    @classmethod
    def from_dict(cls, dct: dict[str, tp.Any]) -> 'TestConfig':
        return cls(
            namespace=dct.get('namespace'),
            cases=cls._get_cases_dict(dct=dct),
            system=cls._get_system_test_bundle(dct=dct)
        )


@dataclass
class AccountConfig(BaseConfig):
    account: str
    analytic: dict[str, tp.Any]


@dataclass
class BalancesConfig(BaseConfig):
    balances: dict[str, list[AccountConfig]]

    @classmethod
    def from_dict(cls, dct: dict[str, tp.Any]) -> 'BalancesConfig':
        return cls(
            balances={
                namespace: [
                    AccountConfig(
                        account=account, analytic=analytic
                    ) for account, analytic in accounts_analytics.items()
                ]
                for namespace, accounts_analytics in dct.items()
            }
        )
