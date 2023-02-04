from mock import patch
import pytest

from intranet.vconf.src.rooms.management.commands.notify_ending_meetings import (
    get_rooms_to_notify_codec_ips,
    send_alerts_to_codecs,
)
from intranet.vconf.src.rooms.management.commands.update_codecs import (
    parse_events,
    create_new_events,
    update_rooms_event,
)


@pytest.mark.django_db
def test_get_rooms_to_notify_codec_ips(events, rooms):
    events_info, events_rooms = parse_events(events)
    new_events = create_new_events(events_info)
    update_rooms_event(new_events, events_rooms)
    for room in rooms:
        room.refresh_from_db()

    rooms_to_notify = get_rooms_to_notify_codec_ips()
    assert len(rooms_to_notify) == 1


@pytest.mark.django_db
def test_send_alerts_to_codecs(events, rooms):
    events_info, events_rooms = parse_events(events)
    new_events = create_new_events(events_info)
    update_rooms_event(new_events, events_rooms)
    for room in rooms:
        room.refresh_from_db()

    rooms_to_notify = get_rooms_to_notify_codec_ips()
    with patch('intranet.vconf.src.rooms.manager.RoomManager.notify_meeting_ends') as mock_notify_meeting_ends:
        send_alerts_to_codecs(rooms_to_notify)
        mock_notify_meeting_ends.assert_called_once()
