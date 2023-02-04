import pytest
from typing import Any

from pydantic import ValidationError

from intranet.trip.src.api.schemas import TripCreate, TripUpdate
from intranet.trip.src.enums import ConferenceParticiationType, Citizenship
from intranet.trip.src.logic.person_trips import get_trip_holding_id
from ..factories import date_from_str, date_to_str, date_from, date_to


pytestmark = pytest.mark.asyncio


base_fields: dict[str, Any] = {
    'city_from': 'Москва',
    'city_to': 'Питер',
    'date_from': date_from_str,
    'date_to': date_to_str,
    'description': 'Описание',
    'with_days_off': True,
    'comment': 'Комментарий',
    'provider_city_from_id': 1,
    'provider_city_to_id': 2,
    'conf_details': None,
}


async def get_action(f, uow, trip_id=None):
    from intranet.trip.src.logic.trips import TripCreateUpdateAction

    await f.create_person(person_id=1)
    user = await uow.persons.get_user(person_id=1)
    action = await TripCreateUpdateAction.init(
        uow=uow,
        user=user,
        trip_id=trip_id,
    )
    return action


async def test_trip_create_wrong_purpose(f, uow):
    trip_create = TripCreate(
        purposes=[999],
        person_trips=[{
            'person_uid': '1',
        }],
        **base_fields
    )
    action = await get_action(f, uow)
    with pytest.raises(ValidationError):
        await action.execute(trip=trip_create)


async def _get_trip_create(f, is_offline=False, is_belarus=False, holding_id=1):
    await f.create_holding(holding_id=holding_id)
    await f.create_purpose(purpose_id=1)
    await f.create_company(company_id=1, holding_id=holding_id)
    await f.create_person(person_id=1, company_id=1)
    aeroclub_company_id = None if is_offline else 2
    country = Citizenship.BY if is_belarus else Citizenship.RU
    await f.create_company(
        company_id=2,
        holding_id=holding_id,
        aeroclub_company_id=aeroclub_company_id,
        country=country,
    )
    await f.create_person(person_id=2, company_id=2)
    fields = dict(
        base_fields,
        person_trips=[
            {'person_uid': '1'},
            {'person_uid': '2'},
        ],
        purposes=[1],
        conf_details=dict(
            conf_date_from=date_from_str,
            conf_date_to=date_to_str,
            conference_name='name',
            price='1000',
            is_another_city=True,
        )
    )
    return TripCreate(**fields)


async def test_trip_create(f, uow):
    trip_create = await _get_trip_create(f)
    action = await get_action(f, uow)
    trip_id = await action.execute(trip=trip_create)
    trip = await uow.trips.get_detailed_trip(trip_id)
    assert trip.city_from == 'Москва'
    assert trip.city_to == 'Питер'
    assert trip.date_from == date_from
    assert trip.date_to == date_to
    assert trip.conf_details is not None
    assert len(trip.person_trips) == 2
    assert len(trip.purposes) == 1
    assert trip.conf_details.is_another_city is True

    person_trip = await uow.person_trips.get_detailed_person_trip(trip_id, person_id=2)
    assert person_trip.person.uid == '2'
    assert len(person_trip.purposes) == 1
    assert person_trip.conf_details is not None
    assert person_trip.conf_details.is_another_city is True
    assert person_trip.is_offline is False


async def test_trip_create_with_person_trips(f, uow):
    await f.create_purpose(purpose_id=1)
    await f.create_person(person_id=1)
    await f.create_person(person_id=2)
    fields = dict(
        base_fields,
        person_trips=[
            {
                'person_uid': '1',
                'conf_details': {
                    'role': ConferenceParticiationType.speaker,
                    'badge_position': 'Position',
                    'badge_name': 'Name',
                },
                'description': 'description',
            },
            {
                'person_uid': '2',
            },
        ],
        purposes=[1],
        conf_details=dict(
            conf_date_from=date_from_str,
            conf_date_to=date_to_str,
            conference_name='name',
            price='1000',
            is_another_city=True,
        )
    )
    trip_create = TripCreate(**fields)
    action = await get_action(f, uow)
    trip_id = await action.execute(trip=trip_create)

    person_trip_1 = await uow.person_trips.get_detailed_person_trip(trip_id, person_id=1)
    assert person_trip_1.person.uid == '1'
    assert person_trip_1.conf_details is not None
    assert person_trip_1.conf_details.is_another_city is True
    assert person_trip_1.conf_details.role == ConferenceParticiationType.speaker
    assert person_trip_1.conf_details.badge_position == 'Position'
    assert person_trip_1.conf_details.badge_name == 'Name'
    assert person_trip_1.description == 'description'

    person_trip_2 = await uow.person_trips.get_detailed_person_trip(trip_id, person_id=2)
    assert person_trip_2.person.uid == '2'
    assert person_trip_2.conf_details is not None
    assert person_trip_1.conf_details.is_another_city is True
    assert person_trip_2.conf_details.role == ConferenceParticiationType.listener
    assert person_trip_2.conf_details.badge_position is None
    assert person_trip_2.conf_details.badge_name is None


async def test_trip_create_with_offline_person_trip(f, uow):
    trip_create = await _get_trip_create(f, is_offline=True)
    action = await get_action(f, uow)

    trip_id = await action.execute(trip=trip_create)
    person_trip_1 = await uow.person_trips.get_detailed_person_trip(trip_id, person_id=1)
    person_trip_2 = await uow.person_trips.get_detailed_person_trip(trip_id, person_id=2)

    assert person_trip_1.is_offline is False
    assert person_trip_2.is_offline is True


async def test_trip_update(f, uow):
    await f.create_purpose(purpose_id=1)
    await f.create_purpose(purpose_id=2)
    await f.create_person(person_id=1)
    await f.create_trip(trip_id=1, person_ids=[1], purpose_ids=[1])
    fields = dict(
        base_fields,
        city_from='Питер',
        city_to='Москва',
        purposes=[2],
    )
    trip_update = TripUpdate(**fields)
    action = await get_action(f, uow, trip_id=1)
    trip_id = await action.execute(trip=trip_update)
    trip = await uow.trips.get_detailed_trip(trip_id)
    assert trip.city_from == 'Питер'
    assert trip.city_to == 'Москва'
    assert len(trip.purposes) == 1
    assert trip.purposes[0].purpose_id == 2
    assert len(trip.person_trips) == 1


async def test_get_holding_id_for_trip(f, uow):
    trip_create = await _get_trip_create(f, holding_id=99)
    action = await get_action(f, uow)
    trip_id = await action.execute(trip=trip_create)
    holding_id = await get_trip_holding_id(uow=uow, trip_id=trip_id)
    assert holding_id == 99
    holding_id = await get_trip_holding_id(uow=uow, trip_id=-trip_id)
    assert holding_id is None
