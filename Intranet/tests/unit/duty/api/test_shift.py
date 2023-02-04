from datetime import date

from freezegun import freeze_time
from mock import call, patch
import pretend
import pytest
import pytz

from django.core.urlresolvers import reverse
from django.test.utils import override_settings
from django.utils import timezone
from django.conf import settings

from plan.common.utils.watcher import WatcherClient
from plan.duty.api.views import V4ShiftView
from plan.duty.models import Shift, Schedule, Role, DutyToWatcher
from plan.duty.schedulers import DutyScheduler
from plan.duty.tasks import check_current_shifts, recalculate_duty_for_service
from plan.staff.constants import LANG
from common import factories
from common.intranet import request_member_side_effect
from utils import _fake_convert_to_html


pytestmark = pytest.mark.django_db


def create_shift(service, **kwargs):
    schedule = factories.ScheduleFactory(service=service,  start_date=timezone.datetime(2019, 1, 1))
    shift = factories.ShiftFactory(schedule=schedule, **kwargs)
    return shift


@pytest.fixture
def shift_data(owner_role, staff_factory):
    staff = staff_factory('full_access')
    vteam_tag = factories.ServiceTagFactory(name='v-team')
    owner = staff_factory('full_access')
    service = factories.ServiceFactory(owner=owner)
    factories.ServiceMemberFactory(staff=owner, role=owner_role, service=service)
    service.tags.add(vteam_tag)

    return pretend.stub(staff=staff, vteam_tag=vteam_tag, service=service)


@pytest.fixture
def shift_replace_data(shift_data, owner_role, staff_factory):
    owner = staff_factory()
    staff = staff_factory()
    factories.ServiceMemberFactory(staff=owner, role=owner_role, service=shift_data.service)
    role = factories.RoleFactory(code='cat')
    factories.ServiceMemberFactory(staff=staff, role=role, service=shift_data.service)

    schedule = factories.ScheduleFactory(service=shift_data.service, start_date=timezone.datetime(2018, 2, 1))

    shift_1 = factories.ShiftFactory(schedule=schedule,
                                     start=timezone.datetime(2019, 1, 30),
                                     end=timezone.datetime(2019, 2, 3))
    shift_2 = factories.ShiftFactory(schedule=schedule,
                                     start=timezone.datetime(2019, 2, 1),
                                     end=timezone.datetime(2019, 2, 2))
    shift_3 = factories.ShiftFactory(schedule=schedule)

    factories.ServiceMemberFactory(staff=shift_1.staff, role=role, service=shift_1.service)
    schedule.role = role
    schedule.save()

    return pretend.stub(
        owner=owner,
        staff=staff,
        schedule=schedule,
        shift_1=shift_1,
        shift_2=shift_2,
        shift_3=shift_3,
        role=role,
    )


def get_shifts(client, fields=None, on_duty=False, **kwargs):
    params = {'date_from': '2017-01-01', 'date_to': '2222-01-01'}
    if fields is not None:
        fields.extend(V4ShiftView.default_fields)
        params['fields'] = ','.join(fields)

    params.update(kwargs)
    response = client.json.get(
        reverse('api-v4:duty-on-duty-list' if on_duty else 'api-v4:duty-shift-list'), params
    )
    assert response.status_code == 200
    data = response.json()
    if not on_duty:
        data = data['results']
    return data


def test_get_shift_with_null_staff(client, shift_data):
    tag = factories.ServiceTagFactory()
    shift_data.service.tags.add(tag)
    schedule = factories.ScheduleFactory(service=shift_data.service)
    factories.ShiftFactory(staff=None, schedule=schedule)
    factories.ShiftFactory(staff=shift_data.staff)

    shifts = get_shifts(client)
    assert len(shifts) == 2


def test_problems_count_0(client):
    factories.ShiftFactory(has_problems=False)
    shifts = get_shifts(client, fields=['problems_count', ])
    assert len(shifts) == 1
    assert shifts[0]['problems_count'] == 0


def test_num_queries(client, django_assert_num_queries):
    """
    Проверка количества запросов при fields, которые
    использует стафф для получения смен
    """
    shift = factories.ShiftFactory()
    for _ in range(12):
        factories.ShiftFactory(replace_for_id=shift.id)
    with django_assert_num_queries(9):
        # staff from request
        # content_type
        # shifts
        # schedule for replaces
        # staff for replaces
        # replaces
        # duty order
        # pg_is_in_recovery
        # waffle
        response = client.json.get(
            reverse('api-v4:duty-shift-list'),
            {'fields': 'id,person.id,person.login,schedule.id,schedule.name,'
                       'is_approved,start,end,replaces.id,replaces.person.id,'
                       'replaces.person.login,replaces.schedule.id,replaces.schedule.name,'
                       'replaces.start,replaces.end,replaces.is_approved',
             'date_from': '2017-01-01', 'date_to': '2222-01-01',
             }
        )
    assert response.status_code == 200
    data = response.json()['results']
    assert len(data) == 1
    assert len(data[0]['replaces']) == 12


def test_problems_count_1(client):
    factories.ShiftFactory(has_problems=True)
    shifts = get_shifts(client, fields=['problems_count', ])
    assert len(shifts) == 1
    assert shifts[0]['problems_count'] == 1


def test_vteams(client, shift_data, django_assert_num_queries):
    tag = factories.ServiceTagFactory()
    shift_data.service.tags.add(tag)
    factories.ServiceMemberFactory(service=shift_data.service, staff=shift_data.staff)
    another_service = factories.ServiceFactory()
    another_service.tags.add(tag)
    factories.ServiceMemberFactory(service=another_service, staff=shift_data.staff)
    for _ in range(10):
        factories.ShiftFactory(staff=shift_data.staff)

    with django_assert_num_queries(8):
        # 1 User + Staff
        # 1 middleware
        # 1 select shifts + staff + schedule + role
        # 1 shifts + 1 orders
        # 1 Vteams
        # 1 pg_is_in_recovery()
        # 1 Waffle readonly switch
        shifts = get_shifts(client, fields=['person.vteams', ], )

    assert len(shifts) == 10
    assert all([shift['person']['vteams'][0]['id'] == shift_data.service.pk for shift in shifts])


def test_filter_by_person(client, shift_data):
    another_staff = factories.StaffFactory()
    factories.ShiftFactory(staff=shift_data.staff)
    factories.ShiftFactory(staff=another_staff)
    shifts = get_shifts(client, person=shift_data.staff.login)
    assert len(shifts) == 1
    assert shifts[0]['person']['login'] == shift_data.staff.login


def test_filter_by_service(client, shift_data):
    service_1 = factories.ServiceFactory()
    service_2 = factories.ServiceFactory()
    shift_1 = create_shift(service_1)
    create_shift(service_2)

    shifts = get_shifts(client, service=service_1.id)
    assert len(shifts) == 1
    assert shifts[0]['id'] == shift_1.id


def test_filter_by_service_slug(client, shift_data):
    service_1 = factories.ServiceFactory()
    service_2 = factories.ServiceFactory()
    shift_1 = create_shift(service_1)
    create_shift(service_2)

    shifts = get_shifts(client, service__slug=service_1.slug)
    assert len(shifts) == 1
    assert shifts[0]['id'] == shift_1.id


@pytest.mark.parametrize(('date_from', 'date_to', 'count'), [
    ('2017-01-01', '2222-01-01', 3),    # все шифты
    ('2017-01-01', '2021-05-30', 2),    # shift_1 b shift_3
    ('2021-06-25', '2021-07-01', 1),    # только shift_2
])
def test_filter_date(client, date_from, date_to, count):
    shift_1 = factories.ShiftFactory(start='2021-04-02', end='2021-05-05',)
    shift_2 = factories.ShiftFactory(start='2021-06-01', end='2021-07-01',)
    shift_3 = factories.ShiftFactory(start='2021-05-24', end='2021-06-07',)
    assert len(set([shift_1.schedule, shift_2.schedule, shift_3.schedule])) == 3

    params = {'date_from': date_from, 'date_to': date_to, }
    shifts = get_shifts(client, **params)
    assert len(shifts) == count


