import datetime

import hamcrest as hm
import pytest
from billing.library.python.calculator.values import PersonType

from billing.hot.tests.clients.accounts.client import Client as AccountsClient
from billing.hot.tests.clients.payout.client import Client as PayoutClient
from billing.hot.tests.clients.processor.taxi_light_client import Client as TaxiLightClient
from billing.hot.tests.lib.state import contract


@pytest.mark.parametrize('dry_run', [True])
@pytest.mark.parametrize('amount', [100])
@pytest.mark.asyncio
async def test_taxi_light_payout(
    taxi_light_client: TaxiLightClient,
    payout_client: PayoutClient,
    accounts_client: AccountsClient,
    create_state_builder,
    yandex_firm_id: int,
    dry_run,
    amount,
    test_on_recipes,
):
    builder = create_state_builder()
    builder.with_person_type(person_type=PersonType.UR)
    builder.with_firm(firm_id=yandex_firm_id)
    builder.fill_contracts(
        contracts=[contract.ServiceContract.generate()],
        namespace='taxi_light',
        dry_run=dry_run,
    )
    state = builder.built_state()

    await _make_payout(taxi_light_client, state, amount=amount)

    # if not test_on_recipes:
    #     await _make_payout_by_client(payout_client, state, amount=amount)
    #
    #     data = await _make_poll_payout_info(payout_client, state, 'pending')
    #     payout = data[0]
    #     assert payout['dry_run'] is dry_run
    #     assert payout['amount'] == str(amount)
    #
    #     if not test_on_recipes:
    #         data = await _make_poll_payout_info(payout_client, state, 'done')
    #
    #         await _get_account_detailed_turnover(accounts_client, state, dry_run, data)


async def _make_payout(taxi_light_client, state, amount=None):
    async with taxi_light_client.payout(state, amount=amount) as response:
        hm.assert_that(response.status, hm.equal_to(200))


async def _make_payout_by_client(payout_client, state, amount=None):
    async with payout_client.payout_by_client(state, namespace='taxi_light') as response:
        hm.assert_that(response.status, hm.equal_to(201))


async def _make_poll_payout_info(
    payout_client,
    state,
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


async def _get_account_detailed_turnover(accounts_client, state, dry_run, payout_data):
    async with accounts_client.get_account_detailed_turnover(
        state,
        client_id=state.client_id,
        dt_from=datetime.datetime.now() - datetime.timedelta(hours=1),
        dt_to=datetime.datetime.now() + datetime.timedelta(hours=1),
        type_='incoming_payments_sent',
        namespace='taxi_light',
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
