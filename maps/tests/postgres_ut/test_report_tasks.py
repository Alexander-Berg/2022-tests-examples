import asyncio
import pytest
import json
import uuid
from asyncio import sleep
from datetime import date

from maps.b2bgeo.libs.py_unistat import global_unistat
from maps.b2bgeo.ya_courier.analytics_backend.lib.background_tasks.tasks import infinite_task
from maps.b2bgeo.ya_courier.analytics_backend.lib.db.types import DbParams
from maps.b2bgeo.ya_courier.analytics_backend.lib.reports.common.types import ReportJob, ReportStatus
from maps.b2bgeo.ya_courier.analytics_backend.lib.infra.db_report_tasks import (
    ATTEMPT_LIMIT,
    DbReportTasks,
    INACTIVE_PERIOD,
    init_unistat,
    SIGNAL_REPORT_COMPLETED,
    SIGNAL_REPORT_EXCEPTION,
    SIGNAL_REPORT_INACTIVE_TASK,
    SIGNAL_REPORT_INTERNAL_ERROR,
    SIGNAL_REPORT_LOST_TASK,
    SIGNAL_REPORT_QUEUE_SIZE,
    UPDATE_PERIOD_SEC,
)


async def _check_signals(
    tasks: DbReportTasks,
    queue_size: int = 0,
    completed: int = 0,
    exception: int = 0,
    inactive: int = 0,
    internal_error: int = 0,
    lost: int = 0,
) -> None:
    expected = [
        [f'{SIGNAL_REPORT_COMPLETED}_summ', float(completed)],
        [f'{SIGNAL_REPORT_EXCEPTION}_summ', float(exception)],
        [f'{SIGNAL_REPORT_INACTIVE_TASK}_summ', float(inactive)],
        [f'{SIGNAL_REPORT_INTERNAL_ERROR}_summ', float(internal_error)],
        [f'{SIGNAL_REPORT_LOST_TASK}_summ', float(lost)],
    ]
    expected.append([f'{SIGNAL_REPORT_QUEUE_SIZE}_axxx', float(queue_size)])

    await tasks.update_unistat()
    assert sorted(json.loads(global_unistat.to_json())) == sorted(expected)


def _init_unistat():
    global_unistat.reset()
    init_unistat()


def _default_job() -> ReportJob:
    return ReportJob(
        uuid=str(uuid.uuid4()), company_id=1, start_date=date(2021, 1, 1), end_date=date(2021, 1, 1), depot_ids=[]
    )


@infinite_task(0.1)
async def _run_stuck_task(tasks: DbReportTasks) -> None:
    async with await tasks.pop():
        await asyncio.sleep(10)


@pytest.mark.asyncio
async def test_task_normal_path_works(pg_instance: dict[str, str]):
    _init_unistat()
    tasks = DbReportTasks(pg_instance, DbParams(ssl_on=False), is_stopping=lambda: False)

    job = _default_job()
    await tasks.put(job)
    assert (await tasks.get(job.uuid)).status == ReportStatus.queued

    async with await tasks.pop() as running_task:
        assert (await tasks.get(job.uuid)).status == ReportStatus.running
        await running_task.complete()

    assert (await tasks.get(job.uuid)).status == ReportStatus.completed
    await _check_signals(tasks, completed=1)


@pytest.mark.asyncio
async def test_server_shutdown_via_cancel_postpones_task(pg_instance: dict[str, str]):
    _init_unistat()
    tasks = DbReportTasks(pg_instance, DbParams(ssl_on=False), is_stopping=lambda: False)

    job = _default_job()
    await tasks.put(job)

    for i in range(ATTEMPT_LIMIT):
        task = asyncio.create_task(_run_stuck_task(tasks))
        await asyncio.sleep(0.1)
        task.cancel()
        await task

    assert (await tasks.get(job.uuid)).status == ReportStatus.queued
    await _check_signals(tasks, queue_size=1)


