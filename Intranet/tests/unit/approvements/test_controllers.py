import pytest

from datetime import timedelta
from itertools import chain
from unittest.mock import patch, Mock

from django.test import override_settings
from django.utils import timezone

from ok.approvements.choices import (
    APPROVEMENT_TYPES,
    APPROVEMENT_STATUSES,
    APPROVEMENT_RESOLUTIONS,
    APPROVEMENT_HISTORY_EVENTS,
    APPROVEMENT_STAGE_STATUSES
)
from ok.approvements.controllers import (
    ApprovementController,
    ExceededApproversLimit,
    _get_active_approvements_with_current_stages_only,
    _get_active_approver_to_approvement_stages_map,
    _get_receiver_to_overdue_approvement_map,
)
from ok.approvements.tracker import IssueFieldEnum
from ok.utils.lock import CantTakeLockException, lock_manager

from tests import factories as f
from tests.utils.assertions import assert_history_exists
from tests.utils.mock import AnyOrderList


pytestmark = pytest.mark.django_db


yesterday = timezone.now() - timedelta(days=1)


@patch('ok.approvements.controllers.sync_group_memberships_task.delay')
@patch('ok.approvements.controllers.set_approvement_tracker_comment_id_task.si')
@patch('ok.approvements.controllers.update_approvement_issue_task.delay')
@patch('ok.approvements.controllers.ping_approvers_task.si')
@pytest.mark.parametrize('is_parallel', (True, False))
@pytest.mark.parametrize('need_approvals', (1, 2))
@pytest.mark.parametrize('type', (APPROVEMENT_TYPES.tracker, APPROVEMENT_TYPES.general))
def test_approvement_create(mocked_ping_approvers, mocked_update_issue,
                            mocked_set_comment_id, mocked_sync_group_memberships,
                            is_parallel, need_approvals, type):
    groups = ['svc_ok', 'outstaff']
    child_stages = [
        {'approver': 'tmalikova'},
        {'approver': 'agrml'},
    ]
    parent_stages = [
        {'approver': 'qazaq'},
        {'stages': child_stages, 'need_approvals': need_approvals},
        {'approver': 'kiparis'},
    ]
    queue = 'ISSUE'
    data = {
        'text': 'Approvement description',
        'type': type,
        'object_id': f'{queue}-1',
        'is_parallel': is_parallel,
        'stages': parent_stages,
        'groups': groups,
        'create_comment': False,
    }
    initiator = 'initiator'
    approvement = ApprovementController.create(data, initiator)

    approvers = list(approvement.stages.root().values_list('approver', flat=True))
    assert approvers == [p.get('approver', '') for p in parent_stages]

    complex_stage = approvement.stages.filter(stages__isnull=False).first()
    assert complex_stage.need_approvals == need_approvals

    child_approvers = list(complex_stage.stages.values_list('approver', flat=True))
    assert child_approvers == [p.get('approver', '') for p in child_stages]

    assert approvement.groups == groups
    assert approvement.approvement_groups.filter(group__in=groups).count() == len(groups)

    if approvement.is_tracker_approvement:
        assert approvement.tracker_queue.name == queue
        mocked_update_issue.assert_called_once_with(
            approvement_id=approvement.id,
            fields=[
                IssueFieldEnum.APPROVEMENT_STATUS,
                IssueFieldEnum.ACCESS,
                IssueFieldEnum.APPROVERS,
                IssueFieldEnum.CURRENT_APPROVERS,
            ],
        )
        mocked_set_comment_id.assert_called_once_with(approvement.id)
    else:
        mocked_update_issue.assert_not_called()
        mocked_set_comment_id.assert_not_called()
    mocked_ping_approvers.assert_called_once_with(approvement.id, initiator)
    mocked_sync_group_memberships.assert_called_once_with(AnyOrderList(groups))


def test_approvement_create_normalize():
    child_stages = [
        {'approver': 'tmalikova'},
        {'approver': ''},
    ]
    parent_stages = [
        {'approver': ''},
        {'approver': 'qazaq'},
        {'stages': child_stages},
        {'stages': [{'approver': ''}, {'approver': ''}]},
        {'approver': 'kiparis'},
    ]
    queue = 'ISSUE'
    data = {
        'text': 'Approvement description',
        'type': APPROVEMENT_TYPES.tracker,
        'object_id': f'{queue}-1',
        'is_parallel': False,
        'stages': parent_stages,
        'create_comment': False,
    }
    initiator = 'initiator'
    approvement = ApprovementController.create(data, initiator)

    approvers = list(approvement.stages.root().values_list('approver', flat=True))
    assert set(approvers) == {'tmalikova', 'qazaq', 'kiparis'}


@override_settings(APPROVEMENT_STAGES_LIMIT=2)
def test_approvement_create_with_approvers_limit():
    data = {
        'text': 'Approvement description',
        'type': APPROVEMENT_TYPES.tracker,
        'object_id': 'Q-1',
        'is_parallel': False,
        'stages': [
            {'approver': 'user1'},
            {'stages': [{'approver': 'user2'}, {'approver': 'user3'}]},
        ],
        'create_comment': False,
    }
    initiator = 'initiator'
    with pytest.raises(ExceededApproversLimit):
        ApprovementController.create(data, initiator)


