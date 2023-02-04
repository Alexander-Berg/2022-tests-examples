import pytest

from datetime import date, datetime
from mock import patch

from intranet.trip.src.enums import PTStatus, TripStatus

pytestmark = pytest.mark.asyncio


class MockedDatetime(datetime):

    @classmethod
    def now(cls, *args, **kwargs):
        return datetime(2020, 3, 15)


async def test_close_completed_trips(f, uow):
    await f.create_person(person_id=1)

    await f.create_trip(trip_id=1, date_to=date(2020, 3, 12))

    await f.create_trip(trip_id=2, date_to=date(2020, 3, 13))
    await f.create_person_trip(trip_id=2, person_id=1, status=PTStatus.cancelled)

    await f.create_trip(trip_id=3, date_to=date(2020, 2, 15))
    await f.create_person_trip(trip_id=3, person_id=1, status=PTStatus.executed)

    await f.create_trip(trip_id=4, date_to=date(2020, 4, 15))
    await f.create_person_trip(trip_id=4, person_id=1, status=PTStatus.closed)

    await f.create_trip(trip_id=5, status=TripStatus.cancelled)

    with patch('intranet.trip.src.db.gateways.trip.datetime', MockedDatetime):
        updated = await uow.trips.close_completed_trips()
    trip_ids = {row['trip_id'] for row in updated}
    assert trip_ids == {1, 4}
