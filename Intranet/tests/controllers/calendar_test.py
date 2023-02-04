import mock
import pytest

from datetime import datetime, date
import pytz

from staff.lib.testing import StaffFactory, OfficeFactory, CityFactory

utc_tz = pytz.timezone('UTC')


@pytest.mark.django_db
def test_calendar_holidays():
    from staff.person_profile.controllers.calendar import PersonHolidays
    geo_id = 333
    city = CityFactory(geo_id=geo_id)
    office = OfficeFactory(city=city)
    person = StaffFactory(office=office)
    data_mock = mock.Mock(**{'json.return_value': holidays_data})
    first_day = date(2018, 1, 1)
    last_day = date(2018, 1, 31)

    with mock.patch('staff.person_profile.controllers.calendar._ask_calendar', return_value=data_mock) as response:
        result = PersonHolidays(person=person, first_day=first_day, last_day=last_day).get_as_dict()
        response.assert_called_once_with(
            handle='get-holidays',
            params={
                'from': first_day.isoformat(),
                'to': last_day.isoformat(),
                'for': geo_id,
            }
        )
        assert result == holidays_result


def test_calendar():
    from staff.person_profile.controllers.calendar import (
        hydrate_events,
        fetch_events_from_calendar,
    )

    date_from = datetime(2017, 11, 13, 0, 0, tzinfo=utc_tz)
    date_to = datetime(2017, 11, 14, 0, 0, tzinfo=utc_tz)

    path = 'staff.lib.requests.Session.get'
    with mock.patch(path, return_value=mock.Mock(**{'json.return_value': data})) as r_get_mock:
        with mock.patch('staff.lib.tvm2.get_user_ticket', return_value='user_ticket_mock_abracadabra'):
            with mock.patch('staff.lib.tvm2.get_tvm_ticket_by_deploy', return_value='service_ticket_mock_abracadabra'):
                events = fetch_events_from_calendar(
                    observer_uid='111',
                    observer_tvm_ticket=None,
                    target_email='eee@y-t.ru',
                    date_from=date_from,
                    date_to=date_to,
                )

                assert len(events) == 5
                assert r_get_mock.call_args[1]['params'] == {
                    'from': '2017-11-13T00:00:00+0000',
                    'to': '2017-11-14T00:00:00+0000',
                    'uid': '111',
                    'emails': 'eee@y-t.ru',
                    'dateFormat': 'zoned',
                    'display': 'events',
                    'shape': 'omit-participants',
                }

    rooms = {
        'conf_spb_6_9': {
            'name': 'Аврора',
            'name_exchange': 'conf_spb_6_9',
            'voice_phone': '3486',
            'video_phone': '3486',
        },
        'conf_sim_zoo': {
            'name': 'Зоопарк',
            'name_exchange': 'conf_sim_zoo',
            'voice_phone': '4444',
            'video_phone': '4444',
        },
        'conf_spb_4_2': {
            'name': 'Белые ночи',
            'name_exchange': 'conf_spb_4_2',
            'voice_phone': '',
            'video_phone': '',

        },
    }
    method_path = 'staff.person_profile.controllers.calendar.get_names_from_rooms'
    with mock.patch(method_path, return_value=rooms):
        result = list(hydrate_events(events))

    assert len(result) == 5

    for key in ('type', 'from', 'to', 'fullDay', 'meta'):
        assert key in result[0]

    for key in ('id', 'name', 'conference_rooms'):
        assert key in result[0]['meta']

    for key in ('name', 'name_exchange', 'voice_phone', 'video_phone'):
        assert key in result[0]['meta']['conference_rooms'][0]

    for event, two_date in zip(result, dates):
        assert event['from'], event['to'] == two_date

    assert result[3]['from'] == result[3]['to']


data = {
    "subjectAvailabilities": [
        {
            "email": "egorova@yandex-team.ru",
            "status": "ok",
            "intervals": [
                {
                    "availability": "busy",
                    "eventType": "user",
                    "start": "2017-11-03T12:00:00+03:00",
                    "end": "2017-11-03T17:00:00+03:00",
                    "isAllDay": False,
                    "instanceStart": "2017-11-03T12:00:00",
                    "eventId": 9012,
                    "eventName": "тест тест тест тест тест тест тест тест тест тест тест тест тест тест тест тест ",
                    "resources": [
                        {
                            "name": "Аврора",
                            "nameAlternative": "Бенуа 4-2, 3486",
                            "type": "room",
                            "email": "conf_spb_4_2@yandex-team.ru"
                        },
                        {
                            "name": "Зоопарк",
                            "nameAlternative": "403",
                            "type": "room",
                            "email": "conf_sim_zoo@yandex-team.ru"
                        }
                    ]
                },
                {
                    "availability": "busy",
                    "eventType": "user",
                    "start": "2017-11-08T10:00:00+03:00",
                    "end": "2017-11-08T13:00:00+03:00",
                    "isAllDay": False,
                    "instanceStart": "2017-11-08T10:00:00",
                    "eventId": 9137,
                    "eventName": "ТЕМА",
                    "resources": [
                        {
                            "name": "Белые ночи",
                            "type": "room",
                            "email": "conf_spb_6_9@yandex-team.ru"
                        },
                    ]
                },
                {
                    "availability": "busy",
                    "eventType": "user",
                    "start": "2017-11-08T12:30:00+03:00",
                    "end": "2017-11-08T15:00:00+03:00",
                    "isAllDay": False,
                    "instanceStart": "2017-11-08T12:30:00",
                    "eventId": 9143,
                    "eventName": "12345",
                    "resources": []
                },
                {
                    "availability": "busy",
                    "eventType": "user",
                    "start": "2017-11-08T00:00:00+03:00",
                    "end": "2017-11-09T00:00:00+03:00",
                    "isAllDay": True,
                    "instanceStart": "2017-11-08T00:00:00",
                    "eventId": 9155,
                    "eventName": "День без встреч",
                    "resources": []
                },
                {
                    "start": "2017-11-08T00:00:00+03:00",
                    "end": "2017-11-09T00:00:00+03:00",
                    "eventId": 9166,
                    "resources": [],
                },
            ]
        }
    ]
}


