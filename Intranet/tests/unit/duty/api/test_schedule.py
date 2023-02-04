import datetime

import pytz

import mock
import pretend
import pytest

from freezegun import freeze_time
from django.core.urlresolvers import reverse
from django.utils import timezone
from django.conf import settings
from django.test import override_settings

from plan.common.utils.watcher import WatcherClient
from plan.common.utils import timezone as utils
from plan.duty.models import Shift, Schedule
from plan.duty.tasks import recalculate_duty_for_service, remove_inactive_schedules
from plan.roles.models import Role
from plan.staff.constants import LANG
from common import factories

pytestmark = pytest.mark.django_db


@pytest.fixture(autouse=True)
def patch_watcher(monkeypatch):
    monkeypatch.setattr(WatcherClient, 'get_schedules', lambda *args, **kwargs: {})


@pytest.fixture
def duty_data(owner_role, transactional_db, staff_factory):
    owner = staff_factory()
    service = factories.ServiceFactory(owner=owner)
    factories.ServiceMemberFactory(service=service, staff=owner, role=owner_role)
    role = factories.RoleFactory()

    return pretend.stub(owner=owner, service=service, role=role)


@pytest.mark.parametrize(('user_lang', 'message'), [
    (LANG.RU, 'Ошибка валидации.'),
    (LANG.EN, 'Invalid input.')
])
@pytest.mark.parametrize('api', ['api-v3', 'api-v4'])
def test_update_schedule_langs(client, duty_data, user_lang, message, api):
    schedule = factories.ScheduleFactory(
        service=duty_data.service,
        start_date='2019-03-06',
        role=duty_data.role,
        name='Найм',
    )

    client.login(duty_data.owner.login)
    response = client.json.patch(
        reverse(f'{api}:duty-schedule-detail', args=[schedule.pk]),
        data={
            'slug': '',
            'start_date': '',
        },
        HTTP_ACCEPT_LANGUAGE=user_lang
    )
    assert response.status_code == 400
    assert response.json()['error']['detail'] == message


@pytest.mark.parametrize('with_watcher', [True, False])
@pytest.mark.parametrize('filter_param', ['id', 'service__slug', 'service', None])
@pytest.mark.parametrize('proxy_enable', [True, False])
@pytest.mark.parametrize('mock_tvm_service_ticket', [settings.WATCHER_PROXY_ENABLE_TVM_IDS[0], 100], indirect=True)
def test_watcher_schedules(client, duty_data, mock_tvm_service_ticket, with_watcher, filter_param, proxy_enable):
    schedule_1 = factories.ScheduleFactory(service=duty_data.service, is_important=True)
    factories.ScheduleFactory()
    url = reverse('api-v4:duty-schedule-list')

    with mock.patch.object(WatcherClient, 'get_schedules') as watcher_response:
        watcher_response.return_value = [
            {
                'slug': 'some_slug', 'id': 100500,
                'service_id': duty_data.service.id, 'name': 'some_name',
                'show_in_staff': True,
            }
        ]
        params = {
            'fields': 'id,service.slug,service.name,role.name,show_in_staff,is_important',
        }
        if filter_param:
            if filter_param == 'id':
                params['id'] = 100500
            elif filter_param == 'service__slug':
                params['service__slug'] = duty_data.service.slug
            elif filter_param == 'service':
                params['service'] = duty_data.service.id
        if with_watcher:
            params['with_watcher'] = '1'

        with override_settings(WATCHER_PROXY_ENABLE=proxy_enable):
            response = client.json.get(
                url, params,
                HTTP_X_YA_SERVICE_TICKET='ticket',
            )

    assert response.status_code == 200
    data = response.json()

    field_params = ['show_in_staff']
    if proxy_enable and (with_watcher or mock_tvm_service_ticket in settings.WATCHER_PROXY_ENABLE_TVM_IDS):
        if not filter_param:
            assert data['count'] == 3
            watcher_schedule = data['results'][-1]
            watcher_response.assert_called_once_with(
                schedule_id=None,
                service_slug=None,
                service_id=None,
                state='active',
                field_params=field_params,
            )
        elif filter_param == 'id':
            assert data['count'] == 1
            watcher_schedule = data['results'][0]
            watcher_response.assert_called_once_with(
                schedule_id='100500',
                service_slug=None,
                service_id=None,
                state='active',
                field_params=field_params,
            )
        elif filter_param == 'service__slug':
            assert data['count'] == 2
            assert data['results'][0]['id'] == schedule_1.pk
            assert data['results'][0]['is_important'] is True
            watcher_response.assert_called_once_with(
                schedule_id=None,
                service_slug=duty_data.service.slug,
                service_id=None,
                state='active',
                field_params=field_params,
            )
            watcher_schedule = data['results'][1]
        else:
            assert data['count'] == 2
            assert data['results'][0]['id'] == schedule_1.pk
            assert data['results'][0]['is_important'] is True
            watcher_response.assert_called_once_with(
                schedule_id=None,
                service_slug=None,
                service_id=str(duty_data.service.id),
                state='active',
                field_params=field_params,
            )
            watcher_schedule = data['results'][1]

        assert watcher_schedule['id'] == 100500
        assert watcher_schedule['slug'] == 'some_slug'
        assert watcher_schedule['name'] == 'some_name'
        assert watcher_schedule['service']['slug'] == duty_data.service.slug
        assert watcher_schedule['service']['name']['ru'] == duty_data.service.name
        assert watcher_schedule['from_watcher'] is True
        assert watcher_schedule['is_important'] is False
        assert watcher_schedule['show_in_staff'] is True
    else:
        if filter_param:
            if filter_param == 'id':
                assert data['count'] == 0
                assert len(data['results']) == 0
            else:
                assert data['count'] == 1
                assert len(data['results']) == 1
        else:
            assert data['count'] == 2
            assert len(data['results']) == 2


def test_filter_by_service(client, duty_data):
    schedule_1 = factories.ScheduleFactory(service=duty_data.service)
    factories.ScheduleFactory()
    url = reverse('api-v3:duty-schedule-list')

    response = client.json.get(url, {'service': duty_data.service.id, 'fields': 'id,service.id'})
    assert response.status_code == 200

    assert response.json()['count'] == 1
    assert response.json()['results'][0]['id'] == schedule_1.pk


def test_filter_by_service_list(client, duty_data):
    schedule_1 = factories.ScheduleFactory(service=duty_data.service)
    schedule_2 = factories.ScheduleFactory()
    factories.ScheduleFactory()

    response = client.json.get(
        reverse('api-v4:duty-schedule-list'),
        {
            'service__slug__in': f'{schedule_1.service.slug},{schedule_2.service.slug}',
            'fields': 'id,service.id'
        }
    )
    assert response.status_code == 200
    data = response.json()['results']
    assert len(data) == 2
    assert {schedule['id'] for schedule in data} == {schedule_1.id, schedule_2.id}


@pytest.mark.parametrize('is_important', [True, False])
def test_filter_by_service_cursor(client, duty_data, is_important):
    schedule_1 = factories.ScheduleFactory(service=duty_data.service, is_important=is_important)
    factories.ScheduleFactory()
    url = reverse('api-v4:duty-schedule-cursor-list')
    response = client.json.get(url, {'service': duty_data.service.id, 'fields': 'id,service.id,is_important'})
    assert response.status_code == 200
    data = response.json()['results']
    assert len(data) == 1
    assert data[0]['id'] == schedule_1.pk
    assert data[0]['is_important'] is is_important


def test_filter_by_service_no_match(client, duty_data):
    factories.ScheduleFactory(service=duty_data.service)
    factories.ScheduleFactory()
    url = reverse('api-v3:duty-schedule-list')

    response = client.json.get(url, {'service': 100500, 'fields': 'id,service.id'})
    assert response.status_code == 200

    assert response.json()['count'] == 0


@pytest.mark.parametrize('is_important', [True, False])
def test_create_is_important(client, duty_data, duty_role, is_important):
    client.login(duty_data.owner.login)
    with mock.patch('plan.duty.tasks.DutyScheduler.recalculate_shifts'):
        response = client.json.post(
            reverse('api-v3:duty-schedule-list'),
            data={
                'name': 'name1',
                'slug': 'some_slug',
                'role': duty_data.role.id,
                'service': duty_data.service.id,
                'start_date': '2019-01-01',
                'duration': '3 00',
                'is_important': is_important,
            }
        )

    assert response.status_code == 201
    schedule = Schedule.objects.get(pk=response.json()['id'])
    assert schedule.is_important is False
    schedule.is_important = True
    schedule.save()
    with mock.patch('plan.duty.tasks.DutyScheduler.recalculate_shifts'):
        response = client.json.patch(
            reverse('api-v3:duty-schedule-detail', args=[schedule.pk]),
            data={'is_important': is_important},
        )
    assert response.status_code == 200
    schedule.refresh_from_db()
    assert schedule.is_important is True


@freeze_time('2019-01-01')
@pytest.mark.parametrize('slug', [None, '123'])
@pytest.mark.parametrize('show_in_staff', [False, True])
def test_create_schedule(client, duty_data, duty_role, slug, show_in_staff):
    client.login(duty_data.owner.login)

    old_schedule = factories.ScheduleFactory(
        service=duty_data.service,
    )

    with mock.patch('plan.duty.tasks.DutyScheduler.recalculate_shifts') as recalculate:
        if slug is None:
            response = client.json.post(
                reverse('api-v3:duty-schedule-list'),
                data={
                    'name': 'name1',
                    'role': duty_data.role.id,
                    'service': duty_data.service.id,
                    'start_date': '2019-01-01',
                    'duration': '3 00',
                }
            )
            assert response.status_code == 400

        else:
            response = client.json.post(
                reverse('api-v3:duty-schedule-list'),
                data={
                    'name': 'name1',
                    'description': 'description',
                    'role': duty_data.role.id,
                    'service': duty_data.service.id,
                    'start_date': '2019-01-01',
                    'duration': '3 00',
                    'slug': slug,
                    'show_in_staff': show_in_staff,
                }
            )

            assert response.status_code == 201
            assert recalculate.call_count == 1
            schedule = duty_data.service.schedules.order_by('id').last()
            assert schedule.id == response.json()['id']
            assert schedule.role == duty_data.role
            assert schedule.start_date == datetime.date(2019, 1, 1)
            assert schedule.name == 'name1'
            assert schedule.description == 'description'
            assert schedule.duration == timezone.timedelta(days=3)
            assert schedule.get_role_on_duty() == duty_role
            assert schedule.show_in_staff == show_in_staff
            assert schedule.is_important is False
            recalculate.assert_called_once_with(duty_data.service, [old_schedule.id, schedule.id], None)


