from datetime import datetime, timezone
from decimal import Decimal
from operator import attrgetter

import pytest

from maps_adv.billing_proxy.lib.domain import CampaignType, CurrencyType, PlatformType
from maps_adv.billing_proxy.proto import common_pb2, orders_pb2
from maps_adv.billing_proxy.tests.helpers import (
    convert_internal_enum_to_proto,
    dt_to_proto,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("db")]

API_URL = "/agencies/internal/orders/"


def _sorted_order_ids(orders):
    return sorted(map(attrgetter("id"), orders.orders))


async def test_returns_internal_orders(api, factory):
    order1 = await factory.create_order(agency_id=None)
    order2 = await factory.create_order(agency_id=None)

    result = await api.get(
        API_URL, decode_as=orders_pb2.Orders, allowed_status_codes=[200]
    )

    assert _sorted_order_ids(result) == sorted([order1["id"], order2["id"]])


async def test_return_data(api, factory):
    product1 = await factory.create_product()
    order1 = await factory.create_order(agency_id=None, product_id=product1["id"])
    product2 = await factory.create_product()
    order2 = await factory.create_order(agency_id=None, product_id=product2["id"])

    result = await api.get(
        API_URL, decode_as=orders_pb2.Orders, allowed_status_codes=[200]
    )

    orders_submessages = sorted(
        [
            orders_pb2.Order(
                id=order1["id"],
                created_at=dt_to_proto(order1["created_at"]),
                title=order1["title"],
                text=order1["text"],
                comment=order1["comment"],
                client_id=order1["client_id"],
                agency_id=order1["agency_id"],
                contract_id=order1["contract_id"],
                product_id=order1["product_id"],
                OBSOLETE__limit=common_pb2.MoneyQuantity(
                    value=int(order1["limit"] * 10000)
                ),
                limit=str(order1["limit"]),
                OBSOLETE__consumed=common_pb2.MoneyQuantity(
                    value=int(order1["consumed"] * 10000)
                ),
                consumed=str(order1["consumed"]),
                external_id=order1["id"],
                service_id=110,
                campaign_type=convert_internal_enum_to_proto(
                    CampaignType(product1["campaign_type"])
                ),
                platform=convert_internal_enum_to_proto(
                    PlatformType(product1["platform"])
                ),
                platforms=convert_internal_enum_to_proto(
                    list(PlatformType(p) for p in product1["platforms"])
                ),
                currency=convert_internal_enum_to_proto(
                    CurrencyType(product1["currency"])
                ),
            ),
            orders_pb2.Order(
                id=order2["id"],
                created_at=dt_to_proto(order2["created_at"]),
                title=order2["title"],
                text=order2["text"],
                comment=order2["comment"],
                client_id=order2["client_id"],
                agency_id=order2["agency_id"],
                contract_id=order2["contract_id"],
                product_id=order2["product_id"],
                OBSOLETE__limit=common_pb2.MoneyQuantity(
                    value=int(order2["limit"] * 10000)
                ),
                limit=str(order2["limit"]),
                OBSOLETE__consumed=common_pb2.MoneyQuantity(
                    value=int(order2["consumed"] * 10000)
                ),
                consumed=str(order2["consumed"]),
                external_id=order2["id"],
                service_id=110,
                campaign_type=convert_internal_enum_to_proto(
                    CampaignType(product2["campaign_type"])
                ),
                platform=convert_internal_enum_to_proto(
                    PlatformType(product2["platform"])
                ),
                platforms=convert_internal_enum_to_proto(
                    list(PlatformType(p) for p in product2["platforms"])
                ),
                currency=convert_internal_enum_to_proto(
                    CurrencyType(product2["currency"])
                ),
            ),
        ],
        key=attrgetter("created_at.seconds", "created_at.nanos"),
        reverse=True,
    )
    expected_result = orders_pb2.Orders(orders=orders_submessages)

    assert result == expected_result


