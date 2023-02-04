from decimal import Decimal
from datetime import datetime, timedelta
from maps_adv.billing_proxy.lib.db.enums import BillingType

import pytest

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    ("limit", "consumed"),
    [(Decimal("100"), Decimal("0")), (Decimal("100"), Decimal("50"))],
)
async def test_returns_order_if_positive_balance(factory, limit, consumed, orders_dm):
    order = await factory.create_order(limit=limit, consumed=consumed)

    result = await orders_dm.list_positive_balance_orders([order["id"]])

    assert result == [order["id"]]


@pytest.mark.parametrize(
    ("limit", "consumed"),
    [(Decimal("100"), Decimal("100")), (Decimal("0"), Decimal("0"))],
)
async def test_not_returns_order_if_nonpositive_balance(
    factory, limit, consumed, orders_dm
):
    order = await factory.create_order(limit=limit, consumed=consumed)

    result = await orders_dm.list_positive_balance_orders([order["id"]])

    assert result == []


async def test_ignores_missing(factory, orders_dm):
    order = await factory.create_order(limit=Decimal("100"), consumed=Decimal("50"))

    result = await orders_dm.list_positive_balance_orders(
        [order["id"], order["id"] + 1]
    )

    assert result == [order["id"]]


async def test_ignores_not_provided(factory, orders_dm):
    order1 = await factory.create_order(limit=Decimal("100"), consumed=Decimal("100"))
    await factory.create_order(limit=Decimal("100"), consumed=Decimal("50"))

    result = await orders_dm.list_positive_balance_orders([order1["id"]])

    assert result == []


async def test_returns_combined(factory, orders_dm):
    order1 = await factory.create_order(limit=Decimal("100"), consumed=Decimal("50"))
    order2 = await factory.create_order(limit=Decimal("200"), consumed=Decimal("200"))
    order3 = await factory.create_order(limit=Decimal("300"), consumed=Decimal("0"))
    await factory.create_order(limit=Decimal("300"), consumed=Decimal("50"))

    result = await orders_dm.list_positive_balance_orders(
        [order1["id"], order2["id"], order3["id"]]
    )

    assert sorted(result) == sorted([order1["id"], order3["id"]])


async def test_returns_all_positive_balanced_if_no_argument_given(factory, orders_dm):
    order1 = await factory.create_order(limit=Decimal("100"), consumed=Decimal("50"))
    await factory.create_order(limit=Decimal("200"), consumed=Decimal("200"))
    order3 = await factory.create_order(limit=Decimal("300"), consumed=Decimal("0"))

    result = await orders_dm.list_positive_balance_orders()

    assert sorted(result) == sorted([order1["id"], order3["id"]])


async def test_not_returns_hidden_orders(factory, orders_dm):
    order1 = await factory.create_order(
        limit=Decimal("100"), consumed=Decimal("50"), hidden=False
    )
    order2 = await factory.create_order(
        limit=Decimal("100"), consumed=Decimal("50"), hidden=True
    )
    order3 = await factory.create_order(
        limit=Decimal("100"), consumed=Decimal("50"), hidden=False
    )

    result = await orders_dm.list_positive_balance_orders(
        [order1["id"], order2["id"], order3["id"]]
    )

    assert sorted(result) == sorted([order1["id"], order3["id"]])


async def test_not_returns_hidden_orders_if_no_argument_given(factory, orders_dm):
    order1 = await factory.create_order(
        limit=Decimal("100"), consumed=Decimal("50"), hidden=False
    )
    await factory.create_order(
        limit=Decimal("100"), consumed=Decimal("50"), hidden=True
    )
    order3 = await factory.create_order(
        limit=Decimal("100"), consumed=Decimal("50"), hidden=False
    )

    result = await orders_dm.list_positive_balance_orders()

    assert sorted(result) == sorted([order1["id"], order3["id"]])


@pytest.mark.parametrize(
    ("limit", "consumed", "cost"),
    [
        (Decimal("100"), Decimal("0"), Decimal("10")),
        (Decimal("100"), Decimal("90"), Decimal("10")),
        (Decimal("100"), Decimal("0"), Decimal("100")),
    ],
)
async def test_returns_fix_billing_orders(factory, orders_dm, limit, consumed, cost):
    product = await factory.create_product(
        billing_type=BillingType.FIX,
        billing_data={"cost": str(cost), "time_interval": "WEEKLY"},
    )
    order = await factory.create_order(
        limit=limit, consumed=consumed, product_id=product["id"]
    )

    result = await orders_dm.list_positive_balance_orders([order["id"]])

    assert result == [order["id"]]


@pytest.mark.parametrize(
    ("limit", "consumed", "cost"),
    [
        (Decimal("100"), Decimal("0"), Decimal("200")),
        (Decimal("100"), Decimal("95"), Decimal("10")),
        (Decimal("100"), Decimal("100"), Decimal("1")),
        (Decimal("0"), Decimal("0"), Decimal("1")),
    ],
)
async def test_filters_out_not_enough_balance(
    factory, orders_dm, limit, consumed, cost
):
    product = await factory.create_product(
        billing_type=BillingType.FIX,
        billing_data={"cost": str(cost), "time_interval": "WEEKLY"},
    )
    fix_order = await factory.create_order(
        limit=limit, consumed=consumed, product_id=product["id"]
    )
    cpm_order = await factory.create_order(limit=1, consumed=0)
    result = await orders_dm.list_positive_balance_orders(
        [cpm_order["id"], fix_order["id"]]
    )
    assert result == [cpm_order["id"]]


async def test_filters_out_no_active_version(factory, orders_dm):
    product = await factory.create_product(
        billing_type=BillingType.FIX, _without_version_=True
    )
    await factory.create_product_version(
        product["id"],
        version=1,
        active_from=datetime.now() - timedelta(days=2),
        active_to=datetime.now() - timedelta(days=1),
        billing_data={"cost": "1", "time_interval": "WEEKLY"},
    )
    await factory.create_product_version(
        product["id"],
        version=2,
        active_from=datetime.now() + timedelta(days=1),
        active_to=None,
        billing_data={"cost": "1", "time_interval": "WEEKLY"},
    )
    fix_order = await factory.create_order(
        limit=100, consumed=0, product_id=product["id"]
    )
    cpm_order = await factory.create_order(limit=1, consumed=0)
    result = await orders_dm.list_positive_balance_orders(
        [cpm_order["id"], fix_order["id"]]
    )
    assert result == [cpm_order["id"]]
