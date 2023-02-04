import datetime
import logging
import pytest

from unittest.mock import patch
from freezegun import freeze_time

from sqlalchemy import or_

from watcher import enums
from watcher.config import settings
from watcher.crud.shift import (
    get_last_shift_by_schedule,
    query_main_shifts_by_slots,
    query_schedule_main_shifts,
    get_first_shift_by_schedule,
)
from watcher.db import Holiday, Interval, Shift
from watcher.logic.timezone import localize, now, today
from watcher.logic.shift import (
    bind_sequence_shifts,
    repair_sequence_shifts,
    generate_new_sequence_by_shift,
)
from watcher.tasks.generating_shifts import (
    get_cycle_duration,
    initial_creation_of_shifts,
    proceed_new_shifts,
    process_new_shifts_for_active_schedules,
    delete_disabled_shifts,
    sequence_shifts,
    create_shifts,
)

logger = logging.getLogger(__name__)


@pytest.mark.parametrize(('future', 'queries_number'), ((True, 10), (False, 9)))
def test_creating_shifts(
    scope_session, future, assert_count_queries, schedule_factory,
    revision_factory, interval_factory, slot_factory, queries_number
):
    """Тестируем обычное создание шифтов"""
    schedule = schedule_factory(threshold_day=datetime.timedelta(days=10))
    revision = revision_factory(schedule=schedule, state=enums.RevisionState.active)
    interval = interval_factory(
        schedule=schedule, duration=datetime.timedelta(days=5),
        revision=revision, unexpected_holidays=enums.IntervalUnexpectedHolidays.ignoring
    )
    slot_factory(interval=interval)
    if future:
        revision.apply_datetime = now() + datetime.timedelta(days=10)
        scope_session.commit()
    assert scope_session.query(Shift).filter(Shift.schedule_id == schedule.id).count() == 0
    with assert_count_queries(queries_number):
        with patch('watcher.tasks.people_allocation.start_people_allocation') as people_allocation_funk:
            initial_creation_of_shifts(schedule.id)
    people_allocation_funk.assert_called_once()
    scope_session.refresh(schedule)
    assert schedule.shifts_boundary_recalculation_error is None
    shifts = scope_session.query(Shift).filter(Shift.schedule_id == schedule.id).order_by(Shift.start).all()
    assert len(shifts) > 0
    for shift in shifts:
        assert shift.is_primary == shift.slot.is_primary


def test_creating_shifts_no_revision(schedule_data, scope_session):
    """
    Тестируем сохранение ошибки.
        - При отсутствии активной ревизии не будем создавать смены.
    """
    schedule = schedule_data.schedule
    revision = schedule_data.revision
    revision.state = enums.RevisionState.disabled
    scope_session.add(revision)
    scope_session.commit()

    assert schedule.shifts_boundary_recalculation_error is None

    initial_creation_of_shifts(schedule.id)

    scope_session.refresh(schedule)
    assert schedule.shifts_boundary_recalculation_error is None
    assert scope_session.query(Shift).filter_by(schedule_id=schedule.id).count() == 0


def test_cut_out_holidays(schedule_data, scope_session, creating_unexpected_holidays):
    """Тестируем выкалывание внезапных выходных"""

    interval = schedule_data.interval_1
    # сделаем смену в 7 дн, чтобы в нее попали обычные выходные
    interval.duration = datetime.timedelta(days=7)
    interval.unexpected_holidays = enums.IntervalUnexpectedHolidays.remove
    slot = schedule_data.slot_1
    slot.is_primary = False
    scope_session.add(interval)
    scope_session.commit()

    initial_creation_of_shifts(schedule_data.schedule.id)
    shift = query_main_shifts_by_slots(db=scope_session, slot_id=slot.id).order_by('start').first()
    assert not shift.empty
    sub_shifts = shift.sub_shifts
    assert len(sub_shifts) == 2
    for sub_shift in sub_shifts:
        assert sub_shift.is_primary == shift.is_primary == shift.slot.is_primary
    subshift = sorted(shift.sub_shifts, key=lambda x: x.start)[0]
    assert subshift.empty

    # в creating_unexpected_holidays создаётся внезапный выходной на сегодня
    # или ближайший рабочий день, если сегодня выходной
    day = [
        holiday.date for holiday in scope_session.query(Holiday).filter(Holiday.date >= today()).all()
        if holiday.date.weekday() < 5
    ][0]
    assert localize(subshift.start).date() == day
    assert localize(subshift.end).date() == day + datetime.timedelta(days=1)


def test_ignoring_holidays(schedule_data, scope_session, creating_unexpected_holidays):
    """Тестируем игнорирование внезапных выходных"""

    initial_creation_of_shifts(schedule_data.schedule.id)

    slot = schedule_data.slot_1
    shift = query_main_shifts_by_slots(
        db=scope_session,
        slot_id=slot.id
    ).order_by('start').first()

    assert not shift.empty
    assert localize(shift.start).date() == today()
    assert len(shift.sub_shifts) == 0


