import datetime
from decimal import Decimal

import responses
import pytest

from freezegun import freeze_time
from fastapi import status
from sqlalchemy import inspect
from unittest.mock import patch

from watcher import enums
from watcher.crud.revision import get_current_revision
from watcher.crud.shift import query_all_shifts_by_schedule
from watcher.db import Revision, Role, Shift
from watcher.logic.timezone import localize, now
from watcher.tasks.generating_shifts import initial_creation_of_shifts
from watcher.crud.rating import get_existing_ratings_by_schedule_staff_ids


def test_get_interval(client, interval_factory, slot_factory, assert_equal_stable_list):
    interval = interval_factory()
    slot1 = slot_factory(interval=interval, show_in_staff=True)
    slot2 = slot_factory(interval=interval, is_primary=False)

    response = client.get(
        f'/api/watcher/v1/interval/{interval.id}'
    )
    assert response.status_code == status.HTTP_200_OK
    assert response.text
    data = response.json()
    assert len(data['slots']) == 2
    assert 'revision_id' in data
    assert_equal_stable_list(
        data['slots'], [
            {
                'id': slot2.id,
                'interval_id': interval.id,
                'role_on_duty_id': slot2.role_on_duty_id,
                'composition_id': slot2.composition_id,
                'points_per_hour': 1,
                'show_in_staff': slot2.show_in_staff,
                'is_primary': slot2.is_primary,
            },
            {
                'id': slot1.id,
                'interval_id': interval.id,
                'points_per_hour': 1,
                'role_on_duty_id': slot1.role_on_duty_id,
                'composition_id': slot1.composition_id,
                'is_primary': slot1.is_primary,
            }
        ],
    )


def test_get_interval_list(client, revision_factory, interval_factory, slot_factory):
    revision = revision_factory(state=enums.RevisionState.active)
    schedule = revision.schedule
    intervals = [interval_factory(schedule=schedule, revision=revision) for _ in range(2)]
    slots = [slot_factory(interval=intervals[0]) for _ in range(2)]

    # их не должно быть в ответе
    interval_factory(schedule=schedule, revision=revision_factory(state=enums.RevisionState.disabled))

    response = client.get(
        '/api/watcher/v1/interval/',
        params={'filter': f'schedule_id={schedule.id}'}
    )
    assert response.status_code == status.HTTP_200_OK
    assert response.text
    data = response.json()['result']
    assert len(data) == 2
    assert {obj['id'] for obj in data} == {obj.id for obj in intervals}
    assert len(data[1]['slots']) == 2
    assert {obj['id'] for obj in data[1]['slots']} == {obj.id for obj in slots}


def test_list_interval_with_revision_id(client, revision_factory, interval_factory):
    revision = revision_factory()
    schedule = revision.schedule
    intervals = [interval_factory(schedule=schedule, revision=revision) for _ in range(2)]
    # этого интервала не должно быть в ответе
    interval_factory(schedule=schedule)

    response = client.get(
        '/api/watcher/v1/interval/',
        params={'filter': f'schedule_id={schedule.id},revision_id={revision.id}'}
    )
    assert response.status_code == status.HTTP_200_OK, response.text
    data = response.json()['result']
    assert {obj['id'] for obj in data} == {obj.id for obj in intervals}


def test_put_interval_while_recalculating(client, schedule_factory, scope_session):
    schedule = schedule_factory(recalculation_in_process=True)

    response = client.put(
        '/api/watcher/v1/interval/',
        json={
            'schedule_id': schedule.id,
            'intervals': [
                {
                    'schedule_id': schedule.id,
                    'duration': 5000,
                    'order': 1,
                },
            ]
        }
    )
    assert response.status_code == 403, response.content
    assert response.json()['error'] == 'recalculation_in_process'


def test_put_intervals_future(client, scope_session, interval_factory, slot_factory, revision_factory):
    start = now() + datetime.timedelta(days=10)
    revision = revision_factory(apply_datetime=start)
    interval = interval_factory(
        revision=revision, schedule=revision.schedule,
        type_employment=enums.IntervalTypeEmployment.empty,
    )
    slot_factory(interval=interval)
    with patch('watcher.tasks.people_allocation.start_people_allocation'):
        initial_creation_of_shifts(schedule_id=revision.schedule.id)

    shift = scope_session.query(Shift).filter(
        Shift.schedule_id == revision.schedule.id
    ).order_by(Shift.start).first()
    assert shift.start == start
    new_start = start + datetime.timedelta(days=5)
    data = {
        'schedule_id': revision.schedule.id,
        'revision_id': revision.id,
        'apply_datetime': new_start.isoformat(),
        'intervals': [
            {
                'schedule_id': revision.schedule.id,
                'duration': interval.duration.total_seconds(),
                'order': 1,
                'type_employment': enums.IntervalTypeEmployment.empty,
            },
        ]
    }
    with patch('watcher.tasks.generating_shifts.initial_creation_of_shifts') as mock_creation:
        response = client.put(
            '/api/watcher/v1/interval/',
            json=data
        )
    assert response.status_code == 200, response.content
    new_revision_id = response.json()[0]['revision_id']
    shifts = scope_session.query(Shift).filter(
        Shift.schedule_id == revision.schedule.id
    ).order_by(Shift.start).count()
    assert shifts == 0
    assert revision.id != new_revision_id
    mock_creation.delay.assert_called_once()


