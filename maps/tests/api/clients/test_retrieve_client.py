from datetime import date, datetime, timezone

import pytest

from maps_adv.billing_proxy.lib.core.balance_client import BalanceApiError
from maps_adv.billing_proxy.lib.db.enums import CurrencyType, PaymentType
from maps_adv.billing_proxy.proto import clients_pb2, common_pb2
from maps_adv.billing_proxy.tests.helpers import urljoin

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("db")]

API_URL = "/clients/"
NOW_IN_UTC = datetime.now(tz=timezone.utc)


@pytest.fixture(autouse=True)
def common_balance_client_mocks(balance_client):
    balance_client.find_client.coro.return_value = {
        "id": 600,
        "name": "Имя клиента",
        "email": "email@example.com",
        "phone": "8(499)123-45-67",
        "is_agency": False,
    }

    balance_client.list_client_contracts.coro.return_value = [
        {
            "id": 11,
            "external_id": "999/11",
            "currency": CurrencyType.RUB,
            "is_active": False,
            "date_start": date(2012, 2, 2),
            "date_end": date(2012, 3, 3),
            "payment_type": PaymentType.POST,
        },
        {
            "id": 22,
            "external_id": "999/22",
            "currency": CurrencyType.RUB,
            "is_active": True,
            "date_start": date(2012, 4, 4),
            "date_end": None,
            "payment_type": PaymentType.PRE,
        },
    ]
    balance_client.list_client_passports.coro.return_value = []


async def test_returns_client_if_found_locally(api, client):
    result = await api.get(
        urljoin(API_URL, client["id"]),
        decode_as=clients_pb2.Client,
        allowed_status_codes=[200],
    )

    assert result == clients_pb2.Client(
        id=client["id"],
        name=client["name"],
        email=client["email"],
        phone=client["phone"],
        account_manager_id=client["account_manager_id"],
        has_accepted_offer=client["has_accepted_offer"],
    )


async def test_returns_balance_client_data_if_not_found_locally(api):
    result = await api.get(
        urljoin(API_URL, 600), decode_as=clients_pb2.Client, allowed_status_codes=[200]
    )

    assert result == clients_pb2.Client(
        id=600,
        name="Имя клиента",
        email="email@example.com",
        phone="8(499)123-45-67",
        has_accepted_offer=False,
    )


async def test_saves_balance_client_data_if_not_found_locally(api, factory):
    await api.get(
        urljoin(API_URL, 600), decode_as=clients_pb2.Client, allowed_status_codes=[200]
    )

    await factory.update_created_at(600, NOW_IN_UTC)
    db_client_data = await factory.get_client(600)
    assert db_client_data == {
        "id": 600,
        "name": "Имя клиента",
        "email": "email@example.com",
        "phone": "8(499)123-45-67",
        "is_agency": False,
        "created_from_cabinet": False,
        "account_manager_id": None,
        "domain": "",
        "partner_agency_id": None,
        "has_accepted_offer": False,
        "created_at": NOW_IN_UTC,
        "representatives": [],
    }


async def test_retrieves_client_contracts_from_balance_if_not_found_locally(
    api, balance_client
):
    await api.get(
        urljoin(API_URL, 600), decode_as=clients_pb2.Client, allowed_status_codes=[200]
    )

    balance_client.list_client_contracts.assert_called_with(600)


async def test_saves_client_contracts_from_balance_if_not_found_locally(
    api, factory, balance_client
):
    await api.get(
        urljoin(API_URL, 600), decode_as=clients_pb2.Client, allowed_status_codes=[200]
    )

    contracts_data = await factory.get_client_contracts(600)
    assert contracts_data == [
        {
            "id": 11,
            "client_id": 600,
            "external_id": "999/11",
            "currency": CurrencyType.RUB.name,
            "is_active": False,
            "date_start": date(2012, 2, 2),
            "date_end": date(2012, 3, 3),
            "payment_type": PaymentType.POST.name,
            "preferred": False,
        },
        {
            "id": 22,
            "client_id": 600,
            "external_id": "999/22",
            "currency": CurrencyType.RUB.name,
            "is_active": True,
            "date_start": date(2012, 4, 4),
            "date_end": None,
            "payment_type": PaymentType.PRE.name,
            "preferred": False,
        },
    ]


async def test_not_saves_client_contracts_from_balance_if_client_fails_on_contracts(
    api, factory, balance_client
):
    balance_client.list_client_contracts.coro.side_effect = BalanceApiError()

    await api.get(
        urljoin(API_URL, 600),
        allowed_status_codes=[503],
        expected_error=(common_pb2.Error.BALANCE_API_ERROR, "Balance API error"),
    )

    assert await factory.get_all_clients() == []


async def test_not_returns_locally_existing_agency(api, factory):
    await factory.create_client(id=600, is_agency=True)

    await api.get(urljoin(API_URL, 600), allowed_status_codes=[404])


async def test_returns_error_for_inexistent_client(api, factory, balance_client):
    inexistent_id = await factory.get_inexistent_client_id()

    balance_client.find_client.coro.return_value = None

    await api.get(urljoin(API_URL, inexistent_id), allowed_status_codes=[404])
