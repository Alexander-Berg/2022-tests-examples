import datetime
import pretend
import pytest

from fastapi import status
from unittest.mock import patch

from watcher import enums
from watcher.crud.shift import query_current_shifts_by_schedule
from watcher.db import Shift
from watcher.logic.timezone import now
from watcher.tasks.generating_shifts import initial_creation_of_shifts
from watcher.tasks.people_allocation import start_people_allocation
from watcher.tasks.shift import start_shift, start_shifts


@pytest.fixture
def sub_shift_data(shift_factory):
    start = now()
    end = start + datetime.timedelta(days=10)

    next_shift, prev_shift = shift_factory(), shift_factory()
    shift = shift_factory(start=start, end=end)
    shift.next = next_shift
    shift.prev = prev_shift

    return pretend.stub(
        shift=shift,
        prev_shift=prev_shift,
        next_shift=next_shift,
        start=start,
        end=end,
    )


def check_subshifts_right(shift: Shift, data: dict, sub_shift_count: int) -> None:
    assert len(data) == sub_shift_count
    assert data[0]['start'] == shift.start.isoformat()
    assert data[-1]['end'] == shift.end.isoformat()
    assert all([sub_shift['slot_id'] == shift.slot_id for sub_shift in data])
    assert all([sub_shift['is_primary'] == shift.is_primary for sub_shift in data])
    assert all([sub_shift['replacement_for_id'] == shift.id for sub_shift in data])
    assert all([sub_shift['approved'] for sub_shift in data])

    for i in range(len(data)):
        assert data[i]['status'] == enums.ShiftStatus.scheduled

        if i + 1 < len(data):
            assert data[i]['end'] == data[i+1]['start']


@pytest.mark.parametrize('approved', (True, False))
@pytest.mark.parametrize('is_primary', (True, False))
def test_sub_shifts_base_creating(client, sub_shift_data, scope_session, is_primary, approved):
    shift = sub_shift_data.shift
    shift.is_primary = is_primary
    scope_session.commit()
    response = client.put(
        f'/api/watcher/v1/subshift/{shift.id}',
        json={
            'sub_shifts': [
                {
                    'start': (sub_shift_data.start + datetime.timedelta(days=2)).isoformat(),
                    'end': (sub_shift_data.start + datetime.timedelta(days=5)).isoformat(),
                    'empty': True,
                    'approved': approved
                },
            ]
        }
    )
    assert response.status_code == status.HTTP_200_OK
    check_subshifts_right(sub_shift_data.shift, response.json(), sub_shift_count=3)

    subshift = sorted(sub_shift_data.shift.sub_shifts, key=lambda x: x.start)

    assert len(subshift) == 3
    assert subshift[0].prev == sub_shift_data.prev_shift
    assert subshift[-1].next == sub_shift_data.next_shift


def test_sub_shifts_for_sub_shift(client, sub_shift_data, scope_session, shift_factory):
    shift = sub_shift_data.shift
    main_shift = shift_factory(schedule=shift.schedule, slot=shift.slot)
    shift.replacement_for_id = main_shift.id
    scope_session.commit()

    response = client.put(
        f'/api/watcher/v1/subshift/{shift.id}',
        json={
            'sub_shifts': [
                {
                    'start': (sub_shift_data.start + datetime.timedelta(days=2)).isoformat(),
                    'end': (sub_shift_data.start + datetime.timedelta(days=5)).isoformat(),
                    'empty': True,
                },
            ]
        }
    )
    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response.json()['context']['message']['ru'] == 'Нельзя создать подсмены у подсмены'


