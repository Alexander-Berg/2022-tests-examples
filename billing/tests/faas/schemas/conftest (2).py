from datetime import datetime
from decimal import Decimal

import pytest
from billing.library.python.calculator.models.transaction import (
    ClientTransactionBatchModel, TransactionModel,
)

from billing.hot.calculators.oplata.calculator.core.models.event import PaymentEvent, PayoutEvent
from billing.hot.calculators.oplata.calculator.core.models.transaction import (
    CashlessTransactionBatch, PayoutTransactionBatch,
)
from billing.hot.calculators.oplata.calculator.core.models.value import AcquirerType, OrderKind, NDS

_DATETIME_FMT = '%Y-%m-%dT%H:%M:%SZ'


def now() -> str:
    return datetime.now().strftime(_DATETIME_FMT)


@pytest.fixture
def cashless_references() -> dict:
    return {
        'contracts': [],
        'firm': {
            'id': 12,
            'mdh_id': 'mdh_id',
            'title': 'Some firm',
            'legal_address': 'Some legal address',
            'inn': '1234567891',
            'kpp': '123456789',
            'email': 'some@email.ru',
            'mnclose_email': 'mnclose@email.ru',
            'payment_invoice_email': 'payment_invoice_email@email.ru',
            'phone': '+79087813132',
            'region_id': 31,
            'currency_rate_src': 'currency_rate_src',
            'default_iso_currency': 'default_iso_currency',
            'pa_prefix': 'pa_prefix',
            'contract_id': 3,
            'unilateral': 112,
            'postpay': 1345,
            'alter_permition_code': 'alter_permition_code',
            'test_env': 1,
            'tax_policies': [
                {
                    'id': 1,
                    'name': 'tax policy',
                    'hidden': 1,
                    'resident': 1,
                    'region_id': 31,
                    'default_tax': 1,
                    'mdh_id': 'mdh_id',
                    'spendable_nds_id': 40,
                    'percents': [
                        {
                            'id': 1,
                            'dt': now(),
                            'nds_pct': Decimal(13.0),
                            'nsp_pct': Decimal(10.0),
                            'hidden': 1,
                            'mdh_id': 'mdh_id',
                        },
                    ]
                }
            ],
            'person_categories': [
                {
                    'category': 'category',
                    'is_resident': 1,
                    'is_legal': 1,
                },
            ],
        },
        'migration_info': [
            {
                'namespace': 'namespace',
                'filter': 'Firm',
                'object_id': 12,
                'from_dt': now(),
                'dry_run': True,
            }
        ],
        'products': {
            '3': '6433b8d4-ae84-4b00-a15d-b44b09a1d77f',
            'default': 'e6a38e6d-f22d-4c05-b533-76031c2ed15c',
        },
    }


@pytest.fixture
def payout_references() -> dict:
    return {
        'contracts': [],
        'migration_info': [
            {
                'namespace': 'namespace',
                'filter': 'Firm',
                'object_id': 12,
                'from_dt': now(),
                'dry_run': True,
            }
        ],
        'accounts': {
            'balances': [
                {
                    'loc': {
                        'type': 'cashless',
                        'namespace': 'oplata',
                        'contract_id': 1,
                        'client_id': 123,
                        'currency': 'RUB',
                    },
                    'debit': 123.31,
                    'credit': 234.42,
                    'dt': 13433241414,
                },
                {
                    'loc': {
                        'type': 'cashless_refunds',
                        'namespace': 'oplata',
                        'contract_id': 2,
                        'client_id': 123,
                        'currency': 'RUB',
                    },
                    'debit': 414.23,
                    'credit': 12.31,
                    'dt': 13433241414,
                },
            ]
        },
        'personal_accounts': [
            {
                'id': 1,
                'contract_id': 1,
                'client_id': 123,
                'version': 3,
                'obj': {
                    'id': 1,
                    'contract_id': 1,
                    'external_id': '2134/7482',
                    'iso_currency': 'RUB',
                    'type': 'personal_account',
                    'service_code': None,
                    'hidden': 1,
                    'postpay': 1,
                },
            },
        ],
    }


@pytest.fixture
def merchant() -> dict:
    return {
        'uid': 149848,
        'revision': 7484,
        'client_id': 425525,
        'person_id': '2123',
        'contract_id': 64243,
        'submerchant_id': '228',
        'parent_uid': 4314,
    }


