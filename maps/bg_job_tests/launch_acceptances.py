from unittest.mock import call

import pytest

from maps.infra.sedem.machine.lib.acceptance_api import (
    AcceptanceApi, AcceptanceTestLaunchDocument, AcceptanceTestStatus
)
from maps.infra.sedem.machine.lib.job_manager import JobManager


@pytest.mark.asyncio
async def test_acceptances_launch_retry(job_manager: JobManager, acceptance_api: AcceptanceApi):
    async def lookup_mock():
        for tries_left in (2, 1):
            yield 'acceptance_id', AcceptanceTestLaunchDocument(
                scheduler_id='1',
                status=AcceptanceTestStatus.PENDING,
                tries_left=tries_left
            )

    acceptance_api.lookup_acceptances_to_start.side_effect = lookup_mock
    acceptance_api.get_task_status.return_value = AcceptanceTestStatus.PENDING
    job_manager.acceptance_sandbox_client.scheduler_run_once.side_effect = Exception('some reason')
    await job_manager.launch_acceptances()
    assert acceptance_api.update_task_launch_info.mock_calls == [
        call(
            acceptance_id='acceptance_id',
            test_launch=AcceptanceTestLaunchDocument(
                scheduler_id='1',
                status=AcceptanceTestStatus.PENDING,
                tries_left=1,
            ),
        ),
        call(
            acceptance_id='acceptance_id',
            test_launch=AcceptanceTestLaunchDocument(
                scheduler_id='1',
                status=AcceptanceTestStatus.FAILURE,
                tries_left=0,
                reason='some reason',
            ),
        ),
    ]
