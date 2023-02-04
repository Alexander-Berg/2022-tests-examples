import pytest

from maps_adv.billing_proxy.lib.data_manager import exceptions as dm_exceptions
from maps_adv.billing_proxy.lib.domain.exceptions import AgencyDoesNotExist

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_uses_dm(clients_domain, clients_dm):
    clients_dm.list_agency_clients.coro.return_value = [
        {"id": 3, "name": "Клиент 1"},
        {"id": 4, "name": "Клиент 2"},
    ]

    result = await clients_domain.list_agency_clients(agency_id=333)

    clients_dm.list_agency_clients.assert_called_with(333)
    assert result == [{"id": 3, "name": "Клиент 1"}, {"id": 4, "name": "Клиент 2"}]


async def raises_for_inexistent_agency(clients_domain, clients_dm):
    clients_dm.list_agency_clients.coro.side_effect = dm_exceptions.AgencyDoesNotExist(
        agency_id=333
    )

    with pytest.raises(AgencyDoesNotExist) as exc:
        await clients_domain.list_agency_clients(333)

    assert exc.value.agency_id == 333