def test_filter_by_schedule(client):
    shift_1 = factories.ShiftFactory()
    shift_2 = factories.ShiftFactory()
    assert shift_2.schedule != shift_1.schedule
    shifts = get_shifts(client, schedule=shift_1.schedule.id)
    assert len(shifts) == 1
    assert shifts[0]['id'] == shift_1.id


@pytest.mark.parametrize('field', ['id', 'slug'])
def test_filter_by_two_schedule(client, field):
    shift_1 = factories.ShiftFactory()
    shift_2 = factories.ShiftFactory()
    shift_3 = factories.ShiftFactory()
    assert len(set([shift_1.schedule, shift_2.schedule, shift_3.schedule])) == 3

    params = {'schedule__in': f'{shift_1.schedule.id},{shift_2.schedule.id}'}
    if field == 'slug':
        params = {'schedule__slug__in': f'{shift_1.schedule.slug},{shift_2.schedule.slug}'}

    shifts = get_shifts(client, **params)
    assert len(shifts) == 2
    assert set([s['id'] for s in shifts]) == set([shift_1.id, shift_2.id])


def test_filter_by_two_person(client):
    shift_1 = factories.ShiftFactory()
    shift_2 = factories.ShiftFactory()
    shift_3 = factories.ShiftFactory()
    assert len(set([shift_1.staff, shift_2.staff, shift_3.staff])) == 3

    params = {'person__in': f'{shift_1.staff.login}, {shift_3.staff.login}'}
    shifts = get_shifts(client, **params)
    assert len(shifts) == 2
    assert set([s['id'] for s in shifts]) == set([shift_1.id, shift_3.id])


@freeze_time('2019-01-01')
def test_filter_by_schedule_slug(client):
    service_1 = factories.ServiceFactory()
    service_2 = factories.ServiceFactory()
    schedule_1 = factories.ScheduleFactory(service=service_1, start_date=timezone.datetime(2019, 1, 1), slug='1')
    schedule_2 = factories.ScheduleFactory(service=service_2, start_date=timezone.datetime(2019, 1, 1), slug='2')
    shift_1 = factories.ShiftFactory(schedule=schedule_1)
    shift_2 = factories.ShiftFactory(schedule=schedule_2)

    assert shift_2.schedule != shift_1.schedule
    shifts = get_shifts(client, schedule__slug=shift_1.schedule.slug)
    assert len(shifts) == 1
    assert shifts[0]['id'] == shift_1.id


@freeze_time('2019-01-30T00:00:00')
def test_approve_shift(client, shift_data, django_assert_num_queries):
    shift = create_shift(shift_data.service)
    assert shift.is_approved is False
    client.login(shift_data.service.owner.login)

    with django_assert_num_queries(15):
        # 1 User + Staff
        # 1 middleware
        # 1 select shifts + staff + schedule + role + service
        # 1 servicemember
        # 1 service
        # 2 update shift
        # 2 create history
        # 1 waffle
        # 1 service + 1 order + 1 shift
        # 1 pg_is_in_recovery
        # 1 waffle switch read only
        response = client.json.patch(reverse('api-v3:duty-shift-detail', args=[shift.id]), data={'is_approved': True})
    assert response.status_code == 200
    shift.refresh_from_db()
    assert shift.is_approved is True
    assert shift.approved_by == shift_data.service.owner
    assert shift.approve_datetime == timezone.now()


@patch('ids.services.formatter.api.convert_to_html', _fake_convert_to_html)
def test_change_person_approved_shift_no_problem(client, shift_data, owner_role, duty_role, staff_factory):
    """
    Делаем полную замену дежурного.
    Подтвержденное дежурство.
    Проблему устранили.
    """

    staff = factories.StaffFactory()
    staff_2 = factories.StaffFactory()
    shift = create_shift(shift_data.service, state=Shift.STARTED, staff=staff_2, has_problems=True, is_approved=True)
    member = factories.ServiceMemberFactory(service=shift.service, staff=shift.staff, role=duty_role)
    owner = staff_factory('full_access')
    factories.ServiceMemberFactory(staff=owner, role=owner_role, service=shift_data.service)
    client.login(owner.login)

    role = factories.RoleFactory(code='cat')
    factories.ServiceMemberFactory(staff=staff, role=role, service=shift_data.service)
    factories.ServiceMemberFactory(staff=staff_2, role=role, service=shift_data.service)
    shift.schedule.role = role
    shift.schedule.save()

    factories.ServiceMemberFactory(staff=owner, role=owner_role, service=shift_data.service)

    shifts = get_shifts(client, fields=['problems_count', ], person=shift.staff.login)
    assert shifts[0]['problems_count'] == 1

    with patch('plan.api.idm.actions.request_membership') as request_membership, \
            patch('plan.api.idm.actions.deprive_role') as deprive_role:
        request_membership.side_effect = request_member_side_effect
        response = client.json.patch(reverse('api-v3:duty-shift-detail', args=[shift.id]), {'person': staff.login})
        assert response.status_code == 200

    shift.refresh_from_db()
    assert not shift.has_problems
    assert shift.staff == staff
    expected_args_list = [call(shift.service, shift.staff, duty_role, comment='Начало дежурства', silent=True)]
    assert request_membership.call_args_list == expected_args_list
    assert deprive_role.call_args_list == [call(member, comment='Отмена дежурства')]

    # для корректной отрисовки фронта прям сейчас важно, что приходит в ответ
    result = response.json()
    assert result['problems_count'] == 0
    assert result['is_approved'] is True
    assert result['person']['login'] == staff.login

    # проверим данные в api
    shifts = get_shifts(client, fields=['problems_count', ], schedule=shift.schedule.id)

    # если есть пересчет календаря => сформируются новые shift'ы
    assert len(shifts) > 1

    new_shift = None
    for s in shifts:
        if s['id'] == shift.id:
            new_shift = s
            break

    assert new_shift['problems_count'] == 0
    assert new_shift['is_approved'] is True
    assert new_shift['person']['login'] == staff.login


@pytest.fixture
@freeze_time('2019-01-01')
def recalculatable_duty_data(owner_role, duty_role, responsible_role, staff_factory):
    owner = staff_factory('full_access')
    service = factories.ServiceFactory(owner=owner)
    factories.ServiceMemberFactory(service=service, role=owner_role, staff=owner)
    member = factories.ServiceMemberFactory(service=service, role=responsible_role)
    staff = member.staff
    schedule = factories.ScheduleFactory(
        role=responsible_role,
        service=service,
        start_date=timezone.datetime(2018, 12, 31).date(),
    )

    recalculate_duty_for_service(service.id)
    return pretend.stub(owner=owner, schedule=schedule, staff=staff, service=service)


@freeze_time('2019-01-01')
def test_change_person_no_approved_shift_no_problem(client, recalculatable_duty_data, duty_role):
    shift = recalculatable_duty_data.schedule.shifts.order_by('start').first()
    staff = recalculatable_duty_data.staff
    shift.state = Shift.STARTED
    shift.staff = None
    shift.has_problems = True
    shift.save()

    client.login(recalculatable_duty_data.owner.login)

    with patch('plan.api.idm.actions.request_membership') as request_membership:
        request_membership.side_effect = request_member_side_effect
        response = client.json.patch(reverse('api-v3:duty-shift-detail', args=[shift.id]), {'person': staff.login})
        assert response.status_code == 200

    shift.refresh_from_db()
    assert not shift.has_problems

    # для корректной отрисовки фронта прям сейчас важно, что приходит в ответ
    result = response.json()
    assert result['problems_count'] == 0
    assert result['is_approved'] is True
    assert result['person']['login'] == staff.login

    resp_shift = [
        resp_shift
        for resp_shift in get_shifts(client, fields=['problems_count', ], schedule=shift.schedule.id)
        if shift.id == resp_shift['id']
    ][0]

    assert resp_shift['problems_count'] == 0
    assert resp_shift['is_approved'] is True
    assert resp_shift['person']['login'] == staff.login

    assert shift.staff == staff


