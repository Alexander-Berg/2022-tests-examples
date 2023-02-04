import datetime

import copy
import pytest

from freezegun import freeze_time
from fastapi import status
from unittest.mock import patch

from watcher.crud.shift import query_all_shifts_by_schedule, query_current_shifts_by_schedule
from watcher.logic.timezone import now
from watcher.tasks.generating_shifts import initial_creation_of_shifts
from watcher.tasks.people_allocation import start_people_allocation
from watcher.tasks.shift import start_shift, start_shifts
from watcher.db import Rating
from watcher import enums


def test_list_shift(client, staff_factory, shift_factory, scope_session):
    staff = staff_factory()
    start = now()
    shift_one = shift_factory(staff=staff, start=start + datetime.timedelta(minutes=10))
    shift_two = shift_factory(staff=staff, start=start, slot=shift_one.slot)
    shift_three = shift_factory(
        staff=staff, slot=shift_one.slot,
        start=start + datetime.timedelta(minutes=10),
        end=start + datetime.timedelta(minutes=20),
    )

    expected = [shift_two, shift_three, shift_one]
    response = client.get(
        '/api/watcher/v1/shift/'
    )
    assert response.status_code == 200, response.text
    data = response.json()['result']
    assert len(data) == 3
    assert {obj['id'] for obj in data} == {obj.id for obj in expected}
    assert {obj['staff']['uid'] for obj in data} == {staff.uid}


def test_list_shift_all_fields(
    client, staff_factory, assert_count_queries,
    shift_factory, scope_session,
):
    staff = staff_factory()

    main_shift = shift_factory(staff=staff)
    replace_shift = shift_factory(
        replacement_for=main_shift,
        slot=main_shift.slot,
        schedule=main_shift.schedule,
        staff=staff_factory(),
    )
    with assert_count_queries(2):
        # стафф
        # сами данные
        response = client.get(
            '/api/watcher/v1/shift/all_fields'
        )
    assert response.status_code == 200, response.text
    data = response.json()['result']
    assert len(data) == 1
    shift_data = data[0]
    assert shift_data['id'] == replace_shift.id
    assert shift_data['schedule']['slug'] == replace_shift.schedule.slug
    assert shift_data['schedule']['id'] == replace_shift.schedule.id
    assert shift_data['schedule']['service']['slug'] == replace_shift.schedule.service.slug
    assert shift_data['schedule']['service']['id'] == replace_shift.schedule.service.id


def test_list_shift_is_primary(client, assert_count_queries, slot_factory, staff_factory, shift_factory, scope_session):
    staff = staff_factory()
    slot_primary = slot_factory(is_primary=True)
    shift_primary = shift_factory(staff=staff, slot=slot_primary)
    slot = slot_factory(is_primary=False, interval=slot_primary.interval)
    shift = shift_factory(staff=staff, slot=slot)

    with assert_count_queries(2):
        # стафф
        # сами данные
        response = client.get(
            '/api/watcher/v1/shift/'
        )
    assert response.status_code == 200, response.text
    data = response.json()['result']

    assert len(data) == 2

    assert data[0]['id'] == shift.id
    assert data[0]['staff']['uid'] == staff.uid
    assert data[0]['slot']['is_primary'] == shift.slot.is_primary
    assert data[0]['slot']['show_in_staff'] == shift.slot.is_primary

    assert data[1]['id'] == shift_primary.id
    assert data[1]['staff']['uid'] == staff.uid
    assert data[1]['slot']['is_primary'] == shift_primary.slot.is_primary