def test_subshift_full_shift(client, shift_factory, scope_session):
    """
    Проверяем поведение - что будет если для смены, у которой были замены
    передать одну на все время основной смены:
     - замены должны удалиться
     - prev/next у основной смены должны выставиться корректно
    """
    shift = shift_factory()
    main_next_shift = shift_factory(
        slot=shift.slot,
        schedule=shift.schedule,
    )
    sub_shift = shift_factory(
        replacement_for_id=shift.id,
        slot=shift.slot,
        schedule=shift.schedule,
        next=main_next_shift,
    )
    main_prev_shift = shift_factory(
        slot=shift.slot,
        schedule=shift.schedule,
        next=sub_shift,
    )

    response = client.put(
        f'/api/watcher/v1/subshift/{shift.id}',
        json={
            'sub_shifts': [
                {
                    'id': sub_shift.id,
                    'replacement_for_id': shift.id,
                    'start': shift.start.isoformat(),
                    'end': shift.end.isoformat(),
                    'empty': True,
                    'approved': True,
                },
            ]
        }
    )

    assert response.status_code == status.HTTP_200_OK, response.text

    scope_session.refresh(shift)

    assert shift.prev == main_prev_shift
    assert shift.next == main_next_shift

    assert scope_session.query(Shift).filter(Shift.id == sub_shift.id).count() == 0


def test_sub_shifts_update_exist(client, sub_shift_data, shift_factory, member_factory, scope_session):
    middle = sub_shift_data.start + datetime.timedelta(days=5)
    shift = sub_shift_data.shift
    sub_shift_data.prev_shift.predicted_ratings = {'smth': 123}
    member = member_factory()
    shift.schedule.service = member.service

    subshift1 = shift_factory(start=sub_shift_data.start, end=middle, replacement_for=shift)
    subshift1.prev = sub_shift_data.prev_shift
    subshift2 = shift_factory(start=middle, end=sub_shift_data.end, replacement_for=shift, prev=subshift1)
    subshift2.next = sub_shift_data.next_shift
    scope_session.commit()

    response = client.put(
        f'/api/watcher/v1/subshift/{shift.id}',
        json={
            'sub_shifts': [
                {
                    'id': subshift1.id,
                    'start': (sub_shift_data.start + datetime.timedelta(days=2)).isoformat(),
                    'end': middle.isoformat(),
                    'empty': True,
                    'approved': False,
                },
                {
                    'start': middle.isoformat(),
                    'end': sub_shift_data.end.isoformat(),
                    'staff_id': member.staff.id,
                },
            ]
        }
    )

    assert response.status_code == status.HTTP_200_OK
    check_subshifts_right(shift, response.json(), sub_shift_count=3)

    scope_session.refresh(shift)
    assert len(shift.sub_shifts) == 3


def test_sub_shifts_has_intersect(client, sub_shift_data):
    timelane1 = sub_shift_data.start + datetime.timedelta(days=3)
    timelane2 = sub_shift_data.start + datetime.timedelta(days=5)

    response = client.put(
        f'/api/watcher/v1/subshift/{sub_shift_data.shift.id}',
        json={
            'sub_shifts': [
                {
                    'start': sub_shift_data.start.isoformat(),
                    'end': timelane2.isoformat(),
                },
                {
                    'start': timelane1.isoformat(),
                    'end': sub_shift_data.end.isoformat(),
                },
            ]
        }
    )
    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response.json()['context']['message'] == {
        'ru': 'Подсмены не должны пересекаться',
        'en': 'Sub shifts should not overlap',
    }


def test_sub_shifts_unbound(client, sub_shift_data):
    timelane = sub_shift_data.start - datetime.timedelta(days=3)

    response = client.put(
        f'/api/watcher/v1/subshift/{sub_shift_data.shift.id}',
        json={
            'sub_shifts': [
                {
                    'start': timelane.isoformat(),
                    'end': sub_shift_data.end.isoformat(),
                },
            ]
        }
    )

    assert response.status_code == status.HTTP_400_BAD_REQUEST


def test_sub_shifts_unexistent(client, sub_shift_data):
    unexistent_id = 99999999

    response = client.put(
        f'/api/watcher/v1/subshift/{sub_shift_data.shift.id}',
        json={
            'sub_shifts': [
                {
                    'id': unexistent_id,
                    'start': sub_shift_data.start.isoformat(),
                    'end': sub_shift_data.end.isoformat(),
                },
            ]
        }
    )

    assert response.status_code == status.HTTP_400_BAD_REQUEST
    expected_ids = {unexistent_id, }
    assert response.json()['context']['message'] == {
        'ru': f'Подшифтов с id={expected_ids} не существует',
        'en': f'Subshifts with id={expected_ids} not exists',
    }


