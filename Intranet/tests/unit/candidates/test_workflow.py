import uuid

import pytest
from unittest.mock import patch

from intranet.femida.src.candidates.choices import VERIFICATION_STATUSES
from intranet.femida.src.candidates.models import Consideration
from intranet.femida.src.utils.datetime import shifted_now

from intranet.femida.src.core.workflow import WorkflowError
from intranet.femida.src.candidates.workflow import CandidateWorkflow
from intranet.femida.src.offers.choices import OFFER_STATUSES

from intranet.femida.tests import factories as f
from intranet.femida.tests.utils import assert_not_raises


pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('offer_status,raises', (
    (OFFER_STATUSES.closed, False),
    (OFFER_STATUSES.on_approval, True),
))
def test_candidate_close_with_existing_offer(offer_status, raises):
    candidate = f.create_candidate_with_consideration()
    recruiter = f.create_recruiter()
    f.OfferFactory.create(
        status=offer_status,
        candidate=candidate,
    )
    workflow = CandidateWorkflow(candidate, recruiter)
    action = workflow.get_action('close')

    params = {
        'application_resolution': 'resolution',
        'consideration_resolution': 'resolution',
    }

    if raises:
        ctx_mgr = pytest.raises(WorkflowError, match=r'^candidate_has_active_offers$')
    else:
        ctx_mgr = assert_not_raises(WorkflowError)

    with ctx_mgr:
        action.perform(**params)


@patch('intranet.femida.src.candidates.workflow.send_interview_survey_task.delay')
def test_survey_not_sent_for_hired(mocked_send_survey):
    candidate = f.create_candidate_with_consideration()
    recruiter = f.create_recruiter()
    f.OfferFactory.create(
        status=OFFER_STATUSES.closed,
        newhire_id=1,
        candidate=candidate,
    )
    workflow = CandidateWorkflow(candidate, recruiter)
    action = workflow.get_action('close')

    params = {
        'is_hired': True,
        'application_resolution': 'resolution',
        'consideration_resolution': 'resolution',
    }

    action.perform(**params)
    assert not mocked_send_survey.called


@pytest.mark.parametrize('was_offer_accepted_in_past', (True, False))
@patch('intranet.femida.src.candidates.workflow.send_interview_survey_task.delay')
def test_survey_send_on_closing_not_hired(mocked_send_survey, was_offer_accepted_in_past):
    candidate = f.CandidateFactory()
    application = f.create_application(
        candidate=candidate,
        consideration__state=Consideration.STATES.in_progress,
    )
    recruiter = f.create_recruiter()
    f.OfferFactory.create(
        status=OFFER_STATUSES.rejected,
        newhire_id=1 if was_offer_accepted_in_past else None,
        candidate=candidate,
        application=application,
    )
    workflow = CandidateWorkflow(candidate, recruiter)
    action = workflow.get_action('close')

    params = {
        'application_resolution': 'resolution',
        'consideration_resolution': 'resolution',
    }

    action.perform(**params)
    should_send_survey = not was_offer_accepted_in_past  # был ли ранее отправлен опрос
    assert mocked_send_survey.called == should_send_survey


def test_create_verification_is_available_no_verification():
    """
    Экшн создания проверки на КИ доступен, потому что нет существующих проверок
    """
    candidate = f.CandidateFactory()
    application = f.ApplicationFactory(candidate=candidate)
    recruiter = f.create_recruiter()
    workflow = CandidateWorkflow(
        instance=candidate,
        user=recruiter,
        consideration=application.consideration,
    )
    action = workflow.get_action('create_verification')
    assert action.is_available()


@pytest.mark.parametrize('date_field, status', (
    ('expiration_date', VERIFICATION_STATUSES.closed),
    ('link_expiration_date', VERIFICATION_STATUSES.new),
))
@pytest.mark.parametrize('date_value, result', (
    (shifted_now(days=-1), True),
    (shifted_now(days=1), False),
))
def test_create_verification_is_available_expired(date_field, status, date_value, result):
    """
    Экшн создания проверки на КИ доступен,
    потому что предыдущие проверки истекли и наоборот
    """
    verification = f.VerificationFactory(
        status=status,
        **{date_field: date_value}
    )
    recruiter = f.create_recruiter()
    workflow = CandidateWorkflow(
        instance=verification.candidate,
        user=recruiter,
        consideration=verification.application.consideration,
    )
    action = workflow.get_action('create_verification')
    assert action.is_available() is result


@pytest.mark.parametrize('status', (
    VERIFICATION_STATUSES.on_check,
    VERIFICATION_STATUSES.on_ess_check,
))
def test_create_verification_is_unavailable_on_check(status):
    """
    Экшн создания проверки на КИ недоступен,
    потому что проверка в процессе
    """
    verification = f.VerificationFactory(
        status=status,
        link_expiration_date=shifted_now(days=-1),
    )
    recruiter = f.create_recruiter()
    workflow = CandidateWorkflow(
        instance=verification.candidate,
        user=recruiter,
        consideration=verification.application.consideration,
    )
    action = workflow.get_action('create_verification')
    assert not action.is_available()


@patch('intranet.femida.src.candidates.workflow.send_verification_form_to_candidate')
def test_create_verification(mocked_send):
    candidate = f.CandidateFactory()
    application = f.ApplicationFactory(candidate=candidate)
    recruiter = f.create_recruiter()
    workflow = CandidateWorkflow(
        instance=candidate,
        user=recruiter,
        consideration=application.consideration,
    )
    _uuid = uuid.uuid4().hex
    verification = workflow.perform_action(
        action_name='create_verification',
        receiver='candidate@ya.ru',
        subject='subject',
        text='text',
        application=application,
        uuid=_uuid,
    )
    assert verification.created_by == recruiter
    assert verification.uuid == _uuid
    assert verification.candidate == candidate
    assert verification.application == application
    assert mocked_send.called


@pytest.mark.parametrize('user_perm, result', (
    ('recruiter_perm', True),
    ('recruiter_assessor_perm', True),
    (None, False),
))
@pytest.mark.parametrize('action', (
    'note_create',
    'outcoming_message_create',
    'interview_create',
    'interview_round_create',
))
def test_recruiter_assessor_create_permissions(user_perm, result, action):
    user = f.create_user_with_perm(user_perm)
    workflow = CandidateWorkflow(
        instance=f.CandidateFactory(),
        user=user,
    )
    assert workflow.get_actions_visibility().get(action) is result
