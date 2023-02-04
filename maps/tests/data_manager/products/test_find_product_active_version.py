from datetime import datetime, timezone
from decimal import Decimal

import pytest

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    "active_to", [datetime(2000, 2, 28, tzinfo=timezone.utc), None]
)
async def test_returns_active_product_version(factory, products_dm, active_to):
    product = await factory.create_product(_without_version_=True)
    version = await factory.create_product_version(
        product["id"],
        version=1,
        active_from=datetime(2000, 1, 1, tzinfo=timezone.utc),
        active_to=active_to,
        billing_data={"base_cpm": "60"},
        min_budget=Decimal("6000"),
        cpm_filters=["filter_one", "filter_two"],
    )

    result = await products_dm.find_product_active_version(
        product["id"], datetime(2000, 2, 1, tzinfo=timezone.utc)
    )

    assert result == {
        "id": version["id"],
        "product_id": product["id"],
        "version": 1,
        "active_from": datetime(2000, 1, 1, tzinfo=timezone.utc),
        "active_to": active_to,
        "billing_data": {"base_cpm": "60"},
        "min_budget": Decimal("6000"),
        "cpm_filters": ["filter_one", "filter_two"],
    }


@pytest.mark.parametrize(
    "dt",
    [
        datetime(1999, 12, 11, tzinfo=timezone.utc),
        datetime(2000, 5, 5, tzinfo=timezone.utc),
    ],
)
async def test_returns_none_if_no_active_version_exist(factory, products_dm, dt):
    product = await factory.create_product(_without_version_=True)
    await factory.create_product_version(
        product["id"],
        version=1,
        active_from=datetime(2000, 1, 1, tzinfo=timezone.utc),
        active_to=datetime(2000, 2, 28, tzinfo=timezone.utc),
        billing_data={"base_cpm": "60"},
        min_budget=Decimal("6000"),
        cpm_filters=["filter_one", "filter_two"],
    )
    await factory.create_product_version(
        product["id"],
        version=2,
        active_from=datetime(2000, 3, 1, tzinfo=timezone.utc),
        active_to=datetime(2000, 4, 30, tzinfo=timezone.utc),
        billing_data={"base_cpm": "60"},
        min_budget=Decimal("6000"),
        cpm_filters=["filter_one", "filter_two"],
    )

    result = await products_dm.find_product_active_version(product["id"], dt)

    assert result is None


async def test_returns_none_for_inexistent_product(products_dm):
    result = await products_dm.find_product_active_version(
        555, datetime(2000, 2, 1, tzinfo=timezone.utc)
    )

    assert result is None
