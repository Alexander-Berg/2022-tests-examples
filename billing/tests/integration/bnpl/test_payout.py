import datetime
import typing as tp

import hamcrest as hm
import pytest

from billing.hot.tests.lib.oebs.oebs import OEBSPipeline
from billing.hot.tests.lib.state.state import PipelineState
from billing.library.python.calculator.models.personal_account import ServiceCode
from billing.library.python.calculator.values import PersonType

from billing.hot.tests.clients.accounts.client import Client as AccountsClient
from billing.hot.tests.clients.payout.client import Client as PayoutClient
from billing.hot.tests.clients.processor.bnpl_client import Client as BnplClient
from billing.hot.tests.lib.date import timestamp
from billing.hot.tests.lib.matchers.base import success_payout_info_data_has_item_with
from billing.hot.tests.lib.state import contract


@pytest.mark.parametrize('dry_run', [True])
@pytest.mark.parametrize(
    'payment_amount, refund_amount, payment_commission, refund_commission, expected_payout_amount', [
        (13_000, 1_000, 10, 5, 12_000),  # коммисии временно не учитывается т.к сервис не договорился
    ]
)
@pytest.mark.asyncio
async def test_payout(
    bnpl_client: BnplClient,
    payout_client: PayoutClient,
    accounts_client: AccountsClient,
    create_state_builder: tp.Callable,
    yandex_firm_id: int,
    bnpl_oebs_pipeline: OEBSPipeline,
    dry_run: bool,
    payment_amount: int,
    refund_amount: int,
    payment_commission: int,
    refund_commission: int,
    expected_payout_amount: int,
    test_on_recipes: bool,
):
    builder = create_state_builder()
    builder.with_person_type(person_type=PersonType.UR)
    builder.with_firm(firm_id=yandex_firm_id)
    builder.with_service_code(service_code=ServiceCode.YANDEX_SERVICE)
    builder.fill_contracts(
        contracts=[contract.BnplContract.generate()],
        namespace='bnpl',
        dry_run=dry_run,
    )
    state = builder.built_state()

    await _make_payment(bnpl_client, state, payment_amount, payment_commission)
    await _make_refund(bnpl_client, state, refund_amount, refund_commission)
    await _make_payout(bnpl_client, state)

    if not test_on_recipes:
        await _make_payout_by_client(payout_client, state)

        now_dt_ms = timestamp.now_dt_ms()

        data = await _make_poll_payout_info(payout_client, state, 'pending')
        payout = data[0]
        assert payout['dry_run'] is dry_run
        assert payout['amount'] == str(expected_payout_amount)

        await bnpl_oebs_pipeline.run_ok_pipeline(
            state,
            payout_client,
            expected_payouts_count=1,
            from_dt_ms=now_dt_ms,
            dry_run=dry_run,
        )

        data = await _make_poll_payout_info(payout_client, state, 'done')

        await _get_account_detailed_turnover(accounts_client, state, dry_run, data)


@pytest.mark.parametrize('dry_run', [True])
@pytest.mark.asyncio
async def test_payout_refunds_more_than_payments(
    bnpl_client: BnplClient,
    payout_client: PayoutClient,
    create_state_builder: tp.Callable,
    yandex_firm_id: int,
    dry_run: bool,
    test_on_recipes: bool,
):
    builder = create_state_builder()
    builder.with_person_type(person_type=PersonType.UR)
    builder.with_firm(firm_id=yandex_firm_id)
    builder.with_service_code(service_code=ServiceCode.YANDEX_SERVICE)
    builder.fill_contracts(
        contracts=[contract.BnplContract.generate()],
        namespace='bnpl',
        dry_run=dry_run,
    )
    state = builder.built_state()

    await _make_initialized_payout(bnpl_client, payout_client, state, test_on_recipes)

    state.with_new_generated_external_id()
    await _make_payment(bnpl_client, state, transaction_amount=10_000, total_commission=10)
    await _make_refund(bnpl_client, state, transaction_amount=10_000, total_commission=10_100)
    await _make_payout(bnpl_client, state)

    if not test_on_recipes:
        await _make_payout_by_client(payout_client, state)

        data = await _make_poll_payout_info(payout_client, state, 'pending', expected_records=1)
        hm.assert_that(data, success_payout_info_data_has_item_with({
            'dry_run': dry_run,
        }))


