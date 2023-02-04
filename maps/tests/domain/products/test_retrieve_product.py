from decimal import Decimal

import pytest

from maps_adv.billing_proxy.lib.db.enums import BillingType, FixTimeIntervalType
from maps_adv.billing_proxy.lib.domain.exceptions import ProductDoesNotExist

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_uses_dm(products_domain, products_dm):
    products_dm.find_product.coro.return_value = {
        "id": 1,
        "oracle_id": 123,
        "billing_type": BillingType.CPM,
        "billing_data": {"base_cpm": 50},
    }

    result = await products_domain.retrieve_product(1)

    products_dm.find_product.assert_called_with(1)
    assert result == {
        "id": 1,
        "oracle_id": 123,
        "billing": {"cpm": {"base_cpm": Decimal("50")}},
    }


@pytest.mark.parametrize(
    ("billing_type", "billing_data", "expected_billing"),
    [
        (BillingType.CPM, {"base_cpm": 50}, {"cpm": {"base_cpm": Decimal("50")}}),
        (BillingType.CPM, {"base_cpm": 150}, {"cpm": {"base_cpm": Decimal("150")}}),
        (
            BillingType.FIX,
            {"cost": 50, "time_interval": "DAILY"},
            {
                "fix": {
                    "cost": Decimal("50"),
                    "time_interval": FixTimeIntervalType.DAILY,
                }
            },
        ),
        (
            BillingType.FIX,
            {"cost": 150, "time_interval": "WEEKLY"},
            {
                "fix": {
                    "cost": Decimal("150"),
                    "time_interval": FixTimeIntervalType.WEEKLY,
                }
            },
        ),
        (
            BillingType.FIX,
            {"cost": 150, "time_interval": "MONTHLY"},
            {
                "fix": {
                    "cost": Decimal("150"),
                    "time_interval": FixTimeIntervalType.MONTHLY,
                }
            },
        ),
    ],
)
async def test_return_data(
    products_domain, products_dm, billing_type, billing_data, expected_billing
):
    products_dm.find_product.coro.return_value = {
        "id": 1,
        "oracle_id": 123,
        "billing_type": billing_type,
        "billing_data": billing_data,
    }

    result = await products_domain.retrieve_product(1)

    assert result == {"id": 1, "oracle_id": 123, "billing": expected_billing}


async def raises_for_inexistent_product(products_domain, products_dm):
    products_dm.find_product.coro.return_value = None

    with pytest.raises(ProductDoesNotExist) as exc:
        await products_domain.retrieve_product(1)

    assert exc.value.product_id == 1