@pytest.mark.parametrize('filtering_by_start', (True, False))
@pytest.mark.parametrize('filtering_by_end', (True, False))
def test_list_shift_with_subshifts(
    scope_session, schedule_data, client, shift_factory, staff_factory,
    filtering_by_start, filtering_by_end
):
    schedule = schedule_data.schedule
    with patch('watcher.tasks.people_allocation.start_people_allocation'):
        initial_creation_of_shifts(schedule.id)
    shift = query_all_shifts_by_schedule(db=scope_session, schedule_id=schedule.id).first()

    start = shift.start
    count = 0
    while start < shift.end:
        end = start + datetime.timedelta(days=1)
        if end > shift.end:
            end = shift.end
        shift_factory(
            staff=staff_factory(),
            start=start,
            end=end,
            slot=shift.slot,
            schedule=schedule,
            empty=False,
            approved=True,
            replacement_for=shift,
        )
        start = end
        count += 1

    # выгружаем данные:
    #   - старт меньше енда
    #   - енд больше старта
    params_filter = [f'schedule_id={schedule.id}']

    if filtering_by_start:
        filter_start = shift.end.date().isoformat()
        params_filter.append(f'start__lt={filter_start}')

    if filtering_by_end:
        filter_end = shift.start.date().isoformat()
        params_filter.append(f'end__gt={filter_end}')

    response = client.get(
        '/api/watcher/frontend/shift/',
        params={'filter': ','.join(params_filter)},
    )
    assert response.status_code == 200, response.text
    data = response.json()['result']

    if not filtering_by_start:
        assert len(data) > count

    else:
        assert len(data) == count

        start = shift.start
        for sub_shift in reversed(data):
            end = start + datetime.timedelta(days=1)
            assert sub_shift['start'] == start.isoformat()
            assert sub_shift['end'] == end.isoformat()
            start = end

        assert end == shift.end


def test_list_shift_for_abc(
    client, staff_factory, assert_count_queries,
    shift_factory, scope_session,
):
    staff = staff_factory()

    main_shift = shift_factory(staff=staff)
    replace_shift = shift_factory(
        replacement_for=main_shift,
        slot=main_shift.slot,
        schedule=main_shift.schedule,
        staff=staff_factory(),
    )
    with assert_count_queries(2):
        # стафф
        # сами данные
        response = client.get(
            '/api/watcher/v1/shift/for_abc'
        )
    assert response.status_code == 200, response.text
    data = response.json()['result']
    assert len(data) == 1
    shift_data = data[0]
    assert shift_data['id'] == replace_shift.id
    assert shift_data['schedule']['id'] == replace_shift.schedule.id
    assert shift_data['schedule']['slug'] == replace_shift.schedule.slug
    assert shift_data['schedule']['service_id'] == replace_shift.schedule.service_id
    assert shift_data['staff_id'] == replace_shift.staff_id
    assert shift_data['staff']['login'] == replace_shift.staff.login
    assert shift_data['staff']['uid'] == replace_shift.staff.uid


def test_get_shift(client, shift_factory):
    main_shift = shift_factory()
    shift = shift_factory(
        replacement_for=main_shift,
        schedule=main_shift.schedule,
        slot=main_shift.slot,
    )
    response = client.get(
        f'/api/watcher/v1/shift/{shift.id}',
    )
    assert response.status_code == 200, response.text
    data = response.json()

    assert data['replacement_for']['id'] == main_shift.id
    assert data['slot']['is_primary'] == main_shift.slot.is_primary

    expected = {
        'id': shift.id,
        'approved': False,
        'approved_at': None,
        'status': 'scheduled',
        'slot_id': shift.slot_id,
    }
    for key, value in expected.items():
        assert data[key] == value


def test_get_shift_subshifts(client, shift_factory, staff_factory, assert_count_queries):
    main_shift = shift_factory()
    subshifts = [
        shift_factory(
            replacement_for=main_shift,
            schedule=main_shift.schedule,
            slot=main_shift.slot,
            staff=staff_factory()
        ) for _ in range(3)
    ]

    shift_id = main_shift.id
    with assert_count_queries(2):
        # select intranet_staff.uid = 123
        # select subshifts where replacement_for_id = shift.id joined
        response = client.get(
            f'/api/watcher/v1/shift/{shift_id}/subshifts',
        )
    assert response.status_code == 200, response.text
    data = response.json()

    assert len(data) == len(subshifts)
    expected_ids = {shift.id for shift in subshifts}
    assert {obj['id'] for obj in data} == expected_ids


