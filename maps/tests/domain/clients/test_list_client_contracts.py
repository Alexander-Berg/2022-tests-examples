import pytest

from maps_adv.billing_proxy.lib.domain.exceptions import ClientDoesNotExist

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_uses_dm(clients_domain, clients_dm):
    clients_dm.client_exists.coro.return_value = True
    clients_dm.list_contacts_by_client.coro.return_value = [
        {"id": 22, "external_id": "2222/22"},
        {"id": 33, "external_id": "3333/33"},
    ]

    result = await clients_domain.list_client_contracts(333)

    clients_dm.list_contacts_by_client.assert_called_with(333)
    assert result == [
        {"id": 22, "external_id": "2222/22"},
        {"id": 33, "external_id": "3333/33"},
    ]


async def test_raises_for_inexistent_agency(clients_domain, clients_dm):
    clients_dm.client_exists.coro.return_value = False

    with pytest.raises(ClientDoesNotExist) as exc:
        await clients_domain.list_client_contracts(333)

    assert exc.value.client_id == 333
