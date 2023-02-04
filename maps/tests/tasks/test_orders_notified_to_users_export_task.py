from datetime import datetime, timezone

import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.booking_yang.server.lib.tasks import (
    OrdersNotifiedToUsersYtExportTask,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("mock_yt")]


@pytest.fixture
def mock_yt(mock_yt):
    mock_yt["exists"].return_value = True

    return mock_yt


@pytest.fixture
def task(config, dm):
    return OrdersNotifiedToUsersYtExportTask(config=config, dm=dm)


async def test_creates_table_if_not_exists(task, mock_yt):
    mock_yt["exists"].return_value = False

    await task

    mock_yt["create"].assert_called()
    assert mock_yt["create"].call_args[0][1] == "//path/to/notified_orders_table"


async def test_does_not_create_table_if_exists(task, mock_yt):
    mock_yt["exists"].return_value = True

    await task

    mock_yt["create"].assert_not_called()


async def test_writes_data_as_expected(factory, task, mock_yt):
    order_id_1 = await factory.create_order(
        yang_suite_id="11-11",
        sms_sent_at=dt("2020-01-01 11:11:11"),
        booking_verdict="booked",
        exported_as_notified_at=None,
    )
    order_id_2 = await factory.create_order(
        yang_suite_id="22-22",
        sms_sent_at=dt("2020-02-02 22:22:22"),
        booking_verdict="no_place",
        exported_as_notified_at=None,
    )

    await task

    mock_yt["write_table"].assert_called()
    assert mock_yt["write_table"].call_args[0][1] == [
        {
            "order_id": order_id_1,
            "yang_suite_id": "11-11",
            "sms_sent_at": 1577877071000000,
            "booking_verdict": "booked",
        },
        {
            "order_id": order_id_2,
            "yang_suite_id": "22-22",
            "sms_sent_at": 1580682142000000,
            "booking_verdict": "no_place",
        },
    ]


@pytest.mark.freeze_time
async def test_marks_orders_as_exported(factory, task, mock_yt, con):
    await factory.create_order(
        yang_suite_id="11-11",
        sms_sent_at=dt("2020-01-01 11:11:11"),
        booking_verdict="booked",
        exported_as_notified_at=None,
    )
    await factory.create_order(
        yang_suite_id="22-22",
        sms_sent_at=dt("2020-02-02 22:22:22"),
        booking_verdict="no_place",
        exported_as_notified_at=None,
    )

    await task

    got = await con.fetchval(
        "SELECT COUNT(*) FROM orders WHERE exported_as_notified_at = $1",
        datetime.now(timezone.utc),
    )

    assert got == 2


@pytest.mark.parametrize(
    "yt_operation", ["exists", "create", "Transaction", "write_table"]
)
async def test_does_not_mark_orders_as_exported_if_yt_operation_fails(
    factory, task, mock_yt, con, yt_operation
):
    mock_yt["exists"].return_value = False  # force 'create' call
    await factory.create_order(
        yang_suite_id="11-11",
        sms_sent_at=dt("2020-01-01 11:11:11"),
        booking_verdict="booked",
        exported_as_notified_at=None,
    )
    await factory.create_order(
        yang_suite_id="22-22",
        sms_sent_at=dt("2020-02-02 22:22:22"),
        booking_verdict="no_place",
        exported_as_notified_at=None,
    )
    mock_yt[yt_operation].side_effect = Exception("Yt operation fails")

    with pytest.raises(Exception, match="Yt operation fails"):
        await task

    got = await con.fetchval(
        "SELECT COUNT(*) FROM orders WHERE exported_as_notified_at IS NULL"
    )

    assert got == 2
