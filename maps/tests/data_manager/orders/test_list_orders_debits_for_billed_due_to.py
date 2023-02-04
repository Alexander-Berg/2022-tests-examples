from datetime import datetime, timezone
from decimal import Decimal

import pytest

from maps_adv.billing_proxy.lib.db.enums import OrderOperationType
from maps_adv.billing_proxy.tests.helpers import coro_mock

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def mocked_connection():
    class MockedConnection:
        fetchval = coro_mock()
        fetchrow = coro_mock()
        fetch = coro_mock()

    return MockedConnection()


async def test_returns_orders_debits_with_provided_billed_due_to(factory, orders_dm):
    order1 = await factory.create_order()
    await factory.create_order_log(
        order_id=order1["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("10.0"),
        billed_due_to=datetime(2000, 1, 2, tzinfo=timezone.utc),
    )
    order2 = await factory.create_order()
    await factory.create_order_log(
        order_id=order2["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("20.0"),
        billed_due_to=datetime(2000, 1, 2, tzinfo=timezone.utc),
    )

    result = await orders_dm.list_orders_debits_for_billed_due_to(
        datetime(2000, 1, 2, tzinfo=timezone.utc)
    )

    assert result == {order1["id"]: Decimal("10.0"), order2["id"]: Decimal("20.0")}


async def test_returns_empty_dict_if_not_debits(orders_dm):
    result = await orders_dm.list_orders_debits_for_billed_due_to(
        datetime(2000, 1, 2, tzinfo=timezone.utc)
    )

    assert result == {}


async def test_not_returns_debits_with_other_billed_due_to(factory, orders_dm):
    order1 = await factory.create_order()
    await factory.create_order_log(
        order_id=order1["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("10.0"),
        billed_due_to=datetime(2000, 1, 1, tzinfo=timezone.utc),
    )
    order2 = await factory.create_order()
    await factory.create_order_log(
        order_id=order2["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("20.0"),
        billed_due_to=datetime(2000, 1, 3, tzinfo=timezone.utc),
    )

    result = await orders_dm.list_orders_debits_for_billed_due_to(
        datetime(2000, 1, 2, tzinfo=timezone.utc)
    )

    assert result == {}


async def test_returns_only_debits_for_provided_billed_due_to(factory, orders_dm):
    order = await factory.create_order()
    await factory.create_order_log(
        order_id=order["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("10.0"),
        billed_due_to=datetime(2000, 1, 1, tzinfo=timezone.utc),
    )
    await factory.create_order_log(
        order_id=order["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("20.0"),
        billed_due_to=datetime(2000, 1, 2, tzinfo=timezone.utc),
    )
    await factory.create_order_log(
        order_id=order["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("30.0"),
        billed_due_to=datetime(2000, 1, 3, tzinfo=timezone.utc),
    )

    result = await orders_dm.list_orders_debits_for_billed_due_to(
        datetime(2000, 1, 2, tzinfo=timezone.utc)
    )

    assert result == {order["id"]: Decimal("20")}


async def test_uses_provided_connection(mocked_connection, orders_dm):
    mocked_connection.fetch.coro.return_value = []

    await orders_dm.list_orders_debits_for_billed_due_to(
        datetime(2000, 1, 2, tzinfo=timezone.utc), mocked_connection
    )

    assert mocked_connection.fetch.called