@freeze_time('2019-01-01')
def test_change_person_consider_other_schedules(client, shift_data, owner_role, duty_role, staff_factory):
    """
    Делаем полную замену дежурного в независимом графике.
    Неподтвержденное дежурство.
    Проблему устранили.
    """

    staff = factories.StaffFactory()
    role = factories.RoleFactory(code='cats')
    schedule = factories.ScheduleFactory(
        consider_other_schedules=False,
        role=role,
        service=shift_data.service,
        start_date='2019-01-01'
    )
    recalculate_duty_for_service(shift_data.service.id)

    shift = schedule.shifts.current_shifts().first()
    assert shift.staff is None
    assert shift.has_problems is True
    shift.state = Shift.STARTED
    shift.save()

    factories.ServiceMemberFactory(staff=staff, role=role, service=shift_data.service)

    owner = staff_factory('full_access')
    factories.ServiceMemberFactory(staff=owner, role=owner_role, service=shift_data.service)
    client.login(owner.login)

    with patch('plan.api.idm.actions.request_membership') as request_membership:
        request_membership.side_effect = request_member_side_effect
        response = client.json.patch(reverse('api-v3:duty-shift-detail', args=[shift.id]), {'person': staff.login})
        assert response.status_code == 200

    shift.refresh_from_db()
    assert not shift.has_problems


@freeze_time('2019-01-20')
@pytest.fixture
def shift_problem(shift_data, owner_role, staff_factory):
    staff = staff_factory('full_access')
    shift = create_shift(
        shift_data.service,
        state=Shift.SCHEDULED,
        staff=None,
        has_problems=True,
        is_approved=True,
        start='2019-01-20',
        end='2019-02-01',
    )
    owner = staff_factory('full_access')
    factories.ServiceMemberFactory(staff=owner, role=owner_role, service=shift_data.service)

    role = factories.RoleFactory(code='cats')
    factories.ServiceMemberFactory(staff=staff, role=role, service=shift_data.service)
    shift.schedule.role = role
    shift.schedule.save()

    factories.ServiceMemberFactory(staff=owner, role=owner_role, service=shift_data.service)

    factories.GapFactory(
        staff=staff,
        start='2019-01-20T00:00:00Z',
        end='2019-02-01T00:00:00Z',
        work_in_absence=False,
        type='type',
        full_day=False
    )

    return pretend.stub(staff=staff, shift=shift, owner=owner)


@override_settings(DEFAULT_TIMEZONE=pytz.timezone('Europe/Moscow'))
@freeze_time('2019-01-01')
def test_change_person_approved_shift_problem(client, shift_problem):
    """
    Делаем полную замену дежурного.
    Подтвержденное дежурство.
    Проблему не устранили.
    """

    shift = shift_problem.shift
    staff = shift_problem.staff
    schedule = shift.schedule
    schedule.start_date = timezone.datetime(2019, 1, 20).date()
    schedule.duration = timezone.timedelta(days=14)
    schedule.save()

    client.login(shift_problem.owner.login)

    response = client.json.patch(reverse('api-v3:duty-shift-detail', args=[shift.id]), {'person': staff.login})
    assert response.status_code == 200

    # для корректной отрисовки фронта прям сейчас важно, что приходит в ответ
    result = response.json()
    assert result['problems_count'] == 1
    assert result['is_approved'] is True
    assert result['person']['login'] == staff.login

    # проверяем, что за данные отдаем в api
    shifts = get_shifts(client, fields=['problems_count', ], schedule=shift.schedule.id)

    # если есть пересчет календаря => сформируются новые shift'ы
    assert len(shifts) > 1

    new_shift = None
    for s in shifts:
        if s['id'] == shift.id:
            new_shift = s
            break

    assert new_shift['problems_count'] == 1
    assert new_shift['is_approved'] is True
    assert new_shift['person']['login'] == staff.login


@freeze_time('2019-01-01')
def test_change_person_no_approved_shift_problem(client, recalculatable_duty_data, responsible_role):
    """
    Делаем полную замену дежурного.
    Неподтвержденное дежурство.
    Проблему не устранили.
    """

    shift = recalculatable_duty_data.schedule.shifts.order_by('start').first()

    member = factories.ServiceMemberFactory(service=recalculatable_duty_data.service, role=responsible_role)
    staff = member.staff

    factories.GapFactory(
        staff=staff,
        start=shift.start,
        end=shift.end,
        work_in_absence=False,
        type='type',
        full_day=False
    )

    shift.is_approved = False
    shift.save()

    client.login(recalculatable_duty_data.owner.login)

    response = client.json.patch(reverse('api-v3:duty-shift-detail', args=[shift.id]), {'person': staff.login})
    assert response.status_code == 200

    # для корректной отрисовки фронта прям сейчас важно, что приходит в ответ
    result = response.json()
    assert result['problems_count'] == 1
    assert result['is_approved'] is True
    assert result['person']['login'] == staff.login

    # проверяем, что за данные отдаем в api
    shifts = get_shifts(client, fields=['problems_count', ], schedule=shift.schedule.id)

    # проверим, что нет пересчета => shift остаётся один
    # assert len(shifts) == 1
    resp_shift = [
        resp_shift
        for resp_shift in shifts
        if resp_shift['id'] == shift.id
    ][0]

    assert resp_shift['problems_count'] == 1
    assert resp_shift['is_approved'] is True
    assert resp_shift['person']['login'] == staff.login


def test_change_schedule(client, shift_data, owner_role, staff_factory):
    shift = create_shift(shift_data.service)
    owner = staff_factory()
    factories.ServiceMemberFactory(staff=owner, role=owner_role, service=shift_data.service)
    client.login(owner.login)

    old_schedule = shift.schedule
    another_schdeule = factories.ScheduleFactory()
    response = client.json.patch(
        reverse('api-v3:duty-shift-detail', args=[shift.id]),
        data={'schedule': another_schdeule.id}
    )

    assert response.status_code == 200
    shift.refresh_from_db()
    assert shift.schedule == old_schedule


def test_change_start(client, shift_data, owner_role, staff_factory):
    shift = create_shift(shift_data.service)
    owner = staff_factory()
    factories.ServiceMemberFactory(staff=owner, role=owner_role, service=shift_data.service)
    client.login(owner.login)

    old_start = shift.start
    response = client.json.patch(
        reverse('api-v3:duty-shift-detail', args=[shift.id]),
        data={'start': '2019-10-10'}
    )
    assert response.status_code == 200
    shift.refresh_from_db()
    assert shift.start == old_start


def test_change_replace_for(client, shift_data, owner_role, staff_factory):
    another_shift = factories.ShiftFactory()
    shift = create_shift(shift_data.service)
    owner = staff_factory()
    factories.ServiceMemberFactory(staff=owner, role=owner_role, service=shift_data.service)
    client.login(owner.login)

    old_replace_for = shift.replace_for
    response = client.json.patch(
        reverse('api-v3:duty-shift-detail', args=[shift.id]),
        data={'replace_for': another_shift.id}
    )
    assert response.status_code == 200
    shift.refresh_from_db()
    assert shift.replace_for == old_replace_for


def test_approve_shift_403(client, shift_data):
    shift = create_shift(shift_data.service)
    client.login(shift_data.staff.login)
    response = client.json.patch(reverse('api-v3:duty-shift-detail', args=[shift.id]), data={'is_approved': True})
    assert response.status_code == 403


