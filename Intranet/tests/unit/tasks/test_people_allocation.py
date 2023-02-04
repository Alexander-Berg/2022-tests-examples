import _bisect
import datetime
import itertools

import pytest
import random
from collections import defaultdict, Counter, deque

from freezegun import freeze_time
from unittest.mock import patch

from watcher import enums
from watcher.config import settings
from watcher.crud.composition import query_participants_by_schedules_group, query_participants_by_composition
from watcher.crud.interval import query_intervals_by_current_revision
from watcher.crud.schedule import query_schedules_without_allocation
from watcher.crud.shift import query_all_shifts_by_schedule, query_schedule_main_shifts
from watcher.db import (
    Interval,
    Staff,
    Shift,
    Problem,
    Rating,
)
from watcher.logic.people_allocation import (
    find_staff_for_shift,
    check_participant_can_duty,
    get_participants_ratings_from_sequence,
    get_participants_last_shift_ends,
)
from watcher.logic.timezone import now, localize
from watcher.tasks.generating_shifts import initial_creation_of_shifts, sequence_shifts
from watcher.tasks.people_allocation import (
    get_people_allocation_data,
    start_people_allocation,
    start_people_allocation_for_groups_with_allocation_error,
)


def set_space_with_start_and_end(staff: Staff, start: int, end: int, space_factory, **kwargs):
    return space_factory(
        staff=staff,
        start=now() + datetime.timedelta(days=start),
        end=now() + datetime.timedelta(days=end),
        **kwargs
    )


def check_rotation_invariant(shifts: list[Shift], rotation: enums.IntervalRotation):
    if rotation is enums.IntervalRotation.backup_is_next_primary:
        backup = deque()
        for shift in shifts:
            # пропускаем смены начинающиеся в то же время что и последняя смена в очереди
            if len(backup) and shift.start == backup[-1].start:
                continue
            if not shift.is_primary:
                # бэкап смены записываем в очередь
                backup.appendleft(shift)
            elif len(backup):
                # проверяем условие ротации - предыдущий бэкап должен дежурить эту смену
                assert shift.staff_id == backup.pop().staff_id
    elif rotation == enums.IntervalRotation.primary_is_next_backup:
        primary = deque()
        for shift in shifts:
            # пропускаем смены начинающиеся в то же время что и последняя смена в очереди
            if len(primary) and shift.start == primary[-1].start:
                continue
            if shift.is_primary:
                # праймари смены записываем в очередь
                primary.appendleft(shift)
            elif len(primary):
                # проверяем условие ротации - предыдущий праймари должен дежурить эту смену
                assert shift.staff_id == primary.pop().staff_id
    elif rotation is enums.IntervalRotation.cross_rotation:
        last_backup = None
        last_primary = None
        for shift in shifts:
            if shift.prev and shift.start == shift.prev.start:
                continue

            if last_primary and not shift.is_primary:
                assert shift.staff_id == last_primary.staff_id
                last_primary = None
                continue
            if last_backup and shift.is_primary:
                assert shift.staff_id == last_backup.staff_id
                last_backup = None
                continue

            if shift.is_primary and not last_primary:
                last_primary = shift
            if not shift.is_primary and not last_backup:
                last_backup = shift


def test_gaps_bisect_insort(staff_factory, gap_factory, shift_factory):
    """ Поддержка списка из Gap отсортированным """
    staff = staff_factory()

    staff_gaps = [
        set_space_with_start_and_end(staff, 1, 3, gap_factory),
        set_space_with_start_and_end(staff, 8, 10, gap_factory),
        set_space_with_start_and_end(staff, 23, 40, gap_factory),
        set_space_with_start_and_end(staff, 60, 80, gap_factory),
        set_space_with_start_and_end(staff, 100, 120, gap_factory),
    ]

    random.seed(42)
    staff_unsorted_gaps = staff_gaps[:]
    random.shuffle(staff_unsorted_gaps)
    assert not staff_unsorted_gaps == staff_gaps

    staff_sorted_gaps = []
    for unsorted_gap in staff_unsorted_gaps:
        _bisect.insort_left(staff_sorted_gaps, unsorted_gap)
    assert staff_gaps == staff_sorted_gaps


@pytest.mark.parametrize(
    'length_of_absences,gap_start,gap_end,can_duty',
    (
        (  # пересекает в начале на 2 часа
            datetime.timedelta(hours=5),
            -datetime.timedelta(days=6),
            -datetime.timedelta(days=4, hours=22),
            True
        ),
        (  # пересекает в начале на 12 часов
            datetime.timedelta(hours=5),
            -datetime.timedelta(days=6),
            -datetime.timedelta(days=4, hours=10),
            False
        ),
        (  # пересекает в конце на 24 часа
            datetime.timedelta(hours=5),
            datetime.timedelta(days=4),
            datetime.timedelta(days=8),
            False
        ),
        (  # пересекает в конце на 4 часа
            datetime.timedelta(hours=5),
            datetime.timedelta(days=4, hours=20),
            datetime.timedelta(days=8),
            True
        ),
        (  # пересекает в середине на 4 часа
            datetime.timedelta(hours=5),
            datetime.timedelta(days=3, hours=23),
            datetime.timedelta(days=4, hours=3),
            True
        ),
        (  # пересекает в середине на 5 часа
            datetime.timedelta(hours=5),
            datetime.timedelta(days=3, hours=23),
            datetime.timedelta(days=4, hours=4),
            True
        ),
        (  # пересекает в середине на 10 часа
            datetime.timedelta(hours=5),
            datetime.timedelta(days=2),
            datetime.timedelta(days=2, hours=10),
            False
        ),
        (  # пересекает в середине на 2 дня
            datetime.timedelta(hours=5),
            datetime.timedelta(days=0),
            datetime.timedelta(days=2),
            False
        ),
        (  # лежит после смены
            datetime.timedelta(hours=5),
            datetime.timedelta(days=6),
            datetime.timedelta(days=8),
            True
        ),
        (  # лежит перед сменой
            datetime.timedelta(hours=5),
            -datetime.timedelta(days=8),
            -datetime.timedelta(days=6),
            True
        ),
        (  # пересекает в середине на 2 дня
            datetime.timedelta(days=5),
            datetime.timedelta(days=0),
            datetime.timedelta(days=2),
            True
        ),
        (  # пересекает в конце на 6 дней
            datetime.timedelta(days=5),
            -datetime.timedelta(days=1),
            datetime.timedelta(days=5),
            False
        ),
        (  # пересекает в конце на 5 дней
            datetime.timedelta(days=5),
            datetime.timedelta(days=0),
            datetime.timedelta(days=5),
            True
        ),
        (  # пересекает в середине на 10 часа
            datetime.timedelta(days=5),
            datetime.timedelta(days=2),
            datetime.timedelta(days=2, hours=10),
            True
        ),
        (  # пересекает в середине на 1 минуту
            datetime.timedelta(days=0),
            datetime.timedelta(days=2),
            datetime.timedelta(days=2, minutes=1),
            False
        ),
    )
)
def test_check_participant_can_duty_with_length_of_absences(
    staff_factory, gap_factory, shift_factory,
    length_of_absences, scope_session,
    gap_start, gap_end, can_duty
):
    print(length_of_absences, gap_start, gap_end, can_duty, 9999)
    staff = staff_factory()
    current_now = now()
    shift = shift_factory(
        start=current_now - datetime.timedelta(days=5),
        end=current_now + datetime.timedelta(days=5),
        staff=staff,
    )
    shift.schedule.length_of_absences = length_of_absences
    scope_session.commit()

    staff_gaps = [
        gap_factory(
            staff=staff,
            start=current_now + gap_start,
            end=current_now + gap_end,
        )
    ]

    assert check_participant_can_duty([shift], staff_gaps) is can_duty


