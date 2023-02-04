import pytest

pytestmark = [pytest.mark.asyncio, pytest.mark.real_db]


async def test_returns_all_clients(factory, clients_dm):
    client0 = (await factory.create_client(name="client0", account_manager_id=100500))[
        "id"
    ]
    client1 = (await factory.create_client(name="client1", account_manager_id=None))[
        "id"
    ]

    got = await clients_dm.list_clients()

    assert got == [
        dict(id=client1, name="client1", orders_count=0, account_manager_id=None),
        dict(id=client0, name="client0", orders_count=0, account_manager_id=100500),
    ]


async def test_result_contains_count_of_client_orders(factory, clients_dm):
    client0 = (await factory.create_client(name="client0", account_manager_id=100500))[
        "id"
    ]
    for i in range(2):
        await factory.create_order(f"order{i}", client0)
    client1 = (await factory.create_client(name="client1", account_manager_id=None))[
        "id"
    ]

    got = await clients_dm.list_clients()

    assert got == [
        dict(id=client1, name="client1", orders_count=0, account_manager_id=None),
        dict(id=client0, name="client0", orders_count=2, account_manager_id=100500),
    ]