@pytest.mark.parametrize('in_service', [False, True])
def test_sub_shifts_staff_not_in_service(client, sub_shift_data, service_factory, member_factory, schedule_factory,
                                         in_service, scope_session):
    if in_service:
        service = sub_shift_data.shift.schedule.service
    else:
        service = service_factory()
    member = member_factory(service=service)
    scope_session.commit()

    response = client.put(
        f'/api/watcher/v1/subshift/{sub_shift_data.shift.id}',
        json={
            'sub_shifts': [
                {
                    'staff_id': member.staff_id,
                    'start': (sub_shift_data.start + datetime.timedelta(days=2)).isoformat(),
                    'end': (sub_shift_data.start + datetime.timedelta(days=5)).isoformat(),
                },
            ]
        }
    )

    if in_service:
        assert response.status_code == status.HTTP_200_OK
    else:
        assert response.status_code == status.HTTP_400_BAD_REQUEST
        assert response.json()['context']['message'] == {
            'ru': 'Не все переданные сотрудники состоят в сервисе',
            'en': 'Not all employees are members of service',
        }


def test_subshift_delete_all(client, shift_factory, scope_session):
    shift = shift_factory()
    main_next_shift = shift_factory(
        slot=shift.slot,
        schedule=shift.schedule,
    )
    sub_shift = shift_factory(
        replacement_for_id=shift.id,
        slot=shift.slot,
        schedule=shift.schedule,
        next=main_next_shift,
    )
    main_prev_shift = shift_factory(
        slot=shift.slot,
        schedule=shift.schedule,
        next=sub_shift,
    )

    response = client.put(
        f'/api/watcher/v1/subshift/{shift.id}',
        json={
            'sub_shifts': []
        }
    )

    assert response.status_code == status.HTTP_200_OK, response.text

    scope_session.refresh(shift)

    assert shift.prev == main_prev_shift
    assert shift.next == main_next_shift

    assert scope_session.query(Shift).filter(
        Shift.replacement_for_id == shift.id
    ).count() == 0


def test_sub_shifts_update_slot(scope_session, sub_shift_data, shift_factory, client):
    shift = sub_shift_data.shift
    assert shift.slot_id is not None

    subshift_1 = shift_factory(
        start=sub_shift_data.start,
        end=(sub_shift_data.start + datetime.timedelta(days=2)),
        replacement_for=shift,
        prev=sub_shift_data.prev_shift,
        slot_id=None,
    )
    subshift_2 = shift_factory(
        start=subshift_1.end,
        end=shift.end,
        replacement_for=shift,
        prev=subshift_1,
        next=shift.next
    )
    shift.prev = None
    shift.next = None
    scope_session.commit()

    response = client.put(
        f'/api/watcher/v1/subshift/{shift.id}',
        json={
            'sub_shifts': [
                {
                    'id': subshift_1.id,
                    'start': subshift_1.start.isoformat(),
                    'end': subshift_1.end.isoformat(),
                    'empty': True,
                    'approved': True,
                    'slot_id': shift.slot_id,
                },
                {
                    'id': subshift_2.id,
                    'start': subshift_2.start.isoformat(),
                    'end': subshift_2.end.isoformat(),
                    'empty': True,
                    'approved': True,
                    'slot_id': shift.slot_id,
                },
            ]
        }
    )

    assert response.status_code == status.HTTP_200_OK
    check_subshifts_right(shift, response.json(), sub_shift_count=2)

    scope_session.refresh(shift)
    assert len(shift.sub_shifts) == 2


