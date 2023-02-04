from datetime import datetime
from decimal import Decimal

import pytest
import pytz

from maps_adv.billing_proxy.lib.db.enums import OrderOperationType

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("db")]

API_URL = "/reconciliation/"
_moscow_tz = pytz.timezone("Europe/Moscow")


def _moscow_dt(*args):
    return _moscow_tz.localize(datetime(*args))


def _lines_to_csv(*lines):
    return "\r\n".join(lines) + "\r\n"


async def test_return_status_and_headers(common_api):
    response = await common_api.get(API_URL + "?dt=20001223")

    assert response.status == 200
    assert response.headers["Content-Type"] == "text/csv; charset=utf-8"


async def test_returns_400_if_no_parameter_passed(common_api):
    response = await common_api.get(API_URL)

    assert response.status == 400


@pytest.mark.parametrize(
    "dt", ["", "28", "notadate", "2000123456", "20001314", "-20001223"]
)
async def test_returns_400_if_bad_parameter_passed(common_api, dt):
    response = await common_api.get(API_URL + "?dt=" + dt)

    assert response.status == 400


async def test_returns_empty_response_on_empty_log(common_api):
    response = await common_api.get(API_URL + "?dt=20001111")

    assert await response.text() == ""


@pytest.mark.parametrize(
    ("dt", "expected_data"),
    [
        (
            "20000109",
            _lines_to_csv("order_id,completion_qty,consumption_qty", "10,0.0,0.0"),
        ),
        (
            "20000111",
            _lines_to_csv(
                "order_id,completion_qty,consumption_qty", "10,0.0,100.0000000000"
            ),
        ),
        (
            "20000112",
            _lines_to_csv(
                "order_id,completion_qty,consumption_qty", "10,0.0,150.0000000000"
            ),
        ),
    ],
)
async def test_returns_orders_credits(common_api, factory, dt, expected_data):
    order = await factory.create_order(
        id=10,
        limit=Decimal("150.0"),
        created_at=_moscow_dt(2000, 1, 8),
        service_id=100,
    )
    await factory.create_order_log(
        created_at=_moscow_dt(2000, 1, 10),
        order_id=order["id"],
        op_type=OrderOperationType.CREDIT,
        amount=Decimal("100.0"),
        limit=Decimal("100.0"),
    )
    await factory.create_order_log(
        created_at=_moscow_dt(2000, 1, 12),
        order_id=order["id"],
        op_type=OrderOperationType.CREDIT,
        amount=Decimal("50.0"),
        limit=Decimal("150.0"),
    )

    response = await common_api.get(API_URL + "?dt=" + dt)

    assert await response.text() == expected_data


