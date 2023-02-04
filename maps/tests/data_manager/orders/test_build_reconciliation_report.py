from datetime import datetime, timezone
from decimal import Decimal

import pytest

from maps_adv.billing_proxy.lib.db.enums import OrderOperationType

pytestmark = [pytest.mark.asyncio]


async def test_returns_empty_list_on_empty_log(orders_dm):
    result = await orders_dm.build_reconciliation_report(datetime(2000, 1, 1))

    assert result == {}


@pytest.mark.parametrize(
    ("due_to", "expected_result"),
    [
        (
            datetime(2000, 1, 1, 10, tzinfo=timezone.utc),
            {10: {"completion_qty": Decimal("0.0"), "consumption_qty": Decimal("0.0")}},
        ),
        (
            datetime(2000, 1, 1, 11, tzinfo=timezone.utc),
            {
                10: {
                    "completion_qty": Decimal("0.0"),
                    "consumption_qty": Decimal("100.0"),
                }
            },
        ),
        (
            datetime(2000, 1, 1, 12, tzinfo=timezone.utc),
            {
                10: {
                    "completion_qty": Decimal("0.0"),
                    "consumption_qty": Decimal("100.0"),
                }
            },
        ),
        (
            datetime(2000, 1, 1, 13, tzinfo=timezone.utc),
            {
                10: {
                    "completion_qty": Decimal("0.0"),
                    "consumption_qty": Decimal("150.0"),
                }
            },
        ),
    ],
)
async def test_returns_orders_credits(factory, orders_dm, due_to, expected_result):
    order = await factory.create_order(
        id=10,
        limit=Decimal("150.0"),
        created_at=datetime(2000, 1, 1, 8, tzinfo=timezone.utc),
    )
    await factory.create_order_log(
        created_at=datetime(2000, 1, 1, 10, tzinfo=timezone.utc),
        order_id=order["id"],
        op_type=OrderOperationType.CREDIT,
        amount=Decimal("100.0"),
        limit=Decimal("100.0"),
    )
    await factory.create_order_log(
        created_at=datetime(2000, 1, 1, 12, tzinfo=timezone.utc),
        order_id=order["id"],
        op_type=OrderOperationType.CREDIT,
        amount=Decimal("50.0"),
        limit=Decimal("150.0"),
    )

    result = await orders_dm.build_reconciliation_report(due_to)

    assert result == expected_result


