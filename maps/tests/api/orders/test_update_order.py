import pytest

from maps_adv.billing_proxy.lib.domain import CampaignType, CurrencyType, PlatformType
from maps_adv.billing_proxy.proto import common_pb2, orders_pb2
from maps_adv.billing_proxy.tests.helpers import (
    convert_internal_enum_to_proto,
    dt_to_proto,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("db")]

API_URL = "/orders/{}/"


@pytest.mark.parametrize(
    "values",
    [
        {"title": "new title", "text": "new text", "comment": "new comment"},
        {"title": "", "text": "", "comment": ""},
    ],
)
async def test_updates_passed_order_data(values, api, factory):
    order = await factory.create_order()

    input_pb = orders_pb2.OrderUpdateInput(
        title=values["title"], text=values["text"], comment=values["comment"]
    )

    await api.put(
        API_URL.format(order["id"]),
        input_pb,
        decode_as=orders_pb2.Order,
        allowed_status_codes=[200],
    )

    updated_order = {**order, **values}
    assert await factory.get_order(order["id"]) == updated_order


@pytest.mark.parametrize(
    "values",
    [
        {"title": "new title", "text": "new text", "comment": "new comment"},
        {"title": "", "text": "", "comment": ""},
    ],
)
async def test_returns_updated_order_data(values, api, factory, product):
    order = await factory.create_order(product_id=product["id"])

    input_pb = orders_pb2.OrderUpdateInput(
        title=values["title"], text=values["text"], comment=values["comment"]
    )

    result = await api.put(
        API_URL.format(order["id"]),
        input_pb,
        decode_as=orders_pb2.Order,
        allowed_status_codes=[200],
    )

    assert result == orders_pb2.Order(
        id=order["id"],
        title=values["title"],
        created_at=dt_to_proto(order["created_at"]),
        client_id=order["client_id"],
        agency_id=order["agency_id"],
        contract_id=order["contract_id"],
        product_id=order["product_id"],
        text=values["text"],
        comment=values["comment"],
        OBSOLETE__limit=common_pb2.MoneyQuantity(value=int(order["limit"] * 10000)),
        limit=str(order["limit"]),
        OBSOLETE__consumed=common_pb2.MoneyQuantity(
            value=int(order["consumed"] * 10000)
        ),
        consumed=str(order["consumed"]),
        external_id=order["id"],
        service_id=110,
        campaign_type=convert_internal_enum_to_proto(
            CampaignType(product["campaign_type"])
        ),
        platform=convert_internal_enum_to_proto(PlatformType(product["platform"])),
        platforms=convert_internal_enum_to_proto(
            list(PlatformType(p) for p in product["platforms"])
        ),
        currency=convert_internal_enum_to_proto(CurrencyType(product["currency"])),
    )


async def test_raises_if_order_does_not_exists(api, factory):
    order = await factory.create_order()
    input_pb = orders_pb2.OrderUpdateInput(
        title="title", text="text", comment="comment"
    )

    await api.put(API_URL.format(order["id"] + 1), input_pb, allowed_status_codes=[404])
