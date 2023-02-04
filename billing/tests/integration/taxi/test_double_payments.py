import datetime

import allure
import pytest
import hamcrest as hm
import json
from billing.hot.tests.lib.date import timestamp
from billing.hot.tests.lib.rand import rand
from billing.hot.tests.lib.state import state


async def make_default_payout(taxi_client, st):
    with allure.step('Отправляем событие в тарификатор: cashless'):
        async with taxi_client.cashless(
            st,
            extended_params={
                "amount": 400,
            },
        ) as response:
            hm.assert_that(response.status, hm.equal_to(200))

    with allure.step('Отправляем событие в тарификатор: revenue'):
        async with taxi_client.revenue(
            st,
            extended_params={
                "amount": 50,
            },
        ) as response:
            hm.assert_that(response.status, hm.equal_to(200))

    with allure.step('Отправляем событие в тарификатор: payout'):
        async with taxi_client.payout(st) as response:
            hm.assert_that(response.status, hm.equal_to(200))


@pytest.mark.asyncio
async def test_double_payments(
    yt,
    create_state_builder,
    payout_client,
    taxi_oebs_pipeline,
    taxi_client,
    accounts_client,
):
    """
    Проверяем, что признак для двух подряд выплат по одному клиенту успешной будет только одна
    """
    st = state.PipelineState.generate()
    builder = create_state_builder(st)
    builder.fill_contracts(dry_run=True)
    st = builder.built_state()

    await make_default_payout(taxi_client, st)

    with allure.step('payout_by_client — 1'):
        now_dt_ms = timestamp.now_dt_ms()
        now = datetime.datetime.now()
        async with payout_client.payout_by_client(st) as response:
            hm.assert_that(response.status, hm.equal_to(201))

    with allure.step('payout_by_client — 2'):
        st.external_id = rand.uuid()
        async with payout_client.payout_by_client(st) as response:
            hm.assert_that(response.status, hm.equal_to(201))

    with allure.step('poll_payout_info'):
        data = await payout_client.poll_payout_info(
            st,
            from_date=now - datetime.timedelta(days=1),
            statuses=[],
            expected_records=2,
            timeout_seconds=10,
        )
        allure.attach(json.dumps(data), 'Ответ poll_payout_info', allure.constants.AttachmentType.JSON)

        assert len(data) == 2
        assert {data[0]["status"], data[1]["status"]} == {"pending", "rejected"}

    with allure.step('taxi_oebs_pipeline.run_ok_pipeline'):
        await taxi_oebs_pipeline.run_ok_pipeline(
            st, payout_client, expected_payouts_count=1, from_dt_ms=now_dt_ms
        )
