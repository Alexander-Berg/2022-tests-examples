import pytest
import pytz

from datetime import datetime, timedelta

from unittest.mock import patch, Mock

from django.contrib.auth import get_user_model
from django.conf import settings

from intranet.vconf.src.call.event import find_zoom_link
from intranet.vconf.src.call.models import ConferenceCall
from intranet.vconf.src.call.manager import CallManager

from intranet.vconf.src.rooms.manager import RoomManager, RegisterWebhook
from intranet.vconf.src.rooms.models import Event
from intranet.vconf.tests.rooms.factories import RoomFactory


User = get_user_model()
pytestmark = pytest.mark.django_db


@pytest.fixture
def event():
    now = datetime.utcnow()
    event = Event(
        name='test',
        event_id='123',
        start_time=now,
        end_time=now,
        organizer='test',
    )
    event.save()
    return event


@pytest.fixture
def robot():
    robot = User.objects.create(username=settings.VCONF_ROBOT_LOGIN)
    return robot


def call(*args, **kwargs):
    now = datetime.utcnow()
    call = ConferenceCall(
        name='test',
        duration=timedelta(0, 100),
        event_id='123',
        event_external_id=None,
        next_event=None,
        conf_cms_id='1234',
        meeting_id='1',
        uri='test',
        secret='test',
        chat_invite_hash='123',
        start_time=now,
        stop_time=now,
        author_login='test',
        state='active',
        template=None,
    )
    call.save()
    return CallManager(call)


@pytest.fixture(name='call')
def call_fixture(*args, **kwargs):
    return call(*args, **kwargs)


def get_event_info_with_zoom(*args, **kwargs):
    return {
        'description': f'{settings.ZOOM_URL}12345678900',
    }


def get_event_info_no_zoom(*args, **kwargs):
    return {
        'description': 'no zoom link',
    }


def test_get_codec_info(rooms):
    manager = RoomManager(rooms[0].codec_ip)
    assert manager.codec_ip == rooms[0].codec_ip


@patch('intranet.vconf.src.rooms.manager.get_codec_credentials', Mock(return_value='1234'))
def test_send_command(rooms):
    manager = RoomManager(rooms[0].codec_ip)
    mock_requests = Mock(return_value=Mock(status_code=200, content=b'some'))
    with patch('intranet.vconf.src.rooms.manager.Session.post', mock_requests):
        try:
            manager.send_command(RegisterWebhook)
        except Exception:
            assert False


def test_set_unset_call(rooms, event):
    rooms[0].event = event
    rooms[0].save()

    manager = RoomManager(rooms[0].codec_ip)
    manager.set_call('123')
    event.refresh_from_db()
    assert event.call_id == '123'

    manager.unset_call()
    event.refresh_from_db()
    assert event.call_id is None


def test_get_rooms(rooms, event):
    rooms[0].event = event
    rooms[0].save()

    rooms_info = RoomManager.get_rooms_for_event(event_id=event.event_id, values=['name', 'event__name'])
    assert rooms_info[0]['name'] == rooms[0].name
    assert rooms_info[0]['event__name'] == event.name

    rooms[1].event = event
    rooms[1].save()


def test_update_codecs_layout(rooms, event):
    manager = RoomManager(rooms[0].codec_ip)
    with patch('intranet.vconf.src.rooms.manager.RoomManager.update_layout') as mock_update_layout:
        codec_ips = manager.update_codecs_layout()
        mock_update_layout.assert_called_once_with(layout='inactive.xml')
        assert codec_ips == [rooms[0].codec_ip]

    for room in rooms:
        room.event = event
        room.save()

    update_layout_params = {
        'layout': 'event.xml',
        'event': {
            'name': event.name,
            'start_time': pytz.timezone(rooms[0].timezone).normalize(pytz.utc.localize(event.start_time)).replace(tzinfo=None),
            'end_time': pytz.timezone(rooms[0].timezone).normalize(pytz.utc.localize(event.end_time)).replace(tzinfo=None),
            'organizer': event.organizer,
            'rooms': [room.name for room in rooms],
            'call_id': event.call_id,
        },
        'language': 'ru',
    }

    with patch('intranet.vconf.src.rooms.manager.RoomManager.update_layout') as mock_update_layout:
        codec_ips = manager.update_codecs_layout(oneself=True)
        mock_update_layout.assert_called_once_with(**update_layout_params)
        assert codec_ips == [rooms[0].codec_ip]

    with patch('intranet.vconf.src.rooms.manager.RoomManager.update_layout') as mock_update_layout:
        codec_ips = manager.update_codecs_layout()
        mock_update_layout.assert_called_with(**update_layout_params)
        assert mock_update_layout.call_count == len(rooms)
        assert set(codec_ips) == set([room.codec_ip for room in rooms])


