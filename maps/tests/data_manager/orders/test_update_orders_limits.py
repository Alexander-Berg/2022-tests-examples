from datetime import datetime
from decimal import Decimal
from operator import itemgetter

import pytest

from maps_adv.billing_proxy.lib.data_manager import OrderOperationType
from maps_adv.billing_proxy.lib.data_manager.exceptions import OrdersDoNotExist
from maps_adv.billing_proxy.tests.helpers import Any

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("db")]


@pytest.mark.parametrize("hidden", [False, True])
async def test_updates_order_limit(factory, orders_dm, hidden):
    order = await factory.create_order(
        limit=Decimal("100"), consumed=Decimal("50"), tid=100, hidden=hidden
    )

    await orders_dm.update_orders_limits(
        {order["id"]: {"new_limit": Decimal("200.1234"), "tid": 123}}
    )

    assert (await factory.get_order(order["id"]))["limit"] == Decimal("200.1234")


@pytest.mark.parametrize("hidden", [False, True])
async def test_updates_tid(factory, orders_dm, hidden):
    order = await factory.create_order(
        limit=Decimal("100"), consumed=Decimal("50"), tid=100, hidden=hidden
    )

    await orders_dm.update_orders_limits(
        {order["id"]: {"new_limit": Decimal("200.1234"), "tid": 123}}
    )

    assert (await factory.get_order(order["id"]))["tid"] == 123


@pytest.mark.parametrize("hidden", [False, True])
async def test_creates_order_log(factory, orders_dm, hidden):
    order = await factory.create_order(
        limit=Decimal("100.1234"), consumed=Decimal("50"), tid=100, hidden=hidden
    )

    await orders_dm.update_orders_limits(
        {order["id"]: {"new_limit": Decimal("200.9876"), "tid": 123}}
    )

    assert await factory.get_orders_logs(order["id"]) == [
        {
            "id": Any(int),
            "order_id": order["id"],
            "created_at": Any(datetime),
            "op_type": OrderOperationType.CREDIT.name,
            "amount": Decimal("100.8642"),
            "consumed": Decimal("50"),
            "limit": Decimal("200.9876"),
            "billed_due_to": None,
        }
    ]


async def test_updates_multiple_orders_limits(factory, orders_dm):
    order1 = await factory.create_order(
        limit=Decimal("100"), consumed=Decimal("50"), tid=100
    )
    order2 = await factory.create_order(
        limit=Decimal("200"), consumed=Decimal("150"), tid=100
    )

    await orders_dm.update_orders_limits(
        {
            order1["id"]: {"new_limit": Decimal("300"), "tid": 123},
            order2["id"]: {"new_limit": Decimal("500"), "tid": 123},
        }
    )

    assert (await factory.get_order(order1["id"]))["limit"] == Decimal("300")
    assert (await factory.get_order(order2["id"]))["limit"] == Decimal("500")


async def test_updates_multiple_orders_tids(factory, orders_dm):
    order1 = await factory.create_order(
        limit=Decimal("100"), consumed=Decimal("50"), tid=100
    )
    order2 = await factory.create_order(
        limit=Decimal("200"), consumed=Decimal("150"), tid=100
    )

    await orders_dm.update_orders_limits(
        {
            order1["id"]: {"new_limit": Decimal("300"), "tid": 123},
            order2["id"]: {"new_limit": Decimal("500"), "tid": 456},
        }
    )

    assert (await factory.get_order(order1["id"]))["tid"] == 123
    assert (await factory.get_order(order2["id"]))["tid"] == 456


