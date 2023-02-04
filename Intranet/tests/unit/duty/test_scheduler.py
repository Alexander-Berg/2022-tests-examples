import datetime

from mock import Mock, patch
import freezegun
import pretend
import pytest
import pytz

from django.conf import settings
from django.core.urlresolvers import reverse
from django.test import override_settings
from django.utils import timezone

from plan.common.utils import timezone as utils
from plan.common.utils.timezone import make_localized_datetime
from plan.duty.models import Order, Problem, Schedule, Shift
from plan.duty.schedulers import DutyScheduler, ManualOrderingScheduler
from plan.duty.tasks import check_current_shifts, recalculate_all_duties, recalculate_duty_for_service
from plan.holidays.models import Holiday
from plan.services.models import ServiceMember
from plan.common.utils.dates import datetime_from_str

from common import factories


START_DATE = timezone.datetime(2018, 12, 31).date()


@pytest.fixture
@freezegun.freeze_time('2019-01-01')
def duty_data(transactional_db):
    owner = factories.StaffFactory()
    service = factories.ServiceFactory(owner=owner)
    role = factories.RoleFactory()
    members = []
    for _ in range(3):
        members.append(factories.ServiceMemberFactory(service=service, role=role))

    schedule = factories.ScheduleFactory(
        role=role,
        start_date=START_DATE,
        service=service,
        autoapprove_timedelta=timezone.timedelta(0),
    )

    return pretend.stub(
        service=service, role=role, schedule=schedule, members=members
    )


@pytest.fixture
@freezegun.freeze_time('2019-01-01')
def multiple_duties_data(responsible_role):
    schedule_count = 3
    members_count = 4
    owner = factories.StaffFactory()
    service = factories.ServiceFactory(owner=owner)
    members = [factories.ServiceMemberFactory(service=service, role=responsible_role) for _ in range(members_count)]

    schedules = []
    for _ in range(schedule_count):
        schedules.append(factories.ScheduleFactory(role=responsible_role, service=service))

    return pretend.stub(
        service=service,
        schedules=schedules,
        members=members,
        duty_role=responsible_role
    )


@pytest.fixture
@freezegun.freeze_time('2019-01-01')
def duty_data_for_replace(duty_data):
    # рассчитаем календарь, когда все могут дежурить
    duty_data.schedule.save()
    recalculate_all_duties()
    shift = duty_data.schedule.shifts.filter(start__gt=timezone.now().date() + 2 * duty_data.schedule.duration).order_by('start').first()

    old_staff = shift.staff
    new_staff = duty_data.members[1].staff

    gap = factories.GapFactory(
        staff=old_staff,
        start='2019-01-15T00:00:00Z',
        end='2020-01-31T00:00:00Z',
        work_in_absence=False,
        type='type',
        full_day=False
    )

    return pretend.stub(shift=shift, old_staff=old_staff, new_staff=new_staff, gap=gap, schedule=duty_data.schedule)


@freezegun.freeze_time('2019-08-30')
@pytest.mark.parametrize('exclusive_mode', [True, False])
def test_multiple_schedules(multiple_duties_data, exclusive_mode):
    assert all([schedule.consider_other_schedules for schedule in multiple_duties_data.schedules])
    # Оставляем три человека на три графика
    multiple_duties_data.members[-1].delete()
    recalculate_all_duties()
    shifts_by_schedule = [list(schedule.shifts.all()[:30]) for schedule in multiple_duties_data.schedules]
    for shift_slice in shifts_by_schedule:
        assert len(shift_slice) == len(set(shift_slice))

    duty_role = multiple_duties_data.duty_role
    new_schedule = factories.ScheduleFactory(
        service=multiple_duties_data.service,
        role=duty_role,
        start_date=multiple_duties_data.schedules[0].start_date,
    )
    multiple_duties_data.schedules.append(new_schedule)
    new_schedule.consider_other_schedules = not exclusive_mode
    new_schedule.save()
    recalculate_all_duties()
    new_shifts_by_schedule = [list(schedule.shifts.all()[:30]) for schedule in multiple_duties_data.schedules]
    # Т.к. мы заняли трех человек на трех графиках полностью
    # то четвертый будет не пустым только если будет игнорировать остальные
    if exclusive_mode:
        assert all([shift.staff is not None for shift in new_shifts_by_schedule[-1]])
    else:
        assert all([shift.staff is None for shift in new_shifts_by_schedule[-1]])


@freezegun.freeze_time('2019-01-01')
def test_cross_schedules(responsible_role, duty_role):
    members_count = 3
    owner = factories.StaffFactory()
    service = factories.ServiceFactory(owner=owner)

    exclusive_role = factories.RoleFactory()
    members = [factories.ServiceMemberFactory(service=service, role=responsible_role) for _ in range(members_count)]
    exclusive_duty = members[-1].staff
    factories.ServiceMemberFactory(service=service, staff=exclusive_duty, role=exclusive_role)

    regular_schedule = factories.ScheduleFactory(
        role=responsible_role,
        service=service,
        start_date=START_DATE
    )
    exclusive_schedule = factories.ScheduleFactory(
        role=exclusive_role,
        consider_other_schedules=False,
        service=service,
        start_date=START_DATE
    )
    recalculate_all_duties()
    duty_people = {shift.staff for shift in regular_schedule.shifts.order_by('start')[:members_count]}
    assert duty_people == {member.staff for member in members}
    duty_people = {shift.staff for shift in exclusive_schedule.shifts.order_by('start')[:members_count]}
    assert duty_people == {exclusive_duty}


def test_no_role(owner_role, transactional_db):
    service = factories.ServiceFactory()
    schedule = factories.ScheduleFactory(start_date=START_DATE, role=None, service=service)
    member = factories.StaffFactory()
    factories.ServiceMemberFactory(service=service, staff=member)
    owner = factories.StaffFactory()
    factories.ServiceMemberFactory(service=service, staff=owner, role=owner_role)
    assert set(schedule.shifts.values_list('staff', flat=True)) == {member.id}


@freezegun.freeze_time('2019-01-02')
def test_no_gaps(duty_data):
    duty_data.schedule.save()
    recalculate_all_duties()
    start = START_DATE
    end = start + timezone.timedelta(days=4)
    shifts = list(duty_data.schedule.shifts.order_by('start'))
    assert len(shifts) == 37
    for i, x in enumerate(shifts[:-3]):
        assert x.start == start
        assert x.end == end
        assert x.staff.login != shifts[i + 1].staff.login and x.staff.login != shifts[i + 2].staff.login
        start += timezone.timedelta(days=5)
        end += timezone.timedelta(days=5)


@freezegun.freeze_time('2019-01-02')
def test_two_persons(duty_data):
    duty_data.schedule.persons_count = 2
    duty_data.schedule.save()
    recalculate_all_duties()
    start = START_DATE
    end = start + timezone.timedelta(days=4)
    shifts = list(duty_data.schedule.shifts.order_by('start'))
    assert len(shifts) == 74
    for i in range(0, len(shifts) - 3, 2):
        assert shifts[i].start == shifts[i + 1].start == start
        assert shifts[i].end == shifts[i + 1].end == end
        assert shifts[i].staff != shifts[i + 1].staff
        start += timezone.timedelta(days=5)
        end += timezone.timedelta(days=5)


@freezegun.freeze_time('2019-01-01T00:00:00')
def test_gaps(duty_data):
    staff = duty_data.members[0].staff
    factories.GapFactory(
        staff=staff,
        start='2017-01-01T00:01:00Z',
        end=START_DATE + timezone.timedelta(days=32),
        work_in_absence=False,
        type='type',
        full_day=False
    )
    recalculate_all_duties()
    shifts = duty_data.schedule.shifts.filter(
        start__gte=START_DATE, start__lte=START_DATE + timezone.timedelta(days=29)
    )
    assert shifts.values_list('staff').distinct().count() == 2

    start = START_DATE + 7 * timezone.timedelta(days=5)
    end = start + timezone.timedelta(days=4)
    shifts = list(duty_data.schedule.shifts.filter(start__gt=START_DATE + timezone.timedelta(days=31)).order_by('start'))
    for i, x in enumerate(shifts[:-3]):
        assert not x.is_approved
        assert x.start == start
        assert x.end == end
        assert x.staff.login != shifts[i + 1].staff.login and x.staff.login != shifts[i + 2].staff.login
        start += timezone.timedelta(days=5)
        end += timezone.timedelta(days=5)


def test_two_schedules_same_members(duty_data):
    factories.ServiceMemberFactory(service=duty_data.service, role=duty_data.role)
    schedule_2 = factories.ScheduleFactory(
        role=duty_data.role,
        service=duty_data.service,
        start_date=duty_data.schedule.start_date,
    )
    duty_data.schedule.shifts.all().delete()
    recalculate_all_duties()
    schedule_1_staffs = set(duty_data.schedule.shifts.values_list('staff', flat=True))
    schedule_2_staffs = set(schedule_2.shifts.values_list('staff', flat=True))
    assert schedule_1_staffs == schedule_2_staffs
    schedule_1_shifts = {
        shift.start: shift
        for shift in duty_data.schedule.shifts.order_by('start')

    }
    for shift in schedule_2.shifts.order_by('start'):
        assert schedule_1_shifts[shift.start].staff != shift.staff


@freezegun.freeze_time('2018-12-31')
def test_recalculate_replace(duty_data_for_replace):
    """
    Проверим, что при пересчете календаря умеем делать замену
    """

    # перессчитаем календарь, с учетом гапа
    recalculate_all_duties()
    duty_data_for_replace.shift.refresh_from_db()
    assert duty_data_for_replace.shift.staff != duty_data_for_replace.old_staff


