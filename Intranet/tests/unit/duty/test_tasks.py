import mock
import pretend
import pytest
import datetime
from collections import defaultdict
from datetime import time

from django.conf import settings
from django.core.urlresolvers import reverse
from django.test.utils import override_settings
from django.utils import timezone
from freezegun import freeze_time
from mock import call, patch
from waffle.testutils import override_switch

from plan.common.utils.http import Session
from plan.common.utils.oauth import get_abc_zombik
from plan.common.utils import timezone as utils
from plan.duty.models import Gap, Shift, Problem, Schedule, Order
from plan.duty.schedulers.auto_ordering_scheduler import AutoOrderingScheduler
from plan.duty.tasks import (
    start_shifts,
    finish_shifts,
    sync_with_gap,
    send_notification_duty,
    send_notifications_problems,
    recalculate_all_duties,
    recalculate_duty_for_service,
    autoapprove_shifts,
    check_current_shifts,
    check_current_shifts_for_service,
    remove_dangling_duty_members,
    upload_duty_to_yt,
    sync_important_schedules,
)
from plan.holidays.models import Holiday
from plan.idm.exceptions import IDMError
from plan.roles.models import Role
from plan.services.models import (
    Service,
    ServiceNotification,
    ServiceMember,
)
from plan.services.state import SERVICEMEMBER_STATE
from common import factories
from common.intranet import deprive_member_side_effect

pytestmark = [pytest.mark.django_db(transaction=True)]


@pytest.fixture
def duty_data(owner_role, staff_factory):
    owner = staff_factory()
    service = factories.ServiceFactory(owner=owner)
    factories.ServiceMemberFactory(service=service, staff=owner, role=owner_role)
    role = factories.RoleFactory()

    return pretend.stub(owner=owner, service=service, role=role)


def request_member_side_effect(service, staff, role, *args, **kwargs):
    members = ServiceMember.all_states.filter(service=service, staff=staff, role=role, from_department=None, resource=None)
    if members.exists():
        members.update(state=ServiceMember.states.ACTIVE, autorequested=True)
    else:
        factories.ServiceMemberFactory(
            service=service, staff=staff, role=role, state=ServiceMember.states.ACTIVE, autorequested=True)
    return {'id': 1}


def test_shift_factory():
    schedule = factories.ScheduleFactory(start_time=time(3))
    shift1 = factories.ShiftFactory(schedule=schedule)
    assert shift1.schedule.start_time == time(hour=3)


def workdays():
    list_day = [9, 10, 11, 14, 15, 16, 17, 18, 21, 22, 23, 24, 25, 28, 29, 30, 31]
    list_date = []
    for day in list_day:
        list_date.append(timezone.datetime(2019, 1, day).date())
    return list_date


@pytest.mark.parametrize(('today_datetime', 'started'), [
    ('2019-12-31T23:00:00+03:00', False),  # до начала дежурства 1 час
    ('2019-12-31T23:30:00+03:00', True),  # до начала дежурства полчаса
    ('2020-01-01T00:00:00+03:00', True),  # начало
    ('2020-01-01T00:30:00+03:00', True),  # после начала
])
@pytest.mark.parametrize('member_exists', [True, False])
def test_start_shifts(duty_role, today_datetime, started, member_exists, django_assert_num_queries):
    """
    Выдаём роль за полчаса до начала дежурства (см. параметр SHIFT_BEGIN_BEFORE_DUTY_START_TIMEDELTA).
    Стандартное время старта смены - 00:00 по Мск (см. параметр DEFAULT_DUTY_START_TIME).

    @param started: удовлетворяет ли текущее время условиям выдачи дежурной роли,
        должен ли шиыт стартовать
    @param member_exists: роль, которая выдаётся на время дежурство,
        может существовать в сервисе до старта смены
    """

    with freeze_time(today_datetime):
        start = timezone.datetime(2020, 1, 1).date()
        schedule = factories.ScheduleFactory(
            start_date=start,
            role=factories.RoleFactory(),
            tracker_component_id=123,
            tracker_queue_id=1234,
        )

        for _ in range(3):
            factories.ServiceMemberFactory(service=schedule.service, role=schedule.role)

        recalculate_duty_for_service(service_id=schedule.service.id)
        next_shift = Shift.objects.select_related('staff').get(start=start, schedule=schedule)
        assert next_shift.state == Shift.SCHEDULED

        if member_exists:
            factories.ServiceMemberFactory(service=schedule.service, role=duty_role, staff=next_shift.staff)

        role_request_count = (
            schedule.shifts
            .startable()
            .with_staff()
            .annotate_member_id()
            .filter(member_id=None)
            .count()
        )

        schedules = Schedule.objects.filter(pk=schedule.pk)
        num_queries = 2 + started * (1 + 3 * role_request_count)
        with patch('plan.api.idm.actions.request_membership') as request_membership:
            with patch('plan.duty.models.set_component_lead') as mock_set_component_lead:
                request_membership.return_value = {'id': 1}
                with django_assert_num_queries(num_queries):
                    # 1 - select role
                    # 1 - select shifts
                    # если роль выдаётся, дополнительно на каждую роль:
                    # 1 - select servicemember - проверить существование роли
                    # 1 - insert services_servicemember (with state='requested')
                    # 1 - update services_servicemember (сохранить role_id из idm)
                    # и общие запросы для всех шифтов, где выдалась роль
                    # 1 - update shifts
                    start_shifts(schedules)

        if not started:
            request_membership.call_count == 0
            mock_set_component_lead.assert_not_called()

        else:
            if not member_exists:
                duty_member_roles = ServiceMember.all_states.filter(
                    role=duty_role, autorequested=True,
                    state=SERVICEMEMBER_STATE.REQUESTED,
                    idm_role_id=1,
                )
                assert duty_member_roles.count() == 1

                assert request_membership.call_count == 1
                expected_args_list = [
                    call(schedule.service, next_shift.staff, duty_role, comment='Начало дежурства', silent=True)
                ]
                assert request_membership.call_args_list == expected_args_list

            else:
                request_membership.call_count == 0

            mock_set_component_lead.assert_called_once_with(component_id=123, staff=next_shift.staff)
            next_shift.refresh_from_db()
            assert next_shift.state == Shift.STARTED


def test_start_shifts_without_role(duty_role):
    with freeze_time('2020-01-01T00:00:00+03:00'):
        start = timezone.datetime(2020, 1, 1).date()
        schedule = factories.ScheduleFactory(
            start_date=start,
            role=factories.RoleFactory(),
            role_on_duty=duty_role,
        )

        for _ in range(3):
            factories.ServiceMemberFactory(service=schedule.service, role=schedule.role)

        recalculate_duty_for_service(service_id=schedule.service.id)
        next_shift = Shift.objects.select_related('staff').get(start=start, schedule=schedule)
        next_shift.role = None
        next_shift.save()
        assert next_shift.state == Shift.SCHEDULED

        with patch('plan.api.idm.actions.request_membership', mock.Mock(return_value={'id': 1})) as request_membership:
            start_shifts(Schedule.objects.filter(pk=schedule.pk))

        duty_member_roles = ServiceMember.all_states.filter(
            role=duty_role, autorequested=True,
            state=ServiceMember.states.REQUESTED,
        )
        assert duty_member_roles.count() == 1

        assert request_membership.call_count == 1
        expected_args_list = [
            call(schedule.service, next_shift.staff, duty_role, comment='Начало дежурства', silent=True)
        ]
        assert request_membership.call_args_list == expected_args_list

        next_shift.refresh_from_db()
        assert next_shift.role == duty_role
        assert next_shift.state == Shift.STARTED


@freeze_time('2020-01-02T00:00:01')
def test_start_yesterday_shift(duty_role):
    now = timezone.now()
    delta = timezone.timedelta(minutes=5)
    start_dt = now - delta
    schedule = factories.ScheduleFactory(start_date=start_dt,
                                         start_time=start_dt.time())

    end_date = start_dt.date()
    shift = factories.ShiftFactory(state=Shift.SCHEDULED,
                                   start=end_date,
                                   end=end_date,
                                   schedule=schedule)

    with patch('plan.api.idm.actions.request_membership', mock.Mock(return_value={'id': 1})) as request_membership:
        start_shifts(Schedule.objects.filter(pk=schedule.pk))

    expected_args_list = [
        call(shift.service, shift.staff, duty_role, comment='Начало дежурства', silent=True)
    ]
    assert request_membership.call_args_list == expected_args_list
    duty_member = ServiceMember.all_states.get(service=schedule.service, role=duty_role)
    assert duty_member.state == SERVICEMEMBER_STATE.REQUESTED

    shift.refresh_from_db()
    assert shift.state == Shift.STARTED


@pytest.mark.parametrize('recalculate', [True, False])
def test_no_recalculation(recalculate):
    # проверяем что если пересчет выключен - уведомления будут отправлены, но в самой
    # задача на пересчет schedule участвовать не будет
    start = timezone.datetime(2020, 1, 1).date()
    schedule = factories.ScheduleFactory(
        start_date=start,
        recalculate=recalculate,
        role=factories.RoleFactory(),
    )

    with patch.object(AutoOrderingScheduler, 'compatible_with_schedule') as scheduler:
        with patch('plan.duty.tasks.notify_staff_duty') as notify:
            recalculate_duty_for_service(service_id=schedule.service.id)
            notify.delay_on_commit.assert_called_once_with(schedule.service.id, schedule.service.slug)
            assert scheduler.called is recalculate


@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=10))
def test_no_recalculation_update_problems():
    """
    Если график без пересчета, проблемы должны создаваться.
    """
    schedule = factories.ScheduleFactory(role=factories.RoleFactory())
    for _ in range(3):
        factories.ServiceMemberFactory(service=schedule.service, role=schedule.role)

    recalculate_duty_for_service(service_id=schedule.service.id)
    shift = schedule.shifts.order_by('start').first()
    factories.GapFactory(staff=shift.staff, start=shift.start, end=shift.end)
    assert not shift.has_problems

    schedule.recalculate = False
    schedule.save()

    recalculate_duty_for_service(service_id=schedule.service.id)
    shift.refresh_from_db()
    assert shift.has_problems


