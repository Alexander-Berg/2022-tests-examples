import datetime
import pretend
import pytest

from django.core.urlresolvers import reverse
from django.utils import timezone
from freezegun import freeze_time

from plan.duty.models import Shift, Schedule
from common import factories


pytestmark = pytest.mark.django_db


@pytest.fixture
def data_about_service(staff_factory, owner_role, responsible_for_duty_role):
    owner = staff_factory('full_access')
    service = factories.ServiceFactory(owner=owner)
    factories.ServiceMemberFactory(staff=owner, role=owner_role, service=service)

    staff = staff_factory('full_access')
    duty_resp = staff_factory('full_access')
    factories.ServiceMemberFactory(staff=duty_resp, role=responsible_for_duty_role, service=service)

    return pretend.stub(
        staff=staff,
        service=service,
        owner=owner,
        duty_resp=duty_resp
    )


@pytest.mark.parametrize(('api', 'start_field_name', 'start_value', 'end_field_name', 'end_value'), [
    ('api-v3:duty-schedule-upload-history', 'start', '2020-12-31', 'end', '2021-01-31'),
    ('api-v4:duty-schedule-shifts-append', 'start_datetime', '2020-12-31T00:00:00', 'end_datetime',
     '2021-01-31T00:00:00'),
    ('api-v4:duty-schedule-shifts-replace', 'start_datetime', '2020-12-31T00:00:00', 'end_datetime',
     '2021-01-31T00:00:00')
])
def test_upload_history_403(api, start_field_name, start_value, end_field_name, end_value, data_about_service, client):
    """
    Если какой-то левый юзер заливает данные - 403
    """

    schedule = factories.ScheduleFactory(service=data_about_service.service)
    staff = data_about_service.staff

    client.login(staff.login)
    response = client.json.post(
        reverse(f'{api}', args=[schedule.id]),
        [
            {
                'person': data_about_service.staff.login,
                'schedule': schedule.pk,
                start_field_name: start_value,
                end_field_name: end_value
            }
        ]
    )
    assert response.status_code == 403
    assert response.json()['error']['detail'] == (
        f'You are not responsible for service {data_about_service.service.slug}'
    )


@pytest.mark.parametrize(('api', 'start_field_name', 'start_value', 'end_field_name', 'end_value', 'status'), [
    ('api-v3:duty-schedule-upload-history', 'start', '2020-12-31', 'end', '2021-01-31', 403),
    ('api-v4:duty-schedule-shifts-append', 'start_datetime', '2020-12-31T00:00:00', 'end_datetime',
     '2021-01-31T00:00:00', 200),
    ('api-v4:duty-schedule-shifts-replace', 'start_datetime', '2020-12-31T00:00:00', 'end_datetime',
     '2021-01-31T00:00:00', 200)
])
def test_upload_history_duty_responsible(api, start_field_name, start_value, end_field_name, end_value, status,
                                         data_about_service, client):
    """
    Если данные заливает управляющий дежурствами:
        * для upload_history - 403
        * для replace и append - 200 ОК
    """

    schedule = factories.ScheduleFactory(service=data_about_service.service)
    duty_resp = data_about_service.duty_resp

    client.login(duty_resp.login)
    response = client.json.post(
        reverse(f'{api}', args=[schedule.id]),
        [
            {
                'person': data_about_service.staff.login,
                'schedule': schedule.pk,
                start_field_name: start_value,
                end_field_name: end_value
            }
        ]
    )

    assert response.status_code == status

    if status == 403:
        assert response.json()['error']['detail'] == (
            f'You are not responsible for service {data_about_service.service.slug}'
        )


@pytest.mark.parametrize(('api', 'start_field_name', 'start_value', 'end_field_name', 'end_value',), [
    ('api-v3:duty-schedule-upload-history', 'start', '2020-12-31', 'end', '2021-01-31',),
    ('api-v4:duty-schedule-shifts-append', 'start_datetime', '2020-12-31T00:00:00', 'end_datetime',
     '2021-01-31T00:00:00'),
    ('api-v4:duty-schedule-shifts-replace', 'start_datetime', '2020-12-31T00:00:00', 'end_datetime',
     '2021-01-31T00:00:00')
])
def test_upload_history_multiple_schedules(api, start_field_name, start_value, end_field_name, end_value,
                                           data_about_service, client):
    """
    Ручки заливки данных (upload_history, append и replace)
    не позволяет заливать данные более чем для одного графика за раз.
    """

    schedule_1 = factories.ScheduleFactory(service=data_about_service.service)
    schedule_2 = factories.ScheduleFactory(service=data_about_service.service)

    client.login(data_about_service.owner.login)
    response = client.json.post(
        reverse(f'{api}', args=[schedule_1.id]),
        [
            {
                'person': data_about_service.staff.login,
                'schedule': schedule_2.pk,
                start_field_name: start_value,
                end_field_name: end_value
            },
            {
                'person': data_about_service.staff.login,
                'schedule': schedule_1.pk,
                start_field_name: start_value,
                end_field_name: end_value
            },
        ]
    )
    assert response.status_code == 400
    assert response.json()['error']['detail'] == f'Wrong schedules {set([schedule_1, schedule_2])} for this calendar'