@freezegun.freeze_time('2018-12-31')
def test_recalculate_no_replace_absence_at_end_shift(duty_data_for_replace):
    """
    Проверим, что не пытаемся сделать замену,
    если есть несколько частичных замен на даты отсутствий
    в конце дежурства
    """
    for start, end in (
        ('2019-01-15', '2019-01-17'),
        ('2019-01-18', '2019-01-19'),
    ):
        factories.ShiftFactory(
            staff=duty_data_for_replace.new_staff,
            replace_for=duty_data_for_replace.shift,
            start=start,
            end=end,
            schedule=duty_data_for_replace.schedule,
        )

    # Сейчас в API добавление замены делает шифт подтвержденным
    duty_data_for_replace.shift.is_approved = True
    duty_data_for_replace.shift.save()

    # перессчитаем календарь, с учетом гапа
    recalculate_all_duties()
    duty_data_for_replace.shift.refresh_from_db()
    assert duty_data_for_replace.shift.staff == duty_data_for_replace.old_staff


@freezegun.freeze_time('2018-12-31')
def test_one_day_gap(duty_data):
    duty_data.schedule.save()
    recalculate_all_duties()
    shift = (
        duty_data.schedule.shifts
        .filter(start__gt=timezone.now().date() + 2 * duty_data.schedule.duration)
        .order_by('start').first()
    )

    assert not shift.has_problems
    assert not shift.problems.active().exists()

    shift.is_approved = True
    shift.save()

    staff = shift.staff

    factories.GapFactory(
        staff=staff,
        start='2019-01-15',
        end='2020-01-16',
        work_in_absence=False,
        type='type',
        full_day=False
    )
    recalculate_all_duties()
    shift.refresh_from_db()

    assert shift.has_problems
    assert shift.problems.active().get().reason == Problem.STAFF_HAS_GAP


@freezegun.freeze_time('2019-01-01')
@pytest.mark.parametrize(('gap_start', 'gap_end', 'has_problems'), [
    ('2019-01-04T20:00:00', '2019-01-04T21:00:00', False),  # до начала дежурства
    ('2019-01-10T06:00:00', '2019-01-10T09:00:00', False),  # после окончания
    ('2019-01-05T06:00:00', '2019-01-05T10:00:00', True),
    ('2019-01-05T01:00:00', '2019-01-05T10:00:00', True),
    ('2019-01-10T00:00:00', '2019-01-10T04:00:00', False),
])
def test_gaps_before_and_after_shifts(duty_data, gap_start, gap_end, has_problems):
    duty_data.schedule.save()
    assert duty_data.schedule.start_time == datetime.time(hour=0)
    recalculate_all_duties()
    shift = (
        duty_data.schedule.shifts
        .filter(start__gte=timezone.now().date())
        .order_by('start').first()
    )

    assert shift.end == timezone.datetime(2019, 1, 9).date()
    assert not shift.has_problems
    assert not shift.problems.active().exists()

    shift.is_approved = True
    shift.save()
    staff = shift.staff
    factories.GapFactory(
        staff=staff,
        start=datetime_from_str(gap_start).replace(tzinfo=timezone.utc),
        end=datetime_from_str(gap_end).replace(tzinfo=timezone.utc),
        work_in_absence=False,
        type='type',
        full_day=False
    )
    recalculate_all_duties()
    shift.refresh_from_db()

    assert shift.has_problems == has_problems
    if has_problems:
        assert shift.problems.active().get().reason == Problem.STAFF_HAS_GAP


@freezegun.freeze_time('2019-01-01')
@override_settings(DEFAULT_TIMEZONE=pytz.timezone('Europe/Moscow'))
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=30))
@pytest.mark.parametrize(('gap_start', 'gap_end', 'full_day', 'replace_start', 'replace_end', 'has_problems'), [
    ('2019-01-05T00:00:00', '2019-01-06T00:00:00', True, '2019-01-05', '2019-01-05', False),
    ('2019-01-10T01:00:00', '2019-01-10T04:00:00', False, '2019-01-09', '2019-01-09', False),
    ('2019-01-07T02:00:00', '2019-01-07T06:00:00', False, '2019-01-07', '2019-01-08', False),
    ('2019-01-07T01:00:00', '2019-01-07T3:00:00', False, '2019-01-07', '2019-01-07', False),
    ('2019-01-08T05:30:00', '2019-01-08T06:00:00', False, '2019-01-07', '2019-01-07', True),
    ('2019-01-06T00:00:00', '2019-01-07T00:00:00', True, '2019-01-06', '2019-01-06', False),
])
def test_gaps_with_replaces(duty_data, gap_start, gap_end, full_day, replace_start, replace_end, has_problems):
    duty_data.schedule.save()
    assert duty_data.schedule.start_time == datetime.time(hour=0)
    recalculate_all_duties()

    shift = (
        duty_data.schedule.shifts
        .filter(start__gte=timezone.now().date())
        .order_by('start').first()
    )
    assert shift.start == timezone.datetime(2019, 1, 5).date()
    assert shift.end == timezone.datetime(2019, 1, 9).date()
    old_staff = shift.staff

    shift.is_approved = True
    shift.save(update_fields=['is_approved'])
    shift.refresh_from_db()

    duty_data.members.append(factories.ServiceMemberFactory(service=duty_data.service,
                                                            role=duty_data.role))
    new_staff = duty_data.members[-1].staff

    assert old_staff != new_staff

    factories.ShiftFactory(
        staff=new_staff,
        replace_for=shift,
        start=replace_start,
        end=replace_end,
        schedule=duty_data.schedule,
    )

    assert not shift.has_problems
    assert not shift.problems.active().exists()

    factories.GapFactory(
        staff=old_staff,
        start=datetime_from_str(gap_start).replace(tzinfo=timezone.utc),
        end=datetime_from_str(gap_end).replace(tzinfo=timezone.utc),
        work_in_absence=False,
        type='type',
        full_day=full_day
    )

    scheduler = DutyScheduler.initialize_scheduler_by_shift(shift)
    scheduler.update_shift_problems(shift)

    recalculate_duty_for_service(duty_data.schedule.service_id)
    shift.refresh_from_db()

    assert shift.has_problems == has_problems
    if has_problems:
        assert shift.problems.active().get().reason == Problem.STAFF_HAS_GAP


@freezegun.freeze_time('2018-12-31')
def test_too_short_duty():

    owner = factories.StaffFactory()
    service = factories.ServiceFactory(owner=owner)
    role = factories.RoleFactory()
    members = []
    for _ in range(3):
        members.append(factories.ServiceMemberFactory(service=service, role=role))
    schedule = factories.ScheduleFactory(
        role=role,
        service=service,
        start_date=START_DATE,
        duration=timezone.timedelta(days=1),
    )

    recalculate_all_duties()

    shift = (
        schedule.shifts
        .filter(start__gt=timezone.now().date() + 2 * schedule.duration)
        .order_by('start').first()
    )

    assert not shift.has_problems
    assert not shift.problems.active().exists()

    shift.is_approved = True
    shift.save()

    staff = shift.staff

    factories.GapFactory(
        staff=staff,
        start='2019-01-2',
        end='2020-01-4',
        work_in_absence=False,
        type='type',
        full_day=False
    )
    recalculate_all_duties()
    shift.refresh_from_db()

    assert shift.has_problems
    assert shift.problems.active().get().reason == Problem.STAFF_HAS_GAP


@freezegun.freeze_time('2020-01-02')
@override_settings(DEFAULT_TIMEZONE=pytz.timezone('Europe/Moscow'))
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=30))
@pytest.mark.parametrize(('default_start_time', 'start_datetime', 'end_datetime'), [
    (True, datetime.datetime(2019, 12, 30, 0, 0), datetime.datetime(2020, 1, 4, 0, 0)),
    (False, datetime.datetime(2019, 12, 30, 12, 0), datetime.datetime(2020, 1, 6, 12, 0))
])
@pytest.mark.parametrize('holiday', [True, False])
def test_duty_on_weekends(default_start_time, start_datetime, end_datetime, holiday):
    """
    В этом тесте замокали период, на который создаются смены: вместо полгода - 30 дней.
    Поэтому всего смен 5.
    У графика установлены настройки: НЕ дежурить по выходным, но дежурить по праздникам.
    Праздник выпадает в будний, поэтому его наличие никак не повлияет.
    Если время дефолтное, то начавшая в пн смена должна закончится в пт.
    Иначе - в пн.

    Смены:
        * 30 декабря (пн) - 3 января (04.01 00:00) или 6 января (06.01 12:00)
        * 6 января (пн) - 10 января (или 13)
        * 13 января (пн) - 17 января (или 20)
        * 20 января (пн) - 24 января (или 27)
        * 27 января (пн) - 31 января (или 2)
    """

    service = factories.ServiceFactory()
    role = factories.RoleFactory()
    for _ in range(3):
        factories.ServiceMemberFactory(service=service, role=role)

    schedule = factories.ScheduleFactory(
        start_date=timezone.datetime(2019, 12, 30).date(),
        duty_on_weekends=False,
        duty_on_holidays=True,
        role=role,
        service=service,
        allow_sequential_shifts=False,
    )
    if not default_start_time:
        schedule.start_time = datetime.time(12, 00)
        schedule.save()

    # если есть праздничный день
    if holiday:
        factories.HolidayFactory(date='2020-01-07')

    recalculate_duty_for_service(service.id)

    shifts = list(schedule.shifts.order_by('start_datetime'))
    assert len(shifts) == 5

    # проверим даты первого шифта
    start_datetime = start_datetime.astimezone(settings.DEFAULT_TIMEZONE)
    end_datetime = end_datetime.astimezone(settings.DEFAULT_TIMEZONE)

    for i, x in enumerate(shifts):
        assert x.start_datetime.astimezone(settings.DEFAULT_TIMEZONE) == start_datetime
        assert x.end_datetime.astimezone(settings.DEFAULT_TIMEZONE) == end_datetime
        # стафф не равен следующему и предыдущему
        # проверяем если это не первый и не последний
        if i not in [0, 4]:
            assert x.staff.login != shifts[i + 1].staff.login
            assert x.staff.login != shifts[i - 1].staff.login
        start_datetime += timezone.timedelta(days=7)
        end_datetime += timezone.timedelta(days=7)