@freeze_time('2019-01-30T00:00:00')
@patch('ids.services.formatter.api.convert_to_html', _fake_convert_to_html)
def test_replaces_create(client, shift_replace_data, duty_role):
    """
    Создаём частичную замену в день начала дежурного.
    Должна выдаваться роль, письма о временных заменах не приходят.
    Но, так как проблемы в заменах это проблема в основном шифте, то про него отправляется письмо
    """

    client.login(shift_replace_data.owner.login)
    shift = shift_replace_data.shift_1
    staff = shift_replace_data.staff

    shift.replaces.add(shift_replace_data.shift_2)
    shift.replaces.add(shift_replace_data.shift_3)
    shift.is_approved = True
    shift.save()

    role = shift_replace_data.role
    factories.ServiceMemberFactory(staff=staff, role=role, service=shift.service)

    with patch('plan.api.idm.actions.request_membership') as request_membership:
        request_membership.side_effect = request_member_side_effect
        response = client.json.post(
            reverse('api-v3:duty-shift-list'),
            {
                'replace_for': shift.id,
                'person': staff.login,
                'start_datetime': '2019-01-30T00:00:00',
                'end_datetime': '2019-01-31T00:00:00'
            }
        )

    expected_args_list = [call(shift.service, staff, duty_role, comment='Начало дежурства', silent=True)]
    assert request_membership.call_args_list == expected_args_list
    assert response.status_code == 201

    shift.refresh_from_db()
    assert shift.replaces.count() == 3


@override_settings(DEFAULT_TIMEZONE=timezone.pytz.timezone('Europe/Moscow'))
@freeze_time('2019-01-30T00:00:00')
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=15))
def test_replaces_create_not_create_role(client, shift_replace_data, shift_data, owner_role, duty_role):
    """
    Создаём частичную замену в день начала дежурства.
    Заменяющий уже дежурит, у него есть роль.
    Роль не должны выдавать, но замену создадим.
    Замена должна быть заапрувлена.

    В этом тесте замокали период, на который создаются смены:
    вместо полгода - 15 дней (достаточно 3-х смен по 5 дней).
    """

    owner = factories.StaffFactory()
    client.login(shift_replace_data.owner.login)

    role = factories.RoleFactory(code='cat')
    with patch('plan.duty.tasks.DutyScheduler.recalculate_shifts'):
        for _ in range(2):
            factories.ServiceMemberFactory(role=role, service=shift_data.service)

        factories.ServiceMemberFactory(staff=owner, role=owner_role, service=shift_data.service)
        factories.ServiceMemberFactory(
            staff=owner,
            service=shift_replace_data.schedule.service,
            role=duty_role
        )

    factories.ScheduleFactory(
        service=shift_data.service,
        start_date=timezone.datetime(2019, 1, 2),
        role=role,
    )
    recalculate_duty_for_service(shift_replace_data.schedule.service.id)

    shift = Shift.objects.get(
        schedule=shift_replace_data.schedule,
        start=timezone.datetime(2019, 1, 30),
        end=timezone.datetime(2019, 2, 3)
    )
    shift.is_approved = True
    shift.has_problems = True
    shift.save()

    factories.GapFactory(
        staff=shift.staff,
        start=timezone.datetime(2019, 1, 30, 0, 0),
        end=timezone.datetime(2019, 2, 1, 0, 0),
        work_in_absence=False,
        type='type',
        full_day=True
    )

    assert shift.replaces.count() == 0

    shifts = get_shifts(client, fields=['problems_count', ], person=shift.staff.login)
    assert shifts[0]['problems_count'] == 1

    with patch('plan.api.idm.actions.request_membership') as request_membership:
        response = client.json.post(
            reverse('api-v3:duty-shift-list'),
            {
                'replace_for': shift.id,
                'person': owner.login,
                'start_datetime': '2019-01-30T00:00:00',
                'end_datetime': '2019-02-01T00:00:00'
            }
        )

    assert request_membership.call_args_list == []
    assert response.status_code == 201

    shift.refresh_from_db()
    assert shift.replaces.count() == 1
    assert shift.replaces.first().is_approved

    shifts = get_shifts(client, fields=['problems_count', ], person=shift.staff.login)
    assert shifts[0]['problems_count'] == 0


def test_replaces_create_no_start(client, shift_replace_data):
    client.login(shift_replace_data.owner.login)

    shift_replace_data.shift_1.replaces.add(shift_replace_data.shift_2)
    shift_replace_data.shift_1.replaces.add(shift_replace_data.shift_3)

    response = client.json.post(
        reverse('api-v3:duty-shift-list'),
        {
            'replace_for': shift_replace_data.shift_1.id,
            'person': shift_replace_data.staff.login,
        }
    )

    assert response.status_code == 400
    assert response.json()['error']['extra']['start_datetime'] == ['This field is required.']
    assert response.json()['error']['extra']['end_datetime'] == ['This field is required.']


def test_replaces_create_no_replace_for(client, shift_replace_data):
    client.login(shift_replace_data.owner.login)

    shift_replace_data.shift_1.replaces.add(shift_replace_data.shift_2)
    shift_replace_data.shift_1.replaces.add(shift_replace_data.shift_3)

    response = client.json.post(
        reverse('api-v3:duty-shift-list'),
        data={
            'person': shift_replace_data.staff.login,
            'start': '2019-01-30',
            'end': '2019-01-31'
        },
    )

    assert response.status_code == 400
    assert response.json()['error']['extra']['replace_for'] == ['This field is required.']


def test_replaces_create_empty_replace_for(client, shift_replace_data):
    client.login(shift_replace_data.owner.login)

    shift_replace_data.shift_1.replaces.add(shift_replace_data.shift_2)
    shift_replace_data.shift_1.replaces.add(shift_replace_data.shift_3)

    response = client.json.post(
        reverse('api-v3:duty-shift-list'),
        {
            'replace_for': '',
            'person': shift_replace_data.staff.login,
            'start': '2019-01-30',
            'end': '2019-01-31'
        },
        HTTP_ACCEPT_LANGUAGE=LANG.RU,
    )

    assert response.status_code == 400
    assert response.json()['error']['extra']['replace_for'] == ['Это поле не может быть пустым.']


@pytest.mark.parametrize('replace_end', ['2019-02-02T00:00:00', '2019-02-04T00:00:00'])
def test_replaces_create_with_intersection_replace_for(client, shift_replace_data, duty_role, replace_end):
    """
    Проверим запрос на создание замен с пересекающимися датами:
     * с частичным перекрытием при end == '2019-02-02'
     * новая частичная замена полностью покрывает предыдущую при end == '2019-02-04'
    """

    client.login(shift_replace_data.owner.login)
    shift = shift_replace_data.shift_1
    staff = shift_replace_data.staff

    shift.replaces.add(shift_replace_data.shift_2)

    response = client.json.post(
        reverse('api-v3:duty-shift-list'),
        {
            'replace_for': shift.id,
            'person': staff.login,
            'start_datetime': '2019-01-31T00:00:00',
            'end_datetime': replace_end
        }
    )

    assert response.status_code == 400
    assert response.json()['error']['extra'] == [
        'Даты пересекаются с частичной заменой {login} c 2019-02-01 по 2019-02-02.'
        .format(login=shift_replace_data.shift_2.staff.login)
    ]


@freeze_time('2018-01-01')
@pytest.mark.parametrize('autorequested', (True, False))
def test_replaces_delete(client, shift_replace_data, duty_role, autorequested):
    client.login(shift_replace_data.owner.login)

    shift_replace_data.shift_1.replaces.add(shift_replace_data.shift_2)
    shift_replace_data.shift_1.replaces.add(shift_replace_data.shift_3)

    shift_2 = shift_replace_data.shift_2
    shift_2.state = Shift.STARTED
    shift_2.role = duty_role
    shift_2.save()
    member = factories.ServiceMemberFactory(
        staff=shift_2.staff, service=shift_2.service,
        role=duty_role, autorequested=autorequested,
    )

    with patch('plan.api.idm.actions.deprive_role') as deprive_role:
        response = client.json.delete(reverse('api-v3:duty-shift-detail', args=[shift_2.id]))
        if autorequested:
            assert deprive_role.call_args_list == [call(member, comment='Отмена дежурства')]
        else:
            deprive_role.assert_not_called()
    assert response.status_code == 204
    shift_replace_data.shift_1.refresh_from_db()
    assert shift_replace_data.shift_1.replaces.count() == 1


