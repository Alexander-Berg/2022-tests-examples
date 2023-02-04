import pytest

import uuid
import random
from mock import call, MagicMock

from django.conf import settings
from django.template import loader

from staff.budget_position.workflow_service import entities
from staff.budget_position.workflow_service.use_cases.interfaces import (
    CreditManagementServiceInterface,
    StartrekService,
)
from staff.budget_position.workflow_service.use_cases.push_workflow import PushWorkflow


@pytest.mark.django_db
def test_try_send_notification_for_workflow_cannot_be_finalized():
    workflow_id = uuid.uuid1()
    repository = MagicMock(spec=entities.WorkflowRepositoryInterface)
    credit_management_service = MagicMock(spec=CreditManagementServiceInterface)
    startrek_service = MagicMock(spec=StartrekService)
    staff_service = MagicMock(spec=entities.StaffService)
    femida_service = MagicMock(spec=entities.FemidaService)

    repository.can_workflow_be_finalized = _return_if_matched(workflow_id, False)
    target = PushWorkflow(repository, startrek_service, staff_service, femida_service, credit_management_service)

    target.try_send_notification_for_workflow(workflow_id)

    repository.get_by_id.assert_not_called()


@pytest.mark.parametrize(
    'workflow_has_errors, ticket_queue, should_pass_local_fields',
    [
        (True, 'TICKET', False),
        (False, 'TICKET', False),
        (True, 'TJOB', True),
        (False, 'TJOB', True),
    ],
)
@pytest.mark.django_db
def test_try_send_notification_for_workflow_without_errors(workflow_has_errors, ticket_queue, should_pass_local_fields):
    repository = MagicMock(spec=entities.WorkflowRepositoryInterface)
    startrek_service = MagicMock(spec=StartrekService)
    credit_management_service = MagicMock(spec=CreditManagementServiceInterface)
    staff_service = MagicMock(spec=entities.StaffService)
    femida_service = MagicMock(spec=entities.FemidaService)
    workflow = MagicMock(spec=entities.AbstractWorkflow)
    workflow.id = uuid.uuid1()
    workflow.changes = [MagicMock(spec=entities.Change), MagicMock(spec=entities.Change, last_oebs_error=None)]

    repository.can_workflow_be_finalized = _return_if_matched(workflow.id, True)
    repository.get_by_id = _return_if_matched(workflow.id, workflow)
    tickets = [f'{ticket_queue}-{random.randint(3, 50000)}' for _ in range(random.randint(3, 10))]
    repository.get_related_tickets = _return_if_matched(workflow.id, tickets)
    workflow.has_errors = lambda: workflow_has_errors
    target = PushWorkflow(repository, startrek_service, staff_service, femida_service, credit_management_service)

    target.try_send_notification_for_workflow(workflow.id)

    expected_local_fields = _get_local_fields(should_pass_local_fields, ticket_queue, workflow_has_errors)
    expected_text = _get_comment_text(workflow, workflow_has_errors, workflow.id)

    if should_pass_local_fields:
        call_pairs = [
            [call.add_comment(key=ticket, text=expected_text), call.update_issue(ticket, **expected_local_fields)] for
            ticket in tickets
        ]
        expected_startrek_calls = [item for sublist in call_pairs for item in sublist]
    else:
        expected_startrek_calls = [
            call.add_comment(key=ticket, text=expected_text)
            for ticket in tickets
        ]

    startrek_service.assert_has_calls(expected_startrek_calls)
    workflow.mark_finished.assert_called_once()
    repository.save.assert_called_once_with(workflow)


def _get_local_fields(should_pass_local_fields, ticket_queue, workflow_has_errors):
    expected_local_fields = {}
    queue_uid = settings.STARTREK_QUEUE_UIDS.get(ticket_queue)
    status = 'wf_err' if workflow_has_errors else 'wf_ok'

    if should_pass_local_fields:
        workflow_status_field = f'{queue_uid}--{settings.STARTREK_WORKFLOW_STATUS_FIELD}'
        expected_local_fields[workflow_status_field] = status

    return expected_local_fields


def _get_comment_text(workflow, workflow_has_errors, workflow_id):
    template_context = {
        'staff_host': settings.STAFF_HOST,
        'workflow_id': workflow_id,
        'changes': workflow.changes,
    }
    if workflow_has_errors:
        template_name = 'startrek/workflow_push_failed.html'
        template_context['error'] = 'Не удалось провести изменения в ОЕБС'
    else:
        template_name = 'startrek/workflow_pushed.html'
    expected_text = loader.render_to_string(
        template_name=template_name,
        context=template_context,
    )
    return expected_text


def _return_if_matched(expected1, value):
    def _call(actual1):
        assert actual1 == expected1
        return value
    return _call


def _return_if_matched_ob_object(expected1, value):
    def _call(_, actual1):
        assert actual1 == expected1
        return value
    return _call
