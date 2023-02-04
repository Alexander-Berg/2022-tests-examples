import datetime
from fastapi import status
import pytest

from watcher.logic.timezone import now, localize
from watcher.crud.shift import query_all_shifts_by_schedule
from watcher.db import Shift


def test_upload_shifts_empty_schedule(
    client, schedule_factory, staff_factory, scope_session,
    assert_count_queries, service_factory, member_factory
):
    service = service_factory()
    schedule = schedule_factory(recalculate=False, service=service)
    staff = staff_factory()
    member_factory(staff=staff, service=service)
    start = now()
    end_first = localize((start + datetime.timedelta(days=1)))
    end = localize((end_first + datetime.timedelta(days=2)))
    initial_data = {
        'schedule_id': schedule.id,
        'shifts': [
            {
                'start': start.isoformat(),
                'end': end_first.isoformat(),
                'empty': False,
                'staff_login': staff.login,
            },
            {
                'start': end_first.isoformat(),
                'end': end.isoformat(),
                'empty': False,
                'staff_login': staff.login,
            },
        ]
    }
    with assert_count_queries(7):
        # 1 select intranet_staff(auth_user)
        # 1 select schedule
        # 1 select existing shifts
        # 1 select intranet_staff by input logins
        # 1 select services_servicemember (check that staff is in service)
        # insert 2 shifts (bulk)
        # 1 update shift next
        response = client.post(
            '/api/watcher/v1/shift/upload',
            json=initial_data,
        )
    assert response.status_code == status.HTTP_204_NO_CONTENT, response.text
    shifts = query_all_shifts_by_schedule(db=scope_session, schedule_id=schedule.id).all()
    assert shifts[0].start.timestamp() == start.timestamp()
    assert shifts[1].start.timestamp() == end_first.timestamp()
    assert shifts[1].end.timestamp() == end.timestamp()
    assert len(shifts) == 2
    assert shifts[0].next == shifts[1]
    assert not shifts[1].next
    assert shifts[0].staff_id == shifts[1].staff_id == staff.id


@pytest.mark.parametrize('have_next', (True, False))
@pytest.mark.parametrize('have_prev', (True, False))
def test_upload_shifts_not_empty_schedule(
    client, schedule_factory, staff_factory, scope_session,
    shift_factory, have_next, have_prev
):
    schedule = schedule_factory(recalculate=False)
    prev_shift = shift_factory(schedule=schedule)
    shift_to_change = shift_factory(
        start=prev_shift.end,
        end=prev_shift.end+datetime.timedelta(hours=2),
        schedule=schedule
    )
    next_shift = shift_factory(
        start=shift_to_change.end,
        end=shift_to_change.end+datetime.timedelta(hours=2),
        schedule=schedule
    )
    if have_prev:
        prev_shift.next = shift_to_change
    if have_next:
        shift_to_change.next = next_shift
    scope_session.commit()

    initial_data = {
        'schedule_id': schedule.id,
        'replace': True,
        'shifts': [
            {
                'start': shift_to_change.start.isoformat(),
                'end': (shift_to_change.start+datetime.timedelta(hours=1)).isoformat(),
                'empty': True,
            },
        ]
    }

    response = client.post(
        '/api/watcher/v1/shift/upload',
        json=initial_data,
    )
    assert response.status_code == 204, response.text
    shifts = scope_session.query(Shift).all()
    ids = set()
    for shift in shifts:
        ids.add(shift.id)
        if shift.id != prev_shift.id and shift.id != next_shift.id:
            new_shift_id = shift.id
    new_shift = scope_session.query(Shift).get(new_shift_id)
    assert prev_shift.id in ids
    assert shift_to_change.id not in ids
    assert next_shift.id in ids
    if have_next:
        assert new_shift.next == next_shift
    else:
        assert not new_shift.next
    if have_prev:
        assert new_shift.prev == prev_shift
    else:
        assert not new_shift.prev