def test_check_participant_can_duty(staff_factory, gap_factory, shift_factory):
    """ Проверка возможности дежурить в определенных сменах в зависимости от занятых дней """
    staff = staff_factory()

    with freeze_time('2018-01-01'):
        staff_gaps = [
            set_space_with_start_and_end(staff, 1, 3, gap_factory),
            set_space_with_start_and_end(staff, 8, 10, gap_factory),
            set_space_with_start_and_end(staff, 23, 40, gap_factory),
            set_space_with_start_and_end(staff, 60, 80, gap_factory),
        ]

        # не пересекает
        shift = set_space_with_start_and_end(staff, 5, 7, shift_factory)
        assert check_participant_can_duty([shift], staff_gaps)

        # не пересекает
        shift = set_space_with_start_and_end(staff, 45, 50, shift_factory)
        assert check_participant_can_duty([shift], staff_gaps)

        # пересекает начало занятого промежутка
        shift = set_space_with_start_and_end(staff, 6, 8, shift_factory)
        assert check_participant_can_duty([shift], staff_gaps)

        # пересекает конец занятого промежутка
        shift = set_space_with_start_and_end(staff, 3, 5, shift_factory)
        assert check_participant_can_duty([shift], staff_gaps)

        # пересекает конец и начало занятых промежутков
        shift = set_space_with_start_and_end(staff, 3, 8, shift_factory)
        assert check_participant_can_duty([shift], staff_gaps)

        # полностью перенадлежит одному из занятых времен
        shift = set_space_with_start_and_end(staff, 1, 3, shift_factory)
        assert not check_participant_can_duty([shift], staff_gaps)

        # полностью перекрывает один из занятых времен
        shift = set_space_with_start_and_end(staff, 9, 11, shift_factory)
        assert not check_participant_can_duty([shift], staff_gaps)

        # полностью перекрывает все из занятые времена
        shift = set_space_with_start_and_end(staff, 0, 1000, shift_factory)
        assert not check_participant_can_duty([shift], staff_gaps)


def test_query_schedules_without_allocation(scope_session, revision_factory, schedule_factory, shift_factory, staff_factory):
    s1 = schedule_factory()
    revision_factory(schedule=s1)
    s2 = schedule_factory(schedules_group=s1.schedules_group)
    revision_factory(schedule=s2)
    s3 = schedule_factory(schedules_group=s1.schedules_group)
    revision_factory(schedule=s3)

    shift_factory(schedule=s2, staff=staff_factory())
    shift_factory(schedule=s3, staff=None)

    schedules_without_allocation = [
        result[0] for result in query_schedules_without_allocation(
            scope_session, s1.schedules_group_id
        ).all()
    ]

    assert s1 not in schedules_without_allocation
    assert s2 not in schedules_without_allocation
    assert s3 in schedules_without_allocation


@pytest.mark.parametrize('has_gap', (True, False))
@pytest.mark.parametrize('has_problem', (True, False))
def test_nobody_on_duty(
    scope_session, staff_factory, gap_factory,
    shift_factory, slot_factory, has_gap, problem_factory,
    has_problem,
):
    staff = staff_factory()
    slot = slot_factory()
    slot.composition.participants.append(staff)
    scope_session.commit()
    shift = shift_factory(slot=slot, schedule=slot.interval.schedule)
    if has_gap:
        gap_factory(
            staff=staff,
            start=shift.start - datetime.timedelta(hours=3),
            end=shift.end + datetime.timedelta(hours=3)
        )
        shift.staff = staff
        scope_session.commit()
        if has_problem:
            problem_factory(
                shift=shift,
                reason=enums.ProblemReason.nobody_on_duty,
            )
    start_people_allocation(
        schedules_group_id=shift.schedule.schedules_group_id,
        start_date=shift.start,
    )
    scope_session.refresh(shift)
    problems = scope_session.query(Problem).filter(Problem.shift_id==shift.id)
    if has_gap:
        assert shift.staff_id is None
        assert problems.count() == 1
        assert problems.first().reason == enums.ProblemReason.nobody_on_duty
    else:
        assert shift.staff_id == staff.id
        assert problems.count() == 0


def test_find_staff_for_shift(staff_factory, gap_factory, shift_factory):
    """ Получение подходящего дежурного смены в зависимости от рейтинга и возможности дежурить """
    with freeze_time('2018-01-01'):
        st1 = staff_factory()
        st1_gaps = [
            set_space_with_start_and_end(st1, 8, 10, gap_factory),
        ]

        st2 = staff_factory()
        st2_gaps = [
            set_space_with_start_and_end(st1, 1, 3, gap_factory),
            set_space_with_start_and_end(st1, 6, 10, gap_factory),
        ]
        st3 = staff_factory()
        st3_gaps = [
            set_space_with_start_and_end(st1, 1, 10, gap_factory),
        ]
        shift = set_space_with_start_and_end(None, 5, 6, shift_factory)

    possible_participants = [st1, st2, st3]
    participants_gaps = {st1.id: st1_gaps, st2.id: st2_gaps, st3.id: st3_gaps}
    participant_ratings = {
        st1.login: 10,
        st2.login: 6,
        st3.login: 2,
        -1: -10,
    }

    participants_last_shift = defaultdict(
        lambda: defaultdict(
            lambda: defaultdict(
                lambda: datetime.datetime.min.replace(tzinfo=settings.DEFAULT_TIMEZONE)
            )
        )
    )
    assert find_staff_for_shift(
        shift=shift,
        shifts_sequence=[],
        possible_participants=possible_participants,
        participants_gaps=participants_gaps,
        participant_ratings=participant_ratings,
        already_found={},
        participant_last_shift_ends=participants_last_shift,
    ).id == st2.id


def test_no_duty_twice(
    staff_factory, gap_factory,
    shift_factory, scope_session
):
    """
    проверим что не назначаем человека на две смены
    подряд если есть такая возможность
    """
    st1 = staff_factory()
    st2 = staff_factory()

    shift = shift_factory()
    # тут другое расписание, поэтому не учитываем
    prev_shift = shift_factory(
        staff=st1,
        end=shift.start,
    )
    # тут никто не дежурит - просто пропустим
    prev_empty_shift = shift_factory(
        next_id=prev_shift.id,
        slot=shift.slot,
        end=shift.start,
        schedule=shift.schedule,
    )
    # и получается что в предыдущей смене дежурил st2
    prev_prev_shift = shift_factory(
        end=shift.start,
        next_id=prev_empty_shift.id,
        staff=st2,
        slot=shift.slot,
        schedule=shift.schedule,
    )
    ratings = {
        st1.login: 10,
        st2.login: 6,
    }

    participant_last_shift_ends = get_participants_last_shift_ends(
        session=scope_session,
        to_shift=shift,
        schedules_participants={shift.schedule.id: {shift.slot_id: [st1, st2]}},
        schedules_to_calculate={shift.schedule},
    )

    # у st2 минимальный рейтинг, но он дежурил предыдщую смену
    # поэтому дежурит другой
    assert find_staff_for_shift(
        shift=shift,
        shifts_sequence=[],
        possible_participants=[st1, st2],
        participants_gaps=defaultdict(list),
        participant_ratings=ratings,
        already_found={},
        participant_last_shift_ends=participant_last_shift_ends,
    ) == st1

    # но если других дежурных не нашлось - дежурит он
    assert find_staff_for_shift(
        shift=shift,
        shifts_sequence=[],
        possible_participants=[st2],
        participants_gaps=defaultdict(list),
        participant_ratings=ratings,
        already_found={},
        participant_last_shift_ends=participant_last_shift_ends,
    ) == st2

    #  а если он предыдущую не дежурил - то по рейтингу дежурить ему
    prev_prev_shift.staff = None
    scope_session.commit()
    participant_last_shift_ends = get_participants_last_shift_ends(
        session=scope_session,
        to_shift=shift,
        schedules_participants={shift.schedule.id: {shift.slot_id: [st1, st2]}},
        schedules_to_calculate={shift.schedule},
    )
    assert find_staff_for_shift(
        shift=shift,
        shifts_sequence=[],
        possible_participants=[st1, st2],
        participants_gaps=defaultdict(list),
        participant_ratings=ratings,
        already_found={},
        participant_last_shift_ends=participant_last_shift_ends,
    ) == st2


