import pytest

from maps_adv.billing_proxy.lib.domain.exceptions import ClientsHaveOrdersWithAgency
from maps_adv.billing_proxy.lib.domain.exceptions import AgencyDoesNotExist

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_uses_dm(clients_domain, clients_dm):
    clients_dm.list_clients_with_orders_with_agency.coro.return_value = []
    clients_dm.agency_exists.coro.return_value = True

    await clients_domain.remove_clients_from_agency([1, 2], 333)

    clients_dm.list_clients_with_orders_with_agency.assert_called_with([1, 2], 333)
    clients_dm.remove_clients_from_agency.assert_called_with([1, 2], 333)


async def raises_for_inexistent_agency(clients_domain, clients_dm):
    clients_dm.agency_exists.coro.return_value = False

    with pytest.raises(AgencyDoesNotExist) as exc:
        await clients_domain.remove_clients_from_agency([1, 2], 333)

    assert exc.value.agency_id == 333


async def test_raises_on_clients_having_orders_with_agency(clients_domain, clients_dm):
    clients_dm.list_clients_with_orders_with_agency.coro.return_value = [1]
    with pytest.raises(ClientsHaveOrdersWithAgency) as exc:
        await clients_domain.remove_clients_from_agency([1, 2], 333)

    assert exc.value.client_ids == [1]
