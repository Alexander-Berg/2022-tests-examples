from datetime import datetime, timedelta, timezone
from decimal import Decimal
from operator import itemgetter

import pytest

from maps_adv.billing_proxy.lib.core.balance_client import BalanceApiError
from maps_adv.billing_proxy.lib.data_manager import OrderOperationType
from maps_adv.billing_proxy.tests.helpers import Any, true_for_all_orders

pytestmark = [pytest.mark.asyncio]

bill_for_dt = datetime(2000, 2, 2, 3, 4, 5, tzinfo=timezone.utc)


@pytest.fixture(autouse=True)
def freeze_2000(freezer):
    freezer.move_to(bill_for_dt + timedelta(minutes=5))


async def test_charges_in_balance(con, factory, orders_dm, balance_client):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    balance_client.update_orders.coro.side_effect = true_for_all_orders

    await orders_dm.charge_orders({order["id"]: Decimal("50")}, bill_for_dt, con=con)

    balance_client.update_orders.assert_called_with(
        {order["id"]: {"consumed": Decimal("150"), "service_id": 110}}, bill_for_dt
    )


async def test_charges_in_balance_for_geoprod(con, factory, orders_dm, balance_client):
    order = await factory.create_order(
        limit=Decimal("200"), consumed=Decimal("100"), service_id=37
    )
    balance_client.update_orders.coro.side_effect = true_for_all_orders

    await orders_dm.charge_orders({order["id"]: Decimal("50")}, bill_for_dt, con=con)

    balance_client.update_orders.assert_called_with(
        {order["external_id"]: {"consumed": Decimal("150.000000"), "service_id": 37}},
        bill_for_dt,
    )


async def test_charges_multiple_in_balance(con, factory, orders_dm, balance_client):
    order1 = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    order2 = await factory.create_order(limit=Decimal("400"), consumed=Decimal("200"))
    order3 = await factory.create_order(
        limit=Decimal("400"), consumed=Decimal("200"), service_id=37
    )
    balance_client.update_orders.coro.side_effect = true_for_all_orders

    await orders_dm.charge_orders(
        {
            order1["id"]: Decimal("50.000000"),
            order2["id"]: Decimal("150.000000"),
            order3["id"]: Decimal("150.000000"),
        },
        bill_for_dt,
        con=con,
    )

    balance_client.update_orders.assert_called_with(
        {
            order1["id"]: {"consumed": Decimal("150.000000"), "service_id": 110},
            order2["id"]: {"consumed": Decimal("350.000000"), "service_id": 110},
            order3["external_id"]: {
                "consumed": Decimal("350.000000"),
                "service_id": 37,
            },
        },
        bill_for_dt,
    )


async def test_updates_consumed_if_balance_returns_true(
    con, factory, orders_dm, balance_client
):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    balance_client.update_orders.coro.side_effect = true_for_all_orders

    await orders_dm.charge_orders({order["id"]: Decimal("50")}, bill_for_dt, con=con)

    assert (await factory.get_order(order["id"]))["consumed"] == Decimal("150")


async def test_creates_order_log_if_balance_returns_true(
    con, factory, orders_dm, balance_client
):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    balance_client.update_orders.coro.side_effect = true_for_all_orders

    await orders_dm.charge_orders({order["id"]: Decimal("50")}, bill_for_dt, con=con)

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


async def test_returns_true_if_balance_returns_true(
    con, factory, orders_dm, balance_client
):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    balance_client.update_orders.coro.side_effect = true_for_all_orders

    result = await orders_dm.charge_orders(
        {order["id"]: Decimal("50")}, bill_for_dt, con=con
    )

    assert result == {order["id"]: True}


async def test_not_updates_consumed_if_balance_returns_false(
    con, factory, orders_dm, balance_client
):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    balance_client.update_orders.coro.return_value = {110: {order["id"]: False}}

    await orders_dm.charge_orders({order["id"]: Decimal("50")}, bill_for_dt, con=con)

    assert (await factory.get_order(order["id"]))["consumed"] == Decimal("100")


