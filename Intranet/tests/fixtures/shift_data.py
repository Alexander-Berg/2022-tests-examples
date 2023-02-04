import datetime
import pretend
import pytest

from watcher.logic.timezone import now


@pytest.fixture
def shift_sequence_data(shift_factory, schedule_factory, interval_factory, composition_factory, staff_factory,
                        member_factory, slot_factory, rating_factory, composition_participants_factory):
    def create_shift(**kwargs):
        return shift_factory(
            schedule=schedule,
            slot=slot,
            staff=staff_1,
            approved=True,
            **kwargs,
        )

    now_time = now()
    schedule = schedule_factory()
    service = schedule.service
    interval = interval_factory(schedule=schedule)
    composition = composition_factory(service=service)
    staff_1 = staff_factory()
    staff_2 = staff_factory()
    member_factory(staff=staff_1, service=service)
    member_factory(staff=staff_2, service=service)
    composition_participants_factory(staff=staff_1, composition=composition)
    composition_participants_factory(staff=staff_2, composition=composition)
    slot = slot_factory(composition=composition, interval=interval)

    prev_prev_shift = create_shift(
        start=now_time - datetime.timedelta(days=5),
        end=now_time - datetime.timedelta(days=3),
        predicted_ratings={staff_1.login: 0.0, staff_2.login: 0.0},
    )
    prev_shift = create_shift(
        start=now_time - datetime.timedelta(days=3),
        end=now_time - datetime.timedelta(days=1),
        predicted_ratings={staff_1.login: 48.0, staff_2.login: 0.0},
        prev=prev_prev_shift,
    )
    rating_factory(staff=staff_1, schedule=schedule, rating=96.0)
    shift = create_shift(
        start=now_time - datetime.timedelta(days=1),
        end=now_time + datetime.timedelta(days=1),
        predicted_ratings={staff_1.login: 96.0, staff_2.login: 0.0},
        prev=prev_shift,
    )
    next_shift = create_shift(
        start=now_time + datetime.timedelta(days=1),
        end=now_time + datetime.timedelta(days=3),
        predicted_ratings={staff_1.login: 144.0, staff_2.login: 0.0},
        prev=shift,
    )
    next_next_shift = create_shift(
        start=now_time + datetime.timedelta(days=3),
        end=now_time + datetime.timedelta(days=5),
        predicted_ratings={staff_1.login: 192.0, staff_2.login: 0.0},
        prev=next_shift,
    )

    return pretend.stub(
        service=service,
        schedule=schedule,
        slot=slot,
        prev_prev_shift=prev_prev_shift,
        prev_shift=prev_shift,
        shift=shift,
        next_shift=next_shift,
        next_next_shift=next_next_shift,
        staff_1=staff_1,
        staff_2=staff_2,
    )