@freezegun.freeze_time('2020-01-02')
@override_settings(DEFAULT_TIMEZONE=pytz.timezone('Europe/Moscow'))
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=20))
@pytest.mark.parametrize('default_start_time', [True, False])
@pytest.mark.parametrize('holiday', [True, False])
def test_duty_on_holidays(default_start_time, holiday):
    """
    В этом тесте замокали период, на который создаются смены: вместо полгода - 20 дней.
    Поэтому всего смен 5.
    У графика установлены настройки: дежурить по выходным и не дежурить по праздникам.
    Если есть праздник, то смещаем эту смену, если есть обычные выходные, то нет.
    Если задан праздничный, то это 7 января
    Смены:
        * 30 декабря (пн) - 4 января (04.01 00:00 или 04.01 12:00)
        * 4 января (сб) - 9 января (или 10)
        * 9 (10) января - 14 января (или 15)
        * 14 (15) января - 19 января (или 20)
        * 19 (20) января - 24 января (или 25)
    """
    service = factories.ServiceFactory()
    role = factories.RoleFactory()
    for _ in range(3):
        factories.ServiceMemberFactory(service=service, role=role)

    schedule = factories.ScheduleFactory(
        start_date=timezone.datetime(2019, 12, 30).date(),
        duty_on_weekends=True,
        duty_on_holidays=False,
        role=role,
        service=service,
    )
    if not default_start_time:
        schedule.start_time = datetime.time(12, 00)
        schedule.save()

    # проверим, что есть выходные
    assert Holiday.objects.filter(date__range=(utils.today(), utils.today() + settings.DUTY_SCHEDULING_PERIOD))

    # если есть праздничный день
    if holiday:
        factories.HolidayFactory(date='2020-01-07')

    recalculate_all_duties()

    shifts = list(schedule.shifts.order_by('start_datetime'))
    assert len(shifts) == 5

    # проверим даты первого и второго шифтов
    assert (
        shifts[0].start_datetime.astimezone(settings.DEFAULT_TIMEZONE).date() ==
        datetime.datetime(2019, 12, 30).astimezone(settings.DEFAULT_TIMEZONE).date()
    )
    assert (
        shifts[0].end_datetime.astimezone(settings.DEFAULT_TIMEZONE).date() ==
        datetime.datetime(2020, 1, 4).astimezone(settings.DEFAULT_TIMEZONE).date()
    )
    if holiday:
        assert (
            shifts[1].start_datetime.astimezone(settings.DEFAULT_TIMEZONE).date() ==
            datetime.datetime(2020, 1, 4).astimezone(settings.DEFAULT_TIMEZONE).date()
        )
        assert (
            shifts[1].end_datetime.astimezone(settings.DEFAULT_TIMEZONE).date() ==
            datetime.datetime(2020, 1, 10).astimezone(settings.DEFAULT_TIMEZONE).date()
        )
    else:
        assert (
            shifts[1].start_datetime.astimezone(settings.DEFAULT_TIMEZONE).date() ==
            datetime.datetime(2020, 1, 4).astimezone(settings.DEFAULT_TIMEZONE).date()
        )
        assert (
            shifts[1].end_datetime.astimezone(settings.DEFAULT_TIMEZONE).date() ==
            datetime.datetime(2020, 1, 9).astimezone(settings.DEFAULT_TIMEZONE).date()
        )


@freezegun.freeze_time('2019-01-02')
def test_empty_shifts(duty_data):
    for member in duty_data.service.members.all():
        member.deprive()
    shifts = duty_data.schedule.shifts.all()
    assert shifts.count() == 37
    assert set(shifts.values_list('staff', flat=True)) == {None}
    member = factories.ServiceMemberFactory(service=duty_data.service, role=duty_data.role)
    recalculate_all_duties()
    assert set(shifts.future().values_list('staff', flat=True)) == {member.staff.pk}


@freezegun.freeze_time('2019-01-02')
def test_replace_replaced(duty_data_for_replace):
    duty_data_for_replace.schedule.save()
    shift_1, shift_2 = (
        duty_data_for_replace
        .schedule
        .shifts
        .filter(start__gt=timezone.now().date() + duty_data_for_replace.schedule.duration * 4)
        .order_by('start')
    )[:2]
    assert shift_1.staff != shift_2.staff and shift_1.staff is not None and shift_2.staff is not None
    shift_2.staff = shift_1.staff
    shift_2.save()
    recalculate_all_duties()
    shift_1.refresh_from_db()
    shift_2.refresh_from_db()
    assert shift_1.staff != shift_2.staff


@freezegun.freeze_time('2019-01-01')
def test_replace_stranger(duty_data_for_replace):
    shift = (
        duty_data_for_replace
        .schedule
        .shifts
        .filter(start__gt=timezone.now().date() + duty_data_for_replace.schedule.duration * 4)
        .order_by('start')
    ).first()
    old_staff = shift.staff
    start, end = shift.start, shift.end
    ServiceMember.objects.get(staff=shift.staff, role=shift.schedule.role).deprive()
    shift = duty_data_for_replace.schedule.shifts.get(start=start, end=end)
    assert old_staff != shift.staff


@freezegun.freeze_time('2019-01-02')
def test_problem_in_replaces(duty_data):
    DutyScheduler.recalculate_shifts(duty_data.schedule.service)

    members = duty_data.members
    shift = duty_data.schedule.shifts.get(
        start='2019-01-15',
        end='2019-01-19'
    )
    members = [member for member in members if member.staff != shift.staff]
    scheduler = DutyScheduler.initialize_scheduler_by_shift(shift)
    scheduler.update_shift_problems(shift)
    assert not shift.has_problems
    assert not shift.problems.active().exists()

    replace_good = factories.ShiftFactory(
        schedule=shift.schedule,
        replace_for=shift,
        start='2019-01-18',
        end='2019-01-19',
        staff=members[0].staff
    )
    del members[0]

    replace_bad = factories.ShiftFactory(
        schedule=shift.schedule,
        replace_for=shift,
        start='2019-01-15',
        end='2019-01-17',
        staff=members[0].staff
    )

    scheduler.fill_gaps()
    scheduler.update_shift_problems(shift, save_if_updated=True)
    shift.refresh_from_db()
    replace_good.refresh_from_db()
    replace_bad.refresh_from_db()
    assert not shift.has_problems
    assert not shift.problems.active().exists()
    assert not replace_good.has_problems
    assert not replace_good.problems.active().exists()
    assert not replace_bad.has_problems
    assert not replace_bad.problems.active().exists()

    factories.GapFactory(
        staff=replace_bad.staff,
        start='2019-01-15',
        end='2019-01-17',
        work_in_absence=False,
        type='type',
        full_day=True
    )

    scheduler.fill_gaps()
    scheduler.update_shift_problems(shift, save_if_updated=True)
    shift.refresh_from_db()
    replace_good.refresh_from_db()
    replace_bad.refresh_from_db()
    assert not replace_good.has_problems
    assert not replace_good.problems.active().exists()
    assert replace_bad.has_problems
    assert replace_bad.problems.active().exists()
    assert shift.has_problems
    assert not shift.problems.active().exists()


@freezegun.freeze_time('2019-01-02')
def test_bad_persons_count():
    service = factories.ServiceFactory()
    role = factories.RoleFactory()
    staff = factories.StaffFactory()
    factories.ServiceMemberFactory(staff=staff, service=service, role=role)
    schedule = factories.ScheduleFactory(
        role=role,
        persons_count=2,
        service=service,
        start_date=START_DATE,
    )
    recalculate_all_duties()

    start = START_DATE
    end = start + timezone.timedelta(days=4)
    shifts = list(schedule.shifts.order_by('start', 'staff'))

    assert len(shifts) == 74
    for i in range(0, len(shifts) - 3, 2):
        assert shifts[i].start == shifts[i + 1].start == start
        assert shifts[i].end == shifts[i + 1].end == end

        assert shifts[i].staff == staff
        assert not shifts[i].has_problems
        assert not shifts[i].problems.active().exists()

        assert shifts[i + 1].staff is None
        assert shifts[i + 1].has_problems
        assert shifts[i + 1].problems.active().get().reason == Problem.NOBODY_ON_DUTY

        start += timezone.timedelta(days=5)
        end += timezone.timedelta(days=5)


@freezegun.freeze_time('2019-01-01')
def test_deep_recalculate_duty_on_add_schedule():
    service = factories.ServiceFactory()
    role = factories.RoleFactory()
    staffs = []
    for _ in range(3):
        member = factories.ServiceMemberFactory(service=service, role=role)
        staffs.append(member.staff)
    schedule = factories.ScheduleFactory(
        role=role,
        service=service,
        start_date='2019-01-02',
        autoapprove_timedelta=timezone.timedelta(0),
    )

    recalculate_duty_for_service(service.id)
    shift_count = 5

    assert {shift.staff for shift in schedule.shifts.order_by('start')[:shift_count]} == set(staffs)

    new_role = factories.RoleFactory()
    factories.ServiceMemberFactory(staff=staffs[0], service=service, role=new_role)
    new_schedule = factories.ScheduleFactory(role=new_role, service=service, start_date='2019-01-02')

    recalculate_duty_for_service(service.id, [schedule.id, new_schedule.id])

    assert {shift.staff for shift in schedule.shifts.order_by('start')[1:shift_count]} == set(staffs[1:])
    assert {shift.staff for shift in new_schedule.shifts.order_by('start')[:shift_count]} == {staffs[0]}


