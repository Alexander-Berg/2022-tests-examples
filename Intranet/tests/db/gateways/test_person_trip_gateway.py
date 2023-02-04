import pytest
import asyncio

from datetime import date, datetime
from mock import patch

from intranet.trip.src.enums import PTStatus

pytestmark = pytest.mark.asyncio


class MockedDatetime(datetime):

    @classmethod
    def now(cls, *args, **kwargs):
        return datetime(2020, 3, 15)


async def test_close_completed_person_trips(f, uow):
    await f.create_person(person_id=1)
    await f.create_person(person_id=2)

    await f.create_trip(trip_id=1)
    await f.create_trip(trip_id=2)

    await f.create_person_trip(
        trip_id=1,
        person_id=1,
        status=PTStatus.executing,
        gap_date_to=date(2020, 2, 15),
    )
    await f.create_person_trip(
        trip_id=1,
        person_id=2,
        status=PTStatus.executed,
        gap_date_to=date(2020, 3, 11),
    )
    await f.create_person_trip(
        trip_id=2,
        person_id=1,
        status=PTStatus.executed,
        gap_date_to=date(2020, 3, 12),
    )
    await f.create_person_trip(
        trip_id=2,
        person_id=2,
        status=PTStatus.executed,
        gap_date_to=date(2020, 3, 13),
    )

    with patch('intranet.trip.src.db.gateways.person_trip.datetime', MockedDatetime):
        updated = await uow.person_trips.close_completed_person_trips()
    pairs = {(row['trip_id'], row['person_id']) for row in updated}
    assert pairs == {(1, 2), (2, 1)}


async def test_get_person_trips_for_aeroclub_create(f, uow):
    async def get_and_update_person_trip(
        new_journey_id: int,
        new_trip_id: int,
        sleep_before: float = 0,
    ):
        async with uow:
            await asyncio.sleep(sleep_before)
            person_trip = await uow.person_trips.get_person_trip_for_aeroclub_create(1, 1)
            if person_trip is not None:
                await uow.person_trips.update(
                    trip_id=person_trip.trip_id,
                    person_id=person_trip.person_id,
                    aeroclub_journey_id=new_journey_id,
                    aeroclub_trip_id=new_trip_id,
                )
            await asyncio.sleep(1)

    await f.create_person(person_id=1)
    await f.create_trip(
        trip_id=1,
        provider_city_from_id=1,
        provider_city_to_id=2,
    )
    await f.create_person_trip(
        trip_id=1,
        person_id=1,
        status=PTStatus.new,
        gap_date_to=date(2020, 3, 13),
        aeroclub_journey_id=None,
        aeroclub_trip_id=None,
    )

    person_trips_to_create = await uow.person_trips.get_person_trip_ids_for_aeroclub_create(1, [1])
    assert len(person_trips_to_create) == 1
    trip_id, person_id = person_trips_to_create[0]
    assert trip_id == 1
    assert person_id == 1
    await asyncio.gather(
        get_and_update_person_trip(new_journey_id=1, new_trip_id=1),
        get_and_update_person_trip(new_journey_id=2, new_trip_id=2, sleep_before=0.5),
    )

    person_trip = await uow.person_trips.get_person_trip(1, 1)
    assert person_trip.aeroclub_journey_id == 1
    assert person_trip.aeroclub_trip_id == 1
