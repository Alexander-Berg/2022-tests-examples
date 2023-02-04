import datetime
import pytest

from maps_adv.billing_proxy.lib.core.balance_client import BalanceApiError
from maps_adv.billing_proxy.lib.db.enums import CurrencyType, PaymentType
from maps_adv.billing_proxy.lib.domain.exceptions import ClientDoesNotExist

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.fixture(autouse=True)
def common_dm_mocks(clients_dm):
    clients_dm.find_client_locally.coro.return_value = {
        "id": 11,
        "name": "name",
        "email": "email@site.com",
        "phone": "322-223",
        "is_agency": False,
        "account_manager_id": 100500,
    }


@pytest.fixture(autouse=True)
def common_balance_client_mocks(balance_client):
    balance_client.find_client.coro.return_value = {
        "id": 600,
        "name": "Имя клиента",
        "email": "email@example.com",
        "phone": "8(499)123-45-67",
        "is_agency": False,
    }

    balance_client.list_client_contracts.coro.return_value = []
    balance_client.list_client_passports.coro.return_value = []


async def test_returns_local_client_if_found(clients_domain, clients_dm):
    result = await clients_domain.retrieve_client(client_id=11)

    clients_dm.find_client_locally.assert_called_with(11)
    clients_dm.upsert_client.assert_not_called()
    assert result == {
        "id": 11,
        "name": "name",
        "email": "email@site.com",
        "phone": "322-223",
        "account_manager_id": 100500,
    }


async def test_imports_client_if_not_found_locally(
    clients_domain, clients_dm, balance_client
):
    clients_dm.find_client_locally.coro.return_value = None
    clients_dm.upsert_client.coro.return_value = {
        "id": 11,
        "name": "imported name",
        "email": "imported@site.com",
        "phone": "455-554",
        "account_manager_id": None,
        "is_agency": False,
    }
    balance_client.find_client.coro.return_value = {
        "id": 11,
        "name": "imported name",
        "email": "imported@site.com",
        "phone": "455-554",
        "is_agency": False,
        "account_manager_id": None,
    }
    balance_client.list_client_contracts.coro.return_value = [
        {
            "id": 11,
            "external_id": "999/11",
            "currency": CurrencyType.RUB,
            "is_active": False,
            "date_start": datetime.date(2012, 2, 2),
            "date_end": datetime.date(2012, 3, 3),
            "payment_type": PaymentType.POST,
        },
        {
            "id": 22,
            "external_id": "999/22",
            "currency": CurrencyType.RUB,
            "is_active": True,
            "date_start": datetime.date(2012, 4, 4),
            "date_end": None,
            "payment_type": PaymentType.PRE,
        },
    ]

    result = await clients_domain.retrieve_client(client_id=11)

    balance_client.find_client.assert_called_with(client_id=11)
    clients_dm.upsert_client.assert_called_with(
        id=11,
        name="imported name",
        email="imported@site.com",
        phone="455-554",
        is_agency=False,
        account_manager_id=None,
        has_accepted_offer=False,
        con=None,
        representatives=[],
    )
    clients_dm.sync_client_contracts.assert_called_with(
        11,
        [
            {
                "id": 11,
                "external_id": "999/11",
                "currency": CurrencyType.RUB,
                "is_active": False,
                "date_start": datetime.date(2012, 2, 2),
                "date_end": datetime.date(2012, 3, 3),
                "payment_type": PaymentType.POST,
            },
            {
                "id": 22,
                "external_id": "999/22",
                "currency": CurrencyType.RUB,
                "is_active": True,
                "date_start": datetime.date(2012, 4, 4),
                "date_end": None,
                "payment_type": PaymentType.PRE,
            },
        ],
        None,
    )
    assert result == {
        "id": 11,
        "name": "imported name",
        "email": "imported@site.com",
        "phone": "455-554",
        "account_manager_id": None,
    }


async def test_not_returns_locally_existing_agency(clients_domain, clients_dm):
    clients_dm.find_client_locally.coro.return_value["is_agency"] = True

    with pytest.raises(ClientDoesNotExist) as exc:
        await clients_domain.retrieve_client(client_id=11)

    assert exc.value.client_id == 11


async def test_raises_if_client_not_found_anywhere(
    clients_domain, clients_dm, balance_client
):
    clients_dm.find_client_locally.coro.return_value = None
    balance_client.find_client.coro.return_value = None

    with pytest.raises(ClientDoesNotExist) as exc:
        await clients_domain.retrieve_client(client_id=11)

    assert exc.value.client_id == 11


async def test_raises_if_agency(clients_domain, clients_dm, balance_client):
    clients_dm.find_client_locally.coro.return_value = None
    balance_client.find_client.coro.return_value["is_agency"] = True

    with pytest.raises(ClientDoesNotExist) as exc:
        await clients_domain.retrieve_client(client_id=11)

    clients_dm.upsert_client.assert_not_called()
    assert exc.value.client_id == 11


async def test_not_saves_client_contracts_from_balance_if_client_fails_on_contracts(
    clients_domain, clients_dm, balance_client
):
    clients_dm.find_client_locally.coro.return_value = None
    balance_client.list_client_contracts.coro.side_effect = BalanceApiError()

    with pytest.raises(BalanceApiError):
        await clients_domain.retrieve_client(client_id=11)

    clients_dm.upsert_client.assert_not_called()
