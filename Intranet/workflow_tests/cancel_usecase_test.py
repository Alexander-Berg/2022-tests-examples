from unittest.mock import Mock
from uuid import uuid1

from staff.budget_position.const import WORKFLOW_STATUS
from staff.budget_position.workflow_service.entities import AbstractWorkflow
from staff.budget_position.workflow_service.use_cases import Cancel, CreditManagementServiceInterface
from staff.budget_position.tests.workflow_tests.workflow_repository_mock import WorkflowRepositoryMock


def test_cancel_usecase_will_not_notify_credit_management_service_on_absent_application_id():
    # given
    credit_management_service_mock = Mock(spec=CreditManagementServiceInterface)
    person_id = 1
    workflow_mock = Mock(spec=AbstractWorkflow)
    workflow_mock.id = uuid1()
    workflow_mock.status = WORKFLOW_STATUS.CANCELLED
    workflow_mock.credit_management_id = None

    repo = WorkflowRepositoryMock()
    repo.save(workflow_mock)

    use_case = Cancel(repo, credit_management_service_mock)

    # when
    use_case.cancel(workflow_mock.id, person_id)

    # then
    credit_management_service_mock.notify_about_changes_for_application.assert_not_called()


def test_cancel_usecase_notifies_credit_management_service_when_needed():
    # given
    credit_management_service_mock = Mock(spec=CreditManagementServiceInterface)
    person_id = 1
    workflow_mock = Mock(spec=AbstractWorkflow)
    workflow_mock.id = uuid1()
    workflow_mock.status = WORKFLOW_STATUS.CANCELLED
    workflow_mock.credit_management_id = 2

    repo = WorkflowRepositoryMock()
    repo.save(workflow_mock)

    use_case = Cancel(repo, credit_management_service_mock)

    # when
    use_case.cancel(workflow_mock.id, person_id)

    # then
    (
        credit_management_service_mock.notify_about_changes_for_application
        .assert_called_once_with(workflow_mock.credit_management_id, person_id)
    )
