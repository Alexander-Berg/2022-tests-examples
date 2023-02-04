import datetime

import pytest
from unittest.mock import patch, call, ANY
from freezegun import freeze_time
from dateutil.parser import parse

from watcher.tasks import (
    create_problem_notifications,
    create_shift_start_soon_notifications,
    send_notifications,
)
from watcher import enums
from watcher.config import settings
from watcher.logic.timezone import now
from watcher.db import (
    Notification,
    Role,
)


def test_create_shift_start_soon_notifications(scope_session, staff_factory, schedule_factory, shift_factory):
    schedule = schedule_factory(
        days_for_notify_of_begin=[
            datetime.timedelta(days=0),
            datetime.timedelta(days=3),
            datetime.timedelta(days=7),
        ]
    )
    staff = staff_factory()

    # scheduled смена и начинается через 3 дня (как в настройках) - отправляем
    shift_3_days = shift_factory(
        schedule=schedule,
        staff=staff,
        start=now() + datetime.timedelta(days=3)
    )

    # активная смена - не отправляем
    shift_factory(
        schedule=schedule,
        slot=shift_3_days.slot,
        staff=staff,
        status=enums.ShiftStatus.active,
        start=now() + datetime.timedelta(days=3)
    )

    # начинается через 3 дня, но стафф нет - не отправляем
    shift_factory(
        slot=shift_3_days.slot,
        schedule=schedule,
        start=now() + datetime.timedelta(days=3)
    )

    # начинается через 4 дня - не отправляем
    shift_factory(
        slot=shift_3_days.slot,
        schedule=schedule,
        start=now() + datetime.timedelta(days=4)
    )

    # смена начинается сегодня - не отправляем, по ней отправит start_shift
    shift_factory(
        slot=shift_3_days.slot,
        schedule=schedule,
        staff=staff,
        start=now(),
    )

    # смена начинается через 7 дней, но есть подсмены - не отправляем
    shift_with_replace = shift_factory(
        slot=shift_3_days.slot,
        schedule=schedule,
        staff=staff,
        start=now() + datetime.timedelta(days=7)
    )

    # а вот по ее подсмене - отправляем
    sub_shift = shift_factory(
        slot=shift_3_days.slot,
        schedule=schedule,
        staff=staff_factory(),
        replacement_for=shift_with_replace,
        start=now() + datetime.timedelta(days=7)
    )

    # но только по той, которая начинается в нужный день
    shift_factory(
        slot=shift_3_days.slot,
        schedule=schedule,
        staff=staff,
        replacement_for=shift_with_replace,
        start=now() + datetime.timedelta(days=8)
    )

    schedule_wo_notify = schedule_factory(
        days_for_notify_of_begin=None
    )

    # вообще нет настроек у расписания - не отправляем
    shift_factory(
        schedule=schedule_wo_notify,
        staff=staff,
        start=now() + datetime.timedelta(days=3)
    )

    assert not scope_session.query(Notification).count()
    current_now = now()
    with freeze_time(current_now, tz_offset=3):
        create_shift_start_soon_notifications()

    assert scope_session.query(Notification).count() == 2
    shift3_notification = shift_3_days.notifications[0]
    assert shift3_notification.staff == shift_3_days.staff
    assert shift3_notification.valid_to.replace(tzinfo=None) == (
        current_now + datetime.timedelta(hours=settings.START_SOON_NOTIFY_VALID)
    ).replace(tzinfo=None)
    assert shift3_notification.type == enums.NotificationType.start_shift_soon
    assert shift3_notification.send_at.replace(tzinfo=None) == current_now.replace(tzinfo=None)
    assert not shift3_notification.processed_at

    sub_shift_notification = sub_shift.notifications[0]
    assert sub_shift_notification.staff == sub_shift.staff


