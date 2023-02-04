from datetime import datetime
from decimal import Decimal

import pytest
import pytz

from maps_adv.billing_proxy.proto import common_pb2, orders_charge_pb2
from maps_adv.billing_proxy.tests.helpers import dt_to_proto, true_for_all_orders

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("db")]

API_URL = "/orders/charge/"


@pytest.fixture(autouse=True)
def common_balance_client_mocks(balance_client):
    balance_client.update_orders.coro.side_effect = true_for_all_orders


def utc_dt(value: str) -> datetime:
    return datetime.strptime(value, "%Y-%m-%d %H:%M:%S").replace(tzinfo=pytz.utc)


def moscow_dt(value: str) -> datetime:
    return pytz.timezone("Europe/Moscow").localize(
        datetime.strptime(value, "%Y-%m-%d %H:%M:%S")
    )


@pytest.mark.parametrize(
    ("now", "bill_for_dt", "expected"),
    [
        (moscow_dt("2000-2-3 15:00:00"), utc_dt("2000-2-2 14:00:00"), True),
        (moscow_dt("2000-2-3 00:00:00"), utc_dt("2000-2-2 20:00:00"), True),
        (moscow_dt("2000-2-3 00:30:00"), utc_dt("2000-2-2 20:00:00"), True),
        (moscow_dt("2000-2-3 02:00:00"), utc_dt("2000-2-2 20:00:00"), True),
        (moscow_dt("2000-2-1 05:00:00"), utc_dt("2000-2-1 01:00:00"), True),
        (moscow_dt("2000-2-1 05:00:00"), utc_dt("2000-1-31 23:55:00"), True),
        (moscow_dt("2000-2-1 00:15:00"), utc_dt("2000-1-31 20:55:00"), True),
        (moscow_dt("2000-2-1 00:45:00"), utc_dt("2000-1-31 20:55:00"), True),
        (moscow_dt("2000-2-1 00:55:00"), utc_dt("2000-1-31 20:55:00"), False),
        (moscow_dt("2000-2-1 03:45:00"), utc_dt("2000-2-1 00:25:00"), True),
    ],
)
async def test_returns_appropriate(api, freezer, factory, now, bill_for_dt, expected):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))

    freezer.move_to(now)

    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=order["id"], charged_amount="50.0000"
            )
        ],
        bill_for_timestamp=dt_to_proto(bill_for_dt),
    )
    result = await api.post(
        API_URL,
        input_bp,
        decode_as=orders_charge_pb2.OrdersChargeOutput,
        allowed_status_codes=[201],
    )

    assert result == orders_charge_pb2.OrdersChargeOutput(
        charge_result=[
            orders_charge_pb2.OrderChargeOutput(order_id=order["id"], success=expected)
        ],
        applied=True,
    )


@pytest.mark.parametrize(
    ("now", "bill_for_dt", "expected_consumed"),
    [
        (moscow_dt("2000-2-3 15:00:00"), utc_dt("2000-2-2 14:00:00"), Decimal("150")),
        (moscow_dt("2000-2-3 00:00:00"), utc_dt("2000-2-2 20:00:00"), Decimal("150")),
        (moscow_dt("2000-2-3 00:30:00"), utc_dt("2000-2-2 20:00:00"), Decimal("150")),
        (moscow_dt("2000-2-3 02:00:00"), utc_dt("2000-2-2 20:00:00"), Decimal("150")),
        (moscow_dt("2000-2-1 05:00:00"), utc_dt("2000-2-1 01:00:00"), Decimal("150")),
        (moscow_dt("2000-2-1 05:00:00"), utc_dt("2000-1-31 23:55:00"), Decimal("150")),
        (moscow_dt("2000-2-1 00:45:00"), utc_dt("2000-1-31 20:55:00"), Decimal("150")),
        (moscow_dt("2000-2-1 00:55:00"), utc_dt("2000-1-31 20:55:00"), Decimal("100")),
        (moscow_dt("2000-2-1 03:45:00"), utc_dt("2000-2-1 00:25:00"), Decimal("150")),
    ],
)
async def test_updates_consumed_appropriate(
    api, freezer, factory, now, bill_for_dt, expected_consumed
):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))

    freezer.move_to(now)

    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=order["id"], charged_amount="50.0000"
            )
        ],
        bill_for_timestamp=dt_to_proto(bill_for_dt),
    )
    await api.post(
        API_URL,
        input_bp,
        decode_as=orders_charge_pb2.OrdersChargeOutput,
        allowed_status_codes=[201],
    )

    assert (await factory.get_order(order["id"]))["consumed"] == expected_consumed


