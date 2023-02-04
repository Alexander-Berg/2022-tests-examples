from decimal import Decimal
from datetime import datetime, timezone

import pytest

from maps_adv.billing_proxy.proto import orders_for_stat_pb2, common_pb2
from maps_adv.billing_proxy.tests.helpers import dt_to_proto

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("db")]

API_URL = "/orders/discounts/"


@pytest.mark.parametrize(
    ("month", "discount"),
    (
        (1, Decimal("0.7")),
        (2, Decimal("0.8")),
        (3, Decimal("1.0")),
        (4, Decimal("1.0")),
        (5, Decimal("1.0")),
        (6, Decimal("1.0")),
        (7, Decimal("1.0")),
        (8, Decimal("1.0")),
        (9, Decimal("1.3")),
        (10, Decimal("1.3")),
        (11, Decimal("1.3")),
        (12, Decimal("1.3")),
    ),
)
async def test_returns_expceted_data(api, factory, month, discount):
    regular_product = await factory.create_product(type="REGULAR")
    yearlong_product = await factory.create_product(type="YEARLONG")

    regular_order = await factory.create_order(product_id=regular_product["id"])
    yearlong_order = await factory.create_order(product_id=yearlong_product["id"])

    input_pb = orders_for_stat_pb2.OrdersDiscountInfoInput(
        order_ids=[regular_order["id"], yearlong_order["id"]],
        billed_at=dt_to_proto(datetime(2000, month, 2, tzinfo=timezone.utc)),
    )
    result = await api.post(
        API_URL,
        input_pb,
        decode_as=orders_for_stat_pb2.OrdersDiscountInfo,
        allowed_status_codes=[200],
    )

    assert result == orders_for_stat_pb2.OrdersDiscountInfo(
        discount_info=[
            orders_for_stat_pb2.OrderDiscountInfo(
                order_id=regular_order["id"],
                discount=str(discount),
            ),
            orders_for_stat_pb2.OrderDiscountInfo(
                order_id=yearlong_order["id"],
                discount="1.0",
            ),
        ]
    )


async def test_raises_for_inexistent_orders(api, factory):
    order = await factory.create_order()
    inexistent_id = order["id"] + 1

    input_pb = orders_for_stat_pb2.OrdersDiscountInfoInput(
        order_ids=[order["id"], inexistent_id],
        billed_at=dt_to_proto(datetime(2000, 1, 2, tzinfo=timezone.utc)),
    )

    await api.post(
        API_URL,
        input_pb,
        expected_error=(
            common_pb2.Error.ORDERS_DO_NOT_EXIST,
            f"order_ids={[inexistent_id]}",
        ),
        allowed_status_codes=[422],
    )


async def test_raises_for_hidden_orders(api, factory):
    order1 = await factory.create_order()
    order2 = await factory.create_order(hidden=True)

    input_pb = orders_for_stat_pb2.OrdersDiscountInfoInput(
        order_ids=[order1["id"], order2["id"]],
        billed_at=dt_to_proto(datetime(2000, 1, 2, tzinfo=timezone.utc)),
    )

    await api.post(
        API_URL,
        input_pb,
        expected_error=(
            common_pb2.Error.ORDERS_DO_NOT_EXIST,
            f"order_ids={[order2['id']]}",
        ),
        allowed_status_codes=[422],
    )
