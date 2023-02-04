import datetime
import logging

import hamcrest as hm
import pytest

from billing.hot.tests.lib.date import timestamp
from billing.hot.tests.lib.state import state
from billing.hot.tests.lib.testgen.actions import (
    ProcessAction,
    Account,
    CheckAccountAction,
)
from billing.hot.tests.lib.testgen.runner import ActionGroup
from billing.hot.tests.lib.testgen.runner import TestRunner

logging.disable(logging.DEBUG)


@pytest.mark.asyncio
@pytest.mark.parametrize("scenario_runner", ["taxi"], indirect=True)
async def test_base_scenarios(scenario_runner: TestRunner):
    scenario_runner.add_action_groups(
        ActionGroup(
            [
                ProcessAction("cashless", 100),
                ProcessAction("cashless", 200),
                ProcessAction("cashless_refunds", 50),
                ProcessAction("commissions", 75),
                ProcessAction("commissions", 15),
                ProcessAction("commissions_refunds", 10),
                ProcessAction("promocodes", 50),
                ProcessAction("promocodes_refunds", 10),
                ProcessAction("logistics", 30),
                ProcessAction("logistics_refunds", 10),
                ProcessAction("fuel_hold", 40),
                ProcessAction("fuel_hold_refunds", 20),
                ProcessAction("subvention", 120),
                ProcessAction("subvention_refunds", 40),
            ]
        ),
        ActionGroup(
            [
                ProcessAction("payout"),
            ]
        ),
        ActionGroup(
            [
                CheckAccountAction(
                    [
                        Account("cashless", 340, 40),  # terminal_id 0
                        Account("cashless", 0, 300),  # terminal_id != 0
                        Account("cashless_refunds", 50, 0),  # terminal_id != 0
                        Account("cashless_refunds", 0, 50),  # terminal_id 0
                        Account("commissions", 80, 80),  # terminal_id != 0
                        Account("commissions_with_vat", 96, 96),  # terminal_id != 0
                        Account("logistics", 30, 30),  # terminal_id 0
                        Account("logistics_refunds", 10, 10),  # terminal_id 0
                        Account("subventions", 120, 120),
                        Account("subventions_refunds", 40, 40),
                        Account("fuel_hold", 40, 20),
                        Account("promocodes", 50, 50),
                        Account("promocodes_refunds", 10, 10),
                        Account("payout", 0, 80),  # for subventions contract
                        Account("payout", 0, 234),  # for everything else
                        Account("payout", 0, 20),  # for logistics
                    ]
                )
            ]
        ),
    )

    async for group_result in scenario_runner.execute():
        assert not any(group_result), [str(value) for value in group_result if value]


@pytest.mark.asyncio
async def test_process(
    accounts_db, taxi_client, create_state_builder, taxi_oebs_pipeline, payout_client
):
    st = state.PipelineState.generate()
    builder = create_state_builder(st)
    builder.fill_contracts()
    st = builder.built_state()

    async with taxi_client.cashless(
        st,
        extended_params={
            "amount": 100,
        },
    ) as response:
        hm.assert_that(response.status, hm.equal_to(200))

    async with taxi_client.revenue(
        st,
        extended_params={
            "amount": 50,
        },
    ) as response:
        hm.assert_that(response.status, hm.equal_to(200))

    async with taxi_client.payout(st) as response:
        hm.assert_that(response.status, hm.equal_to(200))

    # this doesn't guarantee that payout-by-client will see payout event since we check master only :(
    rows = await accounts_db.get_event_batches(st.transaction_ids)
    hm.assert_that(rows, hm.has_length(3))

    now_dt_ms = timestamp.now_dt_ms()
    now = datetime.datetime.now()
    async with payout_client.payout_by_client(st) as response:
        hm.assert_that(response.status, hm.equal_to(201))

    await payout_client.poll_payout_info(
        st,
        from_date=now - datetime.timedelta(days=1),
        statuses=["pending"],
        expected_records=1,
    )

    await taxi_oebs_pipeline.run_ok_pipeline(
        st, payout_client, expected_payouts_count=1, from_dt_ms=now_dt_ms
    )

    record = await payout_client.poll_payout_info(
        st,
        from_date=now - datetime.timedelta(days=1),
        statuses=["done"],
        expected_records=1,
    )
    # 100 - 50 * 1.2 (vat)
    hm.assert_that(record[0]["amount"], hm.equal_to("40"))
