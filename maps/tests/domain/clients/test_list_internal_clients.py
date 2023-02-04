import pytest

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_uses_dm(clients_domain, clients_dm):
    clients_dm.list_agency_clients.coro.return_value = [
        {"id": 3, "name": "Клиент 1"},
        {"id": 4, "name": "Клиент 2"},
    ]

    result = await clients_domain.list_internal_clients()

    clients_dm.list_agency_clients.assert_called_with(None)
    assert result == [{"id": 3, "name": "Клиент 1"}, {"id": 4, "name": "Клиент 2"}]