dates = [
    ('2017-11-03T09:00:00', '2017-11-03T14:00:00'),
    ('2017-11-08T07:00:00', '2017-11-08T10:00:00'),
    ('2017-11-08T09:30:00', '2017-11-08T12:00:00'),
    ('2017-11-08T00:00:00', '2017-11-08T00:00:00'),
    ('2017-11-07T21:00:00', '2017-11-08T21:00:00'),
]


holidays_data = {
    "holidays": [
        {
            "date": "2018-01-01",
            "type": "holiday",
            "name": "Новогодние каникулы"
        },
        {
            "date": "2018-01-02",
            "type": "holiday",
            "name": "Новогодние каникулы"
        },
        {
            "date": "2018-01-03",
            "type": "holiday",
            "name": "Новогодние каникулы"
        },
        {
            "date": "2018-01-04",
            "type": "holiday",
            "name": "Новогодние каникулы"
        },
        {
            "date": "2018-01-05",
            "type": "holiday",
            "name": "Новогодние каникулы"
        },
        {
            "date": "2018-01-06",
            "type": "holiday",
            "name": "Новогодние каникулы"
        },
        {
            "date": "2018-01-07",
            "type": "holiday",
            "name": "Рождество Христово"
        },
        {
            "date": "2018-01-08",
            "type": "holiday",
            "name": "Новогодние каникулы"
        },
        {
            "date": "2018-01-12",
            "type": "weekday",
            "name": "День работника прокуратуры РФ"
        },
        {
            "date": "2018-01-13",
            "type": "weekend",
            "name": "День российской печати"
        },
        {
            "date": "2018-01-14",
            "type": "weekend",
            "name": "Старый Новый год"
        },
        {
            "date": "2018-01-20",
            "type": "weekend"
        },
        {
            "date": "2018-01-21",
            "type": "weekend",
            "name": "День инженерных войск"
        },
        {
            "date": "2018-01-25",
            "type": "weekday",
            "name": "День российского студенчества"
        },
        {
            "date": "2018-01-27",
            "type": "weekend",
            "name": "День воинской славы России: снятие Ленинградской блокады"
        },
        {
            "date": "2018-01-28",
            "type": "weekend"
        }
    ]
}

holidays_result = {
    "2018-01-01": {
        "type": "holiday",
        "description": "Новогодние каникулы"
    },
    "2018-01-02": {
        "type": "holiday",
        "description": "Новогодние каникулы"
    },
    "2018-01-03": {
        "type": "holiday",
        "description": "Новогодние каникулы"
    },
    "2018-01-04": {
        "type": "holiday",
        "description": "Новогодние каникулы"
    },
    "2018-01-05": {
        "type": "holiday",
        "description": "Новогодние каникулы"
    },
    "2018-01-06": {
        "type": "holiday",
        "description": "Новогодние каникулы"
    },
    "2018-01-07": {
        "type": "holiday",
        "description": "Рождество Христово"
    },
    "2018-01-08": {
        "type": "holiday",
        "description": "Новогодние каникулы"
    },
    "2018-01-12": {
        "type": "weekday",
        "description": "День работника прокуратуры РФ"
    },
    "2018-01-13": {
        "type": "weekend",
        "description": "День российской печати"
    },
    "2018-01-14": {
        "type": "weekend",
        "description": "Старый Новый год"
    },
    "2018-01-20": {
        "type": "weekend",
        "description": ""
    },
    "2018-01-21": {
        "type": "weekend",
        "description": "День инженерных войск"
    },
    "2018-01-25": {
        "type": "weekday",
        "description": "День российского студенчества"
    },
    "2018-01-27": {
        "type": "weekend",
        "description": "День воинской славы России: снятие Ленинградской блокады"
    },
    "2018-01-28": {
        "type": "weekend",
        "description": "",
    }
}
