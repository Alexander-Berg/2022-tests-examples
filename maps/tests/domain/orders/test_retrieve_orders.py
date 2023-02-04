import pytest

from maps_adv.billing_proxy.lib.domain.exceptions import OrdersDoNotExist

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_users_dm(orders_domain, orders_dm):
    orders_dm.list_inexistent_order_ids.coro.return_value = []
    orders_dm.find_orders.coro.return_value = [
        {"id": 11, "title": "Заказ 11"},
        {"id": 12, "title": "Заказ 12"},
        {"id": 13, "title": "Заказ 13"},
    ]

    result = await orders_domain.retrieve_orders([11, 12, 13])

    orders_dm.find_orders.assert_called_with([11, 12, 13])
    assert result == [
        {"id": 11, "title": "Заказ 11"},
        {"id": 12, "title": "Заказ 12"},
        {"id": 13, "title": "Заказ 13"},
    ]


async def test_raises_for_inexistent_orders(orders_domain, orders_dm):
    orders_dm.list_inexistent_order_ids.coro.return_value = [11, 12]
    orders_dm.find_orders.coro.return_value = [
        {"id": 11, "title": "Заказ 11"},
        {"id": 12, "title": "Заказ 12"},
        {"id": 13, "title": "Заказ 13"},
    ]

    with pytest.raises(OrdersDoNotExist) as exc:
        await orders_domain.retrieve_orders([11, 12, 13])

    assert exc.value.order_ids == [11, 12]
