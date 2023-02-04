import datetime

import allure
import pytest
import hamcrest as hm
from billing.hot.tests.lib.state import state
from billing.hot.tests.lib.date import timestamp


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
async def test_multiple_payouts(
    yt,
    create_state_builder,
    payout_client,
    taxi_oebs_pipeline,
    taxi_client,
    accounts_client,
):
    """
    Проверяем, что признак dry_run появляется у выплаты и связанных с ней событий в системе счетов
    """
    st = state.PipelineState.generate()
    dry_run = create_state_builder(st)
    dry_run.fill_contracts(dry_run=True)
    dry_run_state = dry_run.built_state()

    await make_default_payout(taxi_client, dry_run_state)

    normal = create_state_builder()
    normal.fill_contracts(dry_run=False)
    normal_state = normal.built_state()

    await make_default_payout(taxi_client, normal_state)

    with allure.step('Создаем выплаты'):
        now_dt_ms = timestamp.now_dt_ms()
        now = datetime.datetime.now()
        async with payout_client.payout_by_client(dry_run_state) as response:
            hm.assert_that(response.status, hm.equal_to(201))

        async with payout_client.payout_by_client(normal_state) as response:
            hm.assert_that(response.status, hm.equal_to(201))

    with allure.step('poll_payout_info'):
        data = await payout_client.poll_payout_info(
            st,
            from_date=now - datetime.timedelta(days=1),
            statuses=["done"],
            expected_records=1,
        )
        assert len(data) == 1
        payout = data[0]

        with allure.step('Проверяем, что у выплаты проставлен признак dry_run'):
            assert payout["dry_run"] is True

        with allure.step('Проверяем, что обычная выплата не подтверждена'):
            await payout_client.poll_payout_info(
                st,
                from_date=now - datetime.timedelta(days=1),
                statuses=["pending"],
                expected_records=1,
            )

    await taxi_oebs_pipeline.run_ok_pipeline(
        st, payout_client, expected_payouts_count=1, from_dt_ms=now_dt_ms
    )
    await payout_client.poll_payout_info(
        st,
        from_date=now - datetime.timedelta(days=1),
        statuses=["done"],
        expected_records=1,
    )