@pytest.mark.parametrize(
    ("due_to", "expected_result"),
    [
        (
            datetime(2000, 1, 1, 10, tzinfo=timezone.utc),
            {
                10: {
                    "completion_qty": Decimal("0.0"),
                    "consumption_qty": Decimal("150.0"),
                }
            },
        ),
        (
            datetime(2000, 1, 1, 11, tzinfo=timezone.utc),
            {
                10: {
                    "completion_qty": Decimal("40.0"),
                    "consumption_qty": Decimal("150.0"),
                }
            },
        ),
        (
            datetime(2000, 1, 1, 12, tzinfo=timezone.utc),
            {
                10: {
                    "completion_qty": Decimal("40.0"),
                    "consumption_qty": Decimal("150.0"),
                }
            },
        ),
        (
            datetime(2000, 1, 1, 13, tzinfo=timezone.utc),
            {
                10: {
                    "completion_qty": Decimal("70.0"),
                    "consumption_qty": Decimal("150.0"),
                }
            },
        ),
    ],
)
async def test_returns_orders_debits(factory, orders_dm, due_to, expected_result):
    order = await factory.create_order(
        id=10,
        limit=Decimal("150.0"),
        consumed=Decimal("70"),
        created_at=datetime(2000, 1, 1, 5, tzinfo=timezone.utc),
    )
    await factory.create_order_log(
        created_at=datetime(2000, 1, 1, 6, tzinfo=timezone.utc),
        order_id=order["id"],
        op_type=OrderOperationType.CREDIT,
        amount=Decimal("150.0"),
        limit=Decimal("150.0"),
    )
    await factory.create_order_log(
        created_at=datetime(2000, 1, 1, 11, tzinfo=timezone.utc),
        order_id=order["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("40.0"),
        limit=Decimal("150.0"),
        consumed=Decimal("40.0"),
        billed_due_to=datetime(2000, 1, 1, 10, tzinfo=timezone.utc),
    )
    await factory.create_order_log(
        created_at=datetime(2000, 1, 1, 13, tzinfo=timezone.utc),
        order_id=order["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("30.0"),
        limit=Decimal("150.0"),
        consumed=Decimal("70.0"),
        billed_due_to=datetime(2000, 1, 1, 12, tzinfo=timezone.utc),
    )

    result = await orders_dm.build_reconciliation_report(due_to)

    assert result == expected_result


async def test_not_returns_data_for_order_created_after_due_to(factory, orders_dm):
    order = await factory.create_order(
        id=10,
        limit=Decimal("100.0"),
        consumed=Decimal("50.0"),
        created_at=datetime(2000, 1, 1, 5, tzinfo=timezone.utc),
    )
    await factory.create_order_log(
        created_at=datetime(2000, 1, 1, 6, tzinfo=timezone.utc),
        order_id=order["id"],
        op_type=OrderOperationType.CREDIT,
        amount=Decimal("100.0"),
        limit=Decimal("100.0"),
    )
    await factory.create_order_log(
        created_at=datetime(2000, 1, 1, 8, tzinfo=timezone.utc),
        order_id=order["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("50.0"),
        limit=Decimal("100.0"),
        consumed=Decimal("50.0"),
        billed_due_to=datetime(2000, 1, 1, 7, tzinfo=timezone.utc),
    )

    result = await orders_dm.build_reconciliation_report(
        datetime(2000, 1, 1, 4, tzinfo=timezone.utc)
    )

    assert result == {}


@pytest.mark.parametrize(
    ("due_to", "expected_result"),
    [
        (datetime(2000, 1, 1, 5, tzinfo=timezone.utc), {}),
        (
            datetime(2000, 1, 1, 6, tzinfo=timezone.utc),
            {10: {"completion_qty": Decimal("0.0"), "consumption_qty": Decimal("0.0")}},
        ),
        (
            datetime(2000, 1, 1, 7, tzinfo=timezone.utc),
            {
                10: {
                    "completion_qty": Decimal("0.0"),
                    "consumption_qty": Decimal("150.0"),
                }
            },
        ),
        (
            datetime(2000, 1, 1, 8, tzinfo=timezone.utc),
            {
                10: {
                    "completion_qty": Decimal("0.0"),
                    "consumption_qty": Decimal("150.0"),
                },
                101: {
                    "completion_qty": Decimal("0.0"),
                    "consumption_qty": Decimal("0.0"),
                },
            },
        ),
        (
            datetime(2000, 1, 1, 9, tzinfo=timezone.utc),
            {
                10: {
                    "completion_qty": Decimal("0.0"),
                    "consumption_qty": Decimal("125.0"),
                },
                101: {
                    "completion_qty": Decimal("25.0"),
                    "consumption_qty": Decimal("25.0"),
                },
            },
        ),
        (
            datetime(2000, 1, 1, 10, tzinfo=timezone.utc),
            {
                10: {
                    "completion_qty": Decimal("0.0"),
                    "consumption_qty": Decimal("125.0"),
                },
                101: {
                    "completion_qty": Decimal("25.0"),
                    "consumption_qty": Decimal("25.0"),
                },
                102: {
                    "completion_qty": Decimal("0.0"),
                    "consumption_qty": Decimal("0.0"),
                },
            },
        ),
        (
            datetime(2000, 1, 1, 11, tzinfo=timezone.utc),
            {
                10: {
                    "completion_qty": Decimal("0.0"),
                    "consumption_qty": Decimal("95.0"),
                },
                101: {
                    "completion_qty": Decimal("25.0"),
                    "consumption_qty": Decimal("25.0"),
                },
                102: {
                    "completion_qty": Decimal("30.0"),
                    "consumption_qty": Decimal("30.0"),
                },
            },
        ),
        (
            datetime(2000, 1, 1, 12, tzinfo=timezone.utc),
            {
                10: {
                    "completion_qty": Decimal("0.0"),
                    "consumption_qty": Decimal("80.0"),
                },
                101: {
                    "completion_qty": Decimal("40.0"),
                    "consumption_qty": Decimal("40.0"),
                },
                102: {
                    "completion_qty": Decimal("30.0"),
                    "consumption_qty": Decimal("30.0"),
                },
            },
        ),
    ],
)
@pytest.mark.vip
async def test_child_orders(factory, orders_dm, due_to, expected_result):
    parent_order = await factory.create_order(
        id=10,
        limit=Decimal("150.0"),
        consumed=Decimal("70.0"),
        created_at=datetime(2000, 1, 1, 5, tzinfo=timezone.utc),
    )
    await factory.create_order_log(
        created_at=datetime(2000, 1, 1, 6, tzinfo=timezone.utc),
        order_id=parent_order["id"],
        op_type=OrderOperationType.CREDIT,
        amount=Decimal("150.0"),
        limit=Decimal("150.0"),
    )
    child_order1 = await factory.create_order(
        id=101,
        parent_order_id=10,
        limit=Decimal("150.0"),
        consumed=Decimal("40.0"),
        created_at=datetime(2000, 1, 1, 7, tzinfo=timezone.utc),
    )
    await factory.create_order_log(
        created_at=datetime(2000, 1, 1, 9, tzinfo=timezone.utc),
        order_id=child_order1["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("25.0"),
        limit=Decimal("0.0"),
        consumed=Decimal("25.0"),
        billed_due_to=datetime(2000, 1, 1, 8, tzinfo=timezone.utc),
    )
    child_order2 = await factory.create_order(
        id=102,
        parent_order_id=10,
        limit=Decimal("0.0"),
        consumed=Decimal("30.0"),
        created_at=datetime(2000, 1, 1, 9, tzinfo=timezone.utc),
    )
    await factory.create_order_log(
        created_at=datetime(2000, 1, 1, 11, tzinfo=timezone.utc),
        order_id=child_order2["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("30.0"),
        limit=Decimal("0.0"),
        consumed=Decimal("30.0"),
        billed_due_to=datetime(2000, 1, 1, 10, tzinfo=timezone.utc),
    )
    await factory.create_order_log(
        created_at=datetime(2000, 1, 1, 12, tzinfo=timezone.utc),
        order_id=child_order1["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("15.0"),
        limit=Decimal("0.0"),
        consumed=Decimal("40.0"),
        billed_due_to=datetime(2000, 1, 1, 11, tzinfo=timezone.utc),
    )

    result = await orders_dm.build_reconciliation_report(due_to)

    assert result == expected_result


async def test_uses_created_at_from_order_logs_if_billed_due_to_is_null(
    factory, orders_dm
):
    order = await factory.create_order(
        id=10,
        limit=Decimal("100.0"),
        consumed=Decimal("80.0"),
        created_at=datetime(2000, 1, 1, 8, tzinfo=timezone.utc),
    )
    await factory.create_order_log(
        created_at=datetime(2000, 1, 1, 10, tzinfo=timezone.utc),
        order_id=order["id"],
        op_type=OrderOperationType.CREDIT,
        amount=Decimal("100.0"),
        limit=Decimal("100.0"),
    )
    await factory.create_order_log(
        created_at=datetime(2000, 1, 1, 11, tzinfo=timezone.utc),
        order_id=order["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("40.0"),
        limit=Decimal("100.0"),
        consumed=Decimal("40.0"),
        billed_due_to=None,
    )
    await factory.create_order_log(
        created_at=datetime(2000, 1, 1, 13, tzinfo=timezone.utc),
        order_id=order["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("30.0"),
        limit=Decimal("100.0"),
        consumed=Decimal("70.0"),
        billed_due_to=datetime(2000, 1, 1, 12, tzinfo=timezone.utc),
    )
    await factory.create_order_log(
        created_at=datetime(2000, 1, 1, 15, tzinfo=timezone.utc),
        order_id=order["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("10.0"),
        limit=Decimal("100.0"),
        consumed=Decimal("80.0"),
        billed_due_to=None,
    )

    result = await orders_dm.build_reconciliation_report(
        datetime(2000, 1, 1, 14, tzinfo=timezone.utc)
    )

    assert result == {
        10: {"completion_qty": Decimal("70.0"), "consumption_qty": Decimal("100.0")}
    }


async def test_uses_created_at_from_order_logs_if_billed_due_to_is_null_for_child_orders(  # noqa: E501
    factory, orders_dm
):
    parent_order = await factory.create_order(
        id=10,
        limit=Decimal("100.0"),
        created_at=datetime(2000, 1, 1, 5, tzinfo=timezone.utc),
    )
    await factory.create_order_log(
        created_at=datetime(2000, 1, 1, 6, tzinfo=timezone.utc),
        order_id=parent_order["id"],
        op_type=OrderOperationType.CREDIT,
        amount=Decimal("100.0"),
        limit=Decimal("100.0"),
    )
    child_order = await factory.create_order(
        id=101,
        parent_order_id=10,
        consumed=Decimal("50.0"),
        created_at=datetime(2000, 1, 1, 7, tzinfo=timezone.utc),
    )
    await factory.create_order_log(
        created_at=datetime(2000, 1, 1, 9, tzinfo=timezone.utc),
        order_id=child_order["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("25.0"),
        limit=Decimal("0.0"),
        consumed=Decimal("25.0"),
        billed_due_to=datetime(2000, 1, 1, 8, tzinfo=timezone.utc),
    )
    await factory.create_order_log(
        created_at=datetime(2000, 1, 1, 10, tzinfo=timezone.utc),
        order_id=child_order["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("15.0"),
        limit=Decimal("0.0"),
        consumed=Decimal("40.0"),
        billed_due_to=None,
    )
    await factory.create_order_log(
        created_at=datetime(2000, 1, 1, 12, tzinfo=timezone.utc),
        order_id=child_order["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("10.0"),
        limit=Decimal("0.0"),
        consumed=Decimal("50.0"),
        billed_due_to=None,
    )

    result = await orders_dm.build_reconciliation_report(
        datetime(2000, 1, 1, 11, tzinfo=timezone.utc)
    )

    assert result == {
        10: {"completion_qty": Decimal("0.0"), "consumption_qty": Decimal("60.0")},
        101: {"completion_qty": Decimal("40.0"), "consumption_qty": Decimal("40.0")},
    }


async def test_not_returns_data_for_orders_from_other_services(factory, orders_dm):
    order1 = await factory.create_order(
        id=10,
        limit=Decimal("100.0"),
        consumed=Decimal("50.0"),
        created_at=datetime(2000, 1, 1, 5, tzinfo=timezone.utc),
    )
    await factory.create_order_log(
        created_at=datetime(2000, 1, 1, 6, tzinfo=timezone.utc),
        order_id=order1["id"],
        op_type=OrderOperationType.CREDIT,
        amount=Decimal("100.0"),
        limit=Decimal("100.0"),
    )
    await factory.create_order_log(
        created_at=datetime(2000, 1, 1, 8, tzinfo=timezone.utc),
        order_id=order1["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("50.0"),
        limit=Decimal("100.0"),
        consumed=Decimal("50.0"),
        billed_due_to=datetime(2000, 1, 1, 7, tzinfo=timezone.utc),
    )
    order2 = await factory.create_order(
        id=20,
        limit=Decimal("100.0"),
        consumed=Decimal("50.0"),
        created_at=datetime(2000, 1, 1, 5, tzinfo=timezone.utc),
        service_id=37,
    )
    await factory.create_order_log(
        created_at=datetime(2000, 1, 1, 6, tzinfo=timezone.utc),
        order_id=order2["id"],
        op_type=OrderOperationType.CREDIT,
        amount=Decimal("100.0"),
        limit=Decimal("100.0"),
    )
    await factory.create_order_log(
        created_at=datetime(2000, 1, 1, 8, tzinfo=timezone.utc),
        order_id=order2["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("50.0"),
        limit=Decimal("100.0"),
        consumed=Decimal("50.0"),
        billed_due_to=datetime(2000, 1, 1, 7, tzinfo=timezone.utc),
    )

    result = await orders_dm.build_reconciliation_report(
        datetime(2000, 1, 1, 9, tzinfo=timezone.utc)
    )

    assert result == {
        10: {
            "completion_qty": Decimal("50.000000"),
            "consumption_qty": Decimal("100.000000"),
        }
    }