@patch('ok.approvements.controllers.get_issue_author', Mock(return_value='author'))
@patch('ok.notifications.approvements.collect_issues_for_approvements', Mock())
@patch('ok.notifications.approvements.StaffUser.fetch', lambda x: {})
@patch('ok.notifications.base.get_user_to_language', lambda x: {})
@pytest.mark.parametrize('is_auto_approving', (True, False))
@pytest.mark.parametrize('is_parallel', (True, False))
@pytest.mark.parametrize('approver', ('author', 'not_author'))
@pytest.mark.parametrize('type', (APPROVEMENT_TYPES.tracker, APPROVEMENT_TYPES.general))
def test_approvement_create_and_auto_approve_create(is_auto_approving, approver, type, is_parallel):

    parent_stages = [
        {'approver': approver},
    ]
    queue = 'ISSUE'
    data = {
        'text': 'Approvement description',
        'type': type,
        'object_id': f'{queue}-1',
        'is_parallel': is_parallel,
        'stages': parent_stages,
        'is_auto_approving': is_auto_approving,
        'create_comment': False,
    }
    initiator = 'initiator'
    approvement = ApprovementController.create(data, initiator)

    approvers = list(approvement.stages.root().values_list('approver', flat=True))
    assert approvers == [p.get('approver', '') for p in parent_stages]

    ctl = ApprovementController(approvement)
    approvement = ctl.perform_auto_approve()
    ctl.on_after_approve('initiator', False)

    if approver == 'author' and is_auto_approving and type == APPROVEMENT_TYPES.tracker:
        assert approvement.status == APPROVEMENT_STATUSES.closed
    else:
        assert approvement.status == APPROVEMENT_STATUSES.in_progress


@patch('ok.approvements.controllers.get_issue_author', Mock(return_value='author'))
@patch('ok.notifications.approvements.collect_issues_for_approvements', Mock())
@patch('ok.notifications.approvements.StaffUser.fetch', lambda x: {})
@patch('ok.notifications.base.get_user_to_language', lambda x: {})
@patch('ok.approvements.controllers.ApprovementController.get_data_from_flow', Mock(return_value={
    'data': {'stages': [{'approver': 'a'}], 'is_auto_approving': True}}))
def test_approvement_create_and_auto_approve_create_with_flow():
    queue = 'ISSUE'
    data = {
        'text': 'Approvement description',
        'object_id': f'{queue}-1',
        'flow_name': 'some_wf',
        'create_comment': False,
    }
    initiator = 'initiator'
    approvement = ApprovementController.create(data, initiator)

    assert approvement.is_auto_approving is True


@patch('ok.approvements.controllers.get_issue_author', Mock(return_value='author'))
@patch('ok.notifications.approvements.collect_issues_for_approvements', Mock())
@patch('ok.notifications.approvements.StaffUser.fetch', lambda x: {})
@patch('ok.notifications.base.get_user_to_language', lambda x: {})
@pytest.mark.parametrize('is_auto_approving', (True, False))
@pytest.mark.parametrize('approver', ('author', 'not_author'))
@pytest.mark.parametrize('type', (APPROVEMENT_TYPES.tracker, APPROVEMENT_TYPES.general))
def test_approvement_create_and_auto_approve(is_auto_approving, approver, type):
    not_ticket_author = 'not a ticket author'
    parent_stages = [
        {'approver': not_ticket_author},
        {'approver': approver},
    ]
    queue = 'ISSUE'
    data = {
        'text': 'Approvement description',
        'type': type,
        'object_id': f'{queue}-1',
        'is_parallel': False,
        'stages': parent_stages,
        'is_auto_approving': is_auto_approving,
        'create_comment': False,
    }
    initiator = 'initiator'
    approvement = ApprovementController.create(data, initiator)

    approvers = list(approvement.stages.root().values_list('approver', flat=True))
    assert approvers == [p.get('approver', '') for p in parent_stages]

    approvement = ApprovementController(approvement).approve(
        stages=[approvement.stages.first()],
        initiator=not_ticket_author,
    )

    if approver == 'author' and is_auto_approving and type == APPROVEMENT_TYPES.tracker:
        assert approvement.status == APPROVEMENT_STATUSES.closed
    else:
        assert approvement.status == APPROVEMENT_STATUSES.in_progress


@patch('ok.approvements.controllers.get_issue_author', Mock(return_value='author'))
@patch('ok.notifications.approvements.collect_issues_for_approvements', Mock())
@patch('ok.notifications.approvements.StaffUser.fetch', lambda x: {})
@patch('ok.notifications.base.get_user_to_language', lambda x: {})
@patch('ok.approvements.controllers.ApprovementController.get_data_from_flow', Mock(return_value={
    'data': {'stages': [], 'approve_if_no_approvers': True},
    'detail': {'error': None}}))