@pytest.mark.parametrize(
    ("now", "bill_for_dt", "expected_logs_entries_count"),
    [
        (moscow_dt("2000-2-3 15:00:00"), utc_dt("2000-2-2 14:00:00"), 1),
        (moscow_dt("2000-2-3 00:00:00"), utc_dt("2000-2-2 20:00:00"), 1),
        (moscow_dt("2000-2-3 00:30:00"), utc_dt("2000-2-2 20:00:00"), 1),
        (moscow_dt("2000-2-3 02:00:00"), utc_dt("2000-2-2 20:00:00"), 1),
        (moscow_dt("2000-2-1 05:00:00"), utc_dt("2000-2-1 01:00:00"), 1),
        (moscow_dt("2000-2-1 05:00:00"), utc_dt("2000-1-31 23:55:00"), 1),
        (moscow_dt("2000-2-1 00:45:00"), utc_dt("2000-1-31 20:55:00"), 1),
        (moscow_dt("2000-2-1 00:55:00"), utc_dt("2000-1-31 20:55:00"), 0),
        (moscow_dt("2000-2-1 03:45:00"), utc_dt("2000-2-1 00:25:00"), 1),
    ],
)
async def test_creates_order_log_appropriate(
    api, freezer, factory, now, bill_for_dt, expected_logs_entries_count
):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))

    freezer.move_to(now)

    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=order["id"], charged_amount="50.0000"
            )
        ],
        bill_for_timestamp=dt_to_proto(bill_for_dt),
    )
    await api.post(
        API_URL,
        input_bp,
        decode_as=orders_charge_pb2.OrdersChargeOutput,
        allowed_status_codes=[201],
    )

    assert (
        len(await factory.get_orders_logs(order["id"])) == expected_logs_entries_count
    )


@pytest.mark.parametrize(
    ("now", "bill_for_dt", "expected"),
    [
        (moscow_dt("2000-2-3 15:00:00"), utc_dt("2000-2-2 14:00:00"), True),
        (moscow_dt("2000-2-3 00:00:00"), utc_dt("2000-2-2 20:00:00"), True),
        (moscow_dt("2000-2-3 00:30:00"), utc_dt("2000-2-2 20:00:00"), True),
        (moscow_dt("2000-2-3 02:00:00"), utc_dt("2000-2-2 20:00:00"), True),
        (moscow_dt("2000-2-1 05:00:00"), utc_dt("2000-2-1 01:00:00"), True),
        (moscow_dt("2000-2-1 05:00:00"), utc_dt("2000-1-31 23:55:00"), True),
        (moscow_dt("2000-2-1 00:45:00"), utc_dt("2000-1-31 20:55:00"), True),
        (moscow_dt("2000-2-1 00:55:00"), utc_dt("2000-1-31 20:55:00"), False),
        (moscow_dt("2000-2-1 03:45:00"), utc_dt("2000-2-1 00:25:00"), True),
    ],
)
async def test_charges_in_balance_appropriate(
    api, freezer, factory, balance_client, now, bill_for_dt, expected
):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))

    freezer.move_to(now)

    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=order["id"], charged_amount="50.0000"
            )
        ],
        bill_for_timestamp=dt_to_proto(bill_for_dt),
    )
    await api.post(
        API_URL,
        input_bp,
        decode_as=orders_charge_pb2.OrdersChargeOutput,
        allowed_status_codes=[201],
    )

    assert balance_client.update_orders.called == expected


@pytest.mark.freeze_time(datetime(2000, 2, 2, 3, 4, 5, tzinfo=pytz.utc))
async def test_returns_error_for_future(api, factory):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    bill_for_dt = datetime(2000, 2, 2, 3, 4, 6, tzinfo=pytz.utc)

    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=order["id"], charged_amount="50.0000"
            )
        ],
        bill_for_timestamp=dt_to_proto(bill_for_dt),
    )
    await api.post(
        API_URL,
        input_bp,
        expected_error=(
            common_pb2.Error.BILL_FOR_TS_IN_FUTURE,
            f"bill_timestamp={int(bill_for_dt.timestamp())}",
        ),
        allowed_status_codes=[422],
    )


@pytest.mark.freeze_time(datetime(2000, 2, 2, 3, 4, 5, tzinfo=pytz.utc))
async def test_not_updates_consumed_for_future(api, factory):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    bill_for_dt = datetime(2000, 2, 2, 3, 4, 6, tzinfo=pytz.utc)

    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=order["id"], charged_amount="50.0000"
            )
        ],
        bill_for_timestamp=dt_to_proto(bill_for_dt),
    )
    await api.post(API_URL, input_bp)

    assert (await factory.get_order(order["id"]))["consumed"] == Decimal("100")


@pytest.mark.freeze_time(datetime(2000, 2, 2, 3, 4, 5, tzinfo=pytz.utc))
async def test_not_creates_order_log_for_future(api, factory):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    bill_for_dt = datetime(2000, 2, 2, 3, 4, 6, tzinfo=pytz.utc)

    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=order["id"], charged_amount="50.0000"
            )
        ],
        bill_for_timestamp=dt_to_proto(bill_for_dt),
    )
    await api.post(API_URL, input_bp)

    assert await factory.get_orders_logs(order["id"]) == []


@pytest.mark.freeze_time(datetime(2000, 2, 2, 3, 4, 5, tzinfo=pytz.utc))
async def test_not_charges_in_balance_for_future(api, factory, balance_client):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    bill_for_dt = datetime(2000, 2, 2, 3, 4, 6, tzinfo=pytz.utc)

    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=order["id"], charged_amount="50.0000"
            )
        ],
        bill_for_timestamp=dt_to_proto(bill_for_dt),
    )
    await api.post(API_URL, input_bp)

    assert not balance_client.update_orders.called