@freezegun.freeze_time('2019-01-01')
def test_deep_recalculate_duty_on_change_algorithm():
    service = factories.ServiceFactory()
    role1 = factories.RoleFactory()
    role2 = factories.RoleFactory()
    staffs_in_schedule1 = []
    for _ in range(5):
        member = factories.ServiceMemberFactory(service=service, role=role1)
        staffs_in_schedule1.append(member.staff)
    schedule1 = factories.ScheduleFactory(
        role=role1,
        service=service,
        start_date='2019-01-02',
        autoapprove_timedelta=timezone.timedelta(0),
    )
    schedule2 = factories.ScheduleFactory(
        role=role2,
        service=service,
        start_date='2019-01-02',
        autoapprove_timedelta=timezone.timedelta(0),
    )

    staffs_in_schedule2 = staffs_in_schedule1[:1]
    factories.ServiceMemberFactory(staff=staffs_in_schedule1[0], service=service, role=role2)

    recalculate_duty_for_service(service.id)
    shift_count = 10

    found_staffs_in_schedule1 = {shift.staff for shift in schedule1.shifts.order_by('start')[:shift_count]}
    assert found_staffs_in_schedule1 == set(staffs_in_schedule1[1:])
    found_staffs_in_schedule2 = {shift.staff for shift in schedule2.shifts.order_by('start')[:shift_count]}
    assert found_staffs_in_schedule2 == set(staffs_in_schedule2)

    schedule2.consider_other_schedules = False
    schedule2.save()

    recalculate_duty_for_service(service.id, [schedule1.id, schedule2.id])

    found_staffs_in_schedule1 = {shift.staff for shift in schedule1.shifts.order_by('start')[:shift_count]}
    assert found_staffs_in_schedule1 == set(staffs_in_schedule1)
    found_staffs_in_schedule2 = {shift.staff for shift in schedule2.shifts.order_by('start')[:shift_count]}
    assert found_staffs_in_schedule2 == set(staffs_in_schedule2)


@freezegun.freeze_time('2019-01-02')
def test_deep_recalculate_duty_started_shift():
    service = factories.ServiceFactory()
    role_big = factories.RoleFactory()
    role_small = factories.RoleFactory()
    user_a = factories.StaffFactory()
    user_b = factories.StaffFactory()
    factories.ServiceMemberFactory(staff=user_a, service=service, role=role_big)
    factories.ServiceMemberFactory(staff=user_b, service=service, role=role_big)
    factories.ServiceMemberFactory(staff=user_b, service=service, role=role_small)
    schedule_big = factories.ScheduleFactory(
        service=service,
        role=role_big,
        start_date='2019-01-01',
        name='big',
    )
    schedule_small = factories.ScheduleFactory(
        service=service,
        role=role_small,
        start_date='2019-01-01',
        name='small',
    )
    ids = [schedule_big.id, schedule_small.id]
    DutyScheduler.recalculate_shifts(service, full_recalculate_schedules_ids=ids)
    shift = schedule_big.shifts.order_by('start').first()
    shift.state = Shift.STARTED
    shift.save()
    factories.GapFactory(
        staff=user_b,
        start='2019-01-01T00:00:00Z',
        end='2019-01-03T00:00:00Z',
        work_in_absence=False,
        type='type',
        full_day=False
    )
    DutyScheduler.recalculate_shifts(service, full_recalculate_schedules_ids=ids)
    assert set(schedule_big.shifts.values_list('staff__id', flat=True)) == {user_a.id}


@freezegun.freeze_time('2019-01-02')
def test_deep_recalculate_approved_shift_with_started():
    service = factories.ServiceFactory()
    role = factories.RoleFactory()
    for _ in range(3):
        user = factories.StaffFactory()
        factories.ServiceMemberFactory(staff=user, service=service, role=role)
    schedule = factories.ScheduleFactory(
        service=service,
        role=role,
        start_date='2019-01-01',
    )
    DutyScheduler.recalculate_shifts(service)
    approved_shift = schedule.shifts.order_by('start')[5]
    approved_shift.is_approved = True
    approved_shift.save()
    started_shift = schedule.shifts.order_by('start')[0]
    started_shift.state = Shift.STARTED
    started_shift.save()
    DutyScheduler.recalculate_shifts(service, full_recalculate_schedules_ids=[schedule.id])
    # Стартонувший шифт не пропал после пересчета
    assert schedule.shifts.order_by('start')[0].id == started_shift.id
    # Подвержденный шифт во-первых не пропал, во-вторых перед ним поместились шифты.
    assert schedule.shifts.order_by('start')[5].id == approved_shift.id


@pytest.fixture
@freezegun.freeze_time('2019-01-02')
def duty_data_for_manual_order(duty_role):
    staff_count = 5
    service = factories.ServiceFactory()
    role = factories.RoleFactory()
    staffs = [
        factories.ServiceMemberFactory(service=service, role=role).staff
        for _ in range(staff_count)
    ]
    schedule = factories.ScheduleFactory(
        service=service,
        role=role,
        algorithm=Schedule.MANUAL_ORDER,
        start_date=START_DATE,
        autoapprove_timedelta=timezone.timedelta(0),
    )
    return pretend.stub(
        staffs=staffs,
        schedule=schedule
    )


@freezegun.freeze_time('2019-01-02')
def test_manual_order_single_staff(duty_data_for_manual_order):
    schedule = duty_data_for_manual_order.schedule
    staff_order = [0, 2, 1]
    for order, staff in enumerate(staff_order):
        Order.objects.create(schedule=schedule, staff=duty_data_for_manual_order.staffs[staff], order=order)
    recalculate_all_duties()
    indexes = staff_order * 5

    shifts = schedule.shifts.order_by('start')[:len(indexes)]

    for index, shift in zip(indexes, shifts):
        assert shift.staff == duty_data_for_manual_order.staffs[index]

    factories.GapFactory(
        staff=shifts[0].staff,
        start=timezone.datetime(2019, 1, 3, 0, 0),
        end=timezone.datetime(2019, 1, 5, 0, 0),
        work_in_absence=False,
        type='type',
        full_day=False
    )

    with freezegun.freeze_time('2019-01-04'):
        recalculate_all_duties()
        shifts[0].refresh_from_db()
        assert shifts[0].has_problems


@freezegun.freeze_time('2019-01-02')
def test_manual_order_multi_staff(duty_data_for_manual_order):
    schedule = duty_data_for_manual_order.schedule
    schedule.persons_count = 2
    schedule.save()
    staffs = list(reversed(duty_data_for_manual_order.staffs))
    for order, staff in enumerate(staffs):
        Order.objects.create(schedule=schedule, staff=staff, order=order)
    recalculate_all_duties()
    shifts = list(schedule.shifts.order_by('index')[:5])
    assert shifts[0].start == datetime.date(2018, 12, 31)
    assert shifts[0].end == datetime.date(2019, 1, 4)
    assert shifts[0].start == shifts[1].start
    assert shifts[0].end == shifts[1].end
    assert shifts[2].start == datetime.date(2019, 1, 5)
    assert shifts[2].end == datetime.date(2019, 1, 9)
    assert shifts[2].start == shifts[3].start
    assert shifts[2].end == shifts[3].end
    for index in range(len(shifts)):
        assert shifts[index].staff == staffs[index]


@freezegun.freeze_time('2019-01-02T12:00:00')
def test_order_with_offset_create(duty_data_for_manual_order):
    schedule = duty_data_for_manual_order.schedule
    staffs = duty_data_for_manual_order.staffs
    for order, staff in enumerate(staffs):
        Order.objects.create(schedule=schedule, staff=staff, order=order)
    offset = ManualOrderingScheduler.get_offset(schedule, staffs[-2])
    schedule.manual_ordering_offset = offset
    schedule.save()
    recalculate_all_duties()
    with patch('plan.api.idm.actions.request_membership', Mock(return_value={'id': 1})):
        check_current_shifts()
    active_shift = schedule.shifts.get(state=Shift.STARTED)
    # Дежурство начинается с того, кого нужно
    assert active_shift.staff == staffs[-2]

    new_order = staffs[:]
    new_order.insert(0, new_order[-1])
    del new_order[-1]
    new_order.insert(0, new_order[-1])
    del new_order[-1]
    new_order *= 3

    # Проверяем порядок
    for staff, shift in zip(new_order, schedule.shifts.order_by('start')[:len(new_order)]):
        assert staff == shift.staff
    # Проверяем, что ничего не сломалось после обновления
    recalculate_all_duties()
    for staff, shift in zip(new_order, schedule.shifts.order_by('start')[:len(new_order)]):
        assert staff == shift.staff


@freezegun.freeze_time('2019-01-02T12:00:00')
def test_order_with_offset_update(duty_data_for_manual_order):
    schedule = duty_data_for_manual_order.schedule
    staffs = duty_data_for_manual_order.staffs
    for order, staff in enumerate(staffs):
        Order.objects.create(schedule=schedule, staff=staff, order=order)
    recalculate_all_duties()
    with patch('plan.api.idm.actions.request_membership', Mock(return_value={'id': 1})):
        check_current_shifts()
    active_shift = schedule.shifts.get(state=Shift.STARTED)
    assert active_shift.staff == staffs[0]
    offset = ManualOrderingScheduler.get_offset(schedule, staffs[-1])
    schedule.manual_ordering_offset = offset
    schedule.save()
    recalculate_all_duties()
    active_shift.refresh_from_db()
    assert active_shift.staff == staffs[0]

    new_order = staffs[:]
    new_order.insert(0, new_order[-1])
    del new_order[-1]
    new_order *= 3

    shifts = schedule.shifts.order_by('start').filter(state=Shift.SCHEDULED)
    for staff, shift in zip(new_order, shifts[:len(new_order)]):
        assert staff == shift.staff


