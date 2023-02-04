from datetime import datetime
from decimal import Decimal
from typing import Union

import arrow
import pytest
from billing.hot.calculators.bnpl.calculator.core.const import BNPL_SERVICE_ID
from billing.hot.calculators.bnpl.calculator.core.models.event import (
    PaymentEvent, PayoutEvent, RefundEvent
)
from billing.hot.calculators.bnpl.calculator.core.models.method import (
    CashlessReferences, PaymentMethod, PayoutMethod, PayoutReferences, RefundMethod,
)
from billing.hot.calculators.bnpl.calculator.tests.builder import gen_event
from billing.library.python.calculator.models.account import AccountsBatchModel
from billing.library.python.calculator.models.firm import FirmModel
from billing.library.python.calculator.models.method import (
    LockModel, LockLocModel, StateModel
)
from billing.library.python.calculator.models.personal_account import ServiceCode
from billing.library.python.calculator.services.account import OperationExpression
from billing.library.python.calculator.test_utils.builder import (
    gen_firm, gen_general_contract
)
from billing.library.python.calculator.util import to_msk_dt
from billing.library.python.calculator.values import PersonType


@pytest.fixture
def migration_info(firm: FirmModel) -> list[dict]:
    return [
        {
            'namespace': 'bnpl',
            'from_dt': to_msk_dt(datetime(2017, 11, 11)),
            'dry_run': True,
            'filter': 'Firm',
            'object_id': firm.id,
        }
    ]


@pytest.fixture
def contracts(
    payment_event: PaymentEvent,
    firm: FirmModel,
) -> list[dict]:
    return [
        gen_general_contract(
            contract_id=payment_event.billing_contract_id,
            client_id=payment_event.billing_client_id,
            person_id=7,
            services=[BNPL_SERVICE_ID],
            firm=firm.id,
            person_type=firm.person_categories[0].category,
        ),
    ]


@pytest.fixture
def client_contracts(payout_event: PayoutEvent, firm: FirmModel):
    not_payments_service_id = 228

    return [
        gen_general_contract(
            contract_id=1,
            client_id=payout_event.client_id,
            person_id=1,
            services=[BNPL_SERVICE_ID],
            firm=firm.id,
            person_type=PersonType.UR,
            withholding_commissions_from_payments=True,
        ),
        gen_general_contract(
            contract_id=2,
            client_id=payout_event.client_id,
            person_id=4,
            services=[not_payments_service_id],
            firm=firm.id,
            person_type=PersonType.UR,
        ),
    ]


@pytest.fixture
def firm() -> FirmModel:
    return FirmModel(**gen_firm(identity=1, mdh_id='mdh id'))


@pytest.fixture
def cashless_references_with_migration_info(
    contracts: list[dict],
    firm: FirmModel,
    migration_info: list[dict],
) -> CashlessReferences:
    return CashlessReferences(
        contracts=contracts,
        firm=firm,
        migration_info=migration_info,
        products={
            'loan_commission': 'd15388b7-8afe-4af0-a8cb-82feb83945fd',
        },
    )


@pytest.fixture
def cashless_references_without_migration_info(
    contracts: list[dict],
    firm: FirmModel,
) -> CashlessReferences:
    return CashlessReferences(
        contracts=contracts,
        firm=firm,
        migration_info=[],
        products={
            'loan_commission': 'd15388b7-8afe-4af0-a8cb-82feb83945fd',
        },
    )


@pytest.fixture
def references(
    request,
    cashless_references_with_migration_info,
    cashless_references_without_migration_info
) -> CashlessReferences:
    if request.param == 'with migration_info':
        return cashless_references_with_migration_info
    if request.param == 'without migration_info':
        return cashless_references_without_migration_info


@pytest.fixture
def accounts(
    payout_event: PayoutEvent,
    client_contracts: list[dict],
    personal_accounts: list[dict],
) -> AccountsBatchModel:
    dt = arrow.get(payout_event.event_time).int_timestamp
    return AccountsBatchModel(balances=[
        {
            'loc': {
                'type': 'cashless',
                'namespace': 'bnpl',
                'contract_id': client_contracts[0]['id'],
                'client_id': client_contracts[0]['client_id'],
                'currency': 'RUB',
            },
            # баланс 5000
            'credit': Decimal('6000.00'),
            'debit': Decimal('1000.00'),
            'dt': dt,
        },
        {
            'loc': {
                'type': 'cashless_refunds',
                'namespace': 'bnpl',
                'contract_id': client_contracts[0]['id'],
                'client_id': client_contracts[0]['client_id'],
                'currency': 'RUB',
            },
            # баланс 500, зачтем cashless-cashless_refunds на 500
            # баланс cashless 4500
            'credit': Decimal('900'),
            'debit': Decimal('1400'),
            'dt': dt,
        },
        {
            'loc': {
                'type': 'commissions_acted',
                'namespace': 'bnpl',
                'contract_id': client_contracts[0]['id'],
                'client_id': client_contracts[0]['client_id'],
                'invoice_id': personal_accounts[0]['id'],
                'currency': 'RUB',
                'operation_type': '',
            },
            # баланс 6150.5
            'credit': Decimal('50.01'),
            'debit': Decimal('6200.51'),
            'dt': dt,
        },
        {
            'loc': {
                'type': 'commission_refunds_acted',
                'namespace': 'bnpl',
                'contract_id': client_contracts[0]['id'],
                'client_id': client_contracts[0]['client_id'],
                'invoice_id': personal_accounts[0]['id'],
                'currency': 'RUB',
                'operation_type': '',
            },
            # баланс 50.11, зачтем commissions_acted-commissions_acted_refunds на 50.11
            # баланс commissions_acted 6100.39 - зачтем cashless-commissions_acted на весь безнал 4500
            # баланс cashless - 0.0
            'credit': Decimal('60'),
            'debit': Decimal('9.89'),
            'dt': dt,
        },
        # дебитуем incoming_payments на сумму зачета cashless-commissions_acted - 4500
        # перенос на выплату
        # дебитуем на остаток cashless 0
        # кредитуем payout 0
    ])