@freeze_time('2018-01-01')
@pytest.mark.parametrize('recalculate,status_code', [(True, 404), (False, 204)])
def test_delete_shift_with_recalculate(client, shift_replace_data, duty_role, recalculate, status_code):
    client.login(shift_replace_data.owner.login)
    shift_replace_data.schedule.recalculate = recalculate
    shift_replace_data.schedule.save()
    assert shift_replace_data.schedule.shifts.count() == 3
    response = client.json.delete(reverse('api-v3:duty-shift-detail', args=[shift_replace_data.shift_2.id]))

    assert response.status_code == status_code
    assert shift_replace_data.schedule.shifts.count() == 3 if recalculate else 2


def test_replaces_delete_based_shift(client, shift_replace_data):
    client.login(shift_replace_data.owner.login)

    shift_replace_data.shift_1.replaces.add(shift_replace_data.shift_2)
    shift_replace_data.shift_1.replaces.add(shift_replace_data.shift_3)

    response = client.json.delete(reverse('api-v3:duty-shift-detail', args=[shift_replace_data.shift_1.id]))

    assert response.status_code == 404


def test_replaces_create_date(client, shift_replace_data):
    client.login(shift_replace_data.owner.login)
    shift_replace_data.shift_1.replaces.add(shift_replace_data.shift_2)

    response = client.json.post(
        reverse('api-v3:duty-shift-list'),
        {
            'replace_for': shift_replace_data.shift_1.id,
            'person': shift_replace_data.staff.login,
            'start_datetime': '2019-02-01T00:00:00',
            'end_datetime': '2019-02-05T00:00:00'
        }
    )

    assert response.status_code == 400
    assert response.json()['error']['extra'] == ['Задан некорректный интервал для замены.']


@pytest.mark.parametrize('role_code', [Role.RESPONSIBLE_FOR_DUTY, Role.EXCLUSIVE_OWNER])
def test_permissions_by_responsible(client, staff_factory, role_code):
    staff = staff_factory()
    service = factories.ServiceFactory()
    shift = create_shift(service)
    role = factories.RoleFactory(code=role_code)
    factories.ServiceMemberFactory(service=service, staff=staff, role=role)
    client.login(staff.login)
    response = client.json.options(reverse('api-v3:duty-shift-detail',  args=[shift.id]))
    assert response.status_code == 200
    assert response.json() == {
        'permissions': ['can_modify_shift']
    }


def test_permissions_by_stranger(client):
    service = factories.ServiceFactory()
    shift = create_shift(service)
    response = client.json.options(reverse('api-v3:duty-shift-detail',  args=[shift.id]))
    assert response.status_code == 200
    assert response.json() == {
        'permissions': []
    }


def test_no_required_filters(client):
    response = client.json.get(reverse('api-v3:duty-shift-list'))
    assert response.status_code == 400
    assert response.json()['error']['extra'] == {
        'date_from': ['This field is required.'],
        'date_to': ['This field is required.']
    }


def test_filter_by_has_problems(client):
    with_problems = factories.ShiftFactory(has_problems=True)
    without_problems = factories.ShiftFactory()

    assert {x['id'] for x in get_shifts(client)} == {with_problems.id, without_problems.id}
    assert {x['id'] for x in get_shifts(client, has_problems=True)} == {with_problems.id}
    assert {x['id'] for x in get_shifts(client, has_problems=False)} == {without_problems.id}


@override_settings(DEFAULT_TIMEZONE=pytz.timezone('Europe/Moscow'))
@freeze_time('2019-01-30T00:00:00')
def test_update_replaces(client, owner_role, person):
    """
    Есть основная смена и к ней замена. Редактируем через ручку patch замены.
    Создаем 2 новых замены. Старая должна быть удалена.
    """

    service = factories.ServiceFactory(owner=person)
    schedule = factories.ScheduleFactory(
        service=service,
        start_date=timezone.datetime(2019, 4, 3),
        duration=timezone.timedelta(days=7),
    )

    # основной шифт
    shift = factories.ShiftFactory(start='2019-04-24', end='2019-05-01', schedule=schedule)

    # замена, которая должна быть удалена
    factories.ShiftFactory(
        schedule=schedule,
        start='2019-04-26',
        end='2019-04-28',
        replace_for=shift,
    )

    old_saved_replace_start = '2019-04-28'
    new_saved_replace_start = '2019-04-29'
    saved_replace_end = '2019-04-30'
    saved_replace = factories.ShiftFactory(
        schedule=shift.schedule,
        start=old_saved_replace_start,
        end=saved_replace_end,
        replace_for=shift
    )
    staff_for_replace = factories.StaffFactory()
    factories.ServiceMemberFactory(staff=service.owner, role=owner_role, service=service)
    client.login(service.owner.login)

    new_replace_start = '2019-04-26'
    new_replace_end = '2019-04-27'

    response = client.json.patch(
        reverse('api-v3:duty-shift-detail', args=[shift.id]),
        data={
            'replaces': [
                {
                    'start': new_replace_start,
                    'end': new_replace_end,
                    'person': staff_for_replace.login
                },
                {
                    'start': new_saved_replace_start,
                    'person': saved_replace.staff.login,
                    'id': saved_replace.id
                },
            ]
        }
    )
    assert response.status_code == 200
    replaces = {shift['person']['uid']: shift for shift in response.json()['replaces']}
    assert len(replaces) == 2
    assert replaces[saved_replace.staff.uid]['id'] == saved_replace.id
    assert replaces[saved_replace.staff.uid]['start'] == new_saved_replace_start
    assert replaces[saved_replace.staff.uid]['end'] == saved_replace_end
    new_replace = replaces[staff_for_replace.uid]
    assert new_replace['start'] == new_replace_start
    assert new_replace['end'] == new_replace_end
    assert new_replace['is_approved']

    response = client.json.patch(
        reverse('api-v3:duty-shift-detail', args=[shift.id]),
        data={
            'replaces': []
        }
    )
    assert response.status_code == 200
    assert len(response.json()['replaces']) == 0


bad_shift_dates = [
    # Замена начинается раньше основной смены
    ('2019-04-23', '2019-04-25', '2019-04-25', '2019-04-27'),
    # Замена аканчивается позже основной смены
    ('2019-04-24', '2019-04-25', '2019-04-27', '2019-05-02'),
    # Пересечение смен
    ('2019-04-24', '2019-04-27', '2019-04-27', '2019-05-01'),
    # Конец шифта раньше начала
    ('2019-04-24', '2019-04-25', '2019-04-28', '2019-04-27')
]


@pytest.mark.parametrize('shift1_start,shift1_end,shift2_start,shift2_end', bad_shift_dates)
@freeze_time('2019-01-30T00:00:00')
def test_update_replaces_overlaps(client, owner_role, shift1_start, shift1_end, shift2_start, shift2_end, person):
    service = factories.ServiceFactory(owner=person)
    shift = create_shift(service, start='2019-04-24', end='2019-05-01')
    # Эта смена должна быть удалена
    staff_for_replace1 = factories.StaffFactory()
    staff_for_replace2 = factories.StaffFactory()
    factories.ServiceMemberFactory(staff=service.owner, role=owner_role, service=service)
    client.login(service.owner.login)

    response = client.json.patch(
        reverse('api-v3:duty-shift-detail', args=[shift.id]),
        data={
            'replaces': [
                {
                    'start': shift1_start,
                    'end': shift1_end,
                    'person': staff_for_replace1.login
                },
                {
                    'start': shift2_start,
                    'end': shift2_end,
                    'person': staff_for_replace2.login,
                },
            ]
        }
    )
    assert response.status_code == 400