def test_unexpected_workday(schedule_data, scope_session):
    """
    Тестируем внезапные рабочие дни для пустого интервала.
    Не используем фикстуру выходных, поэтому все выходные в этом тесте станут "внезапно рабочими" днями.
    """
    # TODO: есть ощущение, что внезапные рабочие дни для пустого интервала будут интересовать только:
    #   - если внезапный рабочий день вначале/конце смены
    #   - если до этого был рабочий интервал
    #   Должно соблюдаться оба условия. Но сейчас не так.
    interval = schedule_data.interval_2
    # для пустого интервала сделаем смену в 7 дн, чтобы в нее попали обычные выходные
    interval.duration = datetime.timedelta(days=7)
    scope_session.add(interval)
    scope_session.commit()

    schedule = schedule_data.schedule
    empty_slot = schedule_data.slot_2

    initial_creation_of_shifts(schedule.id)

    scope_session.refresh(schedule)
    assert schedule.shifts_boundary_recalculation_error is None
    shift = scope_session.query(Shift).filter(Shift.slot_id == empty_slot.id, Shift.empty.is_(True)).first()
    assert len(shift.sub_shifts) > 0
    for s in shift.sub_shifts:
        if localize(s.start).date().weekday() >= 5:
            assert s.empty is False
        else:
            assert s.empty is True


def test_extend_weekends(schedule_data, scope_session, creating_unexpected_holidays):
    """Тестируем растягивание смен на субботу и воскресение"""

    schedule = schedule_data.schedule
    schedule.threshold_day = datetime.timedelta(days=20)
    scope_session.add(schedule)

    interval = schedule_data.interval_1
    interval.duration = datetime.timedelta(days=10)
    interval.weekend_behaviour = enums.IntervalWeekendsBehaviour.extend
    scope_session.add(interval)
    scope_session.commit()

    initial_creation_of_shifts(schedule_data.schedule.id)

    today_is_weekend = today().weekday() >= 5
    slot = schedule_data.slot_1
    shift = query_main_shifts_by_slots(db=scope_session, slot_id=slot.id).order_by('start').first()
    sub_shifts = sorted(shift.sub_shifts, key=lambda x: x.start)
    if today_is_weekend:
        # смены: ['< 2д вых', '5д будни', '2д вых', '5д будни']
        assert len(shift.sub_shifts) == 4
        sub_shift_1, sub_shift_2 = sub_shifts[0], sub_shifts[2]
    else:
        # смены: ['< 5д будни', '2д вых', '5д будни', '2д вых', '< 5д будни']
        assert len(shift.sub_shifts) == 5
        sub_shift_1, sub_shift_2 = sub_shifts[1], sub_shifts[3]
    assert sub_shift_1.empty
    assert sub_shift_2.empty


@freeze_time('2022-03-18 12:00:00')  # Пятница
def test_extend_weekends_empty_slot_shifts(schedule_factory, scope_session, revision_factory, interval_factory,
                                           holiday_factory):
    """Тестируем растягивание смен на субботу и воскресение"""

    now_time = now()
    schedule = schedule_factory(threshold_day=datetime.timedelta(days=15))
    revision = revision_factory(schedule=schedule, state=enums.RevisionState.active, apply_datetime=now_time)

    #  (2022, 3, 20) Суббота  - рабочий день
    holiday_factory(date=datetime.date(2022, 3, 20))  # Воскресенье
    holiday_factory(date=datetime.date(2022, 3, 26))  # Суббота
    holiday_factory(date=datetime.date(2022, 3, 27))  # Воскресенье

    interval_factory(
        schedule=schedule,
        duration=datetime.timedelta(days=1),
        type_employment=enums.IntervalTypeEmployment.empty,
        weekend_behaviour=enums.IntervalWeekendsBehaviour.extend,
        revision=revision
    )

    initial_creation_of_shifts(schedule.id)

    shifts = query_schedule_main_shifts(db=scope_session, schedule_id=schedule.id).order_by('start').all()

    first_weekend_shift = shifts[0]
    assert first_weekend_shift.start == now_time
    assert first_weekend_shift.end == first_weekend_shift.start + datetime.timedelta(days=3)
    assert len(first_weekend_shift.sub_shifts) == 3
    work_day_sub_shift = first_weekend_shift.sub_shifts[1]
    assert not work_day_sub_shift.empty
    assert work_day_sub_shift.start == settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2022, 3, 19))
    assert work_day_sub_shift.end == settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2022, 3, 20))

    next_weekend_shift = shifts[5]
    assert next_weekend_shift.start == first_weekend_shift.end + datetime.timedelta(days=4)
    assert next_weekend_shift.end == next_weekend_shift.start + datetime.timedelta(days=3)
    assert len(next_weekend_shift.sub_shifts) == 0


