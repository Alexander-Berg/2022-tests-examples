import pytest

from staff.budget_position import const
from staff.budget_position.models import Workflow
from staff.budget_position.tests.workflow_tests.utils import WorkflowModelFactory
from staff.budget_position.tests.utils import ChangeFactory
from staff.budget_position.workflow_service.gateways.workflow_repository import WorkflowRepository


@pytest.mark.django_db
def test_repository_saves_status_changes():
    # given
    existing_workflow_id = WorkflowModelFactory(status=const.WORKFLOW_STATUS.PENDING).id
    ChangeFactory(workflow_id=existing_workflow_id)
    repo = WorkflowRepository()
    existing_workflow = repo.get_by_id(existing_workflow_id)

    # when
    existing_workflow.status = const.WORKFLOW_STATUS.PUSHED
    repo.save(existing_workflow)

    # when
    assert Workflow.objects.get(id=existing_workflow.id).status == const.WORKFLOW_STATUS.PUSHED


@pytest.mark.django_db
def test_repository_saves_manually_processed_changes():
    # given
    existing_workflow_id = WorkflowModelFactory(manually_processed=None).id
    ChangeFactory(workflow_id=existing_workflow_id)
    repo = WorkflowRepository()
    existing_workflow = repo.get_by_id(existing_workflow_id)

    # when
    existing_workflow.manually_processed = True
    repo.save(existing_workflow)

    # when
    assert Workflow.objects.get(id=existing_workflow.id).manually_processed


@pytest.mark.django_db
def test_repository_cant_find_changing_workflow_without_budget_position():
    # given
    existing_workflow_id = WorkflowModelFactory(status=const.WORKFLOW_STATUS.PENDING).id
    ChangeFactory(workflow_id=existing_workflow_id, budget_position=None)
    repo = WorkflowRepository()

    # when
    workflows = repo.get_pending_workflows_by_budget_position_code(None)

    # when
    assert not workflows
