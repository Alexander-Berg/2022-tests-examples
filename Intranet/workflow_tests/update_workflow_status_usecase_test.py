from uuid import UUID

from unittest.mock import Mock

from uuid import uuid1

from staff.budget_position.const import PUSH_STATUS
from staff.budget_position.workflow_service.entities import WorkflowRepositoryInterface, workflows, Change
from staff.budget_position.workflow_service.use_cases.update_workflow_push_status import UpdateWorkflowPushStatus
from staff.budget_position.workflow_service.use_cases.interfaces import CreditManagementServiceInterface

from staff.budget_position.tests.workflow_tests.workflow_repository_mock import WorkflowRepositoryMock
from staff.budget_position.tests.workflow_tests.utils import OEBSServiceMock, StaffServiceMock


def update_push_status_mock(_id: UUID):
    pass


def create_usecase(
    credit_management_service_mock: CreditManagementServiceInterface,
    workflow_repo_mock: WorkflowRepositoryInterface,
):
    oebs_mock = OEBSServiceMock()
    usecase = UpdateWorkflowPushStatus(
        workflow_repo_mock,
        oebs_mock,
        update_push_status_mock,
        credit_management_service_mock,
        StaffServiceMock(),
    )
    return usecase


def test_usecase_notifies_credit_management_service_when_needed():
    # given
    workflow = workflows.WorkflowCreditManagement(uuid1(), [Change(push_status=PUSH_STATUS.FINISHED)])
    workflow.credit_management_id = 100500

    workflow_repo_mock = WorkflowRepositoryMock()
    workflow_repo_mock.save(workflow)

    credit_service_mock = Mock(spec=CreditManagementServiceInterface)

    usecase = create_usecase(credit_service_mock, workflow_repo_mock)

    # when
    usecase.update_status(workflow.id)

    # then
    credit_service_mock.notify_about_changes_for_application.assert_called_once_with(
        workflow.credit_management_id,
        None,
    )
