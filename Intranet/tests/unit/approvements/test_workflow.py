import pytest

from unittest.mock import patch

from ok.approvements.choices import APPROVEMENT_STATUSES, APPROVEMENT_STAGE_STATUSES
from ok.approvements.workflow import ApprovementWorkflow
from ok.core.workflow import WorkflowError

from tests import factories as f


pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('user,status,stage_status,is_available', (
    ('author', APPROVEMENT_STATUSES.in_progress, APPROVEMENT_STAGE_STATUSES.pending, True),
    ('author', APPROVEMENT_STATUSES.closed, APPROVEMENT_STAGE_STATUSES.pending, False),
    ('approver', APPROVEMENT_STATUSES.in_progress, APPROVEMENT_STAGE_STATUSES.pending, True),
    ('approver', APPROVEMENT_STATUSES.in_progress, APPROVEMENT_STAGE_STATUSES.approved, False),
    ('approver', APPROVEMENT_STATUSES.in_progress, APPROVEMENT_STAGE_STATUSES.cancelled, False),
    ('approver', APPROVEMENT_STATUSES.closed, APPROVEMENT_STAGE_STATUSES.pending, False),
    ('unknown', APPROVEMENT_STATUSES.in_progress, APPROVEMENT_STAGE_STATUSES.pending, False),
))
def test_approve_action_availability(user, status, stage_status, is_available):
    approvement = f.ApprovementFactory(
        author='author',
        status=status,
    )
    f.ApprovementStageFactory(
        approvement=approvement,
        approver='approver',
        status=stage_status,
    )

    wf = ApprovementWorkflow(approvement, user)
    action = wf.get_action('approve')
    assert action.is_available() == is_available


@patch('ok.approvements.workflow.ApprovementController.approve')
def test_approve_by_approver(mocked_approve):
    approvement = f.create_approvement()
    current_stage = approvement.stages.first()

    wf = ApprovementWorkflow(approvement, current_stage.approver)
    action = wf.get_action('approve')
    action.perform(approver=current_stage.approver)

    mocked_approve.assert_called_once_with(
        [current_stage],
        current_stage.approver,
        approvement_source='',
    )


@patch('ok.approvements.workflow.ApprovementController.approve')
def test_approve_by_wrong_approver(mocked_approve):
    """
    Если к нам пришёл простой согласующий и пытается согласовать не свою стадию,
    мы игнорируем это и согласовываем его стадию
    """
    current_approver = 'current_approver'
    wrong_approver = 'wrong_approver'
    approvement = f.create_approvement(approvers=[current_approver, wrong_approver])

    wf = ApprovementWorkflow(approvement, wrong_approver)
    action = wf.get_action('approve')
    action.perform(approver=current_approver)

    mocked_approve.assert_called_once_with(
        [approvement.stages.get(approver=wrong_approver)],
        wrong_approver,
        approvement_source='',
    )


@patch('ok.approvements.workflow.ApprovementController.approve')
def test_approve_by_author(mocked_approve):
    approvement = f.create_approvement()
    current_stage = approvement.stages.first()

    wf = ApprovementWorkflow(approvement, approvement.author)
    action = wf.get_action('approve')
    action.perform(approver=current_stage.approver)

    mocked_approve.assert_called_once_with(
        [current_stage],
        approvement.author,
        approvement_source='',
    )


@patch('ok.approvements.workflow.ApprovementController.approve')
@patch('ok.approvements.workflow.get_staff_group_member_logins', lambda x: {'group_url_member'})
def test_approve_by_group_member(mocked_approve):
    responsible = 'group_url_member'
    approvement = f.create_approvement(groups=['group_url'])
    current_stage = approvement.stages.first()

    wf = ApprovementWorkflow(approvement, responsible)
    action = wf.get_action('approve')
    action.perform(approver=current_stage.approver)

    mocked_approve.assert_called_once_with([current_stage], responsible, approvement_source='')


def test_approve_by_author_already_approved():
    approvement = f.ApprovementFactory()
    stage = f.ApprovementStageFactory(
        approvement=approvement,
        is_approved=True,
    )

    wf = ApprovementWorkflow(approvement, approvement.author)
    action = wf.get_action('approve')

    with pytest.raises(WorkflowError):
        action.perform(approver=stage.approver)


@pytest.mark.parametrize('user,status,is_available', (
    ('author', APPROVEMENT_STATUSES.in_progress, True),
    ('author', APPROVEMENT_STATUSES.suspended, True),
    ('author', APPROVEMENT_STATUSES.rejected, True),
    ('author', APPROVEMENT_STATUSES.closed, False),
    ('approver', APPROVEMENT_STATUSES.in_progress, False),
    ('unknown', APPROVEMENT_STATUSES.in_progress, False),
    ('approver', APPROVEMENT_STATUSES.suspended, False),
    ('unknown', APPROVEMENT_STATUSES.suspended, False),
    ('approver', APPROVEMENT_STATUSES.rejected, False),
    ('unknown', APPROVEMENT_STATUSES.rejected, False),
))
def test_close_action_availability(user, status, is_available):
    approvement = f.ApprovementFactory(
        author='author',
        status=status,
    )
    f.ApprovementStageFactory(
        approvement=approvement,
        approver='approver',
    )

    wf = ApprovementWorkflow(approvement, user)
    action = wf.get_action('close')
    assert action.is_available() == is_available