@freeze_time('2020-01-01T00:00:00')
@pytest.mark.parametrize('autorequested, error, member_state, shift_state', [
    (True, False, SERVICEMEMBER_STATE.DEPRIVING, Shift.FINISHED),
    (True, True, SERVICEMEMBER_STATE.DEPRIVING, Shift.STARTED),
    (False, False, SERVICEMEMBER_STATE.ACTIVE, Shift.FINISHED),
])
def test_finish_shifts(duty_role, django_assert_num_queries, autorequested, error, member_state, shift_state):
    """
    если смена заканчилась раньше, чем за SHIFT_FINISH_AFTER_DUTY_START_TIMEDELTA до текущего момента,
     то надо отозвать права
    """
    now = timezone.now()
    delta = timezone.timedelta(minutes=5)
    start_dt = now - settings.SHIFT_FINISH_AFTER_DUTY_START_TIMEDELTA-delta
    schedule = factories.ScheduleFactory(start_time=start_dt.time(), tracker_component_id=123, tracker_queue_id=1234)
    # старая смена должна заканчиваться не раньше начала новой
    factories.ShiftFactory(state=Shift.STARTED,
                                           start=start_dt.date(),
                                           schedule=schedule)
    factories.ShiftFactory(state=Shift.SCHEDULED, schedule=schedule)
    ended_shift = factories.ShiftFactory(
        start=start_dt-timezone.timedelta(5),
        state=Shift.STARTED,
        end=start_dt.date()-timezone.timedelta(1),
        schedule=schedule
    )
    member = factories.ServiceMemberFactory(
        service=ended_shift.service, staff=ended_shift.staff, role=duty_role, autorequested=autorequested)
    ended_shift.save()
    def side_effect(*args, **kwargs):
        if error:
            raise IDMError()
    with patch('plan.api.idm.actions.deprive_role') as deprive_role:
        with patch('plan.duty.models.remove_component_lead') as mock_remove_component_lead:
            deprive_role.side_effect = side_effect
            with django_assert_num_queries(4):
                # 2 - select members
                # 1 - update shifts
                # 1 - update members
                finish_shifts(Schedule.objects.filter(pk=schedule.pk))

    if autorequested:
        assert deprive_role.call_args_list == [call(member, comment='Конец дежурства')]
    else:
        deprive_role.assert_not_called()

    mock_remove_component_lead.assert_not_called()
    member = ServiceMember.all_states.get(pk=member.pk)
    assert member.state == member_state
    ended_shift.refresh_from_db()
    assert ended_shift.state == shift_state


@freeze_time('2020-01-01T12:00:00')
def test_finish_shift_with_one_day_duration(duty_role):
    """
    права не должны отзываться у нового дежурного через час после начала дежурства
    """
    now = timezone.now()
    delta = timezone.timedelta(minutes=180)
    start_dt = now - delta
    schedule = factories.ScheduleFactory(start_date=start_dt,
                                         start_time=start_dt.time())

    shift = factories.ShiftFactory(state=Shift.STARTED,
                                   start=start_dt.date(),
                                   end=start_dt.date(),
                                   schedule=schedule)
    factories.ShiftFactory(state=Shift.STARTED,
                           start=start_dt.date(),
                           end=start_dt.date()+timezone.timedelta(5),
                           schedule=schedule)
    factories.ServiceMemberFactory(service=shift.service, staff=shift.staff, role=duty_role)
    shift.save()

    with patch('plan.api.idm.actions.deprive_role') as deprive_role:
        finish_shifts(Schedule.objects.filter(pk=schedule.pk))

    assert deprive_role.call_args_list == []


class GapsMockResponse:
    pages = [
        {
            'gaps': [
                {
                    'id': 10,
                    'person_login': 'ivanushka',
                    'date_from': '2018-01-05T12:00:00',
                    'date_to': '2018-01-07T12:00:00',
                    'workflow': 'type',
                    'work_in_absence': False,
                    'full_day': True
                },
                {
                    'id': 9999,
                    'person_login': 'ivanushka',
                    'date_from': '2018-01-01T12:00:00',
                    'date_to': '2018-02-01T12:00:00',
                    'workflow': 'type',
                    'work_in_absence': False,
                    'full_day': True,
                }
            ],
            'page': 0,
            'pages': 2
        },
        {
            'gaps': [
                {
                    'id': 12,
                    'person_login': 'alenushka',
                    'date_from': '2018-02-01T12:00:00',
                    'date_to': '2018-02-02T12:00:00',
                    'workflow': 'type',
                    'work_in_absence': False,
                    'full_day': True
                }
            ],
            'page': 1,
            'pages': 2
        }
    ]
    page = 0

    def __init__(self, *args, **kwargs):
        pass

    @classmethod
    def json(cls):
        cls.page += 1
        return cls.pages[cls.page - 1]


def test_sync_with_gap(django_assert_num_queries):
    zombik = get_abc_zombik()
    zombik.delete()

    ivanushka = factories.StaffFactory(id=10, login='ivanushka')
    alenushka = factories.StaffFactory(id=11, login='alenushka')
    petrushka = factories.StaffFactory(id=12, login='petrushka')
    gap_1 = factories.GapFactory(gap_id=10, staff=ivanushka)
    gap_2 = factories.GapFactory(gap_id=11, staff=petrushka)
    gap_3 = factories.GapFactory(gap_id=12, staff=alenushka, status=Gap.DELETED)

    num_queries = 11
    # 2 to get staffs
    # 1 to get gaps
    # 2 to update existing gaps
    # 1 to delete gaps
    # 1 to create gaps
    # 4 for taskmetric

    with mock.patch.object(Session, 'get', new=GapsMockResponse):
        with django_assert_num_queries(num_queries):
            sync_with_gap()

    assert Gap.objects.active().count() == 3
    gap_1.refresh_from_db()
    assert gap_1.status == Gap.ACTIVE
    assert gap_1.staff == ivanushka
    assert gap_1.start == timezone.datetime(2018, 1, 5, 12, 0, 0, tzinfo=timezone.utc)
    assert gap_1.end == timezone.datetime(2018, 1, 7, 12, 0, 0, tzinfo=timezone.utc)
    gap_2.refresh_from_db()
    assert gap_2.status == Gap.DELETED
    gap_3.refresh_from_db()
    assert gap_3.staff == alenushka
    assert gap_3.status == Gap.ACTIVE

    gap = Gap.objects.get(gap_id=9999)
    assert gap.staff == ivanushka
    assert gap.start == timezone.datetime(2018, 1, 1, 12, 0, 0, tzinfo=timezone.utc)
    assert gap.end == timezone.datetime(2018, 2, 1, 12, 0, 0, tzinfo=timezone.utc)
    assert gap.type == 'type'
    assert not gap.work_in_absence
    assert gap.full_day


def test_cancel_shift_with_intersection(duty_role):
    shift = factories.ShiftFactory(state=Shift.STARTED)
    factories.ServiceMemberFactory(service=shift.service, staff=shift.staff, role=duty_role)
    shift.save()
    factories.ShiftFactory(state=Shift.STARTED, staff=shift.staff, schedule=shift.schedule)
    with patch('plan.api.idm.actions.deprive_role') as deprive_role:
        shift.cancel()
        shift.save()
        assert deprive_role.call_args_list == []


@pytest.fixture
def notification_duty_data(staff_factory):
    # Четвертое января это пятница
    service = factories.ServiceFactory(name='B', slug='b', owner=staff_factory())
    schedule = factories.ScheduleFactory(
        start_date='2019-01-01',
        service=service,
        duration=timezone.timedelta(days=1),
        duty_on_weekends=True,
        role=factories.RoleFactory(),
        name='A',
        autoapprove_timedelta=timezone.timedelta(0),
    )
    return pretend.stub(
        service=service,
        schedule=schedule,
    )


