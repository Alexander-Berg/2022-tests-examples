from datetime import datetime, timezone
from decimal import Decimal
from operator import itemgetter

import pytest

from maps_adv.billing_proxy.lib.db.enums import CampaignType, CurrencyType, PlatformType

pytestmark = [pytest.mark.asyncio]


def _sorted_order_ids(orders):
    return sorted(map(itemgetter("id"), orders))


async def test_returns_agency_orders(factory, agency, orders_dm):
    order1 = await factory.create_order(agency_id=agency["id"])
    order2 = await factory.create_order(agency_id=agency["id"])

    result = await orders_dm.list_agency_orders(agency["id"])

    assert _sorted_order_ids(result) == sorted([order1["id"], order2["id"]])


async def test_return_data(factory, agency, orders_dm):
    product1 = await factory.create_product()
    order1 = await factory.create_order(
        agency_id=agency["id"], product_id=product1["id"]
    )
    product2 = await factory.create_product()
    order2 = await factory.create_order(
        agency_id=agency["id"], product_id=product2["id"]
    )

    result = await orders_dm.list_agency_orders(agency["id"])

    expected_result = [
        {
            "id": order1["id"],
            "external_id": order1["external_id"],
            "service_id": order1["service_id"],
            "created_at": order1["created_at"],
            "tid": order1["tid"],
            "title": order1["title"],
            "act_text": order1["act_text"],
            "text": order1["text"],
            "comment": order1["comment"],
            "client_id": order1["client_id"],
            "agency_id": order1["agency_id"],
            "contract_id": order1["contract_id"],
            "product_id": order1["product_id"],
            "limit": order1["limit"],
            "consumed": order1["consumed"],
            "campaign_type": CampaignType(product1["campaign_type"]),
            "platforms": list(PlatformType(p) for p in product1["platforms"]),
            "currency": CurrencyType(product1["currency"]),
            "type": "REGULAR",
        },
        {
            "id": order2["id"],
            "external_id": order2["external_id"],
            "service_id": order2["service_id"],
            "created_at": order2["created_at"],
            "tid": order2["tid"],
            "title": order2["title"],
            "act_text": order2["act_text"],
            "text": order2["text"],
            "comment": order2["comment"],
            "client_id": order2["client_id"],
            "agency_id": order2["agency_id"],
            "contract_id": order2["contract_id"],
            "product_id": order2["product_id"],
            "limit": order2["limit"],
            "consumed": order2["consumed"],
            "campaign_type": CampaignType(product2["campaign_type"]),
            "platforms": list(PlatformType(p) for p in product2["platforms"]),
            "currency": CurrencyType(product2["currency"]),
            "type": "REGULAR",
        },
    ]

    assert sorted(result, key=itemgetter("id")) == sorted(
        expected_result, key=itemgetter("id")
    )


async def test_returns_sorted_by_created_at(factory, agency, orders_dm):
    client_1 = await factory.create_client(id=2)
    contract_1 = await factory.create_contract(
        id=222, client_id=agency["id"], external_id="222/222"
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
        agency_id=agency["id"],
        contract_id=contract_1["id"],
        product_id=product_1["id"],
        consumed=Decimal("2000"),
        limit=Decimal("200"),
        created_at=datetime(2000, 1, 1, tzinfo=timezone.utc),
    )

    client_2 = await factory.create_client(id=1)
    contract_2 = await factory.create_contract(
        id=111, client_id=agency["id"], external_id="111/111"
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
        agency_id=agency["id"],
        contract_id=contract_2["id"],
        product_id=product_2["id"],
        consumed=Decimal("1000"),
        limit=Decimal("100"),
        created_at=datetime(2000, 2, 2, tzinfo=timezone.utc),
    )

    client_3 = await factory.create_client(id=3)
    contract_3 = await factory.create_contract(
        id=333, client_id=agency["id"], external_id="333/333"
    )
    product_3 = await factory.create_product()
    order_3 = await factory.create_order(
        id=300,
        external_id=200,
        service_id=110,
        tid=300,
        title="ccc",
        act_text="ccc",
        text="ccc",
        client_id=client_3["id"],
        agency_id=agency["id"],
        contract_id=contract_3["id"],
        product_id=product_3["id"],
        consumed=Decimal("3000"),
        limit=Decimal("300"),
        created_at=datetime(2000, 3, 3, tzinfo=timezone.utc),
    )

    result = await orders_dm.list_agency_orders(agency["id"])

    orders_ids = list(map(itemgetter("id"), result))
    assert orders_ids == [order_3["id"], order_2["id"], order_1["id"]]


