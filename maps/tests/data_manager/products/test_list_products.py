from datetime import datetime, timedelta, timezone
from decimal import Decimal
from operator import itemgetter

import pytest

from maps_adv.billing_proxy.lib.db.enums import (
    BillingType,
    CampaignType,
    CurrencyType,
    PlatformType,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("db")]


async def test_returns_products(factory, products_dm):
    product1 = await factory.create_product()
    product1_version = await factory.get_product_first_version(product1["id"])
    product2 = await factory.create_product()
    product2_version = await factory.get_product_first_version(product2["id"])

    result = await products_dm.list_products()

    assert sorted(result, key=itemgetter("id")) == sorted(
        [
            {
                "id": product1["id"],
                "oracle_id": product1["oracle_id"],
                "service_id": product1["service_id"],
                "version": product1_version["version"],
                "title": product1["title"],
                "act_text": product1["act_text"],
                "description": product1["description"],
                "currency": CurrencyType(product1["currency"]),
                "billing_type": BillingType(product1["billing_type"]),
                "billing_data": product1_version["billing_data"],
                "vat_value": product1["vat_value"],
                "campaign_type": CampaignType(product1["campaign_type"]),
                "platforms": list(PlatformType(p) for p in product1["platforms"]),
                "min_budget": product1_version["min_budget"],
                "cpm_filters": product1_version["cpm_filters"],
                "comment": product1["comment"],
                "available_for_agencies": product1["available_for_agencies"],
                "available_for_internal": product1["available_for_internal"],
                "active_from": product1_version["active_from"],
                "active_to": product1_version["active_to"],
                "versions": [
                    {
                        "active_from": product1_version["active_from"],
                        "active_to": product1_version["active_to"],
                        "billing_data": product1_version["billing_data"],
                        "billing_type": BillingType(product1["billing_type"]),
                        "cpm_filters": [],
                        "min_budget": product1_version["min_budget"],
                        "product_id": product1["id"],
                        "version": 1,
                    }
                ],
                "type": product1["type"],
            },
            {
                "id": product2["id"],
                "oracle_id": product2["oracle_id"],
                "service_id": product2["service_id"],
                "version": product2_version["version"],
                "title": product2["title"],
                "act_text": product2["act_text"],
                "description": product2["description"],
                "currency": CurrencyType(product2["currency"]),
                "billing_type": BillingType(product2["billing_type"]),
                "billing_data": product2_version["billing_data"],
                "vat_value": product2["vat_value"],
                "campaign_type": CampaignType(product2["campaign_type"]),
                "platforms": list(PlatformType(p) for p in product2["platforms"]),
                "min_budget": product2_version["min_budget"],
                "cpm_filters": product2_version["cpm_filters"],
                "comment": product2["comment"],
                "available_for_agencies": product2["available_for_agencies"],
                "available_for_internal": product2["available_for_internal"],
                "active_from": product2_version["active_from"],
                "active_to": product2_version["active_to"],
                "versions": [
                    {
                        "active_from": product2_version["active_from"],
                        "active_to": product2_version["active_to"],
                        "billing_data": product2_version["billing_data"],
                        "billing_type": BillingType(product2["billing_type"]),
                        "cpm_filters": [],
                        "min_budget": product2_version["min_budget"],
                        "product_id": product2["id"],
                        "version": 1,
                    }
                ],
                "type": product2["type"],
            },
        ],
        key=itemgetter("id"),
    )


async def test_returns_edge_active_dates_from_versions(factory, products_dm):
    product1 = await factory.create_product(_without_version_=True)
    await factory.create_product_version(
        product_id=product1["id"],
        version=1,
        active_from=datetime(2002, 4, 1, tzinfo=timezone.utc),
        active_to=datetime(2002, 5, 31, tzinfo=timezone.utc),
    )
    await factory.create_product_version(
        product_id=product1["id"],
        version=2,
        active_from=datetime(2002, 1, 1, tzinfo=timezone.utc),
        active_to=datetime(2002, 2, 28, tzinfo=timezone.utc),
    )
    await factory.create_product_version(
        product_id=product1["id"],
        version=3,
        active_from=datetime(2002, 7, 1, tzinfo=timezone.utc),
        active_to=datetime(2002, 8, 31, tzinfo=timezone.utc),
    )
    await factory.create_product_version(
        product_id=product1["id"],
        version=4,
        active_from=datetime(2002, 3, 10, tzinfo=timezone.utc),
        active_to=datetime(2002, 3, 20, tzinfo=timezone.utc),
    )
    product2 = await factory.create_product(_without_version_=True)
    await factory.create_product_version(
        product_id=product2["id"],
        version=1,
        active_from=datetime(2002, 4, 1, tzinfo=timezone.utc),
        active_to=datetime(2002, 5, 31, tzinfo=timezone.utc),
    )
    await factory.create_product_version(
        product_id=product2["id"],
        version=2,
        active_from=datetime(2002, 1, 15, tzinfo=timezone.utc),
        active_to=datetime(2002, 2, 28, tzinfo=timezone.utc),
    )
    await factory.create_product_version(
        product_id=product2["id"],
        version=3,
        active_from=datetime(2002, 7, 1, tzinfo=timezone.utc),
        active_to=None,
    )
    await factory.create_product_version(
        product_id=product2["id"],
        version=4,
        active_from=datetime(2002, 3, 10, tzinfo=timezone.utc),
        active_to=datetime(2002, 3, 20, tzinfo=timezone.utc),
    )

    result = await products_dm.list_products()

    testing_fields = {
        res["id"]: {"active_from": res["active_from"], "active_to": res["active_to"]}
        for res in result
    }
    assert testing_fields[product1["id"]] == {
        "active_from": datetime(2002, 1, 1, tzinfo=timezone.utc),
        "active_to": datetime(2002, 8, 31, tzinfo=timezone.utc),
    }
    assert testing_fields[product2["id"]] == {
        "active_from": datetime(2002, 1, 15, tzinfo=timezone.utc),
        "active_to": None,
    }


