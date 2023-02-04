import datetime

import pretend
import pytest

from freezegun import freeze_time
from django.core.urlresolvers import reverse
from django.test import override_settings
from django.utils import timezone

from plan.duty.models import Shift, Schedule, Role
from plan.duty.tasks import recalculate_all_duties, recalculate_duty_for_service
from plan.staff.models import Staff
from common import factories


pytestmark = pytest.mark.django_db


@pytest.fixture
def data(owner_role, staff_factory):
    owner = staff_factory()
    service = factories.ServiceFactory(owner=owner)
    factories.ServiceMemberFactory(role=owner_role, service=service, staff=owner)
    role = factories.RoleFactory()
    staffs_role = [
        factories.ServiceMemberFactory(service=service, role=role).staff
        for _ in range(3)
    ]
    staffs_other = [factories.ServiceMemberFactory(service=service).staff for _ in range(3)]
    schedule_role = factories.RoleFactory()
    schedule_members = [
        factories.ServiceMemberFactory(service=service, role=schedule_role)
        for _ in range(3)
    ]
    schedule = factories.ScheduleFactory(
        role=schedule_role,
        service=service,
        algorithm=Schedule.MANUAL_ORDER,
        autoapprove_timedelta=timezone.timedelta(0),
    )
    staffs_schedule = [
        schedule_members[i].staff
        for i in range(3)
    ]
    all_staffs = staffs_role + staffs_other + staffs_schedule
    return pretend.stub(
        schedule_members=schedule_members,
        logins_all={staff.login for staff in all_staffs},
        logins_schedule={staff.login for staff in staffs_schedule},
        logins_role={staff.login for staff in staffs_role},
        logins_other={staff.login for staff in staffs_other},
        service=service,
        role=role,
        schedule=schedule,
        owner=owner.login,
    )


@pytest.fixture()
def schedules(data):
    all_logins = list(data.logins_all)
    schedules = {}
    for algorithm in (Schedule.NO_ORDER, Schedule.MANUAL_ORDER):
        schedules[algorithm] = factories.ScheduleFactory(
            service=data.service, role=None,
            algorithm=algorithm,
            start_date=datetime.date(2020, 12, 14),
            only_workdays=True,
        )
    for index, login in enumerate(all_logins):
        factories.OrderFactory(
            order=index,
            staff=Staff.objects.get(login=login),
            schedule=schedules[Schedule.MANUAL_ORDER],
        )
    recalculate_all_duties()
    return schedules


@freeze_time('2018-01-02')
def test_schedule_with_replaced_shifts(client, data):
    shift_1 = factories.ShiftFactory(
        schedule=data.schedule,
        start=timezone.datetime(2018, 1, 1),
        end=timezone.datetime(2018, 2, 3),
        staff=data.schedule_members[1].staff
    )
    shift_2 = factories.ShiftFactory(
        schedule=data.schedule,
        start=timezone.datetime(2018, 1, 1),
        end=timezone.datetime(2018, 2, 3),
        staff=data.schedule_members[0].staff
    )

    shift_2.replaces.add(shift_1)

    assert shift_1.replace_for is not None

    resp = client.json.get(
        reverse('api-v3:allowforduty-list'),
        {
            'service': data.service.id,
            'schedule': data.schedule.id
        }
    )

    resp_logins = {staff['login'] for staff in resp.json()['results'] if staff['active_duty']}
    assert resp_logins == {data.schedule_members[0].staff.login}


def test_service(client, data):
    schedule_role = factories.RoleFactory(code=Role.RESPONSIBLE_FOR_DUTY)
    factories.ServiceMemberFactory(service=data.service, role=schedule_role)

    resp = client.json.get(
        reverse('api-v3:allowforduty-list'),
        {'service': data.service.id},
    )
    resp_logins = {staff['login'] for staff in resp.json()['results']}
    assert resp_logins == data.logins_all
    assert data.owner not in resp_logins


def test_role(client, data):
    resp = client.json.get(
        reverse('api-v3:allowforduty-list'),
        {
            'service': data.service.id,
            'role': data.role.id,
        }
    )
    resp_logins = {staff['login'] for staff in resp.json()['results']}
    assert resp_logins == data.logins_role


def test_schedule(client, data):
    resp = client.json.get(
        reverse('api-v3:allowforduty-list'),
        {
            'service': data.service.id,
            'schedule': data.schedule.id,
        }
    )
    resp_logins = {staff['login'] for staff in resp.json()['results']}
    assert resp_logins == data.logins_schedule


def test_schedule_and_role(client, data):
    resp = client.json.get(
        reverse('api-v3:allowforduty-list'),
        {
            'service': data.service.id,
            'schedule': data.schedule.id,
            'role': data.role.id,
        }
    )
    resp_logins = {staff['login'] for staff in resp.json()['results']}
    assert resp_logins == (data.logins_schedule | data.logins_role)


def test_ordered_error(client, data):
    response = client.json.get(
        reverse('api-v3:allowforduty-list'),
        {'service': data.service.id, 'ordered': 1},
    )

    assert response.status_code == 400
    assert response.json()['error']['extra']['ordered'] == 'ordered set without schedule'