@pytest.mark.parametrize(
    ('start', 'duration'),
    (
        (settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2022, 4, 29, 22)), datetime.timedelta(hours=4)),
        (settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2022, 4, 29)), datetime.timedelta(days=4))
    )
)
def test_cut_out_weekends_and_holidays(
    holiday_factory, slot_factory, scope_session,
    interval_factory, duration, start
):
    """
    проверим такой кейс - смена начинается в пятницу
    в расписании мы не дежурим в выходные и праздники
    при этом следом за выходными идут два праздника
    - смена в результате должна продлиться и на выходные и на праздники
    """
    # 30 и 1 число выходные
    # 2 и 3 мая праздники
    interval = interval_factory(
        duration=duration,
        unexpected_holidays=enums.IntervalUnexpectedHolidays.remove,
        weekend_behaviour=enums.IntervalWeekendsBehaviour.extend
    )
    slot = slot_factory(interval=interval)
    holidays = [
        holiday_factory(date=datetime.date(2022, 5, 2)),
        holiday_factory(date=datetime.date(2022, 5, 3))
    ]
    main_shift, subshifts = create_shifts(
        session=scope_session,
        start=start,
        interval_cycle={interval: [slot]},
        schedule_id=interval.schedule_id,
        holidays=holidays,
        threshold=settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2022, 5, 10)),
    )
    subshifts.remove(main_shift)

    assert len(subshifts) == 2 if duration == datetime.timedelta(hours=4) else 3
    # часть смены до праздников
    assert subshifts[0].end == settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2022, 4, 30))
    assert not subshifts[0].empty

    if duration == datetime.timedelta(hours=4):
        # вырезаем и выходные и праздники
        assert subshifts[1].end == settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2022, 5, 2, 2))
        assert subshifts[1].empty

    else:
        # вырезаем и выходные и праздники
        assert subshifts[1].end == settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2022, 5, 4))
        assert subshifts[1].empty

        # оставшаяся часть дня после праздников (конец смены сместился на 2 дня)
        assert subshifts[2].end == start + duration + datetime.timedelta(days=2)
        assert not subshifts[2].empty


def test_cut_out_holidays_and_weekends(holiday_factory, slot_factory, scope_session, interval_factory):
    """
    проверим такой кейс - смена начинается 5 января 2022
    и идет три дня, 6 и 7 января - праздники, а 8 и 9 -
    выходные. В расписании мы не дежурим в выходные и праздники
    смена в результате должна должна растянуться и закончиться 10 января
    """
    interval = interval_factory(
        duration=datetime.timedelta(days=3),
        unexpected_holidays=enums.IntervalUnexpectedHolidays.remove,
        weekend_behaviour=enums.IntervalWeekendsBehaviour.extend
    )
    slot = slot_factory(interval=interval)
    holidays = [
        holiday_factory(date=datetime.date(2022, 1, 6)),
        holiday_factory(date=datetime.date(2022, 1, 7))
    ]

    main_shift, shifts = create_shifts(
        session=scope_session,
        start=settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2022, 1, 5, 10)),
        interval_cycle={interval: [slot]},
        schedule_id=interval.schedule_id,
        holidays=holidays,
        threshold=settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2022, 1, 15))
    )
    shifts.remove(main_shift)
    assert len(shifts) == 3
    # часть смены до праздников
    assert shifts[0].start == settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2022, 1, 5, 10, 0))
    assert shifts[0].end == settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2022, 1, 6))
    assert not shifts[0].empty
    # вырезаем праздники и выходные
    assert shifts[1].start == settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2022, 1, 6))
    assert shifts[1].end == settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2022, 1, 10))
    assert shifts[1].empty

    # оставшиеся 2 дня после праздников
    assert shifts[2].start == settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2022, 1, 10))
    assert shifts[2].end == settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2022, 1, 10, 10, 0))
    assert not shifts[2].empty


def test_duplicate_shifts(schedule_data, scope_session, slot_factory):
    """Тестируем создание идентичных копий, если слотов в интервале больше одного."""

    interval = schedule_data.interval_1
    slot_new = slot_factory(interval=interval, is_primary=False)
    scope_session.add(slot_new)
    scope_session.commit()

    initial_creation_of_shifts(schedule_data.schedule.id)
    shifts = scope_session.query(Shift).filter(
        or_(
            Shift.slot_id == slot_new.id,
            Shift.slot_id == schedule_data.slot_1.id
        ), Shift.start <= datetime.datetime.combine(today(), datetime.datetime.max.time())
    ).all()
    assert len(shifts) == 2
    for shift in shifts:
        assert shift.is_primary == shift.slot.is_primary


def test_shifts_cycle_count(schedule_data, scope_session):
    """
    Тестируем кол-во созданных циклов:
        - шифты, создаем на указанное в настройках графика кол-во дней
        - но не менее двух циклов
    В настройках графика schedule_data.schedule указано создание на 5 дней, тк реальный цикл больше 5 дней
        => должны создать два цикла
    Для второго графика сделаем интервал в 1 дн
    """

    initial_creation_of_shifts(schedule_data.schedule.id)

    shifts = scope_session.query(Shift).filter(Shift.slot_id == schedule_data.slot_1.id).all()
    assert len(shifts) == 2
    assert shifts[0].start != shifts[1].start