@patch('intranet.vconf.src.rooms.manager.get_event_info', get_event_info_with_zoom)
@pytest.mark.parametrize('oneself', [False, True])
def test_create_or_join_new_zoom_call_zoom_calls_enabled(rooms, event, robot, oneself):
    settings.CODEC_ZOOM_CALLS_ENABLED = True

    rooms[0].event = event
    rooms[0].save()
    manager = RoomManager(rooms[0].codec_ip)
    zoom_link = find_zoom_link(get_event_info_with_zoom()['description'])
    with patch('intranet.vconf.src.rooms.manager.RoomManager.create_zoom_call') as mock_create_zoom_call:
        manager.create_or_join_call(oneself=oneself)
        mock_create_zoom_call.assert_called_once_with(event, zoom_link, oneself=oneself)


@patch('intranet.vconf.src.rooms.manager.get_event_info', get_event_info_with_zoom)
def test_create_or_join_new_zoom_call_zoom_calls_disabled(rooms, event, robot):
    settings.CODEC_ZOOM_CALLS_ENABLED = False

    rooms[0].event = event
    rooms[0].save()
    manager = RoomManager(rooms[0].codec_ip)
    zoom_link = find_zoom_link(get_event_info_with_zoom()['description'])
    with patch('intranet.vconf.src.rooms.manager.RoomManager.create_cms_call') as mock_create_cms_call:
        manager.create_or_join_call()
        mock_create_cms_call.assert_called_once_with(event, zoom_link, oneself=False)


@patch('intranet.vconf.src.rooms.manager.get_event_info', get_event_info_with_zoom)
def test_create_or_join_existing_zoom_call(rooms, event, robot, call):
    settings.CODEC_ZOOM_CALLS_ENABLED = True

    event.call_id = '1234'
    event.save()
    rooms[1].event = event
    rooms[1].save()
    manager = RoomManager(rooms[1].codec_ip)
    with patch('intranet.vconf.src.rooms.manager.RoomManager.join_zoom_call') as mock_join_zoom_call:
        manager.create_or_join_call()
        mock_join_zoom_call.assert_called_once()


@patch('intranet.vconf.src.rooms.manager.get_event_info', get_event_info_no_zoom)
@pytest.mark.parametrize('oneself', [False, True])
def test_create_or_join_new_cms_call(rooms, event, robot, oneself):
    settings.CODEC_ZOOM_CALLS_ENABLED = True

    rooms[0].event = event
    rooms[0].save()
    manager = RoomManager(rooms[0].codec_ip)
    with patch('intranet.vconf.src.rooms.manager.RoomManager.create_cms_call') as mock_create_cms_call:
        manager.create_or_join_call(oneself=oneself)
        mock_create_cms_call.assert_called_once_with(event, None, oneself=oneself)


@patch('intranet.vconf.src.rooms.manager.get_event_info', get_event_info_no_zoom)
def test_create_or_join_existing_cms_call(rooms, event, robot, call):
    settings.CODEC_ZOOM_CALLS_ENABLED = True

    event.call_id = '1234'
    event.save()
    rooms[1].event = event
    rooms[1].save()
    manager = RoomManager(rooms[1].codec_ip)
    with patch('intranet.vconf.src.rooms.manager.RoomManager.join_cms_call') as mock_join_cms_call:
        manager.create_or_join_call()
        mock_join_cms_call.assert_called_once()


