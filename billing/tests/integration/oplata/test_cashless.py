from datetime import datetime

import hamcrest as hm
import pytest
from billing.library.python.calculator.values import PersonType

from billing.hot.tests.clients.accounts.client import Client as AccountsClient
from billing.hot.tests.clients.processor.oplata_client import Client as OplataClient
from billing.hot.tests.lib.matchers.base import (
    success_accounts_read_batch_response, success_processor_response_entries,
)
from billing.hot.tests.lib.state import contract
from billing.hot.tests.lib.state import state


def _prepare(create_state_builder, firm_id: int, dry_run: bool) -> state.PipelineState:
    builder = create_state_builder()
    builder.with_person_type(person_type=PersonType.UR)
    builder.with_firm(firm_id=firm_id)
    builder.fill_contracts(
        contracts=[contract.OplataContract.generate()],
        namespace='oplata',
        dry_run=dry_run,
    )
    return builder.built_state()


@pytest.mark.parametrize('dry_run', [False, True])
@pytest.mark.asyncio
async def test_cashless_payment(
    accounts_client: AccountsClient,
    oplata_client: OplataClient,
    create_state_builder,
    yandex_firm_id: int,
    dry_run,
):
    built_state = _prepare(create_state_builder, yandex_firm_id, dry_run)

    async with oplata_client.cashless_payment(
        built_state,
        order_price=120_000
    ) as response:
        hm.assert_that(response.status, hm.equal_to(200))
        hm.assert_that(await response.json(), success_processor_response_entries({
            'event': {
                'tariffer_payload': {
                    'dry_run': dry_run,
                    'amount_wo_vat': '100.000000',
                },
            }
        }))

    async with accounts_client.read_balances(
        built_state,
        int(datetime.now().timestamp()),
        ['cashless', 'agent_rewards'],
        'oplata',
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
                        'type': 'agent_rewards',
                    },
                    'debit': '{:.6f}'.format(120)
                },
            ]
        }))


@pytest.mark.parametrize('dry_run', [False, True])
@pytest.mark.asyncio
async def test_cashless_payment_with_promocodes(
    accounts_client: AccountsClient,
    oplata_client: OplataClient,
    create_state_builder,
    yandex_firm_id: int,
    dry_run,
):
    built_state = _prepare(create_state_builder, yandex_firm_id, dry_run)

    async with oplata_client.cashless_payment(
        built_state,
        order_price=120_000,
        item_by_card=110_000,
        item_by_promocode=10_000,
    ) as response:
        hm.assert_that(response.status, hm.equal_to(200))
        hm.assert_that(await response.json(), success_processor_response_entries({
            'event': {
                'tariffer_payload': {
                    'dry_run': dry_run,
                    'amount_wo_vat': '100.000000',
                },
            }
        }))

    async with accounts_client.read_balances(
        built_state,
        int(datetime.now().timestamp()),
        ['cashless', 'agent_rewards', 'promocodes'],
        'oplata',
    ) as response:
        hm.assert_that(response.status, hm.equal_to(200))
        hm.assert_that(await response.json(), success_accounts_read_batch_response({
            'balances': [
                {
                    'loc': {
                        'type': 'cashless',
                    },
                    'credit': '{:.6f}'.format(110_000),
                },
                {
                    'loc': {
                        'type': 'agent_rewards',
                    },
                    'debit': '{:.6f}'.format(120)
                },
                {
                    'loc': {
                        'type': 'promocodes',
                    },
                    'credit': '{:.6f}'.format(10_000)
                }
            ]
        }))


@pytest.mark.parametrize('dry_run', [False, True])
@pytest.mark.asyncio
async def test_cashless_refund(
    accounts_client: AccountsClient,
    oplata_client: OplataClient,
    yandex_firm_id: int,
    create_state_builder,
    dry_run,
):
    built_state = _prepare(create_state_builder, yandex_firm_id, dry_run)

    async with oplata_client.cashless_refund(
        built_state,
        original_order_price=100_000,
        refund_price=10_000,
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
        built_state,
        int(datetime.now().timestamp()),
        ['cashless_refunds'],
        'oplata',
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
            ]
        }))


@pytest.mark.parametrize('dry_run', [False, True])
@pytest.mark.asyncio
async def test_cashless_refund_with_promocodes(
    accounts_client: AccountsClient,
    oplata_client: OplataClient,
    create_state_builder,
    yandex_firm_id: int,
    dry_run,
):
    built_state = _prepare(create_state_builder, yandex_firm_id, dry_run)

    async with oplata_client.cashless_refund(
        built_state,
        original_order_price=10_000,
        refund_price=10_000,
        item_by_card=9_001,
        item_by_promocode=999,
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
        built_state,
        int(datetime.now().timestamp()),
        ['cashless_refunds', 'promocodes_refunds'],
        'oplata',
    ) as response:
        hm.assert_that(response.status, hm.equal_to(200))
        hm.assert_that(await response.json(), success_accounts_read_batch_response({
            'balances': [
                {
                    'loc': {
                        'type': 'cashless_refunds',
                    },
                    'debit': '{:.6f}'.format(9_001),
                },
                {
                    'loc': {
                        'type': 'promocodes_refunds',
                    },
                    'debit': '{:.6f}'.format(999),
                },
            ]
        }))
