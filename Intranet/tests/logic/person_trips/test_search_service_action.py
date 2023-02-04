import pytest

from intranet.trip.src.enums import PTStatus
from intranet.trip.src.logic.person_trips import get_person_trip_actions


pytestmark = pytest.mark.asyncio


@pytest.mark.parametrize('status, actions_search_service', (
    (PTStatus.draft, True),
    (PTStatus.new, True),
    (PTStatus.verification, True),
    (PTStatus.executing, True),
    (PTStatus.executed, True),
    (PTStatus.closed, False),
    (PTStatus.cancelled, False),
))
async def test_search_service_allowed_not_conference(f, uow, status, actions_search_service):
    await f.create_person(person_id=1)
    user = await uow.persons.get_user(person_id=1)
    await f.create_trip(trip_id=1, author_id=1)
    await f.create_person_trip(trip_id=1, person_id=1, status=status)
    person_trip = await uow.person_trips.get_detailed_person_trip(1, 1)

    actions = await get_person_trip_actions(
        uow=uow,
        user=user,
        person_trip=person_trip,
        roles_map=None,
    )
    assert actions.search_service is actions_search_service


@pytest.mark.parametrize('is_another_city', (True, False))
async def test_search_service_conference_other_city(f, uow, is_another_city):
    await f.create_person(person_id=1)
    user = await uow.persons.get_user(person_id=1)
    await f.create_trip(trip_id=1, author_id=1)
    await f.create_person_trip(trip_id=1, person_id=1)
    await f.create_person_conf_details(trip_id=1, person_id=1, is_another_city=is_another_city)
    person_trip = await uow.person_trips.get_detailed_person_trip(1, 1)

    actions = await get_person_trip_actions(
        uow=uow,
        user=user,
        person_trip=person_trip,
        roles_map=None,
    )
    assert actions.search_service == is_another_city