def test_approvement_create_and_auto_approve_create_with_flow():
    queue = 'ISSUE'
    data = {
        'text': 'Approvement description',
        'object_id': f'{queue}-1',
        'flow_name': 'some_wf',
        'create_comment': False,
    }
    initiator = 'initiator'
    approvement = ApprovementController.create(data, initiator)

    assert approvement.status == APPROVEMENT_STATUSES.closed
    assert approvement.resolution == APPROVEMENT_RESOLUTIONS.approved


@patch('ok.approvements.controllers.ApprovementRequiredNotification')
def test_approve_by_approver(mocked_notification):
    approvement = f.create_approvement(2)
    current_stage = approvement.stages.first()
    next_stage = approvement.stages.last()

    ctl = ApprovementController(approvement)
    ctl.approve([current_stage], current_stage.approver)
    next_stage.refresh_from_db()

    assert approvement.status == APPROVEMENT_STATUSES.in_progress
    assert current_stage.is_approved
    assert current_stage.approved_by == current_stage.approver
    assert next_stage.status == APPROVEMENT_STAGE_STATUSES.current

    assert_history_exists(
        obj=current_stage,
        event=APPROVEMENT_HISTORY_EVENTS.status_changed,
        status=APPROVEMENT_STAGE_STATUSES.approved,
    )
    assert_history_exists(
        obj=next_stage,
        event=APPROVEMENT_HISTORY_EVENTS.status_changed,
        status=APPROVEMENT_STAGE_STATUSES.current,
    )
    mocked_notification.assert_called_once_with(
        instance=approvement,
        initiator=approvement.author,
        current_stages=[next_stage],
    )
    assert_history_exists(next_stage, APPROVEMENT_HISTORY_EVENTS.ping_sent)


@patch('ok.approvements.controllers.ApprovementApprovedByResponsibleNotification')
@patch('ok.notifications.approvements.collect_issues_for_approvements', Mock())
@patch('ok.notifications.approvements.StaffUser.fetch', lambda x: {})
@patch('ok.notifications.base.get_user_to_language', lambda x: {})
def test_approve_by_author(mocked_notification):
    approvement = f.create_approvement(2)
    current_stage = approvement.stages.first()

    ctl = ApprovementController(approvement)
    ctl.approve([current_stage], approvement.author)

    assert approvement.status == APPROVEMENT_STATUSES.in_progress
    assert current_stage.is_approved
    assert current_stage.approved_by == approvement.author
    assert_history_exists(
        obj=current_stage,
        event=APPROVEMENT_HISTORY_EVENTS.status_changed,
        status=APPROVEMENT_STAGE_STATUSES.approved
    )
    mocked_notification.assert_called_once_with(
        instance=approvement,
        initiator=approvement.author,
        receivers={current_stage.approver},
    )


@patch('ok.approvements.controllers.update_approvement_issue_task.delay')
@patch('ok.approvements.controllers.ApprovementFinishedNotification')
def test_approve_action_by_last_approver(mocked_notification, mocked_update_issue):
    approvement = f.create_approvement()
    current_stage = approvement.stages.first()

    ctl = ApprovementController(approvement)
    ctl.approve([current_stage], current_stage.approver)

    assert approvement.status == APPROVEMENT_STATUSES.closed
    assert approvement.resolution == APPROVEMENT_RESOLUTIONS.approved
    assert current_stage.is_approved
    assert current_stage.approved_by == current_stage.approver
    stage_history = current_stage.history.filter(
        event=APPROVEMENT_HISTORY_EVENTS.status_changed,
        status=APPROVEMENT_STAGE_STATUSES.approved,
    )
    assert stage_history.exists()
    assert_history_exists(
        obj=approvement,
        event=APPROVEMENT_HISTORY_EVENTS.status_changed,
        status=APPROVEMENT_STATUSES.closed,
        resolution=APPROVEMENT_RESOLUTIONS.approved,
    )
    mocked_notification.assert_called_once_with(
        instance=approvement,
        initiator=current_stage.approver,
    )
    mocked_update_issue.assert_called_once_with(
        approvement.id,
        [IssueFieldEnum.CURRENT_APPROVERS, IssueFieldEnum.APPROVEMENT_STATUS],
    )


@patch('ok.approvements.controllers.ApprovementRequiredNotification')
def test_approve_disordered(mocked_notification):
    approvement = f.create_approvement(3)
    s1, s2, stage_to_approve = approvement.stages.all()

    ctl = ApprovementController(approvement)
    ctl.approve([stage_to_approve], stage_to_approve.approver)

    assert approvement.status == APPROVEMENT_STATUSES.in_progress
    assert stage_to_approve.is_approved
    assert stage_to_approve.approved_by == stage_to_approve.approver
    assert_history_exists(
        obj=stage_to_approve,
        event=APPROVEMENT_HISTORY_EVENTS.status_changed,
        status=APPROVEMENT_STAGE_STATUSES.approved,
    )
    mocked_notification.assert_not_called()

    s1.refresh_from_db()
    s2.refresh_from_db()
    assert s1.status == APPROVEMENT_STAGE_STATUSES.current
    assert s2.status == APPROVEMENT_STAGE_STATUSES.pending
    assert stage_to_approve.status == APPROVEMENT_STAGE_STATUSES.approved


