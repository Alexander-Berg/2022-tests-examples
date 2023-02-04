import datetime

import pytest
from unittest.mock import patch
from freezegun import freeze_time

from watcher.crud.problem import query_active_problems_by_shift_ids, query_active_problems
from watcher.logic.timezone import now, localize
from watcher.tasks.problem import (
    create_problems_for_staff_has_gap_shifts,
    resolve_shifts_problems,
)
from watcher import enums


@patch('watcher.tasks.problem.start_people_allocation')
@pytest.mark.parametrize('approved', [True, False])
def test_create_problems_for_staff_has_gap_approved_shifts(
    start_people_allocation, shift_factory, staff_factory,
    gap_factory, schedule_factory, scope_session, approved
):
    schedule = schedule_factory(length_of_absences=datetime.timedelta(minutes=15))
    watcher = staff_factory()
    shift = shift_factory(
        schedule=schedule,
        staff=watcher,
        start=now()-datetime.timedelta(days=2),
        end=now()+datetime.timedelta(days=1),
        approved=approved,
    )
    create_problems_for_staff_has_gap_shifts.delay(staff_ids=[watcher.id])
    problems = query_active_problems_by_shift_ids(scope_session, [shift.id])
    assert shift.staff
    assert not problems.count()

    gap = gap_factory(staff=watcher, start=now()-datetime.timedelta(days=1))
    create_problems_for_staff_has_gap_shifts.delay(staff_ids=[watcher.id])
    problems = query_active_problems_by_shift_ids(scope_session, [shift.id]).all()

    if approved:
        assert len(problems) == 1
        assert problems[0].shift.id == shift.id
        assert problems[0].duty_gap_id == gap.id
        assert not problems[0].manual_gap_id
        assert not start_people_allocation.delay.called
    else:
        assert not problems
        assert not shift.staff
        start_people_allocation.delay.assert_called_with(
            schedules_group_id=shift.schedule.schedules_group_id,
            start_date=localize(shift.start),
        )


@pytest.mark.parametrize('gap_model', ('Gap', 'ManualGap'))
def test_create_problems_for_staff_has_past_gap(
    shift_factory, staff_factory, gap_factory, manual_gap_factory, manual_gap_settings_factory, schedule_factory,
    scope_session, gap_model
):
    schedule = schedule_factory(length_of_absences=datetime.timedelta(minutes=15))
    watcher = staff_factory()
    problem_shifts = [
        shift_factory(
            schedule=schedule,
            staff=watcher,
            start=now() - datetime.timedelta(days=3),
            end=now() + datetime.timedelta(days=1),
            approved=True
        ),
    ]
    unproblem_shifts = [
        shift_factory(
            schedule=schedule,
            staff=watcher,
            start=now() - datetime.timedelta(days=3),
            end=now() - datetime.timedelta(days=1),
            approved=True
        ),
    ]
    if gap_model == 'Gap':
        gap = gap_factory(
            staff=watcher,
            start=now() - datetime.timedelta(days=4),
            end=now() - datetime.timedelta(days=2)
        )
    elif gap_model == 'ManualGap':
        gap_settings = manual_gap_settings_factory(
            staff=watcher,
            all_services=True,
        )
        gap = manual_gap_factory(
            gap_settings=gap_settings,
            staff=watcher,
            start=now() - datetime.timedelta(days=4),
            end=now() - datetime.timedelta(days=2),
        )

    create_problems_for_staff_has_gap_shifts.delay(staff_ids=[watcher.id])
    shift_ids = [shift.id for shift in problem_shifts + unproblem_shifts]
    problems = query_active_problems_by_shift_ids(scope_session, shift_ids).all()
    problem_shift_ids = [p.shift.id for p in problems]
    assert len(problems) == len(problem_shifts)
    assert sorted(problem_shift_ids) == sorted([shift.id for shift in problem_shifts])
    assert set(problem_shift_ids).difference([shift.id for shift in unproblem_shifts]) == set(problem_shift_ids)
    for problem in problems:
        if gap_model == 'ManualGap':
            assert problem.manual_gap_id == gap.id
            assert not problem.duty_gap_id
        else:
            assert problem.duty_gap_id == gap.id
            assert not problem.manual_gap_id


