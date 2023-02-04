from decimal import Decimal

import pytest

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    ("limit", "consumed", "expected_balance"),
    [
        (Decimal("123.45"), Decimal("100.12"), Decimal("23.33")),
        (Decimal("123.4567"), Decimal("100.1234"), Decimal("23.3333")),
        (Decimal("123.4567"), Decimal("0"), Decimal("123.4567")),
        (Decimal("123.4567"), Decimal("123.4567"), Decimal("0")),
        (Decimal("0"), Decimal("0"), Decimal("0")),
    ],
)
async def test_returns_expected_data(
    factory, limit, consumed, expected_balance, orders_dm
):
    order = await factory.create_order(limit=limit, consumed=consumed)

    result = await orders_dm.list_orders_stats([order["id"]])

    assert result == {order["id"]: {"balance": expected_balance}}


async def test_lists_many_orders_stats(factory, orders_dm):
    order1 = await factory.create_order(
        limit=Decimal("200.00"), consumed=Decimal("100.00")
    )
    order2 = await factory.create_order(
        limit=Decimal("300.00"), consumed=Decimal("150.00")
    )
    order3 = await factory.create_order(
        limit=Decimal("250.00"), consumed=Decimal("200.00")
    )

    result = await orders_dm.list_orders_stats(
        [order1["id"], order2["id"], order3["id"]]
    )

    assert result == {
        order1["id"]: {"balance": Decimal("100.00")},
        order2["id"]: {"balance": Decimal("150.00")},
        order3["id"]: {"balance": Decimal("50.00")},
    }


async def test_ignores_inexistent_orders(factory, orders_dm):
    order = await factory.create_order(
        limit=Decimal("200.00"), consumed=Decimal("100.00")
    )
    inexistent_id = order["id"] + 1

    result = await orders_dm.list_orders_stats([order["id"], inexistent_id])

    assert result == {order["id"]: {"balance": Decimal("100.00")}}


async def test_ignores_hidden_orders(factory, orders_dm):
    order1 = await factory.create_order(
        limit=Decimal("200.00"), consumed=Decimal("100.00"), hidden=False
    )
    order2 = await factory.create_order(
        limit=Decimal("200.00"), consumed=Decimal("100.00"), hidden=True
    )

    result = await orders_dm.list_orders_stats([order1["id"], order2["id"]])

    assert result == {order1["id"]: {"balance": Decimal("100.00")}}
