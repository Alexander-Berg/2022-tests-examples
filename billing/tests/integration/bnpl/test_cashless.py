from datetime import datetime

import hamcrest as hm
import pytest
from billing.library.python.calculator.values import PersonType

from billing.hot.tests.clients.accounts.client import Client as AccountsClient
from billing.hot.tests.clients.processor.bnpl_client import Client as BnplClient
from billing.hot.tests.lib.matchers.base import (
    success_accounts_read_batch_response, success_processor_response_entries,
)
from billing.hot.tests.lib.state import contract


@pytest.mark.parametrize('dry_run', [False, True])
@pytest.mark.asyncio
async def test_cashless_payment(
    accounts_client: AccountsClient,
    bnpl_client: BnplClient,
    create_state_builder,
    yandex_firm_id: int,
    dry_run,
):
    builder = create_state_builder()
    builder.with_person_type(person_type=PersonType.UR)
    builder.with_firm(firm_id=yandex_firm_id)
    builder.fill_contracts(
        contracts=[contract.BnplContract.generate()],
        namespace='bnpl',
        dry_run=dry_run,
        filter='Firm'
    )
    state = builder.built_state()

    async with bnpl_client.cashless_payment(
        state,
        transaction_amount=120_000,
        total_commission=100
    ) as response:
        hm.assert_that(response.status, hm.equal_to(200))
        hm.assert_that(await response.json(), success_processor_response_entries({
            'event': {
                'tariffer_payload': {
                    'dry_run': dry_run,
                    'amount_wo_vat': '83.333333',
                },
            }
        }))

    async with accounts_client.read_balances(
        state,
        int(datetime.now().timestamp()),
        ['cashless', 'commissions'],
        'bnpl',
    ) as response:
        hm.assert_that(response.status, hm.equal_to(200))
        hm.assert_that(await response.json(), success_accounts_read_batch_response({
            'balances': [
                {
                    'loc': {
                        'type': 'cashless',
                    },
                    'credit': '{:.6f}'.format(120_000),
                },
                {
                    'loc': {
                        'type': 'commissions',
                    },
                    'debit': '{:.6f}'.format(100)
                },
            ]
        }))


@pytest.mark.parametrize('dry_run', [False, True])
@pytest.mark.asyncio
async def test_cashless_refund(
    accounts_client: AccountsClient,
    bnpl_client: BnplClient,
    create_state_builder,
    dry_run,
):
    builder = create_state_builder()
    builder.with_person_type(person_type=PersonType.UR)
    builder.with_firm(firm_id=1)
    builder.fill_contracts(
        contracts=[contract.BnplContract.generate()],
        namespace='bnpl',
        dry_run=dry_run,
        filter='Firm'
    )
    state = builder.built_state()

    async with bnpl_client.cashless_refund(
        state,
        transaction_amount=10_000,
        total_commission=100,
    ) as response:
        hm.assert_that(response.status, hm.equal_to(200))
        hm.assert_that(await response.json(), success_processor_response_entries({
            'event': {
                'tariffer_payload': {
                    'dry_run': dry_run,
                    'amount_wo_vat': '83.333333',
                },
            }
        }))

    async with accounts_client.read_balances(
        state,
        int(datetime.now().timestamp()),
        ['cashless_refunds', 'commission_refunds'],
        'bnpl',
    ) as response:
        hm.assert_that(response.status, hm.equal_to(200))
        hm.assert_that(await response.json(), success_accounts_read_batch_response({
            'balances': [
                {
                    'loc': {
                        'type': 'cashless_refunds',
                    },
                    'debit': '{:.6f}'.format(10_000),
                },
                {
                    'loc': {
                        'type': 'commission_refunds',
                    },
                    'credit': '{:.6f}'.format(100),
                },
            ]
        }))