@freezegun.freeze_time('2019-05-01')
def test_recalculate_not_approved_shift_has_problem():
    """
    Если при пересчете для неподтвержденной смены не смогли найти замену,
    то переносим смену в "Дежурный не назначен"
    """

    service = factories.ServiceFactory()
    role = factories.RoleFactory()
    schedule = factories.ScheduleFactory(service=service, role=role, start_date='2019-06-01', algorithm=Schedule.NO_ORDER)
    staff = factories.StaffFactory()
    factories.ServiceMemberFactory(staff=staff, service=service, role=role)

    recalculate_duty_for_service(service.id)

    shift = schedule.shifts.filter(start__gt=timezone.datetime(2019, 6, 1).date()).first()
    shift.is_approved = False
    shift.save(update_fields=['is_approved'])

    factories.GapFactory(
        staff=staff,
        start='2019-04-01T00:01:00Z',
        end='2019-08-01T12:12:12Z',
        work_in_absence=False,
    )

    recalculate_duty_for_service(service.id)
    shift.refresh_from_db()

    assert shift.staff is None


@freezegun.freeze_time('2019-05-01')
def test_recalculate_on_approved_shift(transactional_db):
    service = factories.ServiceFactory()
    role = factories.RoleFactory()
    schedule = factories.ScheduleFactory(
        service=service,
        role=role,
        start_date=timezone.datetime(2019, 5, 1),
    )
    staff_a = factories.StaffFactory()
    staff_b = factories.StaffFactory()
    factories.ServiceMemberFactory(staff=staff_a, service=service, role=role)
    membership = factories.ServiceMemberFactory(staff=staff_b, service=service, role=role)
    shift = schedule.shifts.filter(staff=staff_b).first()
    shift.is_approved = True
    shift.save()
    membership.deprive()
    assert set(schedule.shifts.values_list('staff', flat=True)) == {staff_a.id}


@freezegun.freeze_time('2019-05-01')
def test_recalculate_no_owner(transactional_db):
    service = factories.ServiceFactory(owner=None)
    role = factories.RoleFactory()
    schedule = factories.ScheduleFactory(
        service=service,
        role=role,
        duration=timezone.timedelta(days=3),
        start_date=timezone.datetime(2019, 5, 1),
    )
    staff = factories.StaffFactory()
    gap = factories.GapFactory(
        staff=staff,
        start='2019-05-05',
        end=START_DATE + timezone.timedelta(days=30),
        work_in_absence=False,
        type='type',
        full_day=False
    )
    factories.ServiceMemberFactory(staff=staff, service=service, role=role)
    # перерасчет не упал на попав на gap
    assert schedule.shifts.filter(start__gt=gap.end).exists()


@freezegun.freeze_time('2019-01-01')
def test_recalculate_manual_order_remove_user(transactional_db):
    service = factories.ServiceFactory()
    role = factories.RoleFactory()
    membership = factories.ServiceMemberFactory(role=role, service=service)
    staff = membership.staff
    schedule = factories.ScheduleFactory(
        service=service,
        role=None,
        duration=timezone.timedelta(days=3),
        start_date=timezone.datetime(2019, 1, 1),
        algorithm=Schedule.MANUAL_ORDER,
    )
    factories.OrderFactory(staff=staff, order=0, schedule=schedule)
    recalculate_all_duties()
    assert all([shift.staff == staff for shift in schedule.shifts.all()])
    membership.deprive()
    assert all([shift.staff is None for shift in schedule.shifts.all()])


@freezegun.freeze_time('2019-05-01')
def test_recalculate_manual_order_1(transactional_db):
    service = factories.ServiceFactory()
    role = factories.RoleFactory()

    membership = factories.ServiceMemberFactory(service=service, role=role)
    staff_a = factories.ServiceMemberFactory(service=service, role=role).staff
    staff_b = factories.ServiceMemberFactory(service=service, role=role).staff
    staff_c = membership.staff

    schedule = factories.ScheduleFactory(
        service=service,
        role=role,
        algorithm=Schedule.MANUAL_ORDER,
        start_date='2019-04-30',
    )

    order = factories.OrderFactory(schedule=schedule, staff=staff_c, order=0)
    factories.OrderFactory(schedule=schedule, staff=staff_a, order=1)
    factories.OrderFactory(schedule=schedule, staff=staff_b, order=2)

    recalculate_duty_for_service(schedule.service.id)
    shifts = schedule.shifts.filter(staff=staff_c)
    shift = shifts.first()
    assert set(schedule.shifts.values_list('staff', flat=True)) == set(schedule.orders.values_list('staff', flat=True))

    membership.deprive()

    schedule.refresh_from_db()
    shift.refresh_from_db()
    order.refresh_from_db()

    assert shift.staff is None
    assert shifts.count() == 0
    assert order.staff is None


@freezegun.freeze_time('2019-05-01')
def test_recalculate_resolved_problem(transactional_db):
    service = factories.ServiceFactory()
    role = factories.RoleFactory()

    membership = factories.ServiceMemberFactory(service=service, role=role)
    staff_a = factories.ServiceMemberFactory(service=service, role=role).staff
    staff_b = factories.ServiceMemberFactory(service=service, role=role).staff
    staff_c = membership.staff

    schedule = factories.ScheduleFactory(
        service=service,
        role=role,
        algorithm=Schedule.MANUAL_ORDER,
        start_date='2019-04-30',
    )

    factories.OrderFactory(schedule=schedule, staff=staff_a, order=1)
    factories.OrderFactory(schedule=schedule, staff=staff_c, order=0)
    factories.OrderFactory(schedule=schedule, staff=staff_b, order=2)

    recalculate_duty_for_service(schedule.service.id)
    shifts = schedule.shifts.filter(staff=staff_c)
    shift = shifts.last()

    membership.deprive()

    schedule.refresh_from_db()
    shift.refresh_from_db()

    assert shift.staff is None
    assert shift.has_problems

    problem = shift.problems.first()
    assert problem.status == Problem.NEW

    # решим проблему
    shift.staff = staff_b
    shift.is_approved = True
    shift.save(update_fields=['staff', 'is_approved'])

    recalculate_duty_for_service(schedule.service.id)
    problem.refresh_from_db()

    assert problem.status == Problem.RESOLVED
    assert problem.resolve_date is not None


@freezegun.freeze_time('2019-01-01')
def test_no_hole_after_adding_staff_in_manual_order():
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
        start_date='2019-01-01',
        duration=timezone.timedelta(days=1),
    )
    for i, staff in enumerate(staffs):
        factories.OrderFactory(schedule=schedule, staff=staff, order=i)
    recalculate_all_duties()

    schedule.shifts.filter(start=timezone.datetime(2019, 1, 5)).update(is_approved=True)
    start_must_exists = list(schedule.shifts.values_list('start', flat=True)[:3])

    factories.ServiceMemberFactory(service=service, role=role)

    for start in start_must_exists:
        assert schedule.shifts.filter(start=start).count() == 1


@freezegun.freeze_time('2020-01-01')
def test_deny_repeat():
    service = factories.ServiceFactory()
    member = factories.ServiceMemberFactory(service=service)
    schedule = factories.ScheduleFactory(
        service=service,
        role=member.role,
        allow_sequential_shifts=False,
        start_date='2020-01-01',
        duration=timezone.timedelta(days=5),
    )
    recalculate_all_duties()
    shifts = schedule.shifts.order_by('start').select_related('staff')
    assert shifts[0].staff == member.staff
    assert shifts[1].staff is None


@freezegun.freeze_time('2020-01-01')
def test_deny_repeat_multiple_shifts():
    service = factories.ServiceFactory()
    role = factories.RoleFactory()
    all_staff = {
        factories.ServiceMemberFactory(service=service, role=role).staff.id
        for _ in range(3)
    }
    schedule = factories.ScheduleFactory(
        service=service,
        role=role,
        persons_count=3,
        allow_sequential_shifts=False,
        start_date='2020-01-01',
        duration=timezone.timedelta(days=5),
    )
    recalculate_all_duties()
    shifts = schedule.shifts.order_by('start', 'id')
    assert set(shifts[:3].values_list('staff_id', flat=True)) == all_staff
    assert set(shifts[3:6].values_list('staff_id', flat=True)) == {None}


@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=60))
@freezegun.freeze_time('2020-01-01')
def test_different_sizes():
    """
    Замокала период, на который создаются смены.
    Рассчёта на 2 месяца должно хватить.
    """

    start_date = timezone.datetime(2020, 1, 1).date()
    service = factories.ServiceFactory()
    role = factories.RoleFactory()
    for _ in range(3):
        factories.ServiceMemberFactory(service=service, role=role)
    schedule_long = factories.ScheduleFactory(
        service=service,
        start_date=start_date,
        role=role,
        duration=timezone.timedelta(days=5),
    )
    schedule_medium = factories.ScheduleFactory(
        service=service,
        start_date=start_date,
        role=role,
        duration=timezone.timedelta(days=3),
    )
    schedule_short = factories.ScheduleFactory(
        service=service,
        start_date=start_date,
        role=role,
        duration=timezone.timedelta(days=1),
    )
    recalculate_all_duties()
    assert not schedule_long.shifts.filter(staff=None).exists()
    assert not schedule_medium.shifts.filter(staff=None).exists()
    assert not schedule_short.shifts.filter(staff=None).exists()

    schedule_medium.start_date = start_date + timezone.timedelta(days=1)
    schedule_medium.save()

    recalculate_duty_for_service(service.id, full_recalculate_schedules_ids=[schedule_medium.id])

    assert not schedule_long.shifts.filter(staff=None).exists()
    assert not schedule_medium.shifts.filter(staff=None).exists()
    assert not schedule_short.shifts.filter(staff=None).exists()


