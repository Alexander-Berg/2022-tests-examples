import datetime
import pytest

from mock import patch
from freezegun import freeze_time

from watcher import enums
from watcher.config import settings
from watcher.crud.shift import (
    query_all_shifts_by_schedule,
    query_schedule_main_shifts
)
from watcher.db import Holiday, Shift
from watcher.logic.timezone import localize, now
from watcher.tasks.generating_shifts import initial_creation_of_shifts, revision_shift_boundaries


@pytest.mark.parametrize('revision_start', ['now', 'future'])
def test_revision_shift_boundaries(
    scope_session, revision_start, schedule_data,
    interval_factory, revision_factory, slot_factory,
):
    """
    Тестируем изменение границ с даты новой ревизии.
    Ревизия может стартовать прям сейчас или в будущем,
        как близком, в котором должна быть учтена,
        так и в далёком, за пределыми трешхолда, тогда ничего не поменяется
    """

    schedule = schedule_data.schedule
    old_revision = schedule_data.interval_1.revision
    old_revision.apply_datetime = now() - datetime.timedelta(days=3)

    # TODO: убрать изменения интервала после починки бага ABC-11883
    # ------->
    interval_2 = schedule_data.interval_2
    interval_2.type_employment = enums.IntervalTypeEmployment.full
    # <------

    scope_session.commit()
    scope_session.refresh(schedule)

    with patch('watcher.tasks.people_allocation.start_people_allocation'):
        initial_creation_of_shifts(schedule.id)

    interval_old = schedule_data.interval_1
    apply_datetime = now()
    if revision_start == 'future':
        apply_datetime += datetime.timedelta(days=1)

    revision_new = revision_factory(schedule=schedule, apply_datetime=apply_datetime, prev=interval_old.revision)
    interval_new = interval_factory(schedule=schedule, duration=datetime.timedelta(days=7), revision=revision_new)
    slot_factory(interval=interval_new)

    with patch('watcher.tasks.people_allocation.start_people_allocation.delay'):
        revision_shift_boundaries(schedule_id=schedule.id, revision_id=revision_new.id)
    shifts = query_schedule_main_shifts(scope_session, schedule.id).all()
    for shift in shifts:
        print(shift.start, shift.end, shift.id, shift.replacement_for_id, shift.slot.id)
    assert len(shifts) == 3
    """
    тут с now такая проблема - по умолчанию возвращается 2 шифта
    https://jing.yandex-team.ru/files/smosker/2022-04-06_15-05-46.png

    так как слот первой ревизии - вообще удаляется из за
    https://a.yandex-team.ru/arc_vcs/intranet/watcher/src/watcher/tasks/generating_shifts.py?rev=d7de22a552#L1143
    вот этого месте (revision-shift-boundaries запускается два раза в итоге)
    а происходит это потому что ревизию мы вычесляем зачем-то отдельно а не просто
    берем у последнего шифта
    и вот тут она не совпадает

    а если убрать этот кусок совсем тест проходит - подумать а зачем он?
    https://jing.yandex-team.ru/files/smosker/2022-04-07_08-20-45.png

    а вот если там выставлять ревизию просто от последнего шифта

    а в транке вот такой вывод тут https://jing.yandex-team.ru/files/smosker/2022-04-06_15-59-43.png
    """
    assert shifts[0].start == old_revision.apply_datetime
    assert localize(shifts[0].end).date() == apply_datetime.date()

    for i in range(1, len(shifts)):
        assert localize(shifts[i].start) == localize(shifts[i - 1].end)
        assert localize(shifts[i].end).date() == localize(shifts[i - 1].end).date() + datetime.timedelta(days=7)