def test_list_shift_filter_by_id(client, shift_factory):
    expected = [shift_factory() for _ in range(2)]
    obj = expected[0]
    response = client.get(
        '/api/watcher/v1/shift/',
        params={'filter': f'id={obj.id}'},
    )
    assert response.status_code == 200, response.text
    data = response.json()['result']

    assert len(data) == 1
    assert data[0]['id'] == obj.id


def test_list_shift_filter_by_approved(client, shift_factory):
    shift = shift_factory(approved=True)
    shift_factory(
        approved=False,
        slot=shift.slot,
        schedule=shift.schedule,
    )
    response = client.get(
        '/api/watcher/v1/shift/',
        params={'filter': 'approved=True'},
    )
    assert response.status_code == 200, response.text
    data = response.json()['result']

    assert len(data) == 1
    assert data[0]['id'] == shift.id


def test_list_shift_filter_in_id(client, shift_factory):
    expected = [shift_factory() for _ in range(2)]
    shift_factory()
    response = client.get(
        '/api/watcher/v1/shift/',
        params={'filter': f'id={expected[0].id},id={expected[1].id}'},
    )
    assert response.status_code == 200, response.text
    data = response.json()['result']

    assert len(data) == 2
    assert {obj['id'] for obj in data} == {obj.id for obj in expected}


def test_list_shift_filter_gte_id(client, shift_factory):
    expected = [shift_factory() for _ in range(10)]
    response = client.get(
        '/api/watcher/v1/shift/',
        params={'filter': f'id__gte={expected[5].id},id__lte={expected[-1].id}'},
    )
    assert response.status_code == 200, response.text
    data = response.json()['result']

    assert len(data) == 5
    assert {obj['id'] for obj in data} == {obj.id for obj in expected[5:]}


@pytest.mark.parametrize('order', ['asc', 'desc'])
def test_list_shift_order_by(client, shift_factory, order):
    expected = [shift_factory() for _ in range(2)]
    if order == 'asc':
        order_by = 'id'
        expected_ids = [expected[0].id, expected[1].id]
    else:
        order_by = '-id'
        expected_ids = [expected[1].id, expected[0].id]

    response = client.get(
        '/api/watcher/v1/shift/',
        params={
            'filter': f'id={expected[0].id},id={expected[1].id}',
            'order_by': order_by
        },
    )
    assert response.status_code == 200, response.text
    data = response.json()['result']

    assert [obj['id'] for obj in data] == expected_ids


def test_patch_shift_empty_with_staff_id(client, shift_factory, staff_factory, scope_session):
    shift = shift_factory(staff=staff_factory(), empty=False, approved=True)
    response = client.patch(
        f'/api/watcher/v1/shift/{shift.id}',
        json={
            'staff_id': shift.staff_id,
            'empty': True,
            'approved': shift.approved
        }
    )
    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response.json()['context']['message'] == {
        'ru': 'В пустых сменах не должно быть дежурных',
        'en': 'There should be no attendants in the empty shifts'
    }


def test_patch_shift_set_approved(client, shift_factory, staff_factory, scope_session):
    shift = shift_factory(staff=staff_factory(), empty=False, approved=False)
    response = client.patch(
        f'/api/watcher/v1/shift/{shift.id}',
        json={
            'approved': True
        }
    )
    assert response.status_code == status.HTTP_200_OK, response.text
    scope_session.refresh(shift)
    assert shift.staff_id is not None


