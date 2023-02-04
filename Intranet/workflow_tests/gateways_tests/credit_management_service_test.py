from mock import Mock
from uuid import uuid1

from staff.budget_position.const import WORKFLOW_STATUS
from staff.budget_position.workflow_service import gateways, entities
from staff.budget_position.tests.workflow_tests.workflow_repository_mock import WorkflowRepositoryMock


def test_service_handles_when_all_workflows_cancelled():
    # given
    credit_management_application_id = 1
    person_id = 2
    workflow = Mock(spec=entities.AbstractWorkflow)
    workflow.id = uuid1()
    workflow.status = WORKFLOW_STATUS.CANCELLED
    repo = WorkflowRepositoryMock()
    repo.save(workflow)
    service = gateways.CreditManagementService(repo)
    service._on_all_workflows_cancelled = Mock()
    service._on_all_workflows_pushed = Mock()

    # when
    service.notify_about_changes_for_application(credit_management_application_id, person_id)

    # given
    service._on_all_workflows_cancelled.assert_called_once_with(credit_management_application_id, person_id)
    service._on_all_workflows_pushed.assert_not_called()


def test_service_handles_when_all_or_some_workflows_pushed():
    # given
    credit_management_application_id = 1
    person_id = 2

    cancelled_workflow = Mock(spec=entities.AbstractWorkflow)
    cancelled_workflow.id = uuid1()
    cancelled_workflow.status = WORKFLOW_STATUS.CANCELLED
    finished_workflow = Mock(spec=entities.AbstractWorkflow)
    finished_workflow.id = uuid1()
    finished_workflow.status = WORKFLOW_STATUS.FINISHED
    repo = WorkflowRepositoryMock()
    repo.save(cancelled_workflow)
    repo.save(finished_workflow)
    service = gateways.CreditManagementService(repo)
    service._on_all_workflows_cancelled = Mock()
    service._on_all_workflows_pushed = Mock()

    # when
    service.notify_about_changes_for_application(credit_management_application_id, person_id)

    # given
    service._on_all_workflows_cancelled.assert_not_called()
    service._on_all_workflows_pushed.assert_called_once_with(credit_management_application_id)