@pytest.mark.parametrize('replace', (True, False))
def test_upload_shifts_with_replace_parameter(client, schedule_factory, scope_session, shift_factory, replace):
    schedule = schedule_factory(recalculate=False)
    existing_shift = shift_factory(schedule=schedule)
    initial_data = {
        'schedule_id': schedule.id,
        'replace': replace,
        'shifts': [
            {
                'start': existing_shift.start.isoformat(),
                'end': existing_shift.end.isoformat(),
                'empty': True,
            },
        ]
    }
    response = client.post(
        '/api/watcher/v1/shift/upload',
        json=initial_data,
    )
    if replace:
        assert response.status_code == 204, response.text
    else:
        assert response.status_code == 400
        assert response.json()['context']['message'] == {
            'ru': 'Для перезаписи существующих смен установите параметр replace=True',
            'en': 'To overwrite existing shifts, set the replace=True parameter'
        }


@pytest.mark.parametrize('future', (True, False))
@pytest.mark.parametrize('recalculate', (True, False))
def test_upload_future_shifts(client, schedule_factory, scope_session, recalculate, future):
    schedule = schedule_factory(recalculate=recalculate)
    if future:
        start = now()
    else:
        start = now() - datetime.timedelta(days=1)
    initial_data = {
        'schedule_id': schedule.id,
        'shifts': [
            {
                'start': start.isoformat(),
                'end': (start + datetime.timedelta(hours=1)).isoformat(),
                'empty': True,
            },
        ]
    }
    response = client.post(
        '/api/watcher/v1/shift/upload',
        json=initial_data,
    )
    if future and recalculate:
        assert response.status_code == 400
        assert response.json()['context']['message'] == {
            'ru': 'Нельзя загрузить будущие смены, т.к. график участвует в пересчётах смен и перераспределении '
                  'людей',
            'en': 'It is impossible to load future shifts, because the schedule is involved in '
                  'the recalculation of shifts and the redistribution of people',
        }
    else:
        assert response.status_code == 204, response.text


def test_upload_inconsistent_shifts(client, schedule_factory, scope_session):
    schedule = schedule_factory(recalculate=False)
    start = now()
    initial_data = {
        'schedule_id': schedule.id,
        'shifts': [
            {
                'start': start.isoformat(),
                'end': (start + datetime.timedelta(hours=1)).isoformat(),
                'empty': True,
            },
            {
                'start': (start + datetime.timedelta(hours=2)).isoformat(),
                'end': (start + datetime.timedelta(hours=4)).isoformat(),
                'empty': True,
            },
            {
                'start': (start + datetime.timedelta(hours=3)).isoformat(),
                'end': (start + datetime.timedelta(hours=5)).isoformat(),
                'empty': True,
            },
        ]
    }
    response = client.post(
        '/api/watcher/v1/shift/upload',
        json=initial_data,
    )
    assert response.status_code == 204, response.text
    shifts = scope_session.query(Shift).order_by(Shift.start).all()
    assert shifts[0].next == shifts[1]
    assert shifts[1].next == shifts[2]
    assert shifts[0].end < shifts[1].start
    assert shifts[1].start < shifts[2].start < shifts[1].end
    assert shifts[2].end > shifts[1].end


@pytest.mark.parametrize('empty', (True, False))
@pytest.mark.parametrize('have_staff', (True, False))
def test_upload_empty_shifts(
    client, schedule_factory, scope_session, empty,
    have_staff, staff_factory, service_factory, member_factory
):
    service = service_factory()
    schedule = schedule_factory(recalculate=False, service=service)
    if have_staff:
        staff = staff_factory()
        staff_login = staff.login
        member_factory(staff=staff, service=service)
    else:
        staff_login = None
    initial_data = {
        'schedule_id': schedule.id,
        'shifts': [
            {
                'start': now().isoformat(),
                'end': (now() + datetime.timedelta(hours=1)).isoformat(),
                'empty': empty,
                'staff_login': staff_login
            },
        ]
    }
    response = client.post(
        '/api/watcher/v1/shift/upload',
        json=initial_data,
    )
    if empty and have_staff:
        assert response.status_code == 400
        assert response.json()['context']['message'] == {
            'ru': 'У пустой смены не может быть дежурного',
            'en': 'An empty shift cant have a staff'
        }

    elif not empty and not have_staff:
        assert response.status_code == 400
        assert response.json()['context']['message'] == {
            'ru': 'У непустой смены должен быть дежурный',
            'en': 'An non-empty shift must have a staff'
        }
    else:
        assert response.status_code == 204, response.text