@pytest.mark.parametrize('today', ['2019-01-04T00:00:00', '2019-01-11T00:00:00'])
@pytest.mark.parametrize('algorithm', [Schedule.MANUAL_ORDER, Schedule.NO_ORDER])
@override_switch('dont_send_email', False)
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=30))
def test_send_notification_duty(today, algorithm,  mailoutbox, notification_duty_data):
    """
    В этом тесте замокали период, на который создаются смены: вместо полгода - 30 дней.

    2019-01-04:
        Если сегодня выходной, не отправляем ничего, кроме непосредственного старта смены.

    2019-01-11:
        Если сегодня обычный будень, прислаем уведомления в обычном режиме.
        11 января - пятница, присылаем так же за выходные и пн.
        В уведомлениях смены:
            * 11 января - начало смены, стафф 0
            * 12 января - за 1 день, стафф 1
            * 13 января - за 1 день, стафф 2
            * 14 января - за 1 день, стафф 3
            * 18 января - за 7 дней, стафф 3
            * 19 января - за 7 дней, стафф 0
            * 20 января - за 7 дней, стафф 1

        Итого, 4 письма:
            для 0: сейчас начало (11 янв) + скоро (19 янв)
            для 1: скоро (12 янв) + скоро (20 янв)
            для 2: скоро (13 янв)
            для 3: скоро (14 янв) + скоро (18 янв)
    """

    with freeze_time('2019-01-01T00:00:00'):
        role = factories.RoleFactory()
        service = notification_duty_data.service
        schedule = notification_duty_data.schedule
        for i in range(4):
            member = factories.ServiceMemberFactory(role=role, service=service)
            if algorithm == Schedule.MANUAL_ORDER:
                factories.OrderFactory(schedule=schedule, staff=member.staff, order=i)

        schedule.role = role
        if algorithm == Schedule.MANUAL_ORDER:
            schedule.algorithm = Schedule.MANUAL_ORDER
        schedule.save()

        other_schedule = factories.ScheduleFactory(
            start_date='2019-01-01',
            days_for_begin_shift_notification=[2, 3],
        )

    with freeze_time(today):
        with patch('plan.holidays.calendar.workdays_of_month') as workdays_of_month:
            today = utils.today()
            workdays_of_month.return_value = workdays()

            recalculate_duty_for_service(other_schedule.service.id)
            recalculate_duty_for_service(service.id)

            today_shift = schedule.shifts.get(start=today)
            if today_shift.staff is None and algorithm == Schedule.MANUAL_ORDER:
                # непонятный баг при наличии ещё одного графика: None у текущей смены
                # нужно поисследовать отдельно
                today_shift.staff = schedule.orders.get(order=2).staff
                today_shift.save()

            send_notification_duty()

            if today == timezone.datetime(2019, 1, 4).date():
                mail_count = 1
            else:
                mail_count = 4

            assert len(mailoutbox) == mail_count

            # Проверяем, что второй раз мы не отправляем одно и тоже
            send_notification_duty()
            assert len(mailoutbox) == mail_count

    recipients_to_mail = defaultdict(list)
    for email in mailoutbox:
        for recipient in email.recipients():
            recipients_to_mail[recipient].append(email)

    assert len(recipients_to_mail[today_shift.staff.email]) == 1
    body = recipients_to_mail[today_shift.staff.email][0].body
    assert 'Прямо сейчас начинаются дежурства' in body
    if mail_count == 4:
        assert 'Скоро начнутся дежурства' in body
        assert '19 января' in body
        assert body.count('января') == 1

    for days in range(1, mail_count):
        shift = schedule.shifts.get(start=(today + timezone.timedelta(days=days)))
        assert len(recipients_to_mail[shift.staff.email]) == 1
        body = recipients_to_mail[shift.staff.email][0].body
        assert 'Прямо сейчас начинаются дежурства' not in body
        assert 'Скоро начнутся дежурства' in body

        if days == 1:
            assert '12 января' in body
            assert '20 января' in body
            assert body.count('января') == 2

        elif days == 2:
            assert '13 января' in body
            assert body.count('января') == 1

        elif days == 3:
            assert '14 января' in body
            assert '18 января' in body
            assert body.count('января') == 2


@freeze_time('2019-01-07T12:00:00')
@override_switch('dont_send_email', False)
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=15))
def test_dont_send_notification(mailoutbox, notification_duty_data):
    """
    Не отправляем уведомления если days_for_begin_shift_notification = None
    """

    schedule = notification_duty_data.schedule
    schedule.days_for_begin_shift_notification = None
    schedule.save()
    service = notification_duty_data.service
    for _ in range(2):
        factories.ServiceMemberFactory(role=schedule.role, service=service)

    with patch('plan.holidays.calendar.workdays_of_month') as workdays_of_month:
        workdays_of_month.return_value = workdays()
        send_notification_duty()
        assert len(mailoutbox) == 0


@freeze_time('2019-01-07T12:00:00')
@override_switch('dont_send_email', False)
def test_send_notification_after_change(mailoutbox, notification_duty_data):
    """
    2019-01-07:
        Если сегодня выходной, не отправляем ничего, кроме непосредственного старта смены.
        Но если в шифте обновился стафф, то нужно отправить повторно.
    """

    schedule = notification_duty_data.schedule
    service = notification_duty_data.service
    staffs = [
        factories.ServiceMemberFactory(role=schedule.role, service=service).staff
        for _ in range(2)
    ]

    with patch('plan.holidays.calendar.workdays_of_month') as workdays_of_month:
        workdays_of_month.return_value = workdays()
        send_notification_duty()
        assert len(mailoutbox) == 1

    other_staff = [staff for staff in staffs if staff.email != mailoutbox[0].to[0]][0]
    schedule.shifts.filter(start=timezone.now().date()).update(staff=other_staff)

    with patch('plan.holidays.calendar.workdays_of_month') as workdays_of_month:
        workdays_of_month.return_value = workdays()
        send_notification_duty()
        assert len(mailoutbox) == 2


@pytest.mark.parametrize('today', ['2019-01-07T00:00:00', '2019-01-09T00:00:00'])
@override_switch('dont_send_email', False)
def test_send_notification_customs_settings(today, mailoutbox, notification_duty_data):
    """
    2019-01-07:
        Если сегодня выходной, не отправляем ничего, кроме непосредственного старта смены.

    2019-01-09:
        Если сегодня обычный будень, прислаем уведомления в обычном режиме.
        Уведомления о сменах:
            * 10 января - за 1 день, стафф 0
            * 14 января - за 5 дней, стафф 0

        Итого: 1 письмо.
    """

    with freeze_time(today):
        service = notification_duty_data.service
        schedule = notification_duty_data.schedule
        schedule.days_for_begin_shift_notification = [1, 5]
        schedule.save()
        factories.ServiceMemberFactory(role=schedule.role, service=service)
        today = utils.today()

        with patch('plan.holidays.calendar.workdays_of_month') as workdays_of_month:
            workdays_of_month.return_value = workdays()
            send_notification_duty()

    if today == timezone.datetime(2019, 1, 7).date():
        assert len(mailoutbox) == 0

    else:
        assert len(mailoutbox) == 1
        body = mailoutbox[0].body
        assert 'Скоро начнутся дежурства' in body
        assert body.count('января') == 2
        assert '10 января' in body
        assert '14 января' in body


@freeze_time('2019-01-01')
@override_switch('dont_send_email', False)
def test_send_duty_problem_notification(mailoutbox, notification_duty_data, owner_role):
    service = notification_duty_data.service
    schedule = notification_duty_data.schedule
    another_schedule = factories.ScheduleFactory(
        start_date=schedule.start_date,
        duration=schedule.duration,
        service=service,
    )
    factories.ServiceMemberFactory(role=schedule.role, service=service)
    owner = factories.ServiceMemberFactory(role=owner_role, service=service)
    factories.ServiceMemberFactory(role=another_schedule.role, service=service)

    assert len(mailoutbox) == 0

    shift = schedule.shifts.order_by('start').first()
    shift.has_problems = True
    shift.save()
    Problem.open_shift_problem(shift, Problem.STAFF_HAS_GAP, shift.start_datetime)
    another_shift1 = another_schedule.shifts.get(start=shift.end + timezone.timedelta(days=1))
    another_shift2 = another_schedule.shifts.get(start=shift.end + timezone.timedelta(days=2))
    another_shift3 = another_schedule.shifts.get(start=shift.end + timezone.timedelta(days=3))
    another_shift1.staff = factories.StaffFactory(
        first_name='A',
        last_name='B',
    )
    another_shift2.staff = another_shift1.staff
    another_shift3.staff = factories.StaffFactory(
        first_name='C',
        last_name='D',
    )
    for another_shift in [another_shift1, another_shift2, another_shift3]:
        another_shift.has_problems = True
        another_shift.save()
        Problem.open_shift_problem(another_shift, Problem.STAFF_HAS_GAP, another_shift.start_datetime)

    send_notifications_problems()

    assert len(mailoutbox) == 1

    expected_service = 'В сервисе <b>{}</b> в ABC выявлены следующие проблемы с дежурствами'.format(service.name)
    expected_problem1 = 'Дежурный(A B) не может дежурить всю смену'
    expected_problem2 = 'Дежурный(C D) не может дежурить всю смену'

    assert mailoutbox[0].to == [owner.staff.email]
    assert mailoutbox[0].subject == 'Проблемы с дежурствами в сервисах'
    assert expected_service in mailoutbox[0].body
    assert expected_problem1 in mailoutbox[0].body
    assert expected_problem2 in mailoutbox[0].body
    assert schedule.name in mailoutbox[0].body


@freeze_time('2020-09-01')
@override_switch('dont_send_email', False)
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=20))
def test_send_duty_problem_notification_splice(mailoutbox, owner_role):
    """
    В этом тесте замокан период, на который рассчитываем смены.
    Протеструем склейку проблем.
    Склеиваться должны проблемы и парараллельные, и последовательные.
    Смены из разныех графиков не склеиваются.
    """

    service = factories.ServiceFactory()
    schedule = factories.ScheduleFactory(
        start_date='2020-09-01',
        service=service,
        persons_count=3,

    )
    another_schedule = factories.ScheduleFactory(
        start_date='2020-09-01',
        service=service,
    )
    factories.ServiceMemberFactory(role=owner_role, service=service, staff=service.owner)

    assert len(mailoutbox) == 0
    recalculate_duty_for_service(service.id)
    send_notifications_problems()

    # Проверяем, что шифты разных графиков не слиплись в одну ячейку несмотря на то, что идут подряд
    # А шифты одного графика слиплись, так как идут подряд и имеют одного стаффа
    assert mailoutbox[0].body.count('Дежурный не назначен') == 2
    assert mailoutbox[0].body.count('с 1 сентября 2020 г. по 15 сентября 2020 г.') == 2
    assert mailoutbox[0].body.count('(Проблемных смен: 3)') == 1
    assert mailoutbox[0].body.count('(Проблемных смен: 9)') == 1
    assert schedule.name in mailoutbox[0].body
    assert another_schedule.name in mailoutbox[0].body


