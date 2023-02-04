import json
import pytest
from unittest.mock import patch, MagicMock, Mock

from django.urls import reverse

from intranet.vconf.src.call.ether import AllStreamersAreBusy
from intranet.vconf.src.call.views.calls import clean_call_list_params

from intranet.vconf.tests.call.factories import UserSettingsFactory, ConferenceCallFactory
from intranet.vconf.tests.call.mock import FakeHydrator


pytestmark = pytest.mark.django_db


def test_user_settings_create(ya_client):
    response = ya_client.post(
        path=reverse('frontapi:user_settings'),
        data=json.dumps({'method': 'mobile'}),
        content_type='application/json',
    )
    assert response.status_code == 200

    answer = json.loads(response.content)
    assert answer['response_text']['method'] == 'mobile'


def test_user_settings_update(ya_client):
    UserSettingsFactory()
    response = ya_client.post(
        path=reverse('frontapi:user_settings'),
        data=json.dumps({'method': 'mobile'}),
        content_type='application/json',
    )
    assert response.status_code == 200

    answer = json.loads(response.content)
    assert answer['response_text']['method'] == 'mobile'


def test_meta_bad_participant(ya_client):
    response = ya_client.post(
        path=reverse('frontapi:meta'),
        data=json.dumps({
            'participants': [
                {'type': 'arbitrary', 'id': 'lalala'},
                {'type': 'arbitrary', 'id': '1234'},
            ]
        }),
        content_type='application/json'
    )
    assert response.status_code == 400
    answer = json.loads(response.content)
    assert answer['response_text'] == [{
        'participant': {'id': 'lalala', 'type': 'arbitrary'},
        'error': 'Invalid arbitrary id lalala',
        'code': 400,
    }]


def test_meta(ya_client):
    room_mock = MagicMock(return_value={
        'result': [
            {
                'phone': '',
                'id': 2037,
                'name': {
                    'ru': '2.Orange soda',
                    'en': '2.Orange soda',
                },
                'equipment': {
                    'video_conferencing': '5803'
                }
            },
            {
                'phone': '3456',
                'id': '2038',
                'name': {
                    'ru': '2.Orange woda',
                    'en': '2.Orange woda',
                },
                'equipment': {
                    'video_conferencing': '6543'
                }
            }
        ],
    })
    with patch('intranet.vconf.src.call.hydrator.get_room_info', room_mock):
        response = ya_client.post(
            path=reverse('frontapi:meta'),
            data=json.dumps({
                'participants': [
                    {'type': 'room', 'id': 2037},
                    {'type': 'room', 'id': 2038},
                ]
            }),
            content_type='application/json'
        )

        assert response.status_code == 200, response.content
        answer = json.loads(response.content)
        assert answer['response_text'] == [
            {
                'type': 'room',
                'id': '2037',
                'name': '2.Orange soda',
                'label': '2.Orange soda',
                'number': '5803',
                'camera': True,
                'microphone': True,
                'action': ['cisco'],
                'method': None
            },
            {
                'type': 'room',
                'id': '2038',
                'name': '2.Orange woda',
                'label': '2.Orange woda',
                'number': '6543',
                'camera': True,
                'microphone': True,
                'action': ['cisco'],
                'method': None
            }
        ]


def test_clean_call_list_params():
    from intranet.vconf.src.call.constants import CallFilter, CALL_STATES

    res = clean_call_list_params({})
    assert res == CallFilter(show_all=False, state=CALL_STATES.active, limit=20, page=1, with_stream=None, with_record=None)

    res = clean_call_list_params({'show_all': 0, 'history': 0, 'limit': '25', 'page': '100', 'with_record': 0})
    assert res == CallFilter(show_all=False, state=CALL_STATES.active, limit=25, page=100, with_stream=None, with_record=False)

    res = clean_call_list_params({'show_all': 1, 'history': 1, 'state': 'active', 'limit': 30, 'page': 150, 'with_record': 1})
    assert res == CallFilter(show_all=True, state=CALL_STATES.ended, limit=30, page=150, with_stream=None, with_record=True)

    res = clean_call_list_params({'show_all': '1', 'state': 'active', 'limit': '30', 'page': '150', 'with_stream': 1, 'with_record': 'false'})
    assert res == CallFilter(show_all=True, state=CALL_STATES.active, limit=30, page=150, with_stream=True, with_record=False)

    res = clean_call_list_params({'show_all': 'true', 'state': 'active', 'limit': '30', 'page': '150', 'with_record': 'true'})
    assert res == CallFilter(show_all=True, state=CALL_STATES.active, limit=30, page=150, with_stream=None, with_record=True)


@pytest.mark.parametrize('field_name,result', [
    ('priority', 'priority'),
    ('-priority', '-priority'),
    ('random_string', None),
    ('', None),
    ('1', None)
])
def test_clean_call_list_params_sort(field_name, result):
    res = clean_call_list_params({'sort': field_name})
    assert res.sort == result


@patch('intranet.vconf.src.call.views.calls.ParticipantsHydrator', FakeHydrator)
def test_create_stream_with_busy_channels(ya_client, cms_nodes):
    get_ether_id_mock = Mock(side_effect=AllStreamersAreBusy())
    with patch('intranet.vconf.src.call.manager.get_ether_id', get_ether_id_mock):
        response = ya_client.post(
            path=reverse('frontapi:create'),
            data=json.dumps({
                'participants': [
                    {'type': 'room', 'id': 2037},
                ],
                'name': 'name',
                'duration': 20,
                'stream': True,
                'record': False,
            }),
            content_type='application/json',
        )

    assert response.status_code == 400, response.content
    answer = json.loads(response.content)
    assert answer['response_text']['code'] == 'no_available_channels'


def test_disconnect_participant_not_found(ya_client):
    call = ConferenceCallFactory(conf_cms_id='12345678-aaaa-22bb-b8b7-6ae16224140c')
    response = ya_client.post(
        path=reverse('frontapi:disconnect_participant', kwargs={'conf_cms_id': call.conf_cms_id}),
        data=json.dumps({
            'id': 'user_login',
            'type': 'person',
        }),
        content_type='application/json',
    )

    assert response.status_code == 400
    answer = response.json()['response_text']
    assert answer['code'] == 'participant_not_found'