@freeze_time('2019-01-30T00:00:00')
def test_update_replaces_performance(client, owner_role, django_assert_num_queries, person):
    """
        Проверяем, что рост количества запросов к базе линейный

        Запросы:
        - auth
        - проверка доступа
        - получаем основной шифт
        - получаем связанный сервис
        - получаем всех staff-ов из запроса отдельными SELECT
        - получаем существующие замены
        - batch_create для всех шифтов, которые нужно создать
        - обновляем проблемы шифта
        - делаем UPDATE для шифта, в котором сделали замену
        - сохраняем is_approved
        - для ответа делаем SELECT-ы по основному шифту и всем заменам:
            - шифт
            - стафф
            - сервис
            - тэг
            - привязанная к тегу роль
            - скоуп роли
    """
    base_queries = 25
    queries_per_staff = 10
    service = factories.ServiceFactory(owner=person)
    shift = create_shift(service, start='2019-04-01', end='2019-05-01')
    factories.ServiceMemberFactory(staff=service.owner, role=owner_role, service=service)
    client.login(service.owner.login)
    is_first = True
    with patch('plan.duty.api.serializers.priority_recalculate_shifts_for_service'):
        for count in range(1, 4):
            staffs = [factories.StaffFactory() for _ in range(count)]
            data = {
                'replaces': [
                    {
                        'start': '2019-04-%s' % day,
                        'end': '2019-04-%s' % day,
                        'person': staffs[day - 1].login
                    }
                    for day in range(1, count + 1)
                ]
            }
            expected = base_queries + queries_per_staff * count
            if is_first:
                expected = base_queries + 1 + queries_per_staff * count
                is_first = False
            with django_assert_num_queries(expected):
                response = client.json.patch(
                    reverse('api-v3:duty-shift-detail', args=[shift.id]),
                    data=data,
                )
            assert response.status_code == 200
            shift.replaces.all().delete()


@freeze_time('2019-01-30T00:00:00')
def test_update_replaces_bad_id(client, owner_role, person):
    service = factories.ServiceFactory(owner=person)
    shift = create_shift(service)
    factories.ShiftFactory(
        schedule=shift.schedule,
        start='2019-04-26',
        end='2019-04-28',
        replace_for=shift
    )
    staff_for_replace = factories.StaffFactory()
    factories.ServiceMemberFactory(staff=service.owner, role=owner_role, service=service)
    client.login(service.owner.login)

    response = client.json.patch(
        reverse('api-v3:duty-shift-detail', args=[shift.id]),
        data={
            'replaces': [
                {
                    'id': 9999,
                    'start': '2019-04-26',
                    'end': '2019-04-27',
                    'person': staff_for_replace.login
                },
            ]
        }
    )
    assert response.status_code == 404
    assert response.json() == {'error': {'code': 'not_found', 'detail': 'Shift 9999 not found'}}


@pytest.mark.parametrize('is_approved', [False, True])
@freeze_time('2019-01-30T00:00:00')
def test_update_replaces_recalculate_problems(client, owner_role, is_approved, person):
    service = factories.ServiceFactory(owner=person)
    shift = create_shift(service, start='2019-04-24', end='2019-05-01')
    staff_for_replace = factories.StaffFactory()
    factories.ServiceMemberFactory(staff=service.owner, role=owner_role, service=service)
    client.login(service.owner.login)

    with patch('plan.duty.schedulers.DutyScheduler.recalculate_shifts') as recalculate:
        response = client.json.patch(
            reverse('api-v3:duty-shift-detail', args=[shift.id]),
            data={
                'replaces': [
                    {
                        'start': shift.start,
                        'end': shift.end,
                        'person': staff_for_replace.login,
                        'is_approved': is_approved
                    }
                ]
            }
        )
        assert response.status_code == 200
        assert recalculate.call_count == 1


def test_delete_member(duty_role):
    """
    Не удаляем шифт после удаления роли и пробуем снова выдать роль дежурного
    """

    staff = factories.StaffFactory()
    service = factories.ServiceFactory()
    schedule = factories.ScheduleFactory(service=service, role_on_duty=duty_role)
    member = factories.ServiceMemberFactory(service=service, staff=staff, role=duty_role)
    shift = factories.ShiftFactory(schedule=schedule, staff=staff, state=Shift.STARTED, is_approved=True)

    # удаляем member
    member.deprive()
    shift.refresh_from_db()
    assert shift.get_member() is None

    # если роли нет у стартовавшего шифта, то при запуске таски должны пробовать снова выдать роль
    with patch('plan.api.idm.actions.request_membership') as request_membership:
        request_membership.return_value = {'id': 1}
        check_current_shifts()

    expected_args_list = [call(shift.service, staff, duty_role, comment='Начало дежурства', silent=True)]
    assert request_membership.call_args_list == expected_args_list


@freeze_time('2019-01-01')
def test_update_staff_manual_algorithm(client, owner_role, person):
    role = factories.RoleFactory()
    service = factories.ServiceFactory(owner=person)
    factories.ServiceMemberFactory(staff=service.owner, role=owner_role, service=service)
    staff_a = factories.ServiceMemberFactory(service=service, role=role).staff
    staff_b = factories.ServiceMemberFactory(service=service, role=role).staff

    schedule = factories.ScheduleFactory(
        service=service,
        algorithm=Schedule.MANUAL_ORDER,
        role=role,
        start_date=date(2019, 1, 1),
    )
    factories.OrderFactory(schedule=schedule, staff=staff_a, order=0)
    factories.OrderFactory(schedule=schedule, staff=staff_b, order=1)

    recalculate_duty_for_service(service.id)

    shift = schedule.shifts.filter(staff=staff_b).first()
    factories.GapFactory(
        start=shift.start,
        end=shift.end,
        staff=staff_b,
    )
    scheduler = DutyScheduler.initialize_scheduler_by_shift(shift)
    scheduler.update_shift_problems(shift)
    assert shift.has_problems

    client.login(service.owner.login)

    client.json.patch(
        reverse('api-v3:duty-shift-detail', args=[shift.id]),
        data={'person': staff_a.login}
    )

    shift.refresh_from_db()
    assert shift.staff == staff_a
    assert not shift.has_problems

    # после запуска пересчета стафф не должен измениться на указанный в порядке
    recalculate_duty_for_service(service.id)
    shift.refresh_from_db()
    assert shift.staff == staff_a
    assert not shift.has_problems


@freeze_time('2019-01-01')
@pytest.mark.parametrize('set_approve', [True, False])
def test_update_shift_approve(client, owner_role, set_approve, person):
    service = factories.ServiceFactory(owner=person)
    factories.ServiceMemberFactory(staff=service.owner, role=owner_role, service=service)
    staff_a = factories.ServiceMemberFactory(service=service).staff
    staff_b = factories.ServiceMemberFactory(service=service).staff
    schedule = factories.ScheduleFactory(service=service, start_date='2019-01-01')
    shift = factories.ShiftFactory(schedule=schedule, staff=staff_a)

    client.login(service.owner.login)

    data = {'person': staff_b.login}
    if set_approve:
        data['is_approved'] = False

    response = client.json.patch(reverse('api-v3:duty-shift-detail', args=[shift.id]), data=data)
    assert response.status_code == 200

    shift.refresh_from_db()
    assert shift.is_approved != set_approve


