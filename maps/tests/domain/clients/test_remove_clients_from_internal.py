import pytest

from maps_adv.billing_proxy.lib.domain.exceptions import ClientsHaveOrdersWithAgency

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_uses_dm(clients_domain, clients_dm):
    clients_dm.list_clients_with_orders_with_agency.coro.return_value = []
    await clients_domain.remove_clients_from_internal([1, 2])

    clients_dm.list_clients_with_orders_with_agency.assert_called_with([1, 2], None)
    clients_dm.remove_clients_from_agency.assert_called_with([1, 2], None)


async def test_raises_on_clients_having_orders_with_internal_agency(
    clients_domain, clients_dm
):
    clients_dm.list_clients_with_orders_with_agency.coro.return_value = [1]
    with pytest.raises(ClientsHaveOrdersWithAgency) as exc:
        await clients_domain.remove_clients_from_internal([1, 2])

    assert exc.value.client_ids == [1]