@pytest.mark.parametrize('responsible_type', ('schedule', 'responsible_for_duty', 'responsible'))
def test_create_problem_notifications(scope_session, schedule_factory, shift_factory, staff_factory, problem_factory,
                                      schedule_responsible_factory, member_factory, role_factory, responsible_type):
    current_now = now()

    schedule = schedule_factory(days_for_notify_of_problems=datetime.timedelta(days=7))

    staff_schedule_resp = [staff_factory() for _ in range(3)]
    staff_resp_for_duty = [staff_factory() for _ in range(3)]
    staff_resp = [staff_factory() for _ in range(3)]

    # создаем ответственных
    staff = staff_factory()
    if responsible_type == 'schedule':
        [schedule_responsible_factory(
            schedule=schedule, responsible=staff
        ) for staff in staff_schedule_resp]
    elif responsible_type in ('schedule', 'responsible_for_duty'):
        [member_factory(
            role=role_factory(code=Role.RESPONSIBLE_FOR_DUTY),
            staff=staff, service=schedule.service,
        ) for staff in staff_resp_for_duty]
    if responsible_type in ('schedule', 'responsible_for_duty', 'responsible'):
        [member_factory(
            role=role_factory(code=Role.RESPONSIBLE),
            staff=staff, service=schedule.service,
        ) for staff in staff_resp]

    responsible_ids = None
    if responsible_type == 'schedule':
        responsible_ids = staff_schedule_resp
    elif responsible_type == 'responsible_for_duty':
        responsible_ids = staff_resp_for_duty
    elif responsible_type == 'responsible':
        responsible_ids = staff_resp

    shift_early = shift_factory(
        schedule=schedule,
        start=current_now + datetime.timedelta(days=14),
    )
    shifts = [
        shift_factory(
            schedule=schedule, staff=staff,
            start=current_now + datetime.timedelta(days=3),
        ),
        shift_factory(
            schedule=schedule, staff=staff,
            start=current_now - datetime.timedelta(days=3),
        )
    ]

    # по этом проблеме не будет уведомления
    problem_factory(shift=shift_early)

    # по каждой из этих проблмем должно быть len(responsible_ids) уведомлений
    problem_factory(
        reason=enums.ProblemReason.nobody_on_duty,
        shift=shifts[0],
    )
    problem_factory(
        reason=enums.ProblemReason.staff_has_gap,
        shift=shifts[1],
        staff=staff,
    )

    assert not scope_session.query(Notification).count()
    with freeze_time(current_now, tz_offset=3):
        create_problem_notifications()

    assert scope_session.query(Notification).count() == 2 * len(responsible_ids)
    assert len(shift_early.notifications) == 0
    assert len(shifts[0].notifications) == len(responsible_ids)
    assert len(shifts[1].notifications) == len(responsible_ids)

    notification_types = [
        enums.NotificationType.problem_nobody_on_duty,
        enums.NotificationType.problem_staff_has_gap,
    ]

    for i, shift in enumerate(shifts):
        for j, responsible in enumerate(responsible_ids):
            assert shift.notifications[j].staff_id == responsible.id
            assert shift.notifications[j].type == notification_types[i]
            assert shift.notifications[j].valid_to.replace(tzinfo=None) == shift.end.replace(tzinfo=None)


def test_problem_notifications_repeated(scope_session, problem_factory, staff_factory, schedule_factory, shift_factory,
                                        schedule_responsible_factory):
    current_now = now()

    responsible = staff_factory()
    schedule = schedule_factory(days_for_notify_of_problems=datetime.timedelta(days=7))
    schedule_responsible_factory(
        schedule=schedule, responsible=responsible
    )

    shift = shift_factory(
        schedule=schedule,
        start=current_now + datetime.timedelta(days=3),
    )
    problem_factory(shift=shift)

    with freeze_time(current_now, tz_offset=3):
        create_problem_notifications()

    assert scope_session.query(Notification).count() == 1
    assert len(shift.notifications) == 1
    notification_id = shift.notifications[0].id

    with freeze_time(current_now + datetime.timedelta(days=1), tz_offset=3):
        create_problem_notifications()

    assert scope_session.query(Notification).count() == 1
    assert len(shift.notifications) == 1
    assert shift.notifications[0].id == notification_id


