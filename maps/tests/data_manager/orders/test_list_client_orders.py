from operator import itemgetter

import pytest

pytestmark = [pytest.mark.asyncio]


def _sorted_order_ids(orders):
    return sorted(map(itemgetter("id"), orders))


async def test_returns_client_orders(factory, client, orders_dm):
    order1 = await factory.create_order(
        agency_id=None, contract_id=None, client_id=client["id"]
    )
    order2 = await factory.create_order(agency_id=None, client_id=client["id"])
    order3 = await factory.create_order(contract_id=None, client_id=client["id"])
    order4 = await factory.create_order(client_id=client["id"])

    result = await orders_dm.list_client_orders(client["id"])

    assert _sorted_order_ids(result) == sorted(
        [order1["id"], order2["id"], order3["id"], order4["id"]]
    )


async def test_returns_no_orders(client, orders_dm):
    assert not await orders_dm.list_client_orders(client["id"])
