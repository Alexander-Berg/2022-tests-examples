import datetime
from unittest.mock import patch, call

from watcher.tasks.people_allocation import start_people_allocation
from watcher.logic.timezone import now
from watcher.db import Event
from watcher import enums
from watcher.tasks.task_queue import send_scheduled_tasks


def test_with_force_task_delay(scope_session, set_testing):
    start_people_allocation.delay(
        schedules_group_id=123,
        force_task_delay=True,
    )

    events = scope_session.query(Event).count()
    assert events == 0


def test_create_task_event_for_start_people_allocation(scope_session, set_testing):
    schedule_group_id = 123
    start_date_one = now()
    start_date_two = now() - datetime.timedelta(hours=3)
    start_people_allocation.delay(
        schedules_group_id=schedule_group_id,
        start_date=start_date_one,
    )

    start_people_allocation.delay(
        schedules_group_id=schedule_group_id,
        start_date=start_date_two,
    )

    events = scope_session.query(Event).all()
    assert [event.object_data for event in events] == [
        {'args': [[],
                  {'schedules_group_id': schedule_group_id,
                   'start_date': start_date_one.isoformat()}],
         'kwargs': {},
         'name': 'watcher.tasks.people_allocation.start_people_allocation'},
        {'args': [[],
                  {'schedules_group_id': schedule_group_id,
                   'start_date': start_date_two.isoformat()}],
         'kwargs': {},
         'name': 'watcher.tasks.people_allocation.start_people_allocation'}
    ]
    task_event = events[0]
    assert task_event.type == enums.EventType.task
    assert task_event.source == enums.EventSource.internal


def test_send_task_event_for_start_people_allocation(event_factory, scope_session):
    schedule_group_id = 123
    schedule_group_id_1 = 124
    start_date_one = now()
    start_date_two = now() - datetime.timedelta(hours=3)

    event_now = event_factory(
        type=enums.EventType.task,
        source=enums.EventSource.internal,
        created_at=now() - datetime.timedelta(minutes=10),
        object_data={
            'args': [
                [],
                {
                    'schedules_group_id': schedule_group_id,
                    'start_date': start_date_one.isoformat()
                }
            ],
         'kwargs': {},
         'name': 'watcher.tasks.people_allocation.start_people_allocation'
        }
    )

    event_old = event_factory(
        type=enums.EventType.task,
        source=enums.EventSource.internal,
        created_at=now() - datetime.timedelta(minutes=10),
        object_data={
            'args': [
                [],
                {
                    'schedules_group_id': schedule_group_id,
                    'start_date': start_date_two.isoformat()
                }
            ],
            'kwargs': {},
            'name': 'watcher.tasks.people_allocation.start_people_allocation'
        }
    )

    event_with_none = event_factory(
        type=enums.EventType.task,
        source=enums.EventSource.internal,
        created_at=now() - datetime.timedelta(minutes=10),
        object_data={
            'args': [
                [],
                {
                    'schedules_group_id': schedule_group_id_1,
                    'start_date': None
                }
            ],
            'kwargs': {},
            'name': 'watcher.tasks.people_allocation.start_people_allocation'
        }
    )

    event_without_none = event_factory(
        type=enums.EventType.task,
        source=enums.EventSource.internal,
        created_at=now() - datetime.timedelta(minutes=10),
        object_data={
            'args': [
                [],
                {
                    'schedules_group_id': schedule_group_id_1,
                    'start_date': start_date_two.isoformat(),
                }
            ],
            'kwargs': {},
            'name': 'watcher.tasks.people_allocation.start_people_allocation'
        }
    )

    event_factory(
        type=enums.EventType.task,
        source=enums.EventSource.internal,
        created_at=now() - datetime.timedelta(minutes=1)
    )
    with patch('watcher.tasks.people_allocation.start_people_allocation') as mock_start_people_allocation:
        send_scheduled_tasks()

    mock_start_people_allocation.delay.assert_has_calls([
        call(schedules_group_id=schedule_group_id_1, start_date=None, force_task_delay=True),
        call(schedules_group_id=schedule_group_id, start_date=start_date_two.isoformat(), force_task_delay=True),
    ], any_order=True)

    events = scope_session.query(Event).filter(Event.state == enums.EventState.processed)

    assert {event.id for event in events} == {
        event_now.id, event_old.id,
        event_with_none.id, event_without_none.id,
    }
