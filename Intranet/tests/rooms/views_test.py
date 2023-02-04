import pytest
import json

from unittest.mock import patch, Mock

from intranet.vconf.src.rooms.manager import RoomManager
from intranet.vconf.tests.rooms.factories import RoomFactory

pytestmark = pytest.mark.django_db


start_call_for_me_button_event = {
    'Event': {
        'Identification': {
            'IPAddressV6': {
                'Value': '',
            },
        },
        'UserInterface': {
            'Extensions': {
                'Widget': {
                    'Action': {
                        'WidgetId': {
                            'Value': 'start_call_for_me',
                        },
                        'Type': {
                            'Value': 'clicked',
                        },
                    },
                },
            },
        },
    },
}

start_call_for_all_button_event = {
    'Event': {
        'Identification': {
            'IPAddressV6': {
                'Value': '',
            },
        },
        'UserInterface': {
            'Extensions': {
                'Widget': {
                    'Action': {
                        'WidgetId': {
                            'Value': 'start_call_for_all',
                        },
                        'Type': {
                            'Value': 'clicked',
                        },
                    },
                },
            },
        },
    },
}

stop_call_event = {
    'Event': {
        'Identification': {
            'IPAddressV6': {
                'Value': '',
            },
        },
        'UserInterface': {
            'Extensions': {
                'Widget': {
                    'Action': {
                        'WidgetId': {
                            'Value': 'stop_call_call-id',
                        },
                        'Type': {
                            'Value': 'clicked',
                        },
                    },
                },
            },
        },
    },
}


def fake_failed_create_or_join_call(*args, **kwargs):
    raise RoomManager.Error('something went wrong')


@patch('intranet.vconf.src.rooms.manager.get_codec_credentials', Mock(return_value='1234'))
@patch('intranet.vconf.src.rooms.manager.RoomManager.notify_call_is_being_created', Mock(return_value=None))
@pytest.mark.parametrize('event_data', [start_call_for_me_button_event, start_call_for_all_button_event])
def test_webhook(rooms, ya_client, event_data):
    token = 'abcdef'
    ya_client.defaults['HTTP_X_FORWARDED_FOR'] = '2001:db8:0:0:0:0:0:1'

    with patch('intranet.vconf.src.rooms.manager.RoomManager.create_or_join_call') as mock_join_call:
        with patch('intranet.vconf.src.rooms.manager.RoomManager.update_codecs_layout') as mock_update_layout:
            response = ya_client.post(
                f'/codec/webhook/?token={token}',
                data=json.dumps(event_data),
                content_type='application/json',
            )

            expected_oneself = (event_data == start_call_for_me_button_event)

            mock_join_call.assert_called_once_with(oneself=expected_oneself)
            mock_update_layout.assert_called_once()
            assert response.status_code == 200

    with patch('intranet.vconf.src.rooms.manager.RoomManager.stop_call') as mock_stop_call:
        with patch('intranet.vconf.src.rooms.manager.RoomManager.update_codecs_layout') as mock_update_layout:
            response = ya_client.post(
                f'/codec/webhook/?token={token}',
                data=json.dumps(stop_call_event),
                content_type='application/json',
            )
            mock_stop_call.assert_called_once_with('call-id')
            mock_update_layout.assert_called_once()
            assert response.status_code == 200


@patch('intranet.vconf.src.rooms.manager.get_codec_credentials', Mock(return_value='1234'))
@patch('intranet.vconf.src.rooms.manager.RoomManager.notify_call_is_being_created', Mock(return_value=None))
@patch('intranet.vconf.src.rooms.manager.RoomManager.create_or_join_call', fake_failed_create_or_join_call)
@patch('intranet.vconf.src.rooms.manager.RoomManager.update_codecs_layout', Mock(return_value=None))
def test_webhook_call_creation_failed(rooms, ya_client):
    token = 'abcdef'
    ya_client.defaults['HTTP_X_FORWARDED_FOR'] = '2001:db8:0:0:0:0:0:1'

    with patch('intranet.vconf.src.rooms.manager.RoomManager.notify_call_creation_failed') as mock_notify_failed:
        response = ya_client.post(
            f'/codec/webhook/?token={token}',
            data=json.dumps(start_call_for_me_button_event),
            content_type='application/json',
        )

        mock_notify_failed.assert_called_once()
        assert response.status_code == 200


@patch('intranet.vconf.src.rooms.manager.get_codec_credentials', Mock(return_value='1234'))
@patch('intranet.vconf.src.rooms.manager.RoomManager.notify_call_is_being_created', Mock(return_value=None))
def test_webhook_works_with_ipv6_from_event_body(ya_client):
    event_data = start_call_for_all_button_event

    room = RoomFactory()

    token = 'abcdef'
    ya_client.defaults['HTTP_X_FORWARDED_FOR'] = '192.168.1.1'
    event_data['Event']['Identification']['IPAddressV6']['Value'] = room.codec_ip[1:-1]

    with patch('intranet.vconf.src.rooms.manager.RoomManager.create_or_join_call') as mock_join_call:
        with patch('intranet.vconf.src.rooms.manager.RoomManager.update_codecs_layout') as mock_update_layout:
            response = ya_client.post(
                f'/codec/webhook/?token={token}',
                data=json.dumps(event_data),
                content_type='application/json',
            )

            mock_join_call.assert_called_once_with(oneself=False)
            mock_update_layout.assert_called_once()
            assert response.status_code == 200