@freezegun.freeze_time('2020-01-01')
def test_multi_sequential():
    start_date = timezone.datetime(2020, 1, 1).date()
    service = factories.ServiceFactory()
    role = factories.RoleFactory()
    for _ in range(3):
        factories.ServiceMemberFactory(service=service, role=role).staff
    schedule_long = factories.ScheduleFactory(
        service=service,
        start_date=start_date,
        role=role,
        duration=timezone.timedelta(days=5),
        allow_sequential_shifts=False,
    )
    schedule_medium = factories.ScheduleFactory(
        service=service,
        start_date=start_date,
        role=role,
        duration=timezone.timedelta(days=3),
        allow_sequential_shifts=False,
    )
    schedule_short = factories.ScheduleFactory(
        service=service,
        start_date=start_date,
        role=role,
        duration=timezone.timedelta(days=1),
        allow_sequential_shifts=False,
    )
    recalculate_all_duties()
    schedule_medium.allow_sequential_shifts = True
    schedule_medium.save()
    recalculate_duty_for_service(service.id, [schedule_medium.id])

    shifts = list(schedule_long.shifts.order_by('start'))
    for i in range(len(shifts) - 1):
        if shifts[i].staff and shifts[i+1].staff:
            assert shifts[i].staff != shifts[i+1].staff
    shifts = list(schedule_short.shifts.order_by('start'))
    for i in range(len(shifts) - 1):
        if shifts[i].staff and shifts[i+1].staff:
            assert shifts[i].staff != shifts[i+1].staff


@pytest.mark.parametrize('algorithm', [Schedule.MANUAL_ORDER, Schedule.NO_ORDER])
def test_delete_current_duty(algorithm, transactional_db):
    """
    При удалении человека из роли, если он является текущим дежурным,
    новые смены не должны пересчитываться на него же.
    """

    service = factories.ServiceFactory()
    role = factories.RoleFactory()
    schedule = factories.ScheduleFactory(
        service=service,
        role=role,
        algorithm=algorithm,
        autoapprove_timedelta=timezone.timedelta(0),
    )

    for index in range(3):
        member = factories.ServiceMemberFactory(service=service, role=role)
        if algorithm == Schedule.MANUAL_ORDER:
            factories.OrderFactory(
                order=index,
                staff=member.staff,
                schedule=schedule,
            )

    recalculate_duty_for_service(service.id)
    shift = schedule.shifts.order_by('start').first()
    shift.state = Shift.STARTED
    shift.save()
    staff = shift.staff

    # удалим роль и пересчитаем график
    member = ServiceMember.objects.get(service=service, role=role, staff=staff)
    member.deprive()
    recalculate_duty_for_service(service.id)

    shifts = schedule.shifts.filter(staff=staff)
    assert shifts.count() == 0


def test_delete_duplicate_duty_role():
    """
    При отзыве роли, дежурства не пересчитываются, если у человека есть ещё одна такая же роль
    """
    service = factories.ServiceFactory()
    role = factories.RoleFactory()
    schedule = factories.ScheduleFactory(service=service, role=role, algorithm=Schedule.NO_ORDER,)
    department_member = factories.ServiceMemberDepartmentFactory()
    staff = factories.StaffFactory()

    member1 = factories.ServiceMemberFactory(service=service, role=role, staff=staff)
    factories.ServiceMemberFactory(service=service, role=role, staff=staff, from_department=department_member)
    recalculate_duty_for_service(service.id)
    shifts_ids = set(schedule.shifts.values_list('id', flat=True))
    assert set(schedule.shifts.values_list('staff_id', flat=True)) == {staff.id}
    member1.deprive()
    # Проверим, что отзыв member1 не привело к пересчету графика
    schedule.refresh_from_db()
    assert set(schedule.shifts.values_list('id', flat=True)) == shifts_ids


@freezegun.freeze_time('2020-04-02')
@override_settings(DEFAULT_TIMEZONE=pytz.timezone('Europe/Moscow'))
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=5))
@pytest.mark.parametrize('algorithm', [Schedule.NO_ORDER, Schedule.MANUAL_ORDER])
def test_recalculate_with_starttime(algorithm):
    """
    Дата старта: 3 апреля, пт.
    Если график учитывает только рабочие дни (выключены оба флага), смена заканчивается в пт, а следующая начинается в пн,
    то при пересчет задваивались смены, создавали новую смену с сб, 4 апреля.
    При этом смена за 6 апреля также оставалась жить.

    Как надо:
        * должна остаться 1 смена в пт и 1 смена в пн.

    В этом тесте замокали период, на который создаются смены: вместо полгода - 5 дней (нужны только две смены).
    """

    friday = timezone.datetime(2020, 4, 3)
    monday = timezone.datetime(2020, 4, 6)

    service = factories.ServiceFactory()
    role = factories.RoleFactory()
    schedule = factories.ScheduleFactory(
        service=service,
        start_date=friday.date(),
        role=role,
        duration=timezone.timedelta(days=1),
        algorithm=algorithm,
        duty_on_weekends=False,
        duty_on_holidays=False,
    )

    for _ in range(3):
        factories.ServiceMemberFactory(service=service, role=role)

    recalculate_duty_for_service(service.id)

    # поправим смену, тк она будет заканчиваться в пн
    shift = schedule.shifts.first()
    shift.end = friday.date()
    shift.end_datetime = make_localized_datetime(shift.end + timezone.timedelta(days=1), schedule.start_time)
    shift.save()
    shift.refresh_from_db()

    recalculate_duty_for_service(service.id)

    shifts = schedule.shifts.filter(start__lte=monday.date()).order_by('start_datetime')
    # shifts[0] - смена за пятницу
    # shifts[1] - смена за понедельник
    assert len(shifts) == 2
    assert shifts[0].start == datetime.datetime(2020, 4, 3).date()
    assert shifts[0].end == datetime.datetime(2020, 4, 3).date()
    assert shifts[1].start == datetime.datetime(2020, 4, 6).date()
    assert shifts[1].end == datetime.datetime(2020, 4, 6).date()


@freezegun.freeze_time('2020-04-02')
@override_settings(DEFAULT_TIMEZONE=pytz.timezone('Europe/Moscow'))
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=25))
@pytest.mark.parametrize('algorithm', [Schedule.NO_ORDER, Schedule.MANUAL_ORDER])
def test_recalculate_with_edit_starttime_algorithm(algorithm):
    """
    Поменяем у графика время начала графика с дефолтного 00:00 по Мск, на какое-нибудь ккастомное значение.
    Первые три смены должны быть такие:
        * с 3 апреля 16-00 по Мск до 10 апреля 16-00 по Мск
        * с 10 апреля 16-00 по Мск до 17 апреля 16-00 по Мск
        * с 17 апреля 16-00 по Мск до 24 апреля 16-00 по Мск
    В промежутках не должно быть дополнительных смен.

    В этом тесте замокали период, на который создаются смены: вместо полгода - 25 дней
        (последняя интересующая нас смена == 24 апреля).
    """

    start = timezone.datetime(2020, 4, 3)
    service = factories.ServiceFactory()
    role = factories.RoleFactory()
    schedule = factories.ScheduleFactory(
        service=service,
        start_date=start.date(),
        role=role,
        duration=timezone.timedelta(days=5),
        algorithm=algorithm,
        duty_on_weekends=False,
        duty_on_holidays=False,
    )

    for _ in range(3):
        factories.ServiceMemberFactory(service=service, role=role)

    recalculate_duty_for_service(service.id)

    # поменяем время и пересчитаем
    schedule.start_time = datetime.time(16, 0)
    schedule.save()
    recalculate_duty_for_service(service.id)

    shifts = schedule.shifts.order_by('start_datetime')[:3]

    assert (
        shifts[0].start_datetime.astimezone(settings.DEFAULT_TIMEZONE) ==
        datetime.datetime(2020, 4, 3, 16, 0).astimezone(settings.DEFAULT_TIMEZONE)
    )
    assert (
        shifts[0].end_datetime.astimezone(settings.DEFAULT_TIMEZONE) ==
        datetime.datetime(2020, 4, 10, 16, 0).astimezone(settings.DEFAULT_TIMEZONE)
    )

    assert (
        shifts[1].start_datetime.astimezone(settings.DEFAULT_TIMEZONE) ==
        datetime.datetime(2020, 4, 10, 16, 0).astimezone(settings.DEFAULT_TIMEZONE)
    )
    assert (
        shifts[1].end_datetime.astimezone(settings.DEFAULT_TIMEZONE) ==
        datetime.datetime(2020, 4, 17, 16, 0).astimezone(settings.DEFAULT_TIMEZONE)
    )

    assert (
        shifts[2].start_datetime.astimezone(settings.DEFAULT_TIMEZONE) ==
        datetime.datetime(2020, 4, 17, 16, 0).astimezone(settings.DEFAULT_TIMEZONE)
    )
    assert (
        shifts[2].end_datetime.astimezone(settings.DEFAULT_TIMEZONE) ==
        datetime.datetime(2020, 4, 24, 16, 0).astimezone(settings.DEFAULT_TIMEZONE)
    )