def test_errored_schedules_group_allocation(scope_session, schedule_data_with_composition, shift_factory):
    """
    Проверятся вызов функции перераспределения, если в группе сохранена ошибка
    """
    schedules_group = schedule_data_with_composition.schedule.schedules_group
    schedules_group.people_allocation_error = {'error': 1}
    last_date = datetime.datetime.combine(now() - datetime.timedelta(days=2), datetime.datetime.min.time())
    schedules_group.last_people_allocation_at = last_date
    shift_factory(schedule=schedule_data_with_composition.schedule)
    scope_session.commit()

    with patch('watcher.tasks.people_allocation.start_people_allocation.delay') as people_allocation_func:
        start_people_allocation_for_groups_with_allocation_error()
        people_allocation_func.assert_any_call(schedules_group_id=schedules_group.id, start_date=localize(last_date))


@pytest.mark.parametrize(('interval_count', 'shifts_count'), [(1, 2), (2, 4)])
def test_people_allocation(
    scope_session, schedule_data_with_composition, interval_count, shifts_count, composition_participants_factory
):
    """
    Перераспределения людей в группе расписаний.
    Все шифты могут быть заняты
    """
    schedule = schedule_data_with_composition.schedule

    if interval_count == 1:
        scope_session.query(Interval).filter(Interval.id == schedule_data_with_composition.interval_2.id).delete()

    elif interval_count == 2:
        slot_2 = schedule_data_with_composition.slot_2
        composition_2 = slot_2.composition
        for _ in range(2):
            composition_participants_factory(composition=composition_2)
        interval_2 = schedule_data_with_composition.interval_2
        interval_2.type_employment = enums.IntervalTypeEmployment.full

    scope_session.commit()

    with patch('watcher.tasks.people_allocation.start_people_allocation'):
        initial_creation_of_shifts(schedule.id)

    start_people_allocation(
        start_date=schedule_data_with_composition.revision.apply_datetime,
        schedules_group_id=schedule.schedules_group.id,
    )

    schedule_shifts = query_all_shifts_by_schedule(scope_session, schedule.id).all()
    assert all([shift.staff for shift in schedule_shifts])
    assert len({shift.staff for shift in schedule_shifts}) == shifts_count


def test_people_allocation_without_suitable_staff(scope_session, schedule_data_with_composition, composition_factory):
    """
    Перераспределения людей в группе расписаний.
    Два шифта не имеют возможных дежурных
    """
    schedule = schedule_data_with_composition.schedule
    new_composition = composition_factory()
    schedule_data_with_composition.slot_1.composition = new_composition
    scope_session.commit()

    initial_creation_of_shifts(schedule.id)

    schedule_shifts = query_all_shifts_by_schedule(scope_session, schedule.id)
    assert not all([shift.staff for shift in schedule_shifts])


def test_people_allocation_with_sub_shifts(
    scope_session, schedule_data_with_composition,
    shift_factory, rating_factory, gap_factory,
):
    """
    Одна смена имеет подсмену. Для нее не должно быть дежурного,
    а для её подсмен - должны быть дежурные (причем одинаковые), но
    только если подсмены не пустые
    """
    schedule = schedule_data_with_composition.schedule
    scope_session.query(Interval).filter(Interval.id == schedule_data_with_composition.interval_2.id).delete()
    scope_session.commit()

    with patch('watcher.tasks.people_allocation.start_people_allocation') as people_allocation_func:
        initial_creation_of_shifts(schedule.id)
        people_allocation_func.assert_called_once()

    shift, shift_wo_subshifts = query_all_shifts_by_schedule(scope_session, schedule.id).all()
    possible_participants = shift.slot.composition.participants
    # у трех дежурных минимальные рейтинги
    # но у одного из них геп на вторую часть смены
    # значит можем выбирать из двух оставшихся
    # должны назначить одного дежурного на обе половинки смены
    expected_duties = [possible_participants[1], possible_participants[2]]
    duty_with_gap = possible_participants[0]
    gap_factory(
        staff=duty_with_gap,
        start=shift.end-datetime.timedelta(hours=3),
        end=shift.end-datetime.timedelta(hours=1)
    )
    for staff in [
        staff for staff in possible_participants
        if staff not in [possible_participants[1], duty_with_gap, possible_participants[2]]
    ]:
        rating_factory(staff=staff, schedule=shift.schedule, rating=5.0)

    subshift1 = shift_factory(
        replacement_for=shift, start=shift.start,
        end=shift.end-datetime.timedelta(days=1),
        schedule=shift.schedule, slot=shift.slot
    )
    subshift_empty = shift_factory(
        replacement_for=shift, start=shift.end-datetime.timedelta(days=1),
        end=shift.end-datetime.timedelta(hours=5), schedule=shift.schedule,
        slot=shift.slot, empty=True,
    )
    subshift2 = shift_factory(
        replacement_for=shift, start=shift.end-datetime.timedelta(hours=5),
        end=shift.end, schedule=shift.schedule, slot=shift.slot,
    )

    start_people_allocation(
        schedules_group_id=schedule.schedules_group.id,
        start_date=schedule_data_with_composition.revision.apply_datetime,
    )

    scope_session.refresh(subshift1)
    scope_session.refresh(subshift2)
    scope_session.refresh(shift)
    scope_session.refresh(shift_wo_subshifts)
    scope_session.refresh(subshift_empty)

    assert shift.staff_id is None
    assert subshift_empty.staff_id is None
    assert shift_wo_subshifts.staff in shift_wo_subshifts.slot.composition.participants
    assert shift_wo_subshifts.staff_id != subshift2.staff_id
    assert subshift2.staff in shift.slot.composition.participants
    assert subshift1.staff in shift.slot.composition.participants
    assert subshift1.staff_id == subshift2.staff_id
    assert subshift1.staff in expected_duties


@pytest.mark.parametrize('skippable_property', ['empty', 'status_is_active', 'has_staff_approved', 'has_staff_ended'])
def test_people_allocation_with_skippable_shift(
    scope_session, schedule_data_with_composition, skippable_property,
    gap_factory, composition_participants_factory, staff_factory,
):
    """
    Одна смена имеет свойство, из-за которого для неё нельзя назначить нового дежурного
    """
    schedule = schedule_data_with_composition.schedule
    slot_1 = schedule_data_with_composition.slot_1
    slot_2 = schedule_data_with_composition.slot_2
    interval_2 = schedule_data_with_composition.interval_2
    interval_2.type_employment = enums.IntervalTypeEmployment.full
    scope_session.commit()

    create_gap = (
        lambda staff: [set_space_with_start_and_end(staff, 1, 5, gap_factory)],
        lambda staff: [set_space_with_start_and_end(staff, 7, 8, gap_factory)],
    )

    slot_1_participants = query_participants_by_composition(
        db=scope_session,
        composition_id=slot_1.composition.id
    ).all()

    for i, cp in enumerate(slot_1_participants):
        create_gap[i % 2](cp.staff)

    slot_2_participants = []
    for i in range(2):
        cp = composition_participants_factory(composition=slot_2.composition)
        create_gap[i](cp.staff)
        slot_2_participants.append(cp.staff)

    with patch('watcher.tasks.people_allocation.start_people_allocation') as people_allocation_func:
        initial_creation_of_shifts(schedule.id)
        people_allocation_func.assert_called_once()

    shift = query_all_shifts_by_schedule(scope_session, schedule.id).all()[0]
    probably_watcher = staff_factory()

    if skippable_property == 'empty':
        shift.empty = True
    elif skippable_property == 'status_is_active':
        shift.status = enums.ShiftStatus.active
    elif skippable_property == 'has_staff_approved':
        shift.staff = probably_watcher
        shift.approved = True
    elif skippable_property == 'has_staff_ended':
        shift.staff = probably_watcher
        shift.start = now() - datetime.timedelta(days=2)
        shift.end = now() - datetime.timedelta(days=1)
    scope_session.commit()

    start_people_allocation(
        schedules_group_id=schedule.schedules_group.id,
        start_date=schedule_data_with_composition.revision.apply_datetime,
    )

    schedule_shifts = query_all_shifts_by_schedule(scope_session, schedule.id).all()
    shifts_watchers = {obj.id: obj.staff for obj in schedule_shifts}

    if skippable_property.startswith('has_staff'):
        assert shifts_watchers.pop(shift.id) == probably_watcher
    else:
        assert not shifts_watchers.pop(shift.id)
    assert all(shifts_watchers.values())


