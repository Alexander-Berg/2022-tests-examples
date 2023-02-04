from datetime import datetime
from decimal import Decimal

import pytest
from billing.library.python.calculator.test_utils.builder import gen_general_contract
from billing.library.python.calculator.util import to_msk_dt
from billing.library.python.calculator.values import PersonType

from billing.hot.calculators.taxi_light.calculator.core.models import (
    PayoutEvent, PayoutReferences, PayoutMethod, OperationType
)


@pytest.fixture
def client_contracts(payout_event: PayoutEvent):
    return [
        gen_general_contract(
            client_id=payout_event.client_id,
            contract_id=1,
            firm=1,
            person_id=1,
            person_type=PersonType.UR,
            services=[],
        ),
    ]


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
def payout_references(
    client_contracts: list[dict],
    personal_accounts: list[dict],
) -> PayoutReferences:
    return PayoutReferences(
        contracts=client_contracts,
        personal_accounts=personal_accounts,
    )


@pytest.fixture
def payout_event() -> PayoutEvent:
    return PayoutEvent(
        id="cpf_id",
        client_id=314242,
        contract_id="65345",
        service_id=None,
        invoice_external_id="2134/7482",
        operation_type=OperationType.INSERT_NETTING,
        amount=Decimal("123.4"),
        transaction_dt=to_msk_dt(datetime(2021, 12, 30)),
        dry_run=True,
        tariffer_payload={},
    )


@pytest.fixture
def method(request, payout_event: PayoutEvent, payout_references: PayoutReferences) -> PayoutMethod:
    return PayoutMethod(payout_event, payout_references)
