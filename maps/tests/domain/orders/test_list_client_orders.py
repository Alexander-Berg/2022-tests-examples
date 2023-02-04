import pytest

from maps_adv.billing_proxy.lib.domain.exceptions import (
    ClientDoesNotExist,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.fixture(autouse=True)
async def common_dm_mocks(clients_dm, orders_dm):
    clients_dm.client_exists.coro.return_value = True
    orders_dm.list_client_orders.coro.return_value = [
        {"id": 1, "title": "Заказ 1"},
        {"id": 2, "title": "Заказ 2"},
    ]


async def test_uses_dm(orders_domain, orders_dm):
    result = await orders_domain.list_client_orders(client_id=22)
    orders_dm.list_client_orders.assert_called_with(22)
    assert result == [{"id": 1, "title": "Заказ 1"}, {"id": 2, "title": "Заказ 2"}]


async def test_raises_for_inexistent_client(orders_domain, clients_dm):
    clients_dm.client_exists.coro.return_value = False

    with pytest.raises(ClientDoesNotExist) as exc:
        await orders_domain.list_client_orders(client_id=22)

    assert exc.value.client_id == 22
