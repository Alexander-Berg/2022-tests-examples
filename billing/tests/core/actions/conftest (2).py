from datetime import datetime
from decimal import Decimal
from typing import Union

import arrow
import pytest
from billing.library.python.calculator.models.account import AccountsBatchModel
from billing.library.python.calculator.models.firm import FirmModel
from billing.library.python.calculator.services.account import OperationExpression
from billing.library.python.calculator.test_utils.builder import gen_general_contract, gen_firm
from billing.library.python.calculator.util import to_msk_dt
from billing.library.python.calculator.values import PersonType

from billing.hot.calculators.oplata.calculator.core.const import Service
from billing.hot.calculators.oplata.calculator.core.models.event import (
    PaymentEvent, RefundEvent, SubscriptionEvent,
    PayoutEvent, Merchant, Order,
    Transaction, Refund, CustomerSubscriptionTransaction,
)
from billing.hot.calculators.oplata.calculator.core.models.method import (
    CashlessReferences, PayoutReferences, PaymentMethod,
    RefundMethod, SubscriptionMethod, PayoutMethod,
)
from billing.hot.calculators.oplata.calculator.tests.builder import gen_order


@pytest.fixture
def migration_info(firm: FirmModel) -> list[dict]:
    return [
        {
            'namespace': 'oplata',
            'from_dt': to_msk_dt(datetime(2017, 11, 11)),
            'dry_run': True,
            'filter': 'Firm',
            'object_id': firm.id,
        }
    ]


@pytest.fixture
def contracts(
    order: Order,
    merchant: Merchant,
    firm: FirmModel,
) -> list[dict]:
    return [
        gen_general_contract(
            contract_id=merchant.contract_id,
            client_id=merchant.client_id,
            person_id=7,
            services=[Service.OPLATA],
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
            services=[Service.OPLATA],
            firm=firm.id,
            person_type=PersonType.UR,
        ),
        gen_general_contract(
            contract_id=2,
            client_id=payout_event.client_id,
            person_id=2,
            services=[Service.OPLATA],
            firm=firm.id,
            person_type=PersonType.YT,
        ),
        gen_general_contract(
            contract_id=3,
            client_id=payout_event.client_id,
            person_id=3,
            services=[Service.OPLATA],
            firm=firm.id,
            person_type=PersonType.UR,
        ),
        gen_general_contract(
            contract_id=4,
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
            '3': '6433b8d4-ae84-4b00-a15d-b44b09a1d77f',
            'default': 'e6a38e6d-f22d-4c05-b533-76031c2ed15c',
        }
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
            '3': '6433b8d4-ae84-4b00-a15d-b44b09a1d77f',
            'default': 'e6a38e6d-f22d-4c05-b533-76031c2ed15c',
        }
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
def accounts(payout_event: PayoutEvent, client_contracts: list[dict]) -> AccountsBatchModel:
    dt = arrow.get(payout_event.event_time).int_timestamp
    return AccountsBatchModel(balances=[
        {
            'loc': {
                'type': 'cashless',
                'namespace': 'oplata',
                'contract_id': client_contracts[0]['id'],
                'client_id': client_contracts[0]['client_id'],
                'currency': 'RUB',
            },
            'credit': Decimal('1800.00'),
            'debit': Decimal('1600.00'),
            'dt': dt,
        },
        {
            'loc': {
                'type': 'promocodes',
                'namespace': 'oplata',
                'contract_id': client_contracts[0]['id'],
                'client_id': client_contracts[0]['client_id'],
                'currency': 'RUB',
            },
            'credit': Decimal('300.00'),
            'debit': Decimal('0.00'),
            'dt': dt,
        },
        {
            'loc': {
                'type': 'cashless_refunds',
                'namespace': 'oplata',
                'contract_id': client_contracts[0]['id'],
                'client_id': client_contracts[0]['client_id'],
                'currency': 'RUB',
            },
            'credit': Decimal('100.00'),
            'debit': Decimal('400.00'),
            'dt': dt,
        },
        {
            'loc': {
                'type': 'promocodes_refunds',
                'namespace': 'oplata',
                'contract_id': client_contracts[0]['id'],
                'client_id': client_contracts[0]['client_id'],
                'currency': 'RUB',
            },
            'credit': Decimal('10.00'),
            'debit': Decimal('110.00'),
            'dt': dt,
        },
        {
            'loc': {
                'type': 'agent_rewards',
                'namespace': 'oplata',
                'contract_id': client_contracts[0]['id'],
                'client_id': client_contracts[0]['client_id'],
                'currency': 'RUB',
            },
            'credit': Decimal('100.00'),
            'debit': Decimal('120.00'),
            'dt': dt,
        }
    ])


