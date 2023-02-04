import pytest

pytestmark = [pytest.mark.asyncio]


async def test_returns_200(api):
    await api.get("/ping", expected_status=204)