def test_reducing_slots_in_interval(scope_session, schedule_factory, revision_factory, interval_factory, slot_factory):
    """
    В интервале было несколько слотов. Остался один.
    """
    schedule = schedule_factory(threshold_day=datetime.timedelta(days=15))
    revision = revision_factory(
        schedule=schedule,
        state=enums.RevisionState.active,
        apply_datetime=now() - datetime.timedelta(minutes=5),
    )
    interval = interval_factory(schedule=schedule, duration=datetime.timedelta(days=5), revision=revision)
    slot_1 = slot_factory(interval=interval)
    slot_2 = slot_factory(interval=interval)
    with patch('watcher.tasks.people_allocation.start_people_allocation'):
        initial_creation_of_shifts(schedule.id)

    old_shifts = query_schedule_main_shifts(scope_session, schedule.id).all()
    assert len(old_shifts) == 6
    for shift in old_shifts:
        assert shift.slot_id == slot_1.id or shift.slot_id == slot_2.id

    revision_new = revision_factory(schedule=schedule, prev=revision)
    interval_new = interval_factory(schedule=schedule, duration=datetime.timedelta(days=5), revision=revision_new)
    slot_new = slot_factory(interval=interval_new)

    with patch('watcher.tasks.people_allocation.start_people_allocation.delay'):
        revision_shift_boundaries(schedule_id=schedule.id, revision_id=revision_new.id)

    shifts = query_schedule_main_shifts(scope_session, schedule.id).all()
    # тк два текущих должны закончится и ещё 3 должны быть вперед
    assert len(shifts) == 5
    for shift in shifts[:2]:
        # первые два относятся к старой ревизии и старым слотам
        assert shift.slot_id == slot_1.id or shift.slot_id == slot_2.id
    for shift in shifts[2:]:
        assert shift.slot_id == slot_new.id


def test_increment_slots(scope_session, schedule_factory, revision_factory, interval_factory, slot_factory):
    """
    Был один слот, теперь несколько
    """
    schedule = schedule_factory(threshold_day=datetime.timedelta(days=6))
    revision = revision_factory(
        schedule=schedule,
        state=enums.RevisionState.active,
        apply_datetime=now() - datetime.timedelta(minutes=5)
    )
    interval = interval_factory(schedule=schedule, duration=datetime.timedelta(days=2), revision=revision)
    slot_old = slot_factory(interval=interval)

    with patch('watcher.tasks.people_allocation.start_people_allocation'):
        initial_creation_of_shifts(schedule.id)

    old_shifts = query_schedule_main_shifts(scope_session, schedule.id).all()
    assert len(old_shifts) == 3
    for shift in old_shifts:
        assert shift.slot_id == slot_old.id

    revision_new = revision_factory(schedule=schedule, prev=revision)
    interval_new = interval_factory(
        schedule=schedule, duration=datetime.timedelta(days=2),
        revision=revision_new,
    )
    slot_new_1 = slot_factory(interval=interval_new)
    slot_new_2 = slot_factory(interval=interval_new)

    with patch('watcher.tasks.people_allocation.start_people_allocation.delay'):
        revision_shift_boundaries(schedule_id=schedule.id, revision_id=revision_new.id)

    shifts = query_schedule_main_shifts(scope_session, schedule.id).all()
    # текущий шифт завершим
    # и еще 3 * 2 шифтов
    assert len(shifts) == 7

    assert shifts[0].slot_id == slot_old.id
    for shift in shifts[1::2]:
        assert shift.slot_id == slot_new_1.id

    for shift in shifts[2::2]:
        assert shift.slot_id == slot_new_2.id


