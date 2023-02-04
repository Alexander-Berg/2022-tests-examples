from datetime import datetime, timedelta, timezone
from decimal import Decimal
from operator import itemgetter

import pytest

from maps_adv.billing_proxy.lib.core.balance_client import BalanceApiError
from maps_adv.billing_proxy.lib.domain import OrderOperationType
from maps_adv.billing_proxy.proto import common_pb2, orders_charge_pb2
from maps_adv.billing_proxy.tests.helpers import Any, dt_to_proto, true_for_all_orders

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("db")]

API_URL = "/orders/charge/"

bill_for_dt = datetime(2000, 2, 2, 3, 4, 5, tzinfo=timezone.utc)
bill_for_timestamp = dt_to_proto(bill_for_dt)


@pytest.fixture(autouse=True)
def freeze_2000(freezer):
    freezer.move_to(bill_for_dt + timedelta(minutes=5))


async def test_charges_in_balance(api, factory, balance_client):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    balance_client.update_orders.coro.side_effect = true_for_all_orders

    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=order["id"], charged_amount="50.0000"
            )
        ],
        bill_for_timestamp=bill_for_timestamp,
    )
    await api.post(
        API_URL,
        input_bp,
        decode_as=orders_charge_pb2.OrdersChargeOutput,
        allowed_status_codes=[201],
    )

    assert balance_client.update_orders.call_args[0] == (
        {order["id"]: {"consumed": Decimal("150"), "service_id": 110}},
        bill_for_dt,
    )


async def test_charges_multiple_in_balance(api, factory, balance_client):
    order1 = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    order2 = await factory.create_order(
        limit=Decimal("400"), consumed=Decimal("200"), service_id=37
    )
    balance_client.update_orders.coro.side_effect = true_for_all_orders

    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=order1["id"], charged_amount="50.0000"
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=order2["id"], charged_amount="150.0000"
            ),
        ],
        bill_for_timestamp=bill_for_timestamp,
    )
    await api.post(
        API_URL,
        input_bp,
        decode_as=orders_charge_pb2.OrdersChargeOutput,
        allowed_status_codes=[201],
    )

    assert balance_client.update_orders.call_args[0] == (
        {
            order1["id"]: {"consumed": Decimal("150"), "service_id": 110},
            order2["external_id"]: {"consumed": Decimal("350"), "service_id": 37},
        },
        bill_for_dt,
    )


async def test_updates_consumed_if_balance_returns_true(api, factory, balance_client):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    balance_client.update_orders.coro.return_value = {110: {order["id"]: True}}

    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=order["id"], charged_amount="50.0000"
            )
        ],
        bill_for_timestamp=bill_for_timestamp,
    )
    await api.post(
        API_URL,
        input_bp,
        decode_as=orders_charge_pb2.OrdersChargeOutput,
        allowed_status_codes=[201],
    )

    assert (await factory.get_order(order["id"]))["consumed"] == Decimal("150")


async def test_creates_order_log_if_balance_returns_true(api, factory, balance_client):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    balance_client.update_orders.coro.return_value = {110: {order["id"]: True}}

    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=order["id"], charged_amount="50.0000"
            )
        ],
        bill_for_timestamp=bill_for_timestamp,
    )
    await api.post(
        API_URL,
        input_bp,
        decode_as=orders_charge_pb2.OrdersChargeOutput,
        allowed_status_codes=[201],
    )

    assert await factory.get_orders_logs(order["id"]) == [
        {
            "id": Any(int),
            "order_id": order["id"],
            "created_at": Any(datetime),
            "op_type": OrderOperationType.DEBIT.name,
            "amount": Decimal("50"),
            "consumed": Decimal("150"),
            "limit": Decimal("200"),
            "billed_due_to": bill_for_dt,
        }
    ]


async def test_returns_true_if_balance_returns_true(api, factory, balance_client):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    balance_client.update_orders.coro.return_value = {110: {order["id"]: True}}

    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=order["id"], charged_amount="50.0000"
            )
        ],
        bill_for_timestamp=bill_for_timestamp,
    )
    result = await api.post(
        API_URL,
        input_bp,
        decode_as=orders_charge_pb2.OrdersChargeOutput,
        allowed_status_codes=[201],
    )

    assert result == orders_charge_pb2.OrdersChargeOutput(
        charge_result=[
            orders_charge_pb2.OrderChargeOutput(order_id=order["id"], success=True)
        ],
        applied=True,
    )


