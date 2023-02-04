from datetime import datetime, timedelta, timezone
from decimal import Decimal

import pytest

from maps_adv.billing_proxy.lib.db.enums import (
    BillingType,
    CampaignType,
    CurrencyType,
    PlatformType,
)

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    ("client_ids", "expected_dedicated_clients"),
    [([], None), ([1], [1]), ([1, 3], [1, 3])],
)
async def test_return_data(
    factory, products_dm, client_ids, expected_dedicated_clients
):
    product = await factory.create_product(
        type="YEARLONG",
        service_id=110,
        billing_type=BillingType.CPM,
        billing_data={"base_cpm": "25.0000"},
    )
    for client_id in client_ids:
        client = await factory.create_client(id=client_id)
        await factory.restrict_product_by_client(product, client)
    product_version = await factory.get_product_first_version(product["id"])

    result = await products_dm.find_product(product["id"])

    assert result == {
        "id": product["id"],
        "oracle_id": product["oracle_id"],
        "service_id": product["service_id"],
        "version": product_version["version"],
        "title": product["title"],
        "act_text": product["act_text"],
        "description": product["description"],
        "currency": CurrencyType(product["currency"]),
        "billing_type": BillingType(product["billing_type"]),
        "billing_data": product_version["billing_data"],
        "vat_value": product["vat_value"],
        "campaign_type": CampaignType(product["campaign_type"]),
        "platforms": [PlatformType.NAVI],
        "min_budget": product_version["min_budget"],
        "cpm_filters": product_version["cpm_filters"],
        "comment": product["comment"],
        "available_for_agencies": product["available_for_agencies"],
        "available_for_internal": product["available_for_internal"],
        "active_from": product_version["active_from"],
        "active_to": product_version["active_to"],
        "dedicated_client_ids": expected_dedicated_clients,
        "versions": [
            {
                "active_from": product_version["active_from"],
                "active_to": product_version["active_to"],
                "billing_data": product_version["billing_data"],
                "billing_type": BillingType(product["billing_type"]),
                "cpm_filters": [],
                "min_budget": product_version["min_budget"],
                "product_id": product["id"],
                "version": 1,
            }
        ],
        "type": product["type"],
    }


async def test_returns_edge_active_dates_from_versions(factory, products_dm):
    product = await factory.create_product(_without_version_=True)
    await factory.create_product_version(
        product_id=product["id"],
        version=1,
        active_from=datetime(2002, 4, 1, tzinfo=timezone.utc),
        active_to=datetime(2002, 5, 31, tzinfo=timezone.utc),
    )
    await factory.create_product_version(
        product_id=product["id"],
        version=2,
        active_from=datetime(2002, 1, 1, tzinfo=timezone.utc),
        active_to=datetime(2002, 2, 28, tzinfo=timezone.utc),
    )
    await factory.create_product_version(
        product_id=product["id"],
        version=3,
        active_from=datetime(2002, 7, 1, tzinfo=timezone.utc),
        active_to=datetime(2002, 8, 31, tzinfo=timezone.utc),
    )
    await factory.create_product_version(
        product_id=product["id"],
        version=4,
        active_from=datetime(2002, 3, 10, tzinfo=timezone.utc),
        active_to=datetime(2002, 3, 20, tzinfo=timezone.utc),
    )

    result = await products_dm.find_product(product["id"])

    assert result["active_from"] == datetime(2002, 1, 1, tzinfo=timezone.utc)
    assert result["active_to"] == datetime(2002, 8, 31, tzinfo=timezone.utc)


async def test_returns_none_in_active_to_if_such_version_exists(factory, products_dm):
    product = await factory.create_product(_without_version_=True)
    await factory.create_product_version(
        product_id=product["id"],
        version=1,
        active_from=datetime(2002, 4, 1, tzinfo=timezone.utc),
        active_to=datetime(2002, 5, 31, tzinfo=timezone.utc),
    )
    await factory.create_product_version(
        product_id=product["id"],
        version=2,
        active_from=datetime(2002, 1, 1, tzinfo=timezone.utc),
        active_to=datetime(2002, 2, 28, tzinfo=timezone.utc),
    )
    await factory.create_product_version(
        product_id=product["id"],
        version=3,
        active_from=datetime(2002, 7, 1, tzinfo=timezone.utc),
        active_to=None,
    )
    await factory.create_product_version(
        product_id=product["id"],
        version=4,
        active_from=datetime(2002, 3, 10, tzinfo=timezone.utc),
        active_to=datetime(2002, 3, 20, tzinfo=timezone.utc),
    )

    result = await products_dm.find_product(product["id"])

    assert result["active_to"] is None


