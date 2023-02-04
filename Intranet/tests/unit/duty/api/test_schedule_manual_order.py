import datetime
import pytest

from freezegun import freeze_time
from django.core.urlresolvers import reverse
from django.utils import timezone
from django.test import override_settings
from mock import patch

from plan.duty.models import Schedule
from plan.duty.tasks import recalculate_duty_for_service
from common import factories


pytestmark = pytest.mark.django_db


def prepare_data(start_date, logins, owner_role, person, duration=1):
    service = factories.ServiceFactory(owner=person)
    factories.ServiceMemberFactory(service=service, role=owner_role, staff=person)
    role = factories.RoleFactory()

    schedule = factories.ScheduleFactory(
        service=service,
        role=role,
        start_date=start_date,
        duration=timezone.timedelta(days=duration),
        algorithm=Schedule.MANUAL_ORDER,
        only_workdays=False,
    )

    with patch('plan.duty.tasks.DutyScheduler.recalculate_shifts'):
        for order, login in enumerate(logins):
            staff = factories.StaffFactory(login=login)
            factories.ServiceMemberFactory(service=service, role=role, staff=staff)
            factories.OrderFactory(staff=staff, order=order, schedule=schedule)

    recalculate_duty_for_service(service.id)

    return service, schedule


@freeze_time('2020-12-01')
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=5))
@pytest.mark.parametrize('api', ('api-v3', 'api-v4'))
def test_schedule_edit_start_with_offset_0_no_recalculete(client, owner_role, person, api, transactional_db):
    """
    В этом тесте замокали период, на который создаются смены:
        * вместо полгода - 5 дней (берём короткие смены по 1 дню).

    Если order не передан, а только start_with, то всё просто =>
    наш offset не изменится, если передадим логин того, кто и так по текущему порядку должен быть следующим
    (а сам orders тот же, тк его не передают).

    Ручной порядок, в start_with передаём следующего дежурного, orders не передаётся.
    Если offset нулевой, то зачищать шифты не должны, offset не изменится
    """

    logins = ['anna', 'bob', 'cole', 'diana', 'emma', ]
    service, schedule = prepare_data('2020-11-25', logins, owner_role, person)
    client.login(service.owner.login)
    current_shift = schedule.shifts.current_shifts().first()
    assert current_shift.staff.login == 'bob'

    next_shift_id = schedule.shifts.future().order_by('start_datetime').first().id

    # если оффсет нулевой, то переданный start_with == следующему ничего не меняет:
    response = client.json.patch(
        reverse(f'{api}:duty-schedule-detail', args=[schedule.pk]),
        data={
            'start_with': 'cole',
        }
    )
    assert response.status_code == 200

    schedule.refresh_from_db()
    next_shift = schedule.shifts.future().order_by('start_datetime').first()
    assert next_shift.staff.login == 'cole'
    assert schedule.manual_ordering_offset == 0
    assert next_shift.id == next_shift_id


@freeze_time('2020-12-01')
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=5))
@pytest.mark.parametrize('api', ('api-v3', 'api-v4'))
def test_schedule_edit_start_with_offset_0_recalculate(client, owner_role, person, api, transactional_db):
    """
    В этом тесте замокали период, на который создаются смены:
        * вместо полгода - 5 дней (берём короткие смены по 1 дню).

    Ручной порядок, проверим, что при апдейте offset меняется, если это необоходимо и происходит пересчет.
    Изначально оффсет нулевой.
    """

    logins = ['anna', 'bob', 'cole', 'diana', 'emma', ]
    service, schedule = prepare_data('2020-11-25', logins, owner_role, person)
    client.login(service.owner.login)

    # проапдейтим для задания ненулевого оффсета
    response = client.json.patch(
        reverse(f'{api}:duty-schedule-detail', args=[schedule.pk]),
        data={
            'start_with': 'diana',
        }
    )
    assert response.status_code == 200
    schedule.refresh_from_db()
    next_shift = schedule.shifts.future().order_by('start_datetime').first()
    assert next_shift.staff.login == 'diana'
    assert schedule.manual_ordering_offset == 1


