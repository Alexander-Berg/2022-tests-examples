import pytest
from mock import MagicMock, patch

import json
from random import random
from typing import Dict

from django.core.urlresolvers import reverse

from staff.departments.tests.factories import ProposalMetadataFactory
from staff.lib.testing import BudgetPositionFactory, StaffFactory

from staff.budget_position import const, models, views
from staff.budget_position.views.front_views import _blocked_bp_info_by_position_codes
from staff.budget_position.tests.workflow_tests.utils import WorkflowModelFactory, ChangeRegistryFactory


@pytest.mark.django_db
def test_blocked_bp_info_gives_error_on_empty_form(rf):
    request = rf.post(reverse('budget-position-api:blocked-bp-info'), json.dumps({}), 'application/json')

    # when
    result = views.blocked_bp_info(request)

    # then
    assert result.status_code == 400


@pytest.mark.django_db
def test_blocked_bp_info_gives_empty_result_on_absent_bp(rf):
    budget_position = BudgetPositionFactory()
    request = rf.post(
        reverse('budget-position-api:blocked-bp-info'),
        json.dumps({'codes': [budget_position.code]}),
        'application/json',
    )

    # when
    result = views.blocked_bp_info(request)

    # then
    assert result.status_code == 200
    data = json.loads(result.content)
    assert data == {}


@pytest.mark.django_db
def test_blocked_bp_info_by_position_codes():
    # given
    proposal1 = ProposalMetadataFactory(author=StaffFactory())
    change1 = ChangeRegistryFactory(
        workflow=WorkflowModelFactory(status=const.WORKFLOW_STATUS.PENDING, proposal=proposal1),
        budget_position=BudgetPositionFactory(),
    )
    change2 = ChangeRegistryFactory(
        workflow=WorkflowModelFactory(status=const.WORKFLOW_STATUS.CONFIRMED, proposal=ProposalMetadataFactory()),
        budget_position=BudgetPositionFactory(),
    )
    change3 = ChangeRegistryFactory(
        workflow=WorkflowModelFactory(status=const.WORKFLOW_STATUS.CANCELLED, proposal=ProposalMetadataFactory()),
        budget_position=BudgetPositionFactory(),
    )

    localize_mock = MagicMock()

    # when
    with patch('staff.budget_position.views.front_views.localize', localize_mock):
        result = _blocked_bp_info_by_position_codes([
            change1.budget_position.code,
            change2.budget_position.code,
            change3.budget_position.code,
        ])

    localize_mock.assert_called_once_with(
        {
            'login': proposal1.author.login,
            'first_name': proposal1.author.first_name,
            'last_name': proposal1.author.last_name,
            'first_name_en': proposal1.author.first_name_en,
            'last_name_en': proposal1.author.last_name_en,
        },
    )

    # then
    assert result == {
        change1.budget_position.code: {
            'proposal_id': proposal1.proposal_id,
            'author': localize_mock.return_value,
            'tickets': [],
        }
    }


@pytest.mark.django_db
def test_blocked_bp_info_by_position_codes_with_tickets():
    # given
    change1 = ChangeRegistryFactory(
        workflow=WorkflowModelFactory(
            status=const.WORKFLOW_STATUS.PENDING,
            proposal=ProposalMetadataFactory(author=StaffFactory()),
        ),
        budget_position=BudgetPositionFactory(),
    )
    change2 = ChangeRegistryFactory(
        workflow=WorkflowModelFactory(
            status=const.WORKFLOW_STATUS.PENDING,
            proposal=ProposalMetadataFactory(author=StaffFactory()),
        ),
        budget_position=BudgetPositionFactory(),
        ticket=f'ticket-{random()}',
    )

    localized_authors = [MagicMock(), MagicMock()]
    localize_mock = MagicMock(side_effect=localized_authors)

    # when
    with patch('staff.budget_position.views.front_views.localize', localize_mock):
        result = _blocked_bp_info_by_position_codes([
            change1.budget_position.code,
            change2.budget_position.code,
        ])

    localize_calls_args = [x[0][0] for x in localize_mock.call_args_list]
    localize_author_indices = {}
    for call_index, localize_arg in enumerate(localize_calls_args):
        if localize_arg['login'] == change1.workflow.proposal.author.login:
            current_change = change1
        else:
            current_change = change2

        assert localize_arg == _construct_expected_localize_arg(current_change)
        localize_author_indices[localize_arg['login']] = call_index

    # then
    assert result == {
        change1.budget_position.code: {
            'proposal_id': change1.workflow.proposal.proposal_id,
            'author': localized_authors[localize_author_indices[change1.workflow.proposal.author.login]],
            'tickets': [],
        },
        change2.budget_position.code: {
            'proposal_id': change2.workflow.proposal.proposal_id,
            'author': localized_authors[localize_author_indices[change2.workflow.proposal.author.login]],
            'tickets': [change2.ticket],
        },
    }


def _construct_expected_localize_arg(change: models.ChangeRegistry) -> Dict[str, str]:
    return {
        'login': change.workflow.proposal.author.login,
        'first_name': change.workflow.proposal.author.first_name,
        'last_name': change.workflow.proposal.author.last_name,
        'first_name_en': change.workflow.proposal.author.first_name_en,
        'last_name_en': change.workflow.proposal.author.last_name_en,
    }