@pytest.mark.parametrize('approved', [True, False])
@pytest.mark.parametrize('empty', [True, False])
@pytest.mark.parametrize('has_staff', [True, False])
def test_null_patch(client, shift_factory, staff_factory, approved, empty, has_staff, scope_session):
    shift = shift_factory(approved=approved, empty=empty)
    if has_staff:
        shift.staff = staff_factory()
        scope_session.commit()
        scope_session.refresh(shift)

    response = client.patch(
        f'/api/watcher/v1/shift/{shift.id}',
        json={
            'staff_id': shift.staff_id
        }
    )

    scope_session.refresh(shift)

    if empty and has_staff:
        assert response.status_code == status.HTTP_400_BAD_REQUEST
    else:
        assert response.status_code == status.HTTP_200_OK

    assert shift.approved == approved
    assert shift.empty == empty
    assert (shift.staff is not None) == has_staff


def test_patch_shift_empty_change(client, shift_factory, staff_factory, scope_session):
    shift = shift_factory(staff=staff_factory(), empty=False)
    response = client.patch(
        f'/api/watcher/v1/shift/{shift.id}',
        json={
            'staff_id': None,
            'empty': True,
            'approved': shift.approved
        }
    )
    assert response.status_code == status.HTTP_200_OK, response.text

    data = response.json()
    assert not data['staff_id']
    assert data['empty']

    scope_session.refresh(shift)
    assert not shift.staff
    assert shift.empty


@pytest.mark.parametrize('approved', [True, False])
@pytest.mark.parametrize('different_staff', [True, False])
def test_patch_shift_approved_change(client, shift_factory, staff_factory, scope_session, approved, different_staff):
    shift = shift_factory(staff=staff_factory(), approved=not approved, empty=False)
    staff_id = staff_factory().id if different_staff else shift.staff_id
    with patch('watcher.tasks.people_allocation.start_people_allocation.delay') as people_allocation_func:
        with freeze_time('2018-01-01'):
            changed_time = now()
            response = client.patch(
                f'/api/watcher/v1/shift/{shift.id}',
                json={
                    'staff_id': staff_id,
                    'empty': shift.empty,
                    'approved': approved
                }
            )

    approved_flag = (True if different_staff else approved)

    assert response.status_code == status.HTTP_200_OK, response.text
    assert response.json()['approved'] == approved_flag

    scope_session.refresh(shift)
    assert shift.approved == approved_flag

    if different_staff:
        people_allocation_func.assert_called_once()
    else:
        people_allocation_func.assert_not_called()

    if different_staff and not approved:
        # меняется дежурный, флаг approved - остается True
        pass
    else:
        changed_by_id_attr = 'approved_by_id' if approved_flag else 'approved_removed_by'
        changed_time_attr = 'approved_at' if approved_flag else 'approved_removed_at'

        assert getattr(shift, changed_by_id_attr)
        assert getattr(shift, changed_time_attr).date() == changed_time.date()


def test_patch_shift_add_staff_id_for_empty_shift(client, shift_factory, staff_factory, scope_session):
    shift = shift_factory(empty=True)
    response = client.patch(
        f'/api/watcher/v1/shift/{shift.id}',
        json={
            'staff_id': staff_factory().id,
            'empty': shift.empty,
            'approved': shift.approved
        }
    )
    assert response.status_code == status.HTTP_400_BAD_REQUEST, response.text
    assert response.json()['context']['message'] == {
        'ru': 'В пустых сменах не должно быть дежурных',
        'en': 'There should be no attendants in the empty shifts'
    }