@patch('ok.approvements.controllers.ApprovementRequiredNotification')
@patch('ok.notifications.approvements.collect_issues_for_approvements', Mock())
@patch('ok.notifications.approvements.StaffUser.fetch', lambda x: {})
@patch('ok.notifications.base.get_user_to_language', lambda x: {})
def test_approve_parallel_notification(mocked_notification):
    approvement = f.create_approvement(3, is_parallel=True)
    stages = approvement.stages.all()

    ctl = ApprovementController(approvement)
    for stage in stages:
        ctl.approve([stage], stage.approver)

    mocked_notification.assert_not_called()


@patch('ok.notifications.approvements.collect_issues_for_approvements', Mock())
@patch('ok.notifications.approvements.StaffUser.fetch', lambda x: {})
@patch('ok.notifications.base.get_user_to_language', lambda x: {})
def test_approve_cant_take_lock():
    approvement = f.create_approvement(2)

    lock = lock_manager.lock(str(approvement.uuid))
    with lock:
        stages = approvement.stages.all()

        ctl = ApprovementController(approvement)
        with pytest.raises(CantTakeLockException):
            ctl.approve([stages[0]], stages[0].approver)


@patch('ok.approvements.controllers.ApprovementRequiredNotification')
@patch('ok.notifications.approvements.collect_issues_for_approvements', Mock())
@patch('ok.notifications.approvements.StaffUser.fetch', lambda x: {})
@patch('ok.notifications.base.get_user_to_language', lambda x: {})
def test_approve_complex_stage(mocked_notification):
    complex_stage = f.create_parent_stage(need_approvals=1)
    stage_to_approve = f.create_child_stage(complex_stage)
    stage_to_cancel = f.create_child_stage(complex_stage)

    ctl = ApprovementController(complex_stage.approvement)
    ctl.approve([stage_to_approve], stage_to_approve.approver)

    assert stage_to_approve.is_approved
    assert stage_to_approve.status == APPROVEMENT_STAGE_STATUSES.approved
    assert stage_to_approve.approved_by == stage_to_approve.approver

    stage_to_cancel.refresh_from_db()
    assert stage_to_cancel.is_approved is None
    assert stage_to_cancel.status == APPROVEMENT_STAGE_STATUSES.cancelled

    assert complex_stage.is_approved
    assert complex_stage.status == APPROVEMENT_STAGE_STATUSES.approved
    assert complex_stage.approved_by == stage_to_approve.approved_by

    assert complex_stage.approvement.status == APPROVEMENT_STATUSES.closed
    assert complex_stage.approvement.resolution == APPROVEMENT_RESOLUTIONS.approved

    mocked_notification.assert_not_called()
    for stage in [stage_to_approve, complex_stage]:
        assert_history_exists(
            obj=stage,
            event=APPROVEMENT_HISTORY_EVENTS.status_changed,
            status=APPROVEMENT_STAGE_STATUSES.approved,
        )
    assert_history_exists(
        obj=complex_stage.approvement,
        event=APPROVEMENT_HISTORY_EVENTS.status_changed,
        status=APPROVEMENT_STATUSES.closed,
        resolution=APPROVEMENT_RESOLUTIONS.approved,
    )


@patch('ok.approvements.controllers.ApprovementRequiredNotification')
@patch('ok.notifications.approvements.collect_issues_for_approvements', Mock())
@patch('ok.notifications.approvements.StaffUser.fetch', lambda x: {})
@patch('ok.notifications.base.get_user_to_language', lambda x: {})
def test_approve_complex_stage_need_all(mocked_notification):
    complex_stage = f.create_complex_stage(['u1', 'u2'], need_approvals=2)

    ctl = ApprovementController(complex_stage.approvement)
    stage_to_approve = complex_stage.stages.all().first()
    ctl.approve([stage_to_approve], stage_to_approve.approver)

    assert stage_to_approve.is_approved
    assert stage_to_approve.approved_by == stage_to_approve.approver

    assert not complex_stage.is_approved
    assert complex_stage.approvement.status == APPROVEMENT_STATUSES.in_progress
    mocked_notification.assert_not_called()

    assert_history_exists(
        obj=stage_to_approve,
        event=APPROVEMENT_HISTORY_EVENTS.status_changed,
        status=APPROVEMENT_STAGE_STATUSES.approved,
    )


@patch('ok.approvements.controllers.ApprovementRequiredNotification')
@patch('ok.notifications.approvements.collect_issues_for_approvements', Mock())
@patch('ok.notifications.approvements.StaffUser.fetch', lambda x: {})
@patch('ok.notifications.base.get_user_to_language', lambda x: {})
def test_approve_complex_stage_need_all_last_approver(mocked_notification):
    complex_stage = f.create_parent_stage(need_approvals=2)
    f.create_child_stage(complex_stage, is_approved=True)
    stage_to_approve = f.create_child_stage(complex_stage)

    ctl = ApprovementController(complex_stage.approvement)
    ctl.approve([stage_to_approve], stage_to_approve.approver)

    assert stage_to_approve.is_approved
    assert stage_to_approve.approved_by == stage_to_approve.approver

    assert complex_stage.is_approved
    assert complex_stage.approved_by == stage_to_approve.approved_by

    assert complex_stage.approvement.status == APPROVEMENT_STATUSES.closed
    assert complex_stage.approvement.resolution == APPROVEMENT_RESOLUTIONS.approved
    mocked_notification.assert_not_called()

    for stage in [complex_stage, stage_to_approve]:
        assert_history_exists(
            obj=stage,
            event=APPROVEMENT_HISTORY_EVENTS.status_changed,
            status=APPROVEMENT_STAGE_STATUSES.approved,
        )
    assert_history_exists(
        obj=complex_stage.approvement,
        event=APPROVEMENT_HISTORY_EVENTS.status_changed,
        status=APPROVEMENT_STATUSES.closed,
        resolution=APPROVEMENT_RESOLUTIONS.approved,
    )