@freeze_time('2019-01-01')
@override_switch('dont_send_email', False)
def test_send_duty_problem_notification_many(mailoutbox, notification_duty_data, owner_role):
    """Имитируем ситуацию когда формирование рассылки вызывается чаще чем фактически отправляются письма"""
    service = notification_duty_data.service
    schedule = notification_duty_data.schedule
    another_schedule = factories.ScheduleFactory(
        start_date=schedule.start_date,
        duration=schedule.duration,
        service=service,
    )
    factories.ServiceMemberFactory(role=schedule.role, service=service)
    factories.ServiceMemberFactory(role=owner_role, service=service)
    factories.ServiceMemberFactory(role=another_schedule.role, service=service)
    Shift.objects.all().update(has_problems=False)
    Problem.objects.all().delete()

    with patch('plan.duty.tasks.send_notification_staffs'):
        shift = schedule.shifts.first()
        shift.has_problems = True
        shift.save()
        Problem.open_shift_problem(shift, Problem.NOBODY_ON_DUTY, shift.start_datetime)
        send_notifications_problems()

    send_notifications_problems()
    assert len(mailoutbox) == 1
    # Только один раз упоминается шифт в письме
    assert len(mailoutbox[0].body.split('Дежурный не назначен')) == 2


@freeze_time('2019-01-01')
@override_switch('dont_send_email', False)
def test_send_duty_problem_notification_past(mailoutbox, notification_duty_data, owner_role):
    service = notification_duty_data.service
    schedule = notification_duty_data.schedule
    factories.ServiceMemberFactory(role=schedule.role, service=service)
    factories.ServiceMemberFactory(role=owner_role, service=service)

    factories.ShiftFactory(
        start='2018-12-01',
        end='2018-12-03',
        schedule=schedule,
        has_problems=True,
    )

    send_notifications_problems()
    assert len(mailoutbox) == 0


@freeze_time('2019-01-01')
@override_switch('dont_send_email', False)
def test_send_duty_problem_to_duty_with_responsible_for_duty(mailoutbox, notification_duty_data, owner_role):
    service = notification_duty_data.service
    staff_petrov = factories.StaffFactory(first_name='Петр', last_name='Петров')
    factories.ServiceMemberFactory(role=owner_role, service=service)
    schedule = notification_duty_data.schedule
    schedule.algorithm = Schedule.MANUAL_ORDER
    schedule.role = None
    schedule.save()
    schedule.shifts.update(has_problems=False)
    Problem.objects.filter(shift__in=schedule.shifts.all()).delete()
    staff_ivanov = factories.StaffFactory(first_name='Иван', last_name='Иванов')
    staff_vasiliev = factories.StaffFactory(first_name='Василий', last_name='Васильев')

    staff_to_block_shift_problem = factories.StaffFactory()
    factories.OrderFactory(schedule=schedule, staff=staff_to_block_shift_problem, order=0)
    factories.ServiceMemberFactory(service=service, staff=staff_to_block_shift_problem)

    send_notifications_problems()
    assert len(mailoutbox) == 0
    factories.ServiceMemberFactory(
        staff=staff_ivanov,
        role=factories.RoleFactory(code=Role.DEPUTY_OWNER),
        service=service
    )
    factories.ServiceMemberFactory(
        staff=staff_vasiliev,
        role=factories.RoleFactory(code=Role.DEPUTY_OWNER),
        service=service
    )
    factories.ServiceMemberFactory(
        service=service,
        role=factories.RoleFactory(code=Role.RESPONSIBLE_FOR_DUTY),
        staff=staff_petrov
    )
    send_notifications_problems()
    assert len(mailoutbox) == 1


@freeze_time('2019-01-01')
@override_switch('dont_send_email', False)
def test_send_duty_problem_new_memeber(mailoutbox, notification_duty_data, owner_role):
    service = notification_duty_data.service
    schedule = notification_duty_data.schedule
    schedule.algorithm = Schedule.MANUAL_ORDER
    schedule.save()
    staff_notify = factories.StaffFactory(first_name='Иван', last_name='Иванов')
    notify_membership = factories.ServiceMemberFactory(role=schedule.role, service=service, staff=staff_notify)
    staff_not_notify = factories.ServiceMemberFactory(role=schedule.role, service=service).staff
    factories.OrderFactory(staff=staff_not_notify, schedule=schedule, order=0)
    factories.ServiceMemberFactory(role=owner_role, service=service)
    schedule.shifts.update(has_problems=False)
    Problem.objects.filter(shift__in=schedule.shifts.all()).delete()

    staff_notify_name = f'{staff_notify.first_name} {staff_notify.last_name}'
    staff_not_notify_name = f'{staff_not_notify.first_name} {staff_not_notify.last_name}'

    # Шлем письмо только о том человеке, который все еще не в порядке
    send_notifications_problems()
    assert len(mailoutbox) == 1
    assert staff_notify_name in mailoutbox[0].body
    assert staff_not_notify_name not in mailoutbox[0].body

    send_notifications_problems()
    assert len(mailoutbox) == 1

    # В последующие дни не напоминаем о человеке, который не в порядке, если нет других свежих проблем
    with freeze_time('2019-01-05'):
        send_notifications_problems()
        assert len(mailoutbox) == 1

    shift = schedule.shifts.order_by('id')[0]
    shift.has_problems = True
    shift.save()
    Problem.open_shift_problem(shift, Problem.STAFF_HAS_GAP, shift.start_datetime)

    # Дважды не напоминаем
    send_notifications_problems()
    assert len(mailoutbox) == 2
    assert staff_notify_name not in mailoutbox[1].body
    assert staff_not_notify_name not in mailoutbox[0].body

    # Убираем человека из сервиса и добавляем заново
    # Должно придти новое письмо
    notify_membership.delete()
    factories.ServiceMemberFactory(role=schedule.role, service=service, staff=staff_notify)
    send_notifications_problems()
    assert len(mailoutbox) == 3
    assert staff_notify_name in mailoutbox[2].body

    assert Problem.objects.active(Problem.NEW_MEMBER_IN_SCHEDULE).exists()
    factories.OrderFactory(staff=staff_notify, schedule=schedule, order=1)
    send_notifications_problems()
    assert len(mailoutbox) == 3
    assert not Problem.objects.active(Problem.NEW_MEMBER_IN_SCHEDULE).exists()


@freeze_time('2019-01-01')
@override_switch('dont_send_email', False)
def test_dont_send_notify_if_staff_already_has_one_of_many_roles(mailoutbox, notification_duty_data, owner_role):
    service = notification_duty_data.service
    factories.ServiceMemberFactory(role=owner_role, service=service)
    schedule = notification_duty_data.schedule
    schedule.algorithm = Schedule.MANUAL_ORDER
    schedule.role = None
    schedule.save()
    schedule.shifts.update(has_problems=False)
    Problem.objects.filter(shift__in=schedule.shifts.all()).delete()
    staff = factories.StaffFactory(first_name='Иван', last_name='Иванов')

    staff_to_block_shift_problem = factories.StaffFactory()
    factories.OrderFactory(schedule=schedule, staff=staff_to_block_shift_problem, order=0)
    factories.ServiceMemberFactory(service=service, staff=staff_to_block_shift_problem)

    send_notifications_problems()
    assert len(mailoutbox) == 0
    factories.ServiceMemberFactory(staff=staff, service=service)
    send_notifications_problems()
    assert len(mailoutbox) == 1
    factories.ServiceMemberFactory(staff=staff, service=service)
    send_notifications_problems()
    assert len(mailoutbox) == 1


@freeze_time('2019-01-01')
@override_switch('dont_send_email', False)
@pytest.mark.parametrize('bad_report_date', [True, False])
def test_send_duty_fresh_problems(mailoutbox, notification_duty_data, owner_role, bad_report_date):
    service = notification_duty_data.service
    service.name = 'AService'
    service.save()
    schedule = notification_duty_data.schedule
    factories.ServiceMemberFactory(role=schedule.role, service=service)
    factories.ServiceMemberFactory(role=owner_role, service=service)
    other_service = factories.ServiceFactory(name='BService')
    factories.ServiceMemberFactory(role=owner_role, service=other_service)
    other_schedule = factories.ScheduleFactory(
        service=other_service,
        name='BSchedule',
        start_date=timezone.datetime(2019, 1, 1),
        duration=timezone.timedelta(days=1),
    )
    factories.ServiceMemberFactory(service=other_service)

    recalculate_all_duties()
    Shift.objects.all().update(has_problems=False)

    assert len(mailoutbox) == 0

    shift = schedule.shifts.order_by('id')[0]
    shift.has_problems = True
    shift.save()
    Problem.open_shift_problem(shift, Problem.STAFF_HAS_GAP, shift.start_datetime)

    other_service_shift = other_schedule.shifts.get(start=timezone.datetime(2019, 1, 10).date())
    other_service_shift.has_problems = True
    other_service_shift.save()
    Problem.open_shift_problem(other_service_shift, Problem.STAFF_HAS_GAP, shift.start_datetime)

    send_notifications_problems()
    assert len(mailoutbox) == 2

    if bad_report_date:
        # Имитируем ситуацию, когда дата нотификации в проблеме выставилась неправильно
        shift.problems.all().update(report_date=timezone.datetime(2018, 1, 1))
    send_notifications_problems()
    # свежих проблем нет - новое письмо не шлем
    assert len(mailoutbox) == 2

    first_shift_dates = 'с 1 января 2019 г. по 1 января 2019 г.'
    second_shift_dates = 'с 4 января 2019 г. по 4 января 2019 г.'
    other_shift_dates = 'с 10 января 2019 г. по 10 января 2019 г.'

    # В первом письме только первый шифт
    if service.name in mailoutbox[0].body:
        # В первом письме только первый шифт
        assert first_shift_dates in mailoutbox[0].body
        assert second_shift_dates not in mailoutbox[0].body
        assert other_shift_dates not in mailoutbox[0].body

        # Во втором письме только проблемы второго сервиса
        assert first_shift_dates not in mailoutbox[1].body
        assert second_shift_dates not in mailoutbox[1].body
        assert other_shift_dates in mailoutbox[1].body

    else:
        # Иначе - наоборот
        assert first_shift_dates in mailoutbox[1].body
        assert second_shift_dates not in mailoutbox[1].body
        assert other_shift_dates not in mailoutbox[1].body

        assert first_shift_dates not in mailoutbox[0].body
        assert second_shift_dates not in mailoutbox[0].body
        assert other_shift_dates in mailoutbox[0].body

    another_shift = schedule.shifts.order_by('id')[3]
    another_shift.has_problems = True
    another_shift.save()
    Problem.open_shift_problem(another_shift, Problem.STAFF_HAS_GAP, shift.start_datetime)

    send_notifications_problems()
    # свежая проблема - новое письмо
    assert len(mailoutbox) == 3

    # Во третьем оба шифта первого сервиса
    assert first_shift_dates in mailoutbox[2].body
    assert second_shift_dates in mailoutbox[2].body
    assert other_shift_dates not in mailoutbox[2].body

    dt = timezone.timedelta(minutes=1)
    for problem in Problem.objects.filter(shift__in=[shift, other_service_shift, another_shift]):
        notification = ServiceNotification.objects.filter(problem=problem).order_by('sent_at').last()
        assert (problem.report_date - notification.created_at) < dt


