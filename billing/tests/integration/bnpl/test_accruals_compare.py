import asyncio

import copy
import hamcrest as hm
import pytest
from billing.library.python.calculator.values import PersonType

from billing.hot.tests.clients.accounts.client import Client as AccountsClient
from billing.hot.tests.clients.processor.bnpl_client import Client as BnplClient
from billing.hot.tests.lib.rand import rand
from billing.hot.tests.lib.state import contract


async def send_one(bnpl_client, state, order):
    state = copy.copy(state)
    state.order_uid = order
    async with bnpl_client.cashless_payment(
        state,
        transaction_amount=60_000,
        total_commission=100
    ) as response:
        hm.assert_that(response.status, hm.equal_to(200))


async def check_one(yt, order):
    transaction_id = f'transaction_id-{order}-payment'
    query = f"* FROM [{yt.tables['accruals_common_dry_run']}] " \
            f"WHERE event_external_id = \"{transaction_id}\""
    output = yt.select_rows_dynamic(query)
    offsets = [str(row['_offset']) for row in output.rows]
    assert len(offsets) == 1, transaction_id


@pytest.mark.asyncio
async def test_compare_accruals(
    accounts_client: AccountsClient,
    bnpl_client: BnplClient,
    create_state_builder,
    yandex_firm_id: int,
    yt_lib_client,
    test_on_recipes,
):
    """
    Проверяем, что все отправленные события доехали до выгрузки Начислений в YT в полном объеме и без дублей.
    """
    n = 256

    builder = create_state_builder()
    builder.with_person_type(person_type=PersonType.UR)
    builder.with_firm(firm_id=yandex_firm_id)
    builder.fill_contracts(
        contracts=[contract.BnplContract.generate()],
        namespace='bnpl',
        dry_run=True,
        filter='Firm'
    )
    state = builder.built_state()

    order_uids = [rand.int64() for _ in range(n)]
    await asyncio.gather(*[send_one(bnpl_client, state, order) for order in order_uids])

    if not test_on_recipes:
        # ждем пока события проедут через выгрузки системы счетов, начислятор и реплицируются в дин.таблицу
        wait_interval = 60
        await asyncio.sleep(wait_interval)

        await asyncio.gather(*[check_one(yt_lib_client, order) for order in order_uids])
