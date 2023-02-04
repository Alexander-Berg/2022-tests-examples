from datetime import datetime, timedelta, timezone
from decimal import Decimal

import pytest

from maps_adv.billing_proxy.lib.domain import OrderOperationType
from maps_adv.billing_proxy.proto import orders_charge_pb2
from maps_adv.billing_proxy.tests.helpers import Any, dt_to_proto, true_for_all_orders

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("db")]

API_URL = "/orders/charge/"

bill_for_dt = datetime(2000, 2, 2, 3, 4, 5, tzinfo=timezone.utc)
bill_for_timestamp = dt_to_proto(bill_for_dt)


@pytest.fixture(autouse=True)
def freeze_2000(freezer):
    freezer.move_to(bill_for_dt + timedelta(minutes=5))


@pytest.fixture(autouse=True)
def common_balance_client_mocks(balance_client):
    balance_client.update_orders.coro.side_effect = true_for_all_orders


async def test_returns_false_if_insufficient_funds(api, factory):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))

    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=order["id"], charged_amount="150.0000"
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


async def test_not_updates_consumed_if_insufficient_funds(api, factory):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))

    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=order["id"], charged_amount="150.0000"
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


async def test_not_creates_order_log_if_insufficient_funds(api, factory):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))

    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=order["id"], charged_amount="150.0000"
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


async def test_not_charges_in_balance_if_insufficient_funds(
    api, factory, balance_client
):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))

    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=order["id"], charged_amount="150.0000"
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

    assert not balance_client.create_deposit_request.called


async def test_returns_appropriately_if_insufficient_funds_for_some(api, factory):
    order1 = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    order2 = await factory.create_order(limit=Decimal("400"), consumed=Decimal("150"))
    order3 = await factory.create_order(limit=Decimal("100"), consumed=Decimal("0"))

    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=order1["id"], charged_amount="50.0000"
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=order2["id"], charged_amount="300.0000"
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=order3["id"], charged_amount="100.0000"
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
        ],
        applied=True,
    )


async def test_updates_consumed_appropriately_if_insufficient_funds_for_some(
    api, factory
):
    order1 = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    order2 = await factory.create_order(limit=Decimal("400"), consumed=Decimal("150"))
    order3 = await factory.create_order(limit=Decimal("100"), consumed=Decimal("0"))

    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=order1["id"], charged_amount="50.0000"
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=order2["id"], charged_amount="300.0000"
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=order3["id"], charged_amount="100.0000"
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
    assert (await factory.get_order(order3["id"]))["consumed"] == Decimal("100")


async def test_creates_order_log_appropriately_if_insufficient_funds_for_some(
    api, factory
):
    order1 = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    order2 = await factory.create_order(limit=Decimal("400"), consumed=Decimal("150"))
    order3 = await factory.create_order(limit=Decimal("100"), consumed=Decimal("0"))

    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=order1["id"], charged_amount="50.0000"
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=order2["id"], charged_amount="300.0000"
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=order3["id"], charged_amount="100.0000"
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
            "amount": Decimal("100"),
            "consumed": Decimal("100"),
            "limit": Decimal("100"),
            "billed_due_to": bill_for_dt,
        }
    ]


async def test_charges_in_balance_appropriately_if_insufficient_funds_for_some(
    api, factory, balance_client
):
    order1 = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    order2 = await factory.create_order(limit=Decimal("400"), consumed=Decimal("150"))
    order3 = await factory.create_order(limit=Decimal("100"), consumed=Decimal("0"))

    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=order1["id"], charged_amount="50.0000"
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=order2["id"], charged_amount="300.0000"
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=order3["id"], charged_amount="100.0000"
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

    assert not balance_client.update_orders.call_args[0] == (
        {order1["id"]: Decimal("150"), order3["id"]: Decimal("100")},
    )
