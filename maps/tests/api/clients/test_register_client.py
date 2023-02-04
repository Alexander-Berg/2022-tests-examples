import datetime
import json

import pytest

from maps_adv.billing_proxy.lib.core.balance_client import BalanceApiError
from maps_adv.billing_proxy.proto import clients_pb2, common_pb2
from maps_adv.billing_proxy.tests.helpers import (
    mock_find_by_uid_clients,
    get_user_ticket,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("db")]

API_URL = "/clients/register/"
NOW_IN_UTC = datetime.datetime.now(tz=datetime.timezone.utc)


@pytest.fixture(autouse=True)
def common_balance_client_mocks(balance_client):
    balance_client.create_client.coro.return_value = 55
    balance_client.find_client.coro.side_effect = mock_find_by_uid_clients
    balance_client.list_client_contracts.coro.return_value = []
    balance_client.list_client_passports.coro.return_value = []


async def test_returns_created_client_data(api):
    input_pb = clients_pb2.ClientCreationInput(
        name="Имя клиента",
        email="email@example.com",
        phone="8(499)123-45-67",
        domain="",
    )
    result = await api.post(
        API_URL, input_pb, decode_as=clients_pb2.Client, allowed_status_codes=[201]
    )

    assert result == clients_pb2.Client(
        id=55,
        name="Имя клиента",
        email="email@example.com",
        phone="8(499)123-45-67",
        has_accepted_offer=False,
    )


async def test_returns_created_client_data_with_domain(api):
    input_pb = clients_pb2.ClientCreationInput(
        name="Имя клиента",
        email="email@example.com",
        phone="8(499)123-45-67",
        domain="some domain",
        has_accepted_offer=True,
    )
    result = await api.post(
        API_URL, input_pb, decode_as=clients_pb2.Client, allowed_status_codes=[201]
    )

    assert result == clients_pb2.Client(
        id=55,
        name="Имя клиента",
        email="email@example.com",
        phone="8(499)123-45-67",
        has_accepted_offer=True,
    )


async def test_creates_client_locally(api, factory):
    input_pb = clients_pb2.ClientCreationInput(
        name="Имя клиента",
        email="email@example.com",
        phone="8(499)123-45-67",
        domain="",
        has_accepted_offer=False,
    )
    await api.post(
        API_URL, input_pb, decode_as=clients_pb2.Client, allowed_status_codes=[201]
    )

    await factory.update_created_at(55, NOW_IN_UTC)
    clients_data = await factory.get_all_clients()
    assert clients_data == [
        {
            "id": 55,
            "name": "Имя клиента",
            "email": "email@example.com",
            "phone": "8(499)123-45-67",
            "is_agency": False,
            "created_from_cabinet": True,
            "account_manager_id": None,
            "domain": "",
            "partner_agency_id": None,
            "has_accepted_offer": False,
            "created_at": NOW_IN_UTC,
            "representatives": [],
        }
    ]


async def test_returns_created_client_data_with_account_manager_id(api):
    input_pb = clients_pb2.ClientCreationInput(
        name="Имя клиента",
        email="email@example.com",
        phone="8(499)123-45-67",
        account_manager_id=100500,
        domain="",
    )
    result = await api.post(
        API_URL, input_pb, decode_as=clients_pb2.Client, allowed_status_codes=[201]
    )

    assert result == clients_pb2.Client(
        id=55,
        name="Имя клиента",
        email="email@example.com",
        phone="8(499)123-45-67",
        account_manager_id=100500,
        has_accepted_offer=False,
    )


async def test_creates_client_in_balance(api, balance_client):
    input_pb = clients_pb2.ClientCreationInput(
        name="Имя клиента",
        email="email@example.com",
        phone="8(499)123-45-67",
        domain="someDomain",
    )
    await api.post(
        API_URL, input_pb, decode_as=clients_pb2.Client, allowed_status_codes=[201]
    )

    balance_client.create_client.assert_called_with(
        "Имя клиента", "email@example.com", "8(499)123-45-67"
    )


async def test_returns_error_if_balance_fails(api, balance_client):
    balance_client.create_client.coro.side_effect = BalanceApiError()

    input_pb = clients_pb2.ClientCreationInput(
        name="Имя клиента",
        email="email@example.com",
        phone="8(499)123-45-67",
        domain="",
    )
    await api.post(
        API_URL,
        input_pb,
        allowed_status_codes=[503],
        expected_error=(common_pb2.Error.BALANCE_API_ERROR, "Balance API error"),
    )


