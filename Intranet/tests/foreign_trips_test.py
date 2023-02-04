import pytest
from itertools import chain

from staff.trip_questionary.models import SNGCountry, TripQuestionaryCollection, EVENT_TYPE
from staff.lib.utils.date import parse_datetime


@pytest.fixture
def sng_country_names():
    russia_names = ['россия', 'российскаяфедерация', 'рф', 'rf']
    ukraine_names = ['украина', 'ukraine', 'ua', 'незалежна']
    SNGCountry.objects.bulk_create([
        SNGCountry(name='Россия', spellings='\n'.join(russia_names)),
        SNGCountry(name='Украина', spellings='\n'.join(ukraine_names)),
    ])
    return list(chain(russia_names, ukraine_names))


def citylist_entry(**kwargs):
    entry = {
        "fare": "most_economical",
        "city": "Москва",
        "time_proposal": "",
        "ready_to_upgrade": False,
        "need_hotel": True,
        "country": "Россия",
        "hotel": "",
        "comment": "",
        "is_return_route": False,
        "baggage": "hand",
        "tickets_cost": None,
        "upgrade_comment": "",
        "city_arrive_date_type": "departure",
        "departure_date": parse_datetime('2019-06-18T18:22:13.967'),
        "car_rent": False,
        "delayed_operations": [],
        "tickets_cost_currency": "RUB",
        "has_tickets": False,
        "transport": "aircraft"
    }
    entry.update(kwargs)
    return entry


@pytest.mark.django_db
def test_foreign_trip(company, sng_country_names):

    person = company.persons['dep11-person']

    trip = TripQuestionaryCollection().new(author=person)
    trip.data['event_type'] = 'trip'
    trip_city_list = [
        citylist_entry(country=sng_country_names[0]),
        citylist_entry(country=sng_country_names[2]),
        citylist_entry(country=sng_country_names[-2]),
    ]
    trip.data['city_list'] = trip_city_list
    assert not trip.is_foreign()

    trip.data['city_list'].append(citylist_entry(country='notSNG_country_name'))
    trip._is_foreign = None
    assert trip.is_foreign()

    trip.data['city_list'][-1]['is_return_route'] = True
    trip._is_foreign = None
    assert not trip.is_foreign()

    trip.data['city_list'][-1]['is_return_route'] = False
    trip.data['event_type'] = EVENT_TYPE.CONF
    trip._is_foreign = None
    assert not trip.is_foreign()
