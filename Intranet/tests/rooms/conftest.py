from datetime import datetime, timedelta
import pytest

from django.test import Client

from django.conf import settings
from intranet.vconf.tests.call.factories import create_user
from intranet.vconf.src.rooms.models import Room

NOW = datetime.now()


@pytest.fixture
def rooms():
    for room_number in range(6):
        Room.objects.create(
            name='test',
            room_id=room_number,
            email='room{}@yandex-team.ru'.format(room_number),
            office_id='1',
            codec_ip='[2001:db8:0:0:0:0:0:{}]'.format(room_number),
            timezone='Europe/Moscow',
            language='ru',
        )
    return [Room.objects.get(room_id=room) for room in range(6)]


@pytest.fixture
def ya_client():
    create_user(username=settings.AUTH_TEST_USER)
    return Client()


@pytest.fixture
def events():
    return {
        '1234': {
            'rooms': ['room0@yandex-team.ru', 'room1@yandex-team.ru'],
            'start_time': datetime(2018, 9, 15, 15, 00),
            'end_time': datetime(2018, 9, 15, 15, 30),
            'organizer': 'some organizer',
            'name': 'first event',
        },
        '4321': {
            'rooms': ['room2@yandex-team.ru'],
            'start_time': datetime(2018, 9, 15, 15, 15),
            'end_time': datetime(2018, 9, 15, 15, 45),
            'organizer': 'some organizer',
            'name': 'second event',
        },
        '5678': {
            'rooms': ['room3@yandex-team.ru'],
            'start_time': datetime(2018, 9, 15, 15, 20),
            'end_time': datetime(2018, 9, 15, 15, 45),
            'organizer': 'some organizer',
            'name': 'third event',
        },
        '0005': {
            'rooms': ['room5@yandex-team.ru'],
            'start_time': NOW - timedelta(0, 3600),
            'end_time': NOW + timedelta(0, 330),
            'name': 'Встреча, которая скоро закончится',
            'organizer': 'Организатор',
        },
    }
