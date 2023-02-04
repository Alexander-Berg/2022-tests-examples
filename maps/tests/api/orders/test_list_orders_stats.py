from decimal import Decimal
from operator import attrgetter

import pytest

from maps_adv.billing_proxy.proto import common_pb2, orders_for_stat_pb2

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("db")]

API_URL = "/orders/stats/"


@pytest.mark.parametrize(
    ("limit", "consumed", "expected_balance"),
    [
        (Decimal("123.45"), Decimal("100.12"), "23.3300000000"),
        (Decimal("123.456789"), Decimal("100.123456"), "23.3333330000"),
        (Decimal("123.456789"), Decimal("0"), "123.4567890000"),
        (Decimal("123.4567"), Decimal("123.4567"), "0E-10"),
        (Decimal("0"), Decimal("0"), "0E-10"),
    ],
)
async def test_returns_expected_data(api, factory, limit, consumed, expected_balance):
    order = await factory.create_order(limit=limit, consumed=consumed)

    input_pb = orders_for_stat_pb2.OrdersStatInfoInput(order_ids=[order["id"]])
    result = await api.post(
        API_URL,
        input_pb,
        decode_as=orders_for_stat_pb2.OrdersStatInfo,
        allowed_status_codes=[200],
    )

    assert result == orders_for_stat_pb2.OrdersStatInfo(
        orders_info=[
            orders_for_stat_pb2.OrderStatInfo(
                order_id=order["id"], balance=str(expected_balance)
            )
        ]
    )


async def test_lists_many_orders_stats(api, factory):
    order1 = await factory.create_order(
        limit=Decimal("200.00"), consumed=Decimal("100.00")
    )
    order2 = await factory.create_order(
        limit=Decimal("300.00"), consumed=Decimal("150.00")
    )
    order3 = await factory.create_order(
        limit=Decimal("250.00"), consumed=Decimal("200.00")
    )

    input_pb = orders_for_stat_pb2.OrdersStatInfoInput(
        order_ids=[order1["id"], order2["id"], order3["id"]]
    )
    result = await api.post(
        API_URL,
        input_pb,
        decode_as=orders_for_stat_pb2.OrdersStatInfo,
        allowed_status_codes=[200],
    )

    assert result == orders_for_stat_pb2.OrdersStatInfo(
        orders_info=sorted(
            [
                orders_for_stat_pb2.OrderStatInfo(
                    order_id=order1["id"], balance="100.0000000000"
                ),
                orders_for_stat_pb2.OrderStatInfo(
                    order_id=order2["id"], balance="150.0000000000"
                ),
                orders_for_stat_pb2.OrderStatInfo(
                    order_id=order3["id"], balance="50.0000000000"
                ),
            ],
            key=attrgetter("order_id"),
        )
    )


async def test_raises_for_inexistent_orders(api, factory):
    order = await factory.create_order(
        limit=Decimal("200.00"), consumed=Decimal("100.00")
    )
    inexistent_id = order["id"] + 1

    input_pb = orders_for_stat_pb2.OrdersStatInfoInput(
        order_ids=[order["id"], inexistent_id]
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
    order1 = await factory.create_order(
        limit=Decimal("200.00"), consumed=Decimal("100.00"), hidden=False
    )
    order2 = await factory.create_order(
        limit=Decimal("200.00"), consumed=Decimal("100.00"), hidden=True
    )

    input_pb = orders_for_stat_pb2.OrdersStatInfoInput(
        order_ids=[order1["id"], order2["id"]]
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
