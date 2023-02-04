import pytest

from maps_adv.manul.proto import clients_pb2

pytestmark = [pytest.mark.asyncio, pytest.mark.real_db]

url = "/clients/"


async def test_returns_nothing_if_nothing_exists(api):
    got = await api.get(url, decode_as=clients_pb2.ClientsList, expected_status=200)

    assert got == clients_pb2.ClientsList(clients=[])


async def test_returns_all_clients(factory, api):
    client0 = (await factory.create_client(name="client0"))["id"]
    client1 = (await factory.create_client(name="client1"))["id"]

    got = await api.get(url, decode_as=clients_pb2.ClientsList, expected_status=200)

    assert got == clients_pb2.ClientsList(
        clients=[
            clients_pb2.ClientOutput(
                id=client1, name="client1", orders_count=0, account_manager_id=None
            ),
            clients_pb2.ClientOutput(
                id=client0, name="client0", orders_count=0, account_manager_id=None
            ),
        ]
    )


async def test_result_inclides_client_orders_count(factory, api):
    client0 = (await factory.create_client(name="client0"))["id"]
    for i in range(2):
        await factory.create_order(f"order{i}", client0)
    client1 = (await factory.create_client(name="client1"))["id"]

    got = await api.get(url, decode_as=clients_pb2.ClientsList, expected_status=200)

    assert got == clients_pb2.ClientsList(
        clients=[
            clients_pb2.ClientOutput(
                id=client1, name="client1", orders_count=0, account_manager_id=None
            ),
            clients_pb2.ClientOutput(
                id=client0, name="client0", orders_count=2, account_manager_id=None
            ),
        ]
    )
