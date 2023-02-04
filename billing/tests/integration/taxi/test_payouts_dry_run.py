import datetime

import allure
import pytest
import hamcrest as hm
from billing.hot.tests.lib.state import state


@pytest.mark.asyncio
async def test_dry_run_in_payouts(
    taxi_client,
    accounts_client,
    yt,
    create_state_builder,
    payout_client,
    taxi_oebs_pipeline,
):
    """
    Проверяем, что признак dry_run появляется у выплаты и связанных с ней событий в системе счетов
    """
    st = state.PipelineState.generate()
    builder = create_state_builder(st)
    builder.fill_contracts(dry_run=True)
    st = builder.built_state()

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

    with allure.step('Создаем выплату'):
        now = datetime.datetime.now()
        async with payout_client.payout_by_client(st) as response:
            hm.assert_that(response.status, hm.equal_to(201))

    with allure.step('poll_payout_info'):
        data = await payout_client.poll_payout_info(
            st,
            from_date=now - datetime.timedelta(days=1),
            statuses=["done"],
            expected_records=1,
            timeout_seconds=120,
        )
        assert len(data) == 1
        payout = data[0]

    with allure.step('Проверяем, что у выплаты проставлен признак dry_run'):
        assert payout["dry_run"] is True

    with allure.step('get_account_detailed_turnover'):
        async with accounts_client.get_account_detailed_turnover(
            st,
            client_id=st.client_id,
            dt_from=datetime.datetime.now() - datetime.timedelta(hours=1),
            dt_to=datetime.datetime.now() + datetime.timedelta(hours=1),
            type_="payout_sent",
            add_params={'service_id': None, 'contract_id': None, 'currency': None},
        ) as resp:
            resp_json = await resp.json()

            assert resp_json["status"] == "ok", resp_json["status"]
            acc_data = resp_json["data"][0]
            assert len(acc_data["events"]) == 3

    events = set()
    for event in acc_data["events"]:
        assert event["info"]["tariffer_payload"]["dry_run"] is True, data
        events.add(event["event_type"])

    assert events == {"reserve for payout", "register payout", "confirm payout"}