def request_and_check_role_for_subshift(client, shift_id, params, member_id, subshift_id=None):
    with patch('watcher.logic.member.abc_client.deprive_role') as deprive_role_mock:
        with patch('watcher.tasks.shift.start_shift.delay') as start_shift_mock:
            response = client.put(
                f'/api/watcher/v1/subshift/{shift_id}',
                json=params
            )
    assert response.status_code == status.HTTP_200_OK

    deprive_role_mock.assert_called_once_with(
        membership_id=member_id,
    )
    if subshift_id is None:
        start_shift_mock.called
    else:
        start_shift_mock.assert_called_once_with(
            shift_id=subshift_id,
        )


def check_request_role(sub_shift_id, service_id, login, role_id):
    with patch('watcher.logic.member.abc_client.request_role') as request_role_mock:
        start_shift(shift_id=sub_shift_id)

    request_role_mock.assert_called_once_with(
        service=service_id,
        login=login,
        role=role_id,
    )


def test_update_current_subshift_staff_id(
    scope_session, schedule_data_with_composition, client, member_factory, staff_factory,
):
    schedule = schedule_data_with_composition.schedule
    initial_creation_of_shifts(schedule.id)

    with patch('watcher.logic.member.abc_client.request_role'):
        start_shifts()

    # возьмём текущий и разобьем его
    # после разбивки роль должна отозваться
    shift = query_current_shifts_by_schedule(db=scope_session, schedule_id=schedule.id).first()
    assert shift is not None
    assert shift.staff is not None

    old_staff = shift.staff
    member = member_factory(
        staff=old_staff,
        role=shift.slot.role_on_duty,
        service=schedule.service,
    )
    other_staff = staff_factory()
    member_factory(
        staff=other_staff,
        service=schedule.service,
    )

    params = {
        'sub_shifts': [
            {
                'start': shift.start.isoformat(),
                'end': (shift.start + datetime.timedelta(days=2)).isoformat(),
                'empty': False,
                'staff_id': other_staff.id
            },
            {
                'start': (shift.start + datetime.timedelta(days=2)).isoformat(),
                'end': shift.end.isoformat(),
                'empty': True,
            },
        ]
    }

    request_and_check_role_for_subshift(
        client=client,
        shift_id=shift.id,
        params=params,
        member_id=member.id,
    )

    # узнаем id непустого
    scope_session.refresh(shift)
    sub_shifts = sorted(shift.sub_shifts, key=lambda x: x.start)
    sub_shift = sub_shifts[0]

    check_request_role(
        sub_shift_id=sub_shift.id,
        service_id=schedule.service_id,
        login=other_staff.login,
        role_id=sub_shift.slot.role_on_duty_id,
    )

    # поменяем подсмену обратно на старый стафф
    member_2 = member_factory(
        staff=other_staff,
        role=shift.slot.role_on_duty,
        service=schedule.service,
    )
    sub_shift_2 = sub_shifts[1]
    params['sub_shifts'][0]['staff_id'] = old_staff.id
    params['sub_shifts'][0]['id'] = sub_shift.id
    params['sub_shifts'][1]['id'] = sub_shift_2.id
    request_and_check_role_for_subshift(
        client=client,
        shift_id=shift.id,
        params=params,
        member_id=member_2.id,
        subshift_id=sub_shift.id
    )

    scope_session.delete(member)
    scope_session.commit()
    check_request_role(
        sub_shift_id=sub_shift.id,
        service_id=schedule.service_id,
        login=old_staff.login,
        role_id=sub_shift.slot.role_on_duty_id,
    )