def test_shifts_cycle_count_threshold(schedule_data, scope_session):
    """
    Тестируем кол-во созданных циклов:
        - шифты, создаем на указанное в настройках графика кол-во дней
        - но не менее двух циклов
    Cделаем каждый интервал в 1 дн. Должно получится три цикла.
    """

    interval_1 = schedule_data.interval_1
    interval_1.duration = datetime.timedelta(days=1)
    scope_session.add(interval_1)

    interval_2 = schedule_data.interval_2
    interval_2.duration = datetime.timedelta(days=1)
    scope_session.add(interval_2)

    scope_session.commit()

    initial_creation_of_shifts(schedule_data.schedule.id)

    shifts = scope_session.query(Shift).filter(Shift.slot_id == schedule_data.slot_1.id).all()
    assert len(shifts) == 3
    assert shifts[0].start != shifts[1].start != shifts[2].start


def test_proceed_new_shifts_with_new_cycle(schedule_data, scope_session):
    """
    Тестируем добавление новых шифтов для расписания с течением времени
    """
    schedule_start = localize(datetime.datetime.combine(schedule_data.revision.apply_datetime, datetime.datetime.min.time()))

    initial_creation_of_shifts(schedule_data.schedule.id)

    shifts = query_main_shifts_by_slots(scope_session, schedule_data.slot_1.id).order_by(Shift.start).all()
    shifts_count_by_slot_1 = 2
    assert len(shifts) == shifts_count_by_slot_1
    assert shifts[0].start != shifts[1].start

    start_new_shifts_date = schedule_start + datetime.timedelta(days=7)
    with freeze_time(start_new_shifts_date):
        # достроим еще 1 цикл
        cycle_duration = get_cycle_duration(schedule_data.revision)
        all_shifts = query_schedule_main_shifts(scope_session, schedule_data.schedule.id).order_by(Shift.start).all()
        cycle_count = (localize(all_shifts[-1].end) - start_new_shifts_date) // cycle_duration

        new_shifts_count_by_slot_1 = shifts_count_by_slot_1 + settings.MIN_COUNT_OF_CYCLES
        if cycle_count > 0:
            new_shifts_count_by_slot_1 = shifts_count_by_slot_1 + settings.MIN_COUNT_OF_CYCLES - cycle_count

        proceed_new_shifts(schedule_data.schedule.id)

        shifts = query_main_shifts_by_slots(scope_session, schedule_data.slot_1.id).order_by(Shift.start).all()
        assert len(shifts) == new_shifts_count_by_slot_1

        for i in range(len(shifts) - 1):
            assert shifts[i].start != shifts[i + 1].start


def test_proceed_new_shifts_with_rest_of_last_cycle(schedule_data, scope_session, shift_factory):
    """
    Тестируем продолжение создания шифтов для расписания, прошлый цикл которого оборвался
    """
    schedule_data.schedule.threshold_day = datetime.timedelta(days=2)
    schedule_data.interval_2.type_employment = enums.IntervalTypeEmployment.full
    scope_session.commit()

    with patch('watcher.tasks.people_allocation.start_people_allocation'):
        initial_creation_of_shifts(schedule_data.schedule.id)

    last_shift = get_last_shift_by_schedule(scope_session, schedule_data.schedule.id)
    scope_session.delete(last_shift)
    scope_session.commit()

    shifts_1 = query_main_shifts_by_slots(scope_session, schedule_data.slot_1.id).all()
    shifts_2 = query_main_shifts_by_slots(scope_session, schedule_data.slot_2.id).all()

    assert len(shifts_1) == 2
    assert len(shifts_2) == 1

    with freeze_time(schedule_data.revision.apply_datetime + datetime.timedelta(days=1)):
        with patch('watcher.tasks.people_allocation.start_people_allocation.delay'):
            proceed_new_shifts(schedule_data.schedule.id)

        shifts_1 = query_main_shifts_by_slots(scope_session, schedule_data.slot_1.id).all()
        shifts_2 = query_main_shifts_by_slots(scope_session, schedule_data.slot_2.id).all()

        assert len(shifts_1) == 3
        assert len(shifts_2) == 3


@pytest.mark.skip
def test_proceed_new_shift_if_last_shift_slot_not_in_intervals(schedule_data, interval_factory, scope_session):
    """
    Тестируем досоздание новых шифтов.
    Если последний основной шифт расписания не принадлежит ни одному интервалу
    """
    initial_creation_of_shifts.apply(args=[schedule_data.schedule.id])

    assert get_last_shift_by_schedule(
        scope_session,
        schedule_data.schedule.id,
    ).slot == schedule_data.slot_2

    interval_3 = interval_factory()
    schedule_data.slot_2.interval_id = interval_3.id
    scope_session.add(schedule_data.slot_2)
    scope_session.commit()

    with patch('watcher.tasks.generating_shifts.revision_shift_boundaries.delay') as changing_patch:
        proceed_new_shifts(schedule_data.schedule.id)
        changing_patch.assert_called_once()


