import datetime
import typing as tp

import hamcrest as hm
import pytest

from billing.hot.tests.lib.oebs.oebs import OEBSPipeline
from billing.hot.tests.lib.state.state import PipelineState
from billing.library.python.calculator.values import PersonType
from dataclasses import dataclass

from billing.hot.tests.clients.accounts.client import Client as AccountsClient
from billing.hot.tests.clients.payout.client import Client as PayoutClient
from billing.hot.tests.clients.processor.oplata_client import Client as OplataClient
from billing.hot.tests.lib.date import timestamp
from billing.hot.tests.lib.matchers.base import success_payout_info_data_has_item_with
from billing.hot.tests.lib.state import contract
from billing.hot.tests.lib.state import state


def _prepare(create_state_builder: tp.Callable, firm_id: int, dry_run: bool) -> state.PipelineState:
    builder = create_state_builder()
    builder.with_person_type(person_type=PersonType.UR)
    builder.with_firm(firm_id=firm_id)
    builder.fill_contracts(
        contracts=[contract.OplataContract.generate()],
        namespace='oplata',
        dry_run=dry_run,
    )
    return builder.built_state()


@dataclass
class Price:
    by_card: float
    by_promocode: float = 0

    @property
    def total(self) -> float:
        return self.by_card + self.by_promocode


@pytest.mark.parametrize('dry_run', [True])
@pytest.mark.parametrize(
    'order_price, refund_price, commission, expected_payout_amount', [
        (Price(by_card=13_000), Price(by_card=1_000), 10, 11_987),
        (Price(by_card=10_000), Price(by_card=500), 20, 9_480),
        (Price(by_card=5_000), Price(by_card=2_300), 50, 2_675),
        (Price(by_card=9_000, by_promocode=1_000), Price(by_card=100, by_promocode=500), 3, 9397),
        (Price(by_card=9_999, by_promocode=1), Price(by_card=9_000, by_promocode=900), 1, 99),
    ]
)
@pytest.mark.asyncio
async def test_payout(
    oplata_client: OplataClient,
    payout_client: PayoutClient,
    accounts_client: AccountsClient,
    create_state_builder,
    yandex_firm_id: int,
    oplata_oebs_pipeline: OEBSPipeline,
    dry_run: bool,
    order_price: Price,
    refund_price: Price,
    commission: int,
    expected_payout_amount: int,
    test_on_recipes: bool,
):
    built_state = _prepare(create_state_builder, yandex_firm_id, dry_run)

    await _make_payment(oplata_client, built_state, order_price, commission)
    await _make_refund(oplata_client, built_state, refund_price, refund_price)
    await _make_payout(oplata_client, built_state)

    if not test_on_recipes:
        await _make_payout_by_client(payout_client, built_state)

        now_dt_ms = timestamp.now_dt_ms()

        data = await _make_poll_payout_info(payout_client, built_state, 'pending')
        payout = data[0]
        assert payout['dry_run'] is dry_run
        assert payout['amount'] == str(expected_payout_amount)

        await oplata_oebs_pipeline.run_ok_pipeline(
            built_state,
            payout_client,
            expected_payouts_count=1,
            from_dt_ms=now_dt_ms,
            dry_run=dry_run,
        )

        data = await _make_poll_payout_info(payout_client, built_state, 'done')

        await _get_account_detailed_turnover(accounts_client, built_state, dry_run, data)


@pytest.mark.parametrize('dry_run', [True])
@pytest.mark.asyncio
async def test_payout_refunds_more_than_payments(
    oplata_client: OplataClient,
    payout_client: PayoutClient,
    create_state_builder: tp.Callable,
    yandex_firm_id: int,
    dry_run: bool,
    test_on_recipes: bool,
):
    built_state = _prepare(create_state_builder, yandex_firm_id, dry_run)

    await _make_initialized_payout(oplata_client, payout_client, built_state, test_on_recipes)

    built_state.with_new_generated_external_id()
    await _make_payment(
        oplata_client,
        built_state,
        order_price=Price(by_card=10_000),
        commission=10,
    )
    await _make_refund(
        oplata_client,
        built_state,
        original_order_price=Price(by_card=10_000),
        refund_price=Price(by_card=10_100),
    )
    await _make_payout(oplata_client, built_state)

    if not test_on_recipes:
        await _make_payout_by_client(payout_client, built_state)

        data = await _make_poll_payout_info(payout_client, built_state, 'pending', 'done', expected_records=2)
        hm.assert_that(data, success_payout_info_data_has_item_with({
            'dry_run': dry_run,
            'amount': '0',
        }))