def test_create_or_join_call_fails_no_event(rooms, event, robot):
    manager = RoomManager(rooms[2].codec_ip)
    with patch('intranet.vconf.src.rooms.manager.RoomManager.update_codecs_layout') as mock_update_layout:
        with pytest.raises(RoomManager.Error):
            manager.create_or_join_call()
            mock_update_layout.assert_called_once()


@patch('intranet.vconf.src.call.manager.CallManager.create', call)
@patch('intranet.vconf.src.rooms.manager.RoomManager.set_call', Mock(return_value=None))
def test_create_zoom_call(rooms, event, robot):
    rooms[0].event = event
    rooms[0].save()
    rooms[1].event = event
    rooms[1].save()

    manager = RoomManager(rooms[0].codec_ip)
    zoom_link = f'{settings.ZOOM_URL}1234'
    with patch('intranet.vconf.src.rooms.manager.RoomManager.dial') as mock_dial:
        manager.create_zoom_call(event, zoom_link)
        assert mock_dial.call_count == 2


@patch('intranet.vconf.src.call.manager.CallManager.create', call)
@patch('intranet.vconf.src.rooms.manager.RoomManager.set_call', Mock(return_value=None))
def test_create_zoom_call_oneself(rooms, event, robot):
    rooms[0].event = event
    rooms[0].save()
    rooms[1].event = event
    rooms[1].save()

    manager = RoomManager(rooms[0].codec_ip)
    zoom_link = f'{settings.ZOOM_URL}1234'
    with patch('intranet.vconf.src.rooms.manager.RoomManager.dial') as mock_dial:
        manager.create_zoom_call(event, zoom_link, oneself=True)
        mock_dial.assert_called_once()


@patch('intranet.vconf.src.call.manager.CallManager.create', call)
@patch('intranet.vconf.src.rooms.manager.get_event_info', get_event_info_no_zoom)
@patch('intranet.vconf.src.rooms.manager.RoomManager.get_participants_by_event', Mock(return_value=[]))
def test_create_cms_call(rooms, event, robot):
    rooms[0].event = event
    rooms[0].save()
    rooms[1].event = event
    rooms[1].save()

    manager = RoomManager(rooms[0].codec_ip)
    with patch('intranet.vconf.src.rooms.manager.RoomManager.get_participants_by_event') as mock_get_participants:
        with patch('intranet.vconf.src.rooms.manager.RoomManager.set_call') as mock_set_call:
            manager.create_cms_call(event)
            mock_get_participants.assert_called_once()
            mock_set_call.assert_called_once()


@patch('intranet.vconf.src.call.manager.CallManager.create', call)
@patch('intranet.vconf.src.rooms.manager.get_event_info', get_event_info_no_zoom)
@patch('intranet.vconf.src.rooms.manager.RoomManager.get_participants_by_event', Mock(return_value=[]))
@patch('intranet.vconf.src.rooms.manager.RoomManager.as_participant', Mock(return_value=None))
def test_create_cms_call_oneself(rooms, event, robot):
    rooms[0].event = event
    rooms[0].save()
    rooms[1].event = event
    rooms[1].save()

    manager = RoomManager(rooms[0].codec_ip)
    with patch('intranet.vconf.src.rooms.manager.RoomManager.get_participants_by_event') as mock_get_participants:
        with patch('intranet.vconf.src.rooms.manager.RoomManager.set_call') as mock_set_call:
            manager.create_cms_call(event, oneself=True)
            mock_get_participants.assert_not_called()
            mock_set_call.assert_called_once()


def test_find_room_by_ip():
    try:
        first_room = RoomFactory(codec_ip='[2001:db8:0:0:0:0:2:0]')
        RoomManager(first_room.codec_ip)
        RoomManager('ff::0:0:2:0')

        RoomFactory(codec_ip='[3001:db8:0:0:0:0:2:0]')
        RoomManager('3001:db8:0:0:0:0:2:0')
    except Exception:
        pytest.fail('test_find_room_by_ip failed', pytrace=True)

    with pytest.raises(RoomManager.Error):
        RoomManager('[1001:db8:0:0:0:0:2:0]')

    with pytest.raises(RoomManager.Error):
        RoomManager('ff::0:0:2:0')