@freeze_time('2019-01-01')
@pytest.mark.parametrize(('start_time', 'status_code'), [
    ('12:00', 201),
    ('23:59', 201),
    ('01:00', 201),
    ('25:00', 400),
    ('2019-01-01', 400),
    ('None', 400),
    (None, 201),  # если в запросе не было поля start_time
])
def test_create_schedule_start_time(client, duty_data, start_time, status_code):
    client.login(duty_data.owner.login)

    data = {
        'name': '123',
        'slug': 'slug1',
        'role': duty_data.role.id,
        'service': duty_data.service.id,
        'start_date': '2019-01-01',
        'start_time': start_time,
        'duration': '3 00',
    }

    if start_time is None:
        data.pop('start_time')

    with mock.patch('plan.duty.tasks.DutyScheduler.recalculate_shifts'):
        response = client.json.post(
            reverse('api-v3:duty-schedule-list'),
            data=data
        )
        assert response.status_code == status_code


def test_create_schedule_with_role_on_duty(client, duty_data):
    role_on_duty = Role.objects.globalwide().get(code=Role.DUTY)

    client.login(duty_data.owner.login)
    with mock.patch('plan.duty.tasks.DutyScheduler.recalculate_shifts'):
        response = client.json.post(
            reverse('api-v4:duty-schedule-list'),
            data={
                'name': 'name1',
                'role': duty_data.role.id,
                'service': duty_data.service.id,
                'start_date': '2019-01-01',
                'duration': '3 00',
                'role_on_duty': role_on_duty.pk,
                'slug': '123',
            },
        )

    assert response.status_code == 201
    schedule = duty_data.service.schedules.order_by('id').get(name='name1')
    assert schedule.role_on_duty == role_on_duty


@pytest.mark.parametrize(
    ('language', 'error_text'),
    (('ru', 'Роль %s не может быть выдана во время дежурства'), ('en', 'Role %s can\'t be granted during duty'))
)
@pytest.mark.parametrize('error_source', ('code', 'scope'))
def test_create_schedule_with_role_on_duty_forbidden(client, duty_data, language, error_text, error_source):
    """Запрещено выдавать на дежурствах роли с кодом из Role.CAN_NOT_USE_FOR_DUTY"""
    if error_source == 'code':
        role_on_duty = factories.RoleFactory(code=Role.RESPONSIBLE)
    else:
        scope = factories.RoleScopeFactory(can_issue_at_duty_time=False)
        role_on_duty = factories.RoleFactory(scope=scope)

    client.login(duty_data.owner.login)
    with mock.patch('plan.duty.tasks.DutyScheduler.recalculate_shifts'):
        response = client.json.post(
            reverse('api-v4:duty-schedule-list'),
            data={
                'name': 'name1',
                'role': duty_data.role.id,
                'service': duty_data.service.id,
                'start_date': '2019-01-01',
                'duration': '3 00',
                'role_on_duty': role_on_duty.pk,
                'slug': '123',
            },
            **{
                'HTTP_ACCEPT_LANGUAGE': language
            }
        )

    assert response.status_code == 400
    assert response.json()['error']['extra']['role_on_duty'] == [error_text % role_on_duty.name]


@pytest.mark.parametrize(
    ('language', 'error_text'),
    (('ru', 'Роль %s не может быть выдана во время дежурства'), ('en', 'Role %s can\'t be granted during duty'))
)
def test_create_schedule_with_role_on_duty_conflict(client, duty_data, language, error_text):
    """Запрещено выдавать на дежурство уже имеющиеся у дежурных роли"""
    client.login(duty_data.owner.login)
    with mock.patch('plan.duty.tasks.DutyScheduler.recalculate_shifts'):
        response = client.json.post(
            reverse('api-v4:duty-schedule-list'),
            data={
                'name': 'name1',
                'role': duty_data.role.id,
                'service': duty_data.service.id,
                'start_date': '2019-01-01',
                'duration': '3 00',
                'role_on_duty': duty_data.role.id,
                'slug': '123',
            },
            **{
                'HTTP_ACCEPT_LANGUAGE': language
            }
        )

    assert response.status_code == 400
    assert response.json()['error']['extra']['role_on_duty'] == [error_text % duty_data.role.name]


@freeze_time('2019-01-01')
@pytest.mark.parametrize('param', ['duration', 'role'])
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=15))
def test_update_schedule_critical(client, duty_data, param):
    associated_schedule = factories.ScheduleFactory(
        service=duty_data.service,
    )
    schedule = factories.ScheduleFactory(
        service=duty_data.service,
        start_date='2019-01-01',
        duration=timezone.timedelta(days=1),
        role=duty_data.role,
        name='name1',
    )
    recalculate_duty_for_service(duty_data.service.id)
    started_shift = schedule.shifts.first()
    factories.ShiftFactory(schedule=schedule, state=Shift.SCHEDULED)
    client.login(duty_data.owner.login)
    if param == 'duration':
        data = {'duration': '5 00'}
    elif param == 'role':
        data = {'role': None}
    with mock.patch('plan.duty.tasks.DutyScheduler.recalculate_shifts') as recalculate:
        response = client.json.patch(
            reverse('api-v3:duty-schedule-detail', args=[schedule.pk]),
            data=data,
        )
        assert response.status_code == 200
        assert recalculate.call_count == 1
        assert schedule.shifts.get().id == started_shift.id
        recalculate.assert_called_once_with(duty_data.service, [associated_schedule.id, schedule.id], None)


@freeze_time('2019-05-01')
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=15))
def test_update_schedule_critical_with_old_shift(client, duty_data):
    for _ in range(3):
        factories.ServiceMemberFactory(service=duty_data.service, role=duty_data.role)

    schedule = factories.ScheduleFactory(
        service=duty_data.service,
        start_date='2019-01-01',
        duration=timezone.timedelta(days=1),
        role=duty_data.role,
        name='name1',
    )

    recalculate_duty_for_service(duty_data.service.id)
    today = timezone.now().date()
    old_shifts = schedule.shifts.filter(start__lte=today).values_list('id', 'staff__login')

    client.login(duty_data.owner.login)
    response = client.json.patch(
        reverse('api-v3:duty-schedule-detail', args=[schedule.pk]),
        data={
            'duration': '5 00',
        }
    )
    assert response.status_code == 200

    schedule.refresh_from_db()
    new_shifts = schedule.shifts.filter(start__lte=today).values_list('id', 'staff__login')

    assert set(old_shifts) == set(new_shifts)


@freeze_time('2019-01-01')
def test_update_schedule_noncritical(client, duty_data):
    factories.ScheduleFactory(
        service=duty_data.service,
    )
    schedule = factories.ScheduleFactory(
        service=duty_data.service,
        start_date='2019-01-01',
        role=duty_data.role,
        name='name1',
    )
    factories.ShiftFactory(schedule=schedule, state=Shift.STARTED)
    factories.ShiftFactory(schedule=schedule, state=Shift.SCHEDULED)
    client.login(duty_data.owner.login)
    with mock.patch('plan.duty.tasks.DutyScheduler.recalculate_shifts') as recalculate:
        response = client.json.patch(
            reverse('api-v3:duty-schedule-detail', args=[schedule.pk]),
            data={
                'name': 'name2'
            }
        )
        assert response.status_code == 200
        assert recalculate.call_count == 1
        assert schedule.shifts.count() == 2
        schedule.refresh_from_db()
        assert schedule.name == 'name2'
        recalculate.assert_called_once_with(duty_data.service, [], None)


@freeze_time('2018-01-01')
@pytest.mark.parametrize('start_with_staff', list(range(3)))
@pytest.mark.parametrize('api', ('api-v3', 'api-v4'))
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=15))
def test_update_schedule_with_order_start_with(client, duty_data, start_with_staff, api, duty_role):
    """
    В этом тесте замокали период, на который создаются смены: вместо полгода - 15 дней (нужны 3 смены по 5 дней).
    Проверяем изменение порядка, start_with всегда нулевой элемент.
    """

    client.login(duty_data.owner.login)
    staff_1 = factories.StaffFactory()
    staff_2 = factories.StaffFactory()
    staff_3 = factories.StaffFactory()
    factories.ServiceMemberFactory(service=duty_data.service, staff=staff_1, role=duty_data.role)
    factories.ServiceMemberFactory(service=duty_data.service, staff=staff_2, role=duty_data.role)
    factories.ServiceMemberFactory(service=duty_data.service, staff=staff_3, role=duty_data.role)

    response = client.json.post(
        reverse(f'{api}:duty-schedule-list'),
        data={
            'name': 'name1',
            'role': duty_data.role.pk,
            'start_date': '2018-01-01',
            'service': duty_data.service.id,
            'algorithm': 'manual_order',
            'orders': [staff_1.login, staff_2.login],
            'duration': '3 00',
            'slug': 'mow',
        }
    )
    assert response.status_code == 201

    schedule = Schedule.objects.get()

    with mock.patch('plan.duty.models.Shift.find_or_create_role'):
        shift = schedule.shifts.order_by('start').first()
        shift.begin()
        shift.save()

    # поменяем порядок, старые orders должны были удалиться
    staffs = [staff_1.login, staff_2.login, staff_3.login]
    new_staffs = staffs[start_with_staff:]
    s = 0
    while s < start_with_staff:
        new_staffs.append(staffs[s])
        s += 1

    response = client.json.patch(
        reverse('api-v3:duty-schedule-detail', args=[schedule.pk]),
        data={
            'orders': new_staffs,
        }
    )

    assert response.status_code == 200
    schedule.refresh_from_db()
    assert schedule.shifts.order_by('start')[1].staff.login == staffs[start_with_staff]


@freeze_time('2019-01-01')
def test_delete_schedule(client, duty_data):
    schedule = factories.ScheduleFactory(service=duty_data.service)

    client.login(duty_data.owner.login)
    with mock.patch('plan.duty.tasks.DutyScheduler.recalculate_shifts'):
        response = client.json.delete(reverse('api-v3:duty-schedule-detail', args=[schedule.pk]))
        assert response.status_code == 204

    assert duty_data.service.schedules.deleted().count() == 1
    schedule.refresh_from_db()
    assert schedule.status == Schedule.DELETED
    assert schedule.deleted_at == timezone.now()

    with freeze_time('2019-01-10'):
        remove_inactive_schedules()
        assert duty_data.service.schedules.count() == 0


