import pytest

pytestmark = [pytest.mark.asyncio]


async def test_clears_orders_table_for_matched_orders(factory, dm, con):
    await factory.create_order(
        customer_name="Иван Петров",
        customer_phone="+7 (000) 000-00-00",
        customer_passport_uid=12345,
    )

    await dm.clear_orders_by_passport(passport_uid=12345)

    order = await con.fetchrow(
        "SELECT customer_name, customer_phone, customer_passport_uid FROM orders"
    )
    assert dict(order) == dict(
        customer_name=None, customer_phone=None, customer_passport_uid=None
    )


async def test_does_not_clear_not_matched_orders(factory, dm, con):
    await factory.create_order(
        customer_name="Иван Петров",
        customer_phone="+7 (000) 000-00-00",
        customer_passport_uid=999,
    )

    await dm.clear_orders_by_passport(passport_uid=12345)

    order = await con.fetchrow(
        "SELECT customer_name, customer_phone, customer_passport_uid FROM orders"
    )
    assert dict(order) == dict(
        customer_name="Иван Петров",
        customer_phone="+7 (000) 000-00-00",
        customer_passport_uid=999,
    )


async def test_returns_cleared_order_ids(factory, dm, con):
    id_1 = await factory.create_order(customer_passport_uid=12345, yang_suite_id="111")
    id_2 = await factory.create_order(customer_passport_uid=12345, yang_suite_id="222")

    got = await dm.clear_orders_by_passport(passport_uid=12345)

    assert set(got) == {id_1, id_2}


async def test_returns_nothing_if_no_matches(factory, dm, con):
    await factory.create_order(customer_passport_uid=12345)

    got = await dm.clear_orders_by_passport(passport_uid=999)

    assert got == []
