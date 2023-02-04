import asyncio
import pytest

from maps.infra.ecstatic.common.experimental_worker.lib.hooks_job import HooksJob, HooksJobQueue
from maps.infra.ecstatic.tool.client_interface.local_lock import LocalLockContextDecorator


@pytest.mark.asyncio
async def test_local_lock(hooks_job: HooksJob):
    hooks_job_task = asyncio.create_task(hooks_job.run())
    async with LocalLockContextDecorator(timeout=0):
        hooks_job._queue.put_nowait(HooksJobQueue.SwitchHooks())
        await asyncio.sleep(1)
        assert hooks_job._hook_controller.trigger_checks.call_count == 0
    await asyncio.sleep(1)
    assert hooks_job._hook_controller.trigger_checks.call_count == 1
    hooks_job.stop()
    await hooks_job_task
