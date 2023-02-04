from datetime import datetime, timezone
from decimal import Decimal

import pytest

from maps_adv.billing_proxy.lib.db.enums import OrderOperationType
from maps_adv.billing_proxy.proto import common_pb2, orders_charge_pb2
from maps_adv.billing_proxy.tests.helpers import dt_to_proto

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("db")]

API_URL = "/orders/charge/"
bill_for_dt = datetime(2000, 2, 1, tzinfo=timezone.utc)


@pytest.fixture(autouse=True)
async def orders_with_logs(factory):
    order1 = await factory.create_order(
        id=11, limit=Decimal("1000"), consumed=Decimal("100")
    )
    await factory.create_order_log(
        order_id=order1["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("100"),
        billed_due_to=bill_for_dt,
    )
    order2 = await factory.create_order(
        id=12, limit=Decimal("1000"), consumed=Decimal("200")
    )
    await factory.create_order_log(
        order_id=order2["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("200"),
        billed_due_to=bill_for_dt,
    )
    order3 = await factory.create_order(
        id=13, limit=Decimal("1000"), consumed=Decimal("300")
    )
    await factory.create_order_log(
        order_id=order3["id"],
        op_type=OrderOperationType.DEBIT,
        amount=Decimal("300"),
        billed_due_to=bill_for_dt,
    )
    await factory.create_order(id=14, limit=Decimal("1000"), consumed=Decimal("0"))


async def test_return_values_if_bill_for_dt_already_present_on_order_logs_and_request_matches_logs(  # noqa: E501
    api,
):
    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=11,
                OBSOLETE__charged_amount=common_pb2.MoneyQuantity(value=1000000),
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=12,
                OBSOLETE__charged_amount=common_pb2.MoneyQuantity(value=2000000),
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=13,
                OBSOLETE__charged_amount=common_pb2.MoneyQuantity(value=3000000),
            ),
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
            orders_charge_pb2.OrderChargeOutput(order_id=11, success=True),
            orders_charge_pb2.OrderChargeOutput(order_id=12, success=True),
            orders_charge_pb2.OrderChargeOutput(order_id=13, success=True),
        ],
        applied=False,
    )


async def test_return_values_if_bill_for_dt_already_present_in_order_logs_and_logs_have_less_orders(  # noqa: E501
    api,
):
    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=11,
                OBSOLETE__charged_amount=common_pb2.MoneyQuantity(value=1000000),
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=12,
                OBSOLETE__charged_amount=common_pb2.MoneyQuantity(value=2000000),
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=13,
                OBSOLETE__charged_amount=common_pb2.MoneyQuantity(value=3000000),
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=14,
                OBSOLETE__charged_amount=common_pb2.MoneyQuantity(value=4000000),
            ),
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
            orders_charge_pb2.OrderChargeOutput(order_id=11, success=True),
            orders_charge_pb2.OrderChargeOutput(order_id=12, success=True),
            orders_charge_pb2.OrderChargeOutput(order_id=13, success=True),
            orders_charge_pb2.OrderChargeOutput(order_id=14, success=False),
        ],
        applied=False,
    )


async def test_raises_if_bill_for_dt_already_present_in_order_logs_and_logs_have_more_orders(  # noqa: E501
    api,
):
    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=11,
                OBSOLETE__charged_amount=common_pb2.MoneyQuantity(value=1000000),
            )
        ],
        bill_for_timestamp=dt_to_proto(bill_for_dt),
    )
    await api.post(
        API_URL,
        input_bp,
        expected_error=(common_pb2.Error.BAD_DUPLICATE_CHARGE, "order_ids=[12, 13]"),
        allowed_status_codes=[422],
    )


async def test_raises_if_bill_for_dt_already_present_in_order_logs_and_amounts_differ(
    api,
):
    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=[
            orders_charge_pb2.OrderChargeInput(
                order_id=11,
                OBSOLETE__charged_amount=common_pb2.MoneyQuantity(value=1000000),
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=12,
                OBSOLETE__charged_amount=common_pb2.MoneyQuantity(value=8000000),
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=13,
                OBSOLETE__charged_amount=common_pb2.MoneyQuantity(value=9000000),
            ),
        ],
        bill_for_timestamp=dt_to_proto(bill_for_dt),
    )
    await api.post(
        API_URL,
        input_bp,
        expected_error=(common_pb2.Error.BAD_DUPLICATE_CHARGE, "order_ids=[12, 13]"),
        allowed_status_codes=[422],
    )


@pytest.mark.parametrize(
    "orders_charge",
    [
        [
            orders_charge_pb2.OrderChargeInput(
                order_id=11,
                OBSOLETE__charged_amount=common_pb2.MoneyQuantity(value=1000000),
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=12,
                OBSOLETE__charged_amount=common_pb2.MoneyQuantity(value=1000000),
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=13,
                OBSOLETE__charged_amount=common_pb2.MoneyQuantity(value=2000000),
            ),
        ],
        [
            orders_charge_pb2.OrderChargeInput(
                order_id=11,
                OBSOLETE__charged_amount=common_pb2.MoneyQuantity(value=1000000),
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=12,
                OBSOLETE__charged_amount=common_pb2.MoneyQuantity(value=2000000),
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=13,
                OBSOLETE__charged_amount=common_pb2.MoneyQuantity(value=3000000),
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=14,
                OBSOLETE__charged_amount=common_pb2.MoneyQuantity(value=4000000),
            ),
        ],
        [
            orders_charge_pb2.OrderChargeInput(
                order_id=11,
                OBSOLETE__charged_amount=common_pb2.MoneyQuantity(value=1000000),
            )
        ],
        [
            orders_charge_pb2.OrderChargeInput(
                order_id=11,
                OBSOLETE__charged_amount=common_pb2.MoneyQuantity(value=1000000),
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=12,
                OBSOLETE__charged_amount=common_pb2.MoneyQuantity(value=8000000),
            ),
            orders_charge_pb2.OrderChargeInput(
                order_id=13,
                OBSOLETE__charged_amount=common_pb2.MoneyQuantity(value=9000000),
            ),
        ],
    ],
)
async def test_not_charges_if_bill_for_dt_already_present_on_order_logs(
    api, factory, balance_client, orders_charge
):
    input_bp = orders_charge_pb2.OrdersChargeInput(
        orders_charge=orders_charge, bill_for_timestamp=dt_to_proto(bill_for_dt)
    )
    await api.post(API_URL, input_bp)

    balance_client.update_orders.assert_not_called()

    assert (await factory.get_order(11))["consumed"] == Decimal("100")
    assert len(await factory.get_orders_logs(11)) == 1
    assert (await factory.get_order(12))["consumed"] == Decimal("200")
    assert len(await factory.get_orders_logs(12)) == 1
    assert (await factory.get_order(13))["consumed"] == Decimal("300")
    assert len(await factory.get_orders_logs(13)) == 1
    assert (await factory.get_order(14))["consumed"] == Decimal("0")
    assert len(await factory.get_orders_logs(14)) == 0
