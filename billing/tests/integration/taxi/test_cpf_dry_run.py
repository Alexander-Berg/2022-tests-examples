import datetime

import allure
import pytest
import hamcrest as hm
from billing.hot.tests.lib.state import state


@pytest.mark.asyncio
async def test_dry_run_in_cpf(
    taxi_client,
    yt,
    create_state_builder,
    payout_client,
    taxi_oebs_pipeline,
):
    st = state.PipelineState.generate()
    builder = create_state_builder(st)
    builder.fill_contracts(dry_run=True)
    st = builder.built_state()

    with allure.step('Отправляем событие в тарификатор: fuel_hold'):
        async with taxi_client.fuel_hold(st, extended_params={"amount": 200}) as response:
            hm.assert_that(response.status, hm.equal_to(200))

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
            timeout_seconds=10,
        )
        assert len(data) == 1
        payout_id = data[0]["id"]

    with allure.step('Забираем все CPF по созданной выплате'):
        tp_cpf_table = yt.tables["payout_cpf"] + datetime.datetime.now().strftime("%Y%m%d")
        query_cpf_table = (
            f"id FROM [{tp_cpf_table}] " f"WHERE payout_id = {payout_id} and dry_run = true"
        )
        output = yt.select_rows_dynamic(query_cpf_table)
        cpf_ids = [str(row["id"]) for row in output.rows]
        assert len(cpf_ids) > 0

    with allure.step('Проверяем, что все они попали в cpf_dry_run из ручки, мокающей баланс'):
        t_cpf_dry_run_table = yt.tables["payout_cpf_dry_run"]
        query_cpf_dry_run_table = (
            f"cpf_id FROM [{t_cpf_dry_run_table}] "
            f"WHERE cpf_id IN ({', '.join(cpf_ids)})"
        )
        output = yt.select_rows_dynamic(query_cpf_dry_run_table)
        dry_run_ids = [str(row["cpf_id"]) for row in output.rows]
        assert len(dry_run_ids) == len(cpf_ids)
        assert set(dry_run_ids) == set(cpf_ids)
