import pytest

from maps_adv.billing_proxy.lib.data_manager.exceptions import (
    ClientDoesNotExist,
    AgencyDoesNotExist,
)

pytestmark = [pytest.mark.asyncio]


@pytest.fixture(autouse=True)
def common_balance_client_mocks(balance_client):
    balance_client.create_client.coro.return_value = 55


async def test_sets_has_accepted_offer(client, agency, clients_dm):
    assert not client["has_accepted_offer"]
    await clients_dm.set_client_has_accepted_offer(client["id"], is_agency=False)
    assert (await clients_dm.list_clients())[0]["has_accepted_offer"]

    assert not agency["has_accepted_offer"]
    await clients_dm.set_client_has_accepted_offer(agency["id"], is_agency=True)
    assert (await clients_dm.list_agencies())[0]["has_accepted_offer"]


async def test_fails_non_existing_clients(clients_dm):
    with pytest.raises(ClientDoesNotExist) as exc:
        await clients_dm.set_client_has_accepted_offer(42, is_agency=False)
    assert exc.value.client_id == 42

    with pytest.raises(AgencyDoesNotExist) as exc:
        await clients_dm.set_client_has_accepted_offer(42, is_agency=True)
    assert exc.value.agency_id == 42
