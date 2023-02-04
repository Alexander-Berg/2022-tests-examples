import copy
import pytest
from staff.trip_questionary.models import TripQuestionaryCollection

from staff.lib.utils.date import parse_datetime


@pytest.fixture
def company(company_with_module_scope):
    return company_with_module_scope


# урезанный шаблон командировки на конференцию
template = {
    'comment': '',
    'event_type': 'trip_conf',
    'notificated_chiefs': {'6881': ['6903']},
    'event_name': 'Презентация Эпл',
    'creation_time': parse_datetime('2018-03-01T14:16:26'),
    'event_date_from': parse_datetime('2018-03-01T00:00:00'),
    'event_date_to': parse_datetime('2018-03-02T00:00:00'),
    'trip_date_from': parse_datetime('2018-03-01T00:00:00'),
    'trip_date_to': parse_datetime('2018-03-02T00:00:00'),
    'is_new': False,
    'city_list': [
        {
            'city': 'Санкт-Петербург',
            'country': 'Россия',
            'departure_date': '2018-03-01T00:00:00',
            'comment': '',
            'is_return_route': False,
            'need_hotel': True,
            'has_tickets': False,
            'city_arrive_date_type': 'departure'
        },
        {
            'city': 'Париж',
            'country': 'Франция',
            'departure_date': '2018-03-01T00:00:00',
            'comment': '',
            'is_return_route': False,
            'need_hotel': True,
            'has_tickets': False,
            'city_arrive_date_type': 'departure'
        },
        {
            'city': 'Санкт-Петербург',
            'country': 'Россия',
            'departure_date': '2018-03-01T00:00:00',
            'comment': '',
            'is_return_route': True,
            'need_hotel': False,
            'has_tickets': False,
            'city_arrive_date_type': 'departure'
        }
    ],
    'event_cost': '12345',
    'purpose': [4],
    'is_issue_link_created': True,
    'receiver_side': '',
    'employee_list': [
        {
            'comment': '',
            'need_taxi': False,
            'trip_info': '',
            'mobile_date_from': None,
            'has_holidays': False,
            'passport_number': 'HB123654',
            'conf_issue': {
                'conference': 'lkjhgfd',
                'key': 'INTERCONF-5779',
                'purposeOfTrip1': 'Корпоративное мероприятие (напр., тимбилдинг)',
                'self': 'https://st-api.test.yandex-team.ru/v2/issues/INTERCONF-5779',
                'cities': 'Санкт-Петербург – Париж – Санкт-Петербург',
                'id': '5a97e10d373483001bf2d6ff',
                'createdAt': '2018-03-01T14:16:28'
            },
            'mobile_date_to': None,
            'employee': '6903',
            'trip_issue': {
                'purposeOfTrip1': 'Корпоративное мероприятие (напр., тимбилдинг)',
                'self': 'https://st-api.test.yandex-team.ru/v2/issues/TRAVEL-38063',
                'createdAt': '2018-03-01T14:16:30',
                'department': 'Поисковый портал',
                'countryTo': 'Франция',
                'purpose': '',
                'key': 'TRAVEL-38063',
                'cityTo': 'Париж',
                'countryFrom': 'Россия',
            }
        }
    ],
    'uuid': 'ec9d728e-bb64-458d-9a14-ea919445307e',
    'author': '1715',
    'is_locked': False,
    'conf_issue': {
        'conference': 'lkjhgfd',
        'key': 'INTERCONF-5779',
        'purposeOfTrip1': 'Корпоративное мероприятие (напр., тимбилдинг)',
        'self': 'https://st-api.test.yandex-team.ru/v2/issues/INTERCONF-5779',
        'start': '2018-03-01',
        'cities': 'Санкт-Петербург – Париж – Санкт-Петербург',
        'id': '5a97e10d373483001bf2d6ff',
        'createdAt': '2018-03-01T14:16:28'
    },
    'objective': '',
    'trip_issue': {
        'startDate': '2018-03-01T00:00:00',
        'endDate': '2018-03-01T00:00:00',
        'assignmentID': 463411,
        'id': '5a97e10e687298001b132a93',
        'purposeOfTrip1': 'Корпоративное мероприятие (напр., тимбилдинг)',
        'self': 'https://st-api.test.yandex-team.ru/v2/issues/TRAVEL-38063',
        'createdAt': '2018-03-01T14:16:30',
        'department': 'Поисковый портал',
        'countryTo': 'Франция',
        'hotelNeeded': 'Да',
        'purpose': '',
        'key': 'TRAVEL-38063',
        'cityTo': 'Париж',
        'countryFrom': 'Россия',
        'itinerary': 'Санкт-Петербург – Париж – Санкт-Петербург',
        'cityFrom': 'Санкт-Петербург',
    },
}


@pytest.fixture
def create_trips(company, mocked_mongo):
    collection = mocked_mongo.db[TripQuestionaryCollection.collection_name]

    trip_conf = copy.deepcopy(template)
    trip_conf['event_type'] = 'trip_conf'
    trip_conf['creation_time'] = parse_datetime('2018-01-01T00:00:00')
    trip_conf['trip_date_from'] = parse_datetime('2018-01-01T00:00:00')
    trip_conf['trip_date_to'] = parse_datetime('2018-01-10T00:00:00')
    trip_conf['event_date_from'] = parse_datetime('2018-01-03T00:00:00')
    trip_conf['event_date_to'] = parse_datetime('2018-01-09T00:00:00')

    trip_conf['employee_list'][0]['employee'] = str(company.persons['dep1-person'].id)
    collection.insert_one(trip_conf)

    trip = copy.deepcopy(template)
    trip.pop('conf_issue')
    trip['event_type'] = 'trip'
    trip['creation_time'] = parse_datetime('2018-02-01T00:00:00')
    trip['trip_date_from'] = parse_datetime('2018-02-01T00:00:00')
    trip['trip_date_to'] = parse_datetime('2018-02-10T00:00:00')
    trip.pop('event_date_from')
    trip.pop('event_date_to')

    trip['employee_list'][0]['employee'] = str(company.persons['dep2-person'].id)
    collection.insert_one(trip)

    conf = copy.deepcopy(template)
    conf.pop('trip_issue')
    conf['event_type'] = 'conf'
    conf['creation_time'] = parse_datetime('2018-03-01T00:00:00')
    conf['event_date_from'] = parse_datetime('2018-03-03T00:00:00')
    conf['event_date_to'] = parse_datetime('2018-03-09T00:00:00')
    conf.pop('trip_date_from')
    conf.pop('trip_date_to')

    conf['employee_list'][0]['employee'] = str(company.persons['dep11-person'].id)
    collection.insert_one(conf)