@freeze_time('2020-12-15')
@pytest.mark.parametrize("algorithm", [Schedule.NO_ORDER, Schedule.MANUAL_ORDER])
def test_ordered(client, data, schedules, algorithm):
    schedule = schedules[algorithm]

    response = client.json.get(
        reverse('api-v3:allowforduty-list'),
        {
            'service': schedule.service.id,
            'schedule': schedule.id,
            'ordered': 1,
        }
    )
    assert response.status_code == 200

    duties = response.json()['results']
    duty_logins = [d['login'] for d in duties]

    shifts = Shift.objects.filter(schedule_id=schedule.id).order_by('start_datetime')
    shift_logins = list(
        shifts.values_list('staff__login', flat=True)[:len(data.logins_all)]
    )

    assert len(duty_logins) == len(shift_logins)
    # порядок дежурных в ответе смещен на 1, т.к. 1-я смена уже началась
    expected_logins = shift_logins[1:] + shift_logins[:1]
    assert duty_logins == expected_logins


@pytest.mark.parametrize('ordered, excluded_duties', [
    (0, 0),
    (1, 1),
])
def test_order_and_active_duty(client, data, duty_role, ordered, excluded_duties):
    schedule = data.schedule
    logins_schedule = list(data.logins_schedule)
    for index, login in enumerate(logins_schedule[:2]):
        factories.OrderFactory(
            order=index,
            staff=Staff.objects.get(login=login),
            schedule=schedule,
        )

    recalculate_all_duties()
    shift = schedule.shifts.filter(staff__login=logins_schedule[0]).first()
    shift.state = Shift.STARTED
    shift.save()

    resp = client.json.get(
        reverse('api-v3:allowforduty-list'),
        {
            'service': data.service.id,
            'schedule': data.schedule.id,
            'ordered': ordered,
        }
    )
    staffs = resp.json()['results']
    assert len(staffs) == len(logins_schedule) - excluded_duties

    active_duty = [staff['login'] for staff in staffs if staff['active_duty']]
    assert active_duty == [logins_schedule[0]]

    in_order = [(staff['login'], staff['order']) for staff in staffs if staff['order'] is not None]
    assert set(in_order) == {(logins_schedule[0], 0), (logins_schedule[1], 1)}

    start_with = [staff['login'] for staff in staffs if staff['start_with']]
    assert start_with == [logins_schedule[1]]


@pytest.mark.parametrize('ordered', [0, 1])
def test_order_and_null_staff(client, data, duty_role, ordered):
    """
    Проверим, что не отдаём пустые ордеры (без стаффа)
    """

    schedule = data.schedule
    logins_schedule = list(data.logins_schedule)
    for index, login in enumerate(logins_schedule):
        factories.OrderFactory(
            order=index,
            staff=Staff.objects.get(login=login),
            schedule=schedule,
        )

    recalculate_all_duties()
    login = logins_schedule[1]
    order = schedule.orders.filter(staff__login=login).first()
    schedule.service.members.filter(staff__login=login).deprive()

    recalculate_all_duties()
    order.refresh_from_db()
    assert order.staff is None

    response = client.json.get(
        reverse('api-v3:allowforduty-list'),
        {
            'service': data.service.id,
            'schedule': data.schedule.id,
            'ordered': ordered,
        }
    )
    staffs = response.json()['results']

    assert login not in [staff['login'] for staff in staffs]
    assert len(response.json()['results']) == 2


@freeze_time('2019-01-01')
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=30))
@pytest.mark.parametrize(('api', 'num_queries'), [('api-v3', 18), ('api-v4', 18)])
def test_patch_start_with_two_duties(client, staff_factory, owner_role, transactional_db, api, num_queries, django_assert_num_queries):
    """
    Результат PATCH-запроса по графику с несколькими одновременными дежурными и
    start_with равным текущему дежурному правильно отображается в allowforduty-list.
    """
    owner = staff_factory()
    service = factories.ServiceFactory(owner=owner)
    factories.ServiceMemberFactory(role=owner_role, service=service, staff=owner)

    logins = ['aatjukov', 'trshkv', 'kamtim', 'boolat', 'yavanosta']
    role = factories.RoleFactory()
    for login in logins:
        factories.ServiceMemberFactory(
            service=service,
            role=role,
            staff=factories.StaffFactory(login=login)
        )

    schedule = factories.ScheduleFactory(
        service=service,
        role=role,
        start_date='2019-01-01',
        duration=timezone.timedelta(days=5),
    )
    recalculate_duty_for_service(service.id)

    client.login(owner.login)
    active_duty_login = schedule.shifts.current_shifts().first().staff.login
    response = client.json.patch(
        reverse(f'{api}:duty-schedule-detail', args=[schedule.pk]),
        data={
            'algorithm': Schedule.MANUAL_ORDER,
            'persons_count': 2,
            'orders': logins,
            'start_with': active_duty_login,
        }
    )
    assert response.status_code == 200

    schedule.shifts.current_shifts().update(state=Shift.STARTED)
    with django_assert_num_queries(num_queries):
        resp = client.json.get(
            reverse(f'{api}:allowforduty-list'),
            {'service': service.id, 'schedule': schedule.id, 'fields': 'login,start_with,active_duty'}
        )
    assert resp.status_code == 200

    results = {staff['login']: staff for staff in resp.json()['results']}
    assert results[active_duty_login]['start_with']
    assert results[active_duty_login]['active_duty']
