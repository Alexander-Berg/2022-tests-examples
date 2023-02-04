from _decimal import Decimal

import pytest

from maps_adv.billing_proxy.client.lib.exceptions import UnknownResponse
from maps_adv.common.helpers import dt
from maps_adv.statistics.beekeeper.lib.steps import BillingNotification

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize("applied", [True, False])
async def test_returns_result_data(applied, billing_client_mock):
    billing_client_mock.submit_orders_charges.coro.return_value = (
        applied,
        {111: True, 222: False},
    )

    billing_charger = BillingNotification(billing=billing_client_mock)

    data = await billing_charger.run(
        {
            "packet_end": dt(300),
            "orders": [
                {"order_id": 111, "amount_to_bill": Decimal("10.35")},
                {"order_id": 222, "amount_to_bill": Decimal("10.36")},
            ],
        },
        None,
    )

    assert data == {
        "packet_end": dt(300),
        "billing_applied": applied,
        "orders": [
            {
                "order_id": 111,
                "billing_success": True,
                "amount_to_bill": Decimal("10.35"),
            },
            {
                "order_id": 222,
                "billing_success": False,
                "amount_to_bill": Decimal("10.36"),
            },
        ],
    }


@pytest.mark.parametrize(
    ["orders", "packet_end", "charges"],
    [
        (
            [{"order_id": 111, "amount_to_bill": Decimal("10.35")}],
            dt(300),
            {111: Decimal("10.35")},
        ),
        (
            [
                {"order_id": 111, "amount_to_bill": Decimal("10.35")},
                {"order_id": 222, "amount_to_bill": Decimal("10.36")},
            ],
            dt(500),
            {111: Decimal("10.35"), 222: Decimal("10.36")},
        ),
    ],
)
async def test_expects_arguments_call_billing_client(
    packet_end, orders, charges, billing_client_mock
):
    billing_client_mock.submit_orders_charges.coro.return_value = (
        False,
        {111: True, 222: False, 333: False},
    )

    billing_charger = BillingNotification(billing=billing_client_mock)

    await billing_charger.run({"packet_end": packet_end, "orders": orders}, None)

    billing_client_mock.submit_orders_charges.assert_called_once_with(
        charges=charges, bill_due_to=packet_end
    )


@pytest.mark.parametrize(
    "orders", [[], [{"order_id": 111, "amount_to_bill": Decimal("0")}]]
)
async def test_no_called_billing_proxy_client(orders, billing_client_mock):
    billing_charger = BillingNotification(billing=billing_client_mock)

    await billing_charger.run({"packet_end": dt(300), "orders": orders}, None)

    assert (
        not billing_client_mock.submit_orders_charges.called
    ), "Attempt to call Billing Charger"


async def test_sets_none_for_zero_charges(billing_client_mock):
    billing_charger = BillingNotification(billing=billing_client_mock)

    data = await billing_charger.run(
        {
            "packet_end": dt(300),
            "orders": [{"order_id": 111, "amount_to_bill": Decimal("0")}],
        },
        None,
    )

    assert data["orders"] == [
        {"order_id": 111, "billing_success": None, "amount_to_bill": Decimal("0")}
    ]


async def test_returns_nothing_if_empty_orders(billing_client_mock):
    billing_charger = BillingNotification(billing=billing_client_mock)

    data = await billing_charger.run({"packet_end": dt(300), "orders": []}, None)

    assert data["orders"] == []
    assert (
        not billing_client_mock.submit_orders_charges.called
    ), "Attempt to call Billing Charger"


async def test_raises_for_unknown_error_from_billing_proxy_client(billing_client_mock):
    billing_client_mock.submit_orders_charges.coro.side_effect = UnknownResponse(500)
    billing_charger = BillingNotification(billing=billing_client_mock)

    with pytest.raises(UnknownResponse):
        await billing_charger.run(
            {
                "packet_end": dt(300),
                "orders": [{"order_id": 111, "amount_to_bill": Decimal("30")}],
            },
            None,
        )