@pytest.mark.parametrize(('old_unexpected_holidays', 'new_unexpected_holidays', 'new_shifts_all_count'), [
    (enums.IntervalUnexpectedHolidays.ignoring, enums.IntervalUnexpectedHolidays.remove, 5),
    (enums.IntervalUnexpectedHolidays.remove, enums.IntervalUnexpectedHolidays.ignoring, 4),
])
def test_change_unexpected_holidays(
    scope_session, old_unexpected_holidays, new_unexpected_holidays, new_shifts_all_count,
    schedule_factory, revision_factory, interval_factory, slot_factory,
    creating_unexpected_holidays,
):
    """
    График 7 дн. Есть внезапный выходной.
    Два варианта:
        - Изначально был график без выреза выходных, теперь с вырезом =>
            не было подсмен, теперь будут
        - Изначально подсмена была, теперь отменяем настройку, она должна удалится
    """
    schedule = schedule_factory(threshold_day=datetime.timedelta(days=6))
    revision = revision_factory(
        schedule=schedule,
        state=enums.RevisionState.active,
        apply_datetime=now() - datetime.timedelta(minutes=5),
    )
    interval_old = interval_factory(
        schedule=schedule,
        duration=datetime.timedelta(days=7),
        revision=revision,
        unexpected_holidays=old_unexpected_holidays
    )
    slot_factory(interval=interval_old)

    with patch('watcher.tasks.people_allocation.start_people_allocation'):
        initial_creation_of_shifts(schedule.id)

    old_shifts = query_schedule_main_shifts(scope_session, schedule.id).all()
    assert len(old_shifts) == 2
    holidays = scope_session.query(Holiday).filter(
        Holiday.date >= now(),
        Holiday.date <= now() + datetime.timedelta(days=10)
    ).all()
    assert len(holidays) > 0
    if old_unexpected_holidays == enums.IntervalUnexpectedHolidays.remove:
        old_shifts_all = query_all_shifts_by_schedule(scope_session, schedule.id).order_by(Shift.start).all()
        assert len(old_shifts_all) == 4

    revision_new = revision_factory(schedule=schedule, prev=revision)
    interval_new = interval_factory(
        schedule=schedule,
        duration=datetime.timedelta(days=7),
        revision=revision_new,
        unexpected_holidays=new_unexpected_holidays,
    )
    new_slot = slot_factory(interval=interval_new)

    with patch('watcher.tasks.people_allocation.start_people_allocation.delay'):
        revision_shift_boundaries(schedule_id=schedule.id, revision_id=revision_new.id)

    shifts = query_schedule_main_shifts(scope_session, schedule.id).all()
    # текущий шифт завершим, основных будет 3
    # основные смены (кроме второй) должны быть в олде
    # тк текущая просто заканчивается и апдетится, затем рисуется новая, затем уже отстроенная
    assert len(shifts) == 3
    assert shifts[0] in old_shifts
    shifts_all = query_all_shifts_by_schedule(scope_session, schedule.id).order_by(Shift.start).all()
    assert len(shifts_all) == new_shifts_all_count
    if new_unexpected_holidays == enums.IntervalUnexpectedHolidays.remove:
        for shift in shifts_all:
            if shift.replacement_for_id:
                assert shift.replacement_for_id == shifts[1].id
                assert shift.slot_id == new_slot.id


@pytest.mark.parametrize(('day', 'new_shifts_count'), [(5, 3), (7, 5)])
def test_change_apply_datetime_of_future_revision(
    scope_session, day, new_shifts_count,
    schedule_factory, revision_factory, interval_factory,
    slot_factory,
):
    """
    Изменение даты будущей ревизии.
    Подвинуть дату можем вперед или назад.
    """
    schedule = schedule_factory(threshold_day=datetime.timedelta(days=15))
    revision_1 = revision_factory(schedule=schedule, state=enums.RevisionState.active)
    interval_1 = interval_factory(
        schedule=schedule,
        duration=datetime.timedelta(days=5),
        revision=revision_1,
    )
    slot_factory(interval=interval_1)

    with patch('watcher.tasks.people_allocation.start_people_allocation'):
        initial_creation_of_shifts(schedule.id)

    apply_datetime = revision_1.apply_datetime + datetime.timedelta(days=6)
    revision_2 = revision_factory(
        schedule=schedule,
        state=enums.RevisionState.active,
        apply_datetime=apply_datetime,
        prev=revision_1
    )
    interval_2 = interval_factory(
        schedule=schedule,
        duration=datetime.timedelta(days=7),
        revision=revision_2,
    )
    slot_factory(interval=interval_2)

    with patch('watcher.tasks.people_allocation.start_people_allocation.delay'):
        revision_shift_boundaries(schedule_id=schedule.id, revision_id=revision_2.id)

    old_shifts = query_schedule_main_shifts(scope_session, schedule.id).all()
    assert len(old_shifts) == 4

    # теперь поменяем дату начала у будущей ревизии и пересчитаем
    # при day == 5 двигаем назад, а при day == 7 -> вперед
    # тк изначально ревизия начиналась через 6 дней
    apply_datetime = now() + datetime.timedelta(days=day)
    revision_2.apply_datetime = apply_datetime
    scope_session.commit()
    with patch('watcher.tasks.people_allocation.start_people_allocation.delay'):
        revision_shift_boundaries(schedule_id=schedule.id, revision_id=revision_2.id)

    shifts = query_schedule_main_shifts(scope_session, schedule.id).all()
    assert len(shifts) == new_shifts_count

    for i in range(1, len(shifts)):
        assert localize(shifts[i].start) == localize(shifts[i - 1].end)

    assert localize(shifts[0].start) == localize(revision_1.apply_datetime)
    assert localize(shifts[0].end) == localize(shifts[0].start) + datetime.timedelta(days=5)
    if day == 5:
        assert localize(shifts[0].end) == apply_datetime
        assert localize(shifts[1].end) == apply_datetime + datetime.timedelta(days=7)
        assert localize(shifts[2].end) == apply_datetime + datetime.timedelta(days=14)
    else:
        assert localize(shifts[1].end) == apply_datetime - datetime.timedelta(days=1)
        assert localize(shifts[2].end) == apply_datetime
        assert localize(shifts[3].end) == apply_datetime + datetime.timedelta(days=7)
        assert localize(shifts[4].end) == apply_datetime + datetime.timedelta(days=14)


