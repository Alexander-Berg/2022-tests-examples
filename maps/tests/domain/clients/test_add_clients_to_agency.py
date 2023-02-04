import pytest

from maps_adv.billing_proxy.lib.data_manager import exceptions as dm_exceptions
from maps_adv.billing_proxy.lib.domain.exceptions import (
    AgencyDoesNotExist,
    ClientsDoNotExist,
    ClientsAreAlreadyInAgency,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_calls_dm(clients_domain, clients_dm):
    clients_dm.list_clients_with_agencies.coro.return_value = []

    await clients_domain.add_clients_to_agency(client_ids=[1, 2], agency_id=5)

    clients_dm.add_clients_to_agency.assert_called_with([1, 2], 5)


async def test_raises_for_inexistent_client(clients_domain, clients_dm):
    clients_dm.list_clients_with_agencies.coro.return_value = []
    clients_dm.add_clients_to_agency.coro.side_effect = dm_exceptions.ClientsDoNotExist(
        client_ids=[2, 3]
    )

    with pytest.raises(ClientsDoNotExist) as exc:
        await clients_domain.add_clients_to_agency(client_ids=[1, 2, 3], agency_id=5)

    assert exc.value.client_ids == [2, 3]


async def test_raises_for_inexistent_agency(factory, clients_domain, clients_dm):
    clients_dm.list_clients_with_agencies.coro.return_value = []
    clients_dm.add_clients_to_agency.coro.side_effect = (
        dm_exceptions.AgencyDoesNotExist(agency_id=5)  # noqa: E501
    )

    with pytest.raises(AgencyDoesNotExist) as exc:
        await clients_domain.add_clients_to_agency(client_ids=[1, 2, 3], agency_id=5)

    assert exc.value.agency_id == 5


async def test_raises_for_clients_already_in_agency(clients_domain, clients_dm):
    clients_dm.list_clients_with_agencies.coro.return_value = [1, 2]
    with pytest.raises(ClientsAreAlreadyInAgency) as exc:
        await clients_domain.add_clients_to_agency(client_ids=[1, 2, 3], agency_id=5)

    assert exc.value.client_ids == [1, 2]
