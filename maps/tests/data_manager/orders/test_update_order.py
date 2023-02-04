import pytest

pytestmark = [pytest.mark.asyncio]


async def test_updates_passed_order_data(con, factory, orders_dm):
    order = await factory.create_order()
    values = {"title": "new title", "text": "new text", "comment": "new_comment"}

    await orders_dm.update_order(
        order_id=order["id"],
        title=values["title"],
        text=values["text"],
        comment=values["comment"],
    )

    order_info = await con.fetchrow(
        """
        SELECT *
        FROM orders
        WHERE id = $1
        """,
        order["id"],
    )
    updated_order = {**order, **values}

    assert dict(order_info) == updated_order


async def test_returns_nothing(factory, orders_dm):
    order = await factory.create_order()
    values = {"title": "new title", "text": "new text", "comment": "new_comment"}

    updated_order = await orders_dm.update_order(
        order_id=order["id"],
        title=values["title"],
        text=values["text"],
        comment=values["comment"],
    )

    assert updated_order is None