@pytest.mark.parametrize('dry_run', [True])
@pytest.mark.asyncio
async def test_payments_with_commission_more_than_payments(
    oplata_client: OplataClient,
    payout_client: PayoutClient,
    create_state_builder,
    yandex_firm_id: int,
    dry_run: bool,
    test_on_recipes: bool,
):
    built_state = _prepare(create_state_builder, yandex_firm_id, dry_run)

    await _make_initialized_payout(oplata_client, payout_client, built_state, test_on_recipes)

    built_state.with_new_generated_external_id()
    await _make_payment(oplata_client, built_state, order_price=Price(by_card=100), commission=100_000)
    await _make_payout(oplata_client, built_state)

    if not test_on_recipes:
        await _make_payout_by_client(payout_client, built_state)

        data = await _make_poll_payout_info(
            payout_client, built_state,
            'pending',
            'done',
            expected_records=2,
            timeout_seconds=120,
        )
        hm.assert_that(data, success_payout_info_data_has_item_with({
            'dry_run': dry_run,
            'amount': '0',
        }))


async def _make_initialized_payout(
    oplata_client: OplataClient,
    payout_client: PayoutClient,
    built_state: PipelineState,
    test_on_recipes: bool
):
    await _make_payment(oplata_client, built_state, order_price=Price(by_card=100), commission=0)
    await _make_payout(oplata_client, built_state)
    if not test_on_recipes:
        await _make_payout_by_client(payout_client, built_state)


async def _make_payment(
    oplata_client: OplataClient,
    built_state: PipelineState,
    order_price: Price,
    commission: int,
):
    async with oplata_client.cashless_payment(
        built_state,
        order_price=order_price.total,
        commission=commission,
        item_by_card=order_price.by_card,
        item_by_promocode=order_price.by_promocode,
    ) as response:
        hm.assert_that(response.status, hm.equal_to(200))


async def _make_refund(
    oplata_client: OplataClient,
    built_state: PipelineState,
    original_order_price: Price,
    refund_price: Price,
):
    async with oplata_client.cashless_refund(
        built_state,
        original_order_price=original_order_price.total,
        refund_price=refund_price.total,
        item_by_card=refund_price.by_card,
        item_by_promocode=refund_price.by_promocode,
    ) as response:
        hm.assert_that(response.status, hm.equal_to(200))


async def _make_payout(oplata_client, built_state):
    async with oplata_client.payout(built_state) as response:
        hm.assert_that(response.status, hm.equal_to(200))


async def _make_payout_by_client(payout_client, built_state):
    async with payout_client.payout_by_client(built_state, namespace='oplata') as response:
        hm.assert_that(response.status, hm.equal_to(201))


async def _make_poll_payout_info(
    payout_client: PayoutClient,
    built_state: PipelineState,
    *expected_statuses,
    expected_records=1,
    timeout_seconds=60,
):
    now = datetime.datetime.now()
    data = await payout_client.poll_payout_info(
        built_state,
        from_date=now - datetime.timedelta(days=1),
        statuses=expected_statuses,
        expected_records=expected_records,
        timeout_seconds=timeout_seconds,
    )

    return data


async def _get_account_detailed_turnover(
    accounts_client: AccountsClient,
    built_state: PipelineState,
    dry_run: bool,
    payout_data: list
):
    async with accounts_client.get_account_detailed_turnover(
        built_state,
        client_id=built_state.client_id,
        dt_from=datetime.datetime.now() - datetime.timedelta(hours=1),
        dt_to=datetime.datetime.now() + datetime.timedelta(hours=1),
        type_='payout_sent',
        namespace='oplata',
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
