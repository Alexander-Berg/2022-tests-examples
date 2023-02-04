import pytest
from dataclasses import dataclass
from unittest.mock import MagicMock, call

from maps.infra.sedem.machine.lib.background_task_api import TaskStatus, TaskActionType, TaskDocument
from maps.infra.sedem.machine.lib.job_manager import JobManager


@pytest.mark.asyncio
async def test_comment_st_ticket(job_manager: JobManager, release_api: MagicMock, task_api: MagicMock):
    @dataclass
    class RETURN_TICKET:
        st_ticket: str

    async def lookup_ids(task_type):
        yield 123

    async def document(id):
        return TaskDocument(
            payload={'release_id': 123, 'message': 'test'},
            action_type=TaskActionType.COMMENT_ST_TICKET,
            status=TaskStatus.ENQUEUED
        )

    async def release(id):
        return RETURN_TICKET(st_ticket='TEST-123')

    task_api.lookup_enqueued_tasks_ids.side_effect = lookup_ids
    task_api.load_task.side_effect = document
    task_api.set_task_done.side_effect = None
    release_api.load_release.side_effect = release

    await job_manager.comment_st_tickets()

    assert task_api.lookup_enqueued_tasks_ids.call_count == 1
    assert task_api.load_task.call_count == 1
    assert task_api.set_task_done.call_count == 1
    assert release_api.load_release.call_count == 1

    assert task_api.lookup_enqueued_tasks_ids.mock_calls == [
        call(TaskActionType.COMMENT_ST_TICKET)
    ]
    assert task_api.load_task.mock_calls == [
        call(123)
    ]
    assert task_api.set_task_done.mock_calls == [
        call(123)
    ]
    assert release_api.load_release.mock_calls == [
        call(123)
    ]