async def test_returns_sorted_by_created_at_for_internal_agency(factory, orders_dm):
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
    contract_2 = await factory.create_contract(
        id=111, client_id=client_2["id"], external_id="111/111"
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
        agency_id=None,
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

    result = await orders_dm.list_agency_orders(None)

    orders_ids = list(map(itemgetter("id"), result))
    assert orders_ids == [order_3["id"], order_2["id"], order_1["id"]]


async def test_returns_agency_orders_of_all_clients(factory, agency, orders_dm):
    client1 = await factory.create_client()
    order1 = await factory.create_order(agency_id=agency["id"], client_id=client1["id"])
    client2 = await factory.create_client()
    order2 = await factory.create_order(agency_id=agency["id"], client_id=client2["id"])

    result = await orders_dm.list_agency_orders(agency["id"])

    assert _sorted_order_ids(result) == sorted([order1["id"], order2["id"]])


async def test_returns_empty_list_if_no_orders(factory, agency, orders_dm):
    assert await orders_dm.list_agency_orders(agency["id"]) == []


async def test_not_returns_other_agency_orders(factory, agency, orders_dm):
    other_agency = await factory.create_agency()
    await factory.create_order(agency_id=other_agency["id"])

    assert await orders_dm.list_agency_orders(agency["id"]) == []


async def test_returns_orders_for_internal_agency(factory, orders_dm):
    internal_order = await factory.create_order(agency_id=None)

    result = await orders_dm.list_agency_orders(None)

    assert _sorted_order_ids(result) == sorted([internal_order["id"]])


async def test_returns_client_orders(factory, agency, client, orders_dm):
    order = await factory.create_order(agency_id=agency["id"], client_id=client["id"])

    result = await orders_dm.list_agency_orders(agency["id"], client["id"])

    assert _sorted_order_ids(result) == sorted([order["id"]])


async def test_not_returns_other_client_orders(factory, agency, client, orders_dm):
    other_client = await factory.create_client()
    await factory.create_order(agency_id=agency["id"], client_id=other_client["id"])

    assert await orders_dm.list_agency_orders(agency["id"], client["id"]) == []


async def test_not_returns_other_agency_orders_for_this_client(
    factory, agency, client, orders_dm
):
    other_agency = await factory.create_agency()
    await factory.create_order(agency_id=other_agency["id"], client_id=client["id"])

    assert await orders_dm.list_agency_orders(agency["id"], client["id"]) == []


async def test_not_returns_hidden_agency_orders(factory, agency, orders_dm):
    order1 = await factory.create_order(agency_id=agency["id"], hidden=False)
    await factory.create_order(agency_id=agency["id"], hidden=True)
    order3 = await factory.create_order(agency_id=agency["id"], hidden=False)

    result = await orders_dm.list_agency_orders(agency["id"])

    assert _sorted_order_ids(result) == sorted([order1["id"], order3["id"]])


async def test_not_returns_hidden_agency_client_orders(
    factory, agency, client, orders_dm
):
    order1 = await factory.create_order(
        agency_id=agency["id"], client_id=client["id"], hidden=False
    )
    await factory.create_order(
        agency_id=agency["id"], client_id=client["id"], hidden=True
    )
    order3 = await factory.create_order(
        agency_id=agency["id"], client_id=client["id"], hidden=False
    )

    result = await orders_dm.list_agency_orders(agency["id"], client["id"])

    assert _sorted_order_ids(result) == sorted([order1["id"], order3["id"]])


async def test_not_returns_hidden_internal_orders(factory, orders_dm):
    order1 = await factory.create_order(agency_id=None, hidden=False)
    await factory.create_order(agency_id=None, hidden=True)
    order3 = await factory.create_order(agency_id=None, hidden=False)

    result = await orders_dm.list_agency_orders(None)

    assert _sorted_order_ids(result) == sorted([order1["id"], order3["id"]])


async def test_not_returns_hidden_internal_client_orders(factory, client, orders_dm):
    order1 = await factory.create_order(
        agency_id=None, client_id=client["id"], hidden=False
    )
    await factory.create_order(agency_id=None, client_id=client["id"], hidden=True)
    order3 = await factory.create_order(
        agency_id=None, client_id=client["id"], hidden=False
    )

    result = await orders_dm.list_agency_orders(None, client["id"])

    assert _sorted_order_ids(result) == sorted([order1["id"], order3["id"]])
