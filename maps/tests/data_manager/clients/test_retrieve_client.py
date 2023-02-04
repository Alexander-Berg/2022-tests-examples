import pytest

from maps_adv.manul.lib.data_managers.exceptions import ClientNotFound

pytestmark = [pytest.mark.asyncio]


async def test_returns_client_details(factory, clients_dm):
    client_id = (
        await factory.create_client(name="client0", account_manager_id=100500)
    )["id"]

    got = await clients_dm.retrieve_client(client_id=client_id)

    assert got == dict(
        id=client_id, name="client0", orders_count=0, account_manager_id=100500
    )


async def test_result_contains_count_of_client_orders(factory, clients_dm):
    client_id = (
        await factory.create_client(name="client0", account_manager_id=100500)
    )["id"]
    for i in range(2):
        await factory.create_order(f"order{i}", client_id)

    got = await clients_dm.retrieve_client(client_id=client_id)

    assert got == dict(
        id=client_id, name="client0", orders_count=2, account_manager_id=100500
    )


async def test_result_does_not_contain_orders_of_another_client(factory, clients_dm):
    client0 = (await factory.create_client(name="client0", account_manager_id=100500))[
        "id"
    ]
    for i in range(2):
        await factory.create_order(f"order{i}", client0)
    client1 = (await factory.create_client(name="client1", account_manager_id=200400))[
        "id"
    ]

    got = await clients_dm.retrieve_client(client_id=client1)

    assert got == dict(
        id=client1, name="client1", orders_count=0, account_manager_id=200400
    )


async def test_raises_if_client_does_not_exist(clients_dm):
    with pytest.raises(ClientNotFound, match="1"):
        await clients_dm.retrieve_client(client_id=1)