def test_put_intervals_with_different_schedule(client, interval_factory, revision_factory):
    revision = revision_factory(state=enums.RevisionState.active)
    interval1 = interval_factory(revision=revision)
    interval2 = interval_factory(revision=revision)

    response = client.put(
        '/api/watcher/v1/interval/',
        json={
            'schedule_id': interval1.schedule.id,
            'intervals': [
                {
                    'id': interval1.id,
                    'schedule_id': interval1.schedule.id,
                    'duration': 5000,
                    'order': 1,
                },
                {
                    'id': interval2.id,
                    'order': 2,
                    'schedule_id': interval2.schedule.id,
                    'duration': 2000,
                }
            ]
        }
    )

    assert response.status_code == status.HTTP_400_BAD_REQUEST, response.text
    assert response.json()['context']['message'] == {
        'en': 'All intervals must belong to the specified schedule',
        'ru': 'Все интервалы должны принадлежать заданному расписанию',
    }


@pytest.mark.parametrize(
    'apply_datetime', (
        now() + datetime.timedelta(days=2),
        now() - datetime.timedelta(days=2),
        None,
    )
)
@pytest.mark.parametrize('has_current_revision', (True, False))
def test_put_apply_datetime(client, revision_factory, schedule_factory, scope_session, apply_datetime,
                            has_current_revision):
    schedule = schedule_factory()
    current_now = now()
    if has_current_revision:
        revision_factory(
            schedule=schedule,
            apply_datetime=now() - datetime.timedelta(days=1)
        )
    with freeze_time(current_now):
        with patch('watcher.api.routes.interval.revision_shift_boundaries'):
            response = client.put(
                '/api/watcher/v1/interval/',
                json={
                    'schedule_id': schedule.id,
                    'intervals': [{
                        'schedule_id': schedule.id,
                        'type_employment': enums.IntervalTypeEmployment.empty,
                        'duration': 800,
                        'order': 1,
                    }],
                    'apply_datetime': apply_datetime.isoformat() if apply_datetime is not None else apply_datetime,
                }
            )
    if apply_datetime and apply_datetime < now() and has_current_revision:
        assert response.status_code == status.HTTP_400_BAD_REQUEST, response.text
        assert response.json()['context']['message']['en'] == (
            'Revision cannot start earlier than current revision'
        )
    else:
        assert response.status_code == status.HTTP_200_OK, response.text
        revision = scope_session.query(Revision).get(response.json()[0]['revision_id'])
        assert revision.apply_datetime == apply_datetime if apply_datetime else current_now


@pytest.mark.parametrize('is_active', (True, False))
@pytest.mark.parametrize('recalculate', (True, False))
def test_put_between_revisions(
    client, revision_factory, schedule_factory,
    scope_session, is_active, recalculate
):
    schedule = schedule_factory(
        state=(enums.ScheduleState.active if is_active else enums.ScheduleState.disabled),
        recalculate=recalculate,
    )
    current_now = now()

    revision = revision_factory(
        schedule=schedule,
        apply_datetime=current_now + datetime.timedelta(days=3)
    )

    prev_revision = revision_factory(
        schedule=schedule,
        apply_datetime=current_now - datetime.timedelta(days=3),
        next_id=revision.id
    )

    with patch('watcher.api.routes.interval.revision_shift_boundaries') as mock_revision_shift:
        response = client.put(
            '/api/watcher/v1/interval/',
            json={
                'schedule_id': schedule.id,
                'intervals': [{
                    'schedule_id': schedule.id,
                    'type_employment': enums.IntervalTypeEmployment.empty,
                    'duration': 800,
                    'order': 1,
                }],
            }
        )

    assert response.status_code == status.HTTP_200_OK, response.text
    new_revision = scope_session.query(Revision).get(response.json()[0]['revision_id'])

    assert new_revision.next == revision
    assert new_revision.prev == prev_revision

    if is_active and recalculate:
        mock_revision_shift.delay.assert_called_once()
    else:
        mock_revision_shift.delay.assert_not_called()


def test_put_intervals_null_slots_for_full_interval(client, schedule_factory):
    schedule = schedule_factory()

    response = client.put(
        '/api/watcher/v1/interval/',
        json={
            'schedule_id': schedule.id,
            'intervals': [{
                'schedule_id': schedule.id,
                'type_employment': enums.IntervalTypeEmployment.full,
                'duration': 800,
                'order': 1,
            }]
        }
    )
    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response.text
    assert response.json()['context']['message'] == {
        'en': 'Non-empty intervals must have slots',
        'ru': 'У не пустых интервалов должны быть слоты'
    }


def test_put_intervals_notnull_slots_for_empty_interval(client, schedule_factory):
    schedule = schedule_factory()

    response = client.put(
        '/api/watcher/v1/interval/',
        json={
            'schedule_id': schedule.id,
            'intervals': [{
                'schedule_id': schedule.id,
                'type_employment': enums.IntervalTypeEmployment.empty,
                'duration': 800,
                'order': 1,
                'slots': [{
                    'composition_id': 1,
                    'show_in_staff': False
                }]
            }]
        }
    )
    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response.text
    assert response.json()['context']['message'] == {
        'en': 'Empty intervals should not have slots',
        'ru': 'У пустых интервалов не должно быть слотов'
    }