def test_send_notifications(
    scope_session, bot_user_factory, notification_factory,
    shift_factory, staff_factory
):
    staff = staff_factory()
    shift = shift_factory(staff=staff)
    # send_at в будущем - не отправляем уведомление
    notification_factory(
        shift=shift,
        staff=staff,
        send_at=now() + datetime.timedelta(minutes=15),
    )

    # уже отправлено - не отправляем повторно
    already_send = notification_factory(
        state=enums.NotificationState.send,
        shift=shift,
        staff=staff,
    )

    # уже не актуально - не отправляем
    outdated = notification_factory(
        shift=shift,
        staff=staff,
        valid_to=now() - datetime.timedelta(hours=1),
    )

    # отправляем уведомление по скорому старту смены
    shift_soon = shift_factory(
        slot=shift.slot,
        staff=staff_factory(),
        start=parse('2022-01-05T14:42'),
        is_primary=False,
    )
    send_start_soon = notification_factory(
        type=enums.NotificationType.start_shift_soon,
        shift=shift_soon,
        staff=shift_soon.staff,
    )

    # отправляем уведомление по смене которая начинается сейчас
    send_start_now = notification_factory(
        type=enums.NotificationType.start_shift,
        shift=shift,
        staff=staff,
    )
    bot_user_factory(staff=staff)

    with patch('watcher.tasks.notification.jns_client.send_message') as mock_send_message:
        send_notifications()

    mock_send_message.assert_has_calls(
        [
            call(
                template='duty_start_notification',
                channel='telegram', login=send_start_now.staff.login,
                params={
                    'duty_name': {'string_value': shift.schedule.name},
                    'duty_link': {
                        'string_value': f'https://abc.test.yandex-team.ru/services/{shift.schedule.service.slug}/duty2/{shift.schedule.id}/'
                    },
                    'service_name': {'string_value': shift.schedule.service.name},
                    'service_link': {
                        'string_value': f'https://abc.test.yandex-team.ru/services/{shift.schedule.service.slug}/'
                    },
                    'shift_type': {
                        'string_value': 'primary-дежурство'
                    },
                },
                request_id=ANY,
            ),
            call(
                template='duty_start_notification_soon',
                channel='email', login=settings.ROBOT_LOGIN,
                params={
                    'duty_name': {'string_value': shift_soon.schedule.name},
                    'duty_link': {
                        'string_value': f'https://abc.test.yandex-team.ru/services/{shift_soon.schedule.service.slug}/duty2/{shift_soon.schedule.id}/'
                    },
                    'service_name': {'string_value': shift_soon.schedule.service.name},
                    'service_link': {
                        'string_value': f'https://abc.test.yandex-team.ru/services/{shift_soon.schedule.service.slug}/'
                    },
                    'shift_type': {
                        'string_value': 'backup-дежурство'
                    },
                    'date': {'string_value': '2022-01-05'},
                    'time': {'string_value': '14:42'},
                },
                request_id=ANY,
            ),
        ],
        any_order=True,
    )

    assert {obj.id for obj in scope_session.query(Notification).filter(
        Notification.state==enums.NotificationState.send
    )} == {send_start_now.id, send_start_soon.id, already_send.id}

    assert {obj.id for obj in scope_session.query(Notification).filter(
        Notification.state==enums.NotificationState.outdated
    )} == {outdated.id}


def test_send_problem_notifications(scope_session, bot_user_factory, notification_factory, shift_factory, staff_factory):
    bot_user = bot_user_factory(staff=staff_factory())

    shift = shift_factory(
        staff=staff_factory(),
    )

    nobody_on_duty = notification_factory(
        type=enums.NotificationType.problem_nobody_on_duty,
        shift=shift, staff=bot_user.staff,
    )

    staff_has_gap = notification_factory(
        type=enums.NotificationType.problem_staff_has_gap,
        shift=shift, staff=bot_user.staff
    )

    with patch('watcher.tasks.notification.jns_client.send_message') as mock_send_message:
        send_notifications()

    mock_send_message.assert_has_calls(
        [
            call(
                template='duty_problem_nobody_on_duty',
                channel='telegram', login=bot_user.staff.login,
                params={
                    'duty_name': {'string_value': shift.schedule.name},
                    'duty_link': {
                        'string_value': f'https://abc.test.yandex-team.ru/services/{shift.schedule.service.slug}/duty2/{shift.schedule.id}/'
                    },
                    'service_name': {'string_value': shift.schedule.service.name},
                    'service_link': {
                        'string_value': f'https://abc.test.yandex-team.ru/services/{shift.schedule.service.slug}/'
                    },
                    'shift_start': {
                        'string_value': shift.start.strftime('%Y-%m-%d %H:%M:%S')
                    },
                    'shift_end': {
                        'string_value': shift.end.strftime('%Y-%m-%d %H:%M:%S')
                    },
                },
                request_id=ANY,
            ),
            call(
                template='duty_problem_staff_has_gap',
                channel='telegram', login=bot_user.staff.login,
                params={
                    'duty_name': {'string_value': shift.schedule.name},
                    'duty_link': {
                        'string_value': f'https://abc.test.yandex-team.ru/services/{shift.schedule.service.slug}/duty2/{shift.schedule.id}/'
                    },
                    'service_name': {'string_value': shift.schedule.service.name},
                    'service_link': {
                        'string_value': f'https://abc.test.yandex-team.ru/services/{shift.schedule.service.slug}/'
                    },
                    'staff_link': {
                        'string_value': f'https://staff.yandex-team.ru/{shift.staff.login}/'
                    },
                    'staff_name': {
                        'string_value': ' '.join([shift.staff.first_name, shift.staff.last_name])
                    },
                    'shift_start': {
                        'string_value': shift.start.strftime('%Y-%m-%d %H:%M:%S')
                    },
                    'shift_end': {
                        'string_value': shift.end.strftime('%Y-%m-%d %H:%M:%S')
                    },
                },
                request_id=ANY,
            ),
        ],
        any_order=True,
    )

    assert {obj.id for obj in scope_session.query(Notification).filter(
        Notification.state == enums.NotificationState.send
    )} == {nobody_on_duty.id, staff_has_gap.id}