@pytest.fixture
def operation_expressions() -> list[OperationExpression]:
    return [
        OperationExpression(name='cashless_to_cashless_refunds', expressions=[
            {'amount': Decimal('500.00'), 'account': 'cashless', 'operation_type': 'DEBIT'},
            {'amount': Decimal('500.00'), 'account': 'cashless_refunds', 'operation_type': 'CREDIT'},
        ]),
        OperationExpression(name='commissions_acted_to_commission_refunds_acted', expressions=[
            {'amount': Decimal('50.11'), 'account': 'commissions_acted', 'operation_type': 'CREDIT'},
            {'amount': Decimal('50.11'), 'account': 'commission_refunds_acted', 'operation_type': 'DEBIT'},
        ]),
        OperationExpression(name='cashless_to_commissions_acted', expressions=[
            {'amount': Decimal('4500.00'), 'account': 'cashless', 'operation_type': 'DEBIT'},
            {'amount': Decimal('4500.00'), 'account': 'commissions_acted', 'operation_type': 'CREDIT'},
        ]),
        OperationExpression(name='netting_amount_to_incoming_payments', expressions=[
            {'amount': Decimal('4500.00'), 'account': 'incoming_payments', 'operation_type': 'DEBIT'},
        ]),
        OperationExpression(name='cashless_to_payout', expressions=[
            {'amount': Decimal('0.00'), 'account': 'cashless', 'operation_type': 'DEBIT'},
            {'amount': Decimal('0.00'), 'account': 'payout', 'operation_type': 'CREDIT'},
        ])
    ]


@pytest.fixture
def personal_accounts(client_contracts: list[dict]) -> list[dict]:
    return [
        {
            'id': i,
            'contract_id': contract['id'],
            'client_id': contract['client_id'],
            'version': 2,
            'obj': {
                'id': i,
                'contract_id': contract['id'],
                'external_id': '2134/7482',
                'iso_currency': 'RUB',
                'type': 'personal_account',
                'service_code': ServiceCode.YANDEX_SERVICE.value,
                'hidden': 0,
                'postpay': 1,
            },
        } for i, contract in enumerate(client_contracts)
    ]


@pytest.fixture
def lock(payout_event: PayoutEvent):
    return LockModel(states=[
        StateModel(
            loc=LockLocModel(namespace='bnpl', type='cutoff_dt_state', client_id=payout_event.client_id),
            state=to_msk_dt(datetime(2017, 11, 11)),
        )
    ])


@pytest.fixture
def payout_references_with_migration_info(
    client_contracts: list[dict],
    migration_info: list[dict],
    accounts: AccountsBatchModel,
    personal_accounts: list[dict],
    lock: LockModel,
) -> PayoutReferences:
    return PayoutReferences(
        contracts=client_contracts,
        migration_info=migration_info,
        accounts=accounts,
        personal_accounts=personal_accounts,
        lock=lock,
    )


@pytest.fixture
def payment_event() -> PaymentEvent:
    return PaymentEvent(
        **gen_event(
            price=Decimal(120_000.00),
            aquiring_commission=Decimal(10.0),
            service_commission=Decimal(2.0)
        ),
        tariffer_payload={},
        payload={}
    )


@pytest.fixture
def payment_event_with_price_and_commission(request) -> PaymentEvent:
    price, aquiring_commission, service_commission = request.param
    return PaymentEvent(
        **gen_event(
            price=price,
            aquiring_commission=aquiring_commission,
            service_commission=service_commission,
        ),
        tariffer_payload={},
        payload={}
    )


@pytest.fixture
def refund_event() -> RefundEvent:
    return RefundEvent(**gen_event(
        price=Decimal(120_000.00),
        aquiring_commission=Decimal(10.0),
        service_commission=Decimal(2.0)
    ))


@pytest.fixture
def refund_event_with_price_and_commission(request) -> RefundEvent:
    price, aquiring_commission, service_commission = request.param
    return RefundEvent(**gen_event(
        price=price,
        aquiring_commission=aquiring_commission,
        service_commission=service_commission,
    ))


@pytest.fixture
def payout_event() -> PayoutEvent:
    return PayoutEvent(
        transaction_id=18319,
        event_time=to_msk_dt(datetime(2020, 1, 1)),
        client_id=1234,
        payload={},
        tariffer_payload={},
    )


@pytest.fixture
def method(
    request,
    payment_event: PaymentEvent,
    refund_event: RefundEvent,
    payout_event: PayoutEvent,
    cashless_references_with_migration_info: CashlessReferences,
    payout_references_with_migration_info: PayoutReferences,
) -> Union[PaymentMethod, RefundMethod, PayoutMethod]:
    if request.param == 'payment':
        return PaymentMethod(payment_event, cashless_references_with_migration_info)
    if request.param == 'refund':
        return RefundMethod(refund_event, cashless_references_with_migration_info)
    if request.param == 'payout':
        return PayoutMethod(payout_event, payout_references_with_migration_info)
