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

API_URL = "/orders/by-order-ids/"


async def test_returns_requested_orders(api, factory):
    product_1 = await factory.create_product()
    product_2 = await factory.create_product()

    order_1 = await factory.create_order(product_id=product_1["id"])
    order_2 = await factory.create_order(product_id=product_2["id"])
    await factory.create_order()

    input_pb = orders_pb2.OrderIds(order_ids=[order_1["id"], order_2["id"]])

    result = await api.post(
        API_URL, input_pb, decode_as=orders_pb2.Orders, allowed_status_codes=[200]
    )

    assert result == orders_pb2.Orders(
        orders=sorted(
            [
                orders_pb2.Order(
                    id=order_1["id"],
                    title=order_1["title"],
                    created_at=dt_to_proto(order_1["created_at"]),
                    client_id=order_1["client_id"],
                    agency_id=order_1["agency_id"],
                    contract_id=order_1["contract_id"],
                    product_id=order_1["product_id"],
                    text=order_1["text"],
                    comment=order_1["comment"],
                    OBSOLETE__limit=common_pb2.MoneyQuantity(
                        value=int(order_1["limit"] * 10000)
                    ),
                    limit=str(order_1["limit"]),
                    OBSOLETE__consumed=common_pb2.MoneyQuantity(
                        value=int(order_1["consumed"] * 10000)
                    ),
                    consumed=str(order_1["consumed"]),
                    external_id=order_1["id"],
                    service_id=110,
                    campaign_type=convert_internal_enum_to_proto(
                        CampaignType(product_1["campaign_type"])
                    ),
                    platform=convert_internal_enum_to_proto(
                        PlatformType(product_1["platform"])
                    ),
                    platforms=convert_internal_enum_to_proto(
                        list(PlatformType(p) for p in product_1["platforms"])
                    ),
                    currency=convert_internal_enum_to_proto(
                        CurrencyType(product_1["currency"])
                    ),
                ),
                orders_pb2.Order(
                    id=order_2["id"],
                    title=order_2["title"],
                    created_at=dt_to_proto(order_2["created_at"]),
                    client_id=order_2["client_id"],
                    agency_id=order_2["agency_id"],
                    contract_id=order_2["contract_id"],
                    product_id=order_2["product_id"],
                    text=order_2["text"],
                    comment=order_2["comment"],
                    OBSOLETE__limit=common_pb2.MoneyQuantity(
                        value=int(order_2["limit"] * 10000)
                    ),
                    limit=str(order_2["limit"]),
                    OBSOLETE__consumed=common_pb2.MoneyQuantity(
                        value=int(order_2["consumed"] * 10000)
                    ),
                    consumed=str(order_2["consumed"]),
                    external_id=order_2["id"],
                    service_id=110,
                    campaign_type=convert_internal_enum_to_proto(
                        CampaignType(product_2["campaign_type"])
                    ),
                    platform=convert_internal_enum_to_proto(
                        PlatformType(product_2["platform"])
                    ),
                    platforms=convert_internal_enum_to_proto(
                        list(PlatformType(p) for p in product_2["platforms"])
                    ),
                    currency=convert_internal_enum_to_proto(
                        CurrencyType(product_2["currency"])
                    ),
                ),
            ],
            key=lambda pb: attrgetter("seconds", "nanos")(pb.created_at),
            reverse=True,
        )
    )


async def test_returns_sorted_by_created_at(api, factory):
    client_1 = await factory.create_client(id=2)
    agency_1 = await factory.create_client(id=22)
    contract_1 = await factory.create_contract(
        id=222, client_id=agency_1["id"], external_id="222/222"
    )
    product_1 = await factory.create_product()
    order_1 = await factory.create_order(
        id=200,
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

    input_pb = orders_pb2.OrderIds(
        order_ids=[order_1["id"], order_2["id"], order_3["id"]]
    )

    result = await api.post(
        API_URL, input_pb, decode_as=orders_pb2.Orders, allowed_status_codes=[200]
    )

    orders_ids = list(map(attrgetter("id"), result.orders))
    assert orders_ids == [order_3["id"], order_2["id"], order_1["id"]]


async def test_returns_sorted_by_created_at_with_null_fields(api, factory):
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
        tid=300,
        title="ccc",
        act_text="ccc",
        text="ccc",
        client_id=client_3["id"],
        contract_id=contract_3["id"],
        product_id=product_3["id"],
        consumed=Decimal("3000"),
        limit=Decimal("300"),
        created_at=datetime(2000, 3, 3, tzinfo=timezone.utc),
    )

    input_pb = orders_pb2.OrderIds(
        order_ids=[order_1["id"], order_2["id"], order_3["id"]]
    )

    result = await api.post(
        API_URL, input_pb, decode_as=orders_pb2.Orders, allowed_status_codes=[200]
    )

    orders_ids = list(map(attrgetter("id"), result.orders))
    assert orders_ids == [order_3["id"], order_2["id"], order_1["id"]]


async def test_raises_for_unknown_order(api, factory):
    order = await factory.create_order()

    input_pb = orders_pb2.OrderIds(order_ids=[order["id"], 9999])

    await api.post(API_URL, input_pb, allowed_status_codes=[422])


async def test_raises_for_hidden_order(api, factory):
    order1 = await factory.create_order(hidden=False)
    order2 = await factory.create_order(hidden=True)

    input_pb = orders_pb2.OrderIds(order_ids=[order1["id"], order2["id"]])

    await api.post(API_URL, input_pb, allowed_status_codes=[422])


async def test_no_returns_if_found_nothing(api):
    input_pb = orders_pb2.OrderIds(order_ids=[])
    result = await api.post(
        API_URL, input_pb, decode_as=orders_pb2.Orders, allowed_status_codes=[200]
    )

    assert result == orders_pb2.Orders(orders=[])
