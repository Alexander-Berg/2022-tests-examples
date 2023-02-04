import asyncio
from datetime import timedelta

import pytest

from maps.infra.pycare.lib.bg_job import BackgroundJobManager, BackgroundJob, background_job_errors


@pytest.fixture(scope='function', autouse=True)
async def cleanup_job_manager():
    yield
    await BackgroundJobManager.cleanup()


@pytest.fixture(scope='function')
def job_payload():
    class Job:
        def __init__(self):
            self.runs = 0

        async def __call__(self):
            self.runs += 1

        async def fail(self):
            raise Exception('ooops...')

    return Job()


@pytest.mark.asyncio
async def test_interval_job_run(job_payload):
    job = BackgroundJob(job_payload, interval=timedelta(seconds=1))
    await job.start()  # Spawn background job.
    # Wait for the next iteration with excessive waiting time.
    # Job should execute at least once in next 3 secs (timeout increased for CI).
    await job.wait_next_iteration(timeout=3)
    await job.stop()
    assert job_payload.runs == 1


@pytest.mark.asyncio
async def test_immediate_job_run(job_payload):
    job = BackgroundJob(job_payload, interval=timedelta(minutes=1))
    await job.start()  # Spawn background job.
    job.run_soon()
    await asyncio.sleep(0.1)  # Return to event loop to execute payload.
    await job.stop()
    assert job_payload.runs == 1


@pytest.mark.asyncio
async def test_already_running_job_start(job_payload):
    job = BackgroundJob(job_payload, interval=timedelta(seconds=1))
    await job.start()
    with pytest.raises(AssertionError, match='.*already running.*'):
        await job.start()
    await job.wait_next_iteration()  # Check that job is still running.
    await job.stop()
    assert job_payload.runs == 1


@pytest.mark.asyncio
async def test_job_fail(job_payload):
    assert ('background_job_unhandled_errors_ammx', 0.) == background_job_errors.extract_yasm_metric()
    job = BackgroundJob(job_payload.fail, interval=timedelta(seconds=1))
    await job.start()
    await job.wait_next_iteration()
    assert ('background_job_unhandled_errors_ammx', 1.) == background_job_errors.extract_yasm_metric()
    await job.wait_next_iteration()
    await job.wait_next_iteration()
    assert ('background_job_unhandled_errors_ammx', 2.) == background_job_errors.extract_yasm_metric()
    await job.stop()
    assert ('background_job_unhandled_errors_ammx', 0.) == background_job_errors.extract_yasm_metric()


@pytest.mark.asyncio
async def test_restart(job_payload):
    job = BackgroundJob(job_payload, interval=timedelta(seconds=1))
    await job.start()
    await job.wait_next_iteration(timeout=3)
    await job.stop()
    assert job_payload.runs == 1
    await job.start()
    await job.wait_next_iteration(timeout=3)
    await job.stop()
    assert job_payload.runs == 2


@pytest.mark.asyncio
async def test_concurrent_job_spawn(job_payload):
    job1 = BackgroundJob(job_payload, interval=timedelta(seconds=1))
    job2 = BackgroundJob(job_payload, interval=timedelta(seconds=1))
    await asyncio.gather(
        job1.start(),
        job2.start(),
    )
    # Wait for the next iteration with excessive waiting time.
    # Job should execute at least once in next 3 secs (timeout increased for CI).
    await asyncio.gather(
        job1.wait_next_iteration(timeout=3),
        job2.wait_next_iteration(timeout=3),
    )
    await asyncio.gather(
        job1.stop(),
        job2.stop(),
    )
    assert job_payload.runs == 2
