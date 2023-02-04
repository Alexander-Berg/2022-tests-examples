import pytest

from collections import defaultdict
from datetime import timedelta, datetime
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
    get_absent_approvers,
    invite_deputy_approvers,
    notify_current_approvers,
    notify_overdue_approvement_receivers,
)

from tests import factories as f
from tests.factories import make_approvement_stages_overdue
from tests.utils.approvements import generate_stages_data as _
from tests.utils.assertions import assert_not_raises

pytestmark = pytest.mark.django_db


today = timezone.now()
yesterday = today - timedelta(days=1)
tomorrow = today + timedelta(days=1)


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
        'date_from': today.strftime('%Y-%m-%dT00:00:00'),
        'date_to': tomorrow.strftime('%Y-%m-%dT00:00:00'),
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


@pytest.mark.parametrize('gaps, absence_threshold, is_absent', (
    ([{'date_from': '2022-04-01T00:00:00', 'date_to': '2022-04-04T00:00:00'}], 3, True),  # 3 дня
    ([{'date_from': '2022-04-01T00:00:00', 'date_to': '2022-04-03T00:00:00'}], 3, False),  # 2 дня
    ([{'date_from': '2022-04-01T00:00:00', 'date_to': '2022-04-03T12:00:00'}], 3, False),  # 2 дня
    ([{'date_from': '2022-04-01T00:00:00', 'date_to': '2022-04-02T00:00:00'}], 1, True),  # 1 день
    ([{'date_from': '2022-04-01T00:00:00', 'date_to': '2022-04-01T00:00:00'}], 1, False),  # 0 дней
    ([{'date_from': '2022-04-01T00:00:00', 'date_to': '2022-04-01T12:00:00'}], 1, False),  # 0 дней
    ([{'date_from': '2022-04-02T00:00:00', 'date_to': '2022-04-12T00:00:00'}], 3, False),  # 0 дней
    ([{'date_from': '2022-03-20T00:00:00', 'date_to': '2022-04-03T00:00:00'}], 3, False),  # 2 дня
    ([{'date_from': '2022-04-01T09:00:00', 'date_to': '2022-04-04T00:00:00'}], 3, True),  # 3 дня
    ([{'date_from': '2022-04-01T11:00:00', 'date_to': '2022-04-04T00:00:00'}], 3, False),  # 2 дня
    (
        [
            {
                'date_from': '2022-04-01T00:00:00',
                'date_to': '2022-04-04T00:00:00',
                'work_in_absence': True},
        ],
        3,
        False,
    ),  # 0 дней отсутствия
    (
        [
            {'date_from': '2022-04-01T00:00:00', 'date_to': '2022-04-03T00:00:00'},
            {'date_from': '2022-04-02T00:00:00', 'date_to': '2022-04-04T00:00:00'},
        ],
        3,
        True,
    ),  # пересекающиеся: 3 дня отсутствия
    (
        [
            {'date_from': '2022-03-20T00:00:00', 'date_to': '2022-04-03T00:00:00'},
            {'date_from': '2022-04-02T00:00:00', 'date_to': '2022-04-04T00:00:00'},
        ],
        3,
        True,
    ),  # пересекающиеся: 3 дня отсутствия
    (
        [
            {'date_from': '2022-04-01T00:00:00', 'date_to': '2022-04-03T00:00:00'},
            {'date_from': '2022-04-03T00:00:00', 'date_to': '2022-04-04T00:00:00'},
        ],
        3,
        True,
    ),  # идущие подряд: 3 дня отсутствия
    (
        [
            {'date_from': '2022-04-01T00:00:00', 'date_to': '2022-04-03T00:00:00'},
            {'date_from': '2022-04-04T00:00:00', 'date_to': '2022-04-06T00:00:00'},
        ],
        3,
        False,
    ),  # не пересекающиеся: 2 дня отсутствия
    (
        [
            {'date_from': '2022-04-01T00:00:00', 'date_to': '2022-04-03T12:00:00'},
            {'date_from': '2022-04-03T13:00:00', 'date_to': '2022-04-06T00:00:00'},
        ],
        3,
        False,
    ),  # не пересекающиеся: 2 дня отсутствия
    (
        [
            {'date_from': '2022-04-01T00:00:00', 'date_to': '2022-04-03T13:00:00'},
            {'date_from': '2022-04-03T12:00:00', 'date_to': '2022-04-06T00:00:00'},
        ],
        3,
        False,
    ),
    # пересекающиеся, но не полный день: 2 дня отсутствия
    # (решили пренебрегать такими кейсами и учитываем только полные лни отсутствий)
))
@patch('ok.approvements.controllers.get_holidays', return_value={})
def test_absent_approvers(mocked_holidays, gaps, absence_threshold, is_absent):
    """
    Проверяем есть ли отсутствие у человека 01.04.2022 в течение absence_threshold дней
    """
    default_gap = {
        'workflow': 'absence',
        'person_login': 'login',
        'work_in_absence': False,
    }
    gaps = (dict(default_gap, **gap) for gap in gaps)

    with patch('ok.approvements.controllers.get_gaps', return_value=gaps):
        absent_approvers = get_absent_approvers(
            approvers=['login'],
            time_from=datetime(2022, 4, 1, 10, tzinfo=timezone.utc),
            absence_threshold=absence_threshold,
        )
    assert bool(absent_approvers) is is_absent