@freeze_time('2019-01-01')
def test_delete_schedule_recalculate(client, duty_data):
    associated_schedule = factories.ScheduleFactory(
        service=duty_data.service,
    )
    schedule = factories.ScheduleFactory(
        service=duty_data.service,
        start_date='2019-01-01',
        role=duty_data.role,
        name='name1',
    )
    client.login(duty_data.owner.login)
    with mock.patch('plan.duty.tasks.DutyScheduler.recalculate_shifts') as recalculate,\
            mock.patch('plan.duty.tasks.remove_inactive_schedules') as remove_inactive:
        response = client.json.delete(reverse('api-v3:duty-schedule-detail', args=[schedule.pk]))
        assert response.status_code == 204
        assert recalculate.call_count == 1
        assert remove_inactive.apply_async.call_count == 1
        recalculate.assert_called_once_with(duty_data.service, [associated_schedule.id], None)
    assert duty_data.service.schedules.active().count() == 1
    assert duty_data.service.schedules.deleted().count() == 1
    schedule.refresh_from_db()
    assert schedule.status == Schedule.DELETED


@freeze_time('2019-01-01')
def test_check_delete_schedule(client, duty_data):
    client.login(duty_data.owner.login)

    def check_delete(*schedules):
        url = reverse('api-v4:duty-schedule-check-changes')
        data = [
            {
                'id': schedule.id,
                'status': 'deleted',
            }
            for schedule in schedules
        ]
        return client.json.post(url, data)

    irrelevant_schedule = factories.ScheduleFactory(
        service=duty_data.service,
        consider_other_schedules=False,
    )
    recalculate_duty_for_service(duty_data.service.id)

    response = check_delete(irrelevant_schedule)
    assert response.status_code == 200
    assert response.json() == {'conditions': [], 'fields': [], 'consequence': None}

    schedule = factories.ScheduleFactory(
        service=duty_data.service,
        start_date='2019-01-01',
        role=duty_data.role,
        name='name1',
        consider_other_schedules=True,
    )
    associated_schedule = factories.ScheduleFactory(
        service=duty_data.service,
        consider_other_schedules=True,
    )
    recalculate_duty_for_service(duty_data.service.id)

    response = check_delete(schedule)
    assert response.status_code == 200
    assert response.json() == {'conditions': ['schedule'], 'fields': [], 'consequence': 'soft'}

    response = check_delete(associated_schedule)
    assert response.status_code == 200
    assert response.json() == {'conditions': ['schedule'], 'fields': [], 'consequence': 'soft'}

    response = check_delete(schedule, associated_schedule)
    assert response.status_code == 200
    assert response.json() == {'conditions': [], 'fields': [], 'consequence': None}

    response = check_delete(schedule, associated_schedule, irrelevant_schedule)
    assert response.status_code == 200
    assert response.json() == {'conditions': [], 'fields': [], 'consequence': None}


@freeze_time('2019-01-01')
def test_create_non_associated_schedule(client, duty_data, duty_role):
    factories.ScheduleFactory(
        service=duty_data.service,
    )
    client.login(duty_data.owner.login)
    with mock.patch('plan.duty.tasks.DutyScheduler.recalculate_shifts') as recalculate:
        response = client.json.post(
            reverse('api-v3:duty-schedule-list'),
            data={
                'name': 'name1',
                'role': duty_data.role.id,
                'service': duty_data.service.id,
                'start_date': '2019-01-01',
                'duration': '3 00',
                'consider_other_schedules': False,
                'slug': '123',
                'days_for_problem_notification': 5,
                'days_for_begin_shift_notification': [0, 3],
            }
        )
        assert response.status_code == 201
        assert recalculate.call_count == 1
        schedule = duty_data.service.schedules.order_by('id').last()
        assert schedule.id == response.json()['id']
        assert schedule.role == duty_data.role
        assert schedule.start_date == datetime.date(2019, 1, 1)
        assert schedule.name == 'name1'
        assert schedule.duration == timezone.timedelta(days=3)
        assert schedule.days_for_problem_notification == 5
        assert schedule.days_for_begin_shift_notification == [0, 3]
        recalculate.assert_called_once_with(duty_data.service, [], None)


@freeze_time('2019-01-01')
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=15))
@pytest.mark.parametrize('api', ('api-v3', 'api-v4'))
@pytest.mark.parametrize('start_with_num', [0, 1, 2])
def test_create_schedule_with_manual_order(client, duty_data, duty_role, api, start_with_num):
    """
    В этом тесте замокали период, на который создаются смены: вместо полгода - 15 дней (нужны 3 смены по 5 дней).

    Успешное создание расписания, который должен иметь порядок.
    start_with_num - не имеет отношения к парамтеру start_with при апдейте порядка.
    """

    factories.ScheduleFactory(
        service=duty_data.service,
    )
    client.login(duty_data.owner.login)
    staffs = []
    for _ in range(3):
        mem = factories.ServiceMemberFactory(service=duty_data.service, role=duty_data.role)
        staffs.append(mem.staff.login)

    response = client.json.post(
        reverse(f'{api}:duty-schedule-list'),
        data={
            'name': 'name1',
            'role': duty_data.role.id,
            'service': duty_data.service.id,
            'start_date': '2019-01-01',
            'algorithm': 'manual_order',
            'orders': staffs,
            'duration': '3 00',
            'slug': '123',
        }
    )
    assert response.status_code == 201
    schedule = duty_data.service.schedules.order_by('id').last()
    assert list(schedule.orders.order_by('order').values_list('staff__login', flat=True)) == staffs
    assert schedule.id == response.json()['id']
    assert schedule.orders


@freeze_time('2018-01-01')
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=15))
@pytest.mark.parametrize('api', ('api-v3', 'api-v4'))
def test_create_manual_order_start_with_fail(client, duty_data, api):
    """
    В этом тесте замокали период, на который создаются смены: вместо полгода - 15 дней (нужны 3 смены по 5 дней).

    Нельзя создать график, если start_with не в orders
    """
    client.login(duty_data.owner.login)
    staff_1 = factories.StaffFactory()
    staff_2 = factories.StaffFactory()
    staff_3 = factories.StaffFactory()
    factories.ServiceMemberFactory(service=duty_data.service, staff=staff_1, role=duty_data.role)
    factories.ServiceMemberFactory(service=duty_data.service, staff=staff_2, role=duty_data.role)
    factories.ServiceMemberFactory(service=duty_data.service, staff=staff_3, role=duty_data.role)

    response = client.json.post(
        reverse(f'{api}:duty-schedule-list'),
        data={
            'name': 'name1',
            'role': duty_data.role.id,
            'service': duty_data.service.id,
            'start_date': '2019-01-01',
            'algorithm': 'manual_order',
            'orders': [staff_1.login, staff_2.login],
            'start_with': staff_3.login,
            'duration': '3 00',
        }
    )

    assert response.status_code == 400


@freeze_time('2020-10-01T19:15:00')
@pytest.mark.parametrize('time_start', [
    '23:00',    # смена ещё не началась
    '12:00',    # смена уже началась
    '00:00',    # стандартное время
])
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=15))
def test_update_manual_order(client, duty_data, time_start):
    """
    В этом тесте замокали период, на который создаются смены: вместо полгода - 15 дней (нужны 3 смены по 5 дней).

    Меняем обычный алгоритм на алгоритм с порядком.
    Важно: время начала смены, был баг для смен, которые ещё не начались по времени, но начались по дню.
    """

    staffs = []
    for _ in range(3):
        mem = factories.ServiceMemberFactory(service=duty_data.service, role=duty_data.role)
        staffs.append(mem.staff.login)

    schedule = factories.ScheduleFactory(
        service=duty_data.service,
        role=duty_data.role,
        start_date=timezone.now().date(),
        start_time=time_start,
    )

    client.login(duty_data.owner.login)

    recalculate_duty_for_service(duty_data.service.id)

    response = client.json.patch(
        reverse('api-v3:duty-schedule-detail', args=[schedule.pk]),
        data={
            'algorithm': 'manual_order',
            'orders': staffs,
            'autoapprove_timedelta': '5 00',
        }
    )
    assert response.status_code == 200

    shift = schedule.shifts.order_by('start_datetime').first()
    assert shift.start == schedule.start_date

    # вызываем ещё один пересчет, который удаляет первую смену, хотя не должен
    recalculate_duty_for_service(duty_data.service.id)

    assert list(schedule.orders.order_by('order').values_list('staff__login', flat=True)) == staffs
    assert schedule.manual_ordering_offset == 0
    assert schedule.id == response.json()['id']

    shift = schedule.shifts.order_by('start_datetime').first()
    assert shift.start == schedule.start_date


def test_update_manual_order_wo_critical_fields(client, duty_data):
    """
    Проверяем что если в ручном порядке сделать замены и потом поменять "не критические поля"
    имя, например, замены не будут удалены
    """
    staff_1 = factories.StaffFactory()
    schedule = factories.ScheduleFactory(
        algorithm=Schedule.MANUAL_ORDER,
        service=duty_data.service,
        role=duty_data.role,
        start_date=timezone.now().date(),
        start_time=timezone.now(),
    )
    factories.OrderFactory(
        order=1,
        schedule=schedule,
        staff=staff_1,
    )

    client.login(duty_data.owner.login)

    recalculate_duty_for_service(duty_data.service.id)

    shift = schedule.shifts.last()

    staff = factories.StaffFactory()
    shift.staff = staff
    shift.is_approved = True
    shift.save()
    with mock.patch('plan.duty.api.serializers.priority_recalculate_shifts_for_service') as mock_recalculate:
        response = client.json.patch(
            reverse('api-v3:duty-schedule-detail', args=[schedule.pk]),
            data={
                'name': 'testme',
            }
        )
    assert response.status_code == 200
    mock_recalculate.apply_async_on_commit.assert_called_once_with(args=[schedule.service_id, []])

    shift.refresh_from_db()
    assert shift.staff == staff


@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=15))
def test_change_order_to_no_order(client, duty_data):
    schedule = factories.ScheduleFactory(
        service=duty_data.service,
        role=duty_data.role,
        start_date=timezone.now().date(),
        algorithm=Schedule.MANUAL_ORDER,
    )
    order = factories.OrderFactory(schedule=schedule, order=1)
    order.staff = None
    order.save()

    client.login(duty_data.owner.login)

    response = client.json.patch(
        reverse('api-v3:duty-schedule-detail', args=[schedule.pk]),
        data={
            'algorithm': Schedule.NO_ORDER,
        }
    )
    assert response.status_code == 200


