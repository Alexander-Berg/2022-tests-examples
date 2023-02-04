import pytest

from maps_adv.billing_proxy.lib.data_manager.exceptions import ClientsDoNotExist
from maps_adv.billing_proxy.lib.domain.exceptions import ClientDoesNotExist

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_calls_dm(clients_domain, clients_dm):

    await clients_domain.set_account_manager_for_client(
        client_id=11, account_manager_id=100500
    )

    clients_dm.set_account_manager_for_client.assert_called_with(11, 100500)


async def test_raises_for_nonexistent_agency(factory, clients_dm, clients_domain):
    inexistent_id = await factory.get_inexistent_client_id()

    clients_dm.set_account_manager_for_client.coro.side_effect = ClientsDoNotExist(
        client_ids=[inexistent_id]
    )

    with pytest.raises(ClientDoesNotExist) as exc:
        await clients_domain.set_account_manager_for_client(
            client_id=inexistent_id, account_manager_id=100500
        )

    assert exc.value.client_id == inexistent_id
