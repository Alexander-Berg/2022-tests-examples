from datetime import datetime, timezone
from decimal import Decimal
from unittest import mock

import pytest

from maps_adv.billing_proxy.lib.db.enums import OrderOperationType

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def yt_mock(mocker):
    class YtClientMock:
        __init__ = mock.Mock(return_value=None)
        write_table = mock.Mock()

    return mocker.patch("yt.wrapper.YtClient", YtClientMock)


@pytest.fixture
def written_rows(yt_mock):
    write_table_rows = []

    def write_table(table, rows):
        nonlocal write_table_rows
        write_table_rows.append(list(rows))

    yt_mock.write_table.side_effect = write_table

    return write_table_rows


@pytest.fixture
def table_name(yt_mock):
    table_name = []

    def write_table(table, rows):
        nonlocal table_name
        table_name.append(table)

    yt_mock.write_table.side_effect = write_table

    return table_name


async def test_writes_rows(factory, orders_dm, written_rows):
    order1 = await factory.create_order(
        id=10,
        limit=Decimal("0.0"),
        consumed=Decimal("0"),
        created_at=datetime(2000, 1, 1, 5, tzinfo=timezone.utc),
        service_id=37,
        external_id=1000000,
    )
    await factory.create_order_log(
        created_at=datetime(2000, 1, 1, 6, tzinfo=timezone.utc),
        order_id=order1["id"],
        op_type=OrderOperationType.CREDIT,
        amount=Decimal("150.0"),
        limit=Decimal("150.0"),
    )
    await factory.create_order_log(
        created_at=datetime(2000, 1, 1, 11, tzinfo=timezone.utc),
        order_id=order1["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("40.0"),
        limit=Decimal("150.0"),
        consumed=Decimal("40.0"),
        billed_due_to=datetime(2000, 1, 1, 10, tzinfo=timezone.utc),
    )
    await factory.create_order_log(
        created_at=datetime(2000, 1, 1, 13, tzinfo=timezone.utc),
        order_id=order1["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("30.0"),
        limit=Decimal("150.0"),
        consumed=Decimal("70.0"),
        billed_due_to=datetime(2000, 1, 1, 12, tzinfo=timezone.utc),
    )
    order2 = await factory.create_order(
        id=20,
        limit=Decimal("0.0"),
        consumed=Decimal("0"),
        created_at=datetime(2000, 1, 1, 5, tzinfo=timezone.utc),
        service_id=37,
        external_id=2000000,
    )
    await factory.create_order_log(
        created_at=datetime(2000, 1, 1, 6, tzinfo=timezone.utc),
        order_id=order2["id"],
        op_type=OrderOperationType.CREDIT,
        amount=Decimal("250.0"),
        limit=Decimal("250.0"),
    )
    await factory.create_order_log(
        created_at=datetime(2000, 1, 1, 11, tzinfo=timezone.utc),
        order_id=order2["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("50.0"),
        limit=Decimal("250.0"),
        consumed=Decimal("50.0"),
        billed_due_to=datetime(2000, 1, 1, 10, tzinfo=timezone.utc),
    )
    await factory.create_order_log(
        created_at=datetime(2000, 1, 1, 13, tzinfo=timezone.utc),
        order_id=order2["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("100.0"),
        limit=Decimal("250.0"),
        consumed=Decimal("150.0"),
        billed_due_to=datetime(2000, 1, 1, 12, tzinfo=timezone.utc),
    )

    await orders_dm.load_geoprod_reconciliation_report(
        from_datetime=datetime(2000, 1, 1, tzinfo=timezone.utc),
        to_datetime=datetime(2000, 1, 1, 23, 59, 59, 9999, tzinfo=timezone.utc),
    )

    assert [sorted(rows, key=lambda r: r["order_id"]) for rows in written_rows] == [
        [
            {
                "order_id": 1000000,
                "paid_delta": "150.0000000000",
                "spent_delta": "70.0000000000",
            },
            {
                "order_id": 2000000,
                "paid_delta": "250.0000000000",
                "spent_delta": "150.0000000000",
            },
        ]
    ]


async def test_writes_nothing_if_no_data(orders_dm, written_rows):

    await orders_dm.load_geoprod_reconciliation_report(
        from_datetime=datetime(2000, 1, 1, tzinfo=timezone.utc),
        to_datetime=datetime(2000, 1, 1, 23, 59, 59, 9999, tzinfo=timezone.utc),
    )

    assert [sorted(rows, key=lambda r: r["order_id"]) for rows in written_rows] == [[]]


async def test_does_not_write_logs_for_other_services(factory, orders_dm, written_rows):
    order1 = await factory.create_order(
        id=10,
        limit=Decimal("0.0"),
        consumed=Decimal("0"),
        created_at=datetime(2000, 1, 1, 5, tzinfo=timezone.utc),
    )
    await factory.create_order_log(
        created_at=datetime(2000, 1, 1, 6, tzinfo=timezone.utc),
        order_id=order1["id"],
        op_type=OrderOperationType.CREDIT,
        amount=Decimal("150.0"),
        limit=Decimal("150.0"),
    )
    await factory.create_order_log(
        created_at=datetime(2000, 1, 1, 11, tzinfo=timezone.utc),
        order_id=order1["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("40.0"),
        limit=Decimal("150.0"),
        consumed=Decimal("40.0"),
        billed_due_to=datetime(2000, 1, 1, 10, tzinfo=timezone.utc),
    )
    await factory.create_order_log(
        created_at=datetime(2000, 1, 1, 13, tzinfo=timezone.utc),
        order_id=order1["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("30.0"),
        limit=Decimal("150.0"),
        consumed=Decimal("70.0"),
        billed_due_to=datetime(2000, 1, 1, 12, tzinfo=timezone.utc),
    )
    order2 = await factory.create_order(
        id=20,
        limit=Decimal("0.0"),
        consumed=Decimal("0"),
        created_at=datetime(2000, 1, 1, 5, tzinfo=timezone.utc),
        service_id=37,
        external_id=2000000,
    )
    await factory.create_order_log(
        created_at=datetime(2000, 1, 1, 6, tzinfo=timezone.utc),
        order_id=order2["id"],
        op_type=OrderOperationType.CREDIT,
        amount=Decimal("250.0"),
        limit=Decimal("250.0"),
    )
    await factory.create_order_log(
        created_at=datetime(2000, 1, 1, 11, tzinfo=timezone.utc),
        order_id=order2["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("50.0"),
        limit=Decimal("250.0"),
        consumed=Decimal("50.0"),
        billed_due_to=datetime(2000, 1, 1, 10, tzinfo=timezone.utc),
    )
    await factory.create_order_log(
        created_at=datetime(2000, 1, 1, 13, tzinfo=timezone.utc),
        order_id=order2["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("100.0"),
        limit=Decimal("250.0"),
        consumed=Decimal("150.0"),
        billed_due_to=datetime(2000, 1, 1, 12, tzinfo=timezone.utc),
    )

    await orders_dm.load_geoprod_reconciliation_report(
        from_datetime=datetime(2000, 1, 1, tzinfo=timezone.utc),
        to_datetime=datetime(2000, 1, 1, 23, 59, 59, 9999, tzinfo=timezone.utc),
    )

    assert [sorted(rows, key=lambda r: r["order_id"]) for rows in written_rows] == [
        [
            {
                "order_id": 2000000,
                "paid_delta": "250.0000000000",
                "spent_delta": "150.0000000000",
            },
        ]
    ]


async def test_uses_date_as_table_name(config, orders_dm, table_name):

    await orders_dm.load_geoprod_reconciliation_report(
        from_datetime=datetime(2000, 1, 1, tzinfo=timezone.utc),
        to_datetime=datetime(2000, 1, 1, 23, 59, 59, 9999, tzinfo=timezone.utc),
    )

    assert table_name == [config["YT_RECOINCILIATION_REPORT_DIR"] + "/2000-01-01"]