@freeze_time('2019-09-23')
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=30))
def test_update_manual_order_with_replace(client, duty_data, duty_role):
    schedule = factories.ScheduleFactory(
        service=duty_data.service,
        role=duty_data.role,
        duration=timezone.timedelta(days=7),
        start_date='2019-08-23',
        algorithm=Schedule.NO_ORDER,
    )

    client.login(duty_data.owner.login)
    staffs = []

    with mock.patch('plan.duty.tasks.DutyScheduler.recalculate_shifts'):
        for _ in range(4):
            mem = factories.ServiceMemberFactory(service=duty_data.service, role=duty_data.role)
            staffs.append(mem.staff.login)

    recalculate_duty_for_service(duty_data.service.id)
    shift = schedule.shifts.current_shifts().first()

    replace_shift = factories.ShiftFactory(
        schedule=schedule,
        replace_for=shift,
        staff=shift.staff,
        start=shift.start,
        end=shift.end,
    )

    # делаем замену
    next_shift_staff = schedule.shifts.future().order_by('start').first().staff
    response = client.json.patch(
        reverse('api-v3:duty-shift-detail', args=[shift.id]),
        {'person': next_shift_staff.login}
    )
    assert response.status_code == 200

    shift.state = Shift.STARTED
    shift.save(update_fields=['state'])

    # меняем алгоритм на с заданным порядком
    response = client.json.patch(
        reverse('api-v3:duty-schedule-detail', args=[schedule.pk]),
        data={
            'algorithm': Schedule.MANUAL_ORDER,
            'orders': staffs,
        }
    )
    assert response.status_code == 200

    # поменяем порядок
    new_staffs = staffs[1:]
    new_staffs.append(staffs[0])
    response = client.json.patch(
        reverse('api-v3:duty-schedule-detail', args=[schedule.pk]),
        data={
            'algorithm': Schedule.MANUAL_ORDER,
            'orders': new_staffs,
        }
    )
    assert response.status_code == 200

    replace_shift.refresh_from_db()
    next_shift = schedule.shifts.future().order_by('start').first()
    assert next_shift.staff.login == new_staffs[0]
    assert replace_shift.index is None


@freeze_time('2019-01-01')
@pytest.mark.parametrize(
    ('language', 'error_text'),
    (('ru', 'Название %s не уникально'), ('en', 'Name %s is not unique'))
)
def test_create_schedule_with_same_name(client, duty_data, duty_role, language, error_text):
    factories.ScheduleFactory(
        service=duty_data.service,
    )
    client.login(duty_data.owner.login)
    staffs = []
    for _ in range(3):
        mem = factories.ServiceMemberFactory(service=duty_data.service, role=duty_data.role)
        staffs.append(mem.staff.login)
    data = {
        'name': 'name1',
        'role': duty_data.role.id,
        'service': duty_data.service.id,
        'start_date': '2019-01-01',
        'algorithm': 'manual_order',
        'orders': staffs,
        'start_with': staffs[1],
        'duration': '3 00',
        'slug': '123',
    }
    with mock.patch('plan.duty.tasks.DutyScheduler.recalculate_shifts'):
        response = client.json.post(reverse('api-v3:duty-schedule-list'), data=data)
        assert response.status_code == 201

        data['slug'] = '321'

        response = client.json.post(
            reverse('api-v3:duty-schedule-list'),
            data=data,
            **{
                'HTTP_ACCEPT_LANGUAGE': language
            }
        )
        assert response.status_code == 400
        assert response.json()['error']['extra']['name'] == error_text % 'name1'


@freeze_time('2019-01-01')
@pytest.mark.parametrize('who', ['owner', 'resp', 'other'])
def test_create_schedule_resp_role(client, responsible_for_duty_role, duty_data, who, staff_factory):
    responsible_for_duty_staff = factories.ServiceMemberFactory(
        service=duty_data.service,
        role=responsible_for_duty_role,
        staff=staff_factory()
    ).staff
    other_staff = factories.ServiceMemberFactory(service=duty_data.service, staff=staff_factory()).staff
    if who == 'owner':
        client.login(duty_data.owner.login)
    elif who == 'resp':
        client.login(responsible_for_duty_staff.login)
    else:
        client.login(other_staff.login)

    data = {
        'name': 'name1',
        'role': None,
        'service': duty_data.service.id,
        'start_date': '2019-01-01',
        'duration': '3 00',
        'slug': '123',
    }
    with mock.patch('plan.duty.tasks.DutyScheduler.recalculate_shifts'):
        response = client.json.post(reverse('api-v3:duty-schedule-list'), data=data)
        if who == 'other':
            assert response.status_code == 403
        else:
            assert response.status_code == 201


@pytest.mark.parametrize(
    ('language', 'error_text'),
    (
        ('ru', 'Роль %s не может быть выдана во время дежурства. Роль не относится к сервису'),
        ('en', 'Role %s can\'t be granted during duty. Role is not related to service')
    )
)
def test_create_schedule_with_custom_role_on_duty(client, duty_data, language, error_text):
    client.login(duty_data.owner.login)
    service = factories.ServiceFactory()

    role_on_duty = factories.RoleFactory(service=service)

    response = client.json.post(
        reverse('api-v3:duty-schedule-list'),
        data={
            'name': 'name1',
            'role': duty_data.role.id,
            'service': duty_data.service.id,
            'start_date': '2019-01-01',
            'duration': '3 00',
            'role_on_duty': role_on_duty.pk,
            'slug': '123',
        },
        **{
            'HTTP_ACCEPT_LANGUAGE': language
        }
    )

    assert response.status_code == 400
    assert response.json()['error']['extra']['role_on_duty'] == [error_text % role_on_duty]


@pytest.mark.parametrize(
    ('language', 'error_text'),
    (
        ('ru', 'Роль %s не может быть выдана во время дежурства. Роль не относится к сервису'),
        ('en', 'Role %s can\'t be granted during duty. Role is not related to service')
    )
)
def test_update_schedule_with_custom_role_on_duty(client, duty_data, language, error_text):
    client.login(duty_data.owner.login)
    service = factories.ServiceFactory()

    role_on_duty = factories.RoleFactory(service=duty_data.service)
    new_role_on_duty = factories.RoleFactory(service=service)

    schedule = factories.ScheduleFactory(
        service=duty_data.service,
        start_date='2019-01-01',
        role=duty_data.role,
        name='name1',
        role_on_duty=role_on_duty,
    )

    response = client.json.patch(
        reverse('api-v3:duty-schedule-detail', args=[schedule.pk]),
        data={
            'role_on_duty': new_role_on_duty.pk,
        },
        **{
            'HTTP_ACCEPT_LANGUAGE': language
        }
    )

    assert response.status_code == 400
    assert response.json()['error']['extra']['role_on_duty'] == [error_text % new_role_on_duty]


@freeze_time('2019-01-01')
@pytest.mark.parametrize(
    ('language', 'error_text'),
    (('ru', 'Слаг %s не уникален'), ('en', 'Slug %s is not unique'))
)
def test_create_schedule_double_slug(client, duty_data, duty_role, language, error_text):
    client.login(duty_data.owner.login)
    slug = '123'

    factories.ScheduleFactory(
        service=duty_data.service,
        slug=slug
    )

    with mock.patch('plan.duty.tasks.DutyScheduler.recalculate_shifts'):
        response = client.json.post(
            reverse('api-v3:duty-schedule-list'),
            data={
                'name': 'name1',
                'role': duty_data.role.id,
                'service': duty_data.service.id,
                'start_date': '2019-01-01',
                'duration': '3 00',
                'slug': slug
            },
            **{
                'HTTP_ACCEPT_LANGUAGE': language
            }
        )

        assert response.status_code == 400
        assert response.json()['error']['extra']['slug'] == error_text % slug


@freeze_time('2019-01-01')
def test_create_schedule_double_slug_watcher(client, duty_data, duty_role):
    client.login(duty_data.owner.login)
    slug = '123'

    with mock.patch('plan.duty.tasks.DutyScheduler.recalculate_shifts'):
        with mock.patch.object(WatcherClient, 'get_schedules') as watcher_response:
            watcher_response.return_value = [{'slug': slug}]
            response = client.json.post(
                reverse('api-v3:duty-schedule-list'),
                data={
                    'name': 'name1',
                    'role': duty_data.role.id,
                    'service': duty_data.service.id,
                    'start_date': '2019-01-01',
                    'duration': '3 00',
                    'slug': slug
                },
            )

        assert response.status_code == 400
        assert response.json()['error']['extra']['slug'] == f'Slug {slug} is not unique'


def test_create_schedule_long_slug(client, duty_data):
    client.login(duty_data.owner.login)
    slug = '1' * 999
    response = client.json.post(
        reverse('api-v3:duty-schedule-list'),
        data={
            'slug': slug,
            'service': duty_data.service.id,
        }
    )
    assert response.status_code == 400
    assert response.json()['error']['extra']['slug'] == [
        'Ensure this field has no more than %d characters.' % settings.MAX_SLUG_LENGTH
    ]


@freeze_time('2019-01-01')
@pytest.mark.parametrize(
    ('language', 'error_text'),
    (('ru', 'Слаг не может быть изменен'), ('en', 'Slug can\'t be changed'))
)
def test_update_schedule_slug(client, duty_data, language, error_text):
    client.login(duty_data.owner.login)

    schedule = factories.ScheduleFactory(
        service=duty_data.service,
        slug='123'
    )

    with mock.patch('plan.duty.tasks.DutyScheduler.recalculate_shifts'):
        response = client.json.patch(
            reverse('api-v3:duty-schedule-detail', args=[schedule.pk]),
            data={
                'slug': '321'
            },
            **{
                'HTTP_ACCEPT_LANGUAGE': language
            }
        )

        assert response.status_code == 400
        assert response.json()['error']['extra']['slug'] == [error_text]


@freeze_time('2019-01-01')
def test_patch_notifications_days(client, duty_data):
    client.login(duty_data.owner.login)

    schedule = factories.ScheduleFactory(
        service=duty_data.service,
        slug='123'
    )

    with mock.patch('plan.duty.tasks.DutyScheduler.recalculate_shifts'):
        response = client.json.patch(
            reverse('api-v3:duty-schedule-detail', args=[schedule.pk]),
            data={
                'days_for_problem_notification': 11,
                'days_for_begin_shift_notification': [0, 3],
            }
        )

        assert response.status_code == 200
    schedule.refresh_from_db()
    assert schedule.days_for_problem_notification == 11
    assert schedule.days_for_begin_shift_notification == [0, 3]


