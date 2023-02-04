import asyncio
import pytest

from maps.pylibs.utils.lib.asyncio import run_async
from maps.infra.ecstatic.tool.ecstatic_api.coordinator import Coordinator


@pytest.mark.asyncio
async def test_lock(coordinator_api: Coordinator, coordinator):
    async with coordinator_api.lock('ecstatic/host_switch/hostname',
                                    timeout=10,
                                    keepalive=5,
                                    defer_release=False):
        await asyncio.sleep(10)
        await run_async(coordinator.http_post, '/debug/PurgeExpiredLocks')
        locks = await run_async(coordinator_api.list_locks)
        assert len(locks.lock) == 1

    locks = await run_async(coordinator_api.list_locks)
    assert len(locks.lock) == 0
