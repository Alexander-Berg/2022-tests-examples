import pytest

from maps_adv.manul.proto import orders_pb2

pytestmark = [pytest.mark.asyncio, pytest.mark.real_db]

url = "/orders/by-account-manager/"


async def test_returns_nothing_if_nothing_exists(api):
    input_pb = orders_pb2.AccountManagerIdFilter(account_manager_id=100500)

    got = await api.post(
        url, proto=input_pb, decode_as=orders_pb2.OrdersFilter, expected_status=200
    )

    assert got == orders_pb2.OrdersFilter(ids=[])


async def test_returns_list_of_existing_orders_for_account(api, factory):
    client1_id = (await factory.create_client("client1", 100500))["id"]
    order1_id = (await factory.create_order(f"order1", client1_id))["id"]
    order2_id = (await factory.create_order(f"order2", client1_id))["id"]
    client2_id = (await factory.create_client("client2", 100500))["id"]
    order3_id = (await factory.create_order(f"order3", client2_id))["id"]

    input_pb = orders_pb2.AccountManagerIdFilter(account_manager_id=100500)

    got = await api.post(
        url, proto=input_pb, decode_as=orders_pb2.OrdersFilter, expected_status=200
    )

    assert got == orders_pb2.OrdersFilter(ids=[order1_id, order2_id, order3_id])


async def test_does_not_return_orders_for_other_account(api, factory):
    client1_id = (await factory.create_client("client1", 100500))["id"]
    order1_id = (await factory.create_order(f"order1", client1_id))["id"]
    order2_id = (await factory.create_order(f"order2", client1_id))["id"]
    client2_id = (await factory.create_client("client2", 200600))["id"]
    await factory.create_order(f"order3", client2_id)

    input_pb = orders_pb2.AccountManagerIdFilter(account_manager_id=100500)

    got = await api.post(
        url, proto=input_pb, decode_as=orders_pb2.OrdersFilter, expected_status=200
    )

    assert got == orders_pb2.OrdersFilter(ids=[order1_id, order2_id])
