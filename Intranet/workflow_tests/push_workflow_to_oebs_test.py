from unittest.mock import MagicMock
import uuid

import pytest

from staff.budget_position.workflow_service import entities, use_cases
from staff.lib.testing import StaffFactory


@pytest.mark.django_db
def test_when_workflow_should_be_marked_manually_processed_automatically():
    # given
    catalyst = StaffFactory()
    workflow_mock = MagicMock(spec=entities.AbstractWorkflow)
    workflow_mock.should_be_marked_manually_processed_automatically = True
    repo = MagicMock(spec=entities.WorkflowRepositoryInterface)
    repo.get_by_id.return_value = workflow_mock

    oebs_service = MagicMock(spec=entities.OEBSService)
    update_push_status_callback_mock = MagicMock()

    usecase = use_cases.PushWorkflowToOebs(repo, oebs_service, MagicMock(), update_push_status_callback_mock)

    # when
    usecase.push(uuid.uuid1(), catalyst.id)

    # then
    workflow_mock.mark_manually_processed.assert_called_once()
    update_push_status_callback_mock.assert_not_called()
    oebs_service.push_next_change_to_oebs.assert_not_called()