@pytest.fixture
def order() -> dict:
    return {
        'uid': 14,
        'shop_id': 333,
        'order_id': 123455,
        'parent_order_id': 14313,
        'original_order_id': 2443,
        'revision': 35435,
        'acquirer': 'tinkoff',
        'commission': 9,
        'kind': 'pay',
        'autoclear': True,
        'closed': now(),
        'created': now(),
        'updated': now(),
        'held_at': now(),
        'pay_status_updated_at': now(),
        'caption': 'caption',
        'description': 'description',
        'customer_uid': 1443,
        'price': Decimal(134.00),
        'currency': '$',
        'items': [
            {
                'name': 'item name',
                'currency': '$',
                'product_id': 1433,
                'nds': 'nds_20_120',
                'amount': Decimal(120.00),
                'total_price': Decimal(122.00),
                'payment_method': 'card',
                'trust_order_id': '4021932604.18.1._',
                'prices': Decimal(120.0),
                'markup': {
                    'card': '100.00',
                    'virtual::new_promocode': '22.00',
                },
            },
        ],
        'service_client_id': 87483,
        'service_merchant': {
            'service_merchant_id': 3894,
            'service_id': 4235,
            'service': {
                'service_fee': 3,
            }
        }
    }


@pytest.fixture
def refund(order: dict) -> dict:
    order['trust_refund_id'] = 'trust refund id'
    return order


@pytest.fixture
def transaction() -> dict:
    return {
        'tx_id': 3143,
        'revision': 8983,
        'created': now(),
        'updated': now(),
        'trust_purchase_token': 'trust purchase token',
        'trust_resp_code': 'trust resp code',
        'trust_payment_id': 'trust payment id',
        'trust_terminal_id': 564,
    }


@pytest.fixture
def customer_subscription_transaction() -> dict:
    return {
        'created': now(),
        'updated': now(),
        'trust_purchase_token': 'trust purchase token',
        'trust_terminal_id': 3445,
    }


@pytest.fixture
def payout_event() -> dict:
    return {
        'transaction_id': 'transaction_id',
        'event_time': now(),
        'client_id': 321,
        'payload': {},
    }


@pytest.fixture
def payment_event(merchant: dict, order: dict, transaction: dict) -> dict:
    return {
        'type': 'payment',
        'merchant': merchant,
        'order': order,
        'transaction': transaction
    }


@pytest.fixture
def refund_event(merchant: dict, order: dict, transaction: dict, refund: dict) -> dict:
    return {
        'type': 'refund',
        'merchant': merchant,
        'refund': refund,
        'original_order': order,
        'original_order_transaction': transaction,
    }


@pytest.fixture
def subscription_event(merchant: dict, order: dict, customer_subscription_transaction: dict) -> dict:
    return {
        'type': 'subscription',
        'merchant': merchant,
        'order': order,
        'customer_subscription_transaction': customer_subscription_transaction,
    }


@pytest.fixture
def loaded_payment_event(payment_event: dict) -> dict:
    payment_event['transaction'].update({
        'updated': datetime.now(),
        'created': datetime.now(),
    })
    payment_event['order'].update({
        'updated': datetime.now(),
        'held_at': datetime.now(),
        'created': datetime.now(),
        'closed': datetime.now(),
        'pay_status_updated_at': datetime.now(),
        'acquirer': AcquirerType.TINKOFF,
        'kind': OrderKind.PAY,
    })
    for item in payment_event['order']['items']:
        item['nds'] = NDS.NDS_20_120
        item['markup'].update({
            'by_card': item['markup']['card'],
            'by_promocode': item['markup']['virtual::new_promocode'],
        })
        del item['markup']['card']
        del item['markup']['virtual::new_promocode']

    return payment_event


@pytest.fixture
def loaded_payout_event(payout_event: dict) -> dict:
    payout_event['event_time'] = datetime.now()

    return payout_event


@pytest.fixture
def cashless_transaction_batch(loaded_payment_event: dict) -> CashlessTransactionBatch:
    return CashlessTransactionBatch(
        event=PaymentEvent(**loaded_payment_event),
        client_transactions=[
            ClientTransactionBatchModel(
                client_id=loaded_payment_event['merchant']['client_id'],
                transactions=[
                    TransactionModel(
                        loc={},
                        amount=Decimal('10.22'),
                        type='debit',
                        dt=132532324322,
                    ),
                ],
            ),
        ],
    )


@pytest.fixture
def payout_transaction_batch(loaded_payout_event: dict) -> PayoutTransactionBatch:
    return PayoutTransactionBatch(
        event=PayoutEvent(**loaded_payout_event),
        client_transactions=[
            ClientTransactionBatchModel(
                client_id=loaded_payout_event['client_id'],
                transactions=[
                    TransactionModel(
                        loc={},
                        amount=Decimal('1230.12'),
                        type='credit',
                        dt=132532324322,
                    ),
                ],
            ),
        ],
        states=[],
    )