async def test_not_creates_order_log_if_balance_returns_false(
    con, factory, orders_dm, balance_client
):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    balance_client.update_orders.coro.return_value = {110: {order["id"]: False}}

    await orders_dm.charge_orders({order["id"]: Decimal("50")}, bill_for_dt, con=con)

    assert await factory.get_orders_logs(order["id"]) == []


async def test_returns_false_if_balance_returns_false(
    con, factory, orders_dm, balance_client
):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    balance_client.update_orders.coro.return_value = {110: {order["id"]: False}}

    result = await orders_dm.charge_orders(
        {order["id"]: Decimal("50")}, bill_for_dt, con=con
    )

    assert result == {order["id"]: False}


async def test_updates_consumed_appropriately_if_balance_return_combined(
    con, factory, orders_dm, balance_client
):
    order1 = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    order2 = await factory.create_order(limit=Decimal("400"), consumed=Decimal("150"))
    order3 = await factory.create_order(limit=Decimal("100"), consumed=Decimal("0"))
    balance_client.update_orders.coro.return_value = {
        110: {order1["id"]: True, order2["id"]: False, order3["id"]: True}
    }

    await orders_dm.charge_orders(
        {
            order1["id"]: Decimal("50"),
            order2["id"]: Decimal("100"),
            order3["id"]: Decimal("10"),
        },
        bill_for_dt,
        con=con,
    )

    assert (await factory.get_order(order1["id"]))["consumed"] == Decimal("150")
    assert (await factory.get_order(order2["id"]))["consumed"] == Decimal("150")
    assert (await factory.get_order(order3["id"]))["consumed"] == Decimal("10")


async def test_creates_order_logs_appropriately_if_balance_return_combined(
    con, factory, orders_dm, balance_client
):
    order1 = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    order2 = await factory.create_order(limit=Decimal("400"), consumed=Decimal("150"))
    order3 = await factory.create_order(limit=Decimal("100"), consumed=Decimal("0"))
    balance_client.update_orders.coro.return_value = {
        110: {order1["id"]: True, order2["id"]: False, order3["id"]: True}
    }

    await orders_dm.charge_orders(
        {
            order1["id"]: Decimal("50"),
            order2["id"]: Decimal("100"),
            order3["id"]: Decimal("10"),
        },
        bill_for_dt,
        con=con,
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
    con, factory, orders_dm, balance_client
):
    order1 = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    order2 = await factory.create_order(limit=Decimal("400"), consumed=Decimal("150"))
    order3 = await factory.create_order(limit=Decimal("100"), consumed=Decimal("0"))
    balance_client.update_orders.coro.return_value = {
        110: {order1["id"]: True, order2["id"]: False, order3["id"]: True}
    }

    result = await orders_dm.charge_orders(
        {
            order1["id"]: Decimal("50"),
            order2["id"]: Decimal("100"),
            order3["id"]: Decimal("10"),
        },
        bill_for_dt,
        con=con,
    )

    return result == {order1["id"]: True, order2["id"]: False, order3["id"]: True}


async def test_raises_if_balance_fails(con, factory, orders_dm, balance_client):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    balance_client.update_orders.coro.side_effect = BalanceApiError()

    with pytest.raises(BalanceApiError):
        await orders_dm.charge_orders(
            {order["id"]: Decimal("50")}, bill_for_dt, con=con
        )


async def test_not_updates_consumed_if_balance_fails(
    con, factory, orders_dm, balance_client
):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    balance_client.update_orders.coro.side_effect = BalanceApiError()

    try:
        await orders_dm.charge_orders(
            {order["id"]: Decimal("50")}, bill_for_dt, con=con
        )
    except BalanceApiError:
        pass

    assert (await factory.get_order(order["id"]))["consumed"] == Decimal("100")


async def test_not_creates_order_logs_if_balance_fails(
    con, factory, orders_dm, balance_client
):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    balance_client.update_orders.coro.side_effect = BalanceApiError()

    try:
        await orders_dm.charge_orders(
            {order["id"]: Decimal("50")}, bill_for_dt, con=con
        )
    except BalanceApiError:
        pass

    assert await factory.get_orders_logs(order["id"]) == []