async def test_not_updates_consumed_if_balance_returns_false(
    api, factory, balance_client
):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    balance_client.update_orders.coro.return_value = {110: {order["id"]: False}}

    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=order["id"], charged_amount="50.0000"
            )
        ],
        bill_for_timestamp=bill_for_timestamp,
    )
    await api.post(
        API_URL,
        input_bp,
        decode_as=orders_charge_pb2.OrdersChargeOutput,
        allowed_status_codes=[201],
    )

    assert (await factory.get_order(order["id"]))["consumed"] == Decimal("100")


async def test_not_creates_order_log_if_balance_returns_false(
    api, factory, balance_client
):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    balance_client.update_orders.coro.return_value = {110: {order["id"]: False}}

    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=order["id"], charged_amount="50.0000"
            )
        ],
        bill_for_timestamp=bill_for_timestamp,
    )
    await api.post(
        API_URL,
        input_bp,
        decode_as=orders_charge_pb2.OrdersChargeOutput,
        allowed_status_codes=[201],
    )

    assert await factory.get_orders_logs(order["id"]) == []


async def test_returns_false_if_balance_returns_false(api, factory, balance_client):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    balance_client.update_orders.coro.return_value = {110: {order["id"]: False}}

    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=order["id"], charged_amount="50.0000"
            )
        ],
        bill_for_timestamp=bill_for_timestamp,
    )
    result = await api.post(
        API_URL,
        input_bp,
        decode_as=orders_charge_pb2.OrdersChargeOutput,
        allowed_status_codes=[201],
    )

    assert result == orders_charge_pb2.OrdersChargeOutput(
        charge_result=[
            orders_charge_pb2.OrderChargeOutput(order_id=order["id"], success=False)
        ],
        applied=True,
    )


async def test_updates_consumed_appropriately_if_balance_return_combined(
    api, factory, balance_client
):
    order1 = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    order2 = await factory.create_order(limit=Decimal("400"), consumed=Decimal("150"))
    order3 = await factory.create_order(limit=Decimal("100"), consumed=Decimal("0"))
    order4 = await factory.create_order(
        limit=Decimal("200"), consumed=Decimal("100"), service_id=37
    )
    order5 = await factory.create_order(
        limit=Decimal("400"), consumed=Decimal("150"), service_id=37
    )
    balance_client.update_orders.coro.return_value = {
        110: {order1["id"]: True, order2["id"]: False, order3["id"]: True},
        37: {order4["external_id"]: True, order5["external_id"]: False},
    }

    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=order1["id"], charged_amount="50.0000"
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=order2["id"], charged_amount="100.0000"
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=order3["id"], charged_amount="10.0000"
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=order4["id"], charged_amount="50.0000"
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=order5["id"], charged_amount="100.0000"
            ),
        ],
        bill_for_timestamp=bill_for_timestamp,
    )
    await api.post(
        API_URL,
        input_bp,
        decode_as=orders_charge_pb2.OrdersChargeOutput,
        allowed_status_codes=[201],
    )

    assert (await factory.get_order(order1["id"]))["consumed"] == Decimal("150")
    assert (await factory.get_order(order2["id"]))["consumed"] == Decimal("150")
    assert (await factory.get_order(order3["id"]))["consumed"] == Decimal("10")
    assert (await factory.get_order(order4["id"]))["consumed"] == Decimal("150")
    assert (await factory.get_order(order5["id"]))["consumed"] == Decimal("150")


