from datetime import datetime

import pytest

from maps_adv.manul.lib.data_managers.exceptions import OrderNotFound
from maps_adv.manul.lib.db.enums import CurrencyType, RateType
from maps_adv.manul.tests import Any

pytestmark = [pytest.mark.asyncio]


async def test_returns_order_details(orders_dm, factory):
    client_id = (await factory.create_client(name="client", account_manager_id=100500))[
        "id"
    ]
    order_id = (
        await factory.create_order(
            "order0", client_id, product_id=2, comment="comment", rate=RateType.FREE
        )
    )["id"]

    got = await orders_dm.retrieve_order(order_id)

    assert got == {
        "id": order_id,
        "title": "order0",
        "client_id": client_id,
        "product_id": 2,
        "currency": CurrencyType.RUB,
        "comment": "comment",
        "created_at": Any(datetime),
        "rate": RateType.FREE,
    }


async def test_raises_if_order_does_not_exist(orders_dm):
    with pytest.raises(OrderNotFound):
        await orders_dm.retrieve_order(order_id=1)
