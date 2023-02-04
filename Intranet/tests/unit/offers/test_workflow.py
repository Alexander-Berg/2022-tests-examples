import pytest

from unittest.mock import patch, Mock

from intranet.femida.src.candidates.choices import VERIFICATION_STATUSES, VERIFICATION_RESOLUTIONS
from intranet.femida.src.core.workflow import WorkflowError
from intranet.femida.src.interviews.choices import INTERVIEW_STATES
from intranet.femida.src.offers.choices import INTERNAL_EMPLOYEE_TYPES, OFFER_STATUSES
from intranet.femida.src.offers.workflow import OfferAction, OfferWorkflow
from intranet.femida.src.startrek.utils import TransitionDoesNotExist, StartrekError
from intranet.femida.src.utils.datetime import shifted_now

from intranet.femida.tests import factories as f
from intranet.femida.tests.utils import assert_not_raises, enable_job_issue_workflow


pytestmark = pytest.mark.django_db


_transition = 'intranet.femida.src.offers.workflow.IssueTransitionOperation.__call__'


@pytest.fixture
def offer_action():
    offer = f.OfferFactory()
    user = f.create_user()
    wf = OfferWorkflow(offer, user)
    return OfferAction(wf=wf)


@enable_job_issue_workflow
@patch('intranet.femida.src.offers.workflow.irreversible_checkpoints', Mock())
@patch(_transition, Mock())
def test_job_transition_succeeds(offer_action):
    """
    Успешный переход в JOB-тикете
    """
    with assert_not_raises(WorkflowError):
        offer_action.change_job_issue_status('transition')


@enable_job_issue_workflow
@patch('intranet.femida.src.offers.workflow.irreversible_checkpoints', Mock())
@patch(_transition, Mock(side_effect=TransitionDoesNotExist))
def test_job_transition_does_not_exist(offer_action):
    """
    Если переход в JOB-тикете не существует,
    Фемида тоже должна выбрасывать ошибку
    """
    with pytest.raises(WorkflowError):
        offer_action.change_job_issue_status('transition')


@enable_job_issue_workflow
@patch('intranet.femida.src.offers.workflow.irreversible_checkpoints', Mock())
@patch(_transition, Mock(side_effect=StartrekError))
@patch('intranet.femida.src.offers.workflow.IssueTransitionOperation.delay')
def test_job_transition_is_delayed(mocked_delay, offer_action):
    """
    Если переход существует, но возникла другая ошибка,
    пытаемся сделать переход отложено
    """
    offer_action.change_job_issue_status('transition')
    mocked_delay.assert_called()


@pytest.mark.parametrize('status, resolution, expiration_date, raises', (
    # нет Verification
    ('', '', None, True),
    # Verification in progress
    (VERIFICATION_STATUSES.new, '', None, True),
    (VERIFICATION_STATUSES.on_check, '', None, True),
    (VERIFICATION_STATUSES.on_ess_check, '', None, True),
    # Verification устарела
    (VERIFICATION_STATUSES.closed, VERIFICATION_RESOLUTIONS.hire, shifted_now(days=-1), True),
    # Резолюции
    (VERIFICATION_STATUSES.closed, VERIFICATION_RESOLUTIONS.nohire, shifted_now(days=1), True),
    (VERIFICATION_STATUSES.closed, VERIFICATION_RESOLUTIONS.blacklist, shifted_now(days=1), True),
    (VERIFICATION_STATUSES.closed, VERIFICATION_RESOLUTIONS.hire, shifted_now(days=1), False),
))
def test_validate_verification_status(status, resolution, expiration_date, raises):
    offer = f.create_offer(
        status=OFFER_STATUSES.on_approval,
        budget_position_id=15000,  # OFFER
    )
    if expiration_date:
        f.VerificationFactory(
            candidate=offer.candidate,
            application=offer.application,
            expiration_date=expiration_date,
            status=status,
            resolution=resolution,
        )
    workflow = OfferWorkflow(instance=offer, user=None)
    action = workflow.get_action('send')
    ctx_mgr = (
        pytest.raises(WorkflowError, match=r'verification')
        if raises
        else assert_not_raises(WorkflowError)
    )
    with ctx_mgr:
        action._validate_verification_status()


@pytest.mark.parametrize('employee_type', INTERNAL_EMPLOYEE_TYPES._db_values)
def test_verification_not_required_for_internal_offer(employee_type):
    offer = f.create_offer(
        status=OFFER_STATUSES.on_approval,
        budget_position_id=15000,  # OFFER
        employee_type=employee_type,
    )
    workflow = OfferWorkflow(instance=offer, user=None)
    action = workflow.get_action('send')

    with assert_not_raises(WorkflowError):
        action._validate_verification_status()


@pytest.mark.parametrize('interview_status, raises', (
    (INTERVIEW_STATES.draft, True),
    (INTERVIEW_STATES.assigned, True),
    (INTERVIEW_STATES.estimated, True),
    (INTERVIEW_STATES.finished, False),
    (INTERVIEW_STATES.cancelled, False),
))
def test_approve_action_unfinished_interviews(interview_status, raises):
    offer = f.create_offer(status=OFFER_STATUSES.ready_for_approval)
    f.create_interview(
        state=interview_status,
        candidate=offer.candidate,
        application=offer.application,
    )
    workflow = OfferWorkflow(instance=offer, user=None)
    action = workflow.get_action('approve')

    if raises:
        ctx_mgr = pytest.raises(WorkflowError, match=r'^candidate_has_unfinished_interviews$')
    else:
        ctx_mgr = assert_not_raises(WorkflowError)

    with ctx_mgr:
        action._validate_interviews()
