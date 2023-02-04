from datetime import date

import pytest
from unittest.mock import patch, ANY, PropertyMock

from intranet.femida.src.interviews.choices import APPLICATION_RESOLUTIONS, APPLICATION_STATUSES
from intranet.femida.src.offers.choices import OFFER_STATUSES, EMPLOYEE_TYPES, REJECTION_SIDES
from intranet.femida.src.offers.workflow import OfferWorkflow
from intranet.femida.src.startrek.utils import TransitionEnum, StatusEnum
from intranet.femida.src.vacancies.choices import VACANCY_STATUSES

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('status,is_available', (
    (OFFER_STATUSES.on_rotation_approval, True),
    (OFFER_STATUSES.on_approval, False),
))
def test_approve_by_future_team_availability(status, is_available):
    offer = f.OfferFactory(
        employee_type=EMPLOYEE_TYPES.rotation,
        status=status,
    )
    wf = OfferWorkflow(offer, user=None)
    action = wf.get_action('approve_by_future_team')
    assert action.is_available() == is_available


@patch('intranet.femida.src.offers.workflow.OfferAction.change_job_issue_status')
@patch('intranet.femida.src.offers.startrek.serializers.JobIssueFieldsInternalSerializer')
@patch('intranet.femida.src.offers.workflow.OfferCtl.create_link')
@patch('intranet.femida.src.offers.workflow.OfferAction.job_issue', PropertyMock())
def test_approve_by_future_team(mocked_create_link, mocked_serializer, mocked_change_status):
    offer = f.OfferFactory(
        employee_type=EMPLOYEE_TYPES.rotation,
        status=OFFER_STATUSES.on_rotation_approval,
        vacancy__budget_position_id=15000,  # OFFER
    )
    join_at = date(2020, 1, 1)
    wf = OfferWorkflow(offer, user=None)
    wf.perform_action('approve_by_future_team', join_at=join_at)
    offer.refresh_from_db()

    assert offer.status == OFFER_STATUSES.on_approval
    assert offer.join_at == join_at

    assert not mocked_create_link.called
    assert mocked_serializer.called

    mocked_change_status.assert_called_once_with(
        transition=TransitionEnum.agree_offer,
        delay=False,
        comment=ANY,
    )


def test_accept_action():
    offer = f.OfferFactory(
        employee_type=EMPLOYEE_TYPES.new,
        status=OFFER_STATUSES.on_approval,
        vacancy__budget_position_id=15000,  # OFFER
    )
    offer_data = {
        'bank_details': {},
        'full_name': 'Full Name',
        'join_at': date(2020, 1, 1),
        'profile': {},
    }
    wf = OfferWorkflow(offer, user=None)
    wf.perform_action('accept', **offer_data)
    offer.refresh_from_db()

    assert offer.status == OFFER_STATUSES.accepted
    assert offer.vacancy.status == VACANCY_STATUSES.offer_accepted
    assert offer.application.resolution == APPLICATION_RESOLUTIONS.offer_accepted


def test_accept_internal_action():
    employee = f.create_user()
    offer = f.InternalOfferFactory(
        username=employee.username,
        status=OFFER_STATUSES.on_approval,
        vacancy__budget_position_id=15000,  # OFFER
    )
    wf = OfferWorkflow(offer, user=None)
    wf.perform_action('accept_internal')
    offer.refresh_from_db()

    assert offer.status == OFFER_STATUSES.accepted
    assert offer.vacancy.status == VACANCY_STATUSES.offer_accepted
    assert offer.application.resolution == APPLICATION_RESOLUTIONS.rotated


def test_reject_action():
    user = f.create_user_with_perm('recruiting_manager_perm')
    offer = f.OfferFactory(
        employee_type=EMPLOYEE_TYPES.new,
        status=OFFER_STATUSES.sent,
        startrek_salary_key='KEY-1',
        vacancy__budget_position_id=15000,  # OFFER
    )

    rejection_fields = (
        'rejection_reason',
        'competing_offer_conditions',
        'competing_company',
        'comment',
    )
    params = {field: '' for field in rejection_fields}
    params['recruiter'] = f.create_user()

    wf = OfferWorkflow(offer, user=user)
    wf.perform_action(action_name='reject', **params)
    offer.refresh_from_db()

    assert offer.status == OFFER_STATUSES.rejected
    assert offer.vacancy.status == VACANCY_STATUSES.in_progress
    assert offer.application.status == APPLICATION_STATUSES.closed
    assert offer.application.resolution == APPLICATION_RESOLUTIONS.offer_rejected
    assert offer.rejection.rejection_side == REJECTION_SIDES.candidate


@pytest.mark.parametrize('status, is_issue_updated', (
    (OFFER_STATUSES.ready_for_approval, False),
    (OFFER_STATUSES.on_approval, True),
))
@patch(
    target='intranet.femida.src.offers.workflow.OfferAction.startrek_job_status',
    new=StatusEnum.in_progress,
)
@patch('intranet.femida.src.notifications.offers.OfferDeletedNotification.send')
@patch('intranet.femida.src.offers.workflow.add_issue_comment_task.delay')
@patch('intranet.femida.src.offers.workflow.IssueUpdateOperation.delay')
def test_delete_action(mocked_issue_update, mocked_comment, mocked_notification,
                       status, is_issue_updated):
    user = f.create_user_with_perm('recruiting_manager_perm')
    offer = f.OfferFactory(
        employee_type=EMPLOYEE_TYPES.new,
        status=status,
        startrek_salary_key='KEY-1',
        vacancy__budget_position_id=15000,  # OFFER
    )
    wf = OfferWorkflow(offer, user=user)
    wf.perform_action(
        action_name='delete',
        comment='test',
    )
    offer.refresh_from_db()

    assert offer.status == OFFER_STATUSES.deleted
    assert offer.vacancy.status == VACANCY_STATUSES.in_progress
    assert offer.application.status == APPLICATION_STATUSES.closed
    assert offer.application.resolution == APPLICATION_RESOLUTIONS.no_offer
    assert offer.rejection.rejection_side == REJECTION_SIDES.recruiter
    assert offer.rejection.comment == 'test'

    mocked_comment.assert_called_once_with(offer.startrek_salary_key, ANY)
    assert mocked_notification.called
    assert mocked_issue_update.called == is_issue_updated
