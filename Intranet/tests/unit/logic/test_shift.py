import datetime
import pytest

from watcher.tasks.generating_shifts import create_shifts
from watcher.logic.shift import (
    generate_new_sequence_by_shift,
    shift_total_points,
)
from watcher.config import settings
from watcher import enums


def test_generate_new_sequence_by_shift(shift_factory, slot_factory, scope_session):
    slot1 = slot_factory()
    slot2 = slot_factory()

    shift1 = shift_factory(slot=slot1)
    shift2 = shift_factory(prev=shift1, slot=slot2)
    shift3 = shift_factory(schedule=shift1.schedule, prev=shift2, slot=slot1)

    next_shift = generate_new_sequence_by_shift(scope_session, shift1)
    scope_session.refresh(shift1.next)

    assert next_shift == shift3
    assert shift1.next == shift3


def test_cut_out_holidays_on_day_break(shift_factory, holiday_factory, slot_factory, scope_session, interval_factory):
    """
    Проверяет выкалывание выходных, для случая когда смена
    заканчивается/начинается в 00:00
    """
    interval = interval_factory(
        duration=datetime.timedelta(days=3),
        unexpected_holidays=enums.IntervalUnexpectedHolidays.remove
    )
    slot = slot_factory(interval=interval)
    holidays = [holiday_factory(date=datetime.date(2021, 3, 8))]

    main_shift, subshifts = create_shifts(
        session=scope_session,
        start=settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2021, 3, 5)),
        interval_cycle={interval: [slot]},
        schedule_id=interval.schedule_id,
        holidays=holidays,
        threshold=settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2021, 3, 15)),
    )
    subshifts.remove(main_shift)
    assert not subshifts

    main_shift, subshifts = create_shifts(
        session=scope_session,
        start=settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2021, 3, 8)),
        interval_cycle={interval: [slot]},
        schedule_id=interval.schedule_id,
        holidays=holidays,
        threshold=settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2021, 3, 15)),
    )
    subshifts.remove(main_shift)
    assert len(subshifts) == 2
    assert subshifts[0].start == settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2021, 3, 8))
    assert subshifts[0].end == subshifts[1].start == settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2021, 3, 9))
    assert subshifts[1].end == settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2021, 3, 11))


def test_cut_out_big_holidays_on_day_break(
    shift_factory, holiday_factory, slot_factory,
    scope_session, interval_factory
):
    """
    Проверяет разбиение смены, когда внутри смены - несколько дней выходных
    """
    interval = interval_factory(
        duration=datetime.timedelta(days=2),
        unexpected_holidays=enums.IntervalUnexpectedHolidays.remove
    )
    slot = slot_factory(interval=interval)
    holidays = [
        holiday_factory(date=datetime.date(2022, 5, 9)),
        holiday_factory(date=datetime.date(2022, 5, 10))
    ]
    inside_holidays_shift, subshifts = create_shifts(
        session=scope_session,
        start=settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2022, 5, 9)),
        interval_cycle={interval: [slot]},
        schedule_id=interval.schedule_id,
        holidays=holidays,
        threshold=settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2022, 5, 15)),
    )
    subshifts.remove(inside_holidays_shift)
    assert len(subshifts) == 1
    assert subshifts[0].start == inside_holidays_shift.start
    assert subshifts[0].end == settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2022, 5, 11))

    interval.duration += datetime.timedelta(days=5)
    scope_session.commit()

    overlapping_holidays_shift, subshifts = create_shifts(
        session=scope_session,
        start=settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2022, 5, 5)),
        interval_cycle={interval: [slot]},
        schedule_id=interval.schedule_id,
        holidays=holidays,
        threshold=settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2022, 5, 15)),
    )
    subshifts.remove(overlapping_holidays_shift)
    assert len(subshifts) == 3
    assert subshifts[0].start == overlapping_holidays_shift.start
    assert subshifts[0].end == subshifts[1].start == settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2022, 5, 9))
    assert subshifts[1].end == subshifts[2].start == settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2022, 5, 11))
    assert subshifts[2].end == settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2022, 5, 12))


@pytest.mark.parametrize('cut_holidays', (True, False))
@pytest.mark.parametrize('extend_weekends', (True, False))
def test_cut_holidays_extend_weekends(
    shift_factory, holiday_factory,
    slot_factory, scope_session, cut_holidays,
    extend_weekends, interval_factory
):
    """
    Проверяет разбиение смены, и смещение дат шифтов
    """
    interval = interval_factory(
        duration=datetime.timedelta(days=5),
        unexpected_holidays=enums.IntervalUnexpectedHolidays.remove if cut_holidays else enums.IntervalUnexpectedHolidays.ignoring,
        weekend_behaviour=enums.IntervalWeekendsBehaviour.extend if extend_weekends else enums.IntervalWeekendsBehaviour.ignoring
    )
    slot = slot_factory(interval=interval)
    holidays = [
        holiday_factory(date=datetime.date(2022, 2, 10)),  # четверг
        holiday_factory(date=datetime.date(2022, 2, 12)),  # суббота
        holiday_factory(date=datetime.date(2022, 2, 13))  # воскресенье
    ]
    shift, subs = create_shifts(
        session=scope_session,
        start=settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2022, 2, 10)),
        interval_cycle={interval: [slot]},
        schedule_id=interval.schedule_id,
        holidays=holidays,
        threshold=settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2022, 2, 20)),
    )

    subs.remove(shift)
    if cut_holidays and extend_weekends:
        assert len(subs) == 4
        assert subs[0].start == shift.start
        assert subs[0].end == subs[1].start == settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2022, 2, 11))
        assert subs[1].end == subs[2].start == settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2022, 2, 12))
        assert subs[2].end == subs[3].start == settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2022, 2, 14))
        assert subs[3].end == shift.end == settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2022, 2, 17))
    elif cut_holidays and not extend_weekends:
        assert len(subs) == 2
        assert subs[0].start == shift.start
        assert subs[0].end == subs[1].start == settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2022, 2, 11))
        assert subs[1].end == shift.end == settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2022, 2, 15))
    elif not cut_holidays and extend_weekends:
        assert len(subs) == 3
        assert subs[0].start == shift.start
        assert subs[0].end == subs[1].start == settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2022, 2, 12))
        assert subs[1].end == subs[2].start == settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2022, 2, 14))
        assert subs[2].end == shift.end == settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2022, 2, 17))
    else:
        assert len(subs) == 0


def test_shift_total_points(shift_factory):
    shift = shift_factory(slot=None)
    assert shift_total_points(shift=shift) == 1
