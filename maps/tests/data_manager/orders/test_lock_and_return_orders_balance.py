from decimal import Decimal
from operator import itemgetter

import pytest
from asyncpg.exceptions import LockNotAvailableError

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    ("orders_to_create", "expected_balances"),
    [
        (
            [{"id": 1, "limit": Decimal("100"), "consumed": Decimal("50")}],
            {1: Decimal("50")},
        ),
        (
            [
                {"id": 1, "limit": Decimal("100"), "consumed": Decimal("50")},
                {"id": 2, "limit": Decimal("200"), "consumed": Decimal("100")},
            ],
            {1: Decimal("50"), 2: Decimal("100")},
        ),
        (
            [
                {"id": 1, "limit": Decimal("100"), "consumed": Decimal("0")},
                {"id": 2, "limit": Decimal("200"), "consumed": Decimal("200")},
            ],
            {1: Decimal("100"), 2: Decimal("0")},
        ),
    ],
)
async def test_returns_balance(
    con, factory, orders_to_create, expected_balances, orders_dm
):
    for order_data in orders_to_create:
        await factory.create_order(**order_data)

    orders_ids = list(map(itemgetter("id"), orders_to_create))
    async with orders_dm.lock_and_return_orders_balance(orders_ids) as result:
        assert result == (expected_balances, con)


@pytest.mark.real_db
async def test_select_for_update_locks_orders_to_update(db, factory, orders_dm):
    order = await factory.create_order()
    async with orders_dm.lock_and_return_orders_balance([order["id"]]):
        with pytest.raises(LockNotAvailableError):
            async with db.acquire() as con:
                await con.fetch(
                    """
                    SELECT id
                    FROM orders
                    WHERE id = $1
                    FOR UPDATE NOWAIT
                    """,
                    order["id"],
                )


@pytest.mark.real_db
async def test_select_for_update_not_locks_other_orders(db, factory, orders_dm):
    order_to_charge = await factory.create_order()
    another_order = await factory.create_order()
    async with orders_dm.lock_and_return_orders_balance([order_to_charge["id"]]):
        try:
            async with db.acquire() as con:
                await con.fetch(
                    """
                    SELECT *
                    FROM orders
                    WHERE id = $1
                    FOR UPDATE NOWAIT
                    """,
                    another_order["id"],
                )
        except LockNotAvailableError:
            pytest.fail("Should not raise LockNotAvailableError")
