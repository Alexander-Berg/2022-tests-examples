import pytest

from maps_adv.manul.proto import clients_pb2, errors_pb2

pytestmark = [pytest.mark.asyncio]

API_URL = "/clients/set-account-manager/"


async def test_client_updated_account_manager_id(api, factory):
    client_id = (
        await factory.create_client(name="client_name", account_manager_id=100500)
    )["id"]

    input_data = clients_pb2.ClientSetAccountManagerInput(
        client_id=client_id, account_manager_id=200600
    )

    await api.post(
        API_URL,
        proto=input_data,
        decode_as=clients_pb2.ClientOutput,
        expected_status=200,
    )

    client_details = await factory.retrieve_client(client_id)
    assert client_details == dict(
        id=client_id, name="client_name", orders_count=0, account_manager_id=200600
    )


async def test_returns_error_if_client_does_not_exists(api):

    input_data = clients_pb2.ClientSetAccountManagerInput(
        client_id=100500, account_manager_id=200600
    )

    got = await api.post(
        API_URL, proto=input_data, decode_as=errors_pb2.Error, expected_status=404
    )

    assert got == errors_pb2.Error(code=errors_pb2.Error.CLIENT_NOT_FOUND)