def test_people_allocation_with_past_schedule(
    scope_session, schedule_data_with_composition, composition_participants_factory
):
    """
    Создаем расписание в прошлом.
    Для шифтов, которые начались в прошлом и пересекают настоящее должны найтись дежурные
    """
    schedule = schedule_data_with_composition.schedule
    schedule_data_with_composition.revision.apply_datetime = now() - datetime.timedelta(days=10)
    threshold_date = (now() - datetime.timedelta(days=3)).date()
    slot_2 = schedule_data_with_composition.slot_2
    composition_2 = slot_2.composition
    composition_participants_factory(composition=composition_2)
    interval_2 = schedule_data_with_composition.interval_2
    interval_2.type_employment = enums.IntervalTypeEmployment.full
    scope_session.commit()

    with patch('watcher.tasks.people_allocation.start_people_allocation') as people_allocation_func:
        initial_creation_of_shifts(schedule.id)
        people_allocation_func.assert_called_once()
        assert people_allocation_func.call_args[1]['start_date'].date() == threshold_date

    start_people_allocation(
        schedules_group_id=schedule.schedules_group_id,
        start_date=threshold_date,
    )

    schedule_shifts = query_all_shifts_by_schedule(scope_session, schedule.id).all()
    assert all([
        s.staff is not None
        for s in schedule_shifts
        if localize(s.start) <= now() <= localize(s.end)
    ])


def test_get_participant_ratings(
    scope_session, schedule_data_with_composition, schedule_factory, staff_factory, composition_participants_factory,
    interval_factory, slot_factory, rating_factory, shift_factory,
):
    """ Определение первичного рейтинга дежурных """
    schedule = schedule_data_with_composition.schedule
    slot_1 = schedule_data_with_composition.slot_1
    slot_2 = schedule_data_with_composition.slot_2
    composition_2 = slot_2.composition
    composition_participants_factory(composition=composition_2)
    interval_2 = schedule_data_with_composition.interval_2
    interval_2.type_employment = enums.IntervalTypeEmployment.full
    scope_session.commit()

    with patch('watcher.tasks.people_allocation.start_people_allocation'):
        initial_creation_of_shifts(schedule.id)

    shifts = query_all_shifts_by_schedule(scope_session, schedule.id).all()
    intervals = query_intervals_by_current_revision(scope_session, schedule.id).all()

    participants = query_participants_by_schedules_group(scope_session, schedule.schedules_group_id).all()
    shifts_sequence = sequence_shifts(
        session=scope_session,
        schedule=schedule,
        shifts=shifts,
        intervals=intervals,
        for_group=True,
    )
    # добавим шифт, который еще не завершился - его рейтинг тоже должны учесть,
    # но только рейтинг замены - если у смены есть замены, основную смену не должны учитывать
    main_shift = shift_factory(
        schedule=schedule,
        slot=shifts_sequence[0].slot,
        next=shifts_sequence[0],
        status=enums.ShiftStatus.active,
        staff=participants[0],
        start=shifts_sequence[0].start - datetime.timedelta(hours=15),
        end=shifts_sequence[0].start - datetime.timedelta(hours=5),
    )

    shift_factory(
        schedule=schedule,
        replacement_for=main_shift,
        slot=shifts_sequence[0].slot,
        next=shifts_sequence[0],
        status=enums.ShiftStatus.active,
        staff=participants[0],
        start=shifts_sequence[0].start - datetime.timedelta(hours=10),
        end=shifts_sequence[0].start - datetime.timedelta(hours=5),
    )

    for staff in participants:
        rating_factory(schedule=schedule, staff=staff, rating=5.0)

    # ещё один график, но пока рассчитан с пустым составом
    lonely_schedule = schedule_factory(schedules_group=schedule.schedules_group)
    interval_3 = interval_factory(schedule=lonely_schedule, duration=datetime.timedelta(days=5), )
    slot_3 = slot_factory(interval=interval_3)
    with patch('watcher.tasks.people_allocation.start_people_allocation'):
        initial_creation_of_shifts(lonely_schedule.id)

    # в первый и третий состав добавим по 1 новому участнику
    left_dude = staff_factory()
    new_participants_slot_1 = staff_factory()
    new_participants_slot_3 = staff_factory()
    composition_participants_factory(composition=slot_1.composition, staff=new_participants_slot_1)
    composition_participants_factory(composition=slot_3.composition, staff=new_participants_slot_3)

    slot_1_participants = query_participants_by_composition(
        db=scope_session,
        composition_id=slot_1.composition.id
    ).all()
    assert len(slot_1_participants) == 11

    slot_3_participants = query_participants_by_composition(
        db=scope_session,
        composition_id=slot_3.composition.id
    ).all()
    assert len(slot_3_participants) == 1

    _, _, _, schedules_participants, _, _, _ = get_people_allocation_data(
        session=scope_session,
        schedules_group_id=schedule.schedules_group_id,
        start_date=schedule_data_with_composition.revision.apply_datetime,
    )

    participant_ratings = get_participants_ratings_from_sequence(
        db=scope_session,
        schedules_participants=schedules_participants,
        shifts_sequence=shifts_sequence,
        schedules_to_calculate={schedule, lonely_schedule}
    )

    # в данных будут new_participants_slot_1 и new_participants_slot_3
    # но не будет left_dude
    rating_schedule = {staff.login: 5 for staff in participants} | {new_participants_slot_1.login: 0} | {participants[0].login: 10}
    assert left_dude.login not in rating_schedule
    assert participant_ratings == {
        schedule.id: rating_schedule,
        lonely_schedule.id: dict.fromkeys([p.staff.login for p in slot_3_participants], 0),
    }


@pytest.mark.parametrize('start_date_format', ['%Y-%m-%dT%H:%M:%S.%f', '%Y-%m-%dT%H:%M:%S', '%Y-%m-%d %H:%M:%S.%f'])
def test_people_allocation_with_str_startdate(schedule_data_with_composition, scope_session, start_date_format):
    """ Перераспределения людей в группе расписаний. Все шифты могут быть заняты """
    schedule = schedule_data_with_composition.schedule
    schedule.threshold_day = datetime.timedelta(days=20)
    old_interval = schedule_data_with_composition.interval_1
    old_revision = schedule_data_with_composition.revision
    # оставим только 1 интервал
    scope_session.query(Interval).filter(
        Interval.id != old_interval.id,
        Interval.revision == old_revision,
    ).delete()
    scope_session.commit()

    with patch('watcher.tasks.people_allocation.start_people_allocation'):
        initial_creation_of_shifts(schedule.id)

    start_people_allocation(
        schedules_group_id=schedule.schedules_group.id,
        start_date=datetime.datetime.strftime(
            schedule_data_with_composition.revision.apply_datetime,
            start_date_format
        )
    )

    schedule_shifts = query_all_shifts_by_schedule(scope_session, schedule.id).all()
    assert all([shift.staff for shift in schedule_shifts])
    assert len({shift.staff for shift in schedule_shifts}) == 4


