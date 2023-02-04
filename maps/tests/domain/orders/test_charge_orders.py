from datetime import datetime, timezone
from decimal import Decimal
from unittest.mock import Mock

import pytest
import pytz

from maps_adv.billing_proxy.lib.core.balance_client import BalanceApiError
from maps_adv.billing_proxy.lib.domain.exceptions import (
    BadDuplicatedCharge,
    OrdersBillInFuture,
)
from maps_adv.billing_proxy.tests.helpers import AsyncContextManagerMock

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.mock_dm,
    pytest.mark.freeze_time(datetime(2000, 2, 2, tzinfo=timezone.utc)),
]


@pytest.fixture(autouse=True)
def common_dm_mocks(orders_dm, con):
    orders_dm.list_orders_debits_for_billed_due_to.coro.return_value = {}

    orders_dm.lock_and_return_orders_balance = Mock(
        side_effect=lambda *_: AsyncContextManagerMock(
            ({11: Decimal("200"), 12: Decimal("300"), 13: Decimal("300")}, con)
        )
    )

    orders_dm.charge_orders.coro.return_value = {11: True, 12: False, 13: True}


@pytest.fixture
def mock_list_orders_debits(common_dm_mocks, orders_dm):
    orders_dm.list_orders_debits_for_billed_due_to.coro.return_value = {
        11: Decimal("100"),
        12: Decimal("200"),
        13: Decimal("300"),
    }


def utc_dt(value: str) -> datetime:
    return datetime.strptime(value, "%Y-%m-%d %H:%M:%S").replace(tzinfo=pytz.utc)


def moscow_dt(value: str) -> datetime:
    return pytz.timezone("Europe/Moscow").localize(
        datetime.strptime(value, "%Y-%m-%d %H:%M:%S")
    )


async def test_uses_dm(orders_domain, orders_dm, con):
    result = await orders_domain.charge_orders(
        {11: Decimal("100"), 12: Decimal("200"), 13: Decimal("300")},
        datetime(2000, 2, 1, tzinfo=timezone.utc),
    )

    orders_dm.charge_orders.assert_called_with(
        {11: Decimal("100"), 12: Decimal("200"), 13: Decimal("300")},
        datetime(2000, 2, 1, tzinfo=timezone.utc),
        con=con,
    )
    assert result == ({11: True, 12: False, 13: True}, True)


async def test_raises_if_bill_dt_in_future(orders_domain):
    with pytest.raises(OrdersBillInFuture):
        await orders_domain.charge_orders(
            {11: Decimal("100"), 12: Decimal("200"), 13: Decimal("300")},
            datetime(2000, 2, 3, tzinfo=timezone.utc),
        )


@pytest.mark.parametrize(
    ("now", "bill_for_dt"),
    [
        (moscow_dt("2000-2-3 15:00:00"), utc_dt("2000-2-2 14:00:00")),
        (moscow_dt("2000-2-3 00:00:00"), utc_dt("2000-2-2 20:00:00")),
        (moscow_dt("2000-2-3 00:30:00"), utc_dt("2000-2-2 20:00:00")),
        (moscow_dt("2000-2-3 02:00:00"), utc_dt("2000-2-2 20:00:00")),
        (moscow_dt("2000-2-1 05:00:00"), utc_dt("2000-2-1 01:00:00")),
        (moscow_dt("2000-2-1 00:15:00"), utc_dt("2000-1-31 20:55:00")),
        (moscow_dt("2000-2-1 00:45:00"), utc_dt("2000-1-31 20:55:00")),
        (moscow_dt("2000-2-1 03:45:00"), utc_dt("2000-2-1 00:25:00")),
        (moscow_dt("2000-2-1 01:01:00"), utc_dt("2000-1-31 22:01:00")),
        (moscow_dt("2000-2-1 00:01:00"), utc_dt("2000-1-31 21:01:00")),
    ],
)
async def test_charges_if_bill_for_dt_is_ok(
    orders_domain, orders_dm, freezer, now, bill_for_dt
):
    freezer.move_to(now)

    result = await orders_domain.charge_orders(
        {11: Decimal("100"), 12: Decimal("200"), 13: Decimal("300")}, bill_for_dt
    )

    assert orders_dm.charge_orders.called
    assert result == ({11: True, 12: False, 13: True}, True)


@pytest.mark.parametrize(
    ("now", "bill_for_dt"),
    [
        (moscow_dt("2000-2-1 05:00:00"), utc_dt("2000-1-31 20:00:00")),
        (moscow_dt("2000-2-1 00:51:00"), utc_dt("2000-1-31 20:55:00")),
    ],
)
async def test_not_calls_dm_if_too_late_to_bill(
    freezer, orders_domain, orders_dm, now, bill_for_dt
):
    freezer.move_to(now)

    result = await orders_domain.charge_orders(
        {11: Decimal("100"), 12: Decimal("200"), 13: Decimal("300")}, bill_for_dt
    )

    assert result == ({11: False, 12: False, 13: False}, True)
    assert not orders_dm.charge_orders.called


