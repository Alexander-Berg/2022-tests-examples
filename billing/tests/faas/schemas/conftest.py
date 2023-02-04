from datetime import datetime
from decimal import Decimal

import pytest

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
            'loan_commission': 'd15388b7-8afe-4af0-a8cb-82feb83945fd',
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
                        'namespace': 'bnpl',
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
                        'namespace': 'bnpl',
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
def payout_event() -> dict:
    return {
        'transaction_id': 'transaction_id',
        'event_time': now(),
        'client_id': 321,
        'payload': {},
    }


@pytest.fixture
def loaded_payout_event(payout_event: dict) -> dict:
    payout_event['event_time'] = datetime.now()

    return payout_event


@pytest.fixture
def payment_event() -> dict:
    return {
        "aquiring_commission": Decimal(2.00),
        "cancel_reason": None,
        "service_commission": Decimal(10.00),
        "transaction_amount": Decimal(120.00),
        "transaction_dt": now(),
        "transaction_id": "03b7452a-5969-4249-478c-dbcae3606459-approved",
        "transaction_type": "payment",
        "billing_client_id": 441424,
        "billing_contract_id": 3245907,
        "product_id": "loan_commission",
        "currency": "RUB"
    }


@pytest.fixture
def loaded_payment_event(payment_event: dict) -> dict:
    payment_event['transaction_dt'] = datetime.now()

    return payment_event


@pytest.fixture
def refund_event() -> dict:
    return {
        "aquiring_commission": Decimal(2.00),
        "cancel_reason": None,
        "service_commission": Decimal(10.00),
        "transaction_amount": Decimal(120.00),
        "transaction_dt": now(),
        "transaction_id": "03b7452a-5969-4249-478c-dbcae3606459-approved",
        "transaction_type": "refund",
        "billing_client_id": 441424,
        "billing_contract_id": 3245907,
        "product_id": "loan_commission",
        "currency": "RUB"
    }


@pytest.fixture
def cashless_transaction_batch(loaded_payment_event: dict) -> dict:
    return {
        'event': loaded_payment_event,
        'client_transactions': [
            {
                'client_id': loaded_payment_event['billing_client_id'],
                'transactions': [
                    {
                        'loc': {},
                        'amount': Decimal('10.22'),
                        'type': 'debit',
                        'dt': 132532324322,
                    }
                ]
            }
        ]
    }


@pytest.fixture
def payout_transaction_batch(loaded_payout_event: dict) -> dict:
    return {
        'event': loaded_payout_event,
        'client_transactions': [
            {
                'client_id': loaded_payout_event['client_id'],
                'transactions': [
                    {
                        'loc': {},
                        'amount': Decimal('1203.99'),
                        'type': 'credit',
                        'dt': 1673264289344,
                    }
                ]
            }
        ]
    }