def test_put_interval_with_insufficient_duration(client, schedule_factory):
    schedule = schedule_factory()

    response = client.put(
        '/api/watcher/v1/interval/',
        json={
            'schedule_id': schedule.id,
            'intervals': [{
                'schedule_id': schedule.id,
                'type_employment': enums.IntervalTypeEmployment.empty,
                'duration': 300,
                'order': 1,
            }]
        }
    )
    assert response.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY
    assert response.json()['detail'][0]['msg'] == {
        'ru': 'interval duration should be at least 10 minutes',
        'en': 'interval duration should be at least 10 minutes'
    }


@pytest.mark.parametrize('error_source', ('code', 'scope'))
def test_put_interval_with_prohibited_slot_role(
    client, schedule_factory, role_factory,
    scope_session, scope_factory, error_source
):
    schedule = schedule_factory()
    if error_source == 'code':
        role1 = role_factory(code=Role.EXCLUSIVE_OWNER)
    else:
        scope = scope_factory(can_issue_at_duty_time=False)
        role1 = role_factory(scope=scope)
    role2 = role_factory(code=Role.DUTY)

    response = client.put(
        '/api/watcher/v1/interval/',
        json={
            'schedule_id': schedule.id,
            'intervals': [{
                'schedule_id': schedule.id,
                'type_employment': enums.IntervalTypeEmployment.full,
                'duration': 1200,
                'order': 1,
                'slots': [
                    {
                        'composition_id': 123,
                        'role_on_duty_id': role1.id,
                        'show_in_staff': False
                    },
                    {
                        'composition_id': 123,
                        'role_on_duty_id': role2.id,
                        'show_in_staff': False
                    }
                ]
            }]
        }
    )
    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response.json()['context']['message'] == {
        'ru': 'Указанную роль нельзя выдать на дежурство',
        'en': 'Specified role cannot be assigned to duty'
    }


def test_put_service_id_mismatch_in_roles(client, service_factory, schedule_factory, role_factory, scope_session):
    service1 = service_factory()
    service2 = service_factory()
    schedule = schedule_factory(service=service1)
    role = role_factory(service=service2)

    response = client.put(
        '/api/watcher/v1/interval/',
        json={
            'schedule_id': schedule.id,
            'intervals': [{
                'schedule_id': schedule.id,
                'type_employment': enums.IntervalTypeEmployment.full,
                'duration': 1200,
                'order': 1,
                'slots': [{
                    'composition_id': 123,
                    'role_on_duty_id': role.id,
                    'show_in_staff': False
                }]
            }]
        }
    )
    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response.json()['context']['message'] == {
        'ru': 'Указанная роль не принадлежит данному сервису',
        'en': 'Specified role does not correspond to this service'
    }


def test_put_service_id_mismatch_in_composition(client, service_factory, schedule_factory, composition_factory,
                                                role_factory, scope_session):
    service1 = service_factory()
    service2 = service_factory()
    schedule = schedule_factory(service=service1)
    composition = composition_factory(service=service2)
    role = role_factory(service=service1)

    response = client.put(
        '/api/watcher/v1/interval/',
        json={
            'schedule_id': schedule.id,
            'intervals': [{
                'schedule_id': schedule.id,
                'type_employment': enums.IntervalTypeEmployment.full,
                'duration': 1200,
                'order': 1,
                'slots': [{
                    'composition_id': composition.id,
                    'role_on_duty_id': role.id,
                    'show_in_staff': False
                }]
            }]
        }
    )
    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response.json()['context']['message'] == {
        'ru': 'Указанная композиция не соответствует данному сервису',
        'en': 'Specified composition does not correspond to this service'
    }


def test_put_with_future_revision(client, revision_factory, interval_factory, scope_session):
    next_revision = revision_factory(apply_datetime=now() + datetime.timedelta(days=1))
    current_revision = revision_factory(
        schedule=next_revision.schedule,
        state=enums.RevisionState.active,
        next=next_revision,
    )
    interval = interval_factory(
        schedule=next_revision.schedule,
        revision=current_revision
    )
    with patch('watcher.api.routes.interval.revision_shift_boundaries'):
        response = client.put(
            '/api/watcher/v1/interval/',
            json={
                'schedule_id': interval.schedule_id,
                'intervals': [{
                    'schedule_id': interval.schedule_id,
                    'type_employment': enums.IntervalTypeEmployment.empty,
                    'duration': 800,
                    'order': 1,
                }],
                'apply_datetime': (now() + datetime.timedelta(days=2)).isoformat(),
            }
        )
    assert response.status_code == status.HTTP_200_OK
    assert response.text

    assert inspect(next_revision).persistent
    assert current_revision.next == next_revision
    assert current_revision.state == enums.RevisionState.active
    assert current_revision.next.state == enums.RevisionState.active
    assert next_revision.next.state == enums.RevisionState.active
    assert len(current_revision.intervals) == 1
    assert len(next_revision.next.intervals) == 1
    assert next_revision.next.apply_datetime.date() == (now() + datetime.timedelta(days=2)).date()


