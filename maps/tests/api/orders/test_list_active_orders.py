from decimal import Decimal
from datetime import datetime, timedelta
from maps_adv.billing_proxy.lib.db.enums import BillingType

import pytest

from maps_adv.billing_proxy.proto import common_pb2, orders_pb2

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("db")]

API_URL = "/orders/active/"


@pytest.mark.parametrize(
    ("limit", "consumed"),
    [(Decimal("100"), Decimal("0")), (Decimal("100"), Decimal("50"))],
)
async def test_returns_order_if_positive_balance(api, factory, limit, consumed):
    order = await factory.create_order(limit=limit, consumed=consumed)

    input_pb = orders_pb2.OrderIds(order_ids=[order["id"]])
    result = await api.post(
        API_URL, input_pb, decode_as=orders_pb2.OrderIds, allowed_status_codes=[200]
    )

    assert result == orders_pb2.OrderIds(order_ids=[order["id"]])


@pytest.mark.parametrize(
    ("limit", "consumed"),
    [(Decimal("100"), Decimal("100")), (Decimal("0"), Decimal("0"))],
)
async def test_not_returns_order_if_nonpositive_balance(api, factory, limit, consumed):
    order = await factory.create_order(limit=limit, consumed=consumed)

    input_pb = orders_pb2.OrderIds(order_ids=[order["id"]])
    result = await api.post(
        API_URL, input_pb, decode_as=orders_pb2.OrderIds, allowed_status_codes=[200]
    )

    assert result == orders_pb2.OrderIds(order_ids=[])


async def test_returns_error_for_missing(api, factory):
    order = await factory.create_order(limit=Decimal("100"), consumed=Decimal("50"))

    input_pb = orders_pb2.OrderIds(order_ids=[order["id"], order["id"] + 1])
    await api.post(
        API_URL,
        input_pb,
        expected_error=(
            common_pb2.Error.ORDERS_DO_NOT_EXIST,
            f"order_ids=[{order['id'] + 1}]",
        ),
        allowed_status_codes=[422],
    )


async def test_returns_error_for_hidden(api, factory):
    order1 = await factory.create_order(
        limit=Decimal("100"), consumed=Decimal("50"), hidden=False
    )
    order2 = await factory.create_order(
        limit=Decimal("100"), consumed=Decimal("50"), hidden=True
    )

    input_pb = orders_pb2.OrderIds(order_ids=[order1["id"], order2["id"]])
    await api.post(
        API_URL,
        input_pb,
        expected_error=(
            common_pb2.Error.ORDERS_DO_NOT_EXIST,
            f"order_ids=[{order2['id']}]",
        ),
        allowed_status_codes=[422],
    )


async def test_ignores_not_provided(api, factory):
    order1 = await factory.create_order(limit=Decimal("100"), consumed=Decimal("100"))
    await factory.create_order(limit=Decimal("100"), consumed=Decimal("50"))

    input_pb = orders_pb2.OrderIds(order_ids=[order1["id"]])
    result = await api.post(
        API_URL, input_pb, decode_as=orders_pb2.OrderIds, allowed_status_codes=[200]
    )

    assert result == orders_pb2.OrderIds(order_ids=[])


async def test_returns_combined(api, factory):
    order1 = await factory.create_order(limit=Decimal("100"), consumed=Decimal("50"))
    order2 = await factory.create_order(limit=Decimal("200"), consumed=Decimal("200"))
    order3 = await factory.create_order(limit=Decimal("300"), consumed=Decimal("0"))
    await factory.create_order(limit=Decimal("300"), consumed=Decimal("50"))

    input_pb = orders_pb2.OrderIds(order_ids=[order1["id"], order2["id"], order3["id"]])
    result = await api.post(
        API_URL, input_pb, decode_as=orders_pb2.OrderIds, allowed_status_codes=[200]
    )

    assert result == orders_pb2.OrderIds(order_ids=sorted([order1["id"], order3["id"]]))


async def test_returns_all_positive_balanced_if_no_argument_given(api, factory):
    order1 = await factory.create_order(limit=Decimal("100"), consumed=Decimal("50"))
    await factory.create_order(limit=Decimal("200"), consumed=Decimal("200"))
    order3 = await factory.create_order(limit=Decimal("300"), consumed=Decimal("0"))

    result = await api.post(
        API_URL, decode_as=orders_pb2.OrderIds, allowed_status_codes=[200]
    )

    result.order_ids.sort()
    assert result == orders_pb2.OrderIds(order_ids=sorted([order1["id"], order3["id"]]))


@pytest.mark.parametrize(
    ("limit", "consumed", "cost"),
    [
        (Decimal("100"), Decimal("0"), Decimal("10")),
        (Decimal("100"), Decimal("0"), Decimal("10")),
        (Decimal("100"), Decimal("90"), Decimal("10")),
        (Decimal("100"), Decimal("0"), Decimal("100")),
    ],
)
async def test_returns_fix_billing_orders(api, factory, limit, consumed, cost):
    product = await factory.create_product(
        billing_type=BillingType.FIX,
        billing_data={"cost": str(cost), "time_interval": "WEEKLY"},
    )
    order = await factory.create_order(
        limit=limit, consumed=consumed, product_id=product["id"]
    )

    input_pb = orders_pb2.OrderIds(order_ids=[order["id"]])
    result = await api.post(
        API_URL, input_pb, decode_as=orders_pb2.OrderIds, allowed_status_codes=[200]
    )

    assert result == orders_pb2.OrderIds(order_ids=[order["id"]])


@pytest.mark.parametrize(
    ("limit", "consumed", "cost"),
    [
        (Decimal("100"), Decimal("0"), Decimal("200")),
        (Decimal("100"), Decimal("95"), Decimal("10")),
        (Decimal("100"), Decimal("100"), Decimal("1")),
        (Decimal("0"), Decimal("0"), Decimal("1")),
    ],
)
async def test_not_returns_fix_order_if_not_enough_balance(
    api, factory, limit, consumed, cost
):
    product = await factory.create_product(
        billing_type=BillingType.FIX,
        billing_data={"cost": str(cost), "time_interval": "WEEKLY"},
    )
    order = await factory.create_order(
        limit=limit, consumed=consumed, product_id=product["id"]
    )

    input_pb = orders_pb2.OrderIds(order_ids=[order["id"]])
    result = await api.post(
        API_URL, input_pb, decode_as=orders_pb2.OrderIds, allowed_status_codes=[200]
    )

    assert result == orders_pb2.OrderIds(order_ids=[])


async def test_filters_out_no_active_version(factory, api):
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

    input_pb = orders_pb2.OrderIds(order_ids=[cpm_order["id"], fix_order["id"]])
    result = await api.post(
        API_URL, input_pb, decode_as=orders_pb2.OrderIds, allowed_status_codes=[200]
    )

    assert result == orders_pb2.OrderIds(order_ids=[cpm_order["id"]])
