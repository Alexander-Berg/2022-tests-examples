from datetime import datetime

import pytest

from maps_adv.manul.lib.db.enums import CurrencyType, RateType
from maps_adv.manul.tests import Any

pytestmark = [pytest.mark.asyncio, pytest.mark.real_db]


async def test_returns_list_of_all_orders_if_no_order_ids_passed(orders_dm, factory):
    client_id = (await factory.create_client("client", account_manager_id=100500))["id"]
    order1 = await factory.create_order("order1", client_id)
    order2 = await factory.create_order(
        "order2", client_id, product_id=2, currency=CurrencyType.BYN, rate=RateType.FREE
    )
    order3 = await factory.create_order(
        "order3", client_id, product_id=3, comment="comment"
    )

    got = await orders_dm.list_orders([])

    assert got == [
        {
            "id": order3["id"],
            "title": "order3",
            "client_id": client_id,
            "product_id": 3,
            "currency": CurrencyType.RUB,
            "comment": "comment",
            "created_at": Any(datetime),
            "rate": RateType.PAID,
        },
        {
            "id": order2["id"],
            "title": "order2",
            "client_id": client_id,
            "product_id": 2,
            "currency": CurrencyType.BYN,
            "comment": "",
            "created_at": Any(datetime),
            "rate": RateType.FREE,
        },
        {
            "id": order1["id"],
            "title": "order1",
            "client_id": client_id,
            "product_id": 1,
            "currency": CurrencyType.RUB,
            "comment": "",
            "created_at": Any(datetime),
            "rate": RateType.PAID,
        },
    ]


async def test_returns_list_of_requested_existing_orders_ids(orders_dm, factory):
    client_id = (await factory.create_client("client", account_manager_id=100500))["id"]
    order1 = await factory.create_order("order1", client_id)
    order2 = await factory.create_order("order2", client_id)
    await factory.create_order("order3", client_id)

    got = await orders_dm.list_orders([order1["id"], order2["id"], 9999])

    assert got == [
        {
            "id": order2["id"],
            "title": "order2",
            "client_id": client_id,
            "product_id": 1,
            "currency": CurrencyType.RUB,
            "comment": "",
            "created_at": Any(datetime),
            "rate": RateType.PAID,
        },
        {
            "id": order1["id"],
            "title": "order1",
            "client_id": client_id,
            "product_id": 1,
            "currency": CurrencyType.RUB,
            "comment": "",
            "created_at": Any(datetime),
            "rate": RateType.PAID,
        },
    ]


async def test_returns_nothing_if_nothing_found(orders_dm, factory):
    got = await orders_dm.list_orders([99999])

    assert got == []