@patch('watcher.api.routes.interval.revision_shift_boundaries')
@patch('watcher.api.routes.interval.start_people_allocation')
@pytest.mark.parametrize('future', (True, False))
def test_put_interval_without_tasks(start_people_allocation, revision_shift_boundaries, client, service_factory,
                                    schedule_factory, interval_factory, composition_factory, slot_factory,
                                    revision_factory, scope_session, future):
    service = service_factory()
    schedule = schedule_factory(service=service)
    revision_apply_datetime = now() - datetime.timedelta(days=1)
    if future:
        revision_apply_datetime = now() + datetime.timedelta(days=1)
    revision = revision_factory(schedule=schedule, state=enums.RevisionState.active,
                                apply_datetime=revision_apply_datetime)
    interval = interval_factory(name='interval name', schedule=schedule, revision=revision)
    composition = composition_factory(service=service)
    slot = slot_factory(composition=composition, interval=interval)

    response = client.put(
        '/api/watcher/v1/interval/',
        json={
            'schedule_id': interval.schedule.id,
            'revision_id': revision.id,
            'intervals': [
                {
                    'id': interval.id,
                    'name': interval.name,
                    'schedule_id': interval.schedule.id,
                    'duration': 800,
                    'order': 1,
                    'slots': [
                        {
                            'id': slot.id,
                            'points_per_hour': slot.points_per_hour,
                            'role_on_duty_id': slot.role_on_duty.id,
                            'composition_id': slot.composition.id,
                            'show_in_staff': not slot.show_in_staff,
                            'is_primary': slot.is_primary,
                        }
                    ]
                },
            ]
        }
    )
    if future:
        assert response.status_code == status.HTTP_200_OK, response.text

        data = response.json()
        assert data[0].pop('id') != interval.id
        assert data[0]['slots'][0].pop('id') != slot.id
        assert data[0]['slots'][0].pop('interval_id') != interval.id

        scope_session.refresh(revision)

        new_revision = get_current_revision(db=scope_session, schedule_id=revision.schedule.id)

        assert data == [
            {
                'name': interval.name,
                'schedule_id': interval.schedule.id,
                'revision_id': new_revision.id,
                'duration': 800.0,
                'type_employment': interval.type_employment,
                'unexpected_holidays': interval.unexpected_holidays,
                'weekend_behaviour': interval.weekend_behaviour,
                'order': 1,
                'primary_rotation': False,
                'backup_takes_primary_shift': False,
                'slots': [{
                    'points_per_hour': slot.points_per_hour,
                    'role_on_duty_id': slot.role_on_duty.id,
                    'composition_id': slot.composition.id,
                    'show_in_staff': not slot.show_in_staff,
                    'is_primary': slot.is_primary,
                }],
            }
        ]

        assert not start_people_allocation.delay.called
        assert not revision_shift_boundaries.called

        assert revision.state == enums.RevisionState.disabled
        assert revision.next is None
        assert len(revision.intervals) == 1
        assert len(new_revision.intervals) == 1
    else:
        assert response.status_code == status.HTTP_400_BAD_REQUEST, response.text


@patch('watcher.api.routes.interval.revision_shift_boundaries')
@patch('watcher.api.routes.interval.start_people_allocation')
def test_put_interval_with_new_slot(start_people_allocation, revision_shift_boundaries, client, service_factory,
                                    schedule_factory, interval_factory, role_factory, revision_factory,
                                    composition_factory, scope_session):
    service = service_factory()
    schedule = schedule_factory(service=service)
    revision = revision_factory(schedule=schedule, state=enums.RevisionState.active)
    interval_factory(schedule=schedule, revision=revision)
    interval = interval_factory(schedule=schedule, revision=revision)
    comp = composition_factory(service=service)
    role = role_factory()

    response = client.put(
        '/api/watcher/v1/interval/',
        json={
            'schedule_id': interval.schedule.id,
            'apply_datetime': (now() + datetime.timedelta(days=1)).isoformat(),
            'intervals': [
                {
                    'schedule_id': interval.schedule.id,
                    'duration': 800,
                    'order': 1,
                    'slots': [
                        {
                            'composition_id': comp.id,
                            'role_on_duty_id': role.id,
                            'points_per_hour': 0,
                            'show_in_staff': True,
                        }
                    ]
                },
            ],
        }
    )
    assert response.status_code == status.HTTP_200_OK, response.text
    data = response.json()

    assert data[0].pop('id') != interval.id
    assert data[0]['slots'][0].pop('id')
    assert data[0]['slots'][0].pop('interval_id') != interval.id

    scope_session.refresh(revision)
    assert data == [
        {
            'name': None,
            'schedule_id': interval.schedule.id,
            'revision_id': revision.next.id,
            'duration': 800.0,
            'type_employment': interval.type_employment,
            'unexpected_holidays': interval.unexpected_holidays,
            'weekend_behaviour': interval.weekend_behaviour,
            'order': 1,
            'primary_rotation': False,
            'backup_takes_primary_shift': False,
            'slots': [{
                'role_on_duty_id': role.id,
                'points_per_hour': 0,
                'composition_id': comp.id,
                'show_in_staff': True,
                'is_primary': True,
            }],
        },
    ]
    assert not start_people_allocation.called
    assert revision_shift_boundaries.delay.called

    assert revision.next
    assert len(revision.intervals) == 2
    assert len(revision.next.intervals) == 1