@patch('ok.approvements.workflow.ApprovementController.close')
def test_close_action(mocked_close):
    approvement = f.create_approvement()

    wf = ApprovementWorkflow(approvement, approvement.author)
    action = wf.get_action('close')
    action.perform()

    mocked_close.assert_called_once_with(approvement.author)


@pytest.mark.parametrize('user,status,is_available', (
    ('author', APPROVEMENT_STATUSES.in_progress, True),
    ('author', APPROVEMENT_STATUSES.closed, False),
    ('author', APPROVEMENT_STATUSES.suspended, False),
    ('author', APPROVEMENT_STATUSES.rejected, False),
    ('approver', APPROVEMENT_STATUSES.in_progress, False),
    ('unknown', APPROVEMENT_STATUSES.in_progress, False),
))
def test_suspend_action_availability(user, status, is_available):
    approvement = f.ApprovementFactory(
        author='author',
        status=status,
    )
    f.ApprovementStageFactory(
        approvement=approvement,
        approver='approver',
    )

    wf = ApprovementWorkflow(approvement, user)
    action = wf.get_action('suspend')
    assert action.is_available() == is_available


@patch('ok.approvements.workflow.ApprovementController.suspend')
def test_suspend_action(mocked_suspend):
    approvement = f.create_approvement(status=APPROVEMENT_STATUSES.in_progress)

    wf = ApprovementWorkflow(approvement, approvement.author)
    action = wf.get_action('suspend')
    action.perform()

    mocked_suspend.assert_called_once_with(approvement.author)


@pytest.mark.parametrize('user,status,is_reject_allowed,is_available', (
    ('approver', APPROVEMENT_STATUSES.in_progress, True, True),
    ('approver', APPROVEMENT_STATUSES.in_progress, False, False),
    ('approver', APPROVEMENT_STATUSES.rejected, True, False),
    ('approver', APPROVEMENT_STATUSES.closed, True, False),
    ('approver', APPROVEMENT_STATUSES.suspended, True, False),
    ('author', APPROVEMENT_STATUSES.in_progress, True, False),
    ('unknown', APPROVEMENT_STATUSES.in_progress, True, False),
))
def test_reject_action_availability(user, status, is_reject_allowed, is_available):
    approvement = f.ApprovementFactory(
        author='author',
        status=status,
        is_reject_allowed=is_reject_allowed,
    )
    f.ApprovementStageFactory(
        approvement=approvement,
        approver='approver',
    )

    wf = ApprovementWorkflow(approvement, user)
    action = wf.get_action('reject')
    assert action.is_available() == is_available


@patch('ok.approvements.workflow.ApprovementController.reject')
def test_reject_action(mocked_reject):
    approver = 'approver'
    approvement = f.ApprovementFactory(status=APPROVEMENT_STATUSES.in_progress)
    stage = f.ApprovementStageFactory(
        approvement=approvement,
        approver=approver,
    )

    wf = ApprovementWorkflow(approvement, approver)
    action = wf.get_action('reject')
    action.perform()

    mocked_reject.assert_called_once_with(stage, approver, None)


@pytest.mark.parametrize('user,status,is_available', (
    ('author', APPROVEMENT_STATUSES.suspended, True),
    ('author', APPROVEMENT_STATUSES.rejected, True),
    ('author', APPROVEMENT_STATUSES.closed, False),
    ('author', APPROVEMENT_STATUSES.in_progress, False),
    ('approver', APPROVEMENT_STATUSES.suspended, False),
    ('approver', APPROVEMENT_STATUSES.rejected, False),
    ('unknown', APPROVEMENT_STATUSES.suspended, False),
    ('unknown', APPROVEMENT_STATUSES.rejected, False),
))
def test_resume_action_availability(user, status, is_available):
    approvement = f.ApprovementFactory(
        author='author',
        status=status,
    )
    f.ApprovementStageFactory(
        approvement=approvement,
        approver='approver',
    )

    wf = ApprovementWorkflow(approvement, user)
    action = wf.get_action('resume')
    assert action.is_available() == is_available


@pytest.mark.parametrize('status', (
    APPROVEMENT_STATUSES.suspended,
    APPROVEMENT_STATUSES.rejected,
))
@patch('ok.approvements.workflow.ApprovementController.resume')
def test_resume_action(mocked_resume, status):
    approvement = f.create_approvement(status=status)

    wf = ApprovementWorkflow(approvement, approvement.author)
    action = wf.get_action('resume')
    action.perform()

    mocked_resume.assert_called_once_with(approvement.author)
