import pytest

from maps_adv.billing_proxy.proto import clients_pb2, common_pb2

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("db")]

API_URL = "/clients/set-account-manager/"


async def test_update_client_data_account_manager(api, factory):
    client = await factory.create_client(
        name="Имя клиента", email="email@example.com", phone="8(499)123-45-67"
    )

    input_pb = clients_pb2.ClientSetAccountManagerInput(
        client_id=client["id"], account_manager_id=100500
    )

    await api.post(
        API_URL, input_pb, decode_as=clients_pb2.Client, allowed_status_codes=[200]
    )

    result = await factory.get_client(client["id"])

    assert result == {
        "id": client["id"],
        "name": "Имя клиента",
        "email": "email@example.com",
        "phone": "8(499)123-45-67",
        "is_agency": False,
        "account_manager_id": 100500,
        "domain": "someTestDomain",
        "partner_agency_id": None,
    }


async def test_returns_error_if_client_does_not_exists(api, factory):

    inexistent_id = await factory.get_inexistent_client_id()

    input_data = clients_pb2.ClientSetAccountManagerInput(
        client_id=inexistent_id, account_manager_id=100500
    )

    await api.post(
        API_URL,
        input_data,
        expected_error=(
            common_pb2.Error.CLIENT_DOES_NOT_EXIST,
            f"client_id={inexistent_id}",
        ),
        allowed_status_codes=[422],
    )