async def test_creates_order_logs_for_multiple_orders(factory, orders_dm):
    order1 = await factory.create_order(
        limit=Decimal("100"), consumed=Decimal("50"), tid=100
    )
    order2 = await factory.create_order(
        limit=Decimal("200"), consumed=Decimal("150"), tid=100
    )

    await orders_dm.update_orders_limits(
        {
            order1["id"]: {"new_limit": Decimal("300"), "tid": 123},
            order2["id"]: {"new_limit": Decimal("500"), "tid": 456},
        }
    )

    assert await factory.get_orders_logs(order1["id"]) == [
        {
            "id": Any(int),
            "order_id": order1["id"],
            "created_at": Any(datetime),
            "op_type": OrderOperationType.CREDIT.name,
            "amount": Decimal("200"),
            "consumed": Decimal("50"),
            "limit": Decimal("300"),
            "billed_due_to": None,
        }
    ]

    assert await factory.get_orders_logs(order2["id"]) == [
        {
            "id": Any(int),
            "order_id": order2["id"],
            "created_at": Any(datetime),
            "op_type": OrderOperationType.CREDIT.name,
            "amount": Decimal("300"),
            "consumed": Decimal("150"),
            "limit": Decimal("500"),
            "billed_due_to": None,
        }
    ]


async def test_raises_if_some_orders_do_not_exist(factory, orders_dm):
    order = await factory.create_order(
        limit=Decimal("100"), consumed=Decimal("50"), tid=100
    )

    with pytest.raises(OrdersDoNotExist) as exc:
        await orders_dm.update_orders_limits(
            {
                order["id"]: {"new_limit": Decimal("300"), "tid": 123},
                order["id"] + 1: {"new_limit": Decimal("500"), "tid": 456},
            }
        )

    assert exc.value.order_ids == [order["id"] + 1]


async def test_not_updates_limit_if_some_orders_do_not_exist(factory, orders_dm):
    order = await factory.create_order(
        limit=Decimal("100"), consumed=Decimal("50"), tid=100
    )

    try:
        await orders_dm.update_orders_limits(
            {
                order["id"]: {"new_limit": Decimal("300")},
                order["id"] + 1: {"new_limit": Decimal("500"), "tid": 123},
            }
        )
    except OrdersDoNotExist:
        pass

    assert (await factory.get_order(order["id"]))["limit"] == Decimal("100")


async def test_not_updates_tid_if_some_orders_do_not_exist(factory, orders_dm):
    order = await factory.create_order(
        limit=Decimal("100"), consumed=Decimal("50"), tid=100
    )

    try:
        await orders_dm.update_orders_limits(
            {
                order["id"]: {"new_limit": Decimal("300")},
                order["id"] + 1: {"new_limit": Decimal("500"), "tid": 123},
            }
        )
    except OrdersDoNotExist:
        pass

    assert (await factory.get_order(order["id"]))["tid"] == 100


async def test_not_creates_order_logs_if_some_orders_do_not_exist(factory, orders_dm):
    order = await factory.create_order(
        limit=Decimal("100"), consumed=Decimal("50"), tid=100
    )

    try:
        await orders_dm.update_orders_limits(
            {
                order["id"]: {"new_limit": Decimal("300")},
                order["id"] + 1: {"new_limit": Decimal("500"), "tid": 123},
            }
        )
    except OrdersDoNotExist:
        pass

    assert await factory.get_orders_logs(order["id"]) == []


async def test_not_creates_inexistent_order(factory, orders_dm):
    order = await factory.create_order(
        limit=Decimal("100"), consumed=Decimal("50"), tid=100
    )

    try:
        await orders_dm.update_orders_limits(
            {
                order["id"]: {"new_limit": Decimal("300")},
                order["id"] + 1: {"new_limit": Decimal("500"), "tid": 123},
            }
        )
    except OrdersDoNotExist:
        pass

    assert await factory.get_order(order["id"] + 1) is None


async def test_not_updates_limit_if_update_til_lte_local_tid(factory, orders_dm):
    order = await factory.create_order(
        limit=Decimal("100"), consumed=Decimal("50"), tid=100
    )

    await orders_dm.update_orders_limits(
        {order["id"]: {"new_limit": Decimal("200.9876"), "tid": 70}}
    )

    assert (await factory.get_order(order["id"]))["limit"] == Decimal("100")


