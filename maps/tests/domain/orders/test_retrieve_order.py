import pytest

from maps_adv.billing_proxy.lib.domain.exceptions import OrderDoesNotExist

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_uses_dm(orders_domain, orders_dm):
    orders_dm.find_order.coro.return_value = {"id": 11, "title": "Заказ"}

    result = await orders_domain.retrieve_order(order_id=11)

    orders_dm.find_order.assert_called_with(11)
    assert result == {"id": 11, "title": "Заказ"}


async def test_raises_for_inexistent_order(orders_domain, orders_dm):
    orders_dm.find_order.coro.return_value = None

    with pytest.raises(OrderDoesNotExist) as exc:
        await orders_domain.retrieve_order(order_id=11)

    assert exc.value.order_id == 11


async def test_uses_dm_by_external_id(orders_domain, orders_dm):
    orders_dm.find_order_by_external_id.coro.return_value = {
        "id": 11,
        "external_id": 22,
        "title": "Заказ",
    }

    result = await orders_domain.retrieve_order_by_external_id(external_id=22)

    orders_dm.find_order_by_external_id.assert_called_with(22)
    assert result == {"id": 11, "external_id": 22, "title": "Заказ"}


async def test_raises_for_inexistent_order_by_external_id(orders_domain, orders_dm):
    orders_dm.find_order_by_external_id.coro.return_value = None

    with pytest.raises(OrderDoesNotExist) as exc:
        await orders_domain.retrieve_order_by_external_id(external_id=11)

    assert exc.value.order_id is None