@freeze_time('2019-01-01')
def test_update_schedule_slug_2(client, duty_data):
    client.login(duty_data.owner.login)

    schedule = factories.ScheduleFactory(
        service=duty_data.service,
        slug='123'
    )

    with mock.patch('plan.duty.tasks.DutyScheduler.recalculate_shifts'):
        response = client.json.patch(
            reverse('api-v3:duty-schedule-detail', args=[schedule.pk]),
            data={
                'slug': '123'
            }
        )

        assert response.status_code == 200


@freeze_time('2019-01-01')
def test_clean_orders_after_change_algorithm(client, duty_data):
    client.login(duty_data.owner.login)

    schedule = factories.ScheduleFactory(
        service=duty_data.service,
        role=duty_data.role,
    )

    with mock.patch('plan.duty.tasks.DutyScheduler.recalculate_shifts'):
        staff1 = factories.ServiceMemberFactory(role=duty_data.role, service=duty_data.service).staff
        staff2 = factories.ServiceMemberFactory(role=duty_data.role, service=duty_data.service).staff

        client.json.patch(
            reverse('api-v3:duty-schedule-detail', args=[schedule.pk]),
            data={
                'algorithm': Schedule.MANUAL_ORDER,
                'orders': [staff1.login, staff2.login]
            }
        )
        schedule.refresh_from_db()
        assert schedule.orders.count() == 2

        client.json.patch(
            reverse('api-v3:duty-schedule-detail', args=[schedule.pk]),
            data={
                'algorithm': Schedule.NO_ORDER,
            }
        )
        schedule.refresh_from_db()
        assert schedule.orders.count() == 0


@freeze_time('2019-01-30')
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=15))
def test_deep_recalculate_duty_on_change(client, duty_data):
    role1 = factories.RoleFactory()
    role2 = factories.RoleFactory()
    schedule1 = factories.ScheduleFactory(
        role=role1,
        service=duty_data.service,
        start_date='2019-02-01',
        autoapprove_timedelta=timezone.timedelta(0),
    )
    schedule2 = factories.ScheduleFactory(
        role=role2,
        service=duty_data.service,
        start_date='2019-02-01',
        autoapprove_timedelta=timezone.timedelta(0),
    )

    staffs = []
    for _ in range(3):
        staff = factories.StaffFactory()
        factories.ServiceMemberFactory(service=duty_data.service, staff=staff, role=role1)
        staffs.append(staff)

    factories.ServiceMemberFactory(service=duty_data.service, staff=staffs[0], role=role2)

    recalculate_duty_for_service(duty_data.service.id)

    shift_count = 5
    found_staffs_in_schedule1 = {shift.staff for shift in schedule1.shifts.order_by('start')[:shift_count]}
    assert found_staffs_in_schedule1 == set(staffs[1:])
    found_staffs_in_schedule2 = {shift.staff for shift in schedule2.shifts.order_by('start')[:shift_count]}
    assert found_staffs_in_schedule2 == {staffs[0]}

    client.login(duty_data.owner.login)
    response = client.json.patch(
        reverse('api-v3:duty-schedule-detail', args=[schedule1.pk]),
        data={
            'id': schedule1.id,
            'name': schedule1.name,
            'role': schedule1.role.pk,
            'consider_other_schedules': False,
        },
    )

    assert response.status_code == 200

    found_staffs_in_schedule1 = {shift.staff for shift in schedule1.shifts.order_by('start')[:shift_count]}
    assert found_staffs_in_schedule1 == set(staffs)
    found_staffs_in_schedule2 = {shift.staff for shift in schedule2.shifts.order_by('start')[:shift_count]}

    assert found_staffs_in_schedule2 == {staffs[0]}


@freeze_time('2019-01-01')
def test_create_schedule_no_role(client, duty_data, duty_role):
    """duty-schedule-list корректно создает график с пустыми значениями ролей"""
    client.login(duty_data.owner.login)

    assert Schedule.objects.count() == 0

    with mock.patch('plan.duty.tasks.DutyScheduler.recalculate_shifts') as recalculate:
        response = client.json.post(
            reverse('api-v3:duty-schedule-list'),
            data={
                'service': duty_data.service.pk,
                'name': 'name1',
                'duration': '3 00',
                'role': None,
                'role_on_duty': None,
                'start_date': '2019-01-01',
                'algorithm': 'no_order',
                'slug': '123'
            }
        )
        assert response.status_code == 201
        assert recalculate.call_count == 1
        schedule = Schedule.objects.get()
        recalculate.assert_called_once_with(duty_data.service, [schedule.id], None)


def test_create_schedule_with_empty_order(client, duty_data):
    """
    Создание графика, который должен иметь порядок.
    Однако список людей передаём пустой.
    """

    client.login(duty_data.owner.login)

    response = client.json.post(
        reverse('api-v3:duty-schedule-list'),
        data={
            'name': 'name1',
            'role': duty_data.role.id,
            'service': duty_data.service.id,
            'start_date': '2019-01-01',
            'duration': '3 00',
            'algorithm': 'manual_order',
            'orders': [],
        }
    )

    assert response.status_code == 400
    assert response.json()['error']['code'] == 'invalid'


def test_update_schedule_name(client, duty_data):
    role = factories.RoleFactory()
    schedule = factories.ScheduleFactory(service=duty_data.service, role=role)
    client.login(duty_data.owner.login)

    response = client.json.patch(
        reverse('api-v3:duty-schedule-detail', args=[schedule.pk]),
        data={
            'id': schedule.pk,
            'name': 'name1',
            'role': role.pk,
        }
    )
    assert response.status_code == 200
    schedule.refresh_from_db()

    assert schedule.name == 'name1'


def test_update_schedule_with_empty_order(client, duty_data):
    """
    Апдейт календаря: установка у графика алгоритма, который должен иметь порядок.
    Однако список был передан пустой.
    """

    schedule = factories.ScheduleFactory(service=duty_data.service, algorithm=Schedule.MANUAL_ORDER)

    client.login(duty_data.owner.login)
    staff_1 = factories.StaffFactory()
    staff_2 = factories.StaffFactory()
    factories.ServiceMemberFactory(service=duty_data.service, staff=staff_1, role=duty_data.role)
    factories.ServiceMemberFactory(service=duty_data.service, staff=staff_2, role=duty_data.role)

    base_data = {
        'id': schedule.pk,
        'name': schedule.name,
        'role': duty_data.role.pk,
        'orders': []
    }

    response = client.json.patch(
        reverse('api-v3:duty-schedule-detail', args=[schedule.pk]),
        data=base_data,
    )

    assert response.status_code == 400
    assert response.json()['error']['code'] == 'validation_error'

    schedule.algorithm = Schedule.NO_ORDER
    schedule.save(update_fields=['algorithm'])

    response = client.json.patch(
        reverse('api-v3:duty-schedule-detail', args=[schedule.pk]),
        data={**base_data, 'algorithm': Schedule.MANUAL_ORDER},
    )

    assert response.status_code == 400
    assert response.json()['error']['code'] == 'validation_error'

    base_data.pop('orders')

    response = client.json.patch(
        reverse('api-v3:duty-schedule-detail', args=[schedule.pk]),
        data={**base_data, 'algorithm': Schedule.MANUAL_ORDER},
    )

    assert response.status_code == 400
    assert response.json()['error']['code'] == 'validation_error'


def test_update_schedule_with_order_no_role(client, duty_data, duty_role):
    """
    Апдейт календаря: установка у тега алгоритма, который должен иметь порядок.
    В списке передали логин пользователя, который не имеет роли, указанной в теге дежурства.
    """

    schedule = factories.ScheduleFactory(service=duty_data.service)

    client.login(duty_data.owner.login)
    staff_1 = factories.StaffFactory()
    staff_2 = factories.StaffFactory()
    factories.ServiceMemberFactory(service=duty_data.service, staff=staff_1, role=duty_data.role)

    response = client.json.patch(
        reverse('api-v3:duty-schedule-detail', args=[schedule.pk]),
        data={
            'id': schedule.pk,
            'name': schedule.name,
            'role': duty_data.role.pk,
            'algorithm': 'manual_order',
            'orders': [staff_1.login, staff_2.login]
        }
    )

    assert response.status_code == 400
    assert response.json()['error']['code'] == 'validation_error'


@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=15))
def test_update_schedule_sequential_shifts(client, duty_data, duty_role):
    schedule = factories.ScheduleFactory(
        service=duty_data.service,
        role=duty_data.role,
        allow_sequential_shifts=True,
        persons_count=2,
        start_date=utils.now().date() + timezone.timedelta(days=1),
    )

    client.login(duty_data.owner.login)
    staff_1 = factories.StaffFactory()
    staff_2 = factories.StaffFactory()
    factories.ServiceMemberFactory(service=duty_data.service, staff=staff_1, role=duty_data.role)
    factories.ServiceMemberFactory(service=duty_data.service, staff=staff_2, role=duty_data.role)

    recalculate_duty_for_service(service_id=duty_data.service.id)

    assert all(shift.staff is not None for shift in schedule.shifts.all())

    response = client.json.patch(
        reverse('api-v3:duty-schedule-detail', args=[schedule.pk]),
        data={'allow_sequential_shifts': False},
    )
    assert response.status_code == 200

    assert all(shift.staff is not None for shift in schedule.shifts.order_by('start')[:2])
    assert all(shift.staff is None for shift in schedule.shifts.order_by('start')[2:4])


@pytest.mark.parametrize('api', ('api-v3', 'api-v4'))
def test_update_schedule_with_order_login_repeated(client, duty_data, api):
    """
    Апдейт календаря: установка у тега алгоритма, который должен иметь порядок.
    В списке передали логин пользователя несколько раз.
    """

    staff_1 = factories.StaffFactory()
    factories.ServiceMemberFactory(service=duty_data.service, staff=staff_1, role=duty_data.role)
    schedule = factories.ScheduleFactory(service=duty_data.service)

    client.login(duty_data.owner.login)
    response = client.json.patch(
        reverse(f'{api}:duty-schedule-detail', args=[schedule.pk]),
        data={
            'id': schedule.pk,
            'name': schedule.name,
            'role': duty_data.role.pk,
            'algorithm': 'manual_order',
            'orders': [staff_1.login, staff_1.login]
        }
    )

    assert response.status_code == 400
    assert response.json()['error']['code'] == 'validation_error'