@freeze_time('2020-12-01')
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=5))
@pytest.mark.parametrize('api', ('api-v3', 'api-v4'))
def test_schedule_edit_start_with(client, owner_role, person, api, transactional_db):
    """
    В этом тесте замокали период, на который создаются смены:
        * вместо полгода - 5 дней (берём короткие смены по 1 дню).

    Если order не передан, а только start_with, то всё просто =>
    наш offset не изменится, если передадим логин того, кто и так по текущему порядку должен быть следующим
    (а сам orders тот же, тк его не передаём).

    Ручной порядок, offset должен быть не нулевой, поэтому ДО нашей праверки нужно сделать ещё один патч.
    Старт графика 25.11, апдейт 01.12 => текущий дежурный 'bob'
    После апдейта следующим станет 'diana'
    """

    logins = ['anna', 'bob', 'cole', 'diana', 'emma', ]
    service, schedule = prepare_data('2020-11-25', logins, owner_role, person)
    client.login(service.owner.login)

    # проапдейтим, чтобы оффсет стал ненулевым
    response = client.json.patch(
        reverse(f'{api}:duty-schedule-detail', args=[schedule.pk]),
        data={
            'start_with': 'diana',
        }
    )
    assert response.status_code == 200
    schedule.refresh_from_db()
    assert schedule.manual_ordering_offset == 1
    old_offset = schedule.manual_ordering_offset

    # проверим, что если передать следующего при НЕнулевом оффсете, мы не стираем данные
    with freeze_time('2020-12-03'):
        # текущим будет 'emma' => следующий 'anna'
        current_shift = schedule.shifts.current_shifts().first()
        assert current_shift.staff.login == 'emma'
        next_shift = schedule.shifts.future().order_by('start_datetime').first()
        next_shift_id = next_shift.id

        response = client.json.patch(
            reverse(f'{api}:duty-schedule-detail', args=[schedule.pk]),
            data={
                'start_with': 'anna',
            }
        )
        assert response.status_code == 200

        schedule.refresh_from_db()
        next_shift = schedule.shifts.future().order_by('start_datetime').first()
        assert next_shift.staff.login == 'anna'
        # считаем, что данный оффсет не изменился, тк длина 5
        # 1-4 = 5 => оффсеты 1 и -4 ссылаются на один и тот же стафф в порядке
        assert (
            schedule.manual_ordering_offset == old_offset or
            schedule.manual_ordering_offset == old_offset - len(logins)
        )
        assert next_shift.id == next_shift_id
        offset = schedule.manual_ordering_offset

        # если следующий кто-то другой
        response = client.json.patch(
            reverse(f'{api}:duty-schedule-detail', args=[schedule.pk]),
            data={
                'start_with': 'bob',
            }
        )
        assert response.status_code == 200

        schedule.refresh_from_db()
        next_shift = schedule.shifts.future().order_by('start_datetime').first()
        assert next_shift.staff.login == 'bob'
        assert schedule.manual_ordering_offset == offset + 1     # оффсет изменился, сместился на 1 логин вправо
        assert next_shift.id != next_shift_id   # id-шник тоже


@freeze_time('2020-12-01')
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=5))
@pytest.mark.parametrize('api', ('api-v3', 'api-v4'))
def test_schedule_edit_order_and_start_with(client, owner_role, person, api, transactional_db):
    """
    В этом тесте замокали период, на который создаются смены:
        * вместо полгода - 5 дней (берём короткие смены по 1 дню).

    Проверим поряжок, если передан и orders, и start_with.
    Если последовательность порядка остаётся такой же и передан start_with == следующему дежурному,
    то пересчета глобального быть не должно.
    Есди start_with != следующему, делаем пересчет с удалением смен
    """

    logins = ['anna', 'bob', 'cole', 'diana', 'emma', ]
    service, schedule = prepare_data('2020-11-25', logins, owner_role, person)
    current_shift = schedule.shifts.current_shifts().first()
    assert current_shift.staff.login == 'bob'

    client.login(service.owner.login)
    # проапдейтим первый раз и получим ненулевого оффсета
    # start_with не равен следующему
    response = client.json.patch(
        reverse(f'{api}:duty-schedule-detail', args=[schedule.pk]),
        data={
            'orders': ['bob', 'cole', 'diana', 'emma', 'anna'],
            'start_with': 'diana',
        }
    )
    assert response.status_code == 200

    schedule.refresh_from_db()
    next_shift = schedule.shifts.future().order_by('start_datetime').first()
    assert next_shift.staff.login == 'diana'

    with freeze_time('2020-12-03'):
        # текущим будет 'emma' => следующий 'anna'
        current_shift = schedule.shifts.current_shifts().first()
        assert current_shift.staff.login == 'emma'

        next_shift = schedule.shifts.future().order_by('start_datetime').first()
        next_shift_id = next_shift.id
        assert next_shift.staff.login == "anna"

        # start_with равен следующему
        response = client.json.patch(
            reverse(f'{api}:duty-schedule-detail', args=[schedule.pk]),
            data={
                'orders': logins,
                'start_with': 'anna',
            }
        )
        assert response.status_code == 200

        schedule.refresh_from_db()
        next_shift = schedule.shifts.future().order_by('start_datetime').first()
        assert next_shift.staff.login == 'anna'
        assert next_shift.id == next_shift_id