@pytest.mark.parametrize('past', (True, False))
@pytest.mark.parametrize('has_rating', (True, False))
@pytest.mark.parametrize('in_composition', (True, False))
@pytest.mark.parametrize('completed', (True, False))
def test_patch_shift_staff_id_change(
    client, shift_factory,
    staff_factory, scope_session, past,
    composition_participants_factory,
    schedule_factory, rating_factory,
    has_rating, in_composition, completed,
):
    """
    Проверим что при замене человека в смене корректно обрабатываем рейтинг
    - если смена в прошлом - обновим рейтинг в базе и в будущих шифтах
    - если смена в будущем - обновим рейтинг в будущих шифтах

    при этом если смена не в статусе completed - значит мы рейтинг в базе
    дежурному, который до этого был не добавляли, значит и теперь не стоит отнимать
    :return:
    """
    participant = composition_participants_factory()
    watcher = participant.staff
    kwargs = {}
    if in_composition:
        kwargs['composition'] = participant.composition
    participant_1 = composition_participants_factory(**kwargs)
    left_dude = participant_1.staff
    schedule = schedule_factory(service=participant.composition.service)
    if has_rating:
        rating_factory(schedule=schedule, staff=watcher, rating=42)
    if past:
        kwargs = {
            'start': now() - datetime.timedelta(hours=4),
            'end': now() - datetime.timedelta(hours=2),
        }
    else:
        kwargs = {'end': now() + datetime.timedelta(hours=2)}

    if completed:
        kwargs['status'] = enums.ShiftStatus.completed

    shift = shift_factory(
        schedule=schedule,
        staff=watcher,
        **kwargs
    )
    shift.slot.composition = participant.composition
    next_shift = shift_factory(
        slot=shift.slot,
        prev=shift,
        predicted_ratings={watcher.login: 42, left_dude.login: 0},
        schedule=shift.schedule,
        approved=False,
    )

    response = client.patch(
        f'/api/watcher/v1/shift/{shift.id}',
        json={
            'staff_id': left_dude.id
        }
    )
    assert response.status_code == status.HTTP_200_OK, response.text
    assert response.json()['staff_id'] == left_dude.id

    scope_session.refresh(shift)
    assert shift.staff_id == left_dude.id
    assert shift.approved is True
    rating = scope_session.query(Rating).filter(Rating.staff_id == watcher.id).first()
    rating_1 = scope_session.query(Rating).filter(Rating.staff_id == left_dude.id).first()

    if past:
        if completed:
            assert float(rating.rating) == (40.0 if has_rating else 0.0)
        else:
            if has_rating:
                assert float(rating.rating) == 42.0
            else:
                assert rating is None
        if in_composition:
            assert float(rating_1.rating) == 2.0
        else:
            assert rating_1 is None
    else:
        if has_rating:
            assert float(rating.rating) == 42.0
        else:
            assert rating is None
        assert rating_1 is None

    scope_session.refresh(next_shift)
    assert next_shift.predicted_ratings == {watcher.login: 40, left_dude.login: 2}


def test_patch_off_empty_and_set_staff_id(client, shift_factory, staff_factory, scope_session):
    watcher = staff_factory()
    shift = shift_factory(empty=True)
    response = client.patch(
        f'/api/watcher/v1/shift/{shift.id}',
        json={
            'staff_id': watcher.id,
            'approved': False,
            'empty': False,
        }
    )
    assert response.status_code == status.HTTP_200_OK, response.text
    assert response.json()['staff_id'] == watcher.id
    assert response.json()['approved'] is True
    assert not response.json()['empty']

    scope_session.refresh(shift)
    assert shift.staff_id == watcher.id
    assert not shift.empty


def test_list_shift_no_main_with_replacements(client, shift_factory):
    main_shift = shift_factory()
    sub_shift = shift_factory(replacement_for_id=main_shift.id)
    main_shift_with_no_replacements = shift_factory()

    response = client.get(
        '/api/watcher/v1/shift/',
    )
    assert response.status_code == 200, response.text
    data = response.json()['result']

    assert {obj['id'] for obj in data} == {sub_shift.id, main_shift_with_no_replacements.id}


