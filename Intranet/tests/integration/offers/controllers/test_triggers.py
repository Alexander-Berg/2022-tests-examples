from datetime import date

import pytest
from django.core.exceptions import ValidationError
from unittest.mock import Mock, patch

from intranet.femida.src.core.workflow import WorkflowError
from intranet.femida.src.oebs.api import BudgetPositionError
from intranet.femida.src.offers.choices import OFFER_STATUSES, EMPLOYEE_TYPES
from intranet.femida.src.offers.controllers import (
    offer_confirm_by_current_team,
    offer_confirm_by_issue,
)
from intranet.femida.src.startrek.utils import ApprovementStatusEnum, StatusEnum

from intranet.femida.tests import factories as f
from intranet.femida.tests.utils import assert_not_raises

pytestmark = pytest.mark.django_db


@patch('intranet.femida.src.offers.workflow.ApproveByFutureTeamAction.perform')
def test_confirm_by_current_team_success(mocked_action):
    offer = f.InternalOfferFactory(status=OFFER_STATUSES.on_rotation_approval)
    issue = Mock()
    issue.approvementStatus = ApprovementStatusEnum.approved
    issue.staffDate = '2020-01-01'

    offer_confirm_by_current_team(offer, issue)
    assert mocked_action.called
    mocked_action.assert_called_once_with(join_at=date(2020, 1, 1))


@patch('intranet.femida.src.offers.workflow.ApproveByFutureTeamAction.perform')
def test_confirm_by_current_team_failure(mocked_action):
    offer = f.InternalOfferFactory(status=OFFER_STATUSES.on_rotation_approval)
    issue = Mock()
    issue.approvementStatus = ApprovementStatusEnum.declined
    issue.staffDate = '2020-01-01'

    with pytest.raises(ValidationError):
        offer_confirm_by_current_team(offer, issue)

    assert not mocked_action.called


@pytest.mark.parametrize('error', (
    BudgetPositionError('budget_position_error'),
    WorkflowError('budget_position_invalid_status'),
    WorkflowError('startrek_transition_failed'),
))
@patch('intranet.femida.src.offers.controllers._notify_analyst')
@patch('intranet.femida.src.offers.workflow.ApproveByFutureTeamAction.perform')
def test_confirm_by_current_team_notify_analyst(mocked_perform, mocked_notify_analyst, error):
    offer = f.InternalOfferFactory(status=OFFER_STATUSES.on_rotation_approval)
    issue = Mock()
    issue.approvementStatus = ApprovementStatusEnum.approved
    issue.staffDate = '2020-01-01'

    mocked_perform.side_effect = error
    with assert_not_raises():
        offer_confirm_by_current_team(offer, issue)

    offer.refresh_from_db()
    assert offer.status == OFFER_STATUSES.on_rotation_approval
    assert mocked_notify_analyst.called


@patch('intranet.femida.src.offers.controllers._check_bp_and_get_errors', lambda x: None)
@patch('intranet.femida.src.offers.workflow.OfferWorkflow.perform_action')
@pytest.mark.parametrize('employee_type, action_performed', (
    (EMPLOYEE_TYPES.new, False),
    (EMPLOYEE_TYPES.intern, True),
))
def test_offer_confirm_by_issue(mocked_action, employee_type, action_performed):
    offer = f.OfferFactory(employee_type=employee_type)
    issue = Mock()
    issue.status.key = StatusEnum.resolved

    offer_confirm_by_issue(offer, issue)
    assert mocked_action.called == action_performed

    if action_performed:
        mocked_action.assert_called_once_with('accept_internal')