@patch('watcher.api.routes.interval.revision_shift_boundaries')
@patch('watcher.api.routes.interval.start_people_allocation')
def test_put_new_interval_with_slots(start_people_allocation, revision_shift_boundaries, client, service_factory,
                                     schedule_factory, revision_factory, scope_session, role_factory,
                                     composition_factory):
    service = service_factory()
    schedule = schedule_factory(service=service, threshold_day=datetime.timedelta(days=2))
    revision = revision_factory(
        schedule=schedule,
        state=enums.RevisionState.active,
        apply_datetime=(now() - datetime.timedelta(minutes=3)),
    )
    role = role_factory()
    comp = composition_factory(service=service)
    response = client.put(
        '/api/watcher/v1/interval/',
        json={
            'schedule_id': schedule.id,
            'intervals': [
                {
                    'schedule_id': schedule.id,
                    'duration': 86400,
                    'type_employment': enums.IntervalTypeEmployment.full,
                    'unexpected_holidays': enums.IntervalUnexpectedHolidays.remove,
                    'weekend_behaviour': enums.IntervalWeekendsBehaviour.extend,
                    'order': 1,
                    'slots': [
                        {
                            'composition_id': comp.id,
                            'show_in_staff': True,
                        },
                        {
                            'role_on_duty_id': role.id,
                            'composition_id': comp.id,
                            'show_in_staff': True,
                        }
                    ]
                },
            ]
        }
    )

    assert response.status_code == status.HTTP_200_OK, response.text

    assert not start_people_allocation.delay.called
    assert revision_shift_boundaries.delay.called

    data = response.json()

    assert all([interval.pop('id') for interval in data])
    assert all([slot.pop('id') for interval in data for slot in interval['slots']])
    assert all([slot.pop('interval_id') for interval in data for slot in interval['slots']])

    scope_session.refresh(revision)

    assert data == [
        {
            'name': None,
            'schedule_id': schedule.id,
            'revision_id': revision.next_id,
            'duration': 86400,
            'type_employment': enums.IntervalTypeEmployment.full,
            'unexpected_holidays': enums.IntervalUnexpectedHolidays.remove,
            'weekend_behaviour': enums.IntervalWeekendsBehaviour.extend,
            'order': 1,
            'primary_rotation': False,
            'backup_takes_primary_shift': False,
            'slots': [
                {
                    'role_on_duty_id': None,
                    'composition_id': comp.id,
                    'points_per_hour': 1,
                    'show_in_staff': True,
                    'is_primary': True,
                },
                {
                    'role_on_duty_id': role.id,
                    'points_per_hour': 1,
                    'composition_id': comp.id,
                    'show_in_staff': True,
                    'is_primary': True,
                }
            ]
        }
    ]

    assert revision.next
    assert len(revision.intervals) == 0
    assert len(revision.next.intervals) == 1


@patch('watcher.api.routes.interval.revision_shift_boundaries')
@patch('watcher.api.routes.interval.start_people_allocation')
def test_put_interval_with_mixed_intervals(start_people_allocation, revision_shift_boundaries, client, service_factory,
                                           schedule_factory, composition_factory, revision_factory, interval_factory,
                                           slot_factory, scope_session):
    service = service_factory()
    schedule = schedule_factory(service=service)
    composition = composition_factory(service=service)
    revision = revision_factory(
        schedule=schedule,
        state=enums.RevisionState.active,
        apply_datetime=(now() - datetime.timedelta(minutes=3)),
    )
    interval = interval_factory(schedule=schedule, revision=revision)
    exist_empty_interval_slot = slot_factory(composition=composition, interval=interval, show_in_staff=True)

    response = client.put(
        '/api/watcher/v1/interval/',
        json={
            'schedule_id': schedule.id,
            'intervals': [
                {
                    'schedule_id': schedule.id,
                    'duration': 172800,
                    'type_employment': enums.IntervalTypeEmployment.empty,
                    'unexpected_holidays': enums.IntervalUnexpectedHolidays.remove,
                    'weekend_behaviour': enums.IntervalWeekendsBehaviour.extend,
                    'order': 2,
                },
                {
                    'schedule_id': schedule.id,
                    'duration': 172800,
                    'type_employment': enums.IntervalTypeEmployment.empty,
                    'unexpected_holidays': enums.IntervalUnexpectedHolidays.remove,
                    'weekend_behaviour': enums.IntervalWeekendsBehaviour.extend,
                    'order': 3,
                },
                {
                    'id': interval.id,
                    'schedule_id': interval.schedule.id,
                    'duration': 172800,
                    'type_employment': interval.type_employment,
                    'unexpected_holidays': interval.unexpected_holidays,
                    'weekend_behaviour': interval.weekend_behaviour,
                    'order': interval.order,
                    'slots': [
                        {
                            'composition_id': exist_empty_interval_slot.composition_id,
                            'points_per_hour': 1,
                            'role_on_duty_id': exist_empty_interval_slot.role_on_duty_id,
                            'show_in_staff': exist_empty_interval_slot.show_in_staff,
                            'is_primary': exist_empty_interval_slot.is_primary,
                        }
                    ]
                },
            ]
        }
    )
    assert response.status_code == status.HTTP_200_OK, response.text

    assert not start_people_allocation.called
    assert revision_shift_boundaries.delay.called

    data = response.json()

    assert all([interval.pop('id') for interval in data])
    assert all([slot.pop('id') for interval in data for slot in interval['slots']])
    assert all([slot.pop('interval_id') for interval in data for slot in interval['slots']])

    scope_session.refresh(revision)

    assert data == [
        {
            'name': None,
            'schedule_id': interval.schedule.id,
            'revision_id': revision.next.id,
            'duration': 172800.0,
            'type_employment': interval.type_employment,
            'unexpected_holidays': interval.unexpected_holidays,
            'weekend_behaviour': interval.weekend_behaviour,
            'order': interval.order,
            'primary_rotation': False,
            'backup_takes_primary_shift': False,
            'slots': [{
                'composition_id': exist_empty_interval_slot.composition_id,
                'points_per_hour': 1,
                'role_on_duty_id': exist_empty_interval_slot.role_on_duty_id,
                'show_in_staff': exist_empty_interval_slot.show_in_staff,
                'is_primary': exist_empty_interval_slot.is_primary,
            }]
        },
        {
            'name': None,
            'schedule_id': schedule.id,
            'revision_id': revision.next.id,
            'duration': 172800.0,
            'type_employment': enums.IntervalTypeEmployment.empty,
            'unexpected_holidays': enums.IntervalUnexpectedHolidays.remove,
            'weekend_behaviour': enums.IntervalWeekendsBehaviour.extend,
            'order': 2,
            'primary_rotation': False,
            'backup_takes_primary_shift': False,
            'slots': [
                {
                    'composition_id': None,
                    'points_per_hour': 1,
                    'role_on_duty_id': None,
                    'show_in_staff': False,
                    'is_primary': True,
                }
            ]
        },
        {
            'name': None,
            'schedule_id': schedule.id,
            'revision_id': revision.next.id,
            'duration': 172800.0,
            'type_employment': enums.IntervalTypeEmployment.empty,
            'unexpected_holidays': enums.IntervalUnexpectedHolidays.remove,
            'weekend_behaviour': enums.IntervalWeekendsBehaviour.extend,
            'order': 3,
            'primary_rotation': False,
            'backup_takes_primary_shift': False,
            'slots': [
                {
                    'composition_id': None,
                    'points_per_hour': 1,
                    'role_on_duty_id': None,
                    'show_in_staff': False,
                    'is_primary': True,
                }
            ]
        }
    ]

    assert revision.next
    assert len(revision.intervals) == 1
    assert len(revision.next.intervals) == 3


