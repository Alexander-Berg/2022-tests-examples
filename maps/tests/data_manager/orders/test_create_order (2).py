from datetime import datetime

import pytest

from maps_adv.manul.lib.data_managers.exceptions import ClientNotFound
from maps_adv.manul.lib.db.enums import CurrencyType, RateType
from maps_adv.manul.tests import Any

pytestmark = [pytest.mark.asyncio]


async def test_creates_order(orders_dm, con, factory):
    client_id = (
        await factory.create_client(name="client_name", account_manager_id=100500)
    )["id"]

    got = await orders_dm.create_order(
        title="title1",
        client_id=client_id,
        product_id=2,
        currency=CurrencyType.RUB,
        comment="comment1",
        rate=RateType.FREE,
    )

    sql = """
        SELECT EXISTS(
            SELECT id
            FROM orders
            WHERE id = $1 AND title = $2 AND client_id = $3
                AND product_id = $4 AND currency = $5 AND comment = $6
                AND rate = $7
        )
    """
    assert (
        await con.fetchval(
            sql,
            got["id"],
            "title1",
            client_id,
            2,
            CurrencyType.RUB,
            "comment1",
            RateType.FREE,
        )
        is True
    )


async def test_returns_order_details(orders_dm, factory):
    client_id = (
        await factory.create_client(name="client_name", account_manager_id=100500)
    )["id"]

    got = await orders_dm.create_order(
        title="title1",
        client_id=client_id,
        product_id=2,
        currency=CurrencyType.RUB,
        comment="comment1",
        rate=RateType.FREE,
    )

    assert got == {
        "id": Any(int),
        "title": "title1",
        "client_id": client_id,
        "product_id": 2,
        "currency": CurrencyType.RUB,
        "comment": "comment1",
        "created_at": Any(datetime),
        "rate": RateType.FREE,
    }


async def test_can_create_two_identical_orders(orders_dm, factory):
    client_id = (
        await factory.create_client(name="client_name", account_manager_id=100500)
    )["id"]
    kwargs = dict(
        title="title1",
        client_id=client_id,
        product_id=2,
        currency=CurrencyType.RUB,
        comment="comment1",
        rate=RateType.PAID,
    )

    order1 = await orders_dm.create_order(**kwargs)
    order2 = await orders_dm.create_order(**kwargs)

    assert order1.pop("id") != order2.pop("id")
    assert order1 == order2


async def test_raises_for_unknown_client(orders_dm):
    with pytest.raises(ClientNotFound):
        await orders_dm.create_order(
            title="title1",
            client_id=100500,
            product_id=2,
            currency=CurrencyType.RUB,
            comment="comment1",
            rate=RateType.PAID,
        )
