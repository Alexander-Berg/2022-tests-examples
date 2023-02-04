import pytest
from unittest.mock import MagicMock, patch

import collections
import json
import random

from django.core.urlresolvers import reverse

from staff.budget_position.tests.workflow_tests.utils import ChangeRegistryFactory, WorkflowModelFactory
from staff.budget_position.const import WORKFLOW_STATUS
from staff.budget_position.models import ChangeRegistry
from staff.budget_position.views import export_change_registry
from staff.departments.tests.factories import ProposalMetadataFactory, VacancyFactory, HRProductFactory
from staff.lib.testing import BudgetPositionFactory, StaffFactory, DepartmentFactory, get_random_datetime
from staff.lib.auth import auth_mechanisms as auth


@pytest.fixture
def export_request_factory(rf):
    def make_request(get_kwargs=None):
        request = rf.get(reverse('budget-position-api:export-change-registry'), data=get_kwargs)
        request.auth_mechanism = auth.TVM
        request.yauser = None
        request.user = MagicMock(is_superuser=True)

        return request

    return make_request


@pytest.mark.django_db
def test_export_change_registry_invalid_continuation_token(export_request_factory):
    result = export_change_registry(export_request_factory({'continuation_token': 123}))

    assert result.status_code == 400, result.content


@pytest.mark.django_db
def test_export_change_registry(export_request_factory):
    page_size = 20
    samples = random.randint(int(page_size * 1.2), int(page_size * 1.8))
    valid_workflow_states = [
        WORKFLOW_STATUS.CONFIRMED,
        WORKFLOW_STATUS.QUEUED,
        WORKFLOW_STATUS.PUSHED,
        WORKFLOW_STATUS.SENDING_NOTIFICATION,
        WORKFLOW_STATUS.FINISHED,
    ]
    invalid_workflow_states = [state[0] for state in WORKFLOW_STATUS.choices() if state[0] not in valid_workflow_states]

    for i in range(samples):
        workflow = WorkflowModelFactory(
            code=f'code-{i}',
            status=random.choice(valid_workflow_states),
            confirmed_at=get_random_datetime(),
            proposal=random.choice([None, ProposalMetadataFactory()]),
            vacancy=random.choice([None, VacancyFactory()]),
        )
        invalid_workflow = WorkflowModelFactory(
            code=f'code-{i}-in',
            status=random.choice(invalid_workflow_states),
            confirmed_at=get_random_datetime(),
            proposal=random.choice([None, ProposalMetadataFactory()]),
            vacancy=random.choice([None, VacancyFactory()]),
        )
        ChangeRegistryFactory(
            workflow=workflow,
            budget_position=random.choice([None, BudgetPositionFactory()]),
            ticket=f'ticket-{i}',
            staff=random.choice([None, StaffFactory()]),
            headcount=random.randint(100, 233232),
            department=random.choice([None, DepartmentFactory()]),
            staff_hr_product=random.choice([None, HRProductFactory()]),
            dismissal_date=random.choice([None, get_random_datetime()]),
            effective_date=random.choice([None, get_random_datetime()]),
        )
        ChangeRegistryFactory(
            workflow=invalid_workflow,
            budget_position=random.choice([None, BudgetPositionFactory()]),
            ticket=f'ticket-{i}-in',
            staff=random.choice([None, StaffFactory()]),
            headcount=random.randint(100, 233232),
            department=random.choice([None, DepartmentFactory()]),
            staff_hr_product=random.choice([None, HRProductFactory()]),
            dismissal_date=random.choice([None, get_random_datetime()]),
            effective_date=random.choice([None, get_random_datetime()]),
        )

    with patch('staff.budget_position.views.export_views.PAGE_SIZE', page_size):
        result = export_change_registry(export_request_factory())

        _validate_result(result, page_size)
        continuation_token = json.loads(result.content).get('continuation_token')
        assert continuation_token, 'result should not fit in one page'

        result = export_change_registry(export_request_factory({'continuation_token': continuation_token}))

        _validate_result(result, samples - page_size + 1)
        assert json.loads(result.content).get('continuation_token') is None, 'result should fit in two pages'


def _validate_result(result, expected_data_length):
    assert result.status_code == 200
    response = json.loads(result.content)
    returned_ids = [c['id'] for c in response['data']]
    duplicates = [item for item, count in collections.Counter(returned_ids).items() if count > 1]
    assert duplicates == []
    assert len(response['data']) == expected_data_length

    for change_data in response['data']:
        change = ChangeRegistry.objects.get(id=change_data['id'])
        expected_change_data = {
            'id': change.id,
            'budget_position_id': change.budget_position.id if change.budget_position else None,
            'budget_position_code': change.budget_position.code if change.budget_position else None,
            'workflow_id': str(change.workflow_id),
            'proposal_id': change.workflow.proposal and change.workflow.proposal.proposal_id,
            'vacancy_id': change.workflow.vacancy_id,
            'workflow_confirmed_at': _format_date(change.workflow.confirmed_at),
            'workflow_code': change.workflow.code,
            'ticket': change.ticket,
            'optional_ticket': change.optional_ticket,
            'headcount': change.headcount,
            'department_id': change.department_id,
            'hr_product_id': change.staff_hr_product_id,
            'staff_id': change.staff_id,
            'login': change.staff and change.staff.login,
            'dismissal_date': _format_date(change.dismissal_date),
            'effective_date': _format_date(change.effective_date),
            'oebs_transaction_id': change.oebs_transaction_id,
            'remove_budget_position': change.remove_budget_position,
            'employment_type': change.employment_type,
            'is_replacement': change.is_replacement,
            'join_at': change.join_at,
        }
        for key, value in expected_change_data.items():
            assert change_data[key] == value, key


def _format_date(value):
    if value is None:
        return None
    return value.isoformat()
