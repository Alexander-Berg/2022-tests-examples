import pytest

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_requests_dm_correctly(domain, dm):
    await domain.fetch_pending_yang_tasks_count()

    dm.fetch_pending_yang_tasks_count.assert_called()


async def test_returns_data_from_dm(domain, dm):
    dm.fetch_pending_yang_tasks_count.coro.return_value = 5

    got = await domain.fetch_pending_yang_tasks_count()

    assert got == 5
