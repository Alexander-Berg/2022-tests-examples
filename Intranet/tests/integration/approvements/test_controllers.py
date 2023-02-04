from collections import defaultdict
from datetime import timedelta

import pytest

from unittest.mock import patch, Mock

from django.utils import timezone

from ok.approvements.choices import (
    APPROVEMENT_STATUSES,
    APPROVEMENT_RESOLUTIONS,
    APPROVEMENT_STAGE_STATUSES,
    APPROVEMENT_HISTORY_EVENTS,
)
from ok.approvements.controllers import (
    ApprovementController,
    notify_current_approvers,
    notify_overdue_approvement_receivers,
)

from tests import factories as f
from tests.factories import make_approvement_stages_overdue
from tests.utils.approvements import generate_stages_data as _
from tests.utils.assertions import assert_not_raises

pytestmark = pytest.mark.django_db


yesterday = timezone.now() - timedelta(days=1)


@patch('ok.approvements.controllers.get_absent_approvers', Mock(return_value=set()))
@patch('ok.approvements.controllers.ApprovementReminderNotification')
def test_notify_current_approvers_sequential(mocked_notification):
    approvers = f.get_users(2)
    approvement = f.create_pinged_approvement(
        approvers=approvers,
        ping_time=yesterday,
    )

    notify_current_approvers()
    mocked_notification.assert_called_once_with(
        receiver=approvers[0],
        approvements=[approvement],
    )


@patch('ok.approvements.controllers.get_absent_approvers', Mock(return_value=set()))
@patch('ok.approvements.controllers.ApprovementReminderNotification')
def test_notify_current_approvers_parallel(mocked_notification):
    approvers = f.get_users(2)
    approvement = f.create_pinged_approvement(
        approvers=approvers,
        is_parallel=True,
        ping_time=yesterday,
    )

    notify_current_approvers()
    mocked_notification.assert_any_call(
        receiver=approvers[0],
        approvements=[approvement],
    )
    mocked_notification.assert_any_call(
        receiver=approvers[1],
        approvements=[approvement],
    )


@patch('ok.approvements.controllers.get_absent_approvers', Mock(return_value=set()))
@patch('ok.approvements.controllers.ApprovementReminderNotification')
def test_do_not_notify_approver_second_time_a_day(mocked_notification):
    approvers = f.get_users(2)
    # Пингуем текущей датой
    f.create_pinged_approvement(approvers=approvers)

    notify_current_approvers()
    mocked_notification.assert_not_called()


@patch('ok.approvements.controllers.get_absent_approvers', Mock(return_value=set()))
@patch('ok.approvements.controllers.ApprovementReminderNotification')
def test_do_not_notify_if_question_was_asked(mocked_notification):
    approvement = f.create_pinged_approvement(stages_count=1, ping_time=yesterday)
    f.ApprovementHistoryFactory(
        content_object=approvement.stages.first(),
        event=APPROVEMENT_HISTORY_EVENTS.question_asked,
    )

    notify_current_approvers()
    mocked_notification.assert_not_called()


@patch('ok.approvements.controllers.get_absent_approvers', Mock(return_value=set()))
@patch('ok.approvements.controllers.ApprovementOverdueNotification')
def test_notify_overdue_approvement_author(mocked_notification):
    approvers = f.get_users(2)
    approvement = f.create_approvement(approvers=approvers)
    first_stage = approvement.stages.first()
    make_approvement_stages_overdue([first_stage])

    notify_overdue_approvement_receivers()
    approvement_to_current_stages = defaultdict(list)
    approvement_to_current_stages[approvement] = [first_stage]
    mocked_notification.assert_called_once_with(
        receiver=approvement.author,
        approvement_to_current_stages=approvement_to_current_stages,
    )


@patch('ok.approvements.controllers.get_gaps', return_value=[])
@patch('ok.approvements.controllers.get_holidays', return_value={})
@patch('ok.approvements.controllers.ApprovementReminderNotification')
def test_do_not_notify_if_vacation(mocked_notification, mocked_holidays, mocked_gaps):
    approvers = f.get_users(1)
    f.create_pinged_approvement(approvers=approvers, ping_time=yesterday)
    mocked_gaps.return_value = [{
        'person_login': approvers[0],
        'workflow': 'vacation',
    }]

    notify_current_approvers()
    mocked_notification.assert_not_called()


@patch('ok.approvements.controllers.get_gaps', return_value=[])
@patch('ok.approvements.controllers.get_holidays', return_value={})
@patch('ok.approvements.controllers.ApprovementReminderNotification')
def test_do_not_notify_if_holiday(mocked_notification, mocked_holidays, mocked_gaps):
    approvers = f.get_users(1)
    f.create_pinged_approvement(approvers=approvers, ping_time=yesterday)
    mocked_holidays.return_value = {
        approvers[0]: [{'is-holiday': True, 'day-type': 'holiday'}],
    }

    notify_current_approvers()
    mocked_notification.assert_not_called()