def test_people_allocation_with_two_revisions(
    scope_session, schedule_data_with_composition, staff_factory, composition_factory, composition_participants_factory,
    revision_factory, interval_factory, slot_factory
):
    """
    Перераспределения людей. У графика меняется ревизия и состав
    """
    schedule = schedule_data_with_composition.schedule
    old_interval = schedule_data_with_composition.interval_1
    old_slot = schedule_data_with_composition.slot_1
    old_revision = schedule_data_with_composition.revision
    # оставим только 1 интервал
    scope_session.query(Interval).filter(
        Interval.id != old_interval.id,
        Interval.revision == old_revision,
    ).delete()
    scope_session.commit()
    initial_creation_of_shifts(schedule_id=schedule.id)

    new_composition = composition_factory(service=schedule.service)
    new_staff = staff_factory()
    composition_participants_factory(composition=new_composition, staff=new_staff)

    # изначально new_staff нет в шифтах
    old_shifts = query_all_shifts_by_schedule(scope_session, schedule.id).all()
    assert len(old_shifts) > 0
    assert new_staff not in [shift.staff for shift in old_shifts]

    # в новой ревизии у слота должен изменится только состав
    new_revision = revision_factory(
        schedule=schedule,
        state=enums.RevisionState.active,
        prev=old_revision,
        apply_datetime=schedule_data_with_composition.revision.apply_datetime + datetime.timedelta(days=5),
    )
    new_interval = interval_factory(
        schedule=schedule,
        duration=old_interval.duration,
        type_employment=old_interval.type_employment,
        order=old_interval.order,
        revision=new_revision,
    )
    new_slot = slot_factory(
        interval=new_interval,
        role_on_duty=old_slot.role_on_duty,
        composition=new_composition,
    )
    old_shifts[1].slot_id = new_slot.id
    old_shifts[1].slot = new_slot
    for shift in old_shifts:
        shift.status == enums.ShiftStatus.scheduled
    scope_session.commit()

    with freeze_time(schedule_data_with_composition.revision.apply_datetime - datetime.timedelta(days=1)):
        # не одна смена еще не начата для нас
        start_people_allocation(schedules_group_id=schedule.schedules_group_id)

    new_shifts = query_all_shifts_by_schedule(scope_session, schedule.id).all()
    assert new_shifts[0].staff != new_staff
    assert new_shifts[1].staff == new_staff


@pytest.mark.parametrize('empty_interval', [True, False])
def test_people_allocation_empty_interval(schedule_factory, interval_factory, staff_factory, shift_factory,
                                          slot_factory, composition_participants_factory, empty_interval):
    schedule = schedule_factory()
    mock_return_value = None
    if not empty_interval:
        slot = slot_factory(interval=interval_factory(schedule=schedule))
        composition_participants_factory(composition=slot.composition)
        shift = shift_factory(schedule=schedule, slot=slot, staff=staff_factory())
        mock_return_value = shift.start

    with patch(
        'watcher.tasks.people_allocation.prepare_people_allocation_start_time',
        return_value=mock_return_value
    ) as prepare_people_mock:
        start_people_allocation(schedules_group_id=schedule.schedules_group_id)

    if empty_interval:
        prepare_people_mock.assert_not_called()
    else:
        prepare_people_mock.assert_called_once()


def test_rating_allocation(
    scope_session, staff_factory, schedule_factory, revision_factory,
    interval_factory, slot_factory, rating_factory, shift_factory,
    composition_factory, composition_participants_factory,
):
    """
    проверяем правильность распределения людей по шифтам
    в зависмости от даты последнего шифта
    """
    start_time = now() - datetime.timedelta(days=2)

    staff_1 = staff_factory()
    staff_2 = staff_factory()
    staff_3 = staff_factory()

    schedule = schedule_factory(threshold_day=datetime.timedelta(days=9))
    revision = revision_factory(schedule=schedule, state=enums.RevisionState.active)
    interval = interval_factory(schedule=schedule, duration=datetime.timedelta(days=1), revision=revision)
    slot = slot_factory(interval=interval)

    rating_factory(staff=staff_3, schedule=schedule, rating=0.0)
    rating_factory(staff=staff_1, schedule=schedule, rating=24.0)
    rating_factory(staff=staff_2, schedule=schedule, rating=24.0)

    shifts = []
    prev = None
    for i in range(9):
        shift = shift_factory(
            predicted_ratings={
                staff_1.login: 48.0,
                staff_2.login: 48.0,
            },
            schedule=schedule,
            slot=slot,
            start=start_time + datetime.timedelta(days=i),
            end=start_time + datetime.timedelta(days=i + 1),
            prev=prev
        )
        shifts.append(shift)
        prev = shift

    shifts[0].staff = staff_1
    # смены в прошлом, включенные в последовательность считаем законченными
    shifts[0].status = enums.ShiftStatus.scheduled
    shifts[1].staff = staff_2
    shifts[1].status = enums.ShiftStatus.completed

    composition = composition_factory()
    slot.composition = composition

    composition_participants_factory(composition=composition, staff=staff_1)
    composition_participants_factory(composition=composition, staff=staff_2)
    composition_participants_factory(composition=composition, staff=staff_3)

    scope_session.commit()

    start_people_allocation(schedules_group_id=schedule.schedules_group_id)
    shifts = query_all_shifts_by_schedule(scope_session, schedule.id).all()

    staff_order = [staff_1.id, staff_2.id, staff_3.id]
    assert [shift.staff_id for shift in shifts[:3]] == staff_order
    assert [shift.staff_id for shift in shifts[3:6]] == staff_order
    assert [shift.staff_id for shift in shifts[6:9]] == staff_order


def test_interval_with_primary_rotation(
    scope_session, schedule_factory, revision_factory,
    interval_factory, slot_factory, composition_factory,
    composition_participants_factory, assert_count_queries
):
    """
    проверяем правильное распределение людей при галочке текущий бэкап - это следующий праймари
    """
    schedule = schedule_factory()
    revision = revision_factory(schedule=schedule, state=enums.RevisionState.active)
    interval = interval_factory(schedule=schedule, duration=datetime.timedelta(days=1),
                                revision=revision, primary_rotation=True)
    composition = composition_factory()
    for _ in range(10):
        composition_participants_factory(composition=composition)
    slot_factory(interval=interval, composition=composition)
    slot_factory(interval=interval, is_primary=False, composition=composition)
    with patch('watcher.tasks.people_allocation.start_people_allocation'):
        initial_creation_of_shifts(schedule.id)
    with assert_count_queries(16):
        # Галочка текущий бэкап - следующий праймари не добавляет дополнительных запросов
        start_people_allocation(
            start_date=revision.apply_datetime,
            schedules_group_id=schedule.schedules_group.id,
        )
    shifts = scope_session.query(Shift).order_by(Shift.start).all()
    first_backup = shifts[1] if shifts[0].slot.is_primary else shifts[0]
    prev_backup_staff = first_backup.staff
    for i in range(2, len(shifts)):
        if shifts[i].slot.is_primary:
            assert shifts[i].staff == prev_backup_staff
        else:
            prev_backup_staff = shifts[i].staff