async def test_returns_version_fields_from_currently_active_version_or_from_latest_version(  # noqa: E501
    factory, products_dm
):
    product1 = await factory.create_product(_without_version_=True)
    await factory.create_product_version(
        product_id=product1["id"],
        version=1,
        billing_data={"base_cpm": "50"},
        min_budget=Decimal("1000"),
        cpm_filters=["filter_one"],
        active_from=datetime.now(tz=timezone.utc) - timedelta(days=3),
        active_to=datetime.now(tz=timezone.utc) - timedelta(days=2),
    )
    await factory.create_product_version(
        product_id=product1["id"],
        version=2,
        billing_data={"base_cpm": "60"},
        min_budget=Decimal("2000"),
        cpm_filters=["filter_two"],
        active_from=datetime.now(tz=timezone.utc) - timedelta(days=1),
        active_to=datetime.now(tz=timezone.utc) + timedelta(days=1),
    )
    await factory.create_product_version(
        product_id=product1["id"],
        version=3,
        billing_data={"base_cpm": "70"},
        min_budget=Decimal("3000"),
        cpm_filters=["filter_three"],
        active_from=datetime.now(tz=timezone.utc) + timedelta(days=2),
        active_to=datetime.now(tz=timezone.utc) + timedelta(days=3),
    )
    product2 = await factory.create_product(_without_version_=True)
    await factory.create_product_version(
        product_id=product2["id"],
        version=1,
        billing_data={"base_cpm": "50"},
        min_budget=Decimal("1000"),
        cpm_filters=["filter_one"],
        active_from=datetime.now(tz=timezone.utc) - timedelta(days=3),
        active_to=datetime.now(tz=timezone.utc) - timedelta(days=2),
    )
    await factory.create_product_version(
        product_id=product2["id"],
        version=3,
        billing_data={"base_cpm": "70"},
        min_budget=Decimal("3000"),
        cpm_filters=["filter_three"],
        active_from=datetime.now(tz=timezone.utc) + timedelta(days=2),
        active_to=datetime.now(tz=timezone.utc) + timedelta(days=3),
    )

    result = await products_dm.list_products()

    testing_fields = {
        res["id"]: {
            "version": res["version"],
            "billing_data": res["billing_data"],
            "min_budget": res["min_budget"],
            "cpm_filters": res["cpm_filters"],
        }
        for res in result
    }
    assert testing_fields[product1["id"]] == {
        "version": 2,
        "billing_data": {"base_cpm": "60"},
        "min_budget": Decimal("2000"),
        "cpm_filters": ["filter_two"],
    }
    assert testing_fields[product2["id"]] == {
        "version": 3,
        "billing_data": {"base_cpm": "70"},
        "min_budget": Decimal("3000"),
        "cpm_filters": ["filter_three"],
    }


async def test_returns_filtered_products(factory, products_dm):
    await factory.create_product(service_id=110)
    product2 = await factory.create_product(service_id=37)
    product2_version = await factory.get_product_first_version(product2["id"])

    result = await products_dm.list_products(service_ids=[37])

    assert sorted(result, key=itemgetter("id")) == sorted(
        [
            {
                "id": product2["id"],
                "oracle_id": product2["oracle_id"],
                "service_id": product2["service_id"],
                "version": product2_version["version"],
                "title": product2["title"],
                "act_text": product2["act_text"],
                "description": product2["description"],
                "currency": CurrencyType(product2["currency"]),
                "billing_type": BillingType(product2["billing_type"]),
                "billing_data": product2_version["billing_data"],
                "vat_value": product2["vat_value"],
                "campaign_type": CampaignType(product2["campaign_type"]),
                "platforms": list(PlatformType(p) for p in product2["platforms"]),
                "min_budget": product2_version["min_budget"],
                "cpm_filters": product2_version["cpm_filters"],
                "comment": product2["comment"],
                "available_for_agencies": product2["available_for_agencies"],
                "available_for_internal": product2["available_for_internal"],
                "active_from": product2_version["active_from"],
                "active_to": product2_version["active_to"],
                "versions": [
                    {
                        "active_from": product2_version["active_from"],
                        "active_to": product2_version["active_to"],
                        "billing_data": product2_version["billing_data"],
                        "billing_type": BillingType(product2["billing_type"]),
                        "cpm_filters": [],
                        "min_budget": product2_version["min_budget"],
                        "product_id": product2["id"],
                        "version": 1,
                    }
                ],
                "type": product2["type"],
            }
        ],
        key=itemgetter("id"),
    )


async def test_returns_empty_list_if_no_products_exist(products_dm):
    assert await products_dm.list_products() == []


async def test_returns_empty_list_if_no_service_ids_given(factory, products_dm):
    await factory.create_product(service_id=110)
    await factory.create_product(service_id=37)

    assert await products_dm.list_products(service_ids=[]) == []
