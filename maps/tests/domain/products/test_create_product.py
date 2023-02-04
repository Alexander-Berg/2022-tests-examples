from datetime import datetime, timezone
from decimal import Decimal

import pytest

from maps_adv.billing_proxy.lib.db.enums import (
    BillingType,
    CampaignType,
    CurrencyType,
    FixTimeIntervalType,
    PlatformType,
)

from maps_adv.billing_proxy.lib.domain.exceptions import (
    NoPlatformsSpecified,
    ProductHasZeroCost,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


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
        "platforms": [PlatformType.NAVI],
        "comment": faker.text(max_nb_chars=256),
        "active_from": datetime(2021, 1, 1, tzinfo=timezone.utc),
        "billing": {"cpm": {"base_cpm": "100.5"}},
        "min_budget": Decimal("10.000000"),
        "cpm_filters": [],
        "type": faker.text(max_nb_chars=256),
    }


@pytest.mark.parametrize(
    "billing,billing_type,billing_data",
    [
        ({"cpm": {"base_cpm": "100.5"}}, BillingType.CPM, {"base_cpm": "100.5"}),
        (
            {"fix": {"cost": "100.5", "time_interval": FixTimeIntervalType.MONTHLY}},
            BillingType.FIX,
            {"cost": "100.5", "time_interval": "MONTHLY"},
        ),
    ],
)
async def test_uses_dm(
    products_domain, products_dm, create_data, billing, billing_type, billing_data
):
    products_dm.create_product.coro.return_value = {"product_id": 1}
    create_data["billing"] = billing

    result = await products_domain.create_product(**create_data)

    create_data["platform"] = create_data["platforms"][0]
    create_data["service_id"] = 100
    create_data["billing_type"] = billing_type
    create_data["billing_data"] = billing_data

    products_dm.create_product.assert_called_with(**create_data)
    assert result == {"product_id": 1}


async def test_requires_platforms(products_domain, products_dm, create_data):
    products_dm.create_product.coro.return_value = {"product_id": 1}

    create_data["platforms"] = []

    with pytest.raises(NoPlatformsSpecified):
        await products_domain.create_product(**create_data)

    products_dm.create_product.assert_not_called()


async def test_requires_non_zero_cost(products_domain, products_dm, create_data):
    create_data["billing"]["cpm"]["base_cpm"] = "0"
    with pytest.raises(ProductHasZeroCost):
        await products_domain.create_product(**create_data)

    create_data["billing"] = {"fix": {"cost": "0", "time_interval": "MONTHLY"}}
    with pytest.raises(ProductHasZeroCost):
        await products_domain.create_product(**create_data)

    products_dm.create_product.assert_not_called()