@override_switch('dont_send_email', False)
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=60))
@freeze_time('2019-01-01')
def test_send_only_nearest_duty_problems(mailoutbox, notification_duty_data, owner_role):

    service = notification_duty_data.service
    schedule = notification_duty_data.schedule
    schedule.allow_sequential_shifts = True
    schedule.duration = timezone.timedelta(days=10)
    schedule.save()
    another_schedule = factories.ScheduleFactory(
        start_date=schedule.start_date,
        duration=schedule.duration,
        service=service,
        days_for_problem_notification=50,
        allow_sequential_shifts=True,
    )
    service_member = factories.ServiceMemberFactory(role=schedule.role, service=service)
    service_member2 = factories.ServiceMemberFactory(role=another_schedule.role, service=service)
    factories.ServiceMemberFactory(role=owner_role, service=service)

    recalculate_all_duties()

    Shift.objects.update(is_approved=True)

    factories.GapFactory(
        staff=service_member.staff,
        start=timezone.datetime(2019, 1, 1, 0, 0),
        end=timezone.datetime(2019, 1, 2, 0, 0),
        work_in_absence=False,
        type='type',
        full_day=False
    )
    factories.GapFactory(
        staff=service_member.staff,
        start=timezone.datetime(2019, 1, 18, 0, 0),
        end=timezone.datetime(2019, 1, 19, 0, 0),
        work_in_absence=False,
        type='type',
        full_day=False
    )
    factories.GapFactory(
        staff=service_member.staff,
        start=timezone.datetime(2019, 1, 25, 0, 0),
        end=timezone.datetime(2019, 1, 26, 0, 0),
        work_in_absence=False,
        type='type',
        full_day=False
    )
    factories.GapFactory(
        staff=service_member2.staff,
        start=timezone.datetime(2019, 2, 1, 0, 0),
        end=timezone.datetime(2019, 2, 2, 0, 0),
        work_in_absence=False,
        type='type',
        full_day=False
    )

    recalculate_all_duties()
    send_notifications_problems()

    assert len(mailoutbox) == 1

    assert 'с 1 января 2019 г. по 10 января 2019 г.' in mailoutbox[0].body
    # Проблемы в сменах не присылаются поскольку до прблемы больше 2 недель
    assert 'c 11 января 2019 г. по 20 января 2019 г.' not in mailoutbox[0].body
    assert 'с 21 января 2019 г. по 30 января 2019 г.' not in mailoutbox[0].body
    # Проблема в смене присылается, потому что у данного расписания индивидуальная настройка
    assert 'с 31 января 2019 г. по 9 февраля 2019 г.' in mailoutbox[0].body

    days_before_shift = set(ServiceNotification.objects
                            .filter(problem__reason='staff_has_gap')
                            .values_list('days_before_shift', flat=True))
    assert days_before_shift == {0, 30}


@override_switch('dont_send_email', False)
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=20))
@freeze_time('2019-01-01')
def test_dont_send_past_problems(mailoutbox, notification_duty_data, owner_role):
    """
    Не отправляем письма о прошедших проблемах
    """
    service = notification_duty_data.service
    schedule = notification_duty_data.schedule
    schedule.duration = timezone.timedelta(days=10)
    schedule.allow_sequential_shifts = True
    schedule.save()

    service_member = factories.ServiceMemberFactory(role=schedule.role, service=service)
    factories.ServiceMemberFactory(role=owner_role, service=service)

    factories.GapFactory(
        staff=service_member.staff,
        start=timezone.datetime(2019, 1, 5, 0, 0),
        end=timezone.datetime(2019, 1, 7, 0, 0),
        work_in_absence=False,
        type='type',
        full_day=False
    )

    recalculate_all_duties()

    with freeze_time('2019-01-08'):
        recalculate_all_duties()
        send_notifications_problems()
        assert len(mailoutbox) == 0


@override_switch('dont_send_email', False)
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=20))
@freeze_time('2019-01-01')
def test_dont_send_problems(mailoutbox, notification_duty_data, owner_role):
    """
    Не отправляем письма если у schedule - days_for_problem_notification=None
    """
    service = notification_duty_data.service
    schedule = notification_duty_data.schedule
    schedule.duration = timezone.timedelta(days=10)
    schedule.allow_sequential_shifts = True
    schedule.days_for_problem_notification = None
    schedule.save()

    service_member = factories.ServiceMemberFactory(role=schedule.role, service=service)
    factories.ServiceMemberFactory(role=owner_role, service=service)

    factories.GapFactory(
        staff=service_member.staff,
        start=timezone.datetime(2019, 1, 5, 0, 0),
        end=timezone.datetime(2019, 1, 7, 0, 0),
        work_in_absence=False,
        type='type',
        full_day=False
    )

    recalculate_all_duties()
    send_notifications_problems()
    assert len(mailoutbox) == 0


@override_switch('dont_send_email', False)
@freeze_time('2019-01-05')
def test_send_only_new_shift_problems_on_holidays(mailoutbox, owner_role):
    """
    В выходные дни уведомления о проблемах отправляются, только если у смен есть новые проблемы.
    5 янв - сб
    """

    service = factories.ServiceFactory()
    role = factories.RoleFactory()
    staffs = [
        factories.ServiceMemberFactory(service=service, role=role).staff
        for _ in range(3)
    ]
    schedule = factories.ScheduleFactory(
        service=service,
        role=role,
        algorithm=Schedule.MANUAL_ORDER,
        start_date=timezone.datetime(2018, 12, 31).date(),
    )
    factories.ServiceMemberFactory(role=owner_role, service=service)

    staff_order = [0, 2, 1]
    for order, staff in enumerate(staff_order):
        Order.objects.create(schedule=schedule, staff=staffs[staff], order=order)

    recalculate_all_duties()

    factories.ServiceMemberFactory(service=service, role=role)
    send_notifications_problems()
    assert len(mailoutbox) == 0


@freeze_time('2019-08-19T01:00:00')
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=30))
@pytest.mark.parametrize(('is_holiday', 'result_date'), [
    (True, timezone.datetime(2019, 9, 15).date()),
    (False, timezone.datetime(2019, 9, 16).date())
])
def test_recalculate(is_holiday, result_date):
    """
    Проверим не зацикливается ли пересчет дежурств, если последняя сменя закончилась в пт,
    а в настройках указано не учитывать выходные (настройка duty_on_weekends выключена, а duty_on_holidays включена).

    Если попадает суббота - праздник, то при текущих настройках этот день должен быть рабочим

    В этом тесте замокали период, на который создаются смены: вместо полгода - 30 дней.
    Достаточно рассчитать вперёд пару-тройку смен.
    """
    service = factories.ServiceFactory()
    role = factories.RoleFactory()
    for _ in range(3):
        factories.ServiceMemberFactory(service=service, role=role)

    factories.ScheduleFactory(
        start_date='2019-07-29',
        service=service,
        duration=timezone.timedelta(days=10),
        duty_on_weekends=False,
    )
    recalculate_duty_for_service(service.id)

    schedule_2 = factories.ScheduleFactory(
        start_date='2019-08-05',
        service=service,
        duration=timezone.timedelta(days=10),
        duty_on_weekends=False,
    )

    if is_holiday:
        Holiday.objects.filter(date='2019-09-15').update(is_holiday=True)

    recalculate_duty_for_service(service.id)
    assert schedule_2.shifts.order_by('start').last().start == result_date


@freeze_time('2019-01-16T12:00:00')
@override_switch('dont_send_email', False)
def test_send_notification_duty_recalculate(notification_duty_data, mailoutbox):
    """
    После дефолтного пересчета у автоподтвержденных дежурств стираются шифты, поэтому письма отправляются заново.
    Проверим, что если выполнить пересчет, шифт останется на месте и новых писем не получим.
    """

    schedule = notification_duty_data.schedule
    schedule.consider_other_schedules = False
    schedule.duration = timezone.timedelta(days=4)
    schedule.save()
    service = notification_duty_data.service
    for _ in range(4):
        factories.ServiceMemberFactory(role=schedule.role, service=service)

    recalculate_duty_for_service(service.id)
    with patch('plan.holidays.calendar.workdays_of_month') as workdays_of_month:
        workdays_of_month.return_value = workdays()
        send_notification_duty()

        assert len(mailoutbox) == 1
        next_shift = schedule.shifts.future().order_by('start').first()
        assert ServiceNotification.objects.filter(shift=next_shift).exists()

        # Проверяем, что второй раз после пересчета не отправляем тоже самое
        mailoutbox = []
        recalculate_duty_for_service(service.id)
        send_notification_duty()
        assert len(mailoutbox) == 0


