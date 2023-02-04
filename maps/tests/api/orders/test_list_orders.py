import pytest

from maps_adv.manul.lib.db.enums import CurrencyType, RateType
from maps_adv.manul.proto import orders_pb2
from maps_adv.manul.tests import dt_to_proto

pytestmark = [pytest.mark.asyncio, pytest.mark.real_db]

url = "/orders/list/"


async def test_returns_nothing_if_nothing_exists(api):
    got = await api.post(url, decode_as=orders_pb2.OrdersList, expected_status=200)

    assert got == orders_pb2.OrdersList(orders=[])


async def test_returns_list_of_all_orders_if_no_order_ids_passed(api, factory):
    client_id = (await factory.create_client(name="client", account_manager_id=100500))[
        "id"
    ]
    order1 = await factory.create_order("order1", client_id)
    order2 = await factory.create_order(
        "order2", client_id, product_id=2, currency=CurrencyType.BYN
    )
    order3 = await factory.create_order(
        "order3", client_id, product_id=3, comment="comment", rate=RateType.FREE
    )

    got = await api.post(url, decode_as=orders_pb2.OrdersList, expected_status=200)

    assert got == orders_pb2.OrdersList(
        orders=[
            orders_pb2.OrderOutput(
                id=order3["id"],
                title="order3",
                client_id=client_id,
                product_id=3,
                currency=orders_pb2.CurrencyType.Value("RUB"),
                comment="comment",
                created_at=dt_to_proto(order3["created_at"]),
                rate=orders_pb2.RateType.Value("FREE"),
            ),
            orders_pb2.OrderOutput(
                id=order2["id"],
                title="order2",
                client_id=client_id,
                product_id=2,
                currency=orders_pb2.CurrencyType.Value("BYN"),
                comment="",
                created_at=dt_to_proto(order2["created_at"]),
                rate=orders_pb2.RateType.Value("PAID"),
            ),
            orders_pb2.OrderOutput(
                id=order1["id"],
                title="order1",
                client_id=client_id,
                product_id=1,
                currency=orders_pb2.CurrencyType.Value("RUB"),
                comment="",
                created_at=dt_to_proto(order1["created_at"]),
                rate=orders_pb2.RateType.Value("PAID"),
            ),
        ]
    )


async def test_returns_list_of_passed_existing_orders(api, factory):
    client_id = (await factory.create_client(name="client", account_manager_id=100500))[
        "id"
    ]
    order1 = await factory.create_order("order1", client_id)
    await factory.create_order("order2", client_id)
    await factory.create_order("order3", client_id)
    input_pb = orders_pb2.OrdersFilter(ids=[order1["id"], 9999])

    got = await api.post(
        url, proto=input_pb, decode_as=orders_pb2.OrdersList, expected_status=200
    )

    assert got == orders_pb2.OrdersList(
        orders=[
            orders_pb2.OrderOutput(
                id=order1["id"],
                title="order1",
                client_id=client_id,
                product_id=1,
                currency=orders_pb2.CurrencyType.Value("RUB"),
                comment="",
                created_at=dt_to_proto(order1["created_at"]),
                rate=orders_pb2.RateType.Value("PAID"),
            )
        ]
    )
