import typing as tp
from datetime import datetime

import hamcrest as hm
import pytest
from billing.library.python.calculator.values import PersonType

from billing.hot.tests.clients.accounts.client import Client as AccountsClient
from billing.hot.tests.clients.processor.bnpl_income_client import Client as BnplIncomeCalculator
from billing.hot.tests.lib.matchers.base import (
    success_accounts_read_batch_response, success_processor_response_entries,
)
from billing.hot.tests.lib.state import contract

testcases = [
    pytest.param(
        89_900,
        'payment',
        ['commissions'],
        [
            {
                'loc': {
                    'type': 'commissions',
                },
                'debit': '{:.6f}'.format(89_900)
            },
        ],
    ),
    pytest.param(
        12_001,
        'refund',
        ['commission_refunds'],
        [
            {
                'loc': {
                    'type': 'commission_refunds',
                },
                'credit': '{:.6f}'.format(12_001)
            }
        ]
    )
]


@pytest.mark.parametrize('dry_run', [False, True])
@pytest.mark.parametrize(
    'transaction_amount, transaction_type, accounts, expected_balances',
    testcases,
)
@pytest.mark.asyncio
async def test_commission_payment(
    accounts_client: AccountsClient,
    bnpl_income_client: BnplIncomeCalculator,
    create_state_builder,
    yandex_firm_id: int,
    dry_run: bool,
    transaction_amount: tp.Any,
    transaction_type: str,
    accounts: tp.List[str],
    expected_balances: tp.List[tp.Dict]
):
    builder = create_state_builder()
    builder.with_person_type(person_type=PersonType.UR)
    builder.with_firm(firm_id=yandex_firm_id)
    builder.fill_contracts(
        contracts=[contract.BnplIncomeContract.generate()],
        namespace='bnpl_income',
        dry_run=dry_run,
        filter='Firm'
    )
    state = builder.built_state()

    async with bnpl_income_client.commission_payment(
        state,
        transaction_type=transaction_type,
        transaction_amount=transaction_amount
    ) as response:
        hm.assert_that(response.status, hm.equal_to(200))
        hm.assert_that(await response.json(), success_processor_response_entries({
            'event': {
                'tariffer_payload': {
                    'dry_run': dry_run,
                },
            }
        }))

    async with accounts_client.read_balances(
        state,
        int(datetime.now().timestamp()),
        accounts,
        'bnpl_income',
    ) as response:
        hm.assert_that(response.status, hm.equal_to(200))
        hm.assert_that(await response.json(), success_accounts_read_batch_response({
            'balances': expected_balances,
        }))
