import pytest

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_uses_dm(clients_domain, clients_dm):
    clients_dm.list_agencies.coro.return_value = [
        {"id": 333, "name": "Агентство 1"},
        {"id": 444, "name": "Агентство 2"},
    ]

    result = await clients_domain.list_agencies()

    clients_dm.list_agencies.assert_called_with()
    assert result == [
        {"id": 333, "name": "Агентство 1"},
        {"id": 444, "name": "Агентство 2"},
    ]