@freeze_time('2022-01-10')  # понедельник
@pytest.mark.parametrize('type_employment', [
    enums.IntervalTypeEmployment.full,
    enums.IntervalTypeEmployment.empty,
])
def test_proceed_new_shifts_dont_invoke_people_allocation(interval_factory, schedule_factory, slot_factory,
                                                          scope_session, revision_factory, type_employment):
    """
        Проверяем, что генерация шифтов, не вызывается перераспределение шифтов, если все новые шифты - пустые
    """

    schedule = schedule_factory(threshold_day=datetime.timedelta(days=1))
    revision = revision_factory(schedule=schedule, state=enums.RevisionState.active, apply_datetime=now())
    interval = interval_factory(
        schedule=schedule,
        duration=datetime.timedelta(days=1),
        type_employment=type_employment,
        revision=revision,
    )
    slot_factory(interval=interval)

    initial_creation_of_shifts(schedule.id)

    schedule.threshold_day = datetime.timedelta(days=2)
    scope_session.add(schedule)
    scope_session.commit()

    with patch('watcher.tasks.people_allocation.start_people_allocation.delay') as people_allocation_func:
        proceed_new_shifts(schedule.id)

    if type_employment == enums.IntervalTypeEmployment.full:
        people_allocation_func.assert_called_once()
    else:
        people_allocation_func.assert_not_called()


@pytest.mark.parametrize('state', [enums.ScheduleState.active, enums.ScheduleState.disabled])
def test_proceed_new_shifts_for_active_schedules(schedule_data, scope_session, revision_factory,
                                                 schedule_factory, interval_factory,
                                                 slot_factory, state):
    schedule_with_revision = schedule_factory(
        threshold_day=datetime.timedelta(days=5),
        state=state,
    )
    schedule_without_revision = schedule_factory(
        threshold_day=datetime.timedelta(days=5),
        state=state,
    )

    revision = revision_factory(
        schedule=schedule_with_revision,
        state=enums.RevisionState.active,
    )
    interval = interval_factory(
        schedule=schedule_with_revision,
        duration=datetime.timedelta(days=5),
        revision=revision,
    )
    slot_factory(interval=interval)

    process_new_shifts_for_active_schedules()

    shifts = query_schedule_main_shifts(scope_session, schedule_with_revision.id)
    if state == enums.ScheduleState.active:
        assert shifts.count() == 2
    else:
        assert shifts.count() == 0

    shifts = query_schedule_main_shifts(scope_session, schedule_without_revision.id)
    assert shifts.count() == 0


@pytest.mark.parametrize('date_from_start', ['now', 'future'])
def test_proceed_new_shifts_two_revisions(
    scope_session, revision_factory, schedule_factory,
    interval_factory, slot_factory, shift_factory, date_from_start,
):
    """
    Если нужно в процессе достройки заиспользовать две и более ревизий
    Запускаем таску сейчас или в будущем.
    """
    today_now = now()
    schedule = schedule_factory(
        threshold_day=datetime.timedelta(days=5),
        state=enums.ScheduleState.active,
    )
    prev_revision = revision_factory(
        schedule=schedule,
        state=enums.RevisionState.active,
        apply_datetime=today_now - datetime.timedelta(days=1),
    )
    first_shift = shift_factory(
        schedule=schedule,
        start=today_now - datetime.timedelta(days=1),
        end=today_now + datetime.timedelta(days=1),
        slot=slot_factory(
            interval=interval_factory(
                schedule=schedule,
                revision=prev_revision
            )
        )
    )

    day = today_now + datetime.timedelta(days=1)
    for i in range(3):
        revision = revision_factory(
            schedule=schedule,
            apply_datetime=day,
            prev=prev_revision,
        )
        interval = interval_factory(
            schedule=schedule,
            duration=datetime.timedelta(days=i+1),
            revision=revision,
        )
        slot_factory(interval=interval)
        day += datetime.timedelta(days=1)
        prev_revision = revision

    start = today_now
    if date_from_start == 'future':
        start = first_shift.end + datetime.timedelta(days=1)

    with freeze_time(start):
        with patch('watcher.tasks.generating_shifts.revision_shift_boundaries.delay') as revision_shift_boundaries:
            proceed_new_shifts(schedule.id)

    revision_shift_boundaries.assert_not_called()
    shifts = query_schedule_main_shifts(scope_session, schedule.id).order_by(Shift.start).all()
    for shift in shifts:
        logger.error(f'{shift.start}, {shift.end}, {shift.id}, {shift.replacement_for_id}')
    assert len(shifts) == 4

    assert shifts[0].start == today_now - datetime.timedelta(days=1)
    assert localize(shifts[0].end) == today_now + datetime.timedelta(days=1)
    assert shifts[1].start == shifts[0].end

    if date_from_start == 'future':
        assert shifts[1].slot.interval.duration == datetime.timedelta(days=2)
        assert shifts[2].slot.interval.duration == datetime.timedelta(days=3)
        assert shifts[2].end == shifts[2].start + datetime.timedelta(days=3)
        assert shifts[2].slot.interval.revision.apply_datetime == localize(shifts[2].start)
        assert shifts[2].prev == shifts[1]
    else:
        assert localize(shifts[1].slot.interval.revision.apply_datetime) == localize(shifts[1].start)
        assert shifts[1].slot.interval.duration == datetime.timedelta(days=1)
        assert shifts[2].slot.interval.duration == datetime.timedelta(days=2)
        # тут длительность 1 день, вместо 2 тк встуает в силу новая ревизия
        assert shifts[2].end == shifts[2].start + datetime.timedelta(days=1)
        assert shifts[2].slot.interval.revision.apply_datetime == localize(shifts[2].start)
        assert shifts[2].prev == shifts[1]
        assert shifts[3].slot.interval.revision.apply_datetime == localize(shifts[3].start)

    assert shifts[1].end == shifts[1].start + shifts[1].slot.interval.duration
    assert shifts[1].prev == shifts[0]
    assert shifts[2].start == shifts[1].end
    assert shifts[3].start == shifts[2].end
    assert shifts[3].slot.interval.duration == datetime.timedelta(days=3)
    assert shifts[3].end == shifts[3].start + shifts[3].slot.interval.duration
    assert shifts[3].prev == shifts[2]
    assert shifts[3].next is None