async def test_returns_sorted_by_created_at(api, factory):
    client_1 = await factory.create_client(id=2)
    contract_1 = await factory.create_contract(
        id=222, client_id=client_1["id"], external_id="222/222"
    )
    product_1 = await factory.create_product()
    order_1 = await factory.create_order(
        id=200,
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

    result = await api.get(
        API_URL, decode_as=orders_pb2.Orders, allowed_status_codes=[200]
    )

    orders_ids = list(map(attrgetter("id"), result.orders))
    assert orders_ids == [order_3["id"], order_2["id"], order_1["id"]]


async def test_returns_internal_orders_of_all_clients(api, factory):
    client1 = await factory.create_client()
    order1 = await factory.create_order(agency_id=None, client_id=client1["id"])
    client2 = await factory.create_client()
    order2 = await factory.create_order(agency_id=None, client_id=client2["id"])

    result = await api.get(
        API_URL, decode_as=orders_pb2.Orders, allowed_status_codes=[200]
    )

    assert _sorted_order_ids(result) == sorted([order1["id"], order2["id"]])


async def test_returns_empty_list_if_no_orders(api, factory):
    result = await api.get(
        API_URL, decode_as=orders_pb2.Orders, allowed_status_codes=[200]
    )

    assert result == orders_pb2.Orders(orders=[])


async def test_not_returns_other_agency_orders(api, factory):
    other_agency = await factory.create_agency()
    await factory.create_order(agency_id=other_agency["id"])

    result = await api.get(
        API_URL, decode_as=orders_pb2.Orders, allowed_status_codes=[200]
    )

    assert result == orders_pb2.Orders(orders=[])


async def test_returns_client_orders(api, factory, agency, client):
    order = await factory.create_order(agency_id=None, client_id=client["id"])

    result = await api.get(
        API_URL + f"?client_id={client['id']}",
        decode_as=orders_pb2.Orders,
        allowed_status_codes=[200],
    )

    assert _sorted_order_ids(result) == sorted([order["id"]])


async def test_not_returns_other_client_orders(api, factory, agency, client):
    other_client = await factory.create_client()
    await factory.create_order(client_id=other_client["id"])

    result = await api.get(
        API_URL + f"?client_id={client['id']}",
        decode_as=orders_pb2.Orders,
        allowed_status_codes=[200],
    )

    assert result == orders_pb2.Orders(orders=[])


async def test_not_returns_other_agency_orders_for_this_client(
    api, factory, agency, client
):
    other_agency = await factory.create_agency()
    await factory.create_order(agency_id=other_agency["id"], client_id=client["id"])

    result = await api.get(
        API_URL + f"?client_id={client['id']}",
        decode_as=orders_pb2.Orders,
        allowed_status_codes=[200],
    )

    assert result == orders_pb2.Orders(orders=[])


async def test_returns_error_for_inexistent_client(api, factory, agency):
    inexistent_id = await factory.get_inexistent_client_id()

    await api.get(
        API_URL + f"?client_id={inexistent_id}",
        expected_error=(
            common_pb2.Error.CLIENT_DOES_NOT_EXIST,
            f"client_id={inexistent_id}",
        ),
        allowed_status_codes=[422],
    )


async def test_not_returns_hidden_internal_orders(api, factory):
    order1 = await factory.create_order(agency_id=None, hidden=False)
    await factory.create_order(agency_id=None, hidden=True)
    order3 = await factory.create_order(agency_id=None, hidden=False)

    result = await api.get(
        API_URL, decode_as=orders_pb2.Orders, allowed_status_codes=[200]
    )

    assert _sorted_order_ids(result) == sorted([order1["id"], order3["id"]])


async def test_not_returns_hidden_internal_client_orders(api, factory, client):
    order1 = await factory.create_order(
        agency_id=None, client_id=client["id"], hidden=False
    )
    await factory.create_order(agency_id=None, client_id=client["id"], hidden=True)
    order3 = await factory.create_order(
        agency_id=None, client_id=client["id"], hidden=False
    )

    result = await api.get(
        API_URL + f"?client_id={client['id']}",
        decode_as=orders_pb2.Orders,
        allowed_status_codes=[200],
    )

    assert _sorted_order_ids(result) == sorted([order1["id"], order3["id"]])
