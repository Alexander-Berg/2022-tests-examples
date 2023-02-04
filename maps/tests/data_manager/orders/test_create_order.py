from datetime import datetime
from decimal import Decimal

import pytest

from maps_adv.billing_proxy.lib.core.balance_client import BalanceApiError
from maps_adv.billing_proxy.lib.db.enums import CampaignType, CurrencyType, PlatformType
from maps_adv.billing_proxy.tests.helpers import Any
from smb.common.http_client import UnknownResponse

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
async def create_data(client, agency, agency_contract, product):
    return {
        "service_id": 110,
        "title": "Заголовок заказа",
        "text": "Текст в заказе",
        "comment": "Комментарий к заказу",
        "client_id": client["id"],
        "agency_id": agency["id"],
        "contract_id": agency_contract["id"],
        "product_id": product["id"],
    }


async def test_creates_order_locally(factory, create_data, orders_dm):
    await orders_dm.create_order(**create_data)

    assert await factory.get_all_orders() == [
        {
            "id": Any(int),
            "external_id": Any(int),
            "service_id": 110,
            "created_at": Any(datetime),
            "tid": 0,
            "title": create_data["title"],
            "act_text": "",
            "text": create_data["text"],
            "comment": create_data["comment"],
            "client_id": create_data["client_id"],
            "agency_id": create_data["agency_id"],
            "contract_id": create_data["contract_id"],
            "product_id": create_data["product_id"],
            "parent_order_id": None,
            "limit": Decimal("0"),
            "consumed": Decimal("0"),
            "hidden": False,
        }
    ]


async def test_creates_order_with_external_id_equal_to_id(
    factory, create_data, orders_dm
):
    await orders_dm.create_order(**create_data)

    order = (await factory.get_all_orders())[0]
    assert order["external_id"] == order["id"]


@pytest.mark.geoproduct_product
async def test_creates_order_with_geoproduct_external_id(
    factory, create_data, orders_dm, geoproduct_client
):
    geoproduct_client.create_order_for_media_platform.return_value = 123
    create_data["service_id"] = 37
    create_data["contract_id"] = None

    await orders_dm.create_order(**create_data)

    order = (await factory.get_all_orders())[0]
    assert order["external_id"] == 123


async def test_returns_created_order_data(factory, create_data, product, orders_dm):
    result = await orders_dm.create_order(**create_data)

    created_order = (await factory.get_all_orders())[0]
    assert result == {
        "id": created_order["id"],
        "external_id": created_order["external_id"],
        "service_id": created_order["service_id"],
        "created_at": created_order["created_at"],
        "tid": created_order["tid"],
        "title": created_order["title"],
        "act_text": "",
        "text": created_order["text"],
        "comment": created_order["comment"],
        "client_id": created_order["client_id"],
        "agency_id": created_order["agency_id"],
        "contract_id": created_order["contract_id"],
        "product_id": created_order["product_id"],
        "limit": created_order["limit"],
        "consumed": created_order["consumed"],
        "campaign_type": CampaignType(product["campaign_type"]),
        "platforms": list(PlatformType(p) for p in product["platforms"]),
        "currency": CurrencyType(product["currency"]),
    }


async def test_creates_order_in_balance(
    create_data, product, orders_dm, balance_client
):
    result = await orders_dm.create_order(**create_data)

    balance_client.create_order.assert_called_with(
        order_id=result["id"],
        client_id=create_data["client_id"],
        agency_id=create_data["agency_id"],
        oracle_product_id=product["oracle_id"],
        contract_id=create_data["contract_id"],
        text=create_data["text"],
    )


async def test_not_creates_locally_if_balance_fails(
    factory, create_data, orders_dm, balance_client
):
    balance_client.create_order.coro.side_effect = BalanceApiError()

    try:
        await orders_dm.create_order(**create_data)
    except BalanceApiError:
        pass

    assert await factory.get_all_orders() == []


@pytest.mark.geoproduct_product
async def test_creates_order_in_geoproduct(
    create_data, product, orders_dm, geoproduct_client
):
    create_data["service_id"] = 37
    create_data["contract_id"] = None
    geoproduct_client.create_order_for_media_platform.return_value = 100500

    await orders_dm.create_order(**create_data)

    geoproduct_client.create_order_for_media_platform.assert_called_with(
        operator_id=111,
        client_id=create_data["client_id"],
        product_id=product["oracle_id"],
    )


async def test_not_creates_locally_if_geoproduct_fails(
    factory, create_data, orders_dm, geoproduct_client
):
    geoproduct_client.create_order_for_media_platform.side_effect = UnknownResponse(
        403, None, None
    )
    create_data["service_id"] = 37
    create_data["contract_id"] = None

    try:
        await orders_dm.create_order(**create_data)
    except UnknownResponse:
        pass

    assert await factory.get_all_orders() == []