@freeze_time('2020-12-01')
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=5))
@pytest.mark.parametrize('api', ('api-v3', 'api-v4'))
def test_schedule_edit_order_and_start_with_other_order(client, owner_role, person, api, transactional_db):
    """
    В этом тесте замокали период, на который создаются смены:
        * вместо полгода - 5 дней (берём короткие смены по 1 дню).

    Проверим порядок, если передан и orders, и start_with.
    Если передан совсем другой порядок, а start_with совпадает, то пересчет полный должен быть все ранво
    """

    logins = ['anna', 'bob', 'cole', 'diana', 'emma', ]
    service, schedule = prepare_data('2020-11-25', logins, owner_role, person)
    current_shift = schedule.shifts.current_shifts().first()
    assert current_shift.staff.login == 'bob'

    client.login(service.owner.login)
    # проапдейтим нулевой оффсет
    response = client.json.patch(
        reverse(f'{api}:duty-schedule-detail', args=[schedule.pk]),
        data={
            'orders': ['bob', 'diana', 'anna', 'cole', 'emma'],     # порядок потерял последовательность
            'start_with': 'cole',   # следующий совпадает
        }
    )
    assert response.status_code == 200

    schedule.refresh_from_db()
    future_shifts = list(schedule.shifts.future().order_by('start_datetime'))
    next_shift_1 = future_shifts[0]
    next_shift_2 = future_shifts[1]
    assert next_shift_1.staff.login == 'cole'
    assert next_shift_2.staff.login == 'emma'

    assert schedule.manual_ordering_offset != 0

    # теперь проверим при ненулевом оффсете
    with freeze_time('2020-12-03'):
        # текущим будет 'emma' => следующий 'bob'
        current_shift = schedule.shifts.current_shifts().first()
        assert current_shift.staff.login == 'emma'

        response = client.json.patch(
            reverse(f'{api}:duty-schedule-detail', args=[schedule.pk]),
            data={
                'orders': logins,
                'start_with': 'bob',
            }
        )
        assert response.status_code == 200

        schedule.refresh_from_db()
        future_shifts = list(schedule.shifts.future().order_by('start_datetime'))
        next_shift_1 = future_shifts[0]
        next_shift_2 = future_shifts[1]
        assert next_shift_1.staff.login == 'bob'
        assert next_shift_2.staff.login == 'cole'


@freeze_time('2021-02-02')
@override_settings(DUTY_SCHEDULING_PERIOD=timezone.timedelta(days=18))
@pytest.mark.parametrize('api', ('api-v3', 'api-v4'))
def test_schedule_edit_start_date_and_staff(client, owner_role, person, api, transactional_db):
    """
    Смены рассчитываются правильно при изменении списка дежурных и переносе даты начала дежурств в будущее.
    """
    logins = ['anna', 'eugene', 'bob', 'cole', 'diana', 'emma']
    service, schedule = prepare_data('2021-02-01', logins, owner_role, person, duration=3)
    assert schedule.shifts.current_shifts().first().staff.login == 'anna'

    client.login(service.owner.login)
    response = client.json.patch(
        reverse(f'{api}:duty-schedule-detail', args=[schedule.pk]),
        data={
            'start_date': '2021-02-08',
            'duration': datetime.timedelta(days=2).total_seconds(),
            'orders': ['bob', 'cole', 'diana', 'emma', 'anna'],
        }
    )
    assert response.status_code == 200
    schedule.refresh_from_db()
    future_shifts = list(s.staff.login for s in schedule.shifts.future().order_by('start_datetime'))
    assert future_shifts[:5] == ['bob', 'cole', 'diana', 'emma', 'anna']