@pytest.mark.parametrize('need_approvals,is_approved', ((2, True), (3, False)))
@patch('ok.notifications.approvements.StaffUser.fetch', lambda x: {})
@patch('ok.notifications.base.get_user_to_language', lambda x: {})
def test_approve_need_approvals(need_approvals, is_approved):
    stage = f.create_parent_stage(need_approvals=need_approvals, is_approved=False)
    f.create_child_stage(stage, is_approved=True)
    f.create_child_stage(stage)

    stage_to_approve = f.create_child_stage(stage)

    ctl = ApprovementController(stage.approvement)
    ctl.approve([stage_to_approve], stage_to_approve.approver)

    assert stage.is_approved == is_approved


@patch('ok.approvements.controllers.ApprovementController.ping_approvers')
@patch('ok.notifications.approvements.collect_issues_for_approvements', Mock())
@patch('ok.notifications.approvements.StaffUser.fetch', lambda x: {})
@patch('ok.notifications.base.get_user_to_language', lambda x: {})
def test_approve_complex_multiple_stages(mocked_ping):
    approvement = f.ApprovementFactory()

    stage_all = f.create_parent_stage(approvement=approvement, need_approvals=2)
    stage_all_child_to_approve = f.create_child_stage(stage_all)
    stage_all_child_to_wait = f.create_child_stage(stage_all)

    stage_simple = f.ApprovementStageFactory(approvement=approvement)

    stage_any = f.create_parent_stage(approvement=approvement, need_approvals=1)
    stage_any_child_to_approve = f.create_child_stage(stage_any)
    stage_any_child_to_cancel = f.create_child_stage(stage_any)

    # Обновляем текущие стадии явно, т.к. создание согласования в тесте
    # делается в обход бизнес-логики
    approvement.next_stages.update(status=APPROVEMENT_STAGE_STATUSES.current)

    stages_to_approve = [
        stage_all_child_to_approve,
        stage_simple,
        stage_any_child_to_approve,
    ]
    initiator = 'initiator'
    ctl = ApprovementController(approvement)
    ctl.approve(stages_to_approve, initiator)

    for stage in chain(stages_to_approve, [stage_any]):
        assert stage.is_approved
        assert stage.status == APPROVEMENT_STAGE_STATUSES.approved
        assert stage.approved_by == initiator
        assert_history_exists(
            obj=stage,
            event=APPROVEMENT_HISTORY_EVENTS.status_changed,
            status=APPROVEMENT_STAGE_STATUSES.approved,
        )

    stage_all.refresh_from_db()
    assert stage_all.is_approved is None
    assert stage_all.status == APPROVEMENT_STAGE_STATUSES.current

    stage_any_child_to_cancel.refresh_from_db()
    assert stage_any_child_to_cancel.is_approved is None
    assert stage_any_child_to_cancel.status == APPROVEMENT_STAGE_STATUSES.cancelled
    assert_history_exists(
        obj=stage_any_child_to_cancel,
        event=APPROVEMENT_HISTORY_EVENTS.status_changed,
        status=APPROVEMENT_STAGE_STATUSES.cancelled,
    )

    active_stages = list(approvement.stages.active())
    assert active_stages == [stage_all, stage_all_child_to_wait]
    assert approvement.status == APPROVEMENT_STATUSES.in_progress
    assert mocked_ping.called_once_with(initiator)


@patch('ok.notifications.approvements.collect_issues_for_approvements', Mock())
@patch('ok.notifications.approvements.StaffUser.fetch', lambda x: {})
@patch('ok.notifications.base.get_user_to_language', lambda x: {})
def test_approve_complex_multiple_stages_finished():
    approvement = f.ApprovementFactory()
    stages_to_approve = []
    stages_to_cancel = []

    for _ in range(2):
        parent = f.create_parent_stage(approvement=approvement, need_approvals=1)
        stages_to_approve.append(f.create_child_stage(parent))
        stages_to_cancel.append(f.create_child_stage(parent))

    initiator = 'initiator'
    ctl = ApprovementController(approvement)
    ctl.approve(stages_to_approve, initiator)

    for stage in chain(stages_to_approve, (s.parent for s in stages_to_approve)):
        assert stage.is_approved
        assert stage.status == APPROVEMENT_STAGE_STATUSES.approved
        assert stage.approved_by == initiator
        assert_history_exists(
            obj=stage,
            event=APPROVEMENT_HISTORY_EVENTS.status_changed,
            status=APPROVEMENT_STAGE_STATUSES.approved,
        )

    for stage in stages_to_cancel:
        stage.refresh_from_db()
        assert stage.is_approved is None
        assert stage.status == APPROVEMENT_STAGE_STATUSES.cancelled
        assert_history_exists(
            obj=stage,
            event=APPROVEMENT_HISTORY_EVENTS.status_changed,
            status=APPROVEMENT_STAGE_STATUSES.cancelled,
        )

    assert approvement.status == APPROVEMENT_STATUSES.closed
    assert approvement.resolution == APPROVEMENT_RESOLUTIONS.approved


