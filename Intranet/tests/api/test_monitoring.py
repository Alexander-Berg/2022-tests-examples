import pytest


pytestmark = pytest.mark.asyncio


async def test_unistat(client):
    """
    Тест unistat ручки для мониторинга
    """
    response = await client.get('api/monitoring/unistat')

    assert response.status_code == 200