def test_shift_service_multiple_filters(client, shift_factory, scope_session):
    shift_factory()
    shift_1 = shift_factory()
    shift_2 = shift_factory()
    shift_3 = shift_factory()
    service_id_1 = shift_1.schedule.service_id
    service_id_2 = shift_2.schedule.service_id
    revision_id_2 = shift_2.slot.interval.revision_id
    revision_id_3 = shift_3.slot.interval.revision_id

    response = client.get(
        '/api/watcher/v1/shift/',
        params={
            'filter': ','.join([
                f'schedule.service_id={service_id_1}',
                f'schedule.service_id={service_id_2}',
                f'slot.interval.revision_id={revision_id_2}',
                f'slot.interval.revision_id={revision_id_3}',
            ])
        },
    )
    assert response.status_code == 200, response.text
    data = response.json()['result']

    assert {obj['id'] for obj in data} == {shift_2.id, }


def test_shift_service_filter_current(client, shift_factory, staff_factory):
    staff = staff_factory()
    now_datetime = now()
    shift_factory()
    shift_factory(staff=staff,
                  start=now_datetime + datetime.timedelta(hours=1),
                  end=now_datetime + datetime.timedelta(hours=2))
    shift_1 = shift_factory(staff=staff)
    shift_2 = shift_factory(staff=staff)

    response = client.get(
        '/api/watcher/v1/shift/',
        params={
            'current': 'true',
        },
    )
    assert response.status_code == 200, response.text
    data = response.json()['result']

    assert {obj['id'] for obj in data} == {shift_1.id, shift_2.id}


@pytest.mark.parametrize('staff', [True, False])
@pytest.mark.parametrize('new_staff', [True, False])
def test_patch_current_shift_staff_id(
    scope_session, schedule_data_with_composition, client,
    member_factory, staff_factory, staff, new_staff,
):
    schedule = schedule_data_with_composition.schedule
    initial_creation_of_shifts(schedule.id)

    with patch('watcher.logic.member.abc_client.request_role'):
        start_shifts()

    shift = query_current_shifts_by_schedule(db=scope_session, schedule_id=schedule.id).first()
    assert shift is not None
    assert shift.staff is not None

    if not staff:
        shift.staff_id = None
        scope_session.commit()
    else:
        member = member_factory(
            staff=shift.staff,
            role=shift.slot.role_on_duty,
            service=schedule.service,
        )
    other_staff = staff_factory()

    with patch('watcher.logic.member.abc_client.deprive_role') as deprive_role_mock:
        with patch('watcher.tasks.shift.start_shift.delay') as start_shift_mock:
            response = client.patch(
                f'/api/watcher/v1/shift/{shift.id}',
                json={
                    'staff_id': other_staff.id if new_staff else None,
                }
            )

    assert response.status_code == status.HTTP_200_OK

    if staff:
        deprive_role_mock.assert_called_once_with(
            membership_id=member.id,
        )
    start_shift_mock.assert_called_once_with(
        shift_id=shift.id,
    )

    with patch('watcher.logic.member.abc_client.request_role') as request_role_mock:
        start_shift(shift_id=shift.id)
    if new_staff:
        request_role_mock.assert_called_once_with(
            service=schedule.service_id,
            login=other_staff.login,
            role=shift.slot.role_on_duty_id,
        )


def test_get_shift_staff_ratings(client, shift_factory, staff_factory, assert_count_queries):
    staff = [staff_factory() for _ in range(3)]
    predicted_ratings = {
        staff[0].login: 10.0,
        staff[1].login: 123.123,
        staff[2].login: 20.0,
    }
    shift = shift_factory(predicted_ratings=predicted_ratings)

    shift_id = shift.id
    with assert_count_queries(3):
        # select intranet_staff.uid = 123
        # select shift joined
        # select staff with login in shift.predicted_ratings
        response = client.get(
            f'/api/watcher/v1/shift/{shift_id}/ratings',
        )

    assert response.status_code == status.HTTP_200_OK, response.text
    data = response.json()

    # ожидаем что будет отсортированно по возрастанию rating
    sorted_ratings = sorted(predicted_ratings.items(), key=lambda item: item[1])
    for i in range(len(sorted_ratings)):
        assert data[i]['rating'] == sorted_ratings[i][1]
        assert data[i]['staff']['login'] == sorted_ratings[i][0]


