import datetime
import pretend
import pytest

from watcher import enums


@pytest.fixture
def schedule_data(schedule_factory, revision_factory, interval_factory, slot_factory, composition_participants_factory):
    schedule = schedule_factory(threshold_day=datetime.timedelta(days=5))
    revision = revision_factory(schedule=schedule, state=enums.RevisionState.active)

    interval_1 = interval_factory(
        schedule=schedule, duration=datetime.timedelta(days=5),
        revision=revision
    )
    slot_1 = slot_factory(interval=interval_1)

    interval_2 = interval_factory(
        schedule=schedule,
        duration=datetime.timedelta(days=2),
        type_employment=enums.IntervalTypeEmployment.empty,
        revision=revision,
    )
    slot_2 = slot_factory(interval=interval_2)

    return pretend.stub(
        schedule=schedule,
        revision=revision,
        interval_1=interval_1,
        slot_1=slot_1,
        interval_2=interval_2,
        slot_2=slot_2,
    )


@pytest.fixture
def schedule_data_with_composition(schedule_data, composition_participants_factory):
    slot_1 = schedule_data.slot_1
    composition_1 = slot_1.composition
    for _ in range(10):
        composition_participants_factory(composition=composition_1)

    return pretend.stub(
        schedule=schedule_data.schedule,
        revision=schedule_data.revision,
        interval_1=schedule_data.interval_1,
        slot_1=slot_1,
        interval_2=schedule_data.interval_2,
        slot_2=schedule_data.slot_2,
    )