async def test_locks_orders(orders_domain, orders_dm):
    await orders_domain.charge_orders(
        {11: Decimal("100"), 12: Decimal("200"), 13: Decimal("300")},
        datetime(2000, 2, 1, tzinfo=timezone.utc),
    )

    orders_dm.lock_and_return_orders_balance.assert_called_with([11, 12, 13])


async def test_not_sends_to_dm_orders_with_not_enough_credits(
    orders_domain, orders_dm, con
):
    orders_dm.lock_and_return_orders_balance = Mock(
        side_effect=lambda *_: AsyncContextManagerMock(
            ({11: Decimal("0"), 12: Decimal("100"), 13: Decimal("400")}, con)
        )
    )

    await orders_domain.charge_orders(
        {11: Decimal("100"), 12: Decimal("200"), 13: Decimal("300")},
        datetime(2000, 2, 1, tzinfo=timezone.utc),
    )

    orders_dm.charge_orders.assert_called_with(
        {13: Decimal("300")}, datetime(2000, 2, 1, tzinfo=timezone.utc), con=con
    )


async def test_propagates_balance_exception(orders_domain, orders_dm):
    orders_dm.charge_orders.coro.side_effect = BalanceApiError()

    with pytest.raises(BalanceApiError):
        await orders_domain.charge_orders(
            {11: Decimal("100"), 12: Decimal("200"), 13: Decimal("300")},
            datetime(2000, 2, 1, tzinfo=timezone.utc),
        )


@pytest.mark.usefixtures("mock_list_orders_debits")
async def test_return_values_if_bill_for_dt_already_present_on_order_logs_and_request_matches_logs(  # noqa: E501
    orders_domain,
):
    result = await orders_domain.charge_orders(
        {11: Decimal("100"), 12: Decimal("200"), 13: Decimal("300")},
        datetime(2000, 2, 1, tzinfo=timezone.utc),
    )

    assert result == ({11: True, 12: True, 13: True}, False)


@pytest.mark.usefixtures("mock_list_orders_debits")
async def test_return_values_if_bill_for_dt_already_present_in_order_logs_and_logs_have_less_orders(  # noqa: E501
    orders_domain,
):
    result = await orders_domain.charge_orders(
        {
            11: Decimal("100"),
            12: Decimal("200"),
            13: Decimal("300"),
            14: Decimal("400"),
        },
        datetime(2000, 2, 1, tzinfo=timezone.utc),
    )

    assert result == ({11: True, 12: True, 13: True, 14: False}, False)


@pytest.mark.usefixtures("mock_list_orders_debits")
async def test_raises_if_bill_for_dt_already_present_in_order_logs_and_logs_have_more_orders(  # noqa: E501
    orders_domain,
):
    with pytest.raises(BadDuplicatedCharge) as exc:
        await orders_domain.charge_orders(
            {11: Decimal("100")}, datetime(2000, 2, 1, tzinfo=timezone.utc)
        )

    assert exc.value.order_ids == [12, 13]


@pytest.mark.usefixtures("mock_list_orders_debits")
async def test_raises_if_bill_for_dt_already_present_in_order_logs_and_amounts_differ(
    orders_domain,
):
    with pytest.raises(BadDuplicatedCharge) as exc:
        await orders_domain.charge_orders(
            {11: Decimal("100"), 12: Decimal("800"), 13: Decimal("900")},
            datetime(2000, 2, 1, tzinfo=timezone.utc),
        )

    assert exc.value.order_ids == [12, 13]


@pytest.mark.parametrize(
    "charge_info",
    [
        {11: Decimal("100"), 12: Decimal("200"), 13: Decimal("300")},
        {
            11: Decimal("100"),
            12: Decimal("200"),
            13: Decimal("300"),
            14: Decimal("400"),
        },
        {11: Decimal("100")},
        {11: Decimal("100"), 12: Decimal("800"), 13: Decimal("900")},
    ],
)
@pytest.mark.usefixtures("mock_list_orders_debits")
async def test_not_charges_if_bill_for_dt_already_present_on_order_logs(
    orders_domain, orders_dm, charge_info
):
    try:
        await orders_domain.charge_orders(
            charge_info, datetime(2000, 2, 1, tzinfo=timezone.utc)
        )
    except:  # noqa: B001, E722
        pass

    assert not orders_dm.charge_orders.called
