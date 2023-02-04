import itertools

from freezegun import freeze_time

import pytest

from datetime import time, timedelta
from django.core.urlresolvers import reverse
from django.test.utils import override_settings
from django.utils import timezone

from plan.duty.models import Shift
from plan.holidays.models import Holiday
from common import factories

pytestmark = pytest.mark.django_db


@pytest.fixture()
def redirect_on_duty():
    flag = factories.SwitchFactory(
        name='redirect_on_duty',
        active=True,
    )
    return flag


@freeze_time('2020-01-01T01:00:00')
@override_settings(DEFAULT_TIMEZONE=timezone.pytz.timezone('Europe/Moscow'))
def test_current_duty_one_day(client, data, redirect_on_duty):
    """
    Тестирование ручки, отдающей данные о текущих дежурных у сервиса.
    У сервиса с календарём есть график со сменами длиною в один день

    Ожидаем на выходе: 2 шифт
    """

    service = data.service
    schedule_one_day = factories.ScheduleFactory(service=service)

    today = timezone.now().date()

    factories.ShiftFactory(
        start=today - timedelta(1),
        end=today - timedelta(1),
        schedule=schedule_one_day,
    )

    shift_one_day_2 = factories.ShiftFactory(
        state=Shift.STARTED,
        start=today,
        end=today,
        schedule=schedule_one_day,
    )

    factories.ShiftFactory(
        state=Shift.STARTED,
        start=today + timedelta(1),
        end=today + timedelta(1),
        schedule=schedule_one_day,
    )

    response = client.json.get(
        reverse('api-v4:duty-on-duty-list'),
        {
            'service': service.id,
        }
    )

    data = response.json()
    # на выходе хотим увидеть: shift_one_day_1, а не shift_one_day_2
    # т.к. смена по умолчанию заканчивается в 5 часов по UTC, то в 01 часов будет ещё предыдущая смена
    assert len(data) == 1
    received_shift = data[0]
    assert received_shift['id'] == shift_one_day_2.id
    assert received_shift['start'] == '2020-01-01'
    assert received_shift['end'] == '2020-01-01'
    assert received_shift['start_datetime'] == '2020-01-01T00:00:00+03:00'
    assert received_shift['end_datetime'] == '2020-01-02T00:00:00+03:00'


@freeze_time('2020-01-01T12:00:00')
def test_current_duty_start_day(client, data, redirect_on_duty):
    """
    Тестирование ручки, отдающей данные о текущих дежурных у сервиса.
    У сервиса с календарём есть график со сменой, у которой сегодня начинается дежурство

    Ожидаем на выходе: 1 шифт
    """

    service = data.service
    schedule_start_day = factories.ScheduleFactory(service=service)

    today = timezone.now().date()

    staff = factories.StaffFactory(telegram_account='some_account')
    shift_start_1 = factories.ShiftFactory(
        state=Shift.SCHEDULED,
        staff=staff,
        start=today,
        end=(today + timedelta(3)),
        schedule=schedule_start_day,
    )
    factories.ShiftFactory(
        state=Shift.STARTED,
        start=(today - timedelta(3)),
        end=(today - timedelta(1)),
        schedule=schedule_start_day,
    )

    response = client.json.get(
        reverse('api-v4:duty-on-duty-list'),
        {
            'service': service.id,
            'fields': 'id,person.telegram_account',
        }
    )

    # на выходе хотим увидеть: shift_start_1
    data = response.json()
    assert len(data) == 1
    assert data[0]['id'] == shift_start_1.id
    assert data[0]['person']['telegram_account'] == 'some_account'


@freeze_time('2020-01-01T12:00:00')
def test_current_duty_final_day(client, data, redirect_on_duty):
    """
    Тестирование ручки, отдающей данные о текущих дежурных у сервиса.
    У сервиса с календарём есть график со сменой, у которой сегодня заканчивается дежурство

    Ожидаем на выходе: 1 шифт
    """

    service = data.service
    schedule_final_day = factories.ScheduleFactory(service=service)
    schedule_deleted = factories.ScheduleFactory(service=service, status='deleted')
    today = timezone.now().date()

    shift_final_1 = factories.ShiftFactory(
        state=Shift.STARTED,
        start=(today - timedelta(3)),
        end=today,
        schedule=schedule_final_day,
    )
    factories.ShiftFactory(
        state=Shift.STARTED,
        start=(today - timedelta(3)),
        end=today,
        schedule=schedule_deleted,
    )
    factories.ShiftFactory(
        start=(today + timedelta(1)),
        end=(today + timedelta(3)),
        schedule=schedule_final_day,
    )

    response = client.json.get(
        reverse('api-v4:duty-on-duty-list'),
        {
            'service': service.id,
        }
    )

    # на выходе хотим увидеть: shift_final_1
    assert len(response.json()) == 1
    assert set([shift['id'] for shift in response.json()]) == {shift_final_1.id}