async def test_not_creates_locally_if_balance_fails(api, factory, balance_client):
    balance_client.create_client.coro.side_effect = BalanceApiError()

    input_pb = clients_pb2.ClientCreationInput(
        name="Имя клиента",
        email="email@example.com",
        phone="8(499)123-45-67",
        domain="",
    )
    await api.post(API_URL, input_pb)

    assert await factory.get_all_clients() == []


@pytest.mark.parametrize(
    ("field", "max_length"),
    [("name", 256), ("email", 256), ("phone", 64), ("domain", 256)],
)
async def test_returns_error_for_long_values(api, field, max_length):
    input_pb = clients_pb2.ClientCreationInput(
        name="Имя клиента",
        email="email@example.com",
        phone="8(499)123-45-67",
        domain="some company",
    )
    setattr(input_pb, field, "N" * (max_length + 1))

    await api.post(
        API_URL,
        input_pb,
        expected_error=(
            common_pb2.Error.DATA_VALIDATION_ERROR,
            json.dumps({field: [f"Longer than maximum length {max_length}."]}),
        ),
        allowed_status_codes=[400],
    )


async def test_adds_user_to_client(client, balance_client, api, factory):
    balance_client.list_client_passports.coro.return_value = [123]
    input_pb = clients_pb2.ClientCreationInput(
        name="Имя клиента",
        email="email@example.com",
        phone="8(499)123-45-67",
        domain="",
    )
    result = await api.post(
        API_URL,
        input_pb,
        decode_as=clients_pb2.Client,
        allowed_status_codes=[201],
        headers={"X-Ya-User-Ticket": get_user_ticket(uid=123)},
    )

    assert result == clients_pb2.Client(
        id=55,
        name="Имя клиента",
        email="email@example.com",
        phone="8(499)123-45-67",
        has_accepted_offer=False,
    )

    balance_client.find_client.assert_called_with(uid=123)
    balance_client.create_user_client_association.assert_called_with(result.id, 123)

    assert (await factory.get_client(result.id))["representatives"] == [123]


async def test_returns_existing_client(client, balance_client, api, factory):
    balance_client.list_client_passports.coro.return_value = [10001, 10101]
    input_pb = clients_pb2.ClientCreationInput(
        name="Другое имя клиента",
        email="other_email@example.com",
        phone="8(499)765-43-21",
        domain="",
    )
    result = await api.post(
        API_URL,
        input_pb,
        decode_as=clients_pb2.Client,
        allowed_status_codes=[201],
        headers={"X-Ya-User-Ticket": get_user_ticket(uid=10001)},
    )

    assert result == clients_pb2.Client(
        id=55,
        name="Имя клиента",
        email="email@example.com",
        phone="8(499)123-45-67",
        partner_agency_id=1,
        has_accepted_offer=False,
    )

    balance_client.create_user_client_association.assert_not_called()
    balance_client.find_client.assert_called_with(uid=10001)
    balance_client.create_client.assert_called_with(
        "Имя клиента",
        "email@example.com",
        "8(499)123-45-67",
        55,
    )
    assert (await factory.get_client(55))["representatives"] == [10001, 10101]


async def test_fails_on_balance_error(client, api, balance_client):
    balance_client.find_client.coro.side_effect = BalanceApiError()
    input_pb = clients_pb2.ClientCreationInput(
        name="Имя клиента",
        email="email@example.com",
        phone="8(499)123-45-67",
        domain="",
    )
    await api.post(
        API_URL,
        input_pb,
        decode_as=clients_pb2.Client,
        expected_error=(
            common_pb2.Error.BALANCE_API_ERROR,
            "Balance API error",
        ),
        allowed_status_codes=[503],
        headers={"X-Ya-User-Ticket": get_user_ticket(uid=123)},
    )

    balance_client.find_client.assert_called_with(uid=123)


async def test_fails_on_client_not_found(client, api, balance_client):
    input_pb = clients_pb2.ClientCreationInput(
        name="Имя клиента",
        email="email@example.com",
        phone="8(499)123-45-67",
        domain="",
    )
    await api.post(
        API_URL,
        input_pb,
        decode_as=clients_pb2.Client,
        expected_error=(
            common_pb2.Error.CLIENT_BY_UID_DOES_NOT_EXIST,
            "uid=10003, is_agency=True",
        ),
        allowed_status_codes=[422],
        headers={"X-Ya-User-Ticket": get_user_ticket(uid=10003)},
    )

    balance_client.find_client.assert_called_with(uid=10003)
