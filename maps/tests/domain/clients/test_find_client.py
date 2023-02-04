import pytest

from maps_adv.billing_proxy.lib.domain.exceptions import ClientByUidDoesNotExist
from maps_adv.billing_proxy.tests.helpers import mock_find_by_uid_clients

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("db")]


@pytest.fixture(autouse=True)
def common_balance_client_mocks(balance_client):
    balance_client.find_client_by_uid.coro.side_effect = mock_find_by_uid_clients


async def test_returns_client_data(clients_domain):
    result = await clients_domain.find_client_by_uid(10001)

    assert result == {
        "id": 55,
        "name": "Имя клиента",
        "email": "email@example.com",
        "phone": "8(499)123-45-67",
        "is_agency": False,
        "partner_agency_id": 1,
        "account_manager_id": None,
    }


async def test_client_stored_in_db(clients_domain, balance_client):
    client = await clients_domain.find_client_by_uid(10001)

    result = await clients_domain.retrieve_client(client["id"])

    balance_client.find_client.assert_not_called()

    assert result == {
        "id": 55,
        "name": "Имя клиента",
        "email": "email@example.com",
        "phone": "8(499)123-45-67",
        "partner_agency_id": 1,
        "account_manager_id": None,
    }


@pytest.mark.parametrize("uid", [10002, 10003])  # is_agency
async def test_not_returns_client(clients_domain, uid):
    with pytest.raises(ClientByUidDoesNotExist) as exc:
        await clients_domain.find_client_by_uid(uid)

    assert exc.value.uid == uid