@freeze_time('2019-01-01')
def test_update_shift_replace(client, owner_role, duty_role, person):
    service = factories.ServiceFactory(owner=person)
    factories.ServiceMemberFactory(staff=service.owner, role=owner_role, service=service)
    factories.ServiceMemberFactory(service=service).staff
    staff_b = factories.ServiceMemberFactory(service=service).staff
    schedule = factories.ScheduleFactory(service=service, start_date='2019-01-01')
    recalculate_duty_for_service(service.id)

    client.login(service.owner.login)

    replaced_shift = schedule.shifts.first()

    with patch('plan.api.idm.actions.request_membership'):
        response = client.json.patch(
            reverse('api-v3:duty-shift-detail', args=[replaced_shift.id]),
            data={
                "replaces": [
                    {
                        "replace_for": replaced_shift.id,
                        "person": staff_b.login,
                        "start_datetime": replaced_shift.start_datetime.isoformat(),
                        "end_datetime": replaced_shift.end_datetime.isoformat(),
                    }
                ],
            }
        )

    assert response.status_code == 200
    duty_role = schedule.get_role_on_duty()
    replacing_shift = schedule.shifts.filter(replace_for=replaced_shift).first()
    assert replacing_shift.role == duty_role


@freeze_time('2020-01-01T00:00:00')
@pytest.mark.parametrize('api', ('v3', 'v4'))
@pytest.mark.parametrize('view_type', ('list', 'detail'))
@pytest.mark.parametrize('default_fields', (True, False))
@override_settings(DEFAULT_TIMEZONE=timezone.pytz.timezone('Europe/Moscow'))
def test_shift_date(client, owner_role, person, api, default_fields, view_type):
    service = factories.ServiceFactory(owner=person)
    factories.ServiceMemberFactory(staff=service.owner, role=owner_role, service=service)
    schedule_one_day = factories.ScheduleFactory(service=service)
    today = timezone.now().date()
    shift = factories.ShiftFactory(
        state=Shift.STARTED,
        start=today,
        end=today,
        schedule=schedule_one_day,
    )
    client.login(service.owner.login)
    params = {'date_from': '2020-01-01', 'date_to': '2020-01-01'}
    if not default_fields:
        params['fields'] = 'id,start,start_datetime,end,end_datetime'
    url_data = {'viewname': f'api-{api}:duty-shift-{view_type}'}
    if view_type == 'detail':
        url_data['args'] = [shift.id]
    response = client.get(reverse(**url_data), data=params)

    assert response.status_code == 200
    shift = response.json()
    if view_type == 'list':
        shift = shift['results'][0]

    if default_fields and api == 'v4':
        # проверяем дефолтные поля
        fields = shift.keys()
        assert len(fields) == 8

        set_default_fields = {'id', 'is_approved', 'start', 'start_datetime', 'end', 'end_datetime', 'person', 'schedule'}
        assert fields == set_default_fields

    assert shift['start'] == '2020-01-01'
    assert shift['end'] == '2020-01-01'
    assert shift['start_datetime'] == '2020-01-01T00:00:00+03:00'
    assert shift['end_datetime'] == '2020-01-02T00:00:00+03:00'


@freeze_time('2019-01-01')
def test_shifts_of_deleted_schedule_are_invisible(client, owner_role, staff_factory):
    owner = staff_factory()
    staff = factories.StaffFactory()
    service = factories.ServiceFactory(owner=owner)
    factories.ServiceMemberFactory(service=service, staff=owner, role=owner_role)
    role = factories.RoleFactory()
    factories.ServiceMemberFactory(service=service, staff=owner, role=role)
    factories.ServiceMemberFactory(service=service, staff=staff, role=role)

    schedule = factories.ScheduleFactory(
        service=service,
        role=role,
        duration=timezone.timedelta(days=7),
        start_date='2019-01-01',
        algorithm=Schedule.NO_ORDER,
    )

    client.login(owner.login)
    recalculate_duty_for_service(service.id)
    shift_count = Shift.objects.count()
    assert shift_count > 15

    response = client.json.get(
        reverse('api-v4:duty-shift-list'),
        {'date_from': '2017-01-01', 'date_to': '2222-01-01', 'service': service.pk}
    )
    assert response.status_code == 200
    assert len(response.json()['results']) > 15

    with patch('plan.duty.tasks.DutyScheduler.recalculate_shifts'):
        response = client.json.delete(reverse('api-v3:duty-schedule-detail', args=[schedule.pk]))
        assert response.status_code == 204

    schedule.refresh_from_db()
    assert schedule.status == Schedule.DELETED

    response = client.json.get(
        reverse('api-v4:duty-shift-list'),
        {'date_from': '2017-01-01', 'date_to': '2222-01-01', 'service': service.pk}
    )
    assert response.status_code == 200
    assert response.json()['results'] == []


@freeze_time('2020-09-14T12:00:00')
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=10))
def test_replacement_after_start_of_gap(owner_role, client, person):
    """
    В этом тесте замокали период, на который создаются смены: вместо полгода - 10 дней.
    Добавляем замену в середину смены таким образом, чтобы последний сегмент не был проблемным.
    Есть гэп => у нас есть минимальный день отсуствия за всю смену.
    Однако на сегмент в конце смены отсутствия не выпадают.
    """

    service = factories.ServiceFactory(owner=person)
    schedule = factories.ScheduleFactory(
        service=service,
        start_date=timezone.datetime(2020, 9, 7).date(),
        duty_on_weekends=True,
        duty_on_holidays=True,
        algorithm=Schedule.NO_ORDER,
        duration=timezone.timedelta(days=7)
    )
    factories.ServiceMemberFactory(service=schedule.service, role=owner_role, staff=schedule.service.owner)

    staff = factories.ServiceMemberFactory(service=schedule.service, role=schedule.role).staff

    # рассчитаем график
    recalculate_duty_for_service(schedule.service.id)
    current_shift = schedule.shifts.current_shifts().first()
    assert current_shift.staff == staff

    factories.GapFactory(
        staff=staff,
        start='2020-09-14T00:00:00Z',
        end='2020-09-17T00:00:00Z',
    )

    new_staff = factories.ServiceMemberFactory(service=schedule.service, role=schedule.role).staff
    client.login(schedule.service.owner.login)

    with patch('plan.api.idm.actions.request_membership'):
        response = client.json.patch(
            reverse('api-v3:duty-shift-detail', args=[current_shift.id]),
            data={
                "replaces": [
                    {
                        "replace_for": current_shift.id,
                        "person": new_staff.login,
                        "start_datetime": '2020-09-16T00:00:00Z',
                        "end_datetime": '2020-09-19T00:00:00Z',
                    }
                ],
            }
        )

    assert response.status_code == 200


@freeze_time('2020-09-14T12:00:00')
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=10))
@pytest.mark.parametrize('order', (Schedule.MANUAL_ORDER, Schedule.NO_ORDER))
@pytest.mark.parametrize('has_role', (False, True))
def test_update_shift_change_staff_on_self(client, staff_factory, order, has_role):
    service = factories.ServiceFactory()
    schedule = factories.ScheduleFactory(
        service=service,
        start_date=timezone.datetime(2020, 9, 7).date(),
        duty_on_weekends=True,
        duty_on_holidays=True,
        algorithm=order,
        duration=timezone.timedelta(days=2)
    )
    if not has_role:
        schedule.role = None
        schedule.save()
    staff1 = staff_factory()
    staff2 = staff_factory()
    if has_role:
        factories.ServiceMemberFactory(service=service, role=schedule.role, staff=staff1)
        factories.ServiceMemberFactory(service=service, role=schedule.role, staff=staff2)
    else:
        factories.ServiceMemberFactory(service=service, staff=staff1)
        factories.ServiceMemberFactory(service=service, staff=staff2)

    if order == Schedule.MANUAL_ORDER:
        factories.OrderFactory(schedule=schedule, staff=staff1, order=0)
        factories.OrderFactory(schedule=schedule, staff=staff2, order=1)

    recalculate_duty_for_service(schedule.service.id)

    shift = schedule.shifts.future().first()
    other_staff = staff1 if shift.staff == staff2 else staff2

    # Замена смены на себя работает для участника графика
    client.login(other_staff.login)
    response = client.json.patch(reverse('api-v3:duty-shift-detail', args=[shift.id]), {'person': other_staff.login})
    assert response.status_code == 200

    # Так же можно сразу подтвердить смену если назначаешь ее на себя
    response = client.json.patch(
        reverse('api-v3:duty-shift-detail', args=[shift.id]),
        {'person': other_staff.login, 'is_approved': True}
    )
    assert response.status_code == 200

    # Другие поля редактировать нельзя
    response = client.json.patch(
        reverse('api-v3:duty-shift-detail', args=[shift.id]),
        {'person': other_staff.login, 'start_datetime': '2020-10-01'}
    )
    assert response.status_code == 403

    stranger = staff_factory()
    if order == Schedule.MANUAL_ORDER:
        if has_role:
            factories.ServiceMemberFactory(service=service, role=schedule.role, staff=stranger)
        else:
            factories.ServiceMemberFactory(service=service, staff=stranger)

    # Замена смены на себя не работает для постороннего
    client.login(stranger.login)
    response = client.json.patch(reverse('api-v3:duty-shift-detail', args=[shift.id]), {'person': stranger.login})
    assert response.status_code == 403