@freeze_time('2020-01-01T12:00:00')
def test_current_duty_middle_of_shift(client, data, redirect_on_duty):
    """
    Тестирование ручки, отдающей данные о текущих дежурных у сервиса.
    У сервиса с календарём есть график, для которого сегодня попадает в интервал одной из смен

    Ожидаем на выходе: 1 шифт
    """

    service = data.service
    schedule = factories.ScheduleFactory(service=service)

    today = timezone.now().date()

    shift_1 = factories.ShiftFactory(
        state=Shift.STARTED,
        start=(today - timedelta(3)),
        end=(today + timedelta(3)),
        schedule=schedule,
    )
    factories.ShiftFactory(
        start=(today + timedelta(3)),
        end=(today + timedelta(6)),
        schedule=schedule,
    )

    response = client.json.get(
        reverse('api-v4:duty-on-duty-list'),
        {
            'service': service.id,
        }
    )

    # на выходе хотим увидеть: shift_1
    assert len(response.json()) == 1
    assert set([shift['id'] for shift in response.json()]) == set([shift_1.id])


@freeze_time('2020-01-01T12:00:00')
@pytest.mark.parametrize('endpoint_path', ('services-api:service-on-duty', 'api-v3:service-on-duty'))
def test_current_duty_two_schedule(staff_factory, client, data, endpoint_path, redirect_on_duty):
    """
    Тестирование ручки, отдающей данные о текущих дежурных у сервиса.
    У сервиса с календарём есть несколько графиков, проверим, что получаем данные из всех, а не только из одного

    Ожидаем на выходе: 2 шифта
    Так же в этом тесте проверяем правильность редиректа.
    """

    service = data.service
    schedule_one_day = factories.ScheduleFactory(service=service)
    schedule = factories.ScheduleFactory(service=service)

    today = timezone.now().date()

    # у каждого графика будет по два шифта
    shift_one_day_1 = factories.ShiftFactory(
        state=Shift.STARTED,
        start=today,
        end=today,
        schedule=schedule_one_day,
    )
    factories.ShiftFactory(
        start=today - timedelta(1),
        end=today - timedelta(1),
        schedule=schedule_one_day,
    )

    shift_1 = factories.ShiftFactory(
        state=Shift.STARTED,
        start=(today - timedelta(3)),
        end=(today + timedelta(3)),
        schedule=schedule,
    )
    factories.ShiftFactory(
        start=(today + timedelta(3)),
        end=(today + timedelta(6)),
        schedule=schedule,
    )

    staff_for = staff_factory('full_access')
    client.login(staff_for.login)

    response = client.json.get(
        reverse(endpoint_path, args=[service.id])
    )

    assert response.status_code == 302
    new_url = response.url
    response = client.json.get(new_url)
    assert response.status_code == 200

    # на выходе хотим увидеть: shift_one_day_1 и shift_1
    assert len(response.json()) == 2
    assert set([shift['id'] for shift in response.json()]) == {shift_one_day_1.id, shift_1.id}

    # пофильтруем по одному графику
    response = client.json.get(
        reverse(endpoint_path, args=[service.id]), {'schedule': schedule_one_day.id}
    )
    assert response.status_code == 302
    new_url = response.url
    response = client.json.get(new_url)
    assert response.status_code == 200
    assert len(response.json()) == 1
    assert set([shift['id'] for shift in response.json()]) == {shift_one_day_1.id, }

    # пофильтруем по одному логину юзера
    response = client.json.get(
        reverse(endpoint_path, args=[service.id]), {'person': shift_1.staff.login}
    )
    assert response.status_code == 302
    new_url = response.url
    response = client.json.get(new_url)
    assert response.status_code == 200
    assert len(response.json()) == 1
    assert set([shift['person']['login'] for shift in response.json()]) == {shift_1.staff.login, }


@freeze_time('2020-01-01T12:00:00')
def test_get_service_on_duty_with_fields(client, redirect_on_duty):
    """
    GET /api/v4/duty/on_duty/?fields=...
    """
    service = factories.ServiceFactory()
    schedule = factories.ScheduleFactory(service=service)
    factories.ShiftFactory(start=timezone.now().date(), schedule=schedule)
    response = client.json.get(
        reverse('api-v4:duty-on-duty-list'),
        {
            'service': service.id,
            'fields': 'schedule.orders,person.login,id,replace_for,replaces'
        }
    )
    assert response.status_code == 200
    assert len(response.json()) == 1
    assert 'orders' in response.json()[0]['schedule']
    assert 'replace_for' in response.json()[0]
    assert 'replaces' in response.json()[0]


@freeze_time('2020-01-01T12:00:00')
def test_get_service_on_duty_with_flag(client):
    """
    GET /api/v4/duty/on_duty/?fields=...
    """
    service = factories.ServiceFactory()
    schedule = factories.ScheduleFactory(service=service)
    factories.ShiftFactory(start=timezone.now().date(), schedule=schedule)
    response = client.json.get(reverse('api-v4:duty-on-duty-list'), args=[service.id])
    assert response.status_code == 200


@freeze_time('2020-01-01T12:00:00')
def test_get_service_on_duty_schedule_slug(client, redirect_on_duty):
    """
    GET /api/v4/duty/on_duty/?fields=...
    """
    service = factories.ServiceFactory()
    schedule = factories.ScheduleFactory(service=service)
    factories.ShiftFactory(start=timezone.now().date(), schedule=schedule)
    url = reverse('api-v4:duty-on-duty-list')
    response = client.json.get(url, {'service': service.id})
    assert 'slug' in response.json()[0]['schedule']


