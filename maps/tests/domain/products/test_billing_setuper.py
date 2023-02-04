from decimal import Decimal

import pytest

from maps_adv.billing_proxy.lib.domain.products import (
    BillingSetuper,
    BillingType,
    FixTimeIntervalType,
)


@pytest.mark.parametrize(
    "billing_type, billing_data, expected_billing",
    (
        [
            BillingType.CPM,
            {"base_cpm": "25.1234"},
            {"cpm": {"base_cpm": Decimal("25.1234")}},
        ],
        [
            BillingType.FIX,
            {"cost": "25.1234", "time_interval": "MONTHLY"},
            {
                "fix": {
                    "cost": Decimal("25.1234"),
                    "time_interval": FixTimeIntervalType.MONTHLY,
                }
            },
        ],
    ),
)
def test_setups_billing_fields_as_expected(
    billing_type, billing_data, expected_billing
):
    product = {"billing_type": billing_type, "billing_data": billing_data}

    BillingSetuper.setup_billing(product)

    assert "billing_type" not in product
    assert "billing_data" not in product
    assert product["billing"] == expected_billing


@pytest.mark.parametrize(
    "billing_type, billing_data",
    (
        [BillingType.CPM, {"unknown-field": "25.1234"}],
        [BillingType.CPM, {}],
        [BillingType.FIX, {"invalid-cost": "25.1234", "time_interval": "MONTHLY"}],
        [BillingType.FIX, {"cost": "25.1234", "invalid-time_interval": "MONTHLY"}],
        [BillingType.FIX, {}],
    ),
)
def test_raises_if_setup_with_wrong_parsed_data(billing_type, billing_data):
    product = {"billing_type": billing_type, "billing_data": billing_data}

    with pytest.raises(Exception):
        BillingSetuper.setup_billing(product)
