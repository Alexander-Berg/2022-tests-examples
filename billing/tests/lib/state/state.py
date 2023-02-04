import typing as tp

from billing.library.python.calculator.models.personal_account import ServiceCode
from billing.library.python.calculator.values import PersonType, PaymentMethodID
from dataclasses import dataclass

from billing.hot.tests.lib.rand import rand
from billing.hot.tests.lib.state import contract


@dataclass
class PipelineState:
    request_id: str
    client_id: int
    contracts: dict[str, contract.Contract]
    account_id: int
    passport_id: int
    person_id: int
    person_type: PersonType
    service_code: tp.Optional[ServiceCode]
    withholding_commissions_from_payments: tp.Optional[bool]
    external_id: str
    transaction_ids: list[tp.Union[str, int]]
    new_transaction_ids: list[tp.Union[str, int]]
    service_transaction_id: str
    firm_id: int
    order_uid: int
    order_id: int
    refund_uid: int
    refund_id: int

    def add_transactions(self, ids: list[tp.Union[str, int]]) -> None:
        self.transaction_ids.extend(ids)
        self.new_transaction_ids = ids

    def with_firm(self, firm_id: int) -> None:
        self.firm_id = firm_id

    def with_person_type(self, person_type: PersonType) -> None:
        self.person_type = person_type

    def with_service_code(self, service_code: ServiceCode) -> None:
        self.service_code = service_code

    def with_client_id(self, client_id: int) -> None:
        self.client_id = client_id

    def with_withholding_commissions_from_payments(self, checkmark: bool) -> None:
        self.withholding_commissions_from_payments = checkmark

    def with_new_generated_external_id(self) -> None:
        self.external_id = rand.uuid()

    def with_new_generated_order_uid(self) -> None:
        self.order_uid = rand.int64()

    def try_add_contract(self, c: contract.Contract) -> bool:
        if c.name() in self.contracts:
            return False
        c.add_to(self.contracts)
        return True

    def get_contract(self, contract_type: type) -> contract.Contract:
        assert issubclass(contract_type, contract.Contract)
        result = self.contracts.get(contract_type.name())
        if not result:
            raise ValueError(f"cannot do action: {contract_type.name()} is not initialized for client {self.client_id}")
        return result

    @staticmethod
    def _generate_default_kwargs() -> dict:
        min_safe_client_id = 1_000_000
        # collisions in db are possible by client_id, but very unlikely
        client_id = rand.int64(from_num=min_safe_client_id)
        account_id = rand.int64()
        passport_id = rand.int64()
        person_id = rand.int64()
        person_type = PersonType.UR
        request_id = rand.hex(16)
        external_id = rand.uuid()
        transaction_ids = []
        new_transaction_ids = []
        service_transaction_id = str(rand.int64())
        firm_id = rand.int64()
        order_uid = rand.int64()
        order_id = rand.int64()
        refund_uid = rand.int64()
        refund_id = rand.int64()

        return dict(
            request_id=request_id, client_id=client_id, account_id=account_id, person_id=person_id,
            service_transaction_id=service_transaction_id, passport_id=passport_id,
            contracts={}, external_id=external_id, transaction_ids=transaction_ids,
            new_transaction_ids=new_transaction_ids, firm_id=firm_id, person_type=person_type,
            service_code=None, withholding_commissions_from_payments=None, order_uid=order_uid,
            order_id=order_id, refund_uid=refund_uid, refund_id=refund_id
        )

    @classmethod
    def generate(cls) -> 'PipelineState':
        return PipelineState(**cls._generate_default_kwargs())


@dataclass
class ExtendedPipelineState(PipelineState):
    namespace: str
    endpoint: str
    template: str
    payment_method_id: PaymentMethodID
    service_id: int
    event_currency: str
    contract_params: tp.Optional[dict]
    rows: list[dict[str, tp.Any]]
    refunds: list[dict[str, tp.Any]]
    products_params: tp.Optional[list[dict[str, tp.Any]]]
    event_params: tp.Optional[dict[str, tp.Any]]

    def with_namespace(self, namespace: str) -> None:
        self.namespace = namespace

    def with_endpoint(self, endpoint: str) -> None:
        self.endpoint = endpoint

    def with_template(self, template: str) -> None:
        self.template = template

    def with_payment_method_id(self, payment_method_id: PaymentMethodID) -> None:
        self.payment_method_id = payment_method_id

    def with_service_id(self, service_id: int) -> None:
        self.service_id = service_id

    def with_event_currency(self, event_currency: str) -> None:
        self.event_currency = event_currency

    def with_contract_params(self, contract_params: dict) -> None:
        self.contract_params = contract_params

    def with_rows(self, rows: list[dict[str, tp.Any]]) -> None:
        self.rows = rows

    def with_refunds(self, refunds: list[dict[str, tp.Any]]) -> None:
        self.refunds = refunds

    def with_products_params(self, products_params: list[dict[str, tp.Any]]) -> None:
        self.products_params = products_params

    def with_event_params(self, event_params: dict[str, tp.Any]) -> None:
        self.event_params = event_params

    @classmethod
    def generate(cls) -> 'ExtendedPipelineState':
        kwargs = cls._generate_default_kwargs() | {
            'payment_method_id': PaymentMethodID.CARD,
            'template': None,
            'event_currency': 'RUB',
            'contract_params': None,
            'products_params': None,
            'event_params': None,
            'rows': [],
            'refunds': [],
            'namespace': '',
            'endpoint': '',
            'service_id': -1,
        }
        return ExtendedPipelineState(**kwargs)
