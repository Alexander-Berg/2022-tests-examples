import pytest

from staff.lib.testing import StaffFactory

from staff.budget_position.const import WORKFLOW_STATUS, PUSH_STATUS
from staff.budget_position.models import Workflow
from staff.budget_position.workflow_service import WorkflowRegistryService
from staff.budget_position.tests.utils import ChangeFactory
from staff.budget_position.tests.workflow_tests.utils import WorkflowModelFactory


@pytest.mark.django_db
def test_registry_service_can_mark_workflow_as_processed_manually():
    # given
    existing_workflow = WorkflowModelFactory(status=WORKFLOW_STATUS.CONFIRMED)
    ChangeFactory(workflow=existing_workflow)
    registry_service = WorkflowRegistryService()
    staff = StaffFactory(login='logintest')

    # when
    registry_service.mark_workflow_processed_manually(existing_workflow.id, staff.id)

    # then
    workflow = Workflow.objects.get(id=existing_workflow.id)
    assert workflow.status == WORKFLOW_STATUS.FINISHED
    assert workflow.manually_processed
    assert workflow.catalyst.login == staff.login


@pytest.mark.django_db
@pytest.mark.parametrize('push_status', [PUSH_STATUS.FINISHED, PUSH_STATUS.PUSHED])
def test_registry_service_can_mark_workflow_as_pushed_to_oebs(push_status):
    # given
    existing_workflow = WorkflowModelFactory(status=WORKFLOW_STATUS.CONFIRMED)
    ChangeFactory(workflow=existing_workflow, push_status=push_status, oebs_transaction_id=-1)
    registry_service = WorkflowRegistryService()

    # when
    registry_service.push_workflow_to_oebs(existing_workflow.id, StaffFactory().id)

    # then
    workflow = Workflow.objects.get(id=existing_workflow.id)
    assert workflow.status == WORKFLOW_STATUS.SENDING_NOTIFICATION
    assert not workflow.manually_processed


@pytest.mark.django_db
@pytest.mark.parametrize('push_status', [PUSH_STATUS.ERROR])
def test_registry_service_push_failed_workflow_to_oebs(push_status):
    existing_workflow = WorkflowModelFactory(status=WORKFLOW_STATUS.FINISHED)
    ChangeFactory(workflow=existing_workflow, push_status=push_status, oebs_transaction_id=-1)
    registry_service = WorkflowRegistryService()

    registry_service.push_failed_workflows_to_oebs([existing_workflow.id], StaffFactory().id)

    workflow = Workflow.objects.get(id=existing_workflow.id)
    assert workflow.status == WORKFLOW_STATUS.QUEUED
    assert not workflow.manually_processed


@pytest.mark.django_db
def test_registry_service_confirm_workflows():
    existing_workflow = WorkflowModelFactory(status=WORKFLOW_STATUS.PENDING)
    registry_service = WorkflowRegistryService()

    registry_service.confirm_workflows([existing_workflow.id])

    workflow = Workflow.objects.get(id=existing_workflow.id)
    assert workflow.status == WORKFLOW_STATUS.CONFIRMED
    assert not workflow.manually_processed
