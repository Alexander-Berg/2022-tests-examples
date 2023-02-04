from decimal import Decimal

import pytest

from maps_adv.billing_proxy.lib.domain.exceptions import OrdersDoNotExist

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_uses_dm(orders_domain, orders_dm):
    orders_dm.list_inexistent_order_ids.coro.return_value = []
    orders_dm.list_orders_stats.coro.return_value = [
        {1: {"balance": Decimal("50.00")}},
        {2: {"balance": Decimal("150.00")}},
        {3: {"balance": Decimal("250.00")}},
    ]
    result = await orders_domain.list_orders_stats(order_ids=[1, 2, 3])

    orders_dm.list_orders_stats.assert_called_with([1, 2, 3])
    assert result == [
        {1: {"balance": Decimal("50.00")}},
        {2: {"balance": Decimal("150.00")}},
        {3: {"balance": Decimal("250.00")}},
    ]


async def test_raises_for_inexistent_orders(orders_domain, orders_dm):
    orders_dm.list_inexistent_order_ids.coro.return_value = [1, 2]
    orders_dm.list_orders_stats.coro.return_value = [
        {1: {"balance": Decimal("50.00")}},
        {2: {"balance": Decimal("150.00")}},
        {3: {"balance": Decimal("250.00")}},
    ]

    with pytest.raises(OrdersDoNotExist) as exc:
        await orders_domain.list_orders_stats(order_ids=[1, 2, 3])

    assert exc.value.order_ids == [1, 2]