@patch('ok.approvements.controllers.get_absent_approvers', Mock(side_effect=Exception('stub')))
@patch('ok.approvements.controllers.ApprovementReminderNotification')
def test_do_not_fail_if_cannot_get_approvers(mocked_notification):
    f.create_waffle_switch('skip_gap_errors_on_notify')

    approvers = f.get_users(1)
    approvement = f.create_pinged_approvement(approvers=approvers, ping_time=yesterday)

    with assert_not_raises():
        notify_current_approvers()

    mocked_notification.assert_called_once_with(
        receiver=approvers[0],
        approvements=[approvement],
    )


@patch('ok.approvements.controllers.ApprovementRequiredNotification', Mock())
@patch('ok.approvements.controllers.ApprovementFinishedNotification', Mock())
@patch('ok.approvements.controllers.get_issue_author', Mock(return_value='e'))
def test_current_stages_change_on_approve():
    initiator = 'initiator'
    stages_data = _(_('a1', 'a2', n=1), 'b', 'c', _('d1', 'd2'), 'e', 'f')
    data = {
        'stages': stages_data['stages'],
        'create_comment': False,
        'object_id': 'ISSUE-1',
        'is_auto_approving': True,
    }

    approvement = ApprovementController.create(data, initiator)
    controller = ApprovementController(approvement)
    stages_qs = approvement.stages.values_list('approver', flat=True)
    assert list(stages_qs.current()) == ['', 'a1', 'a2']
    assert list(stages_qs.pending()) == ['b', 'c', '', 'd1', 'd2', 'e', 'f']

    # Ставим ок на текущей сложной стадии – current меняется на следующую
    stage = approvement.stages.get(approver='a1')
    controller.approve([stage], 'a1')
    assert list(stages_qs.approved()) == ['', 'a1']
    assert list(stages_qs.cancelled()) == ['a2']
    assert list(stages_qs.current()) == ['b']
    assert list(stages_qs.pending()) == ['c', '', 'd1', 'd2', 'e', 'f']

    # Ставим ок вне очереди – current не меняется
    stage = approvement.stages.get(approver='c')
    controller.approve([stage], 'c')
    assert list(stages_qs.approved()) == ['', 'a1', 'c']
    assert list(stages_qs.cancelled()) == ['a2']
    assert list(stages_qs.current()) == ['b']
    assert list(stages_qs.pending()) == ['', 'd1', 'd2', 'e', 'f']

    # Ещё один ок вне очереди на сложной стадии – current не меняется
    stage = approvement.stages.get(approver='d1')
    controller.approve([stage], 'd1')
    assert list(stages_qs.approved()) == ['', 'a1', 'c', 'd1']
    assert list(stages_qs.cancelled()) == ['a2']
    assert list(stages_qs.current()) == ['b']
    assert list(stages_qs.pending()) == ['', 'd2', 'e', 'f']

    # Ок, правильный по очереди – current меняется на оставшуюся часть сложной стадии
    stage = approvement.stages.get(approver='b')
    controller.approve([stage], 'b')
    assert list(stages_qs.approved()) == ['', 'a1', 'b', 'c', 'd1']
    assert list(stages_qs.cancelled()) == ['a2']
    assert list(stages_qs.current()) == ['', 'd2']
    assert list(stages_qs.pending()) == ['e', 'f']

    # Ещё один ок, правильный по очереди.
    # Следующим идёт `e`, но он подтверждается автоматом, т.к. стоит is_auto_approving=true
    # – current переносится на последнюю стадию
    stage = approvement.stages.get(approver='d2')
    controller.approve([stage], 'd2')
    assert list(stages_qs.approved()) == ['', 'a1', 'b', 'c', '', 'd1', 'd2', 'e']
    assert list(stages_qs.cancelled()) == ['a2']
    assert list(stages_qs.current()) == ['f']
    assert list(stages_qs.pending()) == []

    # Последний ок – current больше нет
    stage = approvement.stages.get(approver='f')
    controller.approve([stage], 'f')
    assert list(stages_qs.approved()) == ['', 'a1', 'b', 'c', '', 'd1', 'd2', 'e', 'f']
    assert list(stages_qs.cancelled()) == ['a2']
    assert list(stages_qs.current()) == []
    assert list(stages_qs.pending()) == []
    approvement.refresh_from_db()
    assert approvement.status == APPROVEMENT_STATUSES.closed
    assert approvement.resolution == APPROVEMENT_RESOLUTIONS.approved