@pytest.mark.parametrize(
    ("dt", "expected_data"),
    [
        (
            "20000109",
            _lines_to_csv(
                "order_id,completion_qty,consumption_qty", "10,0.0,150.0000000000"
            ),
        ),
        (
            "20000111",
            _lines_to_csv(
                "order_id,completion_qty,consumption_qty",
                "10,40.0000000000,150.0000000000",
            ),
        ),
        (
            "20000121",
            _lines_to_csv(
                "order_id,completion_qty,consumption_qty",
                "10,70.0000000000,150.0000000000",
            ),
        ),
    ],
)
async def test_returns_orders_debits(common_api, factory, dt, expected_data):
    order = await factory.create_order(
        id=10,
        limit=Decimal("150.0"),
        consumed=Decimal("70"),
        created_at=_moscow_dt(2000, 1, 5),
        service_id=100,
    )
    await factory.create_order_log(
        created_at=_moscow_dt(2000, 1, 6),
        order_id=order["id"],
        op_type=OrderOperationType.CREDIT,
        amount=Decimal("150.0"),
        limit=Decimal("150.0"),
    )
    await factory.create_order_log(
        created_at=_moscow_dt(2000, 1, 11),
        order_id=order["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("40.0"),
        limit=Decimal("150.0"),
        consumed=Decimal("40.0"),
        billed_due_to=_moscow_dt(2000, 1, 10),
    )
    await factory.create_order_log(
        created_at=_moscow_dt(2000, 1, 13),
        order_id=order["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("30.0"),
        limit=Decimal("150.0"),
        consumed=Decimal("70.0"),
        billed_due_to=_moscow_dt(2000, 1, 12),
    )

    response = await common_api.get(API_URL + "?dt=" + dt)

    assert await response.text() == expected_data


async def test_not_returns_data_for_order_created_after_dt(common_api, factory):
    order = await factory.create_order(
        id=10,
        limit=Decimal("100.0"),
        consumed=Decimal("50.0"),
        created_at=_moscow_dt(2000, 1, 5),
        service_id=100,
    )
    await factory.create_order_log(
        created_at=_moscow_dt(2000, 1, 6),
        order_id=order["id"],
        op_type=OrderOperationType.CREDIT,
        amount=Decimal("100.0"),
        limit=Decimal("100.0"),
    )
    await factory.create_order_log(
        created_at=_moscow_dt(2000, 1, 8),
        order_id=order["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("50.0"),
        limit=Decimal("100.0"),
        consumed=Decimal("50.0"),
        billed_due_to=_moscow_dt(2000, 1, 7),
    )

    response = await common_api.get(API_URL + "?dt=20000104")

    assert await response.text() == ""


@pytest.mark.parametrize(
    ("dt", "expected_data"),
    [
        ("20000104", ""),
        (
            "20000105",
            _lines_to_csv("order_id,completion_qty,consumption_qty", "10,0.0,0.0"),
        ),
        (
            "20000106",
            _lines_to_csv(
                "order_id,completion_qty,consumption_qty", "10,0.0,150.0000000000"
            ),
        ),
        (
            "20000107",
            _lines_to_csv(
                "order_id,completion_qty,consumption_qty",
                "10,0.0,150.0000000000",
                "101,0.0,0.0",
            ),
        ),
        (
            "20000108",
            _lines_to_csv(
                "order_id,completion_qty,consumption_qty",
                "10,0.0,125.0000000000",
                "101,25.0000000000,25.0000000000",
            ),
        ),
        (
            "20000109",
            _lines_to_csv(
                "order_id,completion_qty,consumption_qty",
                "10,0.0,125.0000000000",
                "101,25.0000000000,25.0000000000",
                "102,0.0,0.0",
            ),
        ),
        (
            "20000110",
            _lines_to_csv(
                "order_id,completion_qty,consumption_qty",
                "10,0.0,95.0000000000",
                "101,25.0000000000,25.0000000000",
                "102,30.0000000000,30.0000000000",
            ),
        ),
        (
            "20000111",
            _lines_to_csv(
                "order_id,completion_qty,consumption_qty",
                "10,0.0,80.0000000000",
                "101,40.0000000000,40.0000000000",
                "102,30.0000000000,30.0000000000",
            ),
        ),
    ],
)
async def test_child_orders(common_api, factory, dt, expected_data):
    parent_order = await factory.create_order(
        id=10,
        limit=Decimal("150.0"),
        consumed=Decimal("70.0"),
        created_at=_moscow_dt(2000, 1, 5),
        service_id=100,
    )
    await factory.create_order_log(
        created_at=_moscow_dt(2000, 1, 6),
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
        created_at=_moscow_dt(2000, 1, 7),
        service_id=100,
    )
    await factory.create_order_log(
        created_at=_moscow_dt(2000, 1, 9),
        order_id=child_order1["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("25.0"),
        limit=Decimal("0.0"),
        consumed=Decimal("25.0"),
        billed_due_to=_moscow_dt(2000, 1, 8),
    )
    child_order2 = await factory.create_order(
        id=102,
        parent_order_id=10,
        limit=Decimal("0.0"),
        consumed=Decimal("30.0"),
        created_at=_moscow_dt(2000, 1, 9),
        service_id=100,
    )
    await factory.create_order_log(
        created_at=_moscow_dt(2000, 1, 11),
        order_id=child_order2["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("30.0"),
        limit=Decimal("0.0"),
        consumed=Decimal("30.0"),
        billed_due_to=_moscow_dt(2000, 1, 10),
    )
    await factory.create_order_log(
        created_at=_moscow_dt(2000, 1, 12),
        order_id=child_order1["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("15.0"),
        limit=Decimal("0.0"),
        consumed=Decimal("40.0"),
        billed_due_to=_moscow_dt(2000, 1, 11),
    )

    response = await common_api.get(API_URL + "?dt=" + dt)

    assert await response.text() == expected_data


async def test_uses_created_at_from_order_logs_if_billed_due_to_is_null(
    common_api, factory
):
    order = await factory.create_order(
        id=10,
        limit=Decimal("100.0"),
        consumed=Decimal("80.0"),
        created_at=_moscow_dt(2000, 1, 8),
        service_id=100,
    )
    await factory.create_order_log(
        created_at=_moscow_dt(2000, 1, 10),
        order_id=order["id"],
        op_type=OrderOperationType.CREDIT,
        amount=Decimal("100.0"),
        limit=Decimal("100.0"),
    )
    await factory.create_order_log(
        created_at=_moscow_dt(2000, 1, 11),
        order_id=order["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("40.0"),
        limit=Decimal("100.0"),
        consumed=Decimal("40.0"),
        billed_due_to=None,
    )
    await factory.create_order_log(
        created_at=_moscow_dt(2000, 1, 13),
        order_id=order["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("30.0"),
        limit=Decimal("100.0"),
        consumed=Decimal("70.0"),
        billed_due_to=_moscow_dt(2000, 1, 12),
    )
    await factory.create_order_log(
        created_at=_moscow_dt(2000, 1, 15),
        order_id=order["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("10.0"),
        limit=Decimal("100.0"),
        consumed=Decimal("80.0"),
        billed_due_to=None,
    )

    response = await common_api.get(API_URL + "?dt=20000114")

    assert await response.text() == _lines_to_csv(
        "order_id,completion_qty,consumption_qty", "10,70.0000000000,100.0000000000"
    )


async def test_uses_created_at_from_order_logs_if_billed_due_to_is_null_for_child_orders(  # noqa: E501
    common_api, factory
):
    parent_order = await factory.create_order(
        id=10,
        limit=Decimal("100.0"),
        created_at=_moscow_dt(2000, 1, 5),
        service_id=100,
    )
    await factory.create_order_log(
        created_at=_moscow_dt(2000, 1, 6),
        order_id=parent_order["id"],
        op_type=OrderOperationType.CREDIT,
        amount=Decimal("100.0"),
        limit=Decimal("100.0"),
    )
    child_order = await factory.create_order(
        id=101,
        parent_order_id=10,
        consumed=Decimal("50.0"),
        created_at=_moscow_dt(2000, 1, 7),
        service_id=100,
    )
    await factory.create_order_log(
        created_at=_moscow_dt(2000, 1, 9),
        order_id=child_order["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("25.0"),
        limit=Decimal("0.0"),
        consumed=Decimal("25.0"),
        billed_due_to=_moscow_dt(2000, 1, 8),
    )
    await factory.create_order_log(
        created_at=_moscow_dt(2000, 1, 10),
        order_id=child_order["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("15.0"),
        limit=Decimal("0.0"),
        consumed=Decimal("40.0"),
        billed_due_to=None,
    )
    await factory.create_order_log(
        created_at=_moscow_dt(2000, 1, 12),
        order_id=child_order["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("10.0"),
        limit=Decimal("0.0"),
        consumed=Decimal("50.0"),
        billed_due_to=None,
    )

    response = await common_api.get(API_URL + "?dt=20000111")

    assert await response.text() == _lines_to_csv(
        "order_id,completion_qty,consumption_qty",
        "10,0.0,60.0000000000",
        "101,40.0000000000,40.0000000000",
    )


async def test_return_data_to_date_inclusive(common_api, factory):
    order = await factory.create_order(
        id=10,
        limit=Decimal("200.0"),
        consumed=Decimal("120.0"),
        created_at=_moscow_dt(2000, 1, 5),
        service_id=100,
    )
    await factory.create_order_log(
        created_at=_moscow_dt(2000, 1, 6),
        order_id=order["id"],
        op_type=OrderOperationType.CREDIT,
        amount=Decimal("200.0"),
        limit=Decimal("200.0"),
    )
    await factory.create_order_log(
        created_at=_moscow_dt(2000, 1, 8, 0, 0),
        order_id=order["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("10.0"),
        limit=Decimal("200.0"),
        consumed=Decimal("10.0"),
        billed_due_to=_moscow_dt(2000, 1, 8, 0, 0),
    )
    await factory.create_order_log(
        created_at=_moscow_dt(2000, 1, 8, 12, 12, 12),
        order_id=order["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("20.0"),
        limit=Decimal("200.0"),
        consumed=Decimal("30.0"),
        billed_due_to=_moscow_dt(2000, 1, 8, 12, 12, 12),
    )
    await factory.create_order_log(
        created_at=_moscow_dt(2000, 1, 8, 23, 59, 59),
        order_id=order["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("40.0"),
        limit=Decimal("200.0"),
        consumed=Decimal("70.0"),
        billed_due_to=_moscow_dt(2000, 1, 8, 23, 59, 59),
    )
    await factory.create_order_log(
        created_at=_moscow_dt(2000, 1, 9, 0, 0, 0),
        order_id=order["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("50.0"),
        limit=Decimal("200.0"),
        consumed=Decimal("120.0"),
        billed_due_to=_moscow_dt(2000, 1, 9, 0, 0, 0),
    )

    response = await common_api.get(API_URL + "?dt=20000108")

    assert await response.text() == _lines_to_csv(
        "order_id,completion_qty,consumption_qty", "10,70.0000000000,200.0000000000"
    )