def test_revision_shifts_priority(scope_session, schedule_factory, revision_factory, interval_factory, slot_factory,
                                  staff_factory, composition_participants_factory):
    """
    Проверяем правильность последовательности шифтов с учетом ревизии и приоритетов слотов
    в первой ревизии приоритеты слотов - сначала 1, затем 2. т.к. во втором слоте больше людей
    во второй ревизии порядок слотов обратный
    """
    schedule = schedule_factory(threshold_day=datetime.timedelta(days=6))
    revision = revision_factory(
        schedule=schedule,
        state=enums.RevisionState.active,
        apply_datetime=now() - datetime.timedelta(minutes=5)
    )

    staff_1 = staff_factory()
    staff_2 = staff_factory()
    interval = interval_factory(schedule=schedule, duration=datetime.timedelta(days=2), revision=revision)
    slot_old_1 = slot_factory(interval=interval)
    slot_old_2 = slot_factory(interval=interval)
    composition_participants_factory(composition=slot_old_1.composition, staff=staff_1)
    composition_participants_factory(composition=slot_old_2.composition, staff=staff_1)
    composition_participants_factory(composition=slot_old_2.composition, staff=staff_2)

    with patch('watcher.tasks.people_allocation.start_people_allocation'):
        initial_creation_of_shifts(schedule.id)

    old_shifts = query_schedule_main_shifts(scope_session, schedule.id).order_by(Shift.slot_id).all()
    assert len(old_shifts) == 6
    next_id = None
    for shift in old_shifts:
        if next_id:
            assert shift.id == next_id
        next_id = shift.next_id

    revision_new = revision_factory(schedule=schedule, prev=revision)
    interval_new = interval_factory(
        schedule=schedule, duration=datetime.timedelta(days=2),
        revision=revision_new,
    )
    slot_new_1 = slot_factory(interval=interval_new)
    slot_new_2 = slot_factory(interval=interval_new)
    composition_participants_factory(composition=slot_new_1.composition, staff=staff_1)
    composition_participants_factory(composition=slot_new_1.composition, staff=staff_2)
    composition_participants_factory(composition=slot_new_2.composition, staff=staff_1)

    with patch('watcher.tasks.people_allocation.start_people_allocation.delay'):
        revision_shift_boundaries(schedule_id=schedule.id)

    new_shifts = query_schedule_main_shifts(scope_session, schedule.id).order_by(Shift.slot_id.desc()).all()
    # 2 старых и еще 3 * 2 шифтов новой ревизии
    assert len(new_shifts) == 8
    # меняем местами 2 первых шифта, т.к. сортируем выборку по убыванию slot_id
    new_shifts[0], new_shifts[1] = new_shifts[1], new_shifts[0]
    assert old_shifts[0].id == new_shifts[0].id
    assert old_shifts[1].id == new_shifts[1].id

    next_id = None
    for shift in new_shifts:
        if next_id:
            assert shift.id == next_id
        next_id = shift.next_id


@freeze_time('2021-03-01')
def test_revision_shift_boundaries_with_sub_shifts(
    scope_session, interval_factory, schedule_factory,
    revision_factory, slot_factory, holiday_factory,
):
    holiday_date = datetime.date(2021, 3, 8)
    holiday_factory(date=holiday_date)
    date_now = settings.DEFAULT_TIMEZONE.localize(datetime.datetime(2021, 3, 1))

    schedule = schedule_factory(threshold_day=datetime.timedelta(days=10))
    revision = revision_factory(
        schedule=schedule,
        state=enums.RevisionState.active,
        apply_datetime=date_now - datetime.timedelta(days=1)
    )
    interval = interval_factory(
        schedule=schedule,
        duration=datetime.timedelta(days=5),
        revision=revision,
        unexpected_holidays=enums.IntervalUnexpectedHolidays.remove
    )
    slot_factory(interval=interval)
    initial_creation_of_shifts(schedule.id)

    old_shifts = scope_session.query(Shift).all()
    assert len(old_shifts) == 6

    revision_2 = revision_factory(
        schedule=schedule,
        prev=revision,
        state=enums.RevisionState.active,
        apply_datetime=date_now + datetime.timedelta(days=1)
    )
    interval_2 = interval_factory(
        schedule=schedule,
        duration=datetime.timedelta(days=3),
        revision=revision_2,
        unexpected_holidays=enums.IntervalUnexpectedHolidays.remove
    )
    slot_factory(interval=interval_2)

    with patch('watcher.tasks.people_allocation.start_people_allocation.delay'):
        revision_shift_boundaries(schedule_id=schedule.id, revision_id=revision_2.id)

    new_shifts = query_schedule_main_shifts(scope_session, schedule.id).order_by(Shift.start).all()
    holiday_shift = new_shifts[3]
    assert len(holiday_shift.sub_shifts) == 2


