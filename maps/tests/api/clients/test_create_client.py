import json

import pytest

from maps_adv.manul.proto import clients_pb2, errors_pb2

pytestmark = [pytest.mark.asyncio]

url = "/clients/"


async def test_returns_client_details(api):
    input_pb = clients_pb2.ClientInput(name="client_name", account_manager_id=100500)

    got = await api.post(
        url, proto=input_pb, decode_as=clients_pb2.ClientOutput, expected_status=201
    )

    assert got == clients_pb2.ClientOutput(
        id=got.id, name="client_name", account_manager_id=100500
    )


@pytest.mark.parametrize("manager_id", (100500, None))
async def test_client_created(api, factory, manager_id):
    input_pb = clients_pb2.ClientInput(name="client_name")
    if manager_id is not None:
        input_pb.account_manager_id = manager_id

    got = await api.post(
        url, proto=input_pb, decode_as=clients_pb2.ClientOutput, expected_status=201
    )

    client_details = await factory.retrieve_client(got.id)
    assert client_details == dict(
        id=got.id, name="client_name", orders_count=0, account_manager_id=manager_id
    )


@pytest.mark.parametrize("name", ("", "N" * 257))
async def test_returns_error_for_wrong_length_name(api, name):
    input_pb = clients_pb2.ClientInput(name=name, account_manager_id=100500)

    got = await api.post(
        url, proto=input_pb, decode_as=errors_pb2.Error, expected_status=400
    )

    assert got == errors_pb2.Error(
        code=errors_pb2.Error.DATA_VALIDATION_ERROR,
        description=json.dumps({"name": ["Length must be between 1 and 256."]}),
    )


async def test_returns_error_if_client_already_exists(api):
    input_pb = clients_pb2.ClientInput(name="client_name", account_manager_id=100500)
    await api.post(url, proto=input_pb)

    got = await api.post(
        url, proto=input_pb, decode_as=errors_pb2.Error, expected_status=400
    )

    assert got == errors_pb2.Error(code=errors_pb2.Error.CLIENT_EXISTS)