async def test_ignores_dates_from_versions_of_other_products(factory, products_dm):
    product = await factory.create_product(_without_version_=True)
    another_product = await factory.create_product(_without_version_=True)
    await factory.create_product_version(
        product_id=product["id"],
        version=1,
        active_from=datetime(2002, 4, 1, tzinfo=timezone.utc),
        active_to=datetime(2002, 5, 31, tzinfo=timezone.utc),
    )
    await factory.create_product_version(
        product_id=product["id"],
        version=2,
        active_from=datetime(2002, 2, 1, tzinfo=timezone.utc),
        active_to=datetime(2002, 3, 31, tzinfo=timezone.utc),
    )
    # Should be ignored
    await factory.create_product_version(
        product_id=another_product["id"],
        version=3,
        active_from=datetime(2002, 1, 1, tzinfo=timezone.utc),
        active_to=datetime(2002, 2, 28, tzinfo=timezone.utc),
    )
    await factory.create_product_version(
        product_id=another_product["id"],
        version=4,
        active_from=datetime(2002, 7, 1, tzinfo=timezone.utc),
        active_to=datetime(2002, 8, 31, tzinfo=timezone.utc),
    )

    result = await products_dm.find_product(product["id"])

    assert result["active_from"] == datetime(2002, 2, 1, tzinfo=timezone.utc)
    assert result["active_to"] == datetime(2002, 5, 31, tzinfo=timezone.utc)


async def test_returns_version_fields_from_currently_active_version(
    factory, products_dm
):
    product = await factory.create_product(_without_version_=True)
    await factory.create_product_version(
        product_id=product["id"],
        version=1,
        billing_data={"base_cpm": "50"},
        min_budget=Decimal("1000"),
        cpm_filters=["filter_one"],
        active_from=datetime.now(tz=timezone.utc) - timedelta(days=3),
        active_to=datetime.now(tz=timezone.utc) - timedelta(days=2),
    )
    await factory.create_product_version(
        product_id=product["id"],
        version=2,
        billing_data={"base_cpm": "60"},
        min_budget=Decimal("2000"),
        cpm_filters=["filter_two"],
        active_from=datetime.now(tz=timezone.utc) - timedelta(days=1),
        active_to=datetime.now(tz=timezone.utc) + timedelta(days=1),
    )
    await factory.create_product_version(
        product_id=product["id"],
        version=3,
        billing_data={"base_cpm": "70"},
        min_budget=Decimal("3000"),
        cpm_filters=["filter_three"],
        active_from=datetime.now(tz=timezone.utc) + timedelta(days=2),
        active_to=datetime.now(tz=timezone.utc) + timedelta(days=3),
    )

    result = await products_dm.find_product(product["id"])

    assert result["version"] == 2
    assert result["billing_data"] == {"base_cpm": "60"}
    assert result["min_budget"] == Decimal("2000")
    assert result["cpm_filters"] == ["filter_two"]


async def test_returns_values_of_latest_version_if_no_active_version_exist(
    factory, products_dm
):
    product = await factory.create_product(_without_version_=True)
    await factory.create_product_version(
        product_id=product["id"],
        version=1,
        billing_data={"base_cpm": "50"},
        min_budget=Decimal("1000"),
        cpm_filters=["filter_one"],
        active_from=datetime.now(tz=timezone.utc) - timedelta(days=3),
        active_to=datetime.now(tz=timezone.utc) - timedelta(days=2),
    )
    await factory.create_product_version(
        product_id=product["id"],
        version=2,
        billing_data={"base_cpm": "70"},
        min_budget=Decimal("3000"),
        cpm_filters=["filter_three"],
        active_from=datetime.now(tz=timezone.utc) + timedelta(days=2),
        active_to=datetime.now(tz=timezone.utc) + timedelta(days=3),
    )

    result = await products_dm.find_product(product["id"])

    assert result["version"] == 2
    assert result["billing_data"] == {"base_cpm": "70"}
    assert result["min_budget"] == Decimal("3000")
    assert result["cpm_filters"] == ["filter_three"]


async def test_returns_none_for_inexistent_product(products_dm):
    assert await products_dm.find_product(55) is None
