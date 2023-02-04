import pytest

from maps_adv.billing_proxy.lib.domain.exceptions import OrdersDoNotExist

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_uses_dm(orders_domain, orders_dm):
    orders_dm.list_positive_balance_orders.coro.return_value = [1, 2]
    orders_dm.list_inexistent_order_ids.coro.return_value = []

    result = await orders_domain.list_active_orders([1, 2, 3])

    orders_dm.list_positive_balance_orders.assert_called_with([1, 2, 3])
    assert result == [1, 2]


async def test_raises_for_inexistent_orders(orders_domain, orders_dm):
    orders_dm.list_positive_balance_orders.coro.return_value = [1, 2]
    orders_dm.list_inexistent_order_ids.coro.return_value = [3]

    with pytest.raises(OrdersDoNotExist) as exc:
        await orders_domain.list_active_orders([1, 2, 3])

    assert exc.value.order_ids == [3]