@freeze_time('2019-05-11')
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=15))
@pytest.mark.parametrize('api', ('api-v3', 'api-v4'))
@pytest.mark.parametrize('start_with, expected_offset', ((True, 3), (False, 2)))
def test_update_schedule_manual_order(client, duty_data, api, start_with, expected_offset):
    """
    Проверим, что у графика сохранится сдвиг,
    и следующее дежурство действительно начнется с нулевого элемента или со start_with (если передан) в списке.
    """

    client.login(duty_data.owner.login)

    role = factories.RoleFactory()
    factories.ServiceMemberFactory(service=duty_data.service, role=role)
    staff_b = factories.ServiceMemberFactory(service=duty_data.service, role=role).staff
    staff_c = factories.ServiceMemberFactory(service=duty_data.service, role=role).staff
    staff_d = factories.ServiceMemberFactory(service=duty_data.service, role=role).staff
    staff_e = factories.ServiceMemberFactory(service=duty_data.service, role=role).staff

    schedule = factories.ScheduleFactory(
        service=duty_data.service,
        role=role,
        algorithm=Schedule.NO_ORDER,
        start_date='2019-04-30',
        persons_count=2,
    )

    recalculate_duty_for_service(schedule.service.id)

    current_shifts = schedule.shifts.current_shifts()
    current_shifts_staffs = current_shifts.values_list('staff__login', flat=True)

    # поменяем расписанию алгоритм и зададим порядок
    data = {
        'algorithm': Schedule.MANUAL_ORDER,
        'orders': [staff_e.login, staff_d.login, staff_c.login, staff_b.login]
    }

    if start_with:
        data['orders'] = [staff_d.login, staff_e.login, staff_c.login, staff_b.login]
        data['start_with'] = staff_e.login

    response = client.json.patch(
        reverse(f'{api}:duty-schedule-detail', args=[schedule.pk]),
        data
    )
    assert response.status_code == 200

    next_shift = schedule.shifts.get(index=6)
    assert next_shift.staff.login == staff_e.login
    assert set(schedule.shifts.current_shifts().values_list('staff__login', flat=True)) == set(current_shifts_staffs)

    schedule.refresh_from_db()
    assert schedule.algorithm == Schedule.MANUAL_ORDER
    assert schedule.manual_ordering_offset == expected_offset


@freeze_time('2019-05-11')
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=15))
def test_update_schedule_not_started_shift(duty_data, client):
    """
    Увеличили одновременное кол-во смен, затем уменьшили, хатем снова увеличили.
    Итог не должен изменится от первоначального: финальная проверка на дату (2019, 5, 23)
    Смены:: 1) 30.04 - 02.05
            2) 03.05 - 05.05
            3) 06.05 - 08.05
            4) 09.05 - 11.05 -- это текущая смена
            5) 12.05 - 15.05
            6) 16.05 - 19.05
            7) 20.05 - 23.05

    """
    schedule = factories.ScheduleFactory(
        service=duty_data.service,
        role=duty_data.role,
        algorithm=Schedule.NO_ORDER,
        start_date='2019-04-30',
        persons_count=1,
        duration=timezone.timedelta(days=3),
    )

    for _ in range(3):
        factories.ServiceMemberFactory(role=duty_data.role, service=duty_data.service)

    recalculate_duty_for_service(schedule.service.id)
    client.login(duty_data.owner.login)

    # поменяли длину дежурства
    response = client.json.patch(
        reverse('api-v3:duty-schedule-detail', args=[schedule.pk]),
        data={
            'duration': '4 00'
        }
    )
    assert response.status_code == 200
    schedule.refresh_from_db()

    # поменяли число одновременных смен
    response = client.json.patch(
        reverse('api-v3:duty-schedule-detail', args=[schedule.pk]),
        data={
            'persons_count': 2
        }
    )
    assert response.status_code == 200

    # вернули число одновременных смен
    response = client.json.patch(
        reverse('api-v3:duty-schedule-detail', args=[schedule.pk]),
        data={
            'persons_count': 1
        }
    )
    assert response.status_code == 200

    shifts = list(schedule.shifts.order_by('end')[:10])
    assert shifts[6].end == datetime.date(2019, 5, 23)


@pytest.mark.parametrize('api', ('v3', 'v4'))
def test_check_changes_add_new_schedule(client, api):
    service = factories.ServiceFactory()
    factories.ScheduleFactory(consider_other_schedules=True, service=service)
    role = factories.RoleFactory()
    url = reverse(f'api-{api}:duty-schedule-check-changes')
    data = [{
        'name': 'name1',
        'role': role.id,
        'service': service.id,
        'start_date': '2019-01-01',
        'duration': '3 00',
        'slug': 'some_test_slug'
    }]

    response = client.json.post(url, data)
    assert response.status_code == 200
    assert response.json() == {'conditions': ['schedule'], 'fields': [], 'consequence': 'soft'}


@pytest.mark.parametrize('api', ['api-v3', 'api-v4'])
@pytest.mark.parametrize('duty_on_holidays', [True, False])
@pytest.mark.parametrize('duty_on_weekends', [True, False])
def test_check_changes_critical_fields_one_schedule(client, api, duty_on_holidays, duty_on_weekends):
    """
    Проверяем ручку прежупреждений.
    Передаем такие же параметры, как и были, ничего не поменялось. Предупреждения быть не должно.
    """
    schedule = factories.ScheduleFactory(
        duty_on_holidays=duty_on_holidays,
        duty_on_weekends=duty_on_weekends,
    )
    url = reverse(f'{api}:duty-schedule-check-changes')
    data = [
        {
            'id': schedule.id,
            'duty_on_holidays': duty_on_holidays,
            'duty_on_weekends': duty_on_weekends,
        }
    ]
    response = client.json.post(url, data)
    assert response.status_code == 200
    assert response.json() == {'conditions': [], 'fields': [], 'consequence': None}


@pytest.mark.parametrize('api', ('api-v3', 'api-v4'))
def test_check_changes_critical_fields_two_dependent_schedule(client, api,):
    """
    Проверяем ручку прежупреждений.
    Изменили критическое поле для зависимых графиков, выводим сообщение.
    """
    service = factories.ServiceFactory()
    factories.ScheduleFactory(duty_on_holidays=False, consider_other_schedules=True, service=service)
    schedule2 = factories.ScheduleFactory(duty_on_holidays=False, consider_other_schedules=True, service=service)
    url = reverse(f'{api}:duty-schedule-check-changes')
    data = [{'id': schedule2.id, 'duty_on_holidays': True}]
    response = client.json.post(url, data)

    assert response.status_code == 200
    assert response.json() == {'conditions': ['fields'], 'fields': ['duty_on_holidays'], 'consequence': 'hard'}


@pytest.mark.parametrize('api', ('api-v3', 'api-v4'))
def test_check_changes_critical_fields_two_independent_schedule(client, api):
    """
    Проверяем ручку прежупреждений.
    Изменили критическое поле для НЕзависимых графиков, сообщение не выводим.
    """
    service = factories.ServiceFactory()
    factories.ScheduleFactory(duty_on_weekends=False, consider_other_schedules=False, service=service)
    schedule2 = factories.ScheduleFactory(duty_on_weekends=False, consider_other_schedules=False, service=service)
    url = reverse(f'{api}:duty-schedule-check-changes')
    data = [{'id': schedule2.id, 'duty_on_weekends': True}]
    response = client.json.post(url, data)

    assert response.status_code == 200
    assert response.json() == {'conditions': [], 'fields': [], 'consequence': None}


@pytest.mark.parametrize('api', ('v3', 'v4'))
def test_check_changes_increase_persons_count_two_schedule(client, api):
    service = factories.ServiceFactory()
    factories.ScheduleFactory(consider_other_schedules=True, service=service)
    schedule2 = factories.ScheduleFactory(consider_other_schedules=True, service=service, persons_count=2)
    url = reverse(f'api-{api}:duty-schedule-check-changes')
    data = [{'id': schedule2.id, 'persons_count': 3}]
    response = client.json.post(url, data)

    assert response.status_code == 200
    assert response.json() == {'conditions': ['fields'], 'fields': ['persons_count'], 'consequence': 'soft'}


@pytest.mark.parametrize('api', ('v3', 'v4'))
def test_check_changes_consider_other_schedules_two_schedule(client, api):
    service = factories.ServiceFactory()
    factories.ScheduleFactory(consider_other_schedules=True, service=service)
    schedule2 = factories.ScheduleFactory(consider_other_schedules=True, service=service)
    url = reverse(f'api-{api}:duty-schedule-check-changes')
    data = [{'id': schedule2.id, 'consider_other_schedules': False}]
    response = client.json.post(url, data)

    assert response.status_code == 200
    assert response.json() == {'conditions': ['fields'], 'fields': ['consider_other_schedules'], 'consequence': 'soft'}


@pytest.mark.parametrize('api', ('api-v3', 'api-v4'))
def test_check_changes_critical_fields_and_add_new_schedule(client, api):
    """
    Проверяем ручку прежупреждений.
    Изменили критическое поле и добавили новый график.
    """
    role = factories.RoleFactory()
    service = factories.ServiceFactory()
    factories.ScheduleFactory(duty_on_weekends=True, consider_other_schedules=True, service=service)
    schedule2 = factories.ScheduleFactory(duty_on_holidays=True, consider_other_schedules=True, service=service)
    url = reverse(f'{api}:duty-schedule-check-changes')
    data = [
        {
            'name': 'name1',
            'role': role.id,
            'service': service.id,
            'start_date': '2019-01-01',
            'duration': '3 00',
            'slug': 'some_test_slug'
        },
        {
            'id': schedule2.id,
            'duty_on_holidays': False,
        },
    ]
    response = client.json.post(url, data)

    assert response.status_code == 200
    data = response.json()
    assert sorted(data['conditions']) == ['fields', 'schedule']
    assert data['fields'] == ['duty_on_holidays']
    assert data['consequence'] == 'hard'


