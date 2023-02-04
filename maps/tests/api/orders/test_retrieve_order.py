import pytest

from maps_adv.billing_proxy.lib.domain import CampaignType, CurrencyType, PlatformType
from maps_adv.billing_proxy.proto import common_pb2, orders_pb2
from maps_adv.billing_proxy.tests.helpers import (
    convert_internal_enum_to_proto,
    dt_to_proto,
)

pytestmark = [pytest.mark.asyncio]

API_URL = "/orders/{}/"
BY_EXTERNAL_ID_API_URL = "/orders/by-external-id/{}/"


async def test_returns_data_if_found(api, factory, product):
    order = await factory.create_order(product_id=product["id"])

    result = await api.get(
        API_URL.format(order["id"]),
        decode_as=orders_pb2.Order,
        allowed_status_codes=[200],
    )

    assert result == orders_pb2.Order(
        id=order["id"],
        title=order["title"],
        created_at=dt_to_proto(order["created_at"]),
        client_id=order["client_id"],
        agency_id=order["agency_id"],
        contract_id=order["contract_id"],
        product_id=order["product_id"],
        text=order["text"],
        comment=order["comment"],
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


async def test_raises_for_nonexistent_order(api):
    await api.get(API_URL.format(55), allowed_status_codes=[404])


async def test_raises_for_hidden_order(api, factory):
    order = await factory.create_order(hidden=True)

    await api.get(API_URL.format(order["id"]), allowed_status_codes=[404])


async def test_returns_data_if_found_by_external_id(api, factory, product):
    order = await factory.create_order(product_id=product["id"])

    result = await api.get(
        BY_EXTERNAL_ID_API_URL.format(order["external_id"]),
        decode_as=orders_pb2.Order,
        allowed_status_codes=[200],
    )

    assert result == orders_pb2.Order(
        id=order["id"],
        title=order["title"],
        created_at=dt_to_proto(order["created_at"]),
        client_id=order["client_id"],
        agency_id=order["agency_id"],
        contract_id=order["contract_id"],
        product_id=order["product_id"],
        text=order["text"],
        comment=order["comment"],
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


async def test_raises_for_nonexistent_order_by_external_id(api):
    await api.get(API_URL.format(55), allowed_status_codes=[404])


async def test_raises_for_hidden_order_by_external_id(api, factory):
    order = await factory.create_order(hidden=True)

    await api.get(API_URL.format(order["external_id"]), allowed_status_codes=[404])