@pytest.fixture
def operation_expressions() -> dict[int, list[OperationExpression]]:
    return {1: [
        OperationExpression(name='cashless_to_cashless_refunds', expressions=[
            {'amount': Decimal('200.00'), 'account': 'cashless', 'operation_type': 'DEBIT'},
            {'amount': Decimal('200.00'), 'account': 'cashless_refunds', 'operation_type': 'CREDIT'},
        ]),
        OperationExpression(name='cashless_to_promocodes_refunds', expressions=[]),
        OperationExpression(name='promocodes_to_cashless_refunds', expressions=[
            {'amount': Decimal('100.00'), 'account': 'promocodes', 'operation_type': 'DEBIT'},
            {'amount': Decimal('100.00'), 'account': 'cashless_refunds', 'operation_type': 'CREDIT'},
        ]),
        OperationExpression(name='promocodes_to_promocodes_refunds', expressions=[
            {'amount': Decimal('100.00'), 'account': 'promocodes', 'operation_type': 'DEBIT'},
            {'amount': Decimal('100.00'), 'account': 'promocodes_refunds', 'operation_type': 'CREDIT'},
        ]),
        OperationExpression(name='cashless_to_agent_rewards', expressions=[]),
        OperationExpression(name='promocodes_to_agent_rewards', expressions=[
            {'amount': Decimal('20.00'), 'account': 'promocodes', 'operation_type': 'DEBIT'},
            {'amount': Decimal('20.00'), 'account': 'agent_rewards', 'operation_type': 'CREDIT'},
        ]),
        OperationExpression(name='payout_cashless', expressions=[]),
        OperationExpression(name='payout_promocodes', expressions=[
            {'amount': Decimal('80.00'), 'account': 'promocodes', 'operation_type': 'DEBIT'},
            {'amount': Decimal('80.00'), 'account': 'payout', 'operation_type': 'CREDIT'}
        ])],
        2: [], 3: [],
    }


@pytest.fixture
def personal_accounts(client_contracts: list[dict]) -> list[dict]:
    return [
        {
            'id': 1,
            'contract_id': client_contracts[0]['id'],
            'client_id': client_contracts[0]['client_id'],
            'version': 2,
            'obj': {
                'id': 1,
                'contract_id': client_contracts[0]['id'],
                'external_id': '2134/7482',
                'iso_currency': 'RUB',
                'type': 'personal_account',
                'service_code': None,
                'hidden': 0,
                'postpay': 1,
            },
        }
    ]


@pytest.fixture
def payout_references_with_migration_info(
    client_contracts: list[dict],
    migration_info: list[dict],
    accounts: AccountsBatchModel,
    personal_accounts: list[dict],
) -> PayoutReferences:
    return PayoutReferences(
        contracts=client_contracts,
        migration_info=migration_info,
        accounts=accounts,
        personal_accounts=personal_accounts,
    )


@pytest.fixture
def merchant() -> Merchant:
    return Merchant(
        uid=123,
        revision=38213,
        client_id=313123,
        person_id='3432',
        contract_id=655452,
        submerchant_id='228',
        parent_uid=1234,
    )


@pytest.fixture
def order() -> Order:
    return Order(**gen_order(
        price=Decimal(120_000.00),
        commission=10,
        kind='pay',
        service_merchant_service_fee=10,
    ))


@pytest.fixture
def order_with_service_fee_3() -> Order:
    return Order(**gen_order(
        price=Decimal(120_000.00),
        commission=10,
        kind='pay',
        service_merchant_service_fee=3,
    ))


@pytest.fixture
def refund() -> Refund:
    return Refund(**gen_order(
        price=Decimal(1_000.00),
        commission=13,
        kind='refund',
        service_merchant_service_fee=2,
    ), trust_refund_id='trust refund id')


