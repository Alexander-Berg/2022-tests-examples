from datetime import datetime, timezone
from decimal import Decimal

import pytest

from maps_adv.billing_proxy.lib.db.enums import (
    BillingType,
    CurrencyType,
    CampaignType,
)

from maps_adv.billing_proxy.lib.domain.exceptions import (
    ConflictingProductVersionTimeSpans,
    InvalidBillingType,
    NoProductVersionsSpecified,
    ProductDoesNotExist,
)


pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.fixture
async def update_data(faker):
    return {
        "product_id": 1,
        "title": faker.text(max_nb_chars=256),
        "act_text": faker.text(max_nb_chars=256),
        "description": faker.text(max_nb_chars=256),
        "vat_value": Decimal("1"),
        "comment": faker.text(max_nb_chars=256),
        "service_id": 100,
        "campaign_type": CampaignType.PIN_ON_ROUTE,
        "currency": CurrencyType.RUB,
        "type": "REGULAR",
        "versions": [
            {
                "active_from": datetime(2021, 10, 12, tzinfo=timezone.utc),
                "active_to": datetime(2021, 10, 13, tzinfo=timezone.utc),
                "billing": {"cpm": {"base_cpm": 250}},
                "min_budget": Decimal("100.000000"),
                "cpm_filters": ["asdf"],
            },
            {
                "active_from": datetime(2021, 10, 13, tzinfo=timezone.utc),
                "active_to": None,
                "billing": {"cpm": {"base_cpm": 2500}},
                "min_budget": Decimal("1000.000000"),
                "cpm_filters": [],
            },
        ],
    }


async def test_uses_dm(products_domain, products_dm, update_data):
    products_dm.find_product.coro.return_value = {
        "billing_type": BillingType.CPM,
        "service_id": 100,
        "currency": CurrencyType.RUB,
        "type": "REGULAR",
        "campaign_type": CampaignType.PIN_ON_ROUTE,
    }
    await products_domain.update_product(**update_data)
    for version in update_data["versions"]:
        version["billing_data"] = version["billing"]["cpm"]
    products_dm.update_product.assert_called_with(**update_data)


async def test_sorts_versions(products_domain, products_dm, update_data):
    products_dm.find_product.coro.return_value = {
        "billing_type": BillingType.CPM,
        "service_id": 100,
        "currency": CurrencyType.RUB,
        "type": "REGULAR",
        "campaign_type": CampaignType.PIN_ON_ROUTE,
    }
    swapped_update_data = update_data.copy()
    swapped_update_data["versions"][0], swapped_update_data["versions"][1] = (
        swapped_update_data["versions"][1],
        swapped_update_data["versions"][0],
    )
    await products_domain.update_product(**swapped_update_data)
    for version in update_data["versions"]:
        version["billing_data"] = version["billing"]["cpm"]
    products_dm.update_product.assert_called_with(**update_data)


async def test_requires_product(products_domain, products_dm, update_data):
    products_dm.find_product.coro.return_value = None
    with pytest.raises(ProductDoesNotExist) as exc:
        await products_domain.update_product(**update_data)
    assert exc.value.product_id == update_data["product_id"]
    products_dm.update_product.assert_not_called()


async def test_requres_versions(products_domain, products_dm, update_data):
    products_dm.find_product.coro.return_value = {
        "billing_type": BillingType.CPM,
        "service_id": 100,
        "currency": CurrencyType.RUB,
        "type": "REGULAR",
        "campaign_type": CampaignType.PIN_ON_ROUTE,
    }
    update_data["versions"] = []
    with pytest.raises(NoProductVersionsSpecified):
        await products_domain.update_product(**update_data)
    products_dm.update_product.assert_not_called()


async def test_requres_matching_billing(products_domain, products_dm, update_data):
    products_dm.find_product.coro.return_value = {
        "billing_type": BillingType.FIX,
        "service_id": 100,
        "currency": CurrencyType.RUB,
        "type": "REGULAR",
        "campaign_type": CampaignType.PIN_ON_ROUTE,
    }
    with pytest.raises(InvalidBillingType) as exc:
        await products_domain.update_product(**update_data)
    assert exc.value.billing_type == BillingType.CPM
    products_dm.update_product.assert_not_called()


async def test_requres_fitting_dates(products_domain, products_dm, update_data):
    products_dm.find_product.coro.return_value = {
        "billing_type": BillingType.CPM,
        "service_id": 100,
        "currency": CurrencyType.RUB,
        "type": "REGULAR",
        "campaign_type": CampaignType.PIN_ON_ROUTE,
    }

    update_data["versions"][1]["active_from"] = datetime(
        2021, 10, 12, tzinfo=timezone.utc
    )
    with pytest.raises(ConflictingProductVersionTimeSpans) as exc:
        await products_domain.update_product(**update_data)
    assert exc.value.to == datetime(2021, 10, 13, tzinfo=timezone.utc)
    assert exc.value.from_ == datetime(2021, 10, 12, tzinfo=timezone.utc)

    update_data["versions"][1]["active_from"] = datetime(
        2021, 10, 14, tzinfo=timezone.utc
    )
    with pytest.raises(ConflictingProductVersionTimeSpans) as exc:
        await products_domain.update_product(**update_data)
    assert exc.value.to == datetime(2021, 10, 13, tzinfo=timezone.utc)
    assert exc.value.from_ == datetime(2021, 10, 14, tzinfo=timezone.utc)

    update_data["versions"][1]["active_from"] = datetime(
        2021, 10, 14, tzinfo=timezone.utc
    )
    update_data["versions"][0]["active_to"] = None
    with pytest.raises(ConflictingProductVersionTimeSpans) as exc:
        await products_domain.update_product(**update_data)
    assert exc.value.to is None
    assert exc.value.from_ == datetime(2021, 10, 14, tzinfo=timezone.utc)

    products_dm.update_product.assert_not_called()
