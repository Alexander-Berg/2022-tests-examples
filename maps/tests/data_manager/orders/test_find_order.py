import pytest

from maps_adv.billing_proxy.lib.db.enums import CampaignType, CurrencyType, PlatformType

pytestmark = [pytest.mark.asyncio]


async def test_returns_data_if_found(factory, product, orders_dm):
    order = await factory.create_order(product_id=product["id"])

    result = await orders_dm.find_order(order["id"])

    assert result == {
        "id": order["id"],
        "external_id": order["external_id"],
        "service_id": order["service_id"],
        "created_at": order["created_at"],
        "tid": order["tid"],
        "title": order["title"],
        "act_text": order["act_text"],
        "text": order["text"],
        "comment": order["comment"],
        "client_id": order["client_id"],
        "agency_id": order["agency_id"],
        "contract_id": order["contract_id"],
        "product_id": order["product_id"],
        "limit": order["limit"],
        "consumed": order["consumed"],
        "campaign_type": CampaignType(product["campaign_type"]),
        "platforms": list(PlatformType(p) for p in product["platforms"]),
        "currency": CurrencyType(product["currency"]),
        "type": "REGULAR",
    }


async def test_returns_none_if_not_found(orders_dm):
    assert await orders_dm.find_order(555) is None


async def test_returns_none_if_order_is_hidden(factory, orders_dm):
    order = await factory.create_order(hidden=True)

    assert await orders_dm.find_order(order["id"]) is None


async def test_returns_data_if_found_by_external_id(factory, product, orders_dm):
    order = await factory.create_order(product_id=product["id"])

    result = await orders_dm.find_order_by_external_id(order["external_id"])

    assert result == {
        "id": order["id"],
        "external_id": order["external_id"],
        "service_id": order["service_id"],
        "created_at": order["created_at"],
        "tid": order["tid"],
        "title": order["title"],
        "act_text": order["act_text"],
        "text": order["text"],
        "comment": order["comment"],
        "client_id": order["client_id"],
        "agency_id": order["agency_id"],
        "contract_id": order["contract_id"],
        "product_id": order["product_id"],
        "limit": order["limit"],
        "consumed": order["consumed"],
        "campaign_type": CampaignType(product["campaign_type"]),
        "platforms": list(PlatformType(p) for p in product["platforms"]),
        "currency": CurrencyType(product["currency"]),
        "type": "REGULAR",
    }


async def test_returns_none_if_not_found_by_external_id(orders_dm):
    assert await orders_dm.find_order_by_external_id(555) is None


async def test_returns_none_if_order_is_hidden_by_external_id(factory, orders_dm):
    order = await factory.create_order(hidden=True)

    assert await orders_dm.find_order_by_external_id(order["external_id"]) is None