def test_interval_with_primary_rotation_with_another_intervals(
    scope_session, schedule_factory, revision_factory,
    interval_factory, slot_factory, composition_factory,
    composition_participants_factory, schedules_group_factory
):
    """
    проверяем правильное распределение людей при галочке текущий бэкап - это следующий праймари,
    когда в группе есть другие расписания и в расписании с галочкой есть другие интервалы
    """
    group = schedules_group_factory()
    schedule1 = schedule_factory(schedules_group=group)
    schedule2 = schedule_factory(schedules_group=group)
    revision1 = revision_factory(schedule=schedule1, state=enums.RevisionState.active)
    revision2 = revision_factory(schedule=schedule2, state=enums.RevisionState.active)
    interval1_1 = interval_factory(schedule=schedule1, duration=datetime.timedelta(days=5),
                                   revision=revision1, primary_rotation=True)
    interval1_2 = interval_factory(schedule=schedule1, duration=datetime.timedelta(days=2), revision=revision1)
    interval2_1 = interval_factory(schedule=schedule2, duration=datetime.timedelta(days=1), revision=revision2)
    composition = composition_factory()
    for _ in range(10):
        composition_participants_factory(composition=composition)
    slot_primary = slot_factory(interval=interval1_1, composition=composition)
    slot_backup = slot_factory(interval=interval1_1, is_primary=False, composition=composition)
    slot1_2 = slot_factory(interval=interval1_2, composition=composition)
    slot_factory(interval=interval2_1, composition=composition)
    with patch('watcher.tasks.people_allocation.start_people_allocation'):
        initial_creation_of_shifts(schedule1.id)
        initial_creation_of_shifts(schedule2.id)
    start_people_allocation(schedules_group_id=group.id)
    all_shifts = (scope_session.query(Shift)
                  .order_by(Shift.start, Shift.slot_id)
                  .filter(Shift.slot_id.in_((slot_backup.id, slot_primary.id, slot1_2.id)))
                  .all())
    shifts = [shift for i, shift in enumerate(all_shifts, start=1) if i % 3 != 0]

    current_predicted_ratings = {participant.login: 0.0 for participant in composition.participants}
    for shift in all_shifts:
        assert shift.predicted_ratings == current_predicted_ratings
        current_predicted_ratings[shift.staff.login] += (shift.slot.interval.duration.total_seconds() / 3600)
    first_backup = shifts[1] if shifts[0].slot.is_primary else shifts[0]
    prev_backup_staff = first_backup.staff
    for i in range(2, len(shifts)):
        if shifts[i].slot.is_primary:
            assert shifts[i].staff == prev_backup_staff
        else:
            prev_backup_staff = shifts[i].staff


def test_interval_primary_rotation_participants_gaps(
    scope_session, schedule_factory, revision_factory,
    interval_factory, slot_factory, composition_factory,
    composition_participants_factory, gap_factory
):
    """
    проверяем правильное распределение людей при галочке текущий бэкап - это следующий праймари
    когда есть отсутствия у участников композиции
    """
    schedule = schedule_factory(threshold_day=datetime.timedelta(days=15))
    revision = revision_factory(schedule=schedule, state=enums.RevisionState.active)
    interval = interval_factory(schedule=schedule, duration=datetime.timedelta(days=2),
                                revision=revision, primary_rotation=True)
    composition = composition_factory()
    participant = composition_participants_factory(composition=composition)
    now_time = now()
    gap_factory(
        start=now_time+datetime.timedelta(days=1),
        end=now_time+datetime.timedelta(days=7),
        staff=participant.staff
    )
    for _ in range(2):
        composition_participants_factory(composition=composition)
    slot_factory(interval=interval, composition=composition)
    slot_factory(interval=interval, is_primary=False, composition=composition)
    initial_creation_of_shifts(schedule.id)
    shifts = scope_session.query(Shift).order_by(Shift.start).all()
    first_backup = shifts[1] if shifts[0].slot.is_primary else shifts[0]
    prev_backup_staff = first_backup.staff
    for i in range(2, len(shifts)):
        if shifts[i].slot.is_primary:
            assert shifts[i].staff == prev_backup_staff
        else:
            prev_backup_staff = shifts[i].staff
    for i in range(1, len(shifts), 2):
        # смены идут парами основная-запасная, проверим что на одну пару назначили разных дежурных
        assert shifts[i].staff_id != shifts[i-1].staff_id


@freeze_time('2022-04-06')  # среда
def test_interval_primary_rotation_with_subshifts(
    scope_session, schedule_factory, revision_factory,
    interval_factory, slot_factory, composition_factory,
    composition_participants_factory, gap_factory
):
    """
    проверяем правильное распределение людей при галочке текущий бэкап - это следующий праймари
    при выкалывании смен на выходные
    """
    schedule = schedule_factory(threshold_day=datetime.timedelta(days=7))
    revision = revision_factory(schedule=schedule, state=enums.RevisionState.active, apply_datetime=now())
    interval = interval_factory(
        schedule=schedule, duration=datetime.timedelta(days=2),
        revision=revision, primary_rotation=True,
        weekend_behaviour=enums.IntervalWeekendsBehaviour.extend
    )
    composition = composition_factory()
    for _ in range(3):
        composition_participants_factory(composition=composition)
    slot_factory(interval=interval, composition=composition)
    slot_factory(interval=interval, is_primary=False, composition=composition)
    initial_creation_of_shifts(schedule.id)
    shifts = scope_session.query(Shift).filter(~Shift.staff_id.is_(None)).order_by(Shift.start, Shift.is_primary).all()
    assert len(shifts) == 8
    # shifts[0] - бэкап смена со среды до пятницы,
    # shifts[3] - смена с пятницы до субботы, shifts[5] - продолжение с понедельника до вторника. Обе праймари
    assert shifts[3].is_primary and shifts[5].is_primary
    assert shifts[3].staff_id == shifts[5].staff_id == shifts[0].staff_id
    # shifts[2], shifts[4] - такие же, только бэкап
    assert not shifts[2].is_primary and not shifts[4].is_primary
    assert shifts[2].staff_id == shifts[4].staff_id
    # shifts[7] - праймари смена с понедельника по среду
    assert shifts[7].is_primary
    assert shifts[7].staff_id == shifts[4].staff_id


def test_check_participant_can_duty_before_and_after_vacation(
    staff_factory, gap_factory, shift_factory,
    schedule_factory, scope_session,
):
    """ Проверка возможности дежурить в определенных сменах с учетом дней без дежурств до и после отпуска """
    staff = staff_factory()
    with freeze_time('2018-01-01'):
        schedule = schedule_factory(
            days_before_vacation=datetime.timedelta(days=3),
            days_after_vacation=datetime.timedelta(days=5),
            length_of_absences=datetime.timedelta(days=0),
        )
        gap_vacation = [set_space_with_start_and_end(staff, 10, 13, gap_factory, type=enums.GapType.vacation)]
        gap = [set_space_with_start_and_end(staff, 10, 13, gap_factory)]

        # не пересекает
        shift = set_space_with_start_and_end(staff, 2, 7, shift_factory, schedule=schedule)
        assert check_participant_can_duty([shift], gap_vacation)
        assert check_participant_can_duty([shift], gap)

        # не пересекает
        shift = set_space_with_start_and_end(staff, 18, 21, shift_factory, schedule=schedule)
        assert check_participant_can_duty([shift], gap_vacation)
        assert check_participant_can_duty([shift], gap)

        # пересекает сверху в дни после отпуска
        shift = set_space_with_start_and_end(staff, 17, 21, shift_factory, schedule=schedule)
        assert not check_participant_can_duty([shift], gap_vacation)
        assert check_participant_can_duty([shift], gap)

        # пересекает снизу и сверху
        shift = set_space_with_start_and_end(staff, 7, 18, shift_factory)
        assert not check_participant_can_duty([shift], gap_vacation)
        assert not check_participant_can_duty([shift], gap)

        # пересекает снизу в дни перед отпуском
        shift = set_space_with_start_and_end(staff, 2, 8, shift_factory, schedule=schedule)
        assert not check_participant_can_duty([shift], gap_vacation)
        assert check_participant_can_duty([shift], gap)

        # пересекает снизу в дни перед отпуском, но длина возможного отсутствия увеличена
        schedule.length_of_absences = datetime.timedelta(days=1)
        scope_session.commit()
        shift = set_space_with_start_and_end(staff, 2, 8, shift_factory, schedule=schedule)
        assert check_participant_can_duty([shift], gap_vacation)
        assert check_participant_can_duty([shift], gap)