@override_settings(DEFAULT_TIMEZONE=pytz.timezone('Europe/Moscow'))
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=15))
@freeze_time('2019-01-08T00:00:00')
@pytest.mark.parametrize('order', (a[0] for a in Schedule.ALGORITHM))
@pytest.mark.parametrize('duty_on_weekends', [True, False])
def test_increase_schedule_start_time(client, duty_data, order, duty_on_weekends):
    """
    Проверим, что после изменения времени передачи дежурства (с дефолтного 00:00 на другое)  через api
    обновятся значения start_datetime и end_datetime у смен.
    Проверим только два варианта: либо дежурим по праздникам и выходным, либо нет.
    """

    # по дефолту прожолжительность = 5 дней
    schedule = factories.ScheduleFactory(
        service=duty_data.service,
        start_date='2019-01-07',
        start_time='00:00:00+0',
        role=duty_data.role,
        name='name1',
        algorithm=order,
        duty_on_weekends=duty_on_weekends,
        duty_on_holidays=duty_on_weekends,
    )
    recalculate_duty_for_service(schedule.service.id)

    shifts = schedule.shifts.order_by('start_datetime')
    started_shift = shifts[0]
    started_shift.state = Shift.STARTED
    started_shift.save()

    client.login(duty_data.owner.login)
    url = reverse('api-v3:duty-schedule-detail', args=[schedule.pk])
    response = client.json.patch(url, data={'start_time': '12:00'})
    assert response.status_code == 200

    # У стартовавшего дежурства изменится время конца смены
    started_shift.refresh_from_db()

    shift = shifts[0]
    if not duty_on_weekends:
        assert (
            shift.start_datetime.astimezone(settings.DEFAULT_TIMEZONE) ==
            datetime.datetime(2019, 1, 7, 0, 0).astimezone(settings.DEFAULT_TIMEZONE)
        )
        assert shift.start == datetime.datetime(2019, 1, 7).astimezone(settings.DEFAULT_TIMEZONE).date()
        assert (
            shift.end_datetime.astimezone(settings.DEFAULT_TIMEZONE) ==
            datetime.datetime(2019, 1, 14, 12, 0).astimezone(settings.DEFAULT_TIMEZONE)
        )
        assert shift.end == datetime.datetime(2019, 1, 14).astimezone(settings.DEFAULT_TIMEZONE).date()

    else:
        assert (
            shift.start_datetime.astimezone(settings.DEFAULT_TIMEZONE) ==
            datetime.datetime(2019, 1, 7, 0, 0).astimezone(settings.DEFAULT_TIMEZONE)
        )
        assert shift.start == datetime.datetime(2019, 1, 7, 0, 0).astimezone(settings.DEFAULT_TIMEZONE).date()

        assert (
            shift.end_datetime.astimezone(settings.DEFAULT_TIMEZONE) ==
            datetime.datetime(2019, 1, 12, 12, 0).astimezone(settings.DEFAULT_TIMEZONE)
        )
        assert shift.end == datetime.datetime(2019, 1, 12, 12, 0).astimezone(settings.DEFAULT_TIMEZONE).date()

    shift = shifts[1]
    if not duty_on_weekends:
        assert (
            shift.start_datetime.astimezone(settings.DEFAULT_TIMEZONE) ==
            datetime.datetime(2019, 1, 14, 12, 0).astimezone(settings.DEFAULT_TIMEZONE)
        )
        assert shift.start == datetime.datetime(2019, 1, 14, 12, 0).astimezone(settings.DEFAULT_TIMEZONE).date()

        assert (
            shift.end_datetime.astimezone(settings.DEFAULT_TIMEZONE) ==
            datetime.datetime(2019, 1, 21, 12, 0).astimezone(settings.DEFAULT_TIMEZONE)
        )
        assert shift.end == datetime.datetime(2019, 1, 21, 12, 0).astimezone(settings.DEFAULT_TIMEZONE).date()

    else:
        assert (
            shift.start_datetime.astimezone(settings.DEFAULT_TIMEZONE) ==
            datetime.datetime(2019, 1, 12, 12, 0).astimezone(settings.DEFAULT_TIMEZONE)
        )
        assert shift.start == datetime.datetime(2019, 1, 12, 12, 0).astimezone(settings.DEFAULT_TIMEZONE).date()

        assert (
            shift.end_datetime.astimezone(settings.DEFAULT_TIMEZONE) ==
            datetime.datetime(2019, 1, 17, 12, 0).astimezone(settings.DEFAULT_TIMEZONE)
        )
        assert shift.end == datetime.datetime(2019, 1, 17, 12, 0).astimezone(settings.DEFAULT_TIMEZONE).date()


@override_settings(DEFAULT_TIMEZONE=pytz.timezone('Europe/Moscow'))
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=15))
@freeze_time('2019-01-08T00:00:00')
@pytest.mark.parametrize('order', (a[0] for a in Schedule.ALGORITHM))
@pytest.mark.parametrize('duty_on_weekends', [True, False])
def test_decrease_schedule_start_time(client, duty_data, order, duty_on_weekends):
    """
    Проверим, что после изменения времени передачи дежурства (с кастомного на дефолтное 00:00) через api
    обновятся значения start_datetime и end_datetime у смен.
    Проверим только два варианта: либо дежурим по праздникам и выходным, либо нет.
    """

    # по дефолту прожолжительность = 5 дней
    schedule = factories.ScheduleFactory(
        service=duty_data.service,
        start_date='2019-01-07',
        start_time='00:00:00+0',
        role=duty_data.role,
        name='name1',
        algorithm=order,
        duty_on_weekends=duty_on_weekends,
        duty_on_holidays=duty_on_weekends,
    )
    recalculate_duty_for_service(schedule.service.id)

    shifts = schedule.shifts.order_by('start_datetime')
    started_shift = shifts[0]
    started_shift.state = Shift.STARTED
    started_shift.save()

    client.login(duty_data.owner.login)
    url = reverse('api-v3:duty-schedule-detail', args=[schedule.pk])
    response = client.json.patch(url, data={'start_time': '12:00'})
    assert response.status_code == 200

    client.login(duty_data.owner.login)
    url = reverse('api-v3:duty-schedule-detail', args=[schedule.pk])
    response = client.json.patch(url, data={'start_time': '00:00'})
    assert response.status_code == 200

    # У стартовавшего дежурства изменится время конца смены
    started_shift.refresh_from_db()

    shift = shifts[0]
    if not duty_on_weekends:
        assert (
            shift.start_datetime.astimezone(settings.DEFAULT_TIMEZONE) ==
            datetime.datetime(2019, 1, 7, 0, 0).astimezone(settings.DEFAULT_TIMEZONE)
        )
        assert shift.start == datetime.datetime(2019, 1, 7).astimezone(settings.DEFAULT_TIMEZONE).date()
        assert (
            shift.end_datetime.astimezone(settings.DEFAULT_TIMEZONE) ==
            datetime.datetime(2019, 1, 12, 00, 0).astimezone(settings.DEFAULT_TIMEZONE)
        )
        assert shift.end == datetime.datetime(2019, 1, 11).astimezone(settings.DEFAULT_TIMEZONE).date()

    else:
        assert (
            shift.start_datetime.astimezone(settings.DEFAULT_TIMEZONE) ==
            datetime.datetime(2019, 1, 7, 0, 0).astimezone(settings.DEFAULT_TIMEZONE)
        )
        assert shift.start == datetime.datetime(2019, 1, 7).astimezone(settings.DEFAULT_TIMEZONE).date()

        assert (
            shift.end_datetime.astimezone(settings.DEFAULT_TIMEZONE) ==
            datetime.datetime(2019, 1, 12, 00, 0).astimezone(settings.DEFAULT_TIMEZONE)
        )
        assert shift.end == datetime.datetime(2019, 1, 11).astimezone(settings.DEFAULT_TIMEZONE).date()

    shift = shifts[1]
    if not duty_on_weekends:
        assert (
            shift.start_datetime.astimezone(settings.DEFAULT_TIMEZONE) ==
            datetime.datetime(2019, 1, 14, 00, 0).astimezone(settings.DEFAULT_TIMEZONE)
        )
        assert shift.start == datetime.datetime(2019, 1, 14).astimezone(settings.DEFAULT_TIMEZONE).date()

        assert (
            shift.end_datetime.astimezone(settings.DEFAULT_TIMEZONE) ==
            datetime.datetime(2019, 1, 19, 00, 0).astimezone(settings.DEFAULT_TIMEZONE)
        )
        assert shift.end == datetime.datetime(2019, 1, 18).astimezone(settings.DEFAULT_TIMEZONE).date()

    else:
        assert (
            shift.start_datetime.astimezone(settings.DEFAULT_TIMEZONE) ==
            datetime.datetime(2019, 1, 12, 00, 0).astimezone(settings.DEFAULT_TIMEZONE)
        )
        assert shift.start == datetime.datetime(2019, 1, 12).astimezone(settings.DEFAULT_TIMEZONE).date()

        assert (
            shift.end_datetime.astimezone(settings.DEFAULT_TIMEZONE) ==
            datetime.datetime(2019, 1, 17, 00, 0).astimezone(settings.DEFAULT_TIMEZONE)
        )
        assert shift.end == datetime.datetime(2019, 1, 16).astimezone(settings.DEFAULT_TIMEZONE).date()


@pytest.mark.parametrize('api', ['v3', 'v4'])
@pytest.mark.parametrize('reverse_order', [True, False])
def test_check_changes_update_orders_for_manual_schedule(client, api, reverse_order):
    """
    Изменение порядка в ордном графике, пр наличии в сервисе ещё одного графика,
    который должен учитывать другие, вызовет предупреждение о пересчете.
    """

    service = factories.ServiceFactory()
    schedule1 = factories.ScheduleFactory(algorithm=Schedule.MANUAL_ORDER, service=service, allow_sequential_shifts=False)
    factories.ScheduleFactory(consider_other_schedules=True, service=service)
    for i in range(3):
        staff = factories.StaffFactory()
        factories.ServiceMemberFactory(role=schedule1.role, staff=staff)
        factories.OrderFactory(staff=staff, order=i, schedule=schedule1)

    url = reverse(f'api-{api}:duty-schedule-check-changes')
    if reverse_order:
        new_orders = list(schedule1.orders.order_by('-order').values_list('staff__login', flat=True))
    else:
        new_orders = [staff.login]
    data = [{'id': schedule1.id, 'orders': new_orders}]
    response = client.json.post(url, data)

    assert response.status_code == 200
    assert response.json() == {'conditions': ['fields'], 'fields': ['orders'], 'consequence': 'hard'}


