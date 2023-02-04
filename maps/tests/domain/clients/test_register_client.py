import pytest

from maps_adv.billing_proxy.lib.core.balance_client import BalanceApiError
from maps_adv.billing_proxy.lib.domain.exceptions import ClientByUidDoesNotExist
from maps_adv.billing_proxy.tests.helpers import mock_find_by_uid_clients

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.fixture(autouse=True)
def common_balance_client_mocks(balance_client):
    balance_client.create_client.coro.return_value = 55
    balance_client.find_client.coro.side_effect = mock_find_by_uid_clients
    balance_client.list_client_passports.coro.return_value = []


async def test_uses_dm(clients_domain, clients_dm):
    clients_dm.insert_client.coro.return_value = {"id": 11, "name": "Имя"}

    result = await clients_domain.register_client(
        uid=None,
        name="Имя",
        email="yandex@yandex.ru",
        phone="322-223",
        account_manager_id=100500,
        domain="someDomain",
        has_accepted_offer=False,
    )

    clients_dm.insert_client.assert_called_with(
        55,
        "Имя",
        "yandex@yandex.ru",
        "322-223",
        100500,
        "someDomain",
        None,
        False,
        True,
    )
    assert result == {"id": 11, "name": "Имя"}


async def test_creates_client_in_balance(clients_domain, balance_client):
    await clients_domain.register_client(
        uid=None,
        name="Имя клиента",
        email="email@example.com",
        phone="8(499)123-45-67",
        account_manager_id=None,
        domain="someDomain",
        has_accepted_offer=False,
    )

    assert balance_client.create_client.call_args[0] == (
        "Имя клиента",
        "email@example.com",
        "8(499)123-45-67",
    )


async def test_not_creates_locally_if_balance_fails(
    clients_domain, clients_dm, balance_client
):
    balance_client.create_client.coro.side_effect = BalanceApiError()

    try:
        await clients_domain.register_client(
            uid=None,
            name="Имя клиента",
            email="email@example.com",
            phone="8(499)123-45-67",
            account_manager_id=None,
            domain="",
            has_accepted_offer=True,
        )
    except BalanceApiError:
        pass

    clients_dm.insert_client.assert_not_called()


async def test_adds_user_to_client(clients_domain, clients_dm, balance_client):
    clients_dm.insert_client.coro.return_value = {"id": 55, "name": "Имя"}
    clients_dm.find_client_locally.coro.return_value = {"is_agency": False}

    result = await clients_domain.register_client(
        uid=123,
        name="Имя",
        email="yandex@yandex.ru",
        phone="322-223",
        account_manager_id=100500,
        domain="someDomain",
        has_accepted_offer=False,
    )

    balance_client.create_client.assert_called_with(
        "Имя",
        "yandex@yandex.ru",
        "322-223",
    )
    clients_dm.insert_client.assert_called_with(
        55,
        "Имя",
        "yandex@yandex.ru",
        "322-223",
        100500,
        "someDomain",
        None,
        False,
        True,
    )
    balance_client.create_user_client_association.assert_called_with(55, 123)
    assert result == {"id": 55, "name": "Имя"}


async def test_returns_existing_client(clients_domain, clients_dm, balance_client):
    #   clients_dm.find_client_locally.coro.return_value = None
    clients_dm.upsert_client.coro.return_value = {
        "name": "Имя клиента",
        "email": "email@example.com",
        "phone": "8(499)123-45-67",
        "id": 55,
    }

    result = await clients_domain.register_client(
        uid=10001,
        name="Имя",
        email="yandex@yandex.ru",
        phone="322-223",
        account_manager_id=100500,
        domain="someDomain",
        has_accepted_offer=False,
    )

    balance_client.create_client.assert_called_with(
        "Имя клиента",
        "email@example.com",
        "8(499)123-45-67",
        55,
    )
    clients_dm.upsert_client.assert_called_with(
        id=55,
        name="Имя клиента",
        email="email@example.com",
        phone="8(499)123-45-67",
        is_agency=False,
        partner_agency_id=1,
        has_accepted_offer=False,
        representatives=[],
        con=None,
    )
    balance_client.create_user_client_association.assert_not_called()
    assert result == {
        "id": 55,
        "name": "Имя клиента",
        "email": "email@example.com",
        "phone": "8(499)123-45-67",
    }


async def test_fails_if_existing_client_is_agency(
    clients_domain, clients_dm, balance_client
):
    clients_dm.insert_client.coro.return_value = {"id": 55, "name": "Имя"}
    #   clients_dm.find_client_locally.coro.return_value = {"is_agency": False}

    with pytest.raises(ClientByUidDoesNotExist) as exc:
        await clients_domain.register_client(
            uid=10002,
            name="Имя",
            email="yandex@yandex.ru",
            phone="322-223",
            account_manager_id=100500,
            domain="someDomain",
            has_accepted_offer=False,
        )

    balance_client.create_client.assert_not_called()
    balance_client.create_user_client_association.assert_not_called()
    clients_dm.insert_client.assert_not_called()
    assert exc.value.uid == 10002
    assert exc.value.is_agency is True
