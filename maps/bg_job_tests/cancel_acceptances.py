import pytest
from unittest.mock import MagicMock
from dataclasses import dataclass, field
from maps.infra.sedem.machine.lib.job_manager import JobManager
from maps.infra.sedem.machine.lib.collections import AcceptanceTestStatus


@dataclass
class CancelAcceptancesCase:
    name: str
    successfully_cancelled: list[str]
    failed_to_cancel: list[str] = field(default_factory=list)

    def __str__(self):
        return self.name


CASES = [
    CancelAcceptancesCase(
        name='one_cancel',
        successfully_cancelled=['1'],
    ),
    CancelAcceptancesCase(
        'multiple_cancel',
        successfully_cancelled=list(map(str, range(10)))
    ),
    CancelAcceptancesCase(
        'failed_to_cancel',
        successfully_cancelled=['1'],
        failed_to_cancel=['2']
    )
]


@pytest.mark.asyncio
@pytest.mark.parametrize('case', CASES, ids=str)
async def test_cancel_acceptances(job_manager: JobManager, acceptance_api: MagicMock, case):
    acceptance_api.lookup_acceptances_to_cancel.return_value = case.failed_to_cancel + case.successfully_cancelled
    job_manager.acceptance_sandbox_client.cancel_tasks.return_value = case.successfully_cancelled
    await job_manager.cancel_acceptances()
    acceptance_api.lookup_acceptances_to_cancel.assert_called_once()
    job_manager.acceptance_sandbox_client.cancel_tasks.assert_called_once_with(case.failed_to_cancel + case.successfully_cancelled)
    acceptance_api.set_tasks_status.assert_called_once_with(case.successfully_cancelled, AcceptanceTestStatus.FAILURE)