@pytest.mark.parametrize('same_staff', (True, False))
def test_check_deprive_role_main_shift(
    client, shift_factory, member_factory, staff_factory,
    same_staff, scope_session,
):
    """
    Проверим поведение если активную основную смену разбивают на подсмены -
    если дежурный текущей подсмены совпадает с дежурным основной смены
    - роль не должна отозваться
    """

    staff = staff_factory()
    staff_other = staff_factory()
    shift = shift_factory(
        start=now() - datetime.timedelta(hours=1),
        status=enums.ShiftStatus.active,
        staff=staff,
        is_role_requested=True,
    )
    member_factory(
        staff=staff,
        role=shift.slot.role_on_duty,
        service=shift.schedule.service,
    )

    member_factory(
        staff=staff_other,
        role=shift.slot.role_on_duty,
        service=shift.schedule.service,
    )
    with patch('watcher.logic.member.abc_client.deprive_role') as deprive_role_mock:
        response = client.put(
            f'/api/watcher/v1/subshift/{shift.id}',
            json={
            'sub_shifts': [
                {
                    'start': shift.start.isoformat(),
                    'end': (shift.start + datetime.timedelta(minutes=62)).isoformat(),
                    'empty': False,
                    'staff_id': staff.id if same_staff else staff_other.id
                },
                {
                    'start': (shift.start + datetime.timedelta(minutes=62)).isoformat(),
                    'end': shift.end.isoformat(),
                    'empty': True,
                },
            ]
        }
        )
    assert response.status_code == 200, response.text
    current_shift = scope_session.query(Shift).filter(
        Shift.replacement_for_id == shift.id,
        Shift.empty.is_(False)
    ).first()

    if same_staff:
        deprive_role_mock.assert_not_called()
        assert current_shift.is_role_requested is True
    else:
        deprive_role_mock.assert_called_once()
        assert current_shift.is_role_requested is False


@pytest.mark.parametrize('sub_shift_action', ('no_change', 'delete', 'one_sub_shift'))
def test_subshift_sequence_consistency(client, scope_session, shift_factory, schedule_factory, sub_shift_action):

    schedule = schedule_factory()
    start = now()
    mid = start + datetime.timedelta(hours=1)
    end = start + datetime.timedelta(hours=2)

    shift_1 = shift_factory(schedule=schedule, start=start, end=end)
    shift_2 = shift_factory(schedule=schedule, start=start, end=end)

    main_last_shift = shift_factory(
        slot=shift_1.slot,
        schedule=schedule,
    )

    sub_shift_2_part_two = shift_factory(
        replacement_for_id=shift_2.id,
        slot=shift_2.slot,
        schedule=schedule,
        next=main_last_shift,
        start=mid,
        end=end,
    )
    sub_shift_1_part_two = shift_factory(
        replacement_for_id=shift_1.id,
        slot=shift_1.slot,
        schedule=schedule,
        next=sub_shift_2_part_two,
        start=mid,
        end=end,
    )
    sub_shift_2_part_one = shift_factory(
        replacement_for_id=shift_2.id,
        slot=shift_2.slot,
        schedule=schedule,
        next=sub_shift_1_part_two,
        start=start,
        end=mid,
    )
    sub_shift_1_part_one = shift_factory(
        replacement_for_id=shift_1.id,
        slot=shift_1.slot,
        schedule=schedule,
        next=sub_shift_2_part_one,
        start=start,
        end=mid,
    )

    main_first_shift = shift_factory(
        slot=shift_1.slot,
        schedule=schedule,
        next=sub_shift_1_part_one,
    )

    'no_change', 'delete', 'one_sub_shift'

    if sub_shift_action == 'delete':
        json_sub_shifts = []
    elif sub_shift_action == 'one_sub_shift':
        json_sub_shifts = [
            {
                'replacement_for_id': shift_1.id,
                'start': sub_shift_1_part_one.start.isoformat(),
                'end': sub_shift_1_part_two.end.isoformat(),
                'empty': True,
                'approved': True,
            },
        ]
    elif sub_shift_action == 'no_change':
        json_sub_shifts = [
            {
                'id': sub_shift_1_part_one.id,
                'replacement_for_id': shift_1.id,
                'start': sub_shift_1_part_one.start.isoformat(),
                'end': sub_shift_1_part_one.end.isoformat(),
                'empty': True,
                'approved': True,
            },
            {
                'id': sub_shift_1_part_two.id,
                'replacement_for_id': shift_1.id,
                'start': sub_shift_1_part_two.start.isoformat(),
                'end': sub_shift_1_part_two.end.isoformat(),
                'empty': True,
                'approved': True,
            },
        ]

    response = client.put(
        f'/api/watcher/v1/subshift/{shift_1.id}',
        json={
            'sub_shifts': json_sub_shifts
        }
    )

    assert response.status_code == status.HTTP_200_OK, response.text

    scope_session.refresh(shift_1)
    scope_session.refresh(shift_2)

    all_shifts = scope_session.query(Shift).all()

    shift_ids = {}
    exist_next_ids = set()
    for shift in all_shifts:
        shift_ids[shift.id] = shift
        # Проверяем что нет задублированных next_id
        if shift.next_id:
            assert shift.next_id not in exist_next_ids
        exist_next_ids.add(shift.next_id)

    # Проверяем что последовательность все так же приводит к последнему шифту
    scope_session.refresh(main_first_shift)
    cur_shift = main_first_shift
    while cur_shift.next_id:
        cur_shift = shift_ids[cur_shift.next_id]
    assert cur_shift.id == main_last_shift.id


