# coding: utf-8

from __future__ import unicode_literals

from easymeeting.core import rooms


RESOURCE_EVENTS_EXAMPLE = [
    {
        'eventId': 10000,
        'start': '2018-01-01T12:00:00',
        'end': '2018-01-01T13:00:00',
    },
    {
        'eventId': 10001,
        'start': '2018-01-01T16:00:00',
        'end': '2018-01-01T18:00:00',
    },
]


RESOURCE_EXAMPLE = {
    'info': {
        'id': 100,
        'name': '3.Гималаи',
        'email': 'conf_ekb_himalaya@yandex-team.ru',
    },
    'events': RESOURCE_EVENTS_EXAMPLE,
}


def test_room():
    room = rooms.Room.from_raw_resource(RESOURCE_EXAMPLE)
    assert room.id == 100
    assert room.display_name == '3.Гималаи'
    assert room.exchange_name == 'conf_ekb_himalaya'


def test_room_hashability():
    first = dict(id=100, display_name='3.Гималаи', exchange_name='hym')
    second = dict(id=101, display_name='Азия', exchange_name='dummy3')
    obj_one = rooms.Room(**first)
    obj_two_same = rooms.Room(**first)
    obj_three = rooms.Room(**second)

    obj_set = {obj_one, obj_two_same, obj_three}
    assert sorted(obj_set) == sorted([
        obj_one,
        obj_three
    ])
