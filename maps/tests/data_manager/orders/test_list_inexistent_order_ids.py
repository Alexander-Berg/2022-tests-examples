import pytest

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    ("existing_ids", "ids_to_check", "expected_ids"),
    [
        ([1, 2, 3], [1, 2, 4], [4]),
        ([1, 2, 3], [4, 5, 6], [4, 5, 6]),
        ([1, 2, 3], [1, 2, 3], []),
        ([1, 2, 3], [], []),
        ([], [1, 2, 3], [1, 2, 3]),
        ([], [], []),
    ],
)
async def test_returns_inexistent_orders(
    factory, existing_ids, ids_to_check, expected_ids, orders_dm
):
    for order_id in existing_ids:
        await factory.create_order(id=order_id)

    assert await orders_dm.list_inexistent_order_ids(ids_to_check) == expected_ids


async def test_returns_hidden_orders(factory, orders_dm):
    order1 = await factory.create_order(hidden=False)
    order2 = await factory.create_order(hidden=True)

    result = await orders_dm.list_inexistent_order_ids([order1["id"], order2["id"]])

    assert result == [order2["id"]]