def test_can_approve_own_shift(client, staff_factory):
    service = factories.ServiceFactory()
    schedule = factories.ScheduleFactory(
        service=service,
        start_date=timezone.datetime(2020, 9, 7).date(),
        duty_on_weekends=True,
        duty_on_holidays=True,
        duration=timezone.timedelta(days=2)
    )
    staff1 = staff_factory()
    staff2 = staff_factory()
    factories.ServiceMemberFactory(service=service, role=schedule.role, staff=staff1)
    factories.ServiceMemberFactory(service=service, role=schedule.role, staff=staff2)

    recalculate_duty_for_service(schedule.service.id)

    shift = schedule.shifts.future().filter(is_approved=False, staff=staff1).first()

    # Подтверждение не работает если смена чужая
    client.login(staff2.login)
    response = client.json.patch(
        reverse('api-v3:duty-shift-detail', args=[shift.id]),
        {'is_approved': True}
    )
    assert response.status_code == 403

    shift.refresh_from_db()
    assert shift.is_approved is False

    # Подтверждение работает если смена твоя
    client.login(staff1.login)
    response = client.json.patch(
        reverse('api-v3:duty-shift-detail', args=[shift.id]),
        {'is_approved': True}
    )
    assert response.status_code == 200

    shift.refresh_from_db()
    assert shift.is_approved


@pytest.mark.parametrize('with_watcher', [True, False])
@pytest.mark.parametrize('proxy_enable', [True, False])
@pytest.mark.parametrize('on_duty', [True, False])
@pytest.mark.parametrize('filter_param', ['schedule', 'service__slug', 'service', 'schedule__slug'])
@pytest.mark.parametrize('mock_tvm_service_ticket', [settings.WATCHER_PROXY_ENABLE_TVM_IDS[0], 100], indirect=True)
def test_watcher_shifts(
    client, mock_tvm_service_ticket, with_watcher,
    proxy_enable, filter_param, on_duty,
):
    shift = factories.ShiftFactory()
    staff = factories.StaffFactory()
    with patch.object(WatcherClient, 'get_shifts') as watcher_response:
        watcher_response.return_value = [
            {
                'id': 1001,
                'staff': {'id': 1005, 'login': staff.login, 'staff_id': 1007},
                'schedule': {
                    'id': shift.schedule.id,
                    'slug': shift.schedule.slug,
                    'name': shift.schedule.name,
                },
                'is_primary': True,
                'approved': True,
                'end': '2022-01-03T00:00:00+03:00',
                'start': '2021-12-31T00:00:00+03:00',
            },
            {'id': 100, 'staff': None},
        ]
        kwargs = {}
        if filter_param == 'schedule':
            kwargs['schedule'] = shift.schedule.id
        elif filter_param == 'service':
            kwargs['service'] = shift.schedule.service.id
        elif filter_param == 'service__slug':
            kwargs['service__slug'] = shift.schedule.service.slug
        elif filter_param == 'schedule__slug':
            kwargs['schedule__slug'] = shift.schedule.slug
        if with_watcher:
            kwargs['with_watcher'] = '1'
        with override_settings(WATCHER_PROXY_ENABLE=proxy_enable):
            shifts = get_shifts(client, on_duty=on_duty, **kwargs)
    assert shifts[0]['id'] == shift.id
    if proxy_enable and (with_watcher or mock_tvm_service_ticket in settings.WATCHER_PROXY_ENABLE_TVM_IDS):
        assert len(shifts) == 2
        watcher_shift = shifts[1]
        assert watcher_shift['id'] == 1001
        assert watcher_shift['schedule']['name'] == shift.schedule.name
        assert watcher_shift['schedule']['slug'] == shift.schedule.slug
        assert watcher_shift['start'] == '2021-12-31'
        assert watcher_shift['end'] == '2022-01-02'
        assert watcher_shift['from_watcher'] is True
        assert watcher_shift['is_primary'] is True
        assert watcher_shift['person']['id'] == 1007
        assert watcher_shift['person']['name']['ru'] == f'{staff.first_name} {staff.last_name}'.strip()
        expected_call = {
            'date_from': '2017-01-01',
            'date_to': '2222-01-01',
            'schedule_id': None,
            'service_id': None,
            'schedule_slug': None,
            'service_slug': None,
        }
        if filter_param == 'service':
            expected_call['service_id'] = str(shift.schedule.service.id)
        elif 'schedule__slug' in filter_param:
            expected_call['schedule_slug'] = shift.schedule.slug
        elif 'service__slug' in filter_param:
            expected_call['service_slug'] = shift.schedule.service.slug
        else:
            expected_call['schedule_id'] = str(shift.schedule.id)
        if on_duty:
            expected_call['current'] = True
            expected_call.pop('date_from')
            expected_call.pop('date_to')

        watcher_response.assert_called_once_with(**expected_call)
    else:
        assert len(shifts) == 1


@pytest.mark.parametrize('mock_tvm_service_ticket', [settings.WATCHER_TVM_ID, 123], indirect=True)
def test_dutytowatcher_create(client, mock_tvm_service_ticket):
    service = factories.ServiceFactory()
    schedule = factories.ScheduleFactory(service=service)

    response = client.json.post(
        reverse('api-v4:duty-abc-to-watcher-list'),
        {
            'abc_id': schedule.pk,
            'watcher_id': 999,
        },
    )
    if mock_tvm_service_ticket == settings.WATCHER_TVM_ID:
        assert response.status_code == 201
        assert DutyToWatcher.objects.get(abc_id=schedule.pk).watcher_id == 999
    else:
        assert response.status_code == 403
        assert not DutyToWatcher.objects.filter(abc_id=schedule.pk).exists()


@pytest.mark.parametrize('username', (settings.MAGICLINKS_ROBOT_LOGIN, 'user'))
def test_watcher_schedule_magiclinks(client, staff_factory, username):
    duty_to_watcher = factories.DutyToWatcherFactory()
    staff = staff_factory(
        'full_access', is_robot=True, user=factories.UserFactory(username=username),
    )

    with patch.object(WatcherClient, 'get_shifts') as watcher_response:
        watcher_response.return_value = [{}]
        client.login(staff.login)
        with override_settings(WATCHER_PROXY_ENABLE=True):
            shifts = get_shifts(
                client, on_duty=True, schedule=duty_to_watcher.abc_id
            )
            assert len(shifts) == 0

    expected_call = {
        'current': True,
        'service_id': None,
        'schedule_slug': None,
        'service_slug': None,
    }
    if username is settings.MAGICLINKS_ROBOT_LOGIN:
        expected_call['schedule_id'] = str(duty_to_watcher.watcher_id)
        watcher_response.assert_called_once_with(**expected_call)
    else:
        expected_call['schedule_id'] = str(duty_to_watcher.abc_id)
        watcher_response.assert_not_called()