@freeze_time('2021-01-01')
@pytest.mark.parametrize(('algorithm', 'index'), [(Schedule.NO_ORDER, None), (Schedule.MANUAL_ORDER, 0)])
@pytest.mark.parametrize(('api', 'start_field_name', 'start_value', 'end_field_name', 'end_value',), [
    ('api-v3:duty-schedule-upload-history', 'start', '2020-12-31', 'end', '2021-01-31',),
    ('api-v4:duty-schedule-shifts-replace', 'start_datetime', '2020-12-31T10:00:00', 'end_datetime',
     '2021-01-31T16:00:00')
])
def test_upload_history_replace(algorithm, index, api, start_field_name, start_value, end_field_name, end_value,
                                data_about_service, client, owner_role):
    """
    При заливки удаляем имеющиеся смены.
    Чтобы не запускался пересчёт графиков, в настройках указываем recalculate False
    """

    schedule = factories.ScheduleFactory(
        role=owner_role,
        service=data_about_service.service,
        start_date=timezone.datetime(2018, 2, 1),
        algorithm=algorithm,
        recalculate=False,
    )
    shift = factories.ShiftFactory(schedule=schedule)  # должно удалиться
    another_shift = factories.ShiftFactory()  # не должно удалиться

    client.login(data_about_service.owner.login)
    response = client.json.post(
        reverse(f'{api}', args=[schedule.id]),
        [
            {
                'person': data_about_service.staff.login,
                'schedule': schedule.pk,
                start_field_name: start_value,
                end_field_name: end_value
            },
        ]
    )
    assert response.status_code == 200

    another_shift.refresh_from_db()
    with pytest.raises(Shift.DoesNotExist):
        shift.refresh_from_db()

    shifts = Shift.objects.filter(schedule=schedule)
    shift = shifts.first()
    assert shift.start == datetime.date(2020, 12, 31)
    assert shift.end == datetime.date(2021, 1, 31)
    assert shift.staff == data_about_service.staff

    assert shift.index == index


@freeze_time('2019-02-01')
@pytest.mark.parametrize(('algorithm', 'index'), [(Schedule.NO_ORDER, None), (Schedule.MANUAL_ORDER, 0)])
def test_upload_history_append(algorithm, index, data_about_service, client, owner_role):
    """
    Не удаляем имеющиеся смены.
    Смены не пересчитываем, чтобы не запускалась таска пересчёта.
    """

    schedule = factories.ScheduleFactory(
        role=owner_role,
        service=data_about_service.service,
        start_date=timezone.datetime(2018, 2, 1),
        algorithm=algorithm,
        recalculate=False,
    )

    # оба шифта должны остаться
    shift = factories.ShiftFactory(schedule=schedule)
    another_shift = factories.ShiftFactory()

    client.login(data_about_service.owner.login)
    response = client.json.post(
        reverse('api-v4:duty-schedule-shifts-append', args=[schedule.id]),
        [
            {
                'person': data_about_service.staff.login,
                'schedule': schedule.pk,
                'start_datetime': '2020-12-31T12:00:00',
                'end_datetime': '2021-01-31T12:00:00'
            }
        ]
    )
    assert response.status_code == 200

    another_shift.refresh_from_db()
    shift.refresh_from_db()
    assert Shift.objects.count() == 3

    shifts = Shift.objects.filter(schedule=schedule).order_by('id')
    shift = shifts.last()
    assert shift.start == datetime.date(2020, 12, 31)
    assert shift.end == datetime.date(2021, 1, 31)
    assert shift.staff == data_about_service.staff

    assert shift.index == index


@freeze_time('2019-02-01')
@pytest.mark.parametrize('algorithm', [Schedule.NO_ORDER, Schedule.MANUAL_ORDER])
@pytest.mark.parametrize('api', [
    'api-v4:duty-schedule-shifts-append',
    'api-v4:duty-schedule-shifts-replace',
])
def test_upload_history_datetime(client, data_about_service, owner_role, algorithm, api):
    """
    Проверяем заливку данных со start_datetime и end_datetime
    """

    schedule = factories.ScheduleFactory(
        role=owner_role,
        service=data_about_service.service,
        start_date=timezone.datetime(2020, 2, 1),
        algorithm=algorithm,
    )

    client.login(data_about_service.owner.login)
    response = client.json.post(
        reverse(f'{api}', args=[schedule.id]),
        [
            {
                'person': data_about_service.staff.login,
                'schedule': schedule.pk,
                'start_datetime': '2020-12-31T16:00:00',
                'end_datetime': '2021-01-31T10:00:00'
            },
            {
                'person': data_about_service.staff.login,
                'schedule': schedule.pk,
                'start_datetime': '2021-01-13T11:00:00',
                'end_datetime': '2021-02-13T05:00:00'
            }
        ]
    )
    assert response.status_code == 200
    assert Shift.objects.filter(schedule=schedule).count() == 2


@freeze_time('2019-02-01')
@pytest.mark.parametrize('algorithm', [Schedule.NO_ORDER, Schedule.MANUAL_ORDER])
@pytest.mark.parametrize('api', [
    'api-v4:duty-schedule-shifts-append',
    'api-v4:duty-schedule-shifts-replace',
])
def test_upload_history_end_before_start(client, data_about_service, owner_role, algorithm, api):
    """
    Проверяем заливку данных, если end_datetime оказался до start_datetime
    """

    schedule = factories.ScheduleFactory(
        role=owner_role,
        service=data_about_service.service,
        start_date=timezone.datetime(2020, 2, 1),
        algorithm=algorithm,
    )

    client.login(data_about_service.owner.login)
    response = client.json.post(
        reverse(f'{api}', args=[schedule.id]),
        [
            {
                'person': data_about_service.staff.login,
                'schedule': schedule.pk,
                'end_datetime': '2020-12-31T16:00:00',
                'start_datetime': '2021-01-31T10:00:00'
            }
        ]
    )
    assert response.status_code == 400
    assert response.json()['error']['detail'] == 'Shift date incorrect: end 2020-12-31 16:00:00+03:00 before start 2021-01-31 10:00:00+03:00.'