@pytest.mark.parametrize(
    ('request_time', 'valid_shift'),
    (('2019-12-30T01:00:00', 'main_shift'), ('2020-01-01T01:00:00', 'replace1'), ('2020-01-01T12:00:00', 'replace2')),
)
@pytest.mark.parametrize('order_of_objects', list(itertools.permutations([1, 2, 3], 3)))
def test_get_service_on_duty_with_replaces(data, client, request_time, valid_shift, redirect_on_duty, order_of_objects):
    """
    Проверим три случая:
      1) Запрос в день начала replace1, но до schedule.start_time
      2) Запрос в день начала replace2, но до schedule.start_time
      3) Запрос в день начала replace2, после schedule.start_time
    """
    id1, id2, id3 = order_of_objects

    with freeze_time('2020-01-01T12:00:00'):
        service = data.service
        schedule = factories.ScheduleFactory(service=service, start_time=time(hour=5))
        staff = factories.StaffFactory()
        replace_staff_1 = factories.StaffFactory()
        replace_staff_2 = factories.StaffFactory()

        today = timezone.now().date()
        shifts = dict()
        shifts['main_shift'] = factories.ShiftFactory(
            id=id1,
            state=Shift.STARTED,
            start=today - timedelta(7),
            end=today + timedelta(7),
            schedule=schedule,
            staff=staff,
            is_approved=True,
        )

        shifts['replace1'] = factories.ShiftFactory(
            id=id2,
            staff=replace_staff_1,
            replace_for=shifts['main_shift'],
            start=today-timedelta(2),
            end=today - timedelta(1),
            schedule=schedule,
        )

        shifts['replace2'] = factories.ShiftFactory(
            id=id3,
            staff=replace_staff_2,
            replace_for=shifts['main_shift'],
            start=today,
            end=today + timedelta(2),
            schedule=schedule,
        )

    url = reverse('api-v4:duty-on-duty-list')
    with freeze_time(request_time):
        response = client.json.get(url, {'service': service.id})
    assert response.status_code == 200
    data = response.json()
    assert len(data) == 1
    received_shift = data[0]
    expected_shift = shifts[valid_shift]
    assert received_shift['id'] == expected_shift.id
    assert received_shift['person']['login'] == expected_shift.staff.login


@freeze_time('2020-12-12T12:00:00')
@pytest.mark.parametrize('is_holiday', [True, False])
@pytest.mark.parametrize('duty_on_weekends', [True, False])
@pytest.mark.parametrize('duty_on_holidays', [True, False])
def test_nobody_on_duty_on_holidays(client, is_holiday, duty_on_weekends, duty_on_holidays):
    """
    Выходной приходится на субботу. Может быть праздничным или нет.
    Если для графика это не рабочий день, данные отдавать не должны.
    """

    service = factories.ServiceFactory()
    schedule = factories.ScheduleFactory(
        service=service,
        duty_on_weekends=duty_on_weekends,
        duty_on_holidays=duty_on_holidays,
    )

    today = timezone.now().date()
    factories.ShiftFactory(
        state=Shift.STARTED,
        start=today-timedelta(days=3),
        end=today+timedelta(days=3),
        schedule=schedule,
    )

    if is_holiday:
        Holiday.objects.filter(date='2020-12-12').update(is_holiday=True)

    response = client.json.get(
        reverse('api-v4:duty-on-duty-list'),
        {'service': service.id}
    )
    assert response.status_code == 200

    if (is_holiday and duty_on_holidays) or (not is_holiday and duty_on_weekends):
        assert len(response.json()) == 1
    else:
        assert len(response.json()) == 0


@freeze_time('2020-12-11T12:00:00')
@pytest.mark.parametrize('is_holiday', [True, False])
@pytest.mark.parametrize('duty_on_weekends', [True, False])
@pytest.mark.parametrize('duty_on_holidays', [True, False])
def test_nobody_on_duty_on_holiday_friday(client, duty_on_weekends, duty_on_holidays, is_holiday):
    """
    Выходной (праздник или нет) выпадает по середине недели (пятница).
    Если для графика это не рабочий день, данные отдавать не должны.
    """

    service = factories.ServiceFactory()
    schedule = factories.ScheduleFactory(
        service=service,
        duty_on_weekends=duty_on_weekends,
        duty_on_holidays=duty_on_holidays,
    )

    today = timezone.now().date()
    factories.ShiftFactory(
        state=Shift.STARTED,
        start=today-timedelta(days=3),
        end=today+timedelta(days=3),
        schedule=schedule,
    )

    factories.HolidayFactory(date='2020-12-11', is_holiday=is_holiday)

    response = client.json.get(
        reverse('api-v4:duty-on-duty-list'),
        {'service': service.id}
    )
    assert response.status_code == 200
    if (is_holiday and duty_on_holidays) or (not is_holiday and duty_on_weekends):
        assert len(response.json()) == 1
    else:
        assert len(response.json()) == 0