@pytest.mark.parametrize('timespan', ['now', 'future', 'past'])
def test_sub_shifts_change_staff_check_ratings(client, scope_session, shift_sequence_data, timespan):

    schedule = shift_sequence_data.schedule
    staff_1 = shift_sequence_data.staff_1
    staff_2 = shift_sequence_data.staff_2

    if timespan == 'now':
        cur_shift = shift_sequence_data.shift
        next_shift = shift_sequence_data.next_shift
    elif timespan == 'future':
        cur_shift = shift_sequence_data.next_shift
        next_shift = shift_sequence_data.next_next_shift
    elif timespan == 'past':
        cur_shift = shift_sequence_data.prev_shift
        next_shift = shift_sequence_data.shift

    shift_middle = cur_shift.start + (cur_shift.end - cur_shift.start) / 2

    with patch('watcher.logic.member.abc_client.deprive_role'):
        with patch('watcher.tasks.people_allocation.start_people_allocation.delay'):
            response = client.put(
                f'/api/watcher/v1/subshift/{cur_shift.id}',
                json={
                    'sub_shifts': [
                        {
                            'start': cur_shift.start.isoformat(),
                            'end': shift_middle.isoformat(),
                            'staff_id': staff_1.id,
                        },
                        {
                            'start': shift_middle.isoformat(),
                            'end': cur_shift.end.isoformat(),
                            'staff_id': staff_2.id,
                        },
                    ]
                }
            )

    if timespan == 'past':
        assert response.status_code == status.HTTP_400_BAD_REQUEST, response.text
        assert response.json()['context']['message']['en'] == 'Only future shifts can be edited'
    else:
        assert response.status_code == status.HTTP_200_OK
        data = response.json()
        assert {sub_row['staff']['id'] for sub_row in data} == {staff_1.id, staff_2.id}
        sub_shift_ids = [sub_row['id'] for sub_row in data]
        sub_shifts = scope_session.query(Shift).filter(Shift.id.in_(sub_shift_ids)).order_by(Shift.start.asc()).all()

        start_people_allocation(schedules_group_id=schedule.schedules_group_id)
        scope_session.refresh(next_shift)
        if timespan == 'now':
            assert sub_shifts[0].predicted_ratings == {staff_1.login: 96.0, staff_2.login: 0.0}
            assert sub_shifts[1].predicted_ratings == {staff_1.login: 120.0, staff_2.login: 0.0}
            assert next_shift.predicted_ratings == {staff_1.login: 120.0, staff_2.login: 24.0}
        elif timespan == 'future':
            assert sub_shifts[0].predicted_ratings == {staff_1.login: 144.0, staff_2.login: 0.0}
            assert sub_shifts[1].predicted_ratings == {staff_1.login: 168.0, staff_2.login: 0.0}
            assert next_shift.predicted_ratings == {staff_1.login: 168.0, staff_2.login: 24.0}