@freezegun.freeze_time('2020-05-28')
@override_settings(DEFAULT_TIMEZONE=pytz.timezone('Europe/Moscow'))
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=10))
@pytest.mark.parametrize('api', ['api-v3', 'api-v4'])
@pytest.mark.parametrize(('holiday', 'result_end'), [
    (True, datetime.datetime(2020, 5, 31, 0, 0)),
    (False, datetime.datetime(2020, 6, 2, 0, 0)),
])
def test_recalculate_with_edit_starttime(client, owner_role, transactional_db, person, api, holiday, result_end):
    """
    Если дата старта прошлая, то при снятии галки duty_on_weekends
    смены, перекрывающие выходные, были на 1 день короче.

    Выходные должны считаться корректно и, если нет праздника, смена заканчивается во вт 00:00, а не пн
        * 29 мая - пт
        * с 29 мая 00-00 по Мск до 2 июня 00-00 по Мск

    Наличие праздника повлияет: праздник будет рабочим, тогда cмена заканчивается в пн
        * с 29 мая 00-00 по Мск до 31 мая 00-00 по Мск

    В этом тесте замокали период, на который создаются смены: вместо полгода - 10 дней
        (проверяем только 1 смену).
    """

    start = timezone.datetime(2020, 5, 15)
    service = factories.ServiceFactory(owner=person)
    factories.ServiceMemberFactory(service=service, staff=service.owner, role=owner_role)
    role = factories.RoleFactory()
    for _ in range(3):
        factories.ServiceMemberFactory(service=service, role=role)

    schedule = factories.ScheduleFactory(
        service=service,
        start_date=start.date(),
        role=role,
        duration=timezone.timedelta(days=2),
        algorithm=Schedule.NO_ORDER,
        duty_on_weekends=True,
        duty_on_holidays=True,
    )

    for _ in range(3):
        factories.ServiceMemberFactory(service=service, role=role)

    if holiday:
        Holiday.objects.filter(date='2020-05-30').update(is_holiday=True)

    recalculate_duty_for_service(service.id)

    client.login(service.owner.login)

    response = client.json.patch(
        reverse(f'{api}:duty-schedule-detail', args=[schedule.pk]),
        data={
            'duty_on_weekends': False,
        }
    )
    assert response.status_code == 200

    shifts = schedule.shifts.future().order_by('start_datetime')[:3]

    assert (
        shifts[0].start_datetime.astimezone(settings.DEFAULT_TIMEZONE) ==
        datetime.datetime(2020, 5, 29, 0, 0).astimezone(settings.DEFAULT_TIMEZONE)
    )
    assert (
        shifts[0].end_datetime.astimezone(settings.DEFAULT_TIMEZONE) ==
        result_end.astimezone(settings.DEFAULT_TIMEZONE)
    )


@freezegun.freeze_time('2020-01-01')
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=25))
def test_replace_staff_for_next_staff():
    """
    Проверяем лесенку дежурств.
    Подтвердили несколько смен, но смену, где сделали замену - не подтверждали, она должна пересчитаться правильно.
    Изначально:
        * staff_0 => shift_0 (appr), shift_2 (appr)
        * staff_1 => shift_1, shift_3 (appr)

    После замены и пересчёта
        * staff_0 => shift_0 (appr), shift_2 (appr), shift_3 (appr)
        * staff_1 => shift_1

    В этом тесте замокали период, на который создаются смены: вместо полгода - 25 дней (интересует всего 4 смены)
    """

    start = timezone.datetime(2020, 1, 2)
    service = factories.ServiceFactory()
    role = factories.RoleFactory()
    for _ in range(2):
        factories.ServiceMemberFactory(service=service, role=role)

    factories.ScheduleFactory(
        service=service,
        start_date=start.date(),
        role=role,
        duration=timezone.timedelta(days=5),
        algorithm=Schedule.NO_ORDER,
        autoapprove_timedelta=timezone.timedelta(0)
    )

    recalculate_duty_for_service(service.id)

    shifts = list(Shift.objects.order_by('start')[:4])

    Shift.objects.filter(pk__in=[shifts[1].id, shifts[3].id]).update(staff=shifts[0].staff)
    Shift.objects.filter(pk__in=[shifts[0].id, shifts[2].id, shifts[3].id]).update(is_approved=True)

    recalculate_duty_for_service(service.id)
    shifts = Shift.objects.order_by('start')[0:4]

    assert shifts[1].staff != shifts[0].staff
    assert shifts[2].staff == shifts[0].staff
    assert shifts[3].staff == shifts[2].staff


@freezegun.freeze_time('2020-01-01')
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=20))
def test_remove_staff_with_approved_shift(transactional_db):
    """
    Проверяем лесенку дежурств.
    Удаляем человека из роли, для этого стаффа есть подтверждённая смена.

    В этом тесте замокали период, на который создаются смены: вместо полгода - 20 дней (интересует всего 3 смены)
    """
    start = timezone.datetime(2020, 1, 2)
    service = factories.ServiceFactory()
    role = factories.RoleFactory()
    for _ in range(3):
        factories.ServiceMemberFactory(service=service, role=role)

    factories.ScheduleFactory(
        service=service,
        start_date=start.date(),
        role=role,
        duration=timezone.timedelta(days=5),
        algorithm=Schedule.NO_ORDER,
    )

    recalculate_duty_for_service(service.id)

    approved_shift = Shift.objects.order_by('start')[2]
    approved_shift.is_approved = True
    approved_shift.save()
    approved_staff = approved_shift.staff
    ServiceMember.objects.get(staff=approved_staff, role=role).deprive()

    recalculate_duty_for_service(service.id)

    changed_shift = Shift.objects.order_by('start')[2]
    assert changed_shift.staff is not None
    assert changed_shift.staff != approved_staff
    assert changed_shift.is_approved is False


@freezegun.freeze_time('2020-05-20')
@pytest.mark.parametrize('consider_other_schedules', (False, True))
def test_removal_from_duty_changes_current_shift(client, consider_other_schedules, transactional_db):
    """
    Проверяем, что при удалении человека из дежурства текущие смены с его участием обновятся:
    снимется подтверждение и удалится стафф
    """
    service = factories.ServiceFactory()
    start = timezone.datetime(2020, 5, 21, 12)
    role = factories.RoleFactory()
    for _ in range(3):
        factories.ServiceMemberFactory(service=service, role=role)
    schedule = factories.ScheduleFactory(
        service=service,
        start_date=start.date(),
        start_time=start.time(),
        duration=timezone.timedelta(days=5),
        algorithm=Schedule.NO_ORDER,
        consider_other_schedules=consider_other_schedules,
        role=role,
        autoapprove_timedelta=timezone.timedelta(0),
    )

    recalculate_all_duties()

    shift1 = Shift.objects.filter(schedule=schedule).get(start='2020-05-26')
    shift1.is_approved = True
    shift1.save()

    with freezegun.freeze_time('2020-05-28'):
        ServiceMember.objects.get(staff=shift1.staff, role=schedule.role).deprive()
        shift1.refresh_from_db()
        assert shift1.is_approved is False
        assert shift1.staff is None


@freezegun.freeze_time('2020-06-10')
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=15))
@pytest.mark.parametrize('api', ['api-v3', 'api-v4'])
def test_recalculate_duty_on_weekends(client, owner_role, transactional_db, person, api):
    """
    Меняем настройку учитывать выходные дни: сначала считали по всем дням, затем вырезаем выходные.
    После апдейта должны измениться следующие смены, но не текущая.

    В тесте участвуют всего две смены, замокали период. на который создаются смены.
    """

    service = factories.ServiceFactory(owner=person)
    factories.ServiceMemberFactory(service=service, staff=service.owner, role=owner_role)
    role = factories.RoleFactory()
    for _ in range(3):
        factories.ServiceMemberFactory(service=service, role=role)

    schedule = factories.ScheduleFactory(
        service=service,
        start_date='2020-06-09',
        start_time='12:00',
        role=role,
        duration=timezone.timedelta(days=6),
        algorithm=Schedule.NO_ORDER,
        duty_on_weekends=True,
    )

    recalculate_duty_for_service(service.id)
    shifts = schedule.shifts.order_by('start_datetime')

    client.login(service.owner.login)
    response = client.json.patch(
        reverse(f'{api}:duty-schedule-detail', args=[schedule.pk]),
        data={
            'duty_on_weekends': False,
        }
    )
    assert response.status_code == 200

    shift = shifts[0]
    # Первая смена не изменилась
    assert (
        shift.start_datetime.astimezone(settings.DEFAULT_TIMEZONE) ==
        datetime.datetime(2020, 6, 9, 12, 0).astimezone(settings.DEFAULT_TIMEZONE)
    )
    assert shift.start == datetime.datetime(2020, 6, 9).astimezone(settings.DEFAULT_TIMEZONE).date()
    assert (
        shift.end_datetime.astimezone(settings.DEFAULT_TIMEZONE) ==
        datetime.datetime(2020, 6, 15, 12, 0).astimezone(settings.DEFAULT_TIMEZONE)
    )
    assert shift.end == datetime.datetime(2020, 6, 15).astimezone(settings.DEFAULT_TIMEZONE).date()

    # Запланированная смена учитывает выходные
    shift = shifts[1]
    assert (
        shift.start_datetime.astimezone(settings.DEFAULT_TIMEZONE) ==
        datetime.datetime(2020, 6, 15, 12, 0).astimezone(settings.DEFAULT_TIMEZONE)
    )
    assert shift.start == datetime.datetime(2020, 6, 15).astimezone(settings.DEFAULT_TIMEZONE).date()

    assert (
        shift.end_datetime.astimezone(settings.DEFAULT_TIMEZONE) ==
        datetime.datetime(2020, 6, 23, 12, 0).astimezone(settings.DEFAULT_TIMEZONE)
    )
    assert shift.end == datetime.datetime(2020, 6, 23).astimezone(settings.DEFAULT_TIMEZONE).date()


@freezegun.freeze_time('2020-06-05T12:00:00')
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=30))
@pytest.mark.parametrize('api', ['api-v3', 'api-v4'])
def test_duty_on_weekends_manual_order(owner_role, client, transactional_db, person, api):
    """
    В этом тесте замокали период, на который создаются смены: вместо полгода - 30 дней.
    Поэтому всего смен 40.

    Есть график с ручным порядком, старт 1 июня, передача в 15-00, длительность 2 дня,
    галка дежурить по выходным duty_on_weekends выключена, одновременно дежурят 4.
    Поменяем кол-во дежурящих на 2, время дефолтное 00-00.
    Во второй раз поменяем кол-во дежурявщих обратно на 4.
    """

    service = factories.ServiceFactory(owner=person)

    schedule = factories.ScheduleFactory(
        service=service,
        start_date=timezone.datetime(2020, 6, 1).date(),
        duty_on_weekends=False,
        algorithm=Schedule.MANUAL_ORDER,
        duration=timezone.timedelta(days=2),
        start_time=datetime.time(15, 00),
        persons_count=4,
    )

    for order in range(6):
        staff = factories.ServiceMemberFactory(service=schedule.service, role=schedule.role).staff
        Order.objects.create(schedule=schedule, staff=staff, order=order)

    recalculate_all_duties()

    factories.ServiceMemberFactory(service=schedule.service, staff=schedule.service.owner, role=owner_role)
    client.login(service.owner.login)
    response = client.json.patch(
        reverse('api-v3:duty-schedule-detail', args=[schedule.pk]),
        data={
            'persons_count': 2,
            'start_time': '00:00',
        }
    )
    assert response.status_code == 200

    # график пересчитается и еще раз поменяем настройки
    response = client.json.patch(
        reverse(f'{api}:duty-schedule-detail', args=[schedule.pk]),
        data={
            'persons_count': 4,
        }
    )
    assert response.status_code == 200

    shifts = list(schedule.shifts.future().order_by('start_datetime', 'index'))
    assert len(shifts) == 40

    # проверяем индексы смен с 9 по 10 июня:
    index = 12
    for s in shifts[:4]:
        assert s.index == index
        index += 1

    # проверяем индексы смен с 14 по 16 июня:
    for s in shifts[4:8]:
        assert s.index == index
        index += 1