@pytest.fixture
def transaction() -> Transaction:
    return Transaction(
        tx_id=124,
        revision=1923,
        created=to_msk_dt(datetime.now()),
        updated=to_msk_dt(datetime.now()),
        trust_purchase_token='trust purchase token',
        trust_resp_code='trust resp code',
        trust_payment_id='trust payment id',
        trust_terminal_id=12,
    )


@pytest.fixture
def payment_event(
    merchant: Merchant,
    order: Order,
    transaction: Transaction,
) -> PaymentEvent:
    return PaymentEvent(
        merchant=merchant,
        order=order,
        transaction=transaction,
        payload={},
        tariffer_payload={},
    )


@pytest.fixture
def payment_event_with_price_and_commission(
    request,
    merchant: Merchant,
    transaction: Transaction,
) -> PaymentEvent:
    price, commission = request.param
    return PaymentEvent(
        merchant=merchant,
        order=Order(**gen_order(price=price, commission=commission)),
        transaction=transaction,
        payload={},
        tariffer_payload={},
    )


@pytest.fixture
def payment_event_with_price_commission_and_markup(
    request,
    merchant: Merchant,
    transaction: Transaction,
) -> PaymentEvent:
    price, commission, markup = request.param
    return PaymentEvent(
        merchant=merchant,
        order=Order(**gen_order(
            price=price,
            commission=commission,
            item_markup=markup,
        )),
        transaction=transaction,
        payload={},
        tariffer_payload={},
    )


@pytest.fixture
def payment_event_with_service_fee_3(
    merchant: Merchant,
    order_with_service_fee_3: Order,
    transaction: Transaction,
) -> PaymentEvent:
    return PaymentEvent(
        merchant=merchant,
        order=order_with_service_fee_3,
        transaction=transaction,
        payload={},
        tariffer_payload={},
    )


@pytest.fixture
def payment_event_with_service_fee_equals(
    request,
    payment_event: PaymentEvent,
    payment_event_with_service_fee_3: PaymentEvent,
) -> PaymentEvent:
    if request.param == 3:
        return payment_event_with_service_fee_3

    return payment_event


@pytest.fixture
def refund_event(
    merchant: Merchant,
    refund: Refund,
    order: Order,
    transaction: Transaction,
) -> RefundEvent:
    return RefundEvent(
        merchant=merchant,
        refund=refund,
        original_order=order,
        original_order_transaction=transaction,
        payload={},
        tariffer_payload={},
    )


@pytest.fixture
def refund_event_with_price_and_markup(
    request,
    merchant: Merchant,
    order: Order,
    transaction: Transaction,
) -> RefundEvent:
    price, markup = request.param

    return RefundEvent(
        merchant=merchant,
        refund=Refund(**gen_order(
            price=price,
            commission=0,
            item_markup=markup
        ), trust_refund_id='trust refund id'),
        original_order=order,
        original_order_transaction=transaction,
        payload={},
        tariffer_payload={},
    )


@pytest.fixture
def customer_subscription_transaction() -> CustomerSubscriptionTransaction:
    return CustomerSubscriptionTransaction(
        created=to_msk_dt(datetime(2020, 9, 9)),
        updated=to_msk_dt(datetime(2020, 9, 10)),
        trust_purchase_token='trust purchase token',
        trust_terminal_id=999,
    )


@pytest.fixture
def subscription_event(
    merchant: Merchant,
    order: Order,
    customer_subscription_transaction: CustomerSubscriptionTransaction,
) -> SubscriptionEvent:
    return SubscriptionEvent(
        merchant=merchant,
        order=order,
        customer_subscription_transaction=customer_subscription_transaction,
        payload={},
        tariffer_payload={},
    )


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
    subscription_event: SubscriptionEvent,
    payout_event: PayoutEvent,
    cashless_references_with_migration_info: CashlessReferences,
    payout_references_with_migration_info: PayoutReferences,
) -> Union[PaymentMethod, RefundMethod, SubscriptionMethod, PayoutMethod]:
    if request.param == 'payment':
        return PaymentMethod(payment_event, cashless_references_with_migration_info)
    if request.param == 'refund':
        return RefundMethod(refund_event, cashless_references_with_migration_info)
    if request.param == 'subscription':
        return SubscriptionMethod(subscription_event, cashless_references_with_migration_info)
    if request.param == 'payout':
        return PayoutMethod(payout_event, payout_references_with_migration_info)
