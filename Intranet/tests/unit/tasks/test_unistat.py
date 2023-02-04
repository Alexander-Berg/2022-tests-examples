import datetime

import pytest

from watcher import enums
from watcher.db import (
    TaskMetric,
    Unistat,
)
from watcher.logic.timezone import now
from watcher.tasks.composition import update_compositions
from watcher.tasks.unistat import calculate_unistat_metrics
from watcher.tasks.generating_shifts import process_new_shifts_for_active_schedules
from watcher.tasks.people_allocation import start_people_allocation


@pytest.fixture
def unistat_data(abc_migration_factory, event_factory, notification_factory, task_metrics_factory, staff_factory,
                 shift_factory, schedule_factory, scope_session):

    now_time = now()
    four_hours_ago = now_time - datetime.timedelta(hours=4)
    three_hours_ago = now_time - datetime.timedelta(hours=3)
    two_hours_ago = now_time - datetime.timedelta(hours=2)
    one_hour_ago = now_time - datetime.timedelta(hours=1)
    half_hour_ago = now_time - datetime.timedelta(minutes=30)
    one_hour_ahead = now_time + datetime.timedelta(hours=1)
    two_hour_ahead = now_time + datetime.timedelta(hours=2)
    three_hour_ahead = now_time + datetime.timedelta(hours=3)

    # schedules_with_uncreated_shifts, schedules_long_time_recalculation
    schedule = schedule_factory(updated_at=half_hour_ago, recalculation_in_process=True)
    staff = staff_factory()
    shift = shift_factory(schedule=schedule)

    # failed_abc_migrations
    abc_migration_factory(status=enums.AbcMigrationStatus.preparing_fail)
    abc_migration_factory(status=enums.AbcMigrationStatus.finalizing_fail)
    abc_migration_factory(status=enums.AbcMigrationStatus.preparing)

    # unprocessed_event_count
    event_factory(state=enums.EventState.new, table='test1')
    event_factory(state=enums.EventState.scheduled, table='test2')
    event_factory(state=enums.EventState.processed, table='test1')

    # outdated_notifications
    notification_factory(shift=shift, state=enums.NotificationState.new)
    notification_factory(shift=shift, state=enums.NotificationState.outdated)
    notification_factory(shift=shift, state=enums.NotificationState.outdated)

    # unstarted_shifts
    shift_factory(schedule=schedule, status=enums.ShiftStatus.scheduled, staff=staff,
                  start=four_hours_ago, end=one_hour_ahead)
    shift_factory(schedule=schedule, status=enums.ShiftStatus.scheduled, staff=staff,
                  start=three_hours_ago, end=one_hour_ahead)
    shift_factory(schedule=schedule, status=enums.ShiftStatus.scheduled, staff=staff,
                  start=half_hour_ago, end=one_hour_ahead)

    # unfinished_shifts
    shift_factory(schedule=schedule, status=enums.ShiftStatus.active, start=four_hours_ago, end=three_hours_ago)
    shift_factory(schedule=schedule, status=enums.ShiftStatus.active, start=three_hours_ago, end=two_hours_ago)
    shift_factory(schedule=schedule, status=enums.ShiftStatus.active, start=one_hour_ago, end=now_time)

    # unallocated_shifts
    shift_factory(schedule=schedule, start=one_hour_ahead, end=two_hour_ahead)
    shift_factory(schedule=schedule, start=two_hour_ahead, end=three_hour_ahead)

    task_metrics_factory(task_name='task_1', send_to_unistat=True)
    task_metrics_factory(task_name='task_2', send_to_unistat=False)


def test_calculate_unistat_metrics(scope_session, unistat_data):

    calculate_unistat_metrics()

    unistat_records = scope_session.query(Unistat).all()
    assert len(unistat_records) == 1

    metrics = unistat_records[0].metrics
    assert 'task_1' in metrics
    assert 'task_2' not in metrics
    assert metrics['failed_abc_migrations'] == 2
    assert metrics['outdated_notifications'] == 2
    assert metrics['unprocessed_event_count'] == 2
    assert metrics['unprocessed_event_count_test1'] == 1
    assert metrics['unprocessed_event_count_test2'] == 1
    assert metrics['unstarted_shifts'] == 2
    assert metrics['unfinished_shifts'] == 2
    assert metrics['unallocated_shifts'] == 2
    assert metrics['schedules_with_uncreated_shifts'] == 1
    assert metrics['schedules_long_time_recalculation'] == 1


def test_task_metric_creation(scope_session, schedule_factory):

    schedule = schedule_factory()

    process_new_shifts_for_active_schedules()
    update_compositions()
    start_people_allocation(schedules_group_id=schedule.schedules_group_id)

    task_metrics = scope_session.query(TaskMetric).all()

    assert len(task_metrics) == 2
    assert {el.task_name for el in task_metrics} == {'process_new_shifts_for_active_schedules', 'update_compositions'}
