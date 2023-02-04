import pytest

pytestmark = [pytest.mark.asyncio]

url = "/ping"


async def test_returns_204(api):
    await api.get(url, expected_status=204)