@freeze_time('2019-01-04')
def test_recalculate_replaces(client, notification_duty_data, owner_role):
    """
    Проверим, что при флаге "не учитывать другие дежурства"
    не удаляются при пересчете шифты, и, соотвественно, их временные замены.
    """

    schedule = notification_duty_data.schedule
    schedule.consider_other_schedules = False
    schedule.duration = timezone.timedelta(days=4)
    schedule.save()
    service = notification_duty_data.service
    factories.ServiceMemberFactory(staff=service.owner, role=owner_role, service=service)
    for _ in range(4):
        factories.ServiceMemberFactory(role=schedule.role, service=service)

    recalculate_duty_for_service(service.id)

    next_shift = schedule.shifts.future().order_by('start').first()

    # сделаем замену
    client.login(service.owner.login)
    response = client.json.post(
        reverse('api-v3:duty-shift-list'),
        {
            'replace_for': next_shift.id,
            'person': next_shift.staff.login,
            'start_datetime': '2019-01-05T00:00:00',
            'end_datetime': '2019-01-06T00:00:00',
        }
    )

    assert response.status_code == 201
    next_shift.refresh_from_db()
    assert next_shift.replaces.exists()


@freeze_time('2019-01-16')
@override_switch('dont_send_email', False)
@pytest.mark.parametrize('service_state', [Service.states.DELETED, Service.states.CLOSED])
def test_send_duty_reminder_notification_deleted_services(mailoutbox, notification_duty_data, owner_role, service_state):
    """
    Проверим, что не отправляем письма-напоминания удаленному/закрытому сервису
    """

    schedule = notification_duty_data.schedule
    schedule.duration = timezone.timedelta(days=4)
    schedule.save()

    service = notification_duty_data.service
    factories.ServiceMemberFactory(staff=service.owner, role=owner_role, service=service)

    for _ in range(4):
        factories.ServiceMemberFactory(role=schedule.role, service=service)

    recalculate_duty_for_service(service.id)
    service.state = service_state
    service.save()

    with patch('plan.holidays.calendar.workdays_of_month') as workdays_of_month:
        workdays_of_month.return_value = workdays()
        send_notification_duty()

    assert len(mailoutbox) == 0


@freeze_time('2019-01-04')
@override_switch('dont_send_email', False)
@pytest.mark.parametrize('service_state', [Service.states.DELETED, Service.states.CLOSED])
def test_send_duty_problems_notification_deleted_services(mailoutbox, notification_duty_data, owner_role, service_state):
    """
    Проверим, что не отправляем письма о проблемах удаленному/закрытому сервису
    """

    schedule = notification_duty_data.schedule
    schedule.duration = timezone.timedelta(days=4)
    schedule.save()

    service = notification_duty_data.service
    factories.ServiceMemberFactory(staff=service.owner, role=owner_role, service=service)

    for _ in range(4):
        factories.ServiceMemberFactory(role=schedule.role, service=service)

    recalculate_duty_for_service(service.id)
    next_shift = schedule.shifts.future().order_by('start').first()
    next_shift.is_approved = True
    next_shift.save()
    factories.GapFactory(
        staff=next_shift.staff,
        start='2019-01-01T00:00:00Z',
        end='2019-01-20T00:00:00Z',
        work_in_absence=False
    )

    # пересчитаем график для создания проблемы у next_shift
    recalculate_duty_for_service(service.id)

    service.state = service_state
    service.save()

    send_notifications_problems()

    assert next_shift.problems.exists()
    assert len(mailoutbox) == 0


@freeze_time('2019-01-04')
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=20))
@pytest.mark.parametrize('service_state', [Service.states.DELETED, Service.states.CLOSED])
def test_recalculate_deleted_services(owner_role, service_state):
    """
    Проверим, что не пересчитываем закрытые/удаленные сервисы
    """

    service = factories.ServiceFactory(state=service_state)
    schedule = factories.ScheduleFactory(
        start_date='2019-01-01',
        service=service,
        duration=timezone.timedelta(days=4),
        role=factories.RoleFactory(),
    )

    factories.ServiceMemberFactory(staff=service.owner, role=owner_role, service=service)

    for _ in range(4):
        factories.ServiceMemberFactory(role=schedule.role, service=service)

    recalculate_all_duties()
    assert not schedule.shifts.exists()

    recalculate_duty_for_service(service.id)
    assert not schedule.shifts.exists()


@freeze_time('2019-01-04')
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=20))
def test_recalculation_when_changing_status(owner_role, client, staff_factory):
    """
    Проверим, что запускается пересчет дежурств при смене статуса с "закрыт" на активный
    """

    service = factories.ServiceFactory(owner=staff_factory())
    factories.ServiceMemberFactory(staff=service.owner, role=owner_role, service=service)

    role = factories.RoleFactory()
    for _ in range(4):
        factories.ServiceMemberFactory(role=role, service=service)

    schedule = factories.ScheduleFactory(
        start_date='2019-01-01',
        service=service,
        duration=timezone.timedelta(days=4),
        role=role,
    )

    service.state = Service.states.CLOSED
    service.save()

    # убедимся, что до этого не было шифтов
    assert not schedule.shifts.exists()

    with mock.patch('plan.duty.tasks.priority_recalculate_shifts_for_service') as recalculate:
        client.login(service.owner.login)
        response = client.json.patch(
            reverse('api-v3:service-detail', args=[service.pk]),
            {'state': Service.states.IN_DEVELOP}
        )

        assert response.status_code == 200

        # после смены статуса должен запуститься пересчет и появятся шифты
        recalculate.call_args_list == [service.id]


def test_notify_staff_on_schedule_update(transactional_db):
    service = factories.ServiceFactory()
    with patch('plan.duty.tasks.notify_staff_duty') as notify:
        recalculate_duty_for_service(service.id)
        notify.delay_on_commit.assert_called_once_with(service.id, service.slug)


def test_notify_staff_on_schedule_delete(owner_role, client, transactional_db, staff_factory):
    staff = staff_factory()
    service = factories.ServiceFactory()
    factories.ServiceMemberFactory(service=service, staff=staff, role=owner_role)
    schedule = factories.ScheduleFactory(service=service)
    with patch('plan.duty.tasks.notify_staff_duty') as notify:
        client.login(staff.login)
        response = client.json.delete(
            reverse('api-v3:duty-schedule-detail', args=[schedule.pk])
        )
        assert response.status_code == 204
        notify.delay_on_commit.assert_called_once_with(service.id, service.slug)  # удаление вызывает пересчет


@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=15))
@pytest.mark.parametrize('approve', [True, False])
@pytest.mark.parametrize('shift_is_approved', [True, False])
def test_notify_staff_on_shift_update(approve, shift_is_approved, client, owner_role, staff_factory):
    staff = staff_factory()
    service = factories.ServiceFactory()
    factories.ServiceMemberFactory(service=service, staff=staff, role=owner_role)
    schedule = factories.ScheduleFactory(service=service)
    recalculate_duty_for_service(service.id)
    shift = schedule.shifts.future().first()
    shift.is_approved = shift_is_approved
    shift.save()

    with patch('plan.duty.api.serializers.notify_staff_duty') as notify_staff_duty:
        with mock.patch('plan.duty.tasks.priority_recalculate_shifts_for_service'):
            client.login(staff.login)
            response = client.json.patch(
                reverse('api-v3:duty-shift-detail', args=[shift.id]),
                {'is_approved': approve}
            )

            assert response.status_code == 200

        if approve != shift_is_approved:
            # если статус изменен
            notify_staff_duty.delay.assert_called_with(service.id, service.slug)

        else:
            assert not notify_staff_duty.delay.called


@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=15))
def test_notify_staff_on_shift_update_staff(owner_role, client, staff_factory):
    staff = staff_factory()
    service = factories.ServiceFactory()
    factories.ServiceMemberFactory(service=service, staff=staff, role=owner_role)
    role = factories.RoleFactory()

    for _ in range(3):
        factories.ServiceMemberFactory(service=service, role=role)

    schedule = factories.ScheduleFactory(service=service, role=role)
    recalculate_duty_for_service(service.id)
    shift = schedule.shifts.future().first()
    next_shift = schedule.shifts.filter(start_datetime__gte=shift.end_datetime).first()

    with patch('plan.duty.api.serializers.notify_staff_duty') as notify_staff_duty:
        with mock.patch('plan.duty.tasks.priority_recalculate_shifts_for_service'):
            client.login(staff.login)
            response = client.json.patch(
                reverse('api-v3:duty-shift-detail', args=[shift.id]),
                {'person': next_shift.staff.login}
            )
            assert response.status_code == 200
            notify_staff_duty.delay.assert_called_with(service.id, service.slug)


@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=15))
def test_notify_staff_autoapprove(client):
    role = factories.RoleFactory()
    for _ in range(3):
        service = factories.ServiceFactory()
        for _ in range(3):
            factories.ServiceMemberFactory(service=service, role=role)

        factories.ScheduleFactory(service=service, role=role, autoapprove_timedelta=timezone.timedelta(15))

    with patch('plan.duty.tasks.autoapprove_shifts'):
        recalculate_all_duties()

    with patch('plan.duty.tasks.notify_staff_duty') as notify_staff_duty:
        autoapprove_shifts()
        assert notify_staff_duty.delay.call_count == 3


@freeze_time('2019-01-01')
def test_autoapprove_shifts():
    service = factories.ServiceFactory()
    role = factories.RoleFactory()
    factories.ServiceMemberFactory(service=service, role=role)
    schedule = factories.ScheduleFactory(service=service, role=role)
    recalculate_duty_for_service(service.id)
    autoapprove_shifts()

    def autoapprove_time(shift):
        return timezone.now() + (shift.schedule.autoapprove_timedelta or settings.DEFAULT_AUTOAPPROVE_TIMEDELTA)

    zombik = get_abc_zombik()

    assert all(
        shift.is_approved and shift.approved_by == zombik and shift.approve_datetime == timezone.now()
        if shift.start_datetime <= autoapprove_time(shift)
        else not shift.is_approved
        for shift in schedule.shifts.all()
    )