def test_put_schedule_without_revisions(client, schedule_factory, scope_session):
    schedule = schedule_factory()
    with patch('watcher.api.routes.interval.revision_shift_boundaries') as mock_revision_shift_boundaries:
        response = client.put(
            'api/watcher/v1/interval/',
            json={
                'schedule_id': schedule.id,
                'intervals': [
                    {
                        'schedule_id': schedule.id,
                        'duration': 5000,
                        'type_employment': 'empty',
                        'unexpected_holidays': 'remove',
                        'weekend_behaviour': 'extend',
                        'order': 1,
                        'slots': [],
                    }
                ]
            }
        )

    assert response.status_code == status.HTTP_200_OK
    mock_revision_shift_boundaries.delay.assert_called_once()


@responses.activate
def test_interval_creating_pipeline(client, service_factory):
    response = client.post(
        '/api/watcher/v1/schedule_group/',
        json={
            'slug': 'group1',
            'name': 'группа 1'
        }
    )
    assert response.status_code == status.HTTP_201_CREATED, response.text
    schedules_group_id = response.json()['id']

    service = service_factory()
    responses.add(
        responses.GET,
        'https://abc-back.test.yandex-team.ru/api/v4/duty/schedules/',
        status=200,
        json={'results': []}
    )

    response = client.post(
        '/api/watcher/v1/schedule/',
        json={
            'slug': 'gold-ololo-test-skyblue-pink',
            'name': 'тестовый график 1',
            'service_id': service.id,
            'recalculate': False,
            'days_for_notify_of_problems': 86400,
            'days_for_notify_of_begin': [86400 * 10],
            'length_of_absences': 1,
            'pin_shifts': 1,
            'schedules_group_id': schedules_group_id,
        }
    )
    assert response.status_code == status.HTTP_201_CREATED, response.text
    schedule_id = response.json()['id']

    response = client.put(
        '/api/watcher/v1/interval/',
        json={
            'schedule_id': schedule_id,
            'intervals': [
                {
                    'schedule_id': schedule_id,
                    'duration': 64000,
                    'type_employment': 'empty',
                    'unexpected_holidays': 'remove',
                    'weekend_behaviour': 'extend',
                    'order': 1,
                    'slots': [],
                }
            ]
        }
    )
    assert response.status_code == status.HTTP_200_OK, response.text


def test_people_allocated(
    scope_session, schedule_data_with_composition,
    client, composition_factory, composition_participants_factory,
):
    schedule = schedule_data_with_composition.schedule
    composition = composition_factory(service=schedule.service)
    composition_participants = composition_participants_factory(composition=composition)

    response = client.put(
        '/api/watcher/v1/interval/',
        json={
            'schedule_id': schedule.id,
            'intervals': [
                {
                    'schedule_id': schedule.id,
                    'order': 1,
                    'duration': 432000.0,
                    'type_employment': 'full',
                    'unexpected_holidays': 'ignoring',
                    'weekend_behaviour': 'ignoring',
                    'slots': [
                        {
                            'composition_id': composition.id,
                            'show_in_staff': False,
                            'points_per_hour': 1,
                        }
                    ],
                }
            ]
        }
    )
    assert response.status_code == status.HTTP_200_OK, response.text

    shifts = query_all_shifts_by_schedule(scope_session, schedule.id)
    assert shifts.count()
    assert all([s.staff_id == composition_participants.staff.id for s in shifts])


