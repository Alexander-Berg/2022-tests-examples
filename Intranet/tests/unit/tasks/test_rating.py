import datetime

from watcher.tasks.rating import remove_obsolete_ratings
from watcher.logic.timezone import now
from watcher.db import Rating


def test_remove_obsolete_ratings(
    schedule_factory, staff_factory, composition_participants_factory,
    composition_factory, shift_factory, rating_factory, scope_session,
):
    schedule = schedule_factory()

    #  есть смена недавняя - не должны удалять
    rating_to_stay_recent_shift = rating_factory(schedule=schedule)
    shift_factory(staff=rating_to_stay_recent_shift.staff, schedule=schedule)

    #  состоит в составе - не должны удалять
    rating_to_stay_in_composition = rating_factory(schedule=schedule)
    composition = composition_factory(service=schedule.service)
    composition_participants_factory(
        composition=composition,
        staff=rating_to_stay_in_composition.staff,
    )

    #  нет недавних смен и в составе не состоит - удаляем
    rating_to_remove_no_recent_shift = rating_factory(schedule=schedule)
    shift_factory(
        schedule=schedule,
        staff=rating_to_remove_no_recent_shift.staff,
        start=now() - datetime.timedelta(weeks=26),
        end=now() - datetime.timedelta(weeks=25)
    )

    # есть недавняя смена, но в другом сервисе - удаляем
    rating_to_remove_no_shift_in_service = rating_factory(schedule=schedule)
    shift_factory(staff=rating_to_remove_no_shift_in_service.staff)

    # есть недавняя смена, но в другом расписании - удаляем
    rating_to_remove_no_shift_in_schedule= rating_factory(schedule=schedule)
    shift_factory(
        staff=rating_to_remove_no_shift_in_schedule.staff,
        schedule=schedule_factory(service=schedule.service)
    )

    remove_obsolete_ratings()
    assert {obj.id for obj in scope_session.query(Rating.id).all()} == {
        rating_to_stay_in_composition.id,
        rating_to_stay_recent_shift.id,
    }
