import pytest

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_uses_dm(clients_domain, clients_dm):
    clients_dm.list_clients.coro.return_value = [
        {"id": 1},
        {"id": 2},
    ]

    result = await clients_domain.list_clients()

    clients_dm.list_clients.assert_called_with()
    assert result == [{"id": 1}, {"id": 2}]
