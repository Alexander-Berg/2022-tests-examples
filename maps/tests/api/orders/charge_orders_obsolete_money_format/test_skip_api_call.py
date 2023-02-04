from datetime import datetime, timedelta, timezone
from decimal import Decimal

import pytest

from maps_adv.billing_proxy.lib.domain import OrderOperationType
from maps_adv.billing_proxy.proto import common_pb2, orders_charge_pb2
from maps_adv.billing_proxy.tests.helpers import Any, dt_to_proto

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.usefixtures("db"),
]

API_URL = "/orders/charge/"

bill_for_dt = datetime(2000, 2, 2, 3, 4, 5, tzinfo=timezone.utc)
bill_for_timestamp = dt_to_proto(bill_for_dt)


@pytest.fixture(autouse=True)
def freeze_2000(freezer):
    freezer.move_to(bill_for_dt + timedelta(minutes=5))


@pytest.mark.config(SKIP_BALANCE_API_CALL_ON_ORDERS_CHARGE=True)
async def test_not_charges_in_balance_if_balance_sync_is_false(
    api, factory, balance_client
):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))

    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=order["id"],
                OBSOLETE__charged_amount=common_pb2.MoneyQuantity(value=1500000),
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

    assert not balance_client.update_orders.called


@pytest.mark.config(SKIP_BALANCE_API_CALL_ON_ORDERS_CHARGE=True)
async def test_updates_consumed_depending_on_local_data_if_balance_sync_is_false(
    api, factory
):
    order1 = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    order2 = await factory.create_order(limit=Decimal("400"), consumed=Decimal("150"))
    order3 = await factory.create_order(limit=Decimal("100"), consumed=Decimal("0"))

    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=order1["id"],
                OBSOLETE__charged_amount=common_pb2.MoneyQuantity(value=500000),
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=order2["id"],
                OBSOLETE__charged_amount=common_pb2.MoneyQuantity(value=3000000),
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=order3["id"],
                OBSOLETE__charged_amount=common_pb2.MoneyQuantity(value=1000000),
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


@pytest.mark.config(SKIP_BALANCE_API_CALL_ON_ORDERS_CHARGE=True)
async def test_creates_order_logs_depending_on_local_data_if_balance_sync_is_false(
    api, factory
):
    order1 = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    order2 = await factory.create_order(limit=Decimal("400"), consumed=Decimal("150"))
    order3 = await factory.create_order(limit=Decimal("100"), consumed=Decimal("0"))

    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=order1["id"],
                OBSOLETE__charged_amount=common_pb2.MoneyQuantity(value=500000),
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=order2["id"],
                OBSOLETE__charged_amount=common_pb2.MoneyQuantity(value=3000000),
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=order3["id"],
                OBSOLETE__charged_amount=common_pb2.MoneyQuantity(value=1000000),
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


@pytest.mark.config(SKIP_BALANCE_API_CALL_ON_ORDERS_CHARGE=True)
async def test_returns_depending_on_local_data_if_balance_sync_is_false(api, factory):
    order1 = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    order2 = await factory.create_order(limit=Decimal("400"), consumed=Decimal("150"))
    order3 = await factory.create_order(limit=Decimal("100"), consumed=Decimal("0"))

    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=order1["id"],
                OBSOLETE__charged_amount=common_pb2.MoneyQuantity(value=500000),
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=order2["id"],
                OBSOLETE__charged_amount=common_pb2.MoneyQuantity(value=3000000),
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=order3["id"],
                OBSOLETE__charged_amount=common_pb2.MoneyQuantity(value=1000000),
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
