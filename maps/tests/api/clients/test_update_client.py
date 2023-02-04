import json

import pytest

from maps_adv.manul.proto import clients_pb2, errors_pb2

pytestmark = [pytest.mark.asyncio]


def url(client_id: int) -> str:
    return f"/clients/{client_id}/"


async def test_returns_details_for_client_without_orders(factory, api):
    client_id = (
        await factory.create_client(name="client_name", account_manager_id=100500)
    )["id"]
    input_data = clients_pb2.ClientInput(name="new_client_name")

    got = await api.put(
        url(client_id),
        proto=input_data,
        decode_as=clients_pb2.ClientOutput,
        expected_status=200,
    )

    assert got == clients_pb2.ClientOutput(
        id=client_id, name="new_client_name", orders_count=0
    )


async def test_returns_details_for_client_with_orders(factory, api):
    client_id = (
        await factory.create_client(name="client_name", account_manager_id=100500)
    )["id"]
    for i in range(2):
        await factory.create_order(f"order{i}", client_id)
    input_data = clients_pb2.ClientInput(name="new_client_name")

    got = await api.put(
        url(client_id),
        proto=input_data,
        decode_as=clients_pb2.ClientOutput,
        expected_status=200,
    )

    assert got == clients_pb2.ClientOutput(
        id=client_id, name="new_client_name", orders_count=2
    )


async def test_skips_other_clients_orders(factory, api):
    client0 = (await factory.create_client(name="client0", account_manager_id=100500))[
        "id"
    ]
    for i in range(2):
        await factory.create_order(f"order{i}", client0)
    client1 = (await factory.create_client(name="client1", account_manager_id=100500))[
        "id"
    ]
    input_data = clients_pb2.ClientInput(name="new_client_name")

    got = await api.put(
        url(client1),
        proto=input_data,
        decode_as=clients_pb2.ClientOutput,
        expected_status=200,
    )

    assert got == clients_pb2.ClientOutput(
        id=client1, name="new_client_name", orders_count=0
    )


async def test_client_updated(api, factory):
    client_id = (
        await factory.create_client(name="client_name", account_manager_id=100500)
    )["id"]
    input_data = clients_pb2.ClientInput(name="new_client_name")

    await api.put(
        url(client_id),
        proto=input_data,
        decode_as=clients_pb2.ClientOutput,
        expected_status=200,
    )

    client_details = await factory.retrieve_client(client_id)
    assert client_details == dict(
        id=client_id, name="new_client_name", orders_count=0, account_manager_id=100500
    )


@pytest.mark.parametrize("name", ("", "N" * 257))
async def test_returns_error_for_wrong_length_name(factory, api, name):
    client_id = (
        await factory.create_client(name="client_name", account_manager_id=100500)
    )["id"]
    input_data = clients_pb2.ClientInput(name=name)

    got = await api.put(
        url(client_id),
        proto=input_data,
        decode_as=errors_pb2.Error,
        expected_status=400,
    )

    assert got == errors_pb2.Error(
        code=errors_pb2.Error.DATA_VALIDATION_ERROR,
        description=json.dumps({"name": ["Length must be between 1 and 256."]}),
    )


async def test_returns_error_if_client_does_not_exists(api):
    input_data = clients_pb2.ClientInput(name="client_name")

    got = await api.get(
        url(100500), proto=input_data, decode_as=errors_pb2.Error, expected_status=404
    )

    assert got == errors_pb2.Error(code=errors_pb2.Error.CLIENT_NOT_FOUND)
