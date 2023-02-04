import pytest

from intranet.trip.src.api.schemas import PersonTripCancel
from intranet.trip.src.enums import TripStatus, PTStatus
from intranet.trip.src.logic.person_trips import CancelAction


pytestmark = pytest.mark.asyncio


person_trip_cancel = PersonTripCancel(cancellation_reason='some reason')


@pytest.fixture
async def user(f, uow):
    await f.create_person(person_id=1)
    user = await uow.persons.get_user(person_id=1)
    return user


async def test_cancel_not_last_person_trip(f, uow, user):
    await f.create_person(person_id=2)
    await f.create_trip(trip_id=1, author_id=1, status=TripStatus.new)
    await f.create_person_trip(trip_id=1, person_id=1, status=PTStatus.new)
    await f.create_person_trip(trip_id=1, person_id=2, status=PTStatus.new)

    action = await CancelAction.init(uow, user=user, trip_id=1, person_id=1)
    await action.execute(person_trip_cancel)

    trip = await uow.trips.get_trip(trip_id=1)
    person_trip_1 = await uow.person_trips.get_person_trip(trip_id=1, person_id=1)
    person_trip_2 = await uow.person_trips.get_person_trip(trip_id=1, person_id=2)

    assert trip.status == TripStatus.new
    assert person_trip_1.status == PTStatus.cancelled
    assert person_trip_2.status == PTStatus.new


async def test_cancel_last_person_trip(f, uow, user):
    await f.create_person(person_id=2)
    await f.create_trip(trip_id=1, author_id=1, status=TripStatus.new)
    await f.create_person_trip(trip_id=1, person_id=1, status=PTStatus.new)
    await f.create_person_trip(trip_id=1, person_id=2, status=PTStatus.cancelled)

    action = await CancelAction.init(uow, user=user, trip_id=1, person_id=1)
    await action.execute(person_trip_cancel)

    trip = await uow.trips.get_trip(trip_id=1)
    person_trip_1 = await uow.person_trips.get_person_trip(trip_id=1, person_id=1)
    person_trip_2 = await uow.person_trips.get_person_trip(trip_id=1, person_id=2)

    assert trip.status == TripStatus.cancelled
    assert person_trip_1.status == PTStatus.cancelled
    assert person_trip_2.status == PTStatus.cancelled
