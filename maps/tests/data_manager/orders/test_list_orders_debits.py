from decimal import Decimal
from maps_adv.billing_proxy.lib.db.enums import (
    OrderOperationType,
)
from maps_adv.common.helpers import dt

import pytest

pytestmark = [pytest.mark.asyncio]


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


async def test_returns_debits(factory, orders_dm):
    order_1 = await factory.create_order()
    order_2 = await factory.create_order()

    for order_log in make_order_logs(order_1["id"], order_2["id"]):
        await factory.create_order_log(**order_log)

    result = await orders_dm.list_orders_debits(
        [order_1["id"], order_2["id"]], dt("2022-01-01 00:00:00")
    )
    assert result == {
        order_1["id"]: [
            {
                "billed_at": dt("2022-01-10 00:00:00"),
                "amount": Decimal("10"),
            },
            {
                "billed_at": dt("2022-01-20 00:00:00"),
                "amount": Decimal("100"),
            },
            {
                "billed_at": dt("2022-01-20 00:00:01"),
                "amount": Decimal("10"),
            },
        ],
        order_2["id"]: [
            {
                "billed_at": dt("2022-01-22 00:00:00"),
                "amount": Decimal("200"),
            },
        ],
    }


async def test_returns_debits_billed_after_date(factory, orders_dm):
    order_1 = await factory.create_order()
    order_2 = await factory.create_order()

    for order_log in make_order_logs(order_1["id"], order_2["id"]):
        await factory.create_order_log(**order_log)

    result = await orders_dm.list_orders_debits(
        [order_1["id"], order_2["id"]], dt("2022-01-21 00:00:00")
    )
    assert result == {
        order_2["id"]: [
            {
                "billed_at": dt("2022-01-22 00:00:00"),
                "amount": Decimal("200"),
            },
        ],
    }


async def test_returns_nothing_on_no_orders(factory, orders_dm):
    order_1 = await factory.create_order()
    order_2 = await factory.create_order()

    for order_log in make_order_logs(order_1["id"], order_2["id"]):
        await factory.create_order_log(**order_log)

    result = await orders_dm.list_orders_debits([1, 2], dt("2022-01-01 00:00:00"))
    assert result == {}