@pytest.mark.parametrize('need_approvals, stages_statuses, absent_approvers, invited_stage_idx', (
    (1, ('current', 'pending', 'pending'), set(), None),
    (1, ('current', 'pending', 'pending'), {'user0'}, [1]),
    (1, ('current', 'pending', 'pending'), {'user1'}, None),
    (1, ('current', 'pending', 'pending'), {'user0', 'user1'}, [2]),
    (1, ('current', 'pending', 'pending'), {'user0', 'user1', 'user2'}, None),
    (1, ('current', 'pending', 'current'), {'user0'}, None),
    (1, ('current', 'pending', 'current'), {'user0', 'user2'}, [1]),
    (2, ('current', 'current', 'pending', 'pending'), {'user0', 'user1'}, [2, 3]),
    (2, ('current', 'current', 'pending', 'pending'), {'user0'}, [2]),
    (2, ('approved', 'current', 'pending', 'pending'), {'user1'}, [2]),
))
@patch('ok.approvements.controllers.ApprovementRequiredNotification')
def test_invite_deputies(mocked_notification, django_assert_num_queries, need_approvals,
                         stages_statuses, absent_approvers, invited_stage_idx):
    parent_stage = f.create_complex_stage(
        approvers=[f'user{i}' for i in range(len(stages_statuses))],
        status=APPROVEMENT_STAGE_STATUSES.current,
        need_approvals=need_approvals,
        is_with_deputies=True,
    )
    child_stages = list(parent_stage.stages.all())
    for i, stage in enumerate(child_stages):
        status = stages_statuses[i]
        if stage.status != status:
            stage.status = status
            stage.save()
    f.create_history_entries(
        objects=parent_stage.approvement.stages.current(),
        event=APPROVEMENT_HISTORY_EVENTS.ping_sent,
        user=None,
        created=yesterday,
    )

    with patch('ok.approvements.controllers.get_absent_approvers', return_value=absent_approvers):
        # 2 - savepoint
        # 2 - select parents + pinged_stages
        queries = 4
        # 1 - update stages
        # 2 - insert history
        # 4 - select approvements + 3 prefetch
        if invited_stage_idx:
            queries += 7
        with django_assert_num_queries(queries):
            invite_deputy_approvers()

    if invited_stage_idx:
        mocked_notification.assert_called_once_with(
            instance=parent_stage.approvement,
            initiator=parent_stage.approvement.author,
            current_stages=[child_stages[i] for i in invited_stage_idx],
        )
    else:
        mocked_notification.assert_not_called()

    for i, stage in enumerate(child_stages):
        stage.refresh_from_db()
        if invited_stage_idx and i in invited_stage_idx:
            assert stage.status == APPROVEMENT_STAGE_STATUSES.current
        else:
            assert stage.status == stages_statuses[i]


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
def test_current_stages_change_on_approve_with_deputies():
    initiator = 'initiator'
    stages_data = _(_('a1', 'a2', n=1, d=True), 'b', _('c1', 'c2', 'c3', 'c4', n=2, d=True))
    data = {
        'stages': stages_data['stages'],
        'create_comment': False,
        'object_id': 'ISSUE-1',
    }

    approvement = ApprovementController.create(data, initiator)
    controller = ApprovementController(approvement)
    stages_qs = approvement.stages.values_list('approver', flat=True)
    assert list(stages_qs.current()) == ['', 'a1']
    assert list(stages_qs.pending()) == ['a2', 'b', '', 'c1', 'c2', 'c3', 'c4']

    # Ставим ок на текущей сложной стадии – current меняется на следующую
    stage = approvement.stages.get(approver='a1')
    controller.approve([stage], 'a1')
    assert list(stages_qs.approved()) == ['', 'a1']
    assert list(stages_qs.cancelled()) == ['a2']
    assert list(stages_qs.current()) == ['b']
    assert list(stages_qs.pending()) == ['', 'c1', 'c2', 'c3', 'c4']

    # Ставим ок на простой стадии – current меняется на следующую сложную.
    # Меняется лишь частично, потому что там стадия с запасными согласующими
    stage = approvement.stages.get(approver='b')
    controller.approve([stage], 'b')
    assert list(stages_qs.approved()) == ['', 'a1', 'b']
    assert list(stages_qs.cancelled()) == ['a2']
    assert list(stages_qs.current()) == ['', 'c1', 'c2']
    assert list(stages_qs.pending()) == ['c3', 'c4']

    # Ставим один из ок-ов на сложной стадии с запасными согласующими, где требуется 2 ок-а
    stage = approvement.stages.get(approver='c1')
    controller.approve([stage], 'c1')
    assert list(stages_qs.approved()) == ['', 'a1', 'b', 'c1']
    assert list(stages_qs.cancelled()) == ['a2']
    assert list(stages_qs.current()) == ['', 'c2']
    assert list(stages_qs.pending()) == ['c3', 'c4']

    # Ставим ок на той же стадии, но от запасного согласующего
    stage = approvement.stages.get(approver='c4')
    controller.approve([stage], 'c4')
    assert list(stages_qs.approved()) == ['', 'a1', 'b', '', 'c1', 'c4']
    assert list(stages_qs.cancelled()) == ['a2', 'c2', 'c3']
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
def test_current_stages_change_on_approve_parallel_with_deputies():
    initiator = 'initiator'
    stages_data = _(_('a1', 'a2', n=1, d=True), 'b', _('c1', 'c2', n=1))
    data = {
        'stages': stages_data['stages'],
        'create_comment': False,
        'object_id': 'ISSUE-1',
        'is_parallel': True,
    }

    approvement = ApprovementController.create(data, initiator)
    controller = ApprovementController(approvement)
    stages_qs = approvement.stages.values_list('approver', flat=True)
    assert list(stages_qs.current()) == ['', 'a1', 'b', '', 'c1', 'c2']
    assert list(stages_qs.pending()) == ['a2']

    stage = approvement.stages.get(approver='a1')
    controller.approve([stage], 'a1')
    assert list(stages_qs.approved()) == ['', 'a1']
    assert list(stages_qs.cancelled()) == ['a2']
    assert list(stages_qs.current()) == ['b', '', 'c1', 'c2']
    assert list(stages_qs.pending()) == []

    stage = approvement.stages.get(approver='c2')
    controller.approve([stage], 'c2')
    assert list(stages_qs.approved()) == ['', 'a1', '', 'c2']
    assert list(stages_qs.cancelled()) == ['a2', 'c1']
    assert list(stages_qs.current()) == ['b']
    assert list(stages_qs.pending()) == []

    stage = approvement.stages.get(approver='b')
    controller.approve([stage], 'b')
    assert list(stages_qs.approved()) == ['', 'a1', 'b', '', 'c2']
    assert list(stages_qs.cancelled()) == ['a2', 'c1']
    assert list(stages_qs.current()) == []
    assert list(stages_qs.pending()) == []
    approvement.refresh_from_db()
    assert approvement.status == APPROVEMENT_STATUSES.closed
    assert approvement.resolution == APPROVEMENT_RESOLUTIONS.approved


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
def test_current_stages_change_on_suspend_with_deputies():
    initiator = 'initiator'
    stages_data = _('a', _('b1', 'b2', 'b3', n=1, d=True), 'c')
    data = {
        'stages': stages_data['stages'],
        'create_comment': False,
        'object_id': 'ISSUE-1',
    }

    approvement = ApprovementController.create(data, initiator)
    controller = ApprovementController(approvement)
    stages_qs = approvement.stages.values_list('approver', flat=True)
    assert list(stages_qs.current()) == ['a']
    assert list(stages_qs.pending()) == ['', 'b1', 'b2', 'b3', 'c']

    # Ставим ок на первой стадии, чтобы дойти до стадии с запасными согласуюющими
    stage = approvement.stages.get(approver='a')
    controller.approve([stage], 'a')
    assert list(stages_qs.approved()) == ['a']
    assert list(stages_qs.current()) == ['', 'b1']
    assert list(stages_qs.pending()) == ['b2', 'b3', 'c']

    # Считаем, что мы позвали запасного согласующего
    stage = approvement.stages.get(approver='b2')
    stage.status = APPROVEMENT_STAGE_STATUSES.current
    stage.save()
    assert list(stages_qs.approved()) == ['a']
    assert list(stages_qs.current()) == ['', 'b1', 'b2']
    assert list(stages_qs.pending()) == ['b3', 'c']

    # Приостанавливаем согласование
    controller.suspend(initiator)
    assert list(stages_qs.active()) == []
    assert list(stages_qs.approved()) == ['a']
    assert list(stages_qs.suspended()) == ['', 'b1', 'b2', 'b3', 'c']

    # Возобновляем согласование – убеждаемся, что статусы, как до приостановки
    controller.resume(initiator)
    assert list(stages_qs.approved()) == ['a']
    assert list(stages_qs.current()) == ['', 'b1', 'b2']
    assert list(stages_qs.pending()) == ['b3', 'c']
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


