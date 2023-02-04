import pytest

pytestmark = [pytest.mark.asyncio]


async def test_returns_nothing_if_no_orders(factory, orders_dm):

    assert await orders_dm.retrieve_order_ids_for_account(200600) == []


async def test_returns_found_data(factory, orders_dm):
    client_1_id = (await factory.create_client(account_manager_id=100500))["id"]
    order_1_id = (await factory.create_order(client_id=client_1_id))["id"]
    order_2_id = (await factory.create_order(client_id=client_1_id))["id"]
    client_2_id = (await factory.create_client(account_manager_id=100500))["id"]
    order_3_id = (await factory.create_order(client_id=client_2_id))["id"]
    order_4_id = (await factory.create_order(client_id=client_2_id))["id"]

    result = await orders_dm.retrieve_order_ids_for_account(100500)

    assert sorted(result) == [order_1_id, order_2_id, order_3_id, order_4_id]


async def test_not_returns_hidden_orders(factory, orders_dm):
    client_id = (await factory.create_client(account_manager_id=100500))["id"]
    order_1_id = (await factory.create_order(client_id=client_id))["id"]
    (await factory.create_order(client_id=client_id, hidden=True))["id"]

    result = await orders_dm.retrieve_order_ids_for_account(100500)

    assert result == [order_1_id]


async def test_not_returns_other_account_orders(factory, orders_dm):
    client_1_id = (await factory.create_client(account_manager_id=100500))["id"]
    order_1_id = (await factory.create_order(client_id=client_1_id))["id"]
    order_2_id = (await factory.create_order(client_id=client_1_id))["id"]
    client_2_id = (await factory.create_client(account_manager_id=200600))["id"]
    await factory.create_order(client_id=client_2_id)

    result = await orders_dm.retrieve_order_ids_for_account(100500)

    assert sorted(result) == [order_1_id, order_2_id]