def test_create_schedule_duplicating_deleted_slug(client, duty_data):
    factories.ScheduleFactory(
        service=duty_data.service,
        name='Old Schedule',
        slug='schedule1',
        status=Schedule.DELETED
    )
    assert duty_data.service.flags['duty1'] is False
    assert duty_data.service.flags['duty2'] is True

    client.login(duty_data.owner.login)
    with mock.patch('plan.duty.tasks.DutyScheduler.recalculate_shifts'):
        response = client.json.post(
            reverse('api-v4:duty-schedule-list'),
            data={
                'name': 'New Schedule',
                'role': duty_data.role.id,
                'service': duty_data.service.id,
                'start_date': '2019-01-01',
                'duration': '3 00',
                'slug': 'schedule1',
                'show_in_staff': False,
            }
        )

        assert response.status_code == 201
        assert Schedule.objects.count() == 1
        assert Schedule.objects.first().name == 'New Schedule'

    duty_data.service.refresh_from_db()
    assert duty_data.service.flags['duty1'] is True
    assert duty_data.service.flags['duty2'] is False


def test_create_schedule_duplicating_deleted_name(client, duty_data):
    factories.ScheduleFactory(
        service=duty_data.service,
        name='Old Schedule',
        slug='schedule1',
        status=Schedule.DELETED
    )

    client.login(duty_data.owner.login)
    with mock.patch('plan.duty.tasks.DutyScheduler.recalculate_shifts'):
        response = client.json.post(
            reverse('api-v4:duty-schedule-list'),
            data={
                'name': 'Old Schedule',
                'role': duty_data.role.id,
                'service': duty_data.service.id,
                'start_date': '2019-01-01',
                'duration': '3 00',
                'slug': 'schedule2',
                'show_in_staff': False,
            }
        )

        assert response.status_code == 201
        assert Schedule.objects.count() == 1
        assert Schedule.objects.first().slug == 'schedule2'


@pytest.mark.parametrize('status', [Schedule.ACTIVE, Schedule.DELETED])
def test_rename_schedule_duplicating_name(client, duty_data, status):
    factories.ScheduleFactory(
        service=duty_data.service,
        name='Old Schedule',
        slug='schedule1',
        status=status
    )
    new_schedule = factories.ScheduleFactory(
        service=duty_data.service,
        name='New Schedule',
        slug='schedule2'
    )

    client.login(duty_data.owner.login)
    with mock.patch('plan.duty.tasks.DutyScheduler.recalculate_shifts'):
        response = client.json.patch(
            reverse('api-v4:duty-schedule-detail', args=[new_schedule.pk]),
            data={
                'name': 'Old Schedule'
            }
        )

        if status == Schedule.ACTIVE:
            assert response.status_code == 400
        else:
            assert response.status_code == 200
            assert Schedule.objects.count() == 1


@pytest.mark.parametrize(
    ('algorithm', 'is_member', 'has_order'),
    (
        (Schedule.NO_ORDER, False, True),
        (Schedule.NO_ORDER, True, False),
        (Schedule.MANUAL_ORDER, False, False),
        (Schedule.MANUAL_ORDER, True, False),
        (Schedule.MANUAL_ORDER, True, True)
    )
)
@pytest.mark.parametrize('has_role', (False, True))
def test_requester_in_duty(
    client, staff_factory, algorithm, is_member, has_order, has_role, django_assert_num_queries_lte
):
    schedule = factories.ScheduleFactory(algorithm=algorithm)
    staff = staff_factory()

    with mock.patch('plan.duty.tasks.DutyScheduler.recalculate_shifts'):
        if has_role:
            factories.ServiceMemberFactory(staff=staff, service=schedule.service)
        else:
            schedule.role = None
            schedule.save()
            responsible_role = factories.RoleFactory(code=Role.RESPONSIBLE)
            factories.ServiceMemberFactory(staff=staff, service=schedule.service, role=responsible_role)

        if is_member:
            if has_role:
                role = schedule.role
            else:
                role = factories.RoleFactory()
            factories.ServiceMemberFactory(staff=staff, service=schedule.service, role=role)

    if has_order:
        factories.OrderFactory(staff=staff, schedule=schedule, order=0)

    client.login(staff.login)

    general_queries = 4
    # 2 middleware
    # 1 pg_in_recovery
    # 1 waffle
    request_related_queries = 7
    # 1 select schedule
    # 1 select role (if has_role=True)
    # 1 select rolescope (if has_role=True)
    # 1 select order
    # 1 select staff (only in module run)
    # 1 select service (2 in module run)
    with django_assert_num_queries_lte(general_queries + request_related_queries):
        response = client.get(reverse('api-frontend:schedule-detail', args=[schedule.id]))
    assert response.status_code == 200
    data = response.json()
    requester_in_duty = data['requester_in_duty']
    assert requester_in_duty == (is_member and (algorithm == Schedule.NO_ORDER or has_order))
    assert data['is_important'] is False


@pytest.mark.parametrize('has_current_shift', [True, False])
def test_update_schedule_tracker_component(client, duty_data, duty_role, has_current_shift):
    client.login(duty_data.owner.login)
    schedule = factories.ScheduleFactory(service=duty_data.service, role=duty_role)
    if has_current_shift:
        factories.ShiftFactory(
            schedule=schedule,
            role=duty_role,
            staff=duty_data.owner,
            state=Shift.STARTED,
        )
    with mock.patch('plan.duty.tasks.DutyScheduler.recalculate_shifts'):
        with mock.patch('plan.duty.api.serializers.check_can_grant_queue') as mock_check_can_grant_queue:
            with mock.patch('plan.duty.api.serializers.enable_auto_assign') as mock_enable_auto_assign:
                with mock.patch('plan.duty.api.serializers.set_component_lead') as mock_set_component_lead:
                    response = client.json.patch(
                        reverse('api-v4:duty-schedule-detail', args=[schedule.pk]),
                        data={
                            'tracker_component_id': 123,
                            'tracker_queue_id': 1234,
                        }
                    )
                    mock_check_can_grant_queue.assert_called_once_with(queue_id=1234, user_ticket=None)
                    mock_enable_auto_assign.assert_called_once_with(component_id=123, user_ticket=None)
                    if has_current_shift:
                        mock_set_component_lead.assert_called_once_with(component_id=123, staff=duty_data.owner)
                    else:
                        mock_set_component_lead.assert_not_called()

    assert response.status_code == 200
    schedule.refresh_from_db()
    assert schedule.tracker_component_id == 123
    assert schedule.tracker_queue_id == 1234


@pytest.mark.parametrize(('current_queue', 'new_queue', 'current_component', 'new_component'), [
    (None, 1234, None, 2),
    (12345, None, 1, None),
    (1234, 12346, 1, 2),
])
def test_update_schedule_tracker_component_no_perm(
    client, duty_data, duty_role, current_queue, new_queue,
    current_component, new_component
):
    client.login(duty_data.owner.login)
    schedule = factories.ScheduleFactory(
        service=duty_data.service, role=duty_role,
        tracker_component_id=current_component,
        tracker_queue_id=current_queue,
    )
    with mock.patch('plan.duty.api.serializers.check_can_grant_queue') as mock_check_can_grant_queue:
        mock_check_can_grant_queue.return_value = False
        response = client.json.patch(
            reverse('api-v4:duty-schedule-detail', args=[schedule.pk]),
            data={
                'tracker_component_id': new_component,
                'tracker_queue_id': new_queue,
            }
        )
        mock_check_can_grant_queue.assert_called_once_with(
            queue_id=new_queue if new_queue else current_queue,
            user_ticket=None,
        )

    assert response.status_code == 400
    message = 'You must have grant permission on queue in order to erase this field'
    if new_component:
        message = 'Grant permissions for chosen queue is missing for current user'
    assert response.json()['error']['message']['en'] == message


@pytest.mark.parametrize(
    (
        'current_queue', 'new_queue', 'current_component',
        'new_component', 'current_persons_count', 'new_persons_count',
        'conflict',
    ), [
    (123, 123, 1234, 1234, 1, 2, True),
    (123, None, 1234, None, 1, 2, False),
    (123, 123, None, 1234, 4, None, True),
    (None, 123, None, 1234, 4, 1, False),
    (123, False, 123, False, 1, 4, True),
    (None, 123, None, None, 1, None, True),
    (123, 123, 123, None, 1, None, True),
])
def test_update_schedule_tracker_component_with_person_count(
    client, duty_data, duty_role, current_queue, new_queue,
    current_component, new_component, current_persons_count,
    new_persons_count, conflict,
):
    client.login(duty_data.owner.login)
    schedule = factories.ScheduleFactory(
        service=duty_data.service, role=duty_role,
        tracker_component_id=current_component,
        tracker_queue_id=current_queue,
        persons_count=current_persons_count,
    )
    with mock.patch('plan.duty.api.serializers.check_can_grant_queue'):
        with mock.patch('plan.duty.api.serializers.enable_auto_assign'):
            with mock.patch('plan.duty.api.serializers.set_component_lead'):
                with mock.patch('plan.duty.api.serializers.remove_component_lead'):
                    data = {}
                    if new_component is not False:
                        data['tracker_component_id'] = new_component
                    if new_queue is not False:
                        data['tracker_queue_id'] = new_queue
                    if new_persons_count:
                        data['persons_count'] = new_persons_count

                    response = client.json.patch(
                        reverse('api-v4:duty-schedule-detail', args=[schedule.pk]),
                        data=data,
                    )

    if conflict:
        assert response.status_code == 400
        if new_persons_count:
            assert response.json()['error']['message']['en'] == 'You cannot set persons_count > 1 if tracker_component_id is set'
    else:
        assert response.status_code == 200


def test_update_schedule_with_tracker_queue_set(client, duty_data):
    client.login(duty_data.owner.login)
    schedule = factories.ScheduleFactory(
        service=duty_data.service,
        tracker_component_id=123,
        tracker_queue_id=324,
    )
    with mock.patch('plan.duty.api.serializers.check_can_grant_queue') as mock_check_can_grant_queue:
        response = client.json.patch(
            reverse('api-v4:duty-schedule-detail', args=[schedule.pk]),
            data={'name': 'changeme'},
        )
    mock_check_can_grant_queue.assert_not_called()
    assert response.status_code == 200


@pytest.mark.parametrize('mock_tvm_service_ticket', [settings.WATCHER_TVM_ID], indirect=True)
def test_watcher_modify_duty(client, mock_tvm_service_ticket):

    service = factories.ServiceFactory()
    schedule = factories.ScheduleFactory(service=service)

    response = client.json.patch(
        reverse('api-v4:duty-schedule-detail', args=[schedule.pk]),
        data={'slug': 'new-slug'},
    )

    schedule.refresh_from_db()
    assert response.status_code == 200
    assert schedule.slug == 'new-slug'
