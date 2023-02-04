import datetime
import pytest
from unittest.mock import patch

from watcher import enums
from watcher.db import (
    ManualGap,
    Shift,
    Problem,
)
from watcher.crud.shift import query_all_shifts_by_schedule
from watcher.logic.shift import bulk_set_shift_approved
from watcher.logic.manual_gap import generate_gaps
from watcher.logic.timezone import now
from watcher.tasks import (
    update_manual_gaps,
    start_generate_manual_gaps,
    initial_creation_of_shifts,
    start_people_allocation,
)


def test_update_manual_gaps(scope_session, manual_gap_settings_factory, schedule_factory, service_factory,
                            revision_factory, interval_factory, slot_factory, composition_participants_factory):
    current_now = now()
    gap_settings = manual_gap_settings_factory(
        start=current_now+datetime.timedelta(hours=1),
        end=current_now+datetime.timedelta(hours=2),
        recurrence=enums.ManualGapRecurrence.week,
    )
    schedules = [schedule_factory(), schedule_factory()]
    schedules[1].schedules_group = schedules[0].schedules_group
    schedules[1].service = schedules[0].service
    gap_settings.schedules = schedules

    for schedule in schedules:
        revision = revision_factory(schedule=schedule, state=enums.RevisionState.active)
        interval = interval_factory(schedule=schedule, duration=datetime.timedelta(days=5), revision=revision)
        slot = slot_factory(interval=interval)
        composition_participants_factory(composition=slot.composition, staff=gap_settings.staff)

    with patch('watcher.tasks.people_allocation.start_people_allocation'):
        initial_creation_of_shifts(schedules[0].id)
        initial_creation_of_shifts(schedules[1].id)

    with patch('watcher.tasks.people_allocation.start_people_allocation.delay') as people_allocation_func:
        update_manual_gaps(gap_settings_id=gap_settings.id)

    people_allocation_func.assert_not_called()
    scope_session.refresh(gap_settings)
    gaps = scope_session.query(ManualGap).all()
    assert len(gaps) == 9


@pytest.mark.parametrize('is_approved', (False, True))
def test_update_manual_gaps_create_problems(
    scope_session, manual_gap_settings_factory, schedule_factory, revision_factory,
    interval_factory, slot_factory, composition_participants_factory, is_approved
):
    current_now = now()
    gap_settings = manual_gap_settings_factory(
        start=current_now + datetime.timedelta(hours=1),
        end=current_now + datetime.timedelta(hours=2),
        recurrence=enums.ManualGapRecurrence.month,
    )
    schedule = schedule_factory()
    gap_settings.schedules = [schedule]

    revision = revision_factory(schedule=schedule, state=enums.RevisionState.active)
    interval = interval_factory(schedule=schedule, duration=datetime.timedelta(days=3), revision=revision)
    slot = slot_factory(interval=interval)
    composition_participants_factory(composition=slot.composition, staff=gap_settings.staff)

    initial_creation_of_shifts(schedule.id)
    shifts = scope_session.query(Shift).filter(Shift.staff_id == gap_settings.staff_id).order_by(Shift.start)
    if is_approved:
        bulk_set_shift_approved(db=scope_session, db_objs=shifts, approved=True, author_id=gap_settings.staff_id)
        scope_session.commit()

    with patch('watcher.tasks.people_allocation.start_people_allocation.delay') as people_allocation_func:
        update_manual_gaps(gap_settings_id=gap_settings.id)

    problems = scope_session.query(Problem).join(Shift, Shift.id == Problem.shift_id).filter(
        Problem.staff_id == gap_settings.staff_id,
        Problem.status != enums.ProblemStatus.resolved,
    ).order_by(Shift.start).all()
    if is_approved:
        assert len(problems) == 2
        assert problems[0].shift_id == shifts[0].id
        assert problems[1].shift_id == shifts[10].id
        people_allocation_func.assert_not_called()
    else:
        assert len(problems) == 0
        people_allocation_func.assert_called_once()