def test_sequence_shifts(
    schedule_data_with_composition, slot_factory, composition_factory,
    composition_participants_factory, scope_session
):
    """
    Тестируем последовательность шифтов для одного графика с несколькими слотами
    У интервала будет три слота:
        - в первом самый большой состав
        - во втором самый маленький
        - в третьем средний
    Сначала будет идти шифт из второго слота, за ним третий, потом первый
    """

    slot_1 = schedule_data_with_composition.slot_1
    composition_2 = composition_factory(service=schedule_data_with_composition.schedule.service)
    slot_2 = slot_factory(interval=slot_1.interval, composition=composition_2)

    for _ in range(2):
        composition_participants_factory(composition=composition_2)

    composition_3 = composition_factory(service=schedule_data_with_composition.schedule.service)
    slot_3 = slot_factory(interval=slot_1.interval, composition=composition_3)

    for _ in range(5):
        composition_participants_factory(composition=composition_3)

    initial_creation_of_shifts(schedule_data_with_composition.schedule.id)

    shift_1 = scope_session.query(Shift).filter(Shift.slot == slot_1).order_by('start').first()
    shift_2 = scope_session.query(Shift).filter(Shift.slot == slot_2).order_by('start').first()
    shift_3 = scope_session.query(Shift).filter(Shift.slot == slot_3).order_by('start').first()
    assert shift_2.next == shift_3
    assert shift_3.next == shift_1
    assert shift_2.prev is None


def test_get_schedule_first_shift_if_sequence_is_broken(shift_factory, scope_session):
    first_shift = shift_factory()
    another_first_shift = shift_factory(
        slot=first_shift.slot,
        start=first_shift.start + datetime.timedelta(minutes=15),
        schedule=first_shift.schedule,
    )

    shift, valid = get_first_shift_by_schedule(
        db=scope_session,
        schedule_id=first_shift.schedule_id,
        query=scope_session.query(Shift).filter(Shift.schedule_id==first_shift.schedule_id)
    )
    assert shift.id == first_shift.id
    assert valid is False

    generate_new_sequence_by_shift(db=scope_session, shift=shift)
    scope_session.commit()

    shift, valid = get_first_shift_by_schedule(
        db=scope_session,
        schedule_id=first_shift.schedule_id,
        query=scope_session.query(Shift).filter(Shift.schedule_id == first_shift.schedule_id)
    )

    scope_session.refresh(first_shift)
    assert shift.id == first_shift.id
    assert first_shift.next_id == another_first_shift.id
    assert valid is True


@pytest.mark.parametrize('is_broken', [True, False])
@pytest.mark.parametrize('is_reversed', [True, False])
def test_get_schedule_last_shift_if_sequence_is_broken(
    shift_factory, slot_factory, interval_factory, scope_session,
    is_reversed, is_broken
):
    """
    Есть параллельные шифты
    Один из них имеет замену, а другой нет, при этом первый более приоритетный слот, поэтому в последовательности
    он будет раньше, а значит последняя замена первого параллельного шифта должна сослаться на второй параллельный,
    и вторая проверка, что последним шифтом мы считаем второй в параллельный, а не замены
    Проверяем как ведет себя функция get_schedule_last_shift в том случае если последовательность сломана
    Если последовательность сломана - мы пытаемся его восстановить и еще раз проверить последний шифт
    """
    last_time = datetime.datetime(2019, 1, 1)
    threshold_time = datetime.datetime(2019, 1, 3)
    end_time = datetime.datetime(2019, 1, 5)

    main_shift = shift_factory(start=last_time, end=end_time)
    schedule = main_shift.schedule

    sub_shift1 = shift_factory(start=last_time, end=threshold_time,
                               schedule=schedule, replacement_for=main_shift, slot=main_shift.slot)
    sub_shift2 = shift_factory(start=threshold_time, end=end_time,
                               schedule=schedule, replacement_for=main_shift, slot=main_shift.slot)
    slot = slot_factory(interval=interval_factory(revision=main_shift.slot.interval.revision))
    parallel_shift = shift_factory(start=last_time, end=end_time, schedule=schedule, slot=slot)

    shifts = [main_shift, sub_shift1, parallel_shift, sub_shift2]
    if is_reversed:
        # в этом случае сабшифт идет первее самого шифта, но все равно дублирования не должно быть
        shifts = [sub_shift1, main_shift, parallel_shift, sub_shift2]

    intervals = [main_shift.slot.interval, parallel_shift.slot.interval]

    sequence = sequence_shifts(scope_session, schedule, shifts, intervals)

    assert len(sequence) == 3
    assert sequence[0] == sub_shift1
    assert sequence[1] == parallel_shift
    assert sequence[2] == sub_shift2

    if not is_broken:
        bind_sequence_shifts(sequence)
        scope_session.commit()

    last_shift = get_last_shift_by_schedule(scope_session, schedule.id)

    if is_broken:
        assert not last_shift
        repair_sequence_shifts(scope_session, schedule.id)
        scope_session.commit()
        last_shift = get_last_shift_by_schedule(scope_session, schedule.id)

    assert last_shift == sub_shift2