def test_people_allocated_apply_datetime(
    scope_session, client, schedule_data_with_composition,
    composition_factory,
):
    """Проверяем, что перераспределение со старта шифта, актуального на начало ревизии"""
    schedule = schedule_data_with_composition.schedule
    schedule.threshold_day = datetime.timedelta(days=15)
    scope_session.commit()
    interval = schedule_data_with_composition.interval_1
    slot = schedule_data_with_composition.slot_1
    initial_creation_of_shifts(schedule.id)

    apply_datetime = localize(now() + datetime.timedelta(days=3))
    current_shift = scope_session.query(Shift).filter(
        Shift.end >= apply_datetime,
        Shift.replacement_for_id.is_(None)
    ).order_by(Shift.start).first()
    with patch('watcher.tasks.people_allocation.start_people_allocation.delay') as people_allocation_func:
        response = client.put(
            '/api/watcher/v1/interval/',
            json={
                'schedule_id': schedule.id,
                'apply_datetime': apply_datetime.isoformat(),
                'intervals': [
                    {
                        'id': interval.id,
                        'schedule_id': schedule.id,
                        'order': interval.order,
                        'duration': interval.duration.total_seconds(),
                        'type_employment': interval.type_employment,
                        'unexpected_holidays': interval.unexpected_holidays,
                        'weekend_behaviour': interval.weekend_behaviour,
                        'slots': [
                            {
                                'id': slot.id,
                                'interval_id': interval.id,
                                'composition_id': composition_factory(service=schedule.service).id,
                            }
                        ],
                    }
                ]
            }
        )

        assert response.status_code == status.HTTP_200_OK, response.text
        revision_id = response.json()[0]['revision_id']
        # проверим что шифт обрезался
        scope_session.refresh(current_shift)
        assert current_shift.end == apply_datetime
        assert current_shift.slot.interval.revision_id != revision_id
        assert current_shift.next.slot.interval.revision_id == revision_id
        # с конца шифта актуального на начало ревизии
        people_allocation_func.assert_called_once_with(
            schedules_group_id=schedule.schedules_group_id,
            start_date=localize(current_shift.end),
            force_task_delay=True,
        )


@pytest.mark.parametrize('case', ('success', 'wrong_number', 'wrong_primary', 'different_composition'))
def test_put_interval_with_primary_rotation_wrong_slots(
    client, service_factory, schedule_factory, composition_factory,
    scope_session, case
):
    service = service_factory()
    schedule = schedule_factory(service=service)
    composition = composition_factory(service=service)
    slots = [
        {
            'composition_id': composition.id,
            'is_primary': True
        },
        {
            'composition_id': composition.id,
            'is_primary': False
        },
    ]
    if case == 'wrong_number':
        slots.pop()
    elif case == 'wrong_primary':
        slots[1]['is_primary'] = True
    elif case == 'different_composition':
        slots[1]['composition_id'] = composition_factory(service=service).id

    response = client.put(
        '/api/watcher/v1/interval/',
        json={
            'schedule_id': schedule.id,
            'intervals': [{
                'schedule_id': schedule.id,
                'type_employment': enums.IntervalTypeEmployment.full,
                'duration': 1200,
                'order': 1,
                'primary_rotation': True,
                'slots': slots
            }]
        }
    )
    data = response.json()
    if case == 'success':
        assert response.status_code == status.HTTP_200_OK, response.text
        assert data[0]['primary_rotation']
    else:
        assert response.status_code == status.HTTP_400_BAD_REQUEST
        assert data['detail'][0]['msg'] == {
            'ru': 'В переданных сменах должен быть один пул дежурных, а также по одному запасному и основному дежурному.',
            'en': 'In the uploaded shifts there must be one same duty pool, as well as one backup and primary duty person in each.',
        }


def test_put_interval_future_revisions(scope_session, client, interval_factory, revision_factory, schedule_factory):
    schedule = schedule_factory()

    now_rev = revision_factory(state=enums.RevisionState.active, schedule=schedule)
    interval_factory(schedule=schedule, revision=now_rev, duration=datetime.timedelta(hours=2))

    future_rev = revision_factory(
        prev=now_rev,
        schedule=schedule,
        state=enums.RevisionState.active,
        apply_datetime=now() + datetime.timedelta(days=10),
    )
    future_interval = interval_factory(schedule=schedule, revision=future_rev, duration=datetime.timedelta(hours=2))

    response = client.put(
        '/api/watcher/v1/interval/',
        json={
            'schedule_id': schedule.id,
            'revision_id': future_rev.id,
            'apply_datetime': future_rev.apply_datetime.isoformat(),
            'intervals': [
                {
                    'id': future_interval.id,
                    'schedule_id': future_interval.schedule.id,
                    'duration': 7200,
                    'order': future_interval.order,
                }
            ]
        }
    )

    data = response.json()

    scope_session.refresh(now_rev)
    scope_session.refresh(future_rev)
    new_future_rev = scope_session.query(Revision).filter(Revision.id == data[0]['id']).first()

    assert now_rev.state == enums.RevisionState.active
    assert now_rev.next == new_future_rev

    assert future_rev.state == enums.RevisionState.disabled
    assert future_rev.next is None

    assert new_future_rev.state == enums.RevisionState.active
    assert new_future_rev.next is None