@patch('plan.duty.tasks.send_notification_duty')
def test_check_current_shifts(send_notification_duty_mock, duty_role):
    service = factories.ServiceFactory()
    role = factories.RoleFactory()
    role_on_duty = factories.RoleFactory()
    for _ in range(4):
        factories.ServiceMemberFactory(service=service, role=role)
    schedule = factories.ScheduleFactory(
        service=service,
        role=role,
        persons_count=2,
        role_on_duty=role_on_duty,
        consider_other_schedules=False,
    )
    factories.ScheduleFactory(
        service=service,
        role=role,
        persons_count=1,
        role_on_duty=role_on_duty,
        consider_other_schedules=False,
    )
    factories.ScheduleFactory(
        service=service,
        role=role,
        persons_count=1,
        role_on_duty=factories.RoleFactory(),
        consider_other_schedules=False,
    )

    with patch('plan.services.models.ServiceMember.request'):
        with patch('plan.api.idm.actions.request_membership') as request_membership:
            with patch('plan.api.idm.actions.deprive_role') as deprive_role:
                request_membership.side_effect = request_member_side_effect
                deprive_role.side_effect = deprive_member_side_effect
                recalculate_duty_for_service(service.id)

                time_now = schedule.shifts.order_by('start').first().start_datetime

                with freeze_time(time_now - settings.SHIFT_BEGIN_BEFORE_DUTY_START_TIMEDELTA + timezone.timedelta(minutes=1)):
                    check_current_shifts()
                send_notification_duty_mock.delay.assert_called_once()

                for i in range(1, 10):
                    time_now += schedule.duration

                    with freeze_time(time_now - settings.SHIFT_BEGIN_BEFORE_DUTY_START_TIMEDELTA + timezone.timedelta(minutes=1)):
                        check_current_shifts()

                    with freeze_time(time_now + settings.SHIFT_FINISH_AFTER_DUTY_START_TIMEDELTA + timezone.timedelta(minutes=1)):
                        check_current_shifts()

                    shifts = schedule.shifts.annotate_member_id().order_by('start')
                    prev_shifts = shifts[(i-1)*schedule.persons_count:(i+0)*schedule.persons_count]
                    curr_shifts = shifts[(i+0)*schedule.persons_count:(i+1)*schedule.persons_count]

                    assert all(shift.state == Shift.FINISHED for shift in prev_shifts)
                    assert all(shift.state == Shift.STARTED for shift in curr_shifts)
                    assert all(shift.member_id is not None for shift in curr_shifts)


@pytest.mark.parametrize(('status', 'result'), ((Schedule.DELETED, 0), (Schedule.ACTIVE, 1)))
def test_check_current_shift_for_inactive_schedule(duty_role, status, result):
    service = factories.ServiceFactory()
    role = factories.RoleFactory()
    schedule = factories.ScheduleFactory(
        service=service,
        role=role,
        persons_count=3,
        role_on_duty=duty_role,
        consider_other_schedules=False,
    )
    for _ in range(3):
        factories.ServiceMemberFactory(service=service, role=role)

    schedule.status = status
    schedule.save()

    with patch('plan.api.idm.actions.request_membership', mock.Mock(return_value={'id': 1})):
        time_now = schedule.shifts.order_by('start').first().start_datetime

        with freeze_time(time_now - settings.SHIFT_BEGIN_BEFORE_DUTY_START_TIMEDELTA):
            check_current_shifts()

            assert schedule.shifts.started().count() == result


def test_remove_dangling_duty_members(duty_role):
    service = factories.ServiceFactory()
    role = factories.RoleFactory()
    staff = factories.ServiceMemberFactory(service=service, role=role).staff

    schedule = factories.ScheduleFactory(service=service, role=role, role_on_duty=factories.RoleFactory())
    factories.ShiftFactory(schedule=schedule, staff=staff, state=Shift.STARTED)
    factories.ShiftFactory(schedule=schedule, staff=staff, state=Shift.FINISHED)
    member = factories.ServiceMemberFactory(
        service=service, role=schedule.get_role_on_duty(), staff=staff, autorequested=True)

    bad_schedule = factories.ScheduleFactory(
        service=service,
        role=role,
        role_on_duty=factories.RoleFactory(),
        start_date=utils.today() - timezone.timedelta(days=5)
    )
    factories.ShiftFactory(
        schedule=bad_schedule,
        staff=staff,
        state=Shift.STARTED,
        start_datetime=utils.today() - timezone.timedelta(days=5),
        end_datetime=utils.today() - timezone.timedelta(days=1),
    )
    bad_member = factories.ServiceMemberFactory(
        service=service, role=bad_schedule.get_role_on_duty(), staff=staff, autorequested=True)

    def get_duty_roles():
        return ServiceMember.objects.filter(
            role__in=[s.get_role_on_duty() for s in [schedule, bad_schedule]],
            staff=staff,
            service=service,
        )

    assert set(get_duty_roles()) == {member, bad_member}

    with patch('plan.api.idm.actions.deprive_role') as deprive_role:
        deprive_role.side_effect = deprive_member_side_effect
        remove_dangling_duty_members()

    assert set(get_duty_roles()) == {member}


@override_switch('dont_send_email', False)
@freeze_time('2020-06-30T21:05:00')    # 0 часов 2020-07-01 по МСК
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=5))
def test_send_notification_morning(mailoutbox, owner_role):
    """
    Отправка уведомления до 3:00.
    Проблема: если письмо о старте смене, фраза в пиьсме была некорректная.
    """

    schedule = factories.ScheduleFactory(
        start_date=timezone.datetime(2020, 7, 1).date(),
    )
    factories.ServiceMemberFactory(role=owner_role, service=schedule.service)
    factories.ServiceMemberFactory(service=schedule.service, role=schedule.role)

    recalculate_duty_for_service(schedule.service.id)
    with patch('plan.holidays.calendar.workdays_of_month') as workdays_of_month:
        workdays_of_month.return_value = workdays()
        send_notification_duty()

    assert len(mailoutbox) == 1

    body = mailoutbox[0].body
    assert 'Прямо сейчас начинаются дежурства' in body


@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=26))
@pytest.mark.parametrize('algorithm', [Schedule.NO_ORDER, Schedule.MANUAL_ORDER])
def test_delete_service_member_department(algorithm):
    """
    Проверяем что при удалении групповой роли обнулятся shift.staff
    для всех удаленных участников
    """
    service = factories.ServiceFactory()
    role = factories.RoleFactory()
    role_on_duty = factories.RoleFactory()
    department = factories.DepartmentFactory()
    member_deparment = factories.ServiceMemberDepartmentFactory(
        service=service,
        role=role,
        department=department
    )
    schedule = factories.ScheduleFactory(
        service=service,
        algorithm=algorithm,
        role=role,
        role_on_duty=role_on_duty,
        consider_other_schedules=False,
        start_date=utils.today() + timezone.timedelta(days=1),
        allow_sequential_shifts=False,
    )

    for i in range(4):
        member = factories.ServiceMemberFactory(from_department=member_deparment, service=service, role=role)
        factories.OrderFactory(schedule=schedule, staff=member.staff, order=i)

    member = factories.ServiceMemberFactory(service=service, role=role)
    factories.OrderFactory(schedule=schedule, staff=member.staff, order=4)

    recalculate_duty_for_service(service.id)
    Shift.objects.filter(schedule=schedule).update(is_approved=True)
    assert Shift.objects.filter(schedule=schedule).count() == 6
    assert Shift.objects.filter(staff_id__isnull=True).count() == 0

    member_deparment.deprive()
    assert Shift.objects.filter(schedule=schedule).count() == 6
    if algorithm == Schedule.MANUAL_ORDER:
        assert Shift.objects.filter(staff_id__isnull=True).count() == 5
        assert Shift.objects.filter(staff_id=member.staff_id).count() == 1
    else:
        # теперь каждая вторая смена принадлежит единственному оставшемуся стаффу
        assert Shift.objects.filter(staff_id__isnull=True).count() == 3
        assert Shift.objects.filter(staff_id=member.staff_id).count() == 3


@pytest.mark.parametrize('api', ['api-v3', 'api-v4'])
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=15))
def test_check_current_shifts_with_update_role_on_duty(client, api, duty_data):
    """
    Поменяем в графике роль, выдаваемую на дежурстве.
    У текущего шифта роль не изменится.
    Но все последующие должны стартовать с новой.
    """

    old_role = factories.RoleFactory()
    new_role = factories.RoleFactory()
    service = duty_data.service

    for _ in range(3):
        factories.ServiceMemberFactory(service=service)

    schedule = factories.ScheduleFactory(service=service, role_on_duty=old_role, role=None)
    recalculate_duty_for_service(service.id)

    # до запуска таски старта у всех шифтов нет роли
    assert all(shift.role is None for shift in schedule.shifts.all())

    with patch('plan.api.idm.actions.request_membership') as request_membership:
        with patch('plan.services.models.ServiceMember.request'):
            request_membership.side_effect = request_member_side_effect
            check_current_shifts_for_service(service.id)

    client.login(duty_data.owner.login)
    response = client.json.patch(
        reverse(f'{api}:duty-schedule-detail', args=[schedule.pk]),
        data={'role_on_duty': new_role.id}
    )
    assert response.status_code == 200

    # role проставляем только у стартовавших
    assert all(shift.role == old_role for shift in schedule.shifts.current_shifts())
    assert all(shift.role is None for shift in schedule.shifts.future())

    with freeze_time(timezone.now() + schedule.duration):
        with patch('plan.api.idm.actions.request_membership') as request_membership:
            with patch('plan.api.idm.actions.deprive_role') as deprive_role:
                with patch('plan.services.models.ServiceMember.request'):
                    request_membership.side_effect = request_member_side_effect
                    deprive_role.side_effect = deprive_member_side_effect
                    check_current_shifts_for_service(service.id)

        # role проставляем только у стартовавших
        assert all(shift.role == old_role for shift in schedule.shifts.past_shifts())
        assert all(shift.role == new_role for shift in schedule.shifts.current_shifts())
        assert all(shift.role is None for shift in schedule.shifts.future())


