import pytest

import random
from typing import Dict
from uuid import UUID

from staff.lib.testing import StaffFactory

from staff.budget_position.const import WORKFLOW_STATUS, PUSH_STATUS
from staff.budget_position.models import Workflow, ChangeRegistry
from staff.budget_position.tests.workflow_tests.utils import ChangeRegistryFactory, WorkflowModelFactory
from staff.budget_position.workflow_service.gateways import WorkflowRepository


@pytest.mark.django_db
def test_retry_workflows_status_filter():
    catalyst = StaffFactory()
    workflows: Dict[UUID, Dict[int, ChangeRegistry]] = dict()
    workflow_statuses: Dict[UUID, str] = dict()

    for workflow_status, _ in WORKFLOW_STATUS.choices():
        workflow = WorkflowModelFactory(status=workflow_status)
        workflows[workflow.id] = dict()
        workflow_statuses[workflow.id] = workflow.status

        changes = [
            ChangeRegistryFactory(workflow=workflow, push_status=None),
            ChangeRegistryFactory(
                workflow=workflow,
                push_status=PUSH_STATUS.ERROR,
                oebs_transaction_id=random.randint(1, 999),
                last_oebs_error='test',
            ),
            ChangeRegistryFactory(
                workflow=workflow,
                push_status=PUSH_STATUS.FINISHED,
                oebs_transaction_id=random.randint(1, 999),
            ),
        ]

        for change in changes:
            workflows[workflow.id][change.id] = change

    matching_workflows = set(Workflow.objects.filter(status=WORKFLOW_STATUS.FINISHED).values_list('id', flat=True))
    not_matching_workflows = set(workflows) - matching_workflows

    target = WorkflowRepository()

    target.retry_workflows(list(workflows.keys()), catalyst)

    for workflow_id in not_matching_workflows:
        actual_workflow = Workflow.objects.get(id=workflow_id)
        assert actual_workflow.status == workflow_statuses[workflow_id]

        for actual_change in actual_workflow.changeregistry_set.all():
            expected_change = workflows[workflow_id][actual_change.id]

            assert actual_change.push_status == expected_change.push_status
            assert actual_change.oebs_transaction_id == expected_change.oebs_transaction_id
            assert actual_change.last_oebs_error == expected_change.last_oebs_error

    for workflow_id in matching_workflows:
        actual_workflow = Workflow.objects.get(id=workflow_id)
        assert actual_workflow.status == WORKFLOW_STATUS.QUEUED
        assert actual_workflow.catalyst_id == catalyst.id

        for actual_change in actual_workflow.changeregistry_set.all():
            expected_change = workflows[workflow_id][actual_change.id]

            if expected_change.push_status == PUSH_STATUS.ERROR:
                assert actual_change.push_status is None
                assert actual_change.oebs_transaction_id is None
                assert actual_change.last_oebs_error is None
            else:
                assert actual_change.push_status == expected_change.push_status
                assert actual_change.oebs_transaction_id == expected_change.oebs_transaction_id
                assert actual_change.last_oebs_error == expected_change.last_oebs_error


@pytest.mark.django_db
def test_get_workflow_list():
    workflows: Dict[UUID, Workflow] = dict()

    for _ in range(random.randint(5, 7)):
        workflow = WorkflowModelFactory()
        workflows[workflow.id] = workflow

    matching_workflows = random.sample(workflows.keys(), random.randint(2, 4))

    target = WorkflowRepository()

    result = target.get_workflow_list(matching_workflows)
    assert set(wf.id for wf in result) == set(matching_workflows)

    for workflow in result:
        expected_workflow = workflows[workflow.id]

        for attr in ('id', 'code', 'status', 'catalyst_id', 'manually_processed'):
            assert getattr(workflow, attr) == getattr(expected_workflow, attr)