# закомментировал до момента договоренности с сервисом о работе коммиссий
# @pytest.mark.parametrize('dry_run', [True])
# @pytest.mark.asyncio
# async def test_payments_with_commission_more_than_payments(
#         bnpl_client: BnplClient,
#         payout_client,
#         create_state_builder,
#         dry_run,
# ):
#     builder = create_state_builder()
#     builder.with_person_type(person_type='ur')
#     builder.with_firm(firm_id=1)
#     builder.fill_contracts(
#         contracts=[contract.BnplContract.generate()],
#         namespace='bnpl',
#         dry_run=dry_run,
#     )
#     state = builder.built_state()
#
#     await _make_initialized_payout(bnpl_client: BnplClient, payout_client, state)
#
#     state.with_new_generated_external_id()
#     await _make_payment(bnpl_client: BnplClient, state, transaction_amount=100, total_commission=100_000)
#     await _make_payout(bnpl_client: BnplClient, state)
#     await _make_payout_by_client(payout_client, state)
#
#     data = await _make_poll_payout_info(
#         payout_client, state, 'pending', 'done', expected_records=2, timeout_seconds=120
#     )
#     hm.assert_that(data, success_payout_info_data_has_item_with({
#         'dry_run': dry_run,
#         'amount': '0',
#     }))


async def _make_initialized_payout(
    bnpl_client: BnplClient,
    payout_client: PayoutClient,
    state: PipelineState,
    test_on_recipes: bool
):
    await _make_payment(bnpl_client, state, transaction_amount=10, total_commission=0)
    await _make_payout(bnpl_client, state)
    if not test_on_recipes:
        await _make_payout_by_client(payout_client, state)


async def _make_payment(
    bnpl_client: BnplClient,
    state: PipelineState,
    transaction_amount: float,
    total_commission: float
):
    async with bnpl_client.cashless_payment(
        state,
        transaction_amount=transaction_amount,
        total_commission=total_commission,
    ) as response:
        hm.assert_that(response.status, hm.equal_to(200))


async def _make_refund(
    bnpl_client: BnplClient,
    state: PipelineState,
    transaction_amount: float,
    total_commission: float
):
    async with bnpl_client.cashless_refund(
        state,
        transaction_amount=transaction_amount,
        total_commission=total_commission,
    ) as response:
        hm.assert_that(response.status, hm.equal_to(200))


async def _make_payout(bnpl_client: BnplClient, state: PipelineState):
    async with bnpl_client.payout(state) as response:
        hm.assert_that(response.status, hm.equal_to(200))


async def _make_payout_by_client(payout_client: PayoutClient, state: PipelineState):
    async with payout_client.payout_by_client(state, namespace='bnpl') as response:
        hm.assert_that(response.status, hm.equal_to(201))


async def _make_poll_payout_info(
    payout_client: PayoutClient,
    state: PipelineState,
    *expected_statuses,
    expected_records=1,
    timeout_seconds=60,
):
    now = datetime.datetime.now()
    data = await payout_client.poll_payout_info(
        state,
        from_date=now - datetime.timedelta(days=1),
        statuses=expected_statuses,
        expected_records=expected_records,
        timeout_seconds=timeout_seconds,
    )

    return data


async def _get_account_detailed_turnover(
    accounts_client: AccountsClient,
    state: PipelineState,
    dry_run: bool,
    payout_data: list
):
    async with accounts_client.get_account_detailed_turnover(
        state,
        client_id=state.client_id,
        dt_from=datetime.datetime.now() - datetime.timedelta(hours=1),
        dt_to=datetime.datetime.now() + datetime.timedelta(hours=1),
        type_='payout_sent',
        namespace='bnpl',
        add_params={'service_id': None, 'contract_id': None, 'currency': None},
    ) as resp:
        resp_json = await resp.json()

    assert resp_json['status'] == 'ok', resp_json['status']

    events = resp_json['data'][0]['events']
    assert len(events) == 3

    event_types = set()
    for event in events:
        event_dry_run = event['info']['tariffer_payload']['dry_run']
        assert event_dry_run is dry_run, payout_data
        event_types.add(event['event_type'])

    assert event_types == {"reserve for payout", "register payout", "confirm payout"}
