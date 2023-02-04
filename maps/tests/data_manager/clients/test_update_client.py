import pytest

pytestmark = [pytest.mark.asyncio]


async def test_updates_client(clients_dm, factory, con):
    client_id = (await factory.create_client("client_name", account_manager_id=100500))[
        "id"
    ]

    await clients_dm.update_client(client_id, "new_client_name")

    sql = """
        SELECT EXISTS(
            SELECT id
            FROM clients
            WHERE id = $1 AND name = $2
        )
    """
    assert await con.fetchval(sql, client_id, "new_client_name") is True


async def test_returns_details_for_client_without_orders(clients_dm, factory):
    client_id = (await factory.create_client("client_name", account_manager_id=100500))[
        "id"
    ]

    got = await clients_dm.update_client(client_id, "new_client_name")

    assert got == dict(id=client_id, name="new_client_name", orders_count=0)


async def test_returns_details_for_client_with_orders(clients_dm, factory):
    client_id = (await factory.create_client("client_name", account_manager_id=100500))[
        "id"
    ]
    for i in range(2):
        await factory.create_order(f"order{i}", client_id)

    got = await clients_dm.update_client(client_id, "new_client_name")

    assert got == dict(id=client_id, name="new_client_name", orders_count=2)


async def test_ignores_other_clients_orders(clients_dm, factory):
    client0 = (await factory.create_client(name="client0", account_manager_id=100500))[
        "id"
    ]
    for i in range(2):
        await factory.create_order(f"order{i}", client0)
    client1 = (await factory.create_client(name="client1", account_manager_id=200600))[
        "id"
    ]

    got = await clients_dm.update_client(client1, "new_client_name")

    assert got == dict(id=client1, name="new_client_name", orders_count=0)