def test_staff_ratings_for_shift_with_disabled_ratings(
    client, scope_session, shift_factory, assert_count_queries
):
    shift = shift_factory()
    shift.slot.points_per_hour = 0
    scope_session.commit()

    shift_id = shift.id
    with assert_count_queries(2):
        # select intranet_staff.uid = 123
        # select shift joined
        response = client.get(
            f'/api/watcher/v1/shift/{shift_id}/ratings',
        )

    assert response.status_code == status.HTTP_400_BAD_REQUEST, response.text
    assert response.json()['detail'][0]['msg'] == {
        'ru': 'Для этой смены рейтинг отключен.',
        'en': 'Rating is disabled for this shift.',
    }


def test_put_rating_change(client, scope_session, staff_factory, schedule_factory, composition_factory,
                           composition_participants_factory, interval_factory, slot_factory, shift_factory):
    staff_list = [staff_factory() for _ in range(5)]
    schedule = schedule_factory()
    composition = composition_factory(service=schedule.service)

    for staff in staff_list:
        composition_participants_factory(staff=staff, composition=composition)

    interval = interval_factory(schedule=schedule)
    slot = slot_factory(composition=composition, interval=interval)
    scope_session.commit()

    cur_shift = None
    start = now()
    end = start + datetime.timedelta(hours=1)
    predicted_ratings = {staff.login: 0.0 for staff in staff_list}
    shifts = []
    for i in range(10):
        cur_staff = staff_list[i % len(staff_list)]
        cur_shift = shift_factory(
            schedule=schedule,
            slot=slot,
            prev=cur_shift,
            staff=cur_staff,
            approved=True,
            predicted_ratings=copy.copy(predicted_ratings),
            start=start,
            end=end,
        )
        shifts.append(cur_shift)
        predicted_ratings[cur_staff.login] += 1.0
        start = end
        end = start + datetime.timedelta(hours=1)

    first_shift_staff = shifts[0].staff
    second_shift = shifts[1]
    with patch('watcher.logic.member.abc_client.deprive_role'):
        with patch('watcher.tasks.shift.start_shift.delay'):
            client.patch(
                f'/api/watcher/v1/shift/{second_shift.id}',
                json={
                    'staff_id': first_shift_staff.id,
                }
            )

    for shift in shifts:
        scope_session.refresh(shift)

    expected_ratings = {staff.login: 0.0 for staff in staff_list}
    for shift in shifts:
        assert shift.predicted_ratings == expected_ratings
        expected_ratings[shift.staff.login] += 1.0


@pytest.mark.parametrize('timespan', ['now', 'future'])
def test_shift_change_staff_check_ratings(client, scope_session, shift_sequence_data, staff_factory, member_factory,
                                          timespan, composition_participants_factory):

    schedule = shift_sequence_data.schedule
    staff_1 = shift_sequence_data.staff_1
    staff_2 = shift_sequence_data.staff_2

    if timespan == 'now':
        cur_shift = shift_sequence_data.shift
        next_shift = shift_sequence_data.next_shift
    elif timespan == 'future':
        cur_shift = shift_sequence_data.next_shift
        next_shift = shift_sequence_data.next_next_shift

    with patch('watcher.logic.member.abc_client.deprive_role'):
        with patch('watcher.tasks.people_allocation.start_people_allocation.delay'):
            response = client.patch(
                f'/api/watcher/v1/shift/{cur_shift.id}',
                json={
                    'staff_id': staff_2.id,
                }
            )

    assert response.status_code == status.HTTP_200_OK
    start_people_allocation(schedules_group_id=schedule.schedules_group_id)

    scope_session.refresh(next_shift)
    if timespan == 'now':
        assert next_shift.predicted_ratings == {staff_1.login: 96.0, staff_2.login: 48.0}
    elif timespan == 'future':
        assert next_shift.predicted_ratings == {staff_1.login: 144.0, staff_2.login: 48.0}
