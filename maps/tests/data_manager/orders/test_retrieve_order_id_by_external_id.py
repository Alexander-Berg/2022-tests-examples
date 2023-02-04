import pytest

pytestmark = [pytest.mark.asyncio]


async def test_returns_id_if_found(factory, product, orders_dm):
    order = await factory.create_order(product_id=product["id"])

    result = await orders_dm.retrieve_order_id_by_external_id(order["external_id"])

    assert result == order["id"]


async def test_returns_none_if_not_found(orders_dm):
    assert await orders_dm.retrieve_order_id_by_external_id(555) is None


async def test_returns_even_if_order_is_hidden(factory, orders_dm):
    order = await factory.create_order(hidden=True)

    assert (
        await orders_dm.retrieve_order_id_by_external_id(order["external_id"])
        is order["id"]
    )