def test_put_interval_with_same_apply_datetime(
    scope_session, client, interval_factory,
    revision_factory, schedule_factory
):
    schedule = schedule_factory()
    current_now = now()

    revision = revision_factory(
        state=enums.RevisionState.active, schedule=schedule,
        apply_datetime=current_now + datetime.timedelta(days=10),
    )
    interval_factory(
        schedule=schedule, revision=revision,
        duration=datetime.timedelta(hours=2),
    )

    next_revision = revision_factory(
        prev=revision,
        schedule=schedule,
        state=enums.RevisionState.active,
        apply_datetime=current_now + datetime.timedelta(days=20),
    )
    future_interval = interval_factory(
        schedule=schedule, revision=next_revision,
        duration=datetime.timedelta(hours=2)
    )

    response = client.put(
        '/api/watcher/v1/interval/',
        json={
            'schedule_id': schedule.id,
            'apply_datetime': (current_now + datetime.timedelta(days=10)).isoformat(),
            'intervals': [
                {
                    'schedule_id': future_interval.schedule.id,
                    'duration': 7200,
                    'order': 1,
                }
            ]
        }
    )
    assert response.status_code == status.HTTP_200_OK, response.text

    data = response.json()

    scope_session.refresh(revision)
    scope_session.refresh(next_revision)
    new_revision = scope_session.query(Revision).filter(Revision.id == data[0]['id']).first()

    assert revision.state == enums.RevisionState.disabled
    assert revision.next is None

    assert next_revision.state == enums.RevisionState.active
    assert next_revision.next is None

    assert new_revision.state == enums.RevisionState.active
    assert new_revision.next is next_revision


@pytest.mark.parametrize('valid_slots', (True, False))
def test_put_interval_with_backup_takes_primary_shift(
    scope_session, client, schedule_factory, composition_factory, valid_slots
):
    schedule = schedule_factory()
    composition = composition_factory(service=schedule.service)

    slots_data = [
        {
            'composition_id': composition.id,
            'is_primary': True
        },
        {
            'composition_id': composition.id,
            'is_primary': False
        }
    ]
    if not valid_slots:
        slots_data[1]['is_primary'] = True

    with patch('watcher.tasks.people_allocation.start_people_allocation.delay'):
        response = client.put(
            '/api/watcher/v1/interval/',
            json={
                'schedule_id': schedule.id,
                'apply_datetime': now().isoformat(),
                'intervals': [
                    {
                        'schedule_id': schedule.id,
                        'duration': 7200,
                        'order': 1,
                        'backup_takes_primary_shift': True,
                        'slots': slots_data,
                    }
                ]
            }
        )

    data = response.json()
    if valid_slots:
        assert response.status_code == status.HTTP_200_OK
        assert data[0]['backup_takes_primary_shift']
    else:
        assert response.status_code == status.HTTP_400_BAD_REQUEST
        assert data['context']['message'] == {
            'ru': 'С настройкой backup_takes_primary_shift должен быть передан хотя бы один backup слот',
            'en': 'At least one backup slot must be provided in presence of backup_takes_primary_shift option',
        }


@pytest.mark.parametrize('initial_rating', (0, 100, None))
def test_put_rating_update(client, scope_session, staff_factory, schedule_factory, composition_factory,
                           composition_participants_factory, rating_factory, interval_factory, slot_factory,
                           revision_factory, initial_rating):
    '''
    Проверяем создание рейтинга человека и при изменении композиции в расписании.
    '''

    schedule = schedule_factory()

    now_rev = revision_factory(state=enums.RevisionState.active, schedule=schedule)

    interval = interval_factory(schedule=schedule, revision=now_rev, duration=datetime.timedelta(hours=2))

    composition1 = composition_factory(service=schedule.service)
    composition2 = composition_factory(service=schedule.service)

    staff1 = staff_factory()
    staff2 = staff_factory()
    staff3 = staff_factory()
    rating_factory(schedule=schedule, staff=staff1, rating=42)
    rating_factory(schedule=schedule, staff=staff2, rating=42)
    if initial_rating == 0:
        rating_factory(schedule=schedule, staff=staff3, rating=0)
    if initial_rating == 100:
        rating_factory(schedule=schedule, staff=staff3, rating=100)

    composition_participants_factory(staff=staff1, composition=composition1)
    composition_participants_factory(staff=staff2, composition=composition1)
    composition_participants_factory(staff=staff1, composition=composition2)
    composition_participants_factory(staff=staff2, composition=composition2)
    composition_participants_factory(staff=staff3,
                                     composition=composition2)  # добавим человека, у которого нет рейтинга, ожидаем что ему рейтинг добавится

    slot1 = slot_factory(interval=interval, composition=composition1)

    response = client.put(
        '/api/watcher/v1/interval/',
        json={
            'schedule_id': schedule.id,
            'intervals': [
                {
                    'id': interval.id,
                    'schedule_id': schedule.id,
                    'duration': interval.duration.total_seconds(),
                    'order': interval.order,
                    'slots': [
                        {
                            'id': slot1.id,
                            'interval_id': interval.id,
                            'composition_id': composition2.id,
                        },
                    ],
                }
            ]
        }
    )

    assert response.status_code == status.HTTP_200_OK, response.text
    if initial_rating is not None:
        assert get_existing_ratings_by_schedule_staff_ids(
            db=scope_session, schedule_ids=[schedule.id],
            staff_ids=[staff3.id]
        )[0].rating == Decimal(f'{initial_rating}')
    else:
        assert get_existing_ratings_by_schedule_staff_ids(
            db=scope_session, schedule_ids=[schedule.id],
            staff_ids=[staff3.id]
        )[0].rating == Decimal('43')
