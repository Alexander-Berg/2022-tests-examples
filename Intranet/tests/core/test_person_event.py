# coding: utf-8

from __future__ import unicode_literals

from easymeeting.core import person_event


def test_get_login_from_email():
    actual = person_event.get_login_from_email('sibirev@yandex-team.ru')
    assert actual == 'sibirev'


def test_get_person_events():
    intervals = [
        {
            'availability': 'busy',
            'start': '2018-04-27T11:30:00',
            'end': '2018-04-27T12:00:00',
            'eventId': 26460406
        },
        {
            'availability': 'busy',
            'start': '2018-04-27T12:00:00',
            'end': '2018-04-27T13:10:00',
            'eventId': 14364929
        }
    ]
    actual = person_event.get_person_events(intervals)
    expected = [
        person_event.PersonEvent(
            availability='busy',
            start='2018-04-27T11:30:00',
            end='2018-04-27T12:00:00',
            eventId=26460406,
        ),
        person_event.PersonEvent(
            availability='busy',
            start='2018-04-27T12:00:00',
            end='2018-04-27T13:10:00',
            eventId=14364929,
        ),
    ]
    assert actual == expected
