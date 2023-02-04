import pytest
from unittest.mock import MagicMock
from maps.infra.sedem.machine.lib.job_manager import JobManager
from maps.infra.sedem.machine.lib.acceptance_api import AcceptanceTestSetLaunchDocument, AcceptanceTestSetStatus
from bson import ObjectId


@pytest.mark.asyncio
async def test_set_acceptances_launch_status_executing(job_manager: JobManager, acceptance_api: MagicMock):
    async def lookup_mock():
        yield ('TICKET-1', AcceptanceTestSetLaunchDocument(
            acceptance_id=ObjectId(),
            stage='testing',
            status=AcceptanceTestSetStatus.PENDING,
        ))
    acceptance_api.lookup_acceptances_to_set_executing.side_effect = lookup_mock
    await job_manager.set_acceptances_launch_status_executing()

    acceptance_api.lookup_acceptances_to_set_executing.assert_called_once()
    job_manager.startrek.add_ticket_comment.assert_called_once()
    acceptance_api.set_acceptance_status.assert_called_once()