@patch('ok.approvements.controllers.ApprovementRequiredNotification', Mock())
@patch('ok.approvements.controllers.ApprovementFinishedNotification', Mock())
@patch('ok.approvements.controllers.ApprovementApprovedByResponsibleNotification', Mock())
def test_current_stages_change_on_reject_with_deputies():
    initiator = 'initiator'
    stages_data = _('a', _('b1', 'b2', 'b3', n=1, d=True), 'c')
    data = {
        'stages': stages_data['stages'],
        'create_comment': False,
        'object_id': 'ISSUE-1',
    }

    approvement = ApprovementController.create(data, initiator)
    controller = ApprovementController(approvement)
    stages_qs = approvement.stages.values_list('approver', flat=True)
    assert list(stages_qs.current()) == ['a']
    assert list(stages_qs.pending()) == ['', 'b1', 'b2', 'b3', 'c']

    # Ставим ок на первой стадии, чтобы дойти до стадии с запасными согласуюющими
    stage = approvement.stages.get(approver='a')
    controller.approve([stage], 'a')
    assert list(stages_qs.approved()) == ['a']
    assert list(stages_qs.current()) == ['', 'b1']
    assert list(stages_qs.pending()) == ['b2', 'b3', 'c']

    # Считаем, что мы позвали запасного согласующего
    stage = approvement.stages.get(approver='b2')
    stage.status = APPROVEMENT_STAGE_STATUSES.current
    stage.save()
    assert list(stages_qs.approved()) == ['a']
    assert list(stages_qs.current()) == ['', 'b1', 'b2']
    assert list(stages_qs.pending()) == ['b3', 'c']

    # Ставим не ок
    stage = approvement.stages.get(approver='b3')
    controller.reject(stage, 'b3')
    assert list(stages_qs.active()) == []
    assert list(stages_qs.approved()) == ['a']
    assert list(stages_qs.rejected()) == ['b3']
    assert list(stages_qs.suspended()) == ['', 'b1', 'b2', 'c']

    # Возобновляем согласование – убеждаемся, что статусы, как до не ока
    controller.resume(initiator)
    assert list(stages_qs.approved()) == ['a']
    assert list(stages_qs.current()) == ['', 'b1', 'b2']
    assert list(stages_qs.pending()) == ['b3', 'c']
    assert list(stages_qs.suspended()) == []
