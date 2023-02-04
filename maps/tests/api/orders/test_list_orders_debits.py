from decimal import Decimal
from maps_adv.billing_proxy.lib.db.enums import (
    OrderOperationType,
)
from maps_adv.common.helpers import dt

import pytest

from maps_adv.billing_proxy.proto import common_pb2
from maps_adv.billing_proxy.proto.orders_for_stat_pb2 import (
    OrdersDebitsInfoInput,
    OrdersDebitsInfo,
    OrderDebitsInfo,
    DebitInfo,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("db")]

API_URL = "/orders/debits/"


def make_order_logs(order_id_1, order_id_2):
    return [
        {
            "order_id": order_id_1,
            "op_type": OrderOperationType.DEBIT,
            "amount": Decimal("10"),
            "limit": Decimal("10"),
            "consumed": Decimal("0"),
            "billed_due_to": dt("2022-01-10 00:00:00"),
        },
        {
            "order_id": order_id_1,
            "op_type": OrderOperationType.DEBIT,
            "amount": Decimal("100"),
            "limit": Decimal("110"),
            "consumed": Decimal("0"),
            "billed_due_to": dt("2022-01-20 00:00:00"),
        },
        {
            "order_id": order_id_1,
            "op_type": OrderOperationType.DEBIT,
            "amount": Decimal("10"),
            "limit": Decimal("120"),
            "consumed": Decimal("0"),
            "billed_due_to": dt("2022-01-20 00:00:01"),
        },
        {
            "order_id": order_id_1,
            "op_type": OrderOperationType.CREDIT,
            "amount": Decimal("100"),
            "limit": Decimal("20"),
            "consumed": Decimal("100"),
            "billed_due_to": dt("2022-01-20 00:00:00"),
        },
        {
            "order_id": order_id_2,
            "op_type": OrderOperationType.DEBIT,
            "amount": Decimal("200"),
            "limit": Decimal("0"),
            "consumed": Decimal("0"),
            "billed_due_to": dt("2022-01-22 00:00:00"),
        },
    ]


async def test_returns_expected_data(api, factory):
    order_1 = await factory.create_order()
    order_2 = await factory.create_order()

    for order_log in make_order_logs(order_1["id"], order_2["id"]):
        await factory.create_order_log(**order_log)

    input_pb = OrdersDebitsInfoInput(
        order_ids=[order_1["id"], order_2["id"]],
        billed_after=dt("2022-01-01 00:00:00", as_proto=True),
    )

    result = await api.post(
        API_URL,
        input_pb,
        decode_as=OrdersDebitsInfo,
        allowed_status_codes=[200],
    )

    assert result == OrdersDebitsInfo(
        orders_debits=[
            OrderDebitsInfo(
                order_id=order_1["id"],
                debits=[
                    DebitInfo(
                        billed_at=dt("2022-01-10 00:00:00", as_proto=True),
                        amount="10.0000000000",
                    ),
                    DebitInfo(
                        billed_at=dt("2022-01-20 00:00:00", as_proto=True),
                        amount="100.0000000000",
                    ),
                    DebitInfo(
                        billed_at=dt("2022-01-20 00:00:01", as_proto=True),
                        amount="10.0000000000",
                    ),
                ],
            ),
            OrderDebitsInfo(
                order_id=order_2["id"],
                debits=[
                    DebitInfo(
                        billed_at=dt("2022-01-22 00:00:00", as_proto=True),
                        amount="200.0000000000",
                    ),
                ],
            ),
        ]
    )


async def test_returns_debits_billed_after_date(api, factory):
    order_1 = await factory.create_order()
    order_2 = await factory.create_order()

    for order_log in make_order_logs(order_1["id"], order_2["id"]):
        await factory.create_order_log(**order_log)

    input_pb = OrdersDebitsInfoInput(
        order_ids=[order_1["id"], order_2["id"]],
        billed_after=dt("2022-01-21 00:00:00", as_proto=True),
    )

    result = await api.post(
        API_URL,
        input_pb,
        decode_as=OrdersDebitsInfo,
        allowed_status_codes=[200],
    )

    assert result == OrdersDebitsInfo(
        orders_debits=[
            OrderDebitsInfo(
                order_id=order_2["id"],
                debits=[
                    DebitInfo(
                        billed_at=dt("2022-01-22 00:00:00", as_proto=True),
                        amount="200.0000000000",
                    ),
                ],
            ),
        ]
    )


async def test_returns_nothing_on_no_orders(api, factory):
    order_1 = await factory.create_order()
    order_2 = await factory.create_order()

    for order_log in make_order_logs(order_1["id"], order_2["id"]):
        await factory.create_order_log(**order_log)

    input_pb = OrdersDebitsInfoInput(
        order_ids=[],
        billed_after=dt("2022-01-01 00:00:00", as_proto=True),
    )

    result = await api.post(
        API_URL,
        input_pb,
        decode_as=OrdersDebitsInfo,
        allowed_status_codes=[200],
    )

    assert result == OrdersDebitsInfo(orders_debits=[])


async def test_raises_on_nonexisting_orders(api, factory):
    input_pb = OrdersDebitsInfoInput(
        order_ids=[1],
        billed_after=dt("2022-01-01 00:00:00", as_proto=True),
    )
    await api.post(
        API_URL,
        input_pb,
        expected_error=(
            common_pb2.Error.ORDERS_DO_NOT_EXIST,
            f"order_ids={[1]}",
        ),
        allowed_status_codes=[422],
    )