async def test_not_replaces_existing_order_logs_if_balance_ok(
    con, factory, orders_dm, balance_client
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
    balance_client.update_orders.coro.side_effect = true_for_all_orders

    await orders_dm.charge_orders({order["id"]: Decimal("50")}, bill_for_dt, con=con)

    order_logs = await factory.get_orders_logs(order["id"])
    expected = [
        order_log1,
        order_log2,
        {
            "id": Any(int),
            "order_id": order["id"],
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
    con, factory, orders_dm, balance_client
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
    balance_client.update_orders.coro.return_value = {order["id"]: False}

    await orders_dm.charge_orders({order["id"]: Decimal("50")}, bill_for_dt, con=con)

    order_logs = await factory.get_orders_logs(order["id"])
    expected = [order_log1, order_log2]
    sort_key = itemgetter("consumed", "limit")
    assert sorted(order_logs, key=sort_key) == sorted(expected, key=sort_key)


@pytest.mark.config(SKIP_BALANCE_API_CALL_ON_ORDERS_CHARGE=True)
async def test_not_charges_in_balance_if_balance_sync_is_false(
    con, factory, orders_dm, balance_client
):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))

    await orders_dm.charge_orders({order["id"]: Decimal("50")}, bill_for_dt, con=con)

    assert not balance_client.update_orders.called


@pytest.mark.config(SKIP_BALANCE_API_CALL_ON_ORDERS_CHARGE=True)
async def test_updates_consumed_if_balance_sync_is_false(con, factory, orders_dm):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))

    await orders_dm.charge_orders({order["id"]: Decimal("50")}, bill_for_dt, con=con)

    assert (await factory.get_order(order["id"]))["consumed"] == Decimal("150")


@pytest.mark.config(SKIP_BALANCE_API_CALL_ON_ORDERS_CHARGE=True)
async def test_creates_order_logs_if_balance_sync_is_false(con, factory, orders_dm):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))

    await orders_dm.charge_orders({order["id"]: Decimal("50")}, bill_for_dt, con=con)

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


@pytest.mark.config(SKIP_BALANCE_API_CALL_ON_ORDERS_CHARGE=True)
async def test_returns_true_if_balance_sync_is_false(
    con, factory, orders_dm, balance_client
):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))

    result = await orders_dm.charge_orders(
        {order["id"]: Decimal("50")}, bill_for_dt, con=con
    )

    assert result == {order["id"]: True}
    assert not balance_client.update_orders.called


@pytest.mark.config(USE_RECALCULATE_STATISTIC_MODE=True)
async def test_calls_update_orders_with_now_datetime_if_use_recalculate_statistics_mode(
    con, factory, orders_dm, balance_client, freeze_2000
):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    balance_client.update_orders.coro.side_effect = true_for_all_orders

    await orders_dm.charge_orders({order["id"]: Decimal("50")}, bill_for_dt, con=con)

    balance_client.update_orders.assert_called_with(
        {order["id"]: {"consumed": Decimal("150.000000"), "service_id": 110}},
        datetime(2000, 2, 2, 3, 9, 5, tzinfo=timezone.utc),
    )


@pytest.mark.config(USE_RECALCULATE_STATISTIC_MODE=True)
async def test_saves_correct_billed_due_to_datetime_when_use_recalculate_statistics_mode(  # noqa: E501
    con, factory, orders_dm, balance_client, freeze_2000
):
    order = await factory.create_order(limit=Decimal("200"), consumed=Decimal("100"))
    balance_client.update_orders.coro.side_effect = true_for_all_orders

    await orders_dm.charge_orders({order["id"]: Decimal("50")}, bill_for_dt, con=con)

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


async def test_not_charges_in_balance_if_all_orders_locally_filtered_out(
    con, orders_dm, balance_client
):
    await orders_dm.charge_orders({}, bill_for_dt, con=con)

    assert not balance_client.update_orders.called
