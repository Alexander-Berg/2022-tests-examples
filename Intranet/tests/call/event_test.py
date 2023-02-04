import pytest

from intranet.vconf.src.call.event import (
    user_is_event_participant,
    update_or_create_event,
    find_zoom_link,
)
from intranet.vconf.tests.call.factories import create_user, EventFactory
from intranet.vconf.tests.call.mock import get_next_event_mock


pytestmark = pytest.mark.django_db


event1 = None
event2 = {}
event3 = {
    'id': 1,
    'attendees': [
        {'login': 'user1'},
        {'login': 'user2'},
    ],
    'ogranizer': {},
}
event4 = {
    'id': 2,
    'attendees': [],
    'organizer': {'login': 'user'},
}
event5 = {
    'attendees': [
        {'login': 'user'},
    ],
    'organizer': {'login': 'user1'},
}


@pytest.mark.parametrize('event, result', [
    (event1, False),
    (event2, False),
    (event3, False),
    (event4, True),
    (event5, True),
])
def test_user_is_event_participant(event, result):
    user = create_user(username='user')
    assert user_is_event_participant(user, event) is result


def test_create_event():
    event_data = get_next_event_mock()
    event = update_or_create_event(event_data)
    assert event.secret


def test_update_event():
    event = EventFactory(id=1)
    event_data = get_next_event_mock(event_id=event.id, master_id=event.id)
    event_data['name'] = 'New Name'
    updated_event = update_or_create_event(event_data)
    assert event.secret == updated_event.secret
    assert updated_event.name == event_data['name']


def test_update_event_regenerate_secret():
    event = EventFactory(id=1)
    event_data = get_next_event_mock(event_id=event.id, master_id=event.id)
    updated_event = update_or_create_event(event_data, regenerate_secret=True)
    assert updated_event.secret != event.secret


@pytest.mark.parametrize('text, result', [
    ('', None),
    ('https://not_yandex.zoom.us/j/123', None),
    ('https://yandex.zoom.us/j/123', 'https://yandex.zoom.us/j/123'),
    (
        'ссылка на zoom: https://yandex.zoom.us/j/1234?pwd=FSD32EFSJL59SFLSE текст...',
        'https://yandex.zoom.us/j/1234?pwd=FSD32EFSJL59SFLSE'
    ),
])
def test_find_zoom_link(text, result):
    assert result == find_zoom_link(text)