@freezegun.freeze_time('2020-06-05T12:00:00')
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=30))
@pytest.mark.parametrize('algorithm', [Schedule.MANUAL_ORDER, Schedule.NO_ORDER])
def test_start_time_and_crit_fields(owner_role, client, person, algorithm):
    """
    В этом тесте замокали период, на который создаются смены: вместо полгода - 30 дней.
    Поэтому всего смен 40.

    При смене времени + критическое поле, происходил пересчет будущих смен, но игнорировался апдейт текущие.
    Из-за чего были неправильные половинки.
    """

    service = factories.ServiceFactory(owner=person)

    schedule = factories.ScheduleFactory(
        service=service,
        start_date=timezone.datetime(2020, 6, 1).date(),
        duty_on_weekends=False,
        algorithm=algorithm,
        duration=timezone.timedelta(days=2),
        start_time=datetime.time(15, 00),
        persons_count=4,
    )

    for order in range(6):
        staff = factories.ServiceMemberFactory(service=schedule.service, role=schedule.role).staff
        if algorithm == Schedule.MANUAL_ORDER:
            Order.objects.create(schedule=schedule, staff=staff, order=order)

    recalculate_duty_for_service(schedule.service.id)

    factories.ServiceMemberFactory(service=schedule.service, staff=schedule.service.owner, role=owner_role)
    client.login(schedule.service.owner.login)
    response = client.json.patch(
        reverse('api-v3:duty-schedule-detail', args=[schedule.pk]),
        data={
            'persons_count': 2,
            'start_time': '00:00',
        }
    )
    assert response.status_code == 200

    current_shifts = list(schedule.shifts.current_shifts().order_by('start_datetime'))
    assert len(current_shifts) == 4

    # проверяем end_datetime текущих смен:
    for s in current_shifts:
        assert (
            s.end_datetime.astimezone(settings.DEFAULT_TIMEZONE) ==
            datetime.datetime(2020, 6, 9, 0, 0).astimezone(settings.DEFAULT_TIMEZONE)
        )


@freezegun.freeze_time('2020-07-04T12:00:00')
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=10))
@pytest.mark.parametrize('algorithm', [Schedule.MANUAL_ORDER, Schedule.NO_ORDER])
def test_double_shifts_in_weekend(owner_role, client, algorithm):
    """
    В этом тесте замокали период, на который создаются смены: вместо полгода - 10 дней.
    Поэтому расчитываем всего 2 шифта, после - уже 3.

    Происходят задвоения при пересчете в день, когда нет текущих смен.
    Например, это возможно, если выключена галка дежурить по выходным, а пересчет проиходит в выходной.
    """

    schedule = factories.ScheduleFactory(
        start_date=timezone.datetime(2020, 6, 29).date(),
        duty_on_weekends=False,
        algorithm=algorithm,
    )

    # рассчитаем график
    with freezegun.freeze_time('2020-07-01'):
        recalculate_duty_for_service(schedule.service.id)

    # теперь пересчитаем в выходной день
    assert schedule.shifts.count() == 2
    recalculate_duty_for_service(schedule.service.id)

    # теперь у нас должно быть 3 шифта (+ ещё одна новая в будущем)
    assert schedule.shifts.count() == 3


@freezegun.freeze_time('2020-11-01T12:00:00')
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=15))
def test_recalculate_no_crit_fields(owner_role, client, person):
    """
    В этом тесте замокали период, на который создаются смены: вместо полгода - 15 дней.
    В этом тесте проверяем лесенку дежурств после апдейта не критического поля.
    """

    service = factories.ServiceFactory(owner=person)
    factories.ServiceMemberFactory(service=service, staff=service.owner, role=owner_role)
    role = factories.RoleFactory()
    for _ in range(5):
        factories.ServiceMemberFactory(service=service, role=role)

    schedule = factories.ScheduleFactory(
        service=service,
        role=role,
        start_date=timezone.datetime(2020, 11, 1).date(),
        duty_on_weekends=True,
        duration=timezone.timedelta(days=2),
        persons_count=1,
        autoapprove_timedelta=timezone.timedelta(1),
        allow_sequential_shifts=False,
    )

    recalculate_duty_for_service(schedule.service.id)
    shifts = schedule.shifts.order_by('start_datetime')
    shift_1 = shifts[1]
    shift_3 = shifts[3]
    staff_1 = shift_1.staff
    # проверим лесенку смен
    assert shifts[0].staff != shift_1.staff != shifts[2].staff != shift_3.staff != shifts[4].staff

    client.login(schedule.service.owner.login)
    with freezegun.freeze_time('2020-11-04T00:00:00'):
        recalculate_duty_for_service(schedule.service.id)

        response = client.json.patch(
            reverse('api-v3:duty-schedule-detail', args=[schedule.pk]),
            data={
                'start_time': '15:00',
            }
        )
    assert response.status_code == 200

    shift_1.refresh_from_db()
    shift_3.refresh_from_db()

    # проверим лесенку смен:
    # текущей сменой при пересчете 4/11 станет смена 1, через одну, у смены 3, стафф не должен поменяться
    assert shift_1.staff == staff_1
    assert shift_3.staff != staff_1     # вот здесь, есть  такой момент:
    # что, если смена неподтверждена и ещё есть люди, которые не были на сменах,
    # то не может быть уверенности, что стафф останется тот же, но повторяться с недавним он не должен

    assert shifts[0].staff != shift_1.staff != shifts[2].staff != shift_3.staff != shifts[4].staff


@freezegun.freeze_time('2020-11-01T12:00:00')
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=15))
def test_recalculate_with_current_replacement(owner_role, client, person):
    """
    В этом тесте замокали период, на который создаются смены: вместо полгода - 15 дней.
    Последоватлеьность воспроизведения бага:
        * Расcчитали график, в прошлом должен быть 1 полный круг
        * У будующей смены (назовём её 1) добавили замену, на следующего после него (назовём следующую смену сменой-2)
        * У смены-2 не должен поменяться стафф. Смена-2 неподтверждена

    Прошлые смены: 1-2, 3-4, 5-6,
    Текущая: 7-8
    Смена-1: 9-10
    Смена-2: 11-12
    """

    service = factories.ServiceFactory(owner=person)
    factories.ServiceMemberFactory(service=service, staff=service.owner, role=owner_role)
    role = factories.RoleFactory()
    for _ in range(3):
        factories.ServiceMemberFactory(service=service, role=role)

    schedule = factories.ScheduleFactory(
        service=service,
        role=role,
        start_date=timezone.datetime(2020, 11, 1).date(),
        duty_on_weekends=True,
        duration=timezone.timedelta(days=2),
        persons_count=1,
        autoapprove_timedelta=timezone.timedelta(1),
        allow_sequential_shifts=False,
    )

    with freezegun.freeze_time('2020-11-08'):
        recalculate_duty_for_service(schedule.service.id)
        shifts = schedule.shifts.fulltime().order_by('start_datetime')
        ordered_staff = [s.staff for s in shifts[:3]]
        shift_1 = shifts[4]
        shift_2 = shifts[5]
        assert shift_1.staff == ordered_staff[1]
        assert shift_2.staff == ordered_staff[2]

        client.login(schedule.service.owner.login)
        response = client.json.patch(
            reverse('api-v4:duty-shift-detail', args=[shift_1.id]),
            data={
                "replaces": [
                    {
                        "replace_for": shift_1.id,
                        "person": ordered_staff[2].login,
                        "start_datetime": '2020-11-09T00:00:00Z',
                        "end_datetime": '2020-11-10T00:00:00Z',
                    }
                ],
            }
        )

        assert response.status_code == 200
        shift_1.refresh_from_db()
        assert len(shift_1.replaces.all()) == 1

        recalculate_duty_for_service(schedule.service.id)
        shift_2.refresh_from_db()
        assert shift_2.staff == ordered_staff[2]


@freezegun.freeze_time('2020-11-01')
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=15))
def test_weekend_gap_with_no_duty_on_weekends():
    role = factories.RoleFactory()
    service = factories.ServiceFactory()
    member = factories.ServiceMemberFactory(service=service, role=role)
    schedule = factories.ScheduleFactory(
        service=service,
        start_date=timezone.datetime(2020, 11, 2).date(),
        duty_on_weekends=False,
        duration=timezone.timedelta(days=7),
        algorithm=Schedule.MANUAL_ORDER,
        role=role,
    )
    factories.OrderFactory(schedule=schedule, staff=member.staff, order=0)

    factories.GapFactory(
        staff=member.staff,
        start=timezone.datetime(2020, 11, 7, 0, 0, tzinfo=timezone.utc),
        end=timezone.datetime(2020, 11, 9, 0, 0, tzinfo=timezone.utc),
        work_in_absence=False,
        type='type',
        full_day=True
    )

    recalculate_duty_for_service(service.id)

    assert Problem.objects.count() == 0