async def test_creates_order_logs_appropriately_if_balance_return_combined(
    api, factory, balance_client
):
    order1 = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    order2 = await factory.create_order(limit=Decimal("400"), consumed=Decimal("150"))
    order3 = await factory.create_order(limit=Decimal("100"), consumed=Decimal("0"))
    order4 = await factory.create_order(
        limit=Decimal("200"), consumed=Decimal("100"), service_id=37
    )
    order5 = await factory.create_order(
        limit=Decimal("400"), consumed=Decimal("150"), service_id=37
    )
    balance_client.update_orders.coro.return_value = {
        110: {order1["id"]: True, order2["id"]: False, order3["id"]: True},
        37: {order4["external_id"]: True, order5["external_id"]: False},
    }

    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=order1["id"], charged_amount="50.0000"
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=order2["id"], charged_amount="100.0000"
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=order3["id"], charged_amount="10.0000"
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=order4["id"], charged_amount="50.0000"
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=order5["id"], charged_amount="100.0000"
            ),
        ],
        bill_for_timestamp=bill_for_timestamp,
    )
    await api.post(
        API_URL,
        input_bp,
        decode_as=orders_charge_pb2.OrdersChargeOutput,
        allowed_status_codes=[201],
    )

    assert await factory.get_orders_logs(order1["id"]) == [
        {
            "id": Any(int),
            "order_id": order1["id"],
            "created_at": Any(datetime),
            "op_type": OrderOperationType.DEBIT.name,
            "amount": Decimal("50"),
            "consumed": Decimal("150"),
            "limit": Decimal("200"),
            "billed_due_to": bill_for_dt,
        }
    ]
    assert await factory.get_orders_logs(order2["id"]) == []
    assert await factory.get_orders_logs(order3["id"]) == [
        {
            "id": Any(int),
            "order_id": order3["id"],
            "created_at": Any(datetime),
            "op_type": OrderOperationType.DEBIT.name,
            "amount": Decimal("10"),
            "consumed": Decimal("10"),
            "limit": Decimal("100"),
            "billed_due_to": bill_for_dt,
        }
    ]


async def test_returns_appropriately_if_balance_return_combined(
    api, factory, balance_client
):
    order1 = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    order2 = await factory.create_order(limit=Decimal("400"), consumed=Decimal("150"))
    order3 = await factory.create_order(limit=Decimal("100"), consumed=Decimal("0"))
    order4 = await factory.create_order(
        limit=Decimal("200"), consumed=Decimal("100"), service_id=37
    )
    order5 = await factory.create_order(
        limit=Decimal("400"), consumed=Decimal("150"), service_id=37
    )
    balance_client.update_orders.coro.return_value = {
        110: {order1["id"]: True, order2["id"]: False, order3["id"]: True},
        37: {order4["external_id"]: True, order5["external_id"]: False},
    }

    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=order1["id"], charged_amount="50.0000"
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=order2["id"], charged_amount="100.0000"
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=order3["id"], charged_amount="10.0000"
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=order4["id"], charged_amount="50.0000"
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=order5["id"], charged_amount="100.0000"
            ),
        ],
        bill_for_timestamp=bill_for_timestamp,
    )
    result = await api.post(
        API_URL,
        input_bp,
        decode_as=orders_charge_pb2.OrdersChargeOutput,
        allowed_status_codes=[201],
    )

    assert result == orders_charge_pb2.OrdersChargeOutput(
        charge_result=[
            orders_charge_pb2.OrderChargeOutput(order_id=order1["id"], success=True),
            orders_charge_pb2.OrderChargeOutput(order_id=order2["id"], success=False),
            orders_charge_pb2.OrderChargeOutput(order_id=order3["id"], success=True),
            orders_charge_pb2.OrderChargeOutput(order_id=order4["id"], success=True),
            orders_charge_pb2.OrderChargeOutput(order_id=order5["id"], success=False),
        ],
        applied=True,
    )


async def test_returns_error_if_balance_fails(api, factory, balance_client):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    balance_client.update_orders.coro.side_effect = BalanceApiError()

    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=order["id"], charged_amount="50.0000"
            )
        ],
        bill_for_timestamp=bill_for_timestamp,
    )

    await api.post(
        API_URL,
        input_bp,
        expected_error=(common_pb2.Error.BALANCE_API_ERROR, "Balance API error"),
        allowed_status_codes=[503],
    )


async def test_not_updates_consumed_if_balance_fails(api, factory, balance_client):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    balance_client.update_orders.coro.side_effect = BalanceApiError()

    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=order["id"], charged_amount="50.0000"
            )
        ],
        bill_for_timestamp=bill_for_timestamp,
    )
    await api.post(API_URL, input_bp, allowed_status_codes=[503])

    assert (await factory.get_order(order["id"]))["consumed"] == Decimal("100")