@freeze_time('2022-03-14')
def test_revision_shift_boundaries_change_revision(
    scope_session, interval_factory, schedule_factory,
    revision_factory, slot_factory,
):
    date_now = now()
    schedule = schedule_factory(threshold_day=datetime.timedelta(days=20))
    revision = revision_factory(
        schedule=schedule,
        state=enums.RevisionState.active,
        apply_datetime=date_now - datetime.timedelta(days=1)
    )
    interval_1 = interval_factory(schedule=schedule, duration=datetime.timedelta(days=5), revision=revision)
    interval_2 = interval_factory(schedule=schedule, duration=datetime.timedelta(days=2), revision=revision,
                                  type_employment=enums.IntervalTypeEmployment.empty)
    slot_factory(interval=interval_1)
    slot_factory(interval=interval_2, composition=None)

    initial_creation_of_shifts(schedule.id)

    initial_revision_shift_dates = [
        (datetime.date(2022, 3, 13), datetime.date(2022, 3, 18), revision),  # 5 days
        (datetime.date(2022, 3, 18), datetime.date(2022, 3, 20), revision),  # 2 days
        (datetime.date(2022, 3, 20), datetime.date(2022, 3, 25), revision),  # 5 days
        (datetime.date(2022, 3, 25), datetime.date(2022, 3, 27), revision),  # 2 days
        (datetime.date(2022, 3, 27), datetime.date(2022, 4, 1), revision),  # 5 days
        (datetime.date(2022, 4, 1), datetime.date(2022, 4, 3), revision),  # 2 days
    ]
    initial_shifts = query_schedule_main_shifts(scope_session, schedule.id).all()
    assert initial_revision_shift_dates == [
        (shift.start.date(), shift.end.date(), shift.slot.interval.revision) for shift in initial_shifts
    ][:6]

    revision_2 = revision_factory(
        schedule=schedule,
        prev=revision,
        state=enums.RevisionState.active,
        apply_datetime=date_now + datetime.timedelta(days=8)
    )
    interval_2_1 = interval_factory(schedule=schedule, duration=datetime.timedelta(days=5), revision=revision_2)
    interval_2_2 = interval_factory(schedule=schedule, duration=datetime.timedelta(days=2), revision=revision_2,
                                    type_employment=enums.IntervalTypeEmployment.empty)
    slot_factory(interval=interval_2_1)
    slot_factory(interval=interval_2_2, composition=None)

    with patch('watcher.tasks.people_allocation.start_people_allocation.delay'):
        revision_shift_boundaries(schedule_id=schedule.id, revision_id=revision_2.id)

    new_revision_shift_dates = [
        (datetime.date(2022, 3, 13), datetime.date(2022, 3, 18), revision),  # 5 days
        (datetime.date(2022, 3, 18), datetime.date(2022, 3, 20), revision),  # 2 days
        (datetime.date(2022, 3, 20), datetime.date(2022, 3, 22), revision),  # 2 days, end of revision 1
        (datetime.date(2022, 3, 22), datetime.date(2022, 3, 27), revision_2),  # 5 days start of revision 2
        (datetime.date(2022, 3, 27), datetime.date(2022, 3, 29), revision_2),  # 2 days
        (datetime.date(2022, 3, 29), datetime.date(2022, 4, 3), revision_2),  # 5 days
    ]
    new_revision_shifts = query_schedule_main_shifts(scope_session, schedule.id).all()
    assert new_revision_shift_dates == [
        (shift.start.date(), shift.end.date(), shift.slot.interval.revision) for shift in new_revision_shifts
    ][:6]