def test_people_allocation_parallel_slots(
    scope_session, schedule_factory, revision_factory, interval_factory, slot_factory, composition_factory,
    composition_participants_factory
):
    schedule = schedule_factory(threshold_day=datetime.timedelta(days=5))
    revision = revision_factory(schedule=schedule, state=enums.RevisionState.active)

    interval = interval_factory(schedule=schedule, duration=datetime.timedelta(days=1), revision=revision)
    composition = composition_factory(service=schedule.service)

    # 15 участников на 10 параллельных слотов
    for _ in range(10):
        slot_factory(interval=interval, composition=composition)
    for _ in range(15):
        composition_participants_factory(composition=composition)

    with patch('watcher.tasks.people_allocation.start_people_allocation'):
        initial_creation_of_shifts(schedule.id)

    start_people_allocation(
        start_date=revision.apply_datetime,
        schedules_group_id=schedule.schedules_group.id,
    )

    schedule_shifts_initial = query_all_shifts_by_schedule(scope_session, schedule.id).all()
    staff_distribution_initial = Counter([f.staff_id for f in schedule_shifts_initial]).values()
    # проверка равномерного распределения людей
    assert (max(staff_distribution_initial) - min(staff_distribution_initial)) <= 1
    # подтверждаем половину смен
    for i, shift in enumerate(schedule_shifts_initial):
        if i % 2 == 0:
            shift.approved = True
    scope_session.commit()

    start_people_allocation(
        start_date=revision.apply_datetime,
        schedules_group_id=schedule.schedules_group.id,
    )

    schedule_shifts_reallocated = query_all_shifts_by_schedule(scope_session, schedule.id).all()
    date_staff_counter = Counter([(f.start.date(), f.staff_id) for f in schedule_shifts_reallocated])
    assert all(count == 1 for count in date_staff_counter.values())

    staff_distribution_reallocated = Counter([f.staff_id for f in schedule_shifts_reallocated]).values()
    # проверка равномерного распределения людей
    assert (max(staff_distribution_reallocated) - min(staff_distribution_reallocated)) <= 1


def test_people_allocation_empty_intervals(schedule_data, scope_session, composition_participants_factory,
                                           staff_factory, rating_factory):
    schedule = schedule_data.schedule
    schedule.threshold_day = datetime.timedelta(days=60)
    scope_session.commit()

    slot = schedule_data.slot_1
    composition = slot.composition
    staff_list = []
    for _ in range(3):
        staff = staff_factory()
        staff_list.append(staff)
        composition_participants_factory(composition=composition, staff=staff)

    rating_factory(staff=staff_list[0], schedule=schedule, rating=10000.0)
    rating_factory(staff=staff_list[1], schedule=schedule, rating=10000.0)

    with patch('watcher.tasks.people_allocation.start_people_allocation'):
        initial_creation_of_shifts(schedule.id)

    start_people_allocation(
        schedules_group_id=schedule.schedules_group.id,
    )

    shifts = query_schedule_main_shifts(scope_session, schedule.id).all()

    current_staff_id = None
    for shift in shifts:
        if shift.empty:
            continue
        # Проверяем что не назначаем подряд человека с минимальным рейтингом
        assert shift.staff_id != current_staff_id
        current_staff_id = shift.staff_id


def test_people_allocation_empty_intervals_two_slots(
    scope_session, schedule_factory, revision_factory, interval_factory, slot_factory, composition_factory,
    composition_participants_factory, staff_factory, rating_factory,
):
    schedule = schedule_factory(threshold_day=datetime.timedelta(days=60))
    revision = revision_factory(
        schedule=schedule, state=enums.RevisionState.active,
        apply_datetime=now()+datetime.timedelta(days=0),
    )
    interval = interval_factory(
        schedule=schedule, revision=revision,
        duration=datetime.timedelta(days=5)
    )
    empty_interval = interval_factory(
        schedule=schedule, revision=revision,
        duration=datetime.timedelta(days=2),
        type_employment=enums.IntervalTypeEmployment.empty,
    )

    composition = composition_factory()
    slot_factory(interval=interval, composition=composition)
    slot_factory(interval=interval, composition=composition)
    slot_factory(interval=empty_interval)

    staff_list = []
    for _ in range(4):
        staff = staff_factory()
        staff_list.append(staff)
        composition_participants_factory(composition=composition, staff=staff)

    rating_factory(staff=staff_list[0], schedule=schedule, rating=10000.0)
    rating_factory(staff=staff_list[1], schedule=schedule, rating=10000.0)

    initial_creation_of_shifts(schedule.id)
    shifts = query_schedule_main_shifts(scope_session, schedule.id).order_by(Shift.start).all()

    # проверяем что человек не дежурит до гапа, и сразу после него
    prev_staff_ids = {}
    curr_staff_ids = {}
    for shift in shifts:
        if not shift.empty:
            assert shift.staff_id not in prev_staff_ids.values()
            curr_staff_ids[shift.slot_id] = shift.staff_id
        else:
            prev_staff_ids, curr_staff_ids = curr_staff_ids, prev_staff_ids


@pytest.mark.parametrize('timeout_days', (0, 7, 21))
def test_people_allocation_timeout_between_shifts(
    scope_session, slot_factory, composition_participants_factory, staff_factory, rating_factory,
    schedules_group_factory, schedule_factory, interval_factory, timeout_days
):
    schedules_group = schedules_group_factory(timeout_between_shifts=datetime.timedelta(days=timeout_days))
    slots = [slot_factory(
        interval=interval_factory(
            schedule=schedule_factory(
                schedules_group=schedules_group
            )
        )
    ) for _ in range(2)]
    staff = [staff_factory() for _ in range(3)]

    staff_ratings = {staff[0].id: 0.0, staff[1].id: 1000.0, staff[2].id: 1000.0}
    for slot, staff_obj in itertools.product(slots, staff):
        composition_participants_factory(
            composition=slot.composition,
            staff=staff_obj
        )
        rating_factory(
            staff=staff_obj,
            schedule=slot.interval.schedule,
            rating=staff_ratings[staff_obj.id]
        )

    initial_creation_of_shifts(slots[0].interval.schedule_id)
    initial_creation_of_shifts(slots[1].interval.schedule_id)

    shifts = scope_session.query(Shift).filter(
        Shift.schedule_id.in_([slots[0].interval.schedule_id, slots[1].interval.schedule_id]),
        Shift.replacement_for_id.is_(None)
    ).order_by(Shift.start).all()

    prev_shift_ends = {
        obj.id: localize(datetime.datetime(year=1970, month=1, day=1))
        for obj in staff
    }
    for shift in shifts:
        if shift.staff is None:
            continue
        gap_length = shift.start - prev_shift_ends[shift.staff_id]
        prev_shift_ends[shift.staff_id] = shift.end
        assert gap_length.total_seconds() >= timeout_days * 3600 * 24


@pytest.mark.parametrize('rotation_type',
                         (enums.IntervalRotation.backup_is_next_primary,
                          enums.IntervalRotation.primary_is_next_backup,
                          enums.IntervalRotation.cross_rotation))