@pytest.mark.parametrize('slot_in_schedule', (True, False))
def test_upload_shifts_wrong_slot(
    client, schedule_factory, scope_session, staff_factory, slot_factory, slot_in_schedule, interval_factory
):
    schedule = schedule_factory(recalculate=False)
    slot = slot_factory()
    if slot_in_schedule:
        slot.interval.schedule_id = schedule.id
        scope_session.commit()
    initial_data = {
        'schedule_id': schedule.id,
        'shifts': [
            {
                'start': now().isoformat(),
                'end': (now() + datetime.timedelta(hours=1)).isoformat(),
                'empty': True,
                'slot_id': slot.id
            },
        ]
    }
    response = client.post(
        '/api/watcher/v1/shift/upload',
        json=initial_data,
    )
    if slot_in_schedule:
        assert response.status_code == 204, response.text
    else:
        assert response.status_code == 400
        assert response.json()['context']['message'] == {
            'ru': 'Slot не принадлежит расписанию',
            'en': 'The slot does not belong to the schedule',
        }


@pytest.mark.parametrize('staff_in_service', (True, False))
def test_upload_shifts_staff_not_in_service(
    client, schedule_factory, staff_factory, scope_session,
    staff_in_service, service_factory, member_factory
):
    service = service_factory()
    schedule = schedule_factory(recalculate=False, service=service)
    staff = staff_factory()
    if staff_in_service:
        member_factory(staff=staff, service=service)
    initial_data = {
        'schedule_id': schedule.id,
        'shifts': [
            {
                'start': now().isoformat(),
                'end': (now() + datetime.timedelta(hours=1)).isoformat(),
                'staff_login': staff.login
            },
        ]
    }
    response = client.post(
        '/api/watcher/v1/shift/upload',
        json=initial_data,
    )
    if staff_in_service:
        assert response.status_code == 204, response.text
    else:
        assert response.status_code == 400
        assert response.json()['context']['message'] == {
            'ru': 'Не все переданные сотрудники состоят в сервисе',
            'en': 'Not all employees are members of service',
        }


@pytest.mark.parametrize('wrong_login', (True, False))
def test_upload_shifts_staff_wrong_login(
    client, schedule_factory, staff_factory, scope_session,
    wrong_login, service_factory, member_factory
):
    service = service_factory()
    schedule = schedule_factory(recalculate=False, service=service)
    staff = staff_factory()
    member_factory(staff=staff, service=service)
    if wrong_login:
        login = 'smth_wrong'
    else:
        login = staff.login
    initial_data = {
        'schedule_id': schedule.id,
        'shifts': [
            {
                'start': now().isoformat(),
                'end': (now() + datetime.timedelta(hours=1)).isoformat(),
                'staff_login': login
            },
        ]
    }
    response = client.post(
        '/api/watcher/v1/shift/upload',
        json=initial_data,
    )
    if not wrong_login:
        assert response.status_code == 204, response.text
    else:
        assert response.status_code == 400
        assert response.json()['context']['message'] == {
            'ru': 'Присутствуют неизвестные логины',
            'en': 'Some logins are not found',
        }


@pytest.mark.parametrize('shift_is_primary', (True, False, None))
@pytest.mark.parametrize('slot_is_primary', (True, False, None))
def test_upload_shifts_is_primary(
    client, schedule_factory, staff_factory, scope_session,
    shift_factory, interval_factory, slot_factory, shift_is_primary, slot_is_primary
):
    schedule = schedule_factory(recalculate=False)
    interval = interval_factory(schedule=schedule)
    slot_id = None
    if slot_is_primary is not None:
        slot_id = slot_factory(interval=interval, is_primary=slot_is_primary).id
    now_ = now()
    initial_data = {
        'schedule_id': schedule.id,
        'replace': True,
        'shifts': [
            {
                'start': now_.isoformat(),
                'end': (now_ + datetime.timedelta(hours=1)).isoformat(),
                'empty': True,
                'is_primary': shift_is_primary,
                'slot_id': slot_id
            },
        ]
    }

    response = client.post(
        '/api/watcher/v1/shift/upload',
        json=initial_data,
    )
    assert response.status_code == 204, response.text
    shift = scope_session.query(Shift).all()[0]
    if shift_is_primary is None:
        if slot_is_primary is not None:
            assert shift.is_primary == shift.slot.is_primary
        else:
            assert shift.is_primary
    else:
        assert shift.is_primary == shift_is_primary