def test_update_manual_gaps_with_existing_problems(
    scope_session, manual_gap_settings_factory, schedule_factory, revision_factory,
    interval_factory, slot_factory, composition_participants_factory
):
    current_now = now()
    gap_settings = manual_gap_settings_factory(
        start=current_now + datetime.timedelta(hours=1),
        end=current_now + datetime.timedelta(hours=2),
        recurrence=enums.ManualGapRecurrence.month,
    )
    schedule = schedule_factory()
    gap_settings.schedules = [schedule]

    revision = revision_factory(schedule=schedule, state=enums.RevisionState.active)
    interval = interval_factory(schedule=schedule, duration=datetime.timedelta(days=5), revision=revision)
    slot = slot_factory(interval=interval)
    composition_participants_factory(composition=slot.composition, staff=gap_settings.staff)

    initial_creation_of_shifts(schedule.id)
    shifts = scope_session.query(Shift).filter(Shift.staff_id == gap_settings.staff_id).order_by(Shift.start).all()
    bulk_set_shift_approved(db=scope_session, db_objs=shifts, approved=True, author_id=gap_settings.staff_id)
    scope_session.commit()

    update_manual_gaps(gap_settings_id=gap_settings.id)
    problems = scope_session.query(Problem).filter(
        Problem.staff_id == gap_settings.staff_id,
        Problem.status != enums.ProblemStatus.resolved,
    ).all()
    assert len(problems) == 2

    gap_settings.recurrence = enums.ManualGapRecurrence.day
    scope_session.commit()

    update_manual_gaps(gap_settings_id=gap_settings.id)
    problems = scope_session.query(Problem).filter(
        Problem.staff_id == gap_settings.staff_id,
        Problem.status != enums.ProblemStatus.resolved,
    ).all()
    assert len(problems) == len(shifts)


def test_create_new_manual_gaps_with_existing_problems(
    scope_session, manual_gap_settings_factory, schedule_factory, revision_factory, staff_factory,
    interval_factory, slot_factory, composition_participants_factory
):
    current_now = now()
    staff = staff_factory()
    schedule = schedule_factory()
    revision = revision_factory(schedule=schedule, state=enums.RevisionState.active)
    interval = interval_factory(schedule=schedule, duration=datetime.timedelta(days=5), revision=revision)
    slot = slot_factory(interval=interval)
    composition_participants_factory(composition=slot.composition, staff=staff)

    gap_settings = manual_gap_settings_factory(
        staff=staff,
        start=current_now + datetime.timedelta(hours=1),
        end=current_now + datetime.timedelta(hours=2),
        recurrence=enums.ManualGapRecurrence.once,
    )
    gap_settings.schedules = [schedule]
    scope_session.commit()

    initial_creation_of_shifts(schedule.id)
    shifts = scope_session.query(Shift).filter(Shift.staff_id == staff.id).order_by(Shift.start).all()
    bulk_set_shift_approved(db=scope_session, db_objs=shifts, approved=True, author_id=staff.id)
    scope_session.commit()

    update_manual_gaps(gap_settings_id=gap_settings.id)
    problems = scope_session.query(Problem).filter(
        Problem.staff_id == staff.id,
        Problem.status != enums.ProblemStatus.resolved,
    ).all()
    assert len(problems) == 1

    gap_settings_2 = manual_gap_settings_factory(
        staff=staff,
        start=current_now + datetime.timedelta(hours=1),
        end=current_now + datetime.timedelta(hours=2),
        recurrence=enums.ManualGapRecurrence.once,
    )
    gap_settings_2.schedules = [schedule]
    scope_session.commit()

    update_manual_gaps(gap_settings_id=gap_settings_2.id)
    problems = scope_session.query(Problem).filter(
        Problem.staff_id == staff.id,
        Problem.status != enums.ProblemStatus.resolved,
    ).all()
    assert len(problems) == 1

    gap_settings.is_active = False
    scope_session.commit()
    update_manual_gaps(gap_settings_id=gap_settings.id)
    problems = scope_session.query(Problem).filter(
        Problem.staff_id == staff.id,
        Problem.status != enums.ProblemStatus.resolved,
    ).all()
    assert len(problems) == 1