def test_resolve_problems_for_staff_has_gap(schedule_factory, staff_factory, shift_factory, gap_factory, scope_session):
    schedule = schedule_factory(length_of_absences=datetime.timedelta(minutes=15))
    watcher = staff_factory()
    problem_shifts = [
        shift_factory(
            schedule=schedule,
            staff=watcher,
            start=now() - datetime.timedelta(days=3),
            end=now() + datetime.timedelta(days=1),
            approved=True
        ) for _ in range(2)
    ]
    gap_factory(staff=watcher, start=now() - datetime.timedelta(days=4), end=now() - datetime.timedelta(days=2))
    create_problems_for_staff_has_gap_shifts.delay(staff_ids=[watcher.id])

    problems = query_active_problems(scope_session).all()
    assert len(problems) == 2

    # поменяем у первого дежурного, а у шифта - начало, так чтобы дежурный уже мог дежурить
    problem_shifts[0].staff = staff_factory()
    problem_shifts[1].start = now() - datetime.timedelta(days=1)
    scope_session.commit()

    resolve_shifts_problems.apply()
    problems = query_active_problems(scope_session).all()
    assert len(problems) == 0


def test_resolve_problem_staff_has_gap_in_past(
    schedule_factory, staff_factory, shift_factory, gap_factory, scope_session
):
    schedule = schedule_factory(length_of_absences=datetime.timedelta(minutes=15))
    watcher = staff_factory()
    shift_factory(
        schedule=schedule, staff=watcher, approved=True,
        start=now() - datetime.timedelta(days=3), end=now() - datetime.timedelta(days=1),
    )
    current_problem_1 = shift_factory(
        schedule=schedule, staff=watcher, approved=True,
        start=now() - datetime.timedelta(days=1), end=now() + datetime.timedelta(days=1),
    )
    current_problem_2 = shift_factory(
        schedule=schedule, staff=watcher, approved=True,
        start=now() + datetime.timedelta(days=1), end=now() + datetime.timedelta(days=3),
    )
    gap_factory(staff=watcher, start=now() - datetime.timedelta(days=4), end=now() + datetime.timedelta(days=4))

    with freeze_time('2022-01-01'):
        create_problems_for_staff_has_gap_shifts.delay(staff_ids=[watcher.id])
    assert len(query_active_problems(scope_session).all()) == 3

    resolve_shifts_problems.apply()
    problems = query_active_problems(scope_session).all()
    assert len(problems) == 2
    expected_shift_ids = {current_problem_1.id, current_problem_2.id}
    assert all(problem.shift_id in expected_shift_ids for problem in problems)


@pytest.mark.parametrize('gap_deleted', (True, False))
def test_resolve_problems_for_nobody_on_duty(
    schedule_factory, staff_factory, shift_factory,
    gap_factory, scope_session, problem_factory,
    gap_deleted,
):
    staff = staff_factory()
    shift = shift_factory(staff=None, start=datetime.datetime(2022, 1, 1))
    gap = gap_factory(
        staff=staff,
        start=now() - datetime.timedelta(days=4),
        end=now() + datetime.timedelta(days=2),
        status=enums.GapStatus.deleted if gap_deleted else enums.GapStatus.active,
    )
    problem = problem_factory(
        reason=enums.ProblemReason.nobody_on_duty,
        shift=shift, duty_gap=gap,
        staff=None,
    )
    with patch('watcher.tasks.problem.start_people_allocation') as mock_allocate:
        resolve_shifts_problems()
    scope_session.refresh(problem)
    if gap_deleted:
        assert problem.status == enums.ProblemStatus.resolved
        mock_allocate.delay.assert_called_once()
    else:
        assert problem.status == enums.ProblemStatus.new
        mock_allocate.delay.assert_not_called()