async def test_not_creates_order_log_if_balance_fails(api, factory, balance_client):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    balance_client.update_orders.coro.side_effect = BalanceApiError()

    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=order["id"], charged_amount="50.0000"
            )
        ],
        bill_for_timestamp=bill_for_timestamp,
    )
    await api.post(API_URL, input_bp)

    assert await factory.get_orders_logs(order["id"]) == []


async def test_not_replaces_existing_order_logs_if_balance_ok(
    api, factory, balance_client
):
    order1 = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    order2 = await factory.create_order(
        limit=Decimal("200"), consumed=Decimal("100"), service_id=37
    )
    order_log1 = await factory.create_order_log(
        op_type=OrderOperationType.CREDIT,
        order_id=order1["id"],
        amount=Decimal("200"),
        consumed=Decimal("0"),
        limit=Decimal("200"),
    )
    order_log2 = await factory.create_order_log(
        op_type=OrderOperationType.DEBIT,
        order_id=order1["id"],
        amount=Decimal("100"),
        consumed=Decimal("100"),
        limit=Decimal("200"),
    )
    order_log3 = await factory.create_order_log(
        op_type=OrderOperationType.CREDIT,
        order_id=order2["id"],
        amount=Decimal("200"),
        consumed=Decimal("0"),
        limit=Decimal("200"),
    )
    balance_client.update_orders.coro.return_value = {
        110: {order1["id"]: True},
        37: {order2["external_id"]: True},
    }

    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=order1["id"], charged_amount="50.0000"
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=order2["id"], charged_amount="50.0000"
            ),
        ],
        bill_for_timestamp=bill_for_timestamp,
    )
    await api.post(
        API_URL,
        input_bp,
        decode_as=orders_charge_pb2.OrdersChargeOutput,
        allowed_status_codes=[201],
    )

    order_logs = await factory.get_orders_logs(order1["id"])
    expected = [
        order_log1,
        order_log2,
        {
            "id": Any(int),
            "order_id": order1["id"],
            "created_at": Any(datetime),
            "op_type": OrderOperationType.DEBIT.name,
            "amount": Decimal("50"),
            "consumed": Decimal("150"),
            "limit": Decimal("200"),
            "billed_due_to": bill_for_dt,
        },
    ]
    sort_key = itemgetter("consumed", "limit")
    assert sorted(order_logs, key=sort_key) == sorted(expected, key=sort_key)
    order_logs = await factory.get_orders_logs(order2["id"])
    expected = [
        order_log3,
        {
            "id": Any(int),
            "order_id": order2["id"],
            "created_at": Any(datetime),
            "op_type": OrderOperationType.DEBIT.name,
            "amount": Decimal("50"),
            "consumed": Decimal("150"),
            "limit": Decimal("200"),
            "billed_due_to": bill_for_dt,
        },
    ]
    sort_key = itemgetter("consumed", "limit")
    assert sorted(order_logs, key=sort_key) == sorted(expected, key=sort_key)


async def test_not_removes_existing_order_logs_if_balance_returns_false(
    api, factory, balance_client
):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    order_log1 = await factory.create_order_log(
        op_type=OrderOperationType.CREDIT,
        order_id=order["id"],
        amount=Decimal("200"),
        consumed=Decimal("0"),
        limit=Decimal("200"),
    )
    order_log2 = await factory.create_order_log(
        op_type=OrderOperationType.DEBIT,
        order_id=order["id"],
        amount=Decimal("100"),
        consumed=Decimal("100"),
        limit=Decimal("200"),
    )
    balance_client.update_orders.coro.return_value = {110: {order["id"]: False}}

    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=order["id"], charged_amount="50.0000"
            )
        ],
        bill_for_timestamp=bill_for_timestamp,
    )
    await api.post(API_URL, input_bp)

    order_logs = await factory.get_orders_logs(order["id"])
    expected = [order_log1, order_log2]
    sort_key = itemgetter("consumed", "limit")
    assert sorted(order_logs, key=sort_key) == sorted(expected, key=sort_key)


async def test_not_charges_in_balance_if_all_orders_locally_rejected(
    api, factory, balance_client
):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("200"))

    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=order["id"], charged_amount="10.0000"
            )
        ],
        bill_for_timestamp=bill_for_timestamp,
    )
    await api.post(API_URL, input_bp)

    assert not balance_client.update_orders.called