@patch('ok.approvements.controllers.ApprovementRequiredNotification', Mock())
@patch('ok.approvements.controllers.ApprovementFinishedNotification', Mock())
def test_current_stages_change_on_approve_parallel():
    initiator = 'initiator'
    stages_data = _('a', _('b1', 'b2', n=1), 'c', _('d1', 'd2'))
    data = {
        'stages': stages_data['stages'],
        'create_comment': False,
        'object_id': 'ISSUE-1',
        'is_parallel': True,
    }

    approvement = ApprovementController.create(data, initiator)
    controller = ApprovementController(approvement)
    stages_qs = approvement.stages.values_list('approver', flat=True)
    assert list(stages_qs.current()) == ['a', '', 'b1', 'b2', 'c', '', 'd1', 'd2']
    assert list(stages_qs.pending()) == []

    stages_qs = stages_qs.leaves()
    approved = []
    for approver in ('a', 'b2', 'c', 'd1', 'd2'):
        stage = approvement.stages.get(approver=approver)
        controller.approve([stage], approver)
        approved.append(approver)
        remaining = stages_qs.exclude(status__in=(
            APPROVEMENT_STAGE_STATUSES.approved,
            APPROVEMENT_STAGE_STATUSES.cancelled,
        ))
        assert list(stages_qs.approved()) == approved
        assert list(stages_qs.pending()) == []
        assert list(remaining) == list(stages_qs.current())


@patch('ok.approvements.controllers.ApprovementRequiredNotification', Mock())
@patch('ok.approvements.controllers.ApprovementFinishedNotification', Mock())
@patch('ok.approvements.controllers.ApprovementApprovedByResponsibleNotification', Mock())
def test_current_stages_change_on_suspend():
    initiator = 'initiator'
    stages_data = _('a', _('b1', 'b2', n=1), 'c', _('d1', 'd2'))
    data = {
        'stages': stages_data['stages'],
        'create_comment': False,
        'object_id': 'ISSUE-1',
    }

    approvement = ApprovementController.create(data, initiator)
    controller = ApprovementController(approvement)
    stages_qs = approvement.stages.values_list('approver', flat=True)
    assert list(stages_qs.current()) == ['a']
    assert list(stages_qs.pending()) == ['', 'b1', 'b2', 'c', '', 'd1', 'd2']

    # На нескольких стадиях ставим ок, потом приостанавливаем согласование
    stages = list(approvement.stages.filter(approver__in=('a', 'b2')))
    controller.approve(stages, initiator)
    controller.suspend(initiator)
    assert list(stages_qs.active()) == []
    assert list(stages_qs.approved()) == ['a', '', 'b2']
    assert list(stages_qs.cancelled()) == ['b1']
    assert list(stages_qs.suspended()) == ['c', '', 'd1', 'd2']

    # Возобновляем согласование
    controller.resume(initiator)
    assert list(stages_qs.current()) == ['c']
    assert list(stages_qs.pending()) == ['', 'd1', 'd2']
    assert list(stages_qs.approved()) == ['a', '', 'b2']
    assert list(stages_qs.cancelled()) == ['b1']
    assert list(stages_qs.suspended()) == []


@patch('ok.approvements.controllers.ApprovementRequiredNotification', Mock())
@patch('ok.approvements.controllers.ApprovementFinishedNotification', Mock())
@patch('ok.approvements.controllers.ApprovementApprovedByResponsibleNotification', Mock())
def test_current_stages_change_on_reject():
    initiator = 'initiator'
    stages_data = _('a', _('b1', 'b2', n=1), 'c', _('d1', 'd2'))
    data = {
        'stages': stages_data['stages'],
        'create_comment': False,
        'object_id': 'ISSUE-1',
    }

    approvement = ApprovementController.create(data, initiator)
    controller = ApprovementController(approvement)
    stages_qs = approvement.stages.values_list('approver', flat=True)
    assert list(stages_qs.current()) == ['a']
    assert list(stages_qs.pending()) == ['', 'b1', 'b2', 'c', '', 'd1', 'd2']

    # На нескольких стадиях ставим ок, а потом не ок
    stages_to_approve = list(approvement.stages.filter(approver__in=('a', 'd1', 'd2')))
    stage_to_reject = approvement.stages.get(approver='b2')
    controller.approve(stages_to_approve, initiator)
    controller.reject(stage_to_reject, 'b2')
    assert list(stages_qs.active()) == []
    assert list(stages_qs.approved()) == ['a', '', 'd1', 'd2']
    assert list(stages_qs.rejected()) == ['b2']
    assert list(stages_qs.suspended()) == ['', 'b1', 'c']

    # Возобновляем согласование
    controller.resume(initiator)
    assert list(stages_qs.current()) == ['', 'b1', 'b2']
    assert list(stages_qs.pending()) == ['c']
    assert list(stages_qs.approved()) == ['a', '', 'd1', 'd2']
    assert list(stages_qs.rejected()) == []
    assert list(stages_qs.suspended()) == []