@pytest.mark.asyncio
async def test_server_shutdown_via_flag_postpones_task(pg_instance: dict[str, str]):
    _init_unistat()
    stopping = False
    tasks = DbReportTasks(pg_instance, DbParams(ssl_on=False), is_stopping=lambda: stopping)

    job = _default_job()
    await tasks.put(job)

    for i in range(ATTEMPT_LIMIT):
        stopping = False
        try:
            async with await tasks.pop() as running_task:
                stopping = True
                await sleep(UPDATE_PERIOD_SEC)
                await running_task.alive()
                raise Exception('Error!')
        except Exception:
            pass

    assert (await tasks.get(job.uuid)).status == ReportStatus.queued
    await _check_signals(tasks, queue_size=1)


@pytest.mark.asyncio
async def test_status_is_internal_error_after_attempt_limit(pg_instance: dict[str, str]):
    _init_unistat()
    tasks = DbReportTasks(pg_instance, DbParams(ssl_on=False), is_stopping=lambda: False)

    job = _default_job()
    await tasks.put(job)

    for i in range(ATTEMPT_LIMIT):
        async with await tasks.pop() as _:
            raise Exception('Testing error!')

    task = await tasks.get(job.uuid)
    assert task.status == ReportStatus.internal_error
    assert 'Testing error!' in task.message
    assert task.completed_at is not None
    await _check_signals(tasks, exception=ATTEMPT_LIMIT, internal_error=1)


@pytest.mark.asyncio
async def test_not_completing_the_task_is_internal_error(pg_instance: dict[str, str]):
    _init_unistat()
    tasks = DbReportTasks(pg_instance, DbParams(ssl_on=False), is_stopping=lambda: False)

    job = _default_job()
    await tasks.put(job)

    for i in range(ATTEMPT_LIMIT):
        async with await tasks.pop() as _:
            pass

    task = await tasks.get(job.uuid)
    assert task.status == ReportStatus.internal_error
    assert task.completed_at is not None
    await _check_signals(tasks, exception=ATTEMPT_LIMIT, internal_error=1)


@pytest.mark.asyncio
async def test_mark_inactive_queue_task_after_sigkill(pg_instance: dict[str, str]):
    _init_unistat()
    tasks = DbReportTasks(pg_instance, DbParams(ssl_on=False), is_stopping=lambda: False)

    job = _default_job()
    await tasks.put(job)

    running_task = await tasks.pop()
    await running_task.__aenter__()
    # don't do __aexit__ which could happen after sigkill
    assert (await tasks.get(job.uuid)).status == ReportStatus.running

    # after half of the period nothing changes
    await sleep(INACTIVE_PERIOD.total_seconds() / 2)
    await tasks.mark_inactive_tasks()
    assert (await tasks.get(job.uuid)).status == ReportStatus.running

    # after another half of the period status becomes queued
    await sleep(INACTIVE_PERIOD.total_seconds() / 2)
    await tasks.mark_inactive_tasks()
    assert (await tasks.get(job.uuid)).status == ReportStatus.queued
    await _check_signals(tasks, inactive=1, queue_size=1)


@pytest.mark.asyncio
async def test_task_is_lost_after_several_attempts(pg_instance: dict[str, str]):
    _init_unistat()
    tasks = DbReportTasks(pg_instance, DbParams(ssl_on=False), is_stopping=lambda: False)

    job = _default_job()
    await tasks.put(job)

    for i in range(ATTEMPT_LIMIT - 1):
        async with await tasks.pop() as _:
            raise Exception('Testing error!')

    task = await tasks.get(job.uuid)
    assert task.status == ReportStatus.queued

    running_task = await tasks.pop()
    await running_task.__aenter__()
    # don't do __aexit__ which could happen after sigkill
    assert (await tasks.get(job.uuid)).status == ReportStatus.running

    # after half of the period nothing changes
    await sleep(INACTIVE_PERIOD.total_seconds() / 2)
    await tasks.mark_inactive_tasks()
    assert (await tasks.get(job.uuid)).status == ReportStatus.running

    # after another half of the period status becomes internal_error
    await sleep(INACTIVE_PERIOD.total_seconds() / 2)
    await tasks.mark_inactive_tasks()

    task = await tasks.get(job.uuid)
    assert task.status == ReportStatus.internal_error
    assert 'Task is lost' in task.message
    assert task.completed_at is not None
    await _check_signals(tasks, exception=ATTEMPT_LIMIT-1, lost=1)