def test_people_allocation_with_update_manual_gaps(
    scope_session, manual_gap_settings_factory, manual_gap_factory, schedule_factory, service_factory, revision_factory,
    interval_factory, slot_factory, composition_participants_factory
):
    current_now = now()
    gap_settings_1 = manual_gap_settings_factory()
    gap_settings_2 = manual_gap_settings_factory()
    schedule = schedule_factory()
    gap_settings_1.schedules = [schedule]
    gap_settings_2.services = [schedule.service]

    [manual_gap_factory(
        start=current_now+datetime.timedelta(days=i),
        end=current_now+datetime.timedelta(days=i, hours=1),
        gap_settings=gap_settings_1,
        staff=gap_settings_1.staff,
    ) for i in range(2)]
    manual_gap_factory(
        start=current_now + datetime.timedelta(days=7),
        end=current_now + datetime.timedelta(days=7, hours=3),
        gap_settings=gap_settings_2,
        staff=gap_settings_2.staff,
    )

    revision = revision_factory(schedule=schedule, state=enums.RevisionState.active)
    interval = interval_factory(schedule=schedule, duration=datetime.timedelta(days=5), revision=revision)
    slot = slot_factory(interval=interval)
    composition_participants_factory(composition=slot.composition, staff=gap_settings_1.staff)
    composition_participants_factory(composition=slot.composition, staff=gap_settings_2.staff)

    scope_session.commit()

    with patch('watcher.tasks.people_allocation.start_people_allocation'):
        initial_creation_of_shifts(schedule_id=schedule.id)

    start_people_allocation(schedules_group_id=schedule.schedules_group_id)

    schedule_shifts = query_all_shifts_by_schedule(db=scope_session, schedule_id=schedule.id).all()
    assert schedule_shifts[0].staff == gap_settings_2.staff
    assert schedule_shifts[1].staff == gap_settings_1.staff
    assert all([shift.staff for shift in schedule_shifts[2:]])


@pytest.mark.parametrize(
    'recurrence',
    (
        enums.ManualGapRecurrence.once,
        enums.ManualGapRecurrence.day,
        enums.ManualGapRecurrence.week,
        enums.ManualGapRecurrence.fortnight,
        enums.ManualGapRecurrence.month
    )
)
def test_start_generate_manual_gaps(scope_session, manual_gap_settings_factory, schedule_factory, recurrence):
    current_now = now()
    gap_settings = manual_gap_settings_factory(
        start=current_now + datetime.timedelta(hours=1),
        end=current_now + datetime.timedelta(hours=2),
        recurrence=recurrence,
    )
    gap_settings.schedules = [schedule_factory()]

    gaps = generate_gaps(
        gap_settings=gap_settings, from_datetime=current_now,
        to_datetime=current_now + datetime.timedelta(days=30)
    )
    if gaps:
        scope_session.bulk_insert_mappings(ManualGap, gaps)
        scope_session.commit()

    with patch('watcher.tasks.people_allocation.start_people_allocation.delay'):
        start_generate_manual_gaps()

    all_gaps = scope_session.query(ManualGap).order_by(ManualGap.start).all()
    if recurrence == enums.ManualGapRecurrence.once:
        assert (len(gaps), len(all_gaps)) == (1, 1)
    elif recurrence == enums.ManualGapRecurrence.day:
        assert (len(gaps), len(all_gaps)) == (30, 60)
    elif recurrence == enums.ManualGapRecurrence.week:
        assert (len(gaps), len(all_gaps)) == (5, 9)
    elif recurrence == enums.ManualGapRecurrence.fortnight:
        assert (len(gaps), len(all_gaps)) == (3, 5)
    elif recurrence == enums.ManualGapRecurrence.month:
        assert (len(gaps), len(all_gaps)) == (1, 2)

    assert max([gap.start for gap in all_gaps]) < current_now + datetime.timedelta(days=60)
