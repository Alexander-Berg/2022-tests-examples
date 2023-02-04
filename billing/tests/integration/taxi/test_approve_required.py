import datetime

import pytest
import hamcrest as hm
from billing.hot.tests.lib.state import state
from billing.hot.tests.lib.date import timestamp


@pytest.skip("TODO: skip reason", allow_module_level=True)
@pytest.mark.asyncio
async def test_approve_required(
    payout_db,
    yt,
    create_state_builder,
    payout_client,
    taxi_oebs_pipeline,
    taxi_client,
    accounts_client,
):
    """
    Проверяем корректность работы механизма ручного подтверждения платежей
    """
    st = state.PipelineState.generate()
    builder = create_state_builder(st)
    builder.fill_contracts()
    st = builder.built_state()

    # отправляем событие в тарификатор
    async with taxi_client.cashless(
        st,
        extended_params={
            "amount": 400,
        },
    ) as response:
        hm.assert_that(response.status, hm.equal_to(200))

    async with taxi_client.payout(st) as response:
        hm.assert_that(response.status, hm.equal_to(200))

    # Создаем выплату
    now_dt_ms = timestamp.now_dt_ms()
    now = datetime.datetime.now()
    async with payout_client.payout_by_client(st) as response:
        hm.assert_that(response.status, hm.equal_to(201))

    await payout_client.poll_payout_info(
        st,
        from_date=now - datetime.timedelta(days=1),
        statuses=["pending"],
        expected_records=0,
    )

    data = await payout_client.poll_payout_info(
        st,
        from_date=now - datetime.timedelta(days=1),
        statuses=["new"],
        expected_records=1,
    )

    payout = data[0]

    # проверяем, что у выплаты проставлен признак approve_req
    assert payout["approve_req"] is True

    await payout_db.execute(
        f"update payout.t_payout set approve_req = false where client_id = {st.client_id} returning approve_req"
    )

    data = await payout_client.poll_payout_info(
        st,
        from_date=now - datetime.timedelta(days=1),
        statuses=["pending"],
        expected_records=1,
    )
    payout = data[0]

    # проверяем, что у выплаты проставлен признак approve_req
    assert payout["approve_req"] is False

    await taxi_oebs_pipeline.run_ok_pipeline(
        st, payout_client, expected_payouts_count=1, from_dt_ms=now_dt_ms
    )

    await payout_client.poll_payout_info(
        st,
        from_date=now - datetime.timedelta(days=1),
        statuses=["done"],
        expected_records=1,
    )