def test_sequence_shifts_group(
    schedule_data_with_composition, scope_session, schedule_factory, revision_factory,
    interval_factory, slot_factory, composition_participants_factory
):
    """
    Тестируем последовательность шифтов для группы графиков.
    Ожидаем сначала 3-ий слот для исходного графика.
    После: слот третьего графика и второго.
    После: 2 и 1 слоты исходного графика.
    """

    slot_2 = slot_factory(interval=schedule_data_with_composition.slot_1.interval)
    for _ in range(3):
        composition_participants_factory(composition=slot_2.composition)

    slot_3 = slot_factory(interval=schedule_data_with_composition.slot_1.interval)
    composition_participants_factory(composition=slot_3.composition)

    initial_creation_of_shifts(schedule_data_with_composition.schedule.id)

    schedules_group = schedule_data_with_composition.schedule.schedules_group
    schedule_ids = [schedule_data_with_composition.schedule.id, ]
    slots = []
    for _ in range(2):
        schedule_new = schedule_factory(threshold_day=datetime.timedelta(days=5), schedules_group=schedules_group)
        revision = revision_factory(schedule=schedule_new, state=enums.RevisionState.active)
        interval = interval_factory(schedule=schedule_new, duration=datetime.timedelta(days=5), revision=revision)
        slot_new = slot_factory(interval=interval)
        slots.append(slot_new)
        initial_creation_of_shifts(schedule_id=schedule_new.id)
        schedule_ids.append(schedule_new.id)

    for _ in range(2):
        composition_participants_factory(composition=slot_new.composition)

    shifts = scope_session.query(Shift).filter(Shift.schedule_id.in_(schedule_ids)).order_by('start').all()
    # для группы мне нужны просто интервалы, порядок не важен
    interval_cycle = scope_session.query(Interval).filter(Interval.schedule_id.in_(schedule_ids)).all()
    result = sequence_shifts(
        session=scope_session,
        schedule=schedule_data_with_composition.schedule,
        shifts=shifts,
        intervals=interval_cycle,
        for_group=True,
    )

    sorted_slots = [s.slot for s in result if localize(s.start).date() == today()]

    assert sorted_slots == [slot_3, slot_new, slots[0], slot_2, schedule_data_with_composition.slot_1]


@freeze_time('2021-11-01')
def test_delete_disabled_shifts(scope_session, revision_factory, schedule_factory, interval_factory, slot_factory, shift_factory,
                                staff_factory, gap_factory):
    """
    Тестируем завершение и удаление шифтов для графиков в статусе disabled.
    Ожидаем:
    - смены которые завершатся до текущего времени - останутся без изменений
    - текущие смены - переводятся в scheduled и отбирается роль у дежурного
    - остальные удаляются
    """

    today_now = now()
    schedule = schedule_factory(length_of_absences=datetime.timedelta(minutes=15), state=enums.ScheduleState.disabled)
    watcher = staff_factory(login='rick')

    shift_first = shift_factory(
        schedule=schedule,
        staff=watcher,
        start=today_now - datetime.timedelta(days=3),
        end=today_now - datetime.timedelta(days=2),
        status=enums.ShiftStatus.completed,
    )

    shift_second = shift_factory(
        schedule=schedule,
        staff=watcher,
        start=today_now - datetime.timedelta(days=2),
        end=today_now + datetime.timedelta(days=1),
        approved=True,
        status=enums.ShiftStatus.active,
    )

    replace_shift = shift_factory(
        schedule=schedule,
        staff=watcher,
        start=today_now - datetime.timedelta(days=2),
        end=today_now - datetime.timedelta(days=1),
        approved=True,
        replacement_for=shift_second,
        prev=shift_first,
    )

    shift_factory(
        schedule=schedule,
        staff=watcher,
        start=today_now + datetime.timedelta(days=3),
        end=today_now + datetime.timedelta(days=5),
        approved=True,
        prev=shift_second,
    )

    delete_disabled_shifts(schedule_id=schedule.id)

    shifts = scope_session.query(Shift).filter(Shift.schedule_id==schedule.id)
    assert shifts.count() == 3
    assert {obj.id for obj in shifts} == {shift_first.id, shift_second.id, replace_shift.id}
    scope_session.refresh(shift_second)
    assert shift_second.status == enums.ShiftStatus.scheduled
    assert not shift_second.next_id