@patch('ok.approvements.controllers.update_approvement_issue_task.delay')
@patch('ok.approvements.controllers.ApprovementCancelledNotification')
@pytest.mark.parametrize('stage_status', (
    APPROVEMENT_STAGE_STATUSES.suspended,
    APPROVEMENT_STAGE_STATUSES.pending,
))
def test_close(mocked_notification, mocked_update_issue, stage_status):
    approvement = f.create_approvement(stages_count=1)
    approvement.stages.update(status=stage_status)

    ctl = ApprovementController(approvement)
    ctl.close(approvement.author)

    assert approvement.status == APPROVEMENT_STATUSES.closed
    assert approvement.resolution == APPROVEMENT_RESOLUTIONS.declined
    assert_history_exists(
        obj=approvement,
        event=APPROVEMENT_HISTORY_EVENTS.status_changed,
        status=APPROVEMENT_STATUSES.closed,
        resolution=APPROVEMENT_RESOLUTIONS.declined,
    )
    for stage in approvement.stages.all():
        assert stage.status == APPROVEMENT_STAGE_STATUSES.cancelled
        assert_history_exists(
            obj=stage,
            event=APPROVEMENT_HISTORY_EVENTS.status_changed,
            status=APPROVEMENT_STAGE_STATUSES.cancelled,
        )
    mocked_notification.assert_not_called()
    mocked_update_issue.assert_called_once_with(approvement.id, [IssueFieldEnum.APPROVEMENT_STATUS])


@patch('ok.approvements.controllers.update_approvement_issue_task.delay')
@patch('ok.approvements.controllers.ApprovementCancelledNotification')
@pytest.mark.parametrize('stage_status', (
    APPROVEMENT_STAGE_STATUSES.approved,
    APPROVEMENT_STAGE_STATUSES.rejected,
    APPROVEMENT_STAGE_STATUSES.cancelled,
))
def test_close_with_finished_stages(mocked_notification, mocked_update_issue, stage_status):
    """
    При закрытии согласования завершённые стадии не меняют статус
    """
    approvement = f.create_approvement(stages_count=1)
    approvement.stages.update(status=stage_status)

    ctl = ApprovementController(approvement)
    ctl.close(approvement.author)

    assert approvement.status == APPROVEMENT_STATUSES.closed
    assert approvement.resolution == APPROVEMENT_RESOLUTIONS.declined

    for stage in approvement.stages.all():
        assert stage.status == stage_status
        assert stage.history.filter(status=APPROVEMENT_STAGE_STATUSES.cancelled).count() == 0

    mocked_notification.assert_not_called()
    mocked_update_issue.assert_called_once_with(approvement.id, [IssueFieldEnum.APPROVEMENT_STATUS])


@patch('ok.approvements.controllers.ApprovementCancelledNotification')
def test_cancelled_notification_enabled_by_switch(mocked_notification):
    f.create_waffle_switch('enable_approvement_cancelled_notification')
    approvement = f.create_approvement(status=APPROVEMENT_STATUSES.in_progress)
    current_stages = approvement.current_stages

    ctl = ApprovementController(approvement)
    ctl.close(approvement.author)

    mocked_notification.assert_called_once_with(
        instance=approvement,
        initiator=approvement.author,
        current_stages=current_stages,
    )


@patch('ok.approvements.controllers.update_approvement_issue_task.delay')
@patch('ok.approvements.controllers.ApprovementSuspendedNotification')
def test_suspend(mocked_notification, mocked_update_issue):
    approvement = f.create_approvement(status=APPROVEMENT_STATUSES.in_progress, stages_count=1)

    ctl = ApprovementController(approvement)
    ctl.suspend(approvement.author)

    assert approvement.status == APPROVEMENT_STATUSES.suspended
    assert_history_exists(
        obj=approvement,
        event=APPROVEMENT_HISTORY_EVENTS.status_changed,
        status=APPROVEMENT_STATUSES.suspended,
    )
    for stage in approvement.stages.all():
        assert stage.status == APPROVEMENT_STAGE_STATUSES.suspended
        assert_history_exists(
            obj=stage,
            event=APPROVEMENT_HISTORY_EVENTS.status_changed,
            status=APPROVEMENT_STAGE_STATUSES.suspended,
        )
    mocked_notification.assert_not_called()
    mocked_update_issue.assert_called_once_with(approvement.id, [IssueFieldEnum.APPROVEMENT_STATUS])


