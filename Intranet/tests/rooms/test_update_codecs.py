import pytest

from datetime import datetime

from intranet.vconf.src.rooms.management.commands.update_codecs import (
    parse_events,
    get_office_ids,
    create_new_events,
    update_events,
    update_rooms_event,
    delete_ended_events,
)
from intranet.vconf.src.rooms.models import Room, Event


@pytest.mark.django_db
def test_get_office_ids(rooms):
    office_ids = get_office_ids()
    assert office_ids == ['1']


def test_parse_events(events):
    events_info, events_rooms = parse_events(events)
    event_data_rooms = {}
    for event_id, event in events.items():
        event_data_rooms[event_id] = event.pop('rooms')
        event['event_id'] = event_id

    assert events_info == events
    assert events_rooms == event_data_rooms


@pytest.mark.django_db
def test_create_new_events(events):
    events_info = parse_events(events)[0]
    Event.objects.create(
        name=events['1234']['name'],
        event_id='1234',
        start_time=events['1234']['start_time'],
        end_time=events['1234']['end_time'],
        organizer=events['1234']['organizer'],
    )

    new_events = create_new_events(events_info)
    assert set(new_events) == {'4321', '5678', '0005'}
    assert Event.objects.get(event_id='4321').name == 'second event'


@pytest.mark.django_db
def test_update_events(events, rooms):
    events_info, events_rooms = parse_events(events)
    events_to_update = update_events(events_info, events_rooms)
    assert events_to_update == []

    event_id = '4321'
    Event.objects.create(
        name=events[event_id]['name'],
        event_id=event_id,
        start_time=events[event_id]['start_time'],
        end_time=events[event_id]['end_time'],
        organizer=events[event_id]['organizer'] + 'some',
    )
    events_to_update = update_events(events_info, events_rooms)
    update_rooms_event(events_to_update, events_rooms)
    assert events_to_update == [event_id]
    assert Event.objects.get(event_id=event_id).organizer == events[event_id]['organizer']

    event_id = '1234'
    Event.objects.create(
        name=events[event_id]['name'],
        event_id=event_id,
        start_time=events[event_id]['start_time'],
        end_time=events[event_id]['end_time'],
        organizer=events[event_id]['organizer'],
    )
    Room.objects.filter(event__event_id=event_id).update(event=None)
    events_to_update = update_events(events_info, events_rooms)
    update_rooms_event(events_to_update, events_rooms)
    assert events_to_update == [event_id]
    assert set(
        Room.objects
        .filter(event__event_id=event_id)
        .values_list('email', flat=True)
    ) == set(events[event_id]['rooms'])


@pytest.mark.django_db
def test_update_rooms_event(events, rooms):
    events_info, events_rooms = parse_events(events)
    new_events = create_new_events(events_info)
    update_rooms_event(new_events, events_rooms)
    for room in rooms:
        room.refresh_from_db()

    assert rooms[0].event.event_id == '1234'
    assert rooms[1].event.event_id == '1234'
    assert rooms[2].event.event_id == '4321'
    assert rooms[3].event.event_id == '5678'
    assert rooms[4].event is None
    assert rooms[5].event.event_id == '0005'


@pytest.mark.django_db
def test_delete_ended_events(events, rooms):
    event = Event.objects.create(
        name='name',
        event_id='event_id',
        start_time=datetime(2018, 9, 15, 14, 00),
        end_time=datetime(2018, 9, 15, 14, 30),
        organizer='organizer',
    )
    rooms[4].event = event
    rooms[4].save()

    events_info = parse_events(events)[0]
    rooms_with_ended_events = delete_ended_events(events_info)
    assert rooms_with_ended_events == [rooms[4].codec_ip]
