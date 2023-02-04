from datetime import datetime
from decimal import Decimal

import pytest
from billing.library.python.calculator.models.transaction import (
    ClientTransactionBatchModel, TransactionModel
)

from billing.hot.calculators.taxi_light.calculator.core.models import (
    PayoutEvent, PayoutTransactionBatch, OperationType
)

_DATETIME_FMT = '%Y-%m-%dT%H:%M:%SZ'


def now() -> str:
    return datetime.now().strftime(_DATETIME_FMT)


@pytest.fixture
def payout_references() -> dict:
    return {
        'contracts': [],
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
def payout_event() -> dict:
    return {
        'id': 120,
        'client_id': 314242,
        'contract_id': "65345",
        'service_id': None,
        'invoice_external_id': "2134/7482",
        'operation_type': OperationType.INSERT_NETTING,
        'amount': 123.4,
        'transaction_dt': "2021-12-30T20:59:53+00:00",
        'dry_run': True,
    }


@pytest.fixture
def loaded_payout_event(payout_event: dict) -> dict:
    payout_event['transaction_dt'] = datetime.now()
    return payout_event


@pytest.fixture
def payout_transaction_batch(loaded_payout_event: dict) -> PayoutTransactionBatch:
    return PayoutTransactionBatch(
        event=PayoutEvent(**loaded_payout_event),
        client_transactions=[
            ClientTransactionBatchModel(
                client_id=loaded_payout_event['client_id'],
                transactions=[
                    TransactionModel(loc={}, amount=Decimal('1230.12'), type='credit', dt=132532324322),
                ],
            ),
        ],
        states=[],
    )