def test_interval_rotation_backup_and_primary(
    scope_session, schedule_factory, interval_factory, slot_factory, composition_factory,
    composition_participants_factory, assert_count_queries, rotation_type
):
    schedule = schedule_factory(rotation=rotation_type)
    interval = interval_factory(schedule=schedule, duration=datetime.timedelta(days=1))
    composition = composition_factory()
    slot_factory(interval=interval, composition=composition, is_primary=True)
    slot_factory(interval=interval, composition=composition, is_primary=False)
    [composition_participants_factory(composition=composition) for _ in range(5)]

    with patch('watcher.tasks.people_allocation.start_people_allocation'):
        initial_creation_of_shifts(schedule.id)

    kwargs = {
        'start_date': interval.revision.apply_datetime,
        'schedules_group_id': schedule.schedules_group.id,
    }
    with assert_count_queries(14):
        start_people_allocation(**kwargs)

    shifts = scope_session.query(Shift).order_by(Shift.start).all()
    check_rotation_invariant(shifts=shifts, rotation=rotation_type)


def test_timeout_between_shifts_with_interval_rotation(
    scope_session, slot_factory, composition_factory, composition_participants_factory,
    schedules_group_factory, schedule_factory, interval_factory
):
    schedules_group = schedules_group_factory(timeout_between_shifts=datetime.timedelta(days=7))
    interval = interval_factory(
        schedule=schedule_factory(
            schedules_group=schedules_group,
            rotation=enums.IntervalRotation.backup_is_next_primary,
        )
    )
    composition = composition_factory()
    slots = [
        slot_factory(interval=interval, composition=composition, is_primary=False),
        slot_factory(interval=interval, composition=composition, is_primary=True),
    ]
    [composition_participants_factory(composition=composition) for _ in range(2)]

    initial_creation_of_shifts(interval.schedule_id)

    shifts = scope_session.query(Shift).filter(
        Shift.schedule_id.in_([slots[0].interval.schedule_id, slots[1].interval.schedule_id]),
        Shift.replacement_for_id.is_(None)
    ).order_by(Shift.start).all()
    check_rotation_invariant(shifts=shifts, rotation=enums.IntervalRotation.backup_is_next_primary)


@pytest.mark.parametrize('rotation_type',
                         (enums.IntervalRotation.backup_is_next_primary,
                          enums.IntervalRotation.primary_is_next_backup))
def test_check_rotation_takes_into_account_approved_shifts(
    scope_session, schedule_factory, interval_factory, slot_factory, composition_factory,
    composition_participants_factory, assert_count_queries, rotation_type
):
    schedule = schedule_factory(rotation=rotation_type)
    interval = interval_factory(schedule=schedule, duration=datetime.timedelta(days=14))
    composition = composition_factory()
    slot_factory(interval=interval, composition=composition, is_primary=True)
    slot_factory(interval=interval, composition=composition, is_primary=False)
    participants = [composition_participants_factory(composition=composition) for _ in range(5)]

    with patch('watcher.tasks.people_allocation.start_people_allocation'):
        initial_creation_of_shifts(schedule.id)

    shifts = scope_session.query(Shift).order_by(Shift.start, Shift.is_primary).all()

    assert shifts[0].is_primary is False
    shifts[0].approved = True
    shifts[0].staff = participants[1]

    assert shifts[1].is_primary is True
    shifts[1].approved = True
    shifts[1].staff = participants[2]

    assert shifts[6].is_primary is False
    shifts[6].approved = True
    shifts[6].staff = participants[1]

    assert shifts[7].is_primary is True
    shifts[7].approved = True
    shifts[7].staff = participants[0]

    kwargs = {
        'start_date': interval.revision.apply_datetime,
        'schedules_group_id': schedule.schedules_group.id,
    }
    with assert_count_queries(14):
        start_people_allocation(**kwargs)

    scope_session.expire_all()
    check_rotation_invariant(shifts=shifts, rotation=rotation_type)


def test_change_composition_check_ratings(scope_session, shift_sequence_data, staff_factory, member_factory,
                                          composition_factory, composition_participants_factory, slot_factory,
                                          rating_factory):
    schedule = shift_sequence_data.schedule
    staff_1 = shift_sequence_data.staff_1
    staff_2 = shift_sequence_data.staff_2
    staff_3 = staff_factory()
    member_factory(staff=staff_3, service=schedule.service)
    rating_factory(staff=staff_3, schedule=schedule, rating=0.0)

    composition_2 = composition_factory(service=schedule.service)
    composition_participants_factory(staff=staff_2, composition=composition_2)
    composition_participants_factory(staff=staff_3, composition=composition_2)
    shift_sequence_data.slot.composition = composition_2
    scope_session.add(shift_sequence_data.slot)
    scope_session.commit()

    start_people_allocation(schedules_group_id=schedule.schedules_group_id)

    scope_session.refresh(shift_sequence_data.prev_prev_shift)
    scope_session.refresh(shift_sequence_data.prev_shift)
    scope_session.refresh(shift_sequence_data.shift)
    scope_session.refresh(shift_sequence_data.next_shift)
    scope_session.refresh(shift_sequence_data.next_next_shift)

    assert shift_sequence_data.prev_prev_shift.predicted_ratings == {staff_1.login: 0.0, staff_2.login: 0.0}
    assert shift_sequence_data.prev_shift.predicted_ratings == {staff_1.login: 48.0, staff_2.login: 0.0}

    # predicted_rating актуализируется в соответствии с новым составом
    assert (
        shift_sequence_data.shift.predicted_ratings
        == shift_sequence_data.next_shift.predicted_ratings
        == shift_sequence_data.next_next_shift.predicted_ratings
        == {staff_2.login: 0.0, staff_3.login: 0.0}
    )

    assert scope_session.query(Rating).filter(Rating.staff_id == staff_1.id).first().rating == 96.0


def test_do_not_take_into_account_current_ratings_when_rating_is_disabled(
    scope_session, schedule_factory, interval_factory, slot_factory, composition_factory, rating_factory,
    composition_participants_factory, assert_count_queries,
):
    interval = interval_factory(duration=datetime.timedelta(days=7))
    composition = composition_factory()
    slot_factory(interval=interval, composition=composition, points_per_hour=0)

    participants = [composition_participants_factory(composition=composition) for _ in range(4)]
    rating_values = [1000, 500, 0, 0]
    [
        rating_factory(staff=participants[i].staff, schedule=interval.schedule, rating=rating_values[i])
        for i in range(4)
    ]

    with patch('watcher.tasks.people_allocation.start_people_allocation'):
        initial_creation_of_shifts(interval.schedule.id)

    kwargs = {
        'start_date': interval.revision.apply_datetime,
        'schedules_group_id': interval.schedule.schedules_group.id,
    }
    with assert_count_queries(14):
        start_people_allocation(**kwargs)

    shifts = scope_session.query(Shift).order_by(Shift.start).all()
    # проверяем что дежурные чередуются в фиксированном порядке, без влияния рейтинга
    for i in range(4):
        for j in range(4, len(shifts) - i, 4):
            assert shifts[i+j].staff_id == shifts[i].staff_id


def test_changing_alternation_primary_backup(scope_session, slot_factory, composition_factory, staff_factory,
                                             composition_participants_factory, shift_factory):
    composition = composition_factory()
    for _ in range(2):
        composition_participants_factory(composition=composition, staff=staff_factory())
    slot1 = slot_factory(is_primary=False, composition=composition)
    slot_factory(interval=slot1.interval, is_primary=True, composition=composition)
    composition.service = slot1.interval.revision.schedule.service
    scope_session.add(composition)
    scope_session.commit()
    initial_creation_of_shifts(slot1.interval.schedule_id)
    shifts = scope_session.query(Shift).order_by(Shift.start).all()
    is_changing = True
    shifts_by_staff_login = defaultdict(list)
    for shift in shifts:
        shifts_by_staff_login[shift.staff.login].append(shift.is_primary)
    for key in shifts_by_staff_login:
        if is_changing:
            prev_is_prime = None
            for shift_status in shifts_by_staff_login[key]:
                if prev_is_prime == shift_status:
                    is_changing = False
                    break
                else:
                    prev_is_prime = shift_status
    assert is_changing is True
