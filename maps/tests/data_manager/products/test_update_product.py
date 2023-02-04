from datetime import datetime, timezone
from decimal import Decimal

import pytest

from maps_adv.billing_proxy.lib.db.enums import (
    BillingType,
    CampaignType,
    CurrencyType,
    PlatformType,
    DbEnumConverter,
)

from maps_adv.billing_proxy.lib.data_manager.exceptions import ConflictingProducts

pytestmark = [pytest.mark.asyncio]


async def test_updates_product_fields(product, faker, products_dm):
    new_values = {
        "title": faker.text(max_nb_chars=256),
        "act_text": faker.text(max_nb_chars=256),
        "description": faker.text(max_nb_chars=256),
        "vat_value": Decimal("1"),
        "comment": faker.text(max_nb_chars=256),
    }

    result = await products_dm.find_product(product["id"])

    await products_dm.update_product(
        product_id=product["id"],
        service_id=product["service_id"],
        campaign_type=DbEnumConverter.to_enum(CampaignType, product["campaign_type"]),
        currency=DbEnumConverter.to_enum(CurrencyType, product["currency"]),
        type=product["type"],
        versions=result["versions"],
        **new_values,
    )

    updated_result = await products_dm.find_product(product["id"])
    result.update(new_values)
    assert updated_result == result


async def test_updates_versions(product, products_dm):
    new_versions = [
        {
            "active_from": datetime(2021, 10, 10, tzinfo=timezone.utc),
            "active_to": datetime(2021, 10, 11, tzinfo=timezone.utc),
            "billing_data": {"base_cpm": "25.0000"},
            "min_budget": Decimal("0.000000"),
            "cpm_filters": [],
        },
        {
            "active_from": datetime(2021, 10, 12, tzinfo=timezone.utc),
            "active_to": datetime(2021, 10, 13, tzinfo=timezone.utc),
            "billing_data": {"base_cpm": "250.0000"},
            "min_budget": Decimal("100.000000"),
            "cpm_filters": ["asdf"],
        },
        {
            "active_from": datetime(2021, 10, 12, tzinfo=timezone.utc),
            "active_to": None,
            "billing_data": {"base_cpm": "2500.0000"},
            "min_budget": Decimal("1000.000000"),
            "cpm_filters": [],
        },
    ]

    await products_dm.update_product(
        product_id=product["id"],
        title=product["title"],
        act_text=product["act_text"],
        description=product["description"],
        vat_value=product["vat_value"],
        comment=product["comment"],
        service_id=product["service_id"],
        campaign_type=DbEnumConverter.to_enum(CampaignType, product["campaign_type"]),
        currency=DbEnumConverter.to_enum(CurrencyType, product["currency"]),
        type=product["type"],
        versions=new_versions,
    )

    result = await products_dm.find_product(product["id"])
    updated_result = await products_dm.find_product(product["id"])

    result["versions"].clear()
    for i, version in enumerate(new_versions):
        version.update(
            product_id=product["id"], billing_type=BillingType.CPM, version=i + 1
        )
        result["versions"].append(version)

    assert updated_result == result


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
        "active_to": datetime(2021, 2, 1, tzinfo=timezone.utc),
        "billing_type": BillingType.CPM,
        "billing_data": {"cpm": {"base_cpm": 100}},
        "min_budget": Decimal("10.000000"),
        "cpm_filters": [],
        "service_id": 100,
        "type": "REGULAR",
    }


async def test_refuses_overlapping_products(products_dm, create_data):
    product_1_id = (await products_dm.create_product(**create_data))["product_id"]
    create_data["active_from"] = datetime(2021, 2, 1, tzinfo=timezone.utc)
    create_data["active_to"] = datetime(2021, 3, 1, tzinfo=timezone.utc)
    product_2_id = (await products_dm.create_product(**create_data))["product_id"]
    product_2 = await products_dm.find_product(product_2_id)

    new_versions = [
        {
            "active_from": datetime(2021, 1, 31, tzinfo=timezone.utc),
            "active_to": datetime(2021, 2, 1, tzinfo=timezone.utc),
            "billing_data": {"base_cpm": "25.0000"},
            "min_budget": Decimal("0.000000"),
            "cpm_filters": [],
        },
        {
            "active_from": datetime(2021, 2, 1, tzinfo=timezone.utc),
            "active_to": datetime(2021, 3, 1, tzinfo=timezone.utc),
            "billing_data": {"base_cpm": "250.0000"},
            "min_budget": Decimal("100.000000"),
            "cpm_filters": ["asdf"],
        },
    ]

    with pytest.raises(ConflictingProducts) as excepiton:
        await products_dm.update_product(
            product_id=product_2["id"],
            title=product_2["title"],
            act_text=product_2["act_text"],
            description=product_2["description"],
            vat_value=product_2["vat_value"],
            comment=product_2["comment"],
            service_id=product_2["service_id"],
            campaign_type=product_2["campaign_type"],
            currency=product_2["currency"],
            type=product_2["type"],
            versions=new_versions,
        )
    assert excepiton.value.product_ids == [product_1_id]

    new_versions = [
        {
            "active_from": datetime(2020, 12, 30, tzinfo=timezone.utc),
            "active_to": datetime(2020, 12, 31, tzinfo=timezone.utc),
            "billing_data": {"base_cpm": "25.0000"},
            "min_budget": Decimal("0.000000"),
            "cpm_filters": [],
        },
        {
            "active_from": datetime(2020, 12, 31, tzinfo=timezone.utc),
            "active_to": datetime(2021, 1, 2, tzinfo=timezone.utc),
            "billing_data": {"base_cpm": "250.0000"},
            "min_budget": Decimal("100.000000"),
            "cpm_filters": ["asdf"],
        },
    ]

    with pytest.raises(ConflictingProducts) as excepiton:
        await products_dm.update_product(
            product_id=product_2["id"],
            title=product_2["title"],
            act_text=product_2["act_text"],
            description=product_2["description"],
            vat_value=product_2["vat_value"],
            comment=product_2["comment"],
            service_id=product_2["service_id"],
            campaign_type=product_2["campaign_type"],
            currency=product_2["currency"],
            type=product_2["type"],
            versions=new_versions,
        )
    assert excepiton.value.product_ids == [product_1_id]
