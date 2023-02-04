from decimal import Decimal

import pytest

from maps_adv.billing_proxy.lib.db.enums import BillingType, FixTimeIntervalType

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_uses_dm(products_domain, products_dm):
    products_dm.list_products.coro.return_value = [
        {
            "id": 1,
            "oracle_id": 123,
            "billing_type": BillingType.CPM,
            "billing_data": {"base_cpm": 50},
        },
        {
            "id": 2,
            "oracle_id": 234,
            "billing_type": BillingType.CPM,
            "billing_data": {"base_cpm": 50},
        },
    ]

    result = await products_domain.list_products(service_ids=[110])

    products_dm.list_products.assert_called_with(service_ids=[110])
    assert result == [
        {"id": 1, "oracle_id": 123, "billing": {"cpm": {"base_cpm": Decimal("50")}}},
        {"id": 2, "oracle_id": 234, "billing": {"cpm": {"base_cpm": Decimal("50")}}},
    ]


async def test_return_data(products_domain, products_dm):
    products_dm.list_products.coro.return_value = [
        {
            "id": 1,
            "oracle_id": 123,
            "billing_type": BillingType.CPM,
            "billing_data": {"base_cpm": 50},
        },
        {
            "id": 2,
            "oracle_id": 234,
            "billing_type": BillingType.CPM,
            "billing_data": {"base_cpm": 150},
        },
        {
            "id": 3,
            "oracle_id": 345,
            "billing_type": BillingType.FIX,
            "billing_data": {"cost": 50, "time_interval": "DAILY"},
        },
        {
            "id": 4,
            "oracle_id": 456,
            "billing_type": BillingType.FIX,
            "billing_data": {"cost": 150, "time_interval": "WEEKLY"},
        },
        {
            "id": 5,
            "oracle_id": 567,
            "billing_type": BillingType.FIX,
            "billing_data": {"cost": 150, "time_interval": "MONTHLY"},
        },
    ]

    result = await products_domain.list_products()

    assert result == [
        {"id": 1, "oracle_id": 123, "billing": {"cpm": {"base_cpm": Decimal("50")}}},
        {"id": 2, "oracle_id": 234, "billing": {"cpm": {"base_cpm": Decimal("150")}}},
        {
            "id": 3,
            "oracle_id": 345,
            "billing": {
                "fix": {
                    "cost": Decimal("50"),
                    "time_interval": FixTimeIntervalType.DAILY,
                }
            },
        },
        {
            "id": 4,
            "oracle_id": 456,
            "billing": {
                "fix": {
                    "cost": Decimal("150"),
                    "time_interval": FixTimeIntervalType.WEEKLY,
                }
            },
        },
        {
            "id": 5,
            "oracle_id": 567,
            "billing": {
                "fix": {
                    "cost": Decimal("150"),
                    "time_interval": FixTimeIntervalType.MONTHLY,
                }
            },
        },
    ]