def test_check_current_shift_finish_shifts(duty_data):
    """
    Проверим, что при окончании смены роль отзывается, если в графике нет других смен.
    """
    service = duty_data.service
    for _ in range(3):
        factories.ServiceMemberFactory(service=service)

    schedule = factories.ScheduleFactory(
        service=service,
        role=None,
        start_date=utils.today() - timezone.timedelta(days=1)
    )
    recalculate_duty_for_service(service_id=service.id)
    shift = schedule.shifts.current_shifts().first()
    assert shift.staff is not None

    schedule.recalculate = False
    schedule.save()

    with patch('plan.api.idm.actions.request_membership') as request_membership:
        with patch('plan.services.models.ServiceMember.request'):
            request_membership.side_effect = request_member_side_effect
            check_current_shifts_for_service(duty_data.service.id)

            shift.refresh_from_db()
            assert shift.state == Shift.STARTED
            member = ServiceMember.objects.get(staff=shift.staff, role=shift.role, service=duty_data.service)

            schedule.shifts.future().delete()
            assert len(schedule.shifts.all()) == 1

            with freeze_time(timezone.now() + schedule.duration):
                with patch('plan.api.idm.actions.deprive_role') as deprive_role:
                    deprive_role.side_effect = deprive_member_side_effect
                    check_current_shifts_for_service(duty_data.service.id)

                    shift.refresh_from_db()
                    assert shift.state == Shift.FINISHED
                    assert ServiceMember.all_states.get(pk=member.pk).state == SERVICEMEMBER_STATE.DEPRIVED


@pytest.mark.parametrize('case', ['match', 'missmatch', 'norole'])
@pytest.mark.parametrize('recalculate', [True, False])
def test_recalculate_duty_on_add_user(duty_data, client, person, robot, case, recalculate):
    """
    Добавляем новый сервис мембер, по сигналу должен происходить пересчет.
    Но если у графика стоит флаг recalculate == False, то не пересчитываем.
    """

    schedule_role = duty_data.role
    idm_role = factories.RoleFactory()

    if case == 'match':
        idm_role = schedule_role

    elif case == 'norole':
        schedule_role = None

    factories.ScheduleFactory(
        service=duty_data.service,
        role=schedule_role,
        recalculate=recalculate,
    )

    with patch('plan.duty.schedulers.DutyScheduler.recalculate_shifts') as recalculate_shifts:
        factories.ServiceMemberFactory(
            service=duty_data.service, staff=duty_data.owner, role=idm_role)

    if not recalculate or case == 'missmatch':
        assert recalculate_shifts.call_count == 0

    else:
        assert recalculate_shifts.call_count == 1


@pytest.mark.parametrize('case', ['match', 'missmatch', 'norole'])
@pytest.mark.parametrize('recalculate', [True, False])
def test_recalculate_duty_on_remove_user(duty_data, client, person, robot, case, recalculate):
    schedule_role = duty_data.role
    idm_role = factories.RoleFactory()

    if case == 'match':
        idm_role = schedule_role

    elif case == 'norole':
        schedule_role = None
        idm_role = factories.RoleFactory()

    factories.ScheduleFactory(
        service=duty_data.service,
        role=schedule_role,
        recalculate=recalculate
    )

    member = factories.ServiceMemberFactory(
        service=duty_data.service,
        role=idm_role,
        staff=person,
    )

    with patch('plan.duty.schedulers.DutyScheduler.recalculate_shifts') as recalculate_shifts:
        member.deprive()

    if not recalculate or case == 'missmatch':
        assert recalculate_shifts.call_count == 0

    else:
        assert recalculate_shifts.call_count == 1


def test_recalculate_duty_on_remove_duty_user(duty_data, client, person, duty_role, robot):
    """
    Проверим, что если в сервисе с заданным порядком удалить человека,
    при этом у человека есnь другая активная роль, подходящая для графика дежурств,
    то не происходит удаления шифтов этого человека.
    """

    member = factories.ServiceMemberFactory(
        service=duty_data.service,
        role=duty_role,
        staff=person,
    )

    factories.ServiceMemberFactory(
        service=duty_data.service,
        role=factories.RoleFactory(),
        staff=person,
    )

    factories.ScheduleFactory(
        service=duty_data.service,
        role=None,
        algorithm=Schedule.MANUAL_ORDER,
    )

    with patch('plan.duty.schedulers.DutyScheduler.recalculate_shifts') as recalculate:
        member.deprive()
        assert recalculate.call_count == 0


@pytest.mark.parametrize('algorithm', [Schedule.MANUAL_ORDER, Schedule.NO_ORDER])
def test_recalculate_duty_on_remove_duty_with_replaces(duty_data, client, person, duty_role, robot, algorithm):
    """
    Проверим, что если в сервисе удалить человека,
    то происходит удаления временных шифтят.
    """

    member = factories.ServiceMemberFactory(
        service=duty_data.service,
        role=duty_role,
        staff=person,
    )

    schedule = factories.ScheduleFactory(
        service=duty_data.service,
        role=None,
        algorithm=algorithm,
    )

    recalculate_duty_for_service(duty_data.service.id)
    shift = Shift.objects.future_and_present().first()
    factories.ShiftFactory(replace_for=shift, staff=person, schedule=schedule)

    assert Shift.objects.parttime().filter(staff=person).count() == 1

    member.deprive()
    assert Shift.objects.parttime().filter(staff=person).count() == 0


@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=25))
@pytest.mark.parametrize('algorithm', [Schedule.NO_ORDER, Schedule.MANUAL_ORDER])
def test_delete_staff_is_dismissed(algorithm):
    """
    Проверяем что при увольнении человека все его смены, включая активные и прдтверждённые, зачищаются
    """

    service = factories.ServiceFactory()

    for i in range(2):
        staff = factories.StaffFactory()
        factories.ServiceMemberFactory(service=service, staff=staff)

    schedule = factories.ScheduleFactory(
        service=service,
        algorithm=algorithm,
        role=None,
    )

    if algorithm == Schedule.MANUAL_ORDER:
        for i, member in enumerate(service.members.all()):
            factories.OrderFactory(schedule=schedule, staff=member.staff, order=i)

    # рассчитаем график
    recalculate_duty_for_service(service.id)

    assert Shift.objects.filter(schedule=schedule, staff=staff).count() > 1
    shift = Shift.objects.filter(schedule=schedule, staff=staff).first()
    shift.is_approved = True
    shift.save(update_fields=['is_approved'])

    # уволим staff
    staff.is_dismissed = True
    staff.save(update_fields=['is_dismissed'])
    staff.refresh_from_db()
    # роль отзовём вручную
    ServiceMember.objects.get(service=service, staff=staff).deprive()

    assert Shift.objects.filter(schedule=schedule, staff=staff).count() == 0


@pytest.fixture
def yql_query_mock():
    class FakeYQLRequest:
        def __init__(self):
            self.status = 'COMPLETED'
            self.run = mock.MagicMock()

    with patch('yql.api.v1.client.YqlClient.query', return_value=FakeYQLRequest()) as _mock:
        yield _mock


def test_upload_duty_to_yt(yql_query_mock):
    yesterday = datetime.date.today() - datetime.timedelta(days=1)
    staff = factories.StaffFactory(login='in yt')
    staff_1 = factories.StaffFactory(login='not in yt')
    shift = factories.ShiftFactory(
        staff=staff,
        start=yesterday,
        end=yesterday + datetime.timedelta(days=2),
    )

    factories.ShiftFactory(
        staff=staff_1,
        state=Shift.CANCELLED,
        start=yesterday,
        end=datetime.date.today(),
    )
    factories.ShiftFactory(
        staff=staff_1,
        start=yesterday + datetime.timedelta(days=3),
        end=yesterday + datetime.timedelta(days=5),
    )
    factories.ShiftFactory(
        staff=staff_1,
        start=yesterday - datetime.timedelta(days=2),
        end=yesterday - datetime.timedelta(days=1),
    )
    upload_duty_to_yt()

    yql_query_mock.assert_called_once_with(
        'USE hahn; INSERT INTO `home/abc/duty/{date}` with truncate ({columns}) VALUES ({values})'.format(
            date=yesterday.isoformat(),
            columns=','.join((
                'service_slug',
                'staff_login',
                'schedule_slug',
                'duty_hours',
            )),
            values=', '.join((
                f'"{shift.schedule.service.slug}"',
                f'"{shift.staff.login}"',
                f'"{shift.schedule.slug}"',
                '24',
            ))
        )
    )
    yql_query_mock.return_value.run.assert_called_once()


def test_sync_important_schedules(duty_data):
    schedule = factories.ScheduleFactory(service=duty_data.service)
    schedule_one = factories.ScheduleFactory(service=duty_data.service)
    schedule_two = factories.ScheduleFactory(
        service=duty_data.service, is_important=True
    )
    class FakeYQLRequest:
        def __init__(self):
            self.status = 'COMPLETED'
            self.run = mock.MagicMock()
            table_mock = mock.MagicMock()
            table_mock.rows = (
                (schedule.slug, schedule.service.slug),
            )
            self.get_results = mock.MagicMock(return_value=[table_mock])

    with patch('yql.api.v1.client.YqlClient.query', return_value=FakeYQLRequest()):
        sync_important_schedules()
    schedule.refresh_from_db()
    assert schedule.is_important is True
    schedule_one.refresh_from_db()
    assert schedule_one.is_important is False
    schedule_two.refresh_from_db()
    assert schedule_two.is_important is False