async def test_not_updates_tid_if_update_til_lte_local_tid(factory, orders_dm):
    order = await factory.create_order(
        limit=Decimal("100"), consumed=Decimal("50"), tid=100
    )

    await orders_dm.update_orders_limits(
        {order["id"]: {"new_limit": Decimal("200.9876"), "tid": 70}}
    )

    assert (await factory.get_order(order["id"]))["tid"] == 100


async def test_not_creates_order_log_if_update_til_lte_local_tid(factory, orders_dm):
    order = await factory.create_order(
        limit=Decimal("100"), consumed=Decimal("50"), tid=100
    )

    await orders_dm.update_orders_limits(
        {order["id"]: {"new_limit": Decimal("200.9876"), "tid": 70}}
    )

    assert await factory.get_orders_logs(order["id"]) == []


async def test_updates_limit_other_orders_if_one_update_til_lte_local_tid(
    factory, orders_dm
):
    order1 = await factory.create_order(
        limit=Decimal("100"), consumed=Decimal("50"), tid=100
    )
    order2 = await factory.create_order(
        limit=Decimal("200"), consumed=Decimal("150"), tid=100
    )

    await orders_dm.update_orders_limits(
        {
            order1["id"]: {"new_limit": Decimal("300"), "tid": 123},
            order2["id"]: {"new_limit": Decimal("500"), "tid": 70},
        }
    )

    assert (await factory.get_order(order1["id"]))["limit"] == Decimal("300")
    assert (await factory.get_order(order2["id"]))["limit"] == Decimal("200")


async def test_updates_tid_other_orders_if_one_update_til_lte_local_tid(
    factory, orders_dm
):
    order1 = await factory.create_order(
        limit=Decimal("100"), consumed=Decimal("50"), tid=100
    )
    order2 = await factory.create_order(
        limit=Decimal("200"), consumed=Decimal("150"), tid=100
    )

    await orders_dm.update_orders_limits(
        {
            order1["id"]: {"new_limit": Decimal("300"), "tid": 123},
            order2["id"]: {"new_limit": Decimal("500"), "tid": 70},
        }
    )

    assert (await factory.get_order(order1["id"]))["tid"] == 123
    assert (await factory.get_order(order2["id"]))["tid"] == 100


async def test_creates_order_log_for_other_orders_if_one_update_tid_lte_local_tid(
    factory, orders_dm
):
    order1 = await factory.create_order(
        limit=Decimal("100"), consumed=Decimal("50"), tid=100
    )
    order2 = await factory.create_order(
        limit=Decimal("200"), consumed=Decimal("150"), tid=100
    )

    await orders_dm.update_orders_limits(
        {
            order1["id"]: {"new_limit": Decimal("300"), "tid": 123},
            order2["id"]: {"new_limit": Decimal("500"), "tid": 70},
        }
    )

    assert await factory.get_orders_logs(order1["id"]) == [
        {
            "id": Any(int),
            "order_id": order1["id"],
            "created_at": Any(datetime),
            "op_type": OrderOperationType.CREDIT.name,
            "amount": Decimal("200"),
            "consumed": Decimal("50"),
            "limit": Decimal("300"),
            "billed_due_to": None,
        }
    ]
    assert await factory.get_orders_logs(order2["id"]) == []


async def test_not_replaces_existing_order_logs(factory, orders_dm):
    order = await factory.create_order(
        limit=Decimal("200"), consumed=Decimal("100"), tid=100
    )
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

    await orders_dm.update_orders_limits(
        {order["id"]: {"new_limit": Decimal("300"), "tid": 123}}
    )

    order_logs = await factory.get_orders_logs(order["id"])
    expected = [
        order_log1,
        order_log2,
        {
            "id": Any(int),
            "order_id": order["id"],
            "created_at": Any(datetime),
            "op_type": OrderOperationType.CREDIT.name,
            "amount": Decimal("100"),
            "consumed": Decimal("100"),
            "limit": Decimal("300"),
            "billed_due_to": None,
        },
    ]
    sort_key = itemgetter("consumed", "limit")
    assert sorted(order_logs, key=sort_key) == sorted(expected, key=sort_key)