@patch('ok.approvements.controllers.ApprovementSuspendedNotification')
def test_suspended_notification_enabled_by_switch(mocked_notification):
    f.create_waffle_switch('enable_approvement_suspended_notification')
    approvement = f.create_approvement(status=APPROVEMENT_STATUSES.in_progress)
    current_stages = approvement.current_stages

    ctl = ApprovementController(approvement)
    ctl.suspend(approvement.author)

    mocked_notification.assert_called_once_with(
        instance=approvement,
        initiator=approvement.author,
        current_stages=current_stages,
    )


@patch('ok.approvements.controllers.update_approvement_issue_task.delay')
@patch('ok.approvements.controllers.ApprovementSuspendedNotification')
def test_reject(mocked_notification, mocked_update_issue):
    approvement = f.ApprovementFactory(status=APPROVEMENT_STATUSES.in_progress)
    stage, another_stage = f.ApprovementStageFactory.create_batch(2, approvement=approvement)

    ctl = ApprovementController(approvement)
    ctl.reject(stage, stage.approver)

    assert approvement.status == APPROVEMENT_STATUSES.rejected
    assert_history_exists(
        obj=approvement,
        event=APPROVEMENT_HISTORY_EVENTS.status_changed,
        status=APPROVEMENT_STATUSES.rejected,
    )

    stage.refresh_from_db()
    assert stage.status == APPROVEMENT_STAGE_STATUSES.rejected
    assert stage.is_approved is False
    assert_history_exists(
        obj=stage,
        event=APPROVEMENT_HISTORY_EVENTS.status_changed,
        status=APPROVEMENT_STAGE_STATUSES.rejected,
    )

    another_stage.refresh_from_db()
    assert another_stage.status == APPROVEMENT_STAGE_STATUSES.suspended
    assert another_stage.is_approved is None
    assert_history_exists(
        obj=another_stage,
        event=APPROVEMENT_HISTORY_EVENTS.status_changed,
        status=APPROVEMENT_STAGE_STATUSES.suspended,
    )

    mocked_notification.assert_not_called()
    mocked_update_issue.assert_called_once_with(approvement.id, [IssueFieldEnum.APPROVEMENT_STATUS])


@patch('ok.approvements.controllers.ApprovementSuspendedNotification')
def test_rejected_notification_enabled_by_switch(mocked_notification):
    f.create_waffle_switch('enable_approvement_rejected_notification')
    approvement = f.create_approvement(stages_count=2)
    stage, _ = approvement.stages.all()

    initiator = stage.approver
    ctl = ApprovementController(approvement)
    ctl.reject(stage, stage.approver)

    mocked_notification.assert_called_once_with(
        instance=approvement,
        initiator=initiator,
        current_stages=[stage],
    )


@patch('ok.approvements.controllers.update_approvement_issue_task.delay')
@patch('ok.approvements.controllers.ApprovementRequiredNotification')
def test_resume_suspended(mocked_notification, mocked_update_issue):
    approvement = f.create_approvement(status=APPROVEMENT_STATUSES.suspended, stages_count=1)
    approvement.stages.update(status=APPROVEMENT_STAGE_STATUSES.suspended)

    ctl = ApprovementController(approvement)
    ctl.resume(approvement.author)

    assert approvement.status == APPROVEMENT_STATUSES.in_progress
    assert_history_exists(
        obj=approvement,
        event=APPROVEMENT_HISTORY_EVENTS.status_changed,
        status=APPROVEMENT_STATUSES.in_progress,
    )
    for stage in approvement.stages.all():
        assert stage.status == APPROVEMENT_STAGE_STATUSES.current
        assert_history_exists(
            obj=stage,
            event=APPROVEMENT_HISTORY_EVENTS.status_changed,
            status=APPROVEMENT_STAGE_STATUSES.current,
        )
    mocked_notification.assert_called_once_with(
        instance=approvement,
        initiator=approvement.author,
        current_stages=approvement.current_stages,
    )
    mocked_update_issue.assert_called_once_with(approvement.id, [IssueFieldEnum.APPROVEMENT_STATUS])


