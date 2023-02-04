import asyncio
import pytest
from unittest.mock import AsyncMock
from maps.infra.ecstatic.common.experimental_worker.lib.ymtorrent_job import YmtorrentJob
from maps.infra.ecstatic.common.experimental_worker.lib.hooks_job import HooksJob
from maps.infra.ecstatic.common.experimental_worker.lib.coordinator_polling_job import CoordinatorPollingJob
from maps.infra.ecstatic.common.experimental_worker.lib.coordinator_push_job import CoordinatorPushJob


@pytest.mark.asyncio
async def test_shutdown_hooks_job(hooks_job: HooksJob):
    job = asyncio.create_task(hooks_job.run())
    await asyncio.sleep(0)
    hooks_job.stop()
    await asyncio.wait_for(job, 5)


@pytest.mark.asyncio
async def test_shutdown_ymtorrent_job(ymtorrent_job: YmtorrentJob):
    job = asyncio.create_task(ymtorrent_job.run())
    await asyncio.sleep(0)
    ymtorrent_job.stop()
    await asyncio.wait_for(job, 5)


@pytest.mark.asyncio
async def test_shutdown_coordinator_push_job(coordinator_push_job: CoordinatorPushJob):
    job = asyncio.create_task(coordinator_push_job.run())
    await asyncio.sleep(0)
    coordinator_push_job.stop()
    await asyncio.wait_for(job, 5)


@pytest.mark.asyncio
async def test_shutdown_coordinator_polling_job(coordinator_polling_job: CoordinatorPollingJob):
    coordinator_polling_job._update_torrents = AsyncMock()
    coordinator_polling_job._update_postdl = AsyncMock()
    coordinator_polling_job._update_switches = AsyncMock()
    job = asyncio.create_task(coordinator_polling_job.run())
    await asyncio.sleep(0)
    coordinator_polling_job.stop()
    await asyncio.wait_for(job, 5)