def test_cut_out_holidays_with_some_slots(
    schedule_data, scope_session, schedule_factory,
    interval_factory, slot_factory, creating_unexpected_holidays
):
    """Тестируем правильное выставление параметра is_primary при
    выкалывании внезапных выходных в интервале со слотами с разным is_primary"""
    schedule = schedule_factory(threshold_day=datetime.timedelta(days=5))
    interval = interval_factory(schedule=schedule)
    interval.unexpected_holidays = enums.IntervalUnexpectedHolidays.remove
    interval.duration = datetime.timedelta(days=7)
    slot_factory(interval=interval)
    slot_factory(interval=interval, is_primary=False)
    scope_session.commit()
    initial_creation_of_shifts(schedule.id)
    shifts = scope_session.query(Shift).filter(Shift.replacement_for_id.is_(None)).order_by('start').limit(2)
    for shift in shifts:
        sub_shifts = shift.sub_shifts
        assert len(sub_shifts) == 2
        for sub_shift in sub_shifts:
            assert sub_shift.is_primary == shift.is_primary == shift.slot.is_primary


def test_proceed_new_shifts_two_revisions_extend_weekends(
    scope_session, revision_factory, schedule_factory,
    interval_factory, slot_factory, shift_factory,
):
    """
    Проверим, что продолжение смены из-за вырезанных выходных не залезает на следующую ревизию
    """
    today_now = settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2022, 4, 11, 10))  # понедельник
    schedule = schedule_factory(
        threshold_day=datetime.timedelta(days=12),
        state=enums.ScheduleState.active,
    )
    # Создадим начальный шифт и ревизию
    prev_revision = revision_factory(
        schedule=schedule,
        state=enums.RevisionState.active,
        apply_datetime=today_now - datetime.timedelta(days=1),
    )
    shift_factory(
        schedule=schedule,
        start=today_now - datetime.timedelta(days=1),
        end=today_now,
        slot=slot_factory(
            interval=interval_factory(
                schedule=schedule,
                revision=prev_revision
            )
        )
    )
    # Первая ревизия должна вырезать выходные, но не продлевать смену, т.к. со следующего пн новая ревизия
    revision_1 = revision_factory(
        schedule=schedule,
        apply_datetime=today_now,
        prev=prev_revision,
    )
    interval_1 = interval_factory(
        schedule=schedule,
        duration=datetime.timedelta(days=7),
        revision=revision_1,
        weekend_behaviour=enums.IntervalWeekendsBehaviour.extend
    )
    slot_factory(interval=interval_1)

    revision_2 = revision_factory(
        schedule=schedule,
        apply_datetime=today_now + datetime.timedelta(days=7),
        prev=revision_1,
    )
    interval_2 = interval_factory(
        schedule=schedule,
        duration=datetime.timedelta(days=5),
        revision=revision_2,
    )
    slot_factory(interval=interval_2)

    with freeze_time(today_now):
        with patch('watcher.tasks.generating_shifts.revision_shift_boundaries.delay') as revision_shift_boundaries:
            proceed_new_shifts(schedule.id)

    revision_shift_boundaries.assert_not_called()
    shifts = query_schedule_main_shifts(scope_session, schedule.id).order_by(Shift.start).all()
    assert len(shifts) == 3

    assert shifts[0].start == today_now - datetime.timedelta(days=1)
    assert localize(shifts[0].end) == today_now

    assert shifts[1].start == shifts[0].end
    assert shifts[1].slot.interval.duration == datetime.timedelta(days=7)
    assert shifts[1].end == today_now + datetime.timedelta(days=7)

    sub_shifts = shifts[1].sub_shifts
    assert len(sub_shifts) == 3
    assert not sub_shifts[0].empty
    assert sub_shifts[0].end == settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2022, 4, 16))  # начало субботы
    assert sub_shifts[1].end == settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2022, 4, 18))  # конец воскресенья
    assert sub_shifts[1].empty
    assert sub_shifts[2].end == today_now + datetime.timedelta(days=7)
    assert not sub_shifts[2].empty

    assert shifts[2].slot.interval.duration == datetime.timedelta(days=5)


@freeze_time('2022-02-08')  # Вторник
def test_extend_weekends_threshold(schedule_factory, scope_session, revision_factory, interval_factory,
                                   holiday_factory, slot_factory):
    now_time = now()
    schedule = schedule_factory(threshold_day=datetime.timedelta(days=30))
    revision = revision_factory(schedule=schedule, state=enums.RevisionState.active, apply_datetime=now_time)

    interval = interval_factory(
        schedule=schedule,
        duration=datetime.timedelta(days=5),
        weekend_behaviour=enums.IntervalWeekendsBehaviour.extend,
        revision=revision
    )
    slot_factory(interval=interval)

    with patch('watcher.tasks.people_allocation.start_people_allocation'):
        initial_creation_of_shifts(schedule.id)

    shifts = query_schedule_main_shifts(db=scope_session, schedule_id=schedule.id).order_by('start').all()

    # 5-дневные смены должны растягиваться до 7-дневных, вне зависимости от threshold
    for shift in shifts:
        assert (shift.end - shift.start) == datetime.timedelta(days=7)