@patch('ok.approvements.controllers.update_approvement_issue_task.delay')
@patch('ok.approvements.controllers.ApprovementRequiredNotification')
def test_resume_rejected(mocked_notification, mocked_update_issue):
    approvement = f.ApprovementFactory(status=APPROVEMENT_STATUSES.rejected)
    complex_stage = f.create_complex_stage(
        approvers=['approver', 'waiter'],
        approvement=approvement,
    )
    complex_stage.status = APPROVEMENT_STAGE_STATUSES.approved
    complex_stage.is_approved = True
    complex_stage.save()

    approved_stage, cancelled_stage = complex_stage.stages.all()
    approved_stage.status = APPROVEMENT_STAGE_STATUSES.approved
    approved_stage.is_approved = True
    approved_stage.save()
    cancelled_stage.status = APPROVEMENT_STAGE_STATUSES.cancelled
    cancelled_stage.save()

    rejected_stage = f.ApprovementStageFactory(
        approvement=approvement,
        status=APPROVEMENT_STAGE_STATUSES.rejected,
        is_approved=False,
    )

    suspended_stage = f.ApprovementStageFactory(
        approvement=approvement,
        status=APPROVEMENT_STAGE_STATUSES.suspended,
        is_approved=None,
    )

    ctl = ApprovementController(approvement)
    ctl.resume(approvement.author)

    assert approvement.status == APPROVEMENT_STATUSES.in_progress
    assert_history_exists(
        obj=approvement,
        event=APPROVEMENT_HISTORY_EVENTS.status_changed,
        status=APPROVEMENT_STATUSES.in_progress,
    )

    cancelled_stage.refresh_from_db()
    assert cancelled_stage.status == APPROVEMENT_STAGE_STATUSES.cancelled
    assert cancelled_stage.is_approved is None

    for stage in (complex_stage, approved_stage):
        stage.refresh_from_db()
        assert stage.status == APPROVEMENT_STAGE_STATUSES.approved
        assert stage.is_approved

    statuses = (APPROVEMENT_STAGE_STATUSES.current, APPROVEMENT_STAGE_STATUSES.pending)
    for stage, status in zip((rejected_stage, suspended_stage), statuses):
        stage.refresh_from_db()
        assert stage.status == status
        assert stage.is_approved is None
        assert_history_exists(
            obj=stage,
            event=APPROVEMENT_HISTORY_EVENTS.status_changed,
            status=status,
        )

    mocked_notification.assert_called_once_with(
        instance=approvement,
        initiator=approvement.author,
        current_stages=[rejected_stage],
    )
    mocked_update_issue.assert_called_once_with(approvement.id, [IssueFieldEnum.APPROVEMENT_STATUS])


def test_active_approvements_only():
    """
    Проверяем фильтрацию: что попадают только активные согласования
    """
    active_approvement = f.create_approvement()
    f.create_approvement(status=APPROVEMENT_STATUSES.closed)

    approvements_computed = _get_active_approvements_with_current_stages_only()
    assert len(approvements_computed) == 1
    assert approvements_computed[0] == active_approvement


@pytest.mark.parametrize('is_parallel', (True, False))
def test_current_stages_only(is_parallel):
    """
    Проверяем фильтрацию: что попадают только текущие стадии согласований
    """
    approvement = f.create_pinged_approvement(
        stages_count=3,
        is_parallel=is_parallel,
        ping_time=yesterday,
    )
    f.approve_stage(approvement.stages.first())

    approvement_computed = _get_active_approvements_with_current_stages_only()[0]
    positions = {stage.position for stage in approvement_computed.active_stages}
    assert positions == {1, 2} if is_parallel else {1}


def test_approvers_grouping(active_approvements_with_active_stages_only, two_persons):
    approvements = active_approvements_with_active_stages_only
    mapping = _get_active_approver_to_approvement_stages_map(approvements)

    assert set(mapping.keys()) == set(two_persons)
    assert set(mapping[two_persons[0]]) == {
        approvements[0].stages.get(position=0),
        approvements[1].stages.get(position=0),
    }
    assert set(mapping[two_persons[1]]) == {
        approvements[1].stages.get(position=1),
    }


def test_overdue_approvements_grouping(overdue_approvements_map, two_persons):
    receivers_map = _get_receiver_to_overdue_approvement_map()

    assert len(receivers_map) == 2

    assert two_persons[0] in receivers_map
    assert set(receivers_map[two_persons[0]]) == {
        overdue_approvements_map['sequential_approvement_author1'],
        overdue_approvements_map['parallel_approvement_author1'],
    }

    assert two_persons[1] in receivers_map
    assert set(receivers_map[two_persons[1]]) == {
        overdue_approvements_map['parallel_approvement_author2'],
    }


@patch('ok.approvements.controllers.ApprovementRequiredNotification')
@pytest.mark.parametrize('is_parallel', (True, False))
def test_ping_approvers(mocked_notification, is_parallel):
    initiator = 'initiator'
    approvement = f.create_approvement(stages_count=3, is_parallel=is_parallel)
    ApprovementController(approvement).ping_approvers(initiator)

    mocked_notification.assert_called_once_with(
        instance=approvement,
        initiator=approvement.author,
        current_stages=approvement.current_stages,
    )
    stages = approvement.stages.all()
    stages = stages if is_parallel else stages[:1]
    for stage in stages:
        assert_history_exists(stage, APPROVEMENT_HISTORY_EVENTS.ping_sent, user=initiator)


@patch('ok.approvements.controllers.ApprovementRequiredNotification')
def test_ping_approvers_complex_stage(mocked_notification):
    complex_stage = f.create_parent_stage(status=APPROVEMENT_STAGE_STATUSES.current)
    stage = f.create_child_stage(complex_stage, status=APPROVEMENT_STAGE_STATUSES.current)
    approvement = stage.approvement
    initiator = 'initiator'

    ApprovementController(stage.approvement).ping_approvers(initiator)

    mocked_notification.assert_called_once_with(
        instance=approvement,
        initiator=approvement.author,
        current_stages=approvement.current_stages,
    )
    for stage in [stage, complex_stage]:
        assert_history_exists(stage, APPROVEMENT_HISTORY_EVENTS.ping_sent, user=initiator)
