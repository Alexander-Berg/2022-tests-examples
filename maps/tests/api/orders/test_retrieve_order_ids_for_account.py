import pytest

from maps_adv.billing_proxy.proto import orders_pb2

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("db")]

API_URL = "/orders/by-account-manager/"


async def test_returns_nothing_if_no_orders(api, factory):
    input_pb = orders_pb2.AccountManagerId(account_manager_id=100500)

    result = await api.post(
        API_URL, input_pb, decode_as=orders_pb2.OrderIds, allowed_status_codes=[200]
    )

    assert result == orders_pb2.OrderIds(order_ids=[])


async def test_returns_all_orders_for_one_client(api, factory):
    client_id = (await factory.create_client(account_manager_id=100500))["id"]
    order_1_id = (await factory.create_order(client_id=client_id))["id"]
    order_2_id = (await factory.create_order(client_id=client_id))["id"]

    input_pb = orders_pb2.AccountManagerId(account_manager_id=100500)

    result = await api.post(
        API_URL, input_pb, decode_as=orders_pb2.OrderIds, allowed_status_codes=[200]
    )

    assert result == orders_pb2.OrderIds(order_ids=[order_1_id, order_2_id])


async def test_returns_all_orders_for_several_clients(api, factory):
    client_1_id = (await factory.create_client(account_manager_id=100500))["id"]
    order_1_id = (await factory.create_order(client_id=client_1_id))["id"]
    order_2_id = (await factory.create_order(client_id=client_1_id))["id"]
    client_2_id = (await factory.create_client(account_manager_id=100500))["id"]
    order_3_id = (await factory.create_order(client_id=client_2_id))["id"]
    order_4_id = (await factory.create_order(client_id=client_2_id))["id"]

    input_pb = orders_pb2.AccountManagerId(account_manager_id=100500)

    result = await api.post(
        API_URL, input_pb, decode_as=orders_pb2.OrderIds, allowed_status_codes=[200]
    )

    assert sorted(result.order_ids) == [order_1_id, order_2_id, order_3_id, order_4_id]


async def test_does_not_return_orders_for_other_accounts(api, factory):
    client_1_id = (await factory.create_client(account_manager_id=100500))["id"]
    order_1_id = (await factory.create_order(client_id=client_1_id))["id"]
    order_2_id = (await factory.create_order(client_id=client_1_id))["id"]
    client_2_id = (await factory.create_client(account_manager_id=200600))["id"]
    await factory.create_order(client_id=client_2_id)
    await factory.create_order(client_id=client_2_id)

    input_pb = orders_pb2.AccountManagerId(account_manager_id=100500)

    result = await api.post(
        API_URL, input_pb, decode_as=orders_pb2.OrderIds, allowed_status_codes=[200]
    )

    assert sorted(result.order_ids) == [order_1_id, order_2_id]
