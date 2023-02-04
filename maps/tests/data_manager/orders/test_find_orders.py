from datetime import datetime, timezone
from decimal import Decimal
from operator import itemgetter
from unittest.mock import ANY

import pytest

pytestmark = [pytest.mark.asyncio]


async def test_returns_found_data(factory, orders_dm):
    order_1 = await factory.create_order()
    order_2 = await factory.create_order()
    await factory.create_order()

    result = await orders_dm.find_orders([order_1["id"], order_2["id"], 99999])

    assert sorted(result, key=itemgetter("id")) == sorted(
        [
            {
                "id": order_1["id"],
                "external_id": order_1["external_id"],
                "service_id": order_1["service_id"],
                "created_at": order_1["created_at"],
                "tid": order_1["tid"],
                "title": order_1["title"],
                "act_text": order_1["act_text"],
                "text": order_1["text"],
                "comment": order_1["comment"],
                "client_id": order_1["client_id"],
                "agency_id": order_1["agency_id"],
                "contract_id": order_1["contract_id"],
                "product_id": order_1["product_id"],
                "limit": order_1["limit"],
                "consumed": order_1["consumed"],
                "campaign_type": ANY,
                "platforms": ANY,
                "currency": ANY,
                "type": "REGULAR",
            },
            {
                "id": order_2["id"],
                "external_id": order_2["external_id"],
                "service_id": order_2["service_id"],
                "created_at": order_2["created_at"],
                "tid": order_2["tid"],
                "title": order_2["title"],
                "act_text": order_2["act_text"],
                "text": order_2["text"],
                "comment": order_2["comment"],
                "client_id": order_2["client_id"],
                "agency_id": order_2["agency_id"],
                "contract_id": order_2["contract_id"],
                "product_id": order_2["product_id"],
                "limit": order_2["limit"],
                "consumed": order_2["consumed"],
                "campaign_type": ANY,
                "platforms": ANY,
                "currency": ANY,
                "type": "REGULAR",
            },
        ],
        key=itemgetter("id"),
    )


async def test_returns_sorted_by_created_at(factory, orders_dm):
    client_1 = await factory.create_client(id=2)
    agency_1 = await factory.create_client(id=22)
    contract_1 = await factory.create_contract(
        id=222, client_id=agency_1["id"], external_id="222/222"
    )
    product_1 = await factory.create_product()
    order_1 = await factory.create_order(
        id=200,
        external_id=200,
        service_id=110,
        tid=200,
        title="bbb",
        act_text="bbb",
        text="bbb",
        client_id=client_1["id"],
        agency_id=agency_1["id"],
        contract_id=contract_1["id"],
        product_id=product_1["id"],
        consumed=Decimal("2000"),
        limit=Decimal("200"),
        created_at=datetime(2000, 1, 1, tzinfo=timezone.utc),
    )

    client_2 = await factory.create_client(id=1)
    agency_2 = await factory.create_client(id=11)
    contract_2 = await factory.create_contract(
        id=111, client_id=agency_2["id"], external_id="111/111"
    )
    product_2 = await factory.create_product()
    order_2 = await factory.create_order(
        id=100,
        external_id=100,
        service_id=110,
        tid=100,
        title="aaa",
        act_text="aaa",
        text="aaa",
        client_id=client_2["id"],
        agency_id=agency_2["id"],
        contract_id=contract_2["id"],
        product_id=product_2["id"],
        consumed=Decimal("1000"),
        limit=Decimal("100"),
        created_at=datetime(2000, 2, 2, tzinfo=timezone.utc),
    )

    client_3 = await factory.create_client(id=3)
    agency_3 = await factory.create_client(id=37)
    contract_3 = await factory.create_contract(
        id=333, client_id=agency_3["id"], external_id="333/333"
    )
    product_3 = await factory.create_product()
    order_3 = await factory.create_order(
        id=300,
        external_id=300,
        service_id=110,
        tid=300,
        title="ccc",
        act_text="ccc",
        text="ccc",
        client_id=client_3["id"],
        agency_id=agency_3["id"],
        contract_id=contract_3["id"],
        product_id=product_3["id"],
        consumed=Decimal("3000"),
        limit=Decimal("300"),
        created_at=datetime(2000, 3, 3, tzinfo=timezone.utc),
    )

    result = await orders_dm.find_orders([order_1["id"], order_2["id"], order_3["id"]])

    orders_ids = list(map(itemgetter("id"), result))
    assert orders_ids == [order_3["id"], order_2["id"], order_1["id"]]


async def test_returns_sorted_by_created_at_with_null_fields(factory, orders_dm):
    client_1 = await factory.create_client(id=2)
    contract_1 = await factory.create_contract(
        id=222, client_id=client_1["id"], external_id="222/222"
    )
    product_1 = await factory.create_product()
    order_1 = await factory.create_order(
        id=200,
        external_id=200,
        service_id=110,
        tid=200,
        title="bbb",
        act_text="bbb",
        text="bbb",
        client_id=client_1["id"],
        agency_id=None,
        contract_id=contract_1["id"],
        product_id=product_1["id"],
        consumed=Decimal("2000"),
        limit=Decimal("200"),
        created_at=datetime(2000, 1, 1, tzinfo=timezone.utc),
    )

    client_2 = await factory.create_client(id=1)
    agency_2 = await factory.create_client(id=11)
    contract_2 = await factory.create_contract(
        id=111, client_id=agency_2["id"], external_id="111/111"
    )
    product_2 = await factory.create_product()
    order_2 = await factory.create_order(
        id=100,
        external_id=100,
        service_id=110,
        tid=100,
        title="aaa",
        act_text="aaa",
        text="aaa",
        client_id=client_2["id"],
        agency_id=agency_2["id"],
        contract_id=contract_2["id"],
        product_id=product_2["id"],
        consumed=Decimal("1000"),
        limit=Decimal("100"),
        created_at=datetime(2000, 2, 2, tzinfo=timezone.utc),
    )

    client_3 = await factory.create_client(id=3)
    contract_3 = await factory.create_contract(
        id=333, client_id=client_3["id"], external_id="333/333"
    )
    product_3 = await factory.create_product()
    order_3 = await factory.create_order(
        id=300,
        external_id=300,
        service_id=110,
        tid=300,
        title="ccc",
        act_text="ccc",
        text="ccc",
        client_id=client_3["id"],
        agency_id=None,
        contract_id=contract_3["id"],
        product_id=product_3["id"],
        consumed=Decimal("3000"),
        limit=Decimal("300"),
        created_at=datetime(2000, 3, 3, tzinfo=timezone.utc),
    )

    result = await orders_dm.find_orders([order_1["id"], order_2["id"], order_3["id"]])

    orders_ids = list(map(itemgetter("id"), result))
    assert orders_ids == [order_3["id"], order_2["id"], order_1["id"]]


@pytest.mark.parametrize("order_ids", ([], [555]))
async def test_returns_nothing_if_no_orders_was_found(order_ids, orders_dm):
    assert await orders_dm.find_orders(order_ids) == []


async def test_not_returns_hidden_orders(factory, orders_dm):
    order1 = await factory.create_order(hidden=False)
    order2 = await factory.create_order(hidden=True)
    order3 = await factory.create_order(hidden=False)

    result = await orders_dm.find_orders([order1["id"], order2["id"], order3["id"]])

    orders_ids = list(map(itemgetter("id"), result))
    assert orders_ids == [order3["id"], order1["id"]]
