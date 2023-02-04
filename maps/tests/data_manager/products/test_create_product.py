from datetime import datetime, timezone
from decimal import Decimal

import pytest

from maps_adv.billing_proxy.lib.db.enums import (
    BillingType,
    CampaignType,
    CurrencyType,
    PlatformType,
)

from maps_adv.billing_proxy.lib.data_manager.exceptions import ConflictingProducts

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
async def create_data(faker):
    return {
        "oracle_id": 123,
        "title": faker.text(max_nb_chars=256),
        "act_text": faker.text(max_nb_chars=256),
        "description": faker.text(max_nb_chars=256),
        "currency": CurrencyType.BYN,
        "vat_value": Decimal("1"),
        "campaign_type": CampaignType.ZERO_SPEED_BANNER,
        "platform": PlatformType.NAVI,
        "platforms": [PlatformType.NAVI],
        "comment": faker.text(max_nb_chars=256),
        "active_from": datetime(2021, 1, 1, tzinfo=timezone.utc),
        "billing_type": BillingType.CPM,
        "billing_data": {"cpm": {"base_cpm": "100.5"}},
        "min_budget": Decimal("10.000000"),
        "cpm_filters": [],
        "service_id": 100,
        "type": faker.text(max_nb_chars=256),
    }


async def test_creates_product_and_version(products_dm, create_data):
    product_id = (await products_dm.create_product(**create_data))["product_id"]

    result = await products_dm.find_product(product_id)

    assert result == {
        "id": product_id,
        "version": 1,
        "act_text": create_data["act_text"],
        "active_from": create_data["active_from"],
        "active_to": None,
        "available_for_agencies": True,
        "available_for_internal": True,
        "billing_data": create_data["billing_data"],
        "billing_type": create_data["billing_type"],
        "campaign_type": create_data["campaign_type"],
        "comment": create_data["comment"],
        "cpm_filters": create_data["cpm_filters"],
        "currency": create_data["currency"],
        "dedicated_client_ids": None,
        "description": create_data["description"],
        "min_budget": create_data["min_budget"],
        "oracle_id": create_data["oracle_id"],
        "platforms": create_data["platforms"],
        "service_id": create_data["service_id"],
        "title": create_data["title"],
        "vat_value": create_data["vat_value"],
        "versions": [
            {
                "version": 1,
                "active_from": create_data["active_from"],
                "active_to": None,
                "billing_data": create_data["billing_data"],
                "billing_type": create_data["billing_type"],
                "cpm_filters": create_data["cpm_filters"],
                "min_budget": create_data["min_budget"],
                "product_id": product_id,
            }
        ],
        "type": create_data["type"],
    }


async def test_allows_duplicate_yearlong_products(products_dm, create_data):
    create_data["type"] = "YEARLONG"
    await products_dm.create_product(**create_data)
    await products_dm.create_product(**create_data)
    await products_dm.create_product(**create_data)


async def test_refuses_duplicate_regular_products(products_dm, create_data):
    create_data["type"] = "REGULAR"
    product_id = (await products_dm.create_product(**create_data))["product_id"]
    with pytest.raises(ConflictingProducts) as excepiton:
        await products_dm.create_product(**create_data)

    assert excepiton.value.product_ids == [product_id]


async def test_allows_disjoint_duplicate_regular_products(products_dm, create_data):
    create_data["type"] = "REGULAR"
    create_data["active_to"] = datetime(2021, 2, 1, tzinfo=timezone.utc)
    await products_dm.create_product(**create_data)

    create_data["active_from"] = datetime(2021, 2, 1, tzinfo=timezone.utc)
    create_data["active_to"] = datetime(2021, 3, 1, tzinfo=timezone.utc)
    await products_dm.create_product(**create_data)

    create_data["active_from"] = datetime(2020, 12, 1, tzinfo=timezone.utc)
    create_data["active_to"] = datetime(2021, 1, 1, tzinfo=timezone.utc)
    await products_dm.create_product(**create_data)


async def test_refuses_overlapping_regular_products(products_dm, create_data):
    create_data["type"] = "REGULAR"
    create_data["active_from"] = datetime(2021, 1, 1, tzinfo=timezone.utc)
    create_data["active_to"] = datetime(2021, 2, 1, tzinfo=timezone.utc)
    product_id = (await products_dm.create_product(**create_data))["product_id"]

    create_data["active_from"] = datetime(2021, 1, 31, tzinfo=timezone.utc)
    create_data["active_to"] = datetime(2021, 3, 1, tzinfo=timezone.utc)
    with pytest.raises(ConflictingProducts) as excepiton:
        await products_dm.create_product(**create_data)
    assert excepiton.value.product_ids == [product_id]

    create_data["active_from"] = datetime(2020, 12, 1, tzinfo=timezone.utc)
    create_data["active_to"] = datetime(2021, 1, 2, tzinfo=timezone.utc)
    with pytest.raises(ConflictingProducts) as excepiton:
        await products_dm.create_product(**create_data)
    assert excepiton.value.product_ids == [product_id]

    create_data["active_from"] = datetime(2021, 1, 10, tzinfo=timezone.utc)
    create_data["active_to"] = datetime(2021, 1, 20, tzinfo=timezone.utc)
    with pytest.raises(ConflictingProducts) as excepiton:
        await products_dm.create_product(**create_data)
    assert excepiton.value.product_ids == [product_id]


async def test_returns_all_conflicting_products(products_dm, create_data):
    create_data["type"] = "REGULAR"
    create_data["active_to"] = datetime(2021, 1, 31, tzinfo=timezone.utc)
    product_id_1 = (await products_dm.create_product(**create_data))["product_id"]

    create_data["active_from"] = datetime(2021, 3, 1, tzinfo=timezone.utc)
    create_data["active_to"] = datetime(2021, 3, 31, tzinfo=timezone.utc)
    product_id_2 = (await products_dm.create_product(**create_data))["product_id"]

    create_data["active_from"] = datetime(2021, 1, 30, tzinfo=timezone.utc)
    create_data["active_to"] = datetime(2021, 3, 2, tzinfo=timezone.utc)
    with pytest.raises(ConflictingProducts) as excepiton:
        await products_dm.create_product(**create_data)
    assert excepiton.value.product_ids == [product_id_1, product_id_2]
