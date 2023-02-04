import pytest

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]

from maps_adv.billing_proxy.lib.core.balance_client import UserIsAlreadyAssociatedError
from maps_adv.billing_proxy.lib.domain.exceptions import (
    ClientDoesNotExist,
    AgencyDoesNotExist,
    UserIsAssignedToAnotherClient,
)


async def test_calls_balance(clients_domain, clients_dm, balance_client):
    clients_dm.find_client_locally.coro.return_value = {"is_agency": False}
    await clients_domain.add_user_to_client(123, False, 456)

    balance_client.create_user_client_association.coro.assert_called_with(123, 456)


async def test_raises_if_user_is_already_associated(
    clients_domain, clients_dm, balance_client
):
    clients_dm.find_client_locally.coro.return_value = {"is_agency": False}
    balance_client.create_user_client_association.coro.side_effect = (
        UserIsAlreadyAssociatedError()
    )
    with pytest.raises(UserIsAssignedToAnotherClient):
        await clients_domain.add_user_to_client(123, False, 456)


async def test_raises_if_no_client(clients_domain, clients_dm):
    clients_dm.find_client_locally.coro.return_value = None

    with pytest.raises(ClientDoesNotExist) as exc:
        await clients_domain.add_user_to_client(123, False, 456)
    assert exc.value.client_id == 123

    with pytest.raises(AgencyDoesNotExist) as exc:
        await clients_domain.add_user_to_client(123, True, 456)
    assert exc.value.agency_id == 123

    clients_dm.find_client_locally.coro.return_value = {"is_agency": True}

    with pytest.raises(ClientDoesNotExist) as exc:
        await clients_domain.add_user_to_client(123, False, 456)
    assert exc.value.client_id == 123

    clients_dm.find_client_locally.coro.return_value = {"is_agency": False}

    with pytest.raises(AgencyDoesNotExist) as exc:
        await clients_domain.add_user_to_client(123, True, 456)
    assert exc.value.agency_id == 123
