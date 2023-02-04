import pytest

from maps_adv.manul.proto import clients_pb2, errors_pb2

pytestmark = [pytest.mark.asyncio]


def url(client_id: int) -> str:
    return f"/clients/{client_id}/"


async def test_returns_client_details(factory, api):
    client_id = (
        await factory.create_client(name="client0", account_manager_id=100500)
    )["id"]

    got = await api.get(
        url(client_id), decode_as=clients_pb2.ClientOutput, expected_status=200
    )

    assert got == clients_pb2.ClientOutput(
        id=client_id, name="client0", orders_count=0, account_manager_id=100500
    )


async def test_result_contains_count_of_client_orders(factory, api):
    client_id = (await factory.create_client(name="client0"))["id"]
    for i in range(2):
        await factory.create_order(f"order{i}", client_id)

    got = await api.get(
        url(client_id), decode_as=clients_pb2.ClientOutput, expected_status=200
    )

    assert got == clients_pb2.ClientOutput(
        id=client_id, name="client0", orders_count=2, account_manager_id=None
    )


async def test_result_does_not_contain_orders_of_another_client(factory, api):
    client0 = (await factory.create_client(name="client0"))["id"]
    for i in range(2):
        await factory.create_order(f"order{i}", client0)
    client1 = (await factory.create_client(name="client1"))["id"]

    got = await api.get(
        url(client1), decode_as=clients_pb2.ClientOutput, expected_status=200
    )

    assert got == clients_pb2.ClientOutput(
        id=client1, name="client1", orders_count=0, account_manager_id=None
    )


async def test_errored_if_client_does_not_exist(api):
    got = await api.get(url(100500), decode_as=errors_pb2.Error, expected_status=404)

    assert got == errors_pb2.Error(code=errors_pb2.Error.CLIENT_NOT_FOUND)
