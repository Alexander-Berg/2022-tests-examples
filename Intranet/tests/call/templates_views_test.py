import json
import pytest
from unittest.mock import patch

from django.conf import settings
from django.urls import reverse
from django.forms import ValidationError

from intranet.vconf.tests.lib.mocks import participants_hydrator_mock
from intranet.vconf.tests.call.factories import (
    CallTemplateFactory,
    ParticipantTemplateFactory,
    ConferenceCallFactory,
    EventFactory,
)
from intranet.vconf.src.call.constants import CALL_STATES
from intranet.vconf.src.call.views.templates import validate_template_data

from intranet.vconf.tests.call.mock import get_event_info_mock, get_next_event_mock

pytestmark = pytest.mark.django_db


@patch('intranet.vconf.src.call.call_template.ParticipantsHydrator', participants_hydrator_mock)
@patch('intranet.vconf.src.call.views.templates.get_next_event', get_next_event_mock)
def test_save_template(ya_client):
    event_id = '123'
    event_external_id = 'abcd' + event_id
    response = ya_client.post(
        path=reverse('frontapi:create_template'),
        data=json.dumps({
            'name': 'test',
            'duration': 30,
            'stream': False,
            'event_id': event_id,
            'record': False,
            'owners': [],
            'participants': [
                {'id': '1234', 'type': 'room', 'method': 'cisco'},
                {'id': 'user1', 'type': 'person', 'method': 'cisco'},
                {'id': 'user2', 'type': 'person', 'method': 'cisco'},
            ]
        }),
        content_type='application/json'
    )

    assert response.status_code == 200, response.content
    answer = json.loads(response.content)

    tid = answer['response_text'].pop('id')
    answer['response_text']['participants'].sort(key=lambda x: x['id'])

    assert tid
    assert answer['response_text'] == {
        'name': 'test',
        'duration': 30,
        'stream': False,
        'stream_description': '',
        'stream_picture': '',
        'record': False,
        'next_event': {
            'id': 1,
            'end_time': '2020-01-24T15:00:00+03:00',
            'start_time': '2020-01-24T12:00:00+03:00',
            'description': 'some text',
        },
        'event_id': int(event_id),
        'event_external_id': event_external_id,
        'owners': [settings.AUTH_TEST_USER],
        'priority': 0,
        'participants': [
            {'id': '1234', 'type': 'room', 'method': 'cisco'},
            {'id': 'user1', 'type': 'person', 'method': 'cisco'},
            {'id': 'user2', 'type': 'person', 'method': 'cisco'},
        ]
    }

    event_id = '345'
    event_external_id = 'abcd' + event_id
    response = ya_client.post(
        path=reverse('frontapi:template_detail', kwargs={'obj_id': tid}),
        data=json.dumps({
            'name': 'test',
            'duration': 30,
            'stream': False,
            'stream_description': 'Описание трансляции',
            'record': False,
            'event_id': event_id,
            'owners': [settings.AUTH_TEST_USER, 'user_x'],
            'participants': [
                {'id': '1234', 'type': 'room', 'method': 'cisco'},
                {'id': '1235', 'type': 'room', 'method': 'cisco'},
                {'id': 'user1', 'type': 'person', 'method': 'cisco'},
            ]
        }),
        content_type='application/json'
    )

    assert response.status_code == 200, response.content
    answer = json.loads(response.content)

    assert answer['response_text'].pop('id') == tid
    answer['response_text']['participants'].sort(key=lambda x: x['id'])

    assert answer['response_text'] == {
        'name': 'test',
        'duration': 30,
        'stream': False,
        'stream_description': 'Описание трансляции',
        'stream_picture': '',
        'record': False,
        'next_event': {
            'id': 1,
            'end_time': '2020-01-24T15:00:00+03:00',
            'start_time': '2020-01-24T12:00:00+03:00',
            'description': 'some text',
        },
        'event_id': int(event_id),
        'event_external_id': event_external_id,
        'owners': [settings.AUTH_TEST_USER, 'user_x'],
        'priority': 0,
        'participants': [
            {'id': '1234', 'type': 'room', 'method': 'cisco'},
            {'id': '1235', 'type': 'room', 'method': 'cisco'},
            {'id': 'user1', 'type': 'person', 'method': 'cisco'},
        ]
    }

    response = ya_client.post(
        path=reverse('frontapi:template_detail', kwargs={'obj_id': tid}),
        data=json.dumps({
            'name': 'test',
            'duration': 30,
            'stream': False,
            'record': False,
            'owners': [],
            'event_id': None,
            'participants': [],
        }),
        content_type='application/json',
    )
    assert response.status_code == 200, response.content
    answer = json.loads(response.content)
    assert answer['response_text']['event_id'] is None
    assert answer['response_text']['event_external_id'] is None


@patch('intranet.vconf.src.call.event.get_event_info', get_event_info_mock)
def test_get_template(ya_client):
    template = CallTemplateFactory(event_id=1)
    tid = template.id
    response = ya_client.get(
        path=reverse('frontapi:template_detail', kwargs={'obj_id': tid}),
        content_type='application/json',
    )
    assert response.status_code == 200, response.content

    answer = json.loads(response.content)['response_text']
    assert answer.pop('id') == tid
    assert 'event' in answer
    assert answer['event']['name'] == 'Event'
    assert answer['event']['startTs'] == '2020-01-24T12:00:00+03:00'


@patch('intranet.vconf.src.call.views.templates.ParticipantsHydrator', participants_hydrator_mock)
def test_delete_template(ya_client):
    c1 = CallTemplateFactory()
    ParticipantTemplateFactory(call_template=c1)
    ParticipantTemplateFactory(call_template=c1)

    c2 = CallTemplateFactory()
    ParticipantTemplateFactory(call_template=c2)
    ParticipantTemplateFactory(call_template=c2)

    response = ya_client.post(
        path=reverse('frontapi:delete_template', kwargs={'obj_id': c1.id}),
    )

    assert response.status_code == 200, response.content
    answer = json.loads(response.content)
    assert len(answer['response_text']) == 1


def test_template_stream_not_found(ya_client):
    response = ya_client.get(
        path=reverse('frontapi:template_stream', kwargs={'obj_id': 1})
    )
    assert response.status_code == 404
    answer = json.loads(response.content)
    assert answer['response_text']['code'] == 'template_is_not_found'


def test_template_stream_no_active_calls_for_template(ya_client):
    template = CallTemplateFactory(stream=True, event_external_id=None)
    response = ya_client.get(
        path=reverse('frontapi:template_stream', kwargs={'obj_id': template.id})
    )
    assert response.status_code == 404
    answer = json.loads(response.content)
    assert answer['response_text']['code'] == 'no_active_calls_for_template'


def test_template_stream_with_active_call_by_template(ya_client):
    template = CallTemplateFactory(stream=True, event_external_id=None)
    call = ConferenceCallFactory(stream=True, template=template, state=CALL_STATES.active)
    response = ya_client.get(
        path=reverse('frontapi:template_stream', kwargs={'obj_id': template.id})
    )
    assert response.status_code == 200
    answer = json.loads(response.content)
    assert answer['response_text']['id'] == call.conf_cms_id


@patch('intranet.vconf.src.call.views.templates.get_next_event', get_event_info_mock)
def test_template_stream_no_active_calls_for_event(ya_client):
    template = CallTemplateFactory(stream=True, event_external_id='1')
    response = ya_client.get(
        path=reverse('frontapi:template_stream', kwargs={'obj_id': template.id})
    )
    assert response.status_code == 404
    answer = json.loads(response.content)
    assert answer['response_text']['code'] == 'no_active_calls_for_event_external_id'


def test_template_stream_is_not_stream(ya_client):
    template = CallTemplateFactory(stream=False)
    ConferenceCallFactory(stream=False, template=template, state=CALL_STATES.active)

    response = ya_client.get(
        path=reverse('frontapi:template_stream', kwargs={'obj_id': template.id})
    )
    assert response.status_code == 404
    answer = json.loads(response.content)
    assert answer['response_text']['code'] == 'call_is_not_stream'


def test_template_stream_with_active_call_by_event_external_id(ya_client):
    event_external_id = '1'
    template = CallTemplateFactory(event_external_id=event_external_id, owners=['user'])
    call = ConferenceCallFactory(stream=True, event_external_id=event_external_id)
    response = ya_client.get(
        path=reverse('frontapi:template_stream', kwargs={'obj_id': template.id})
    )
    assert response.status_code == 200
    answer = json.loads(response.content)
    assert answer['response_text']['id'] == call.conf_cms_id


def test_templates_with_stream(ya_client):
    event = EventFactory()
    template = CallTemplateFactory(next_event=event, stream=True)
    template.save()
    response = ya_client.get(
        path=reverse('frontapi:templates_with_stream')
    )
    assert response.status_code == 200
    answer = json.loads(response.content)['response_text']
    assert answer[0]['id'] == template.id
    assert answer[0]['next_event']['description'] == template.next_event.description


@pytest.mark.parametrize('data,is_valid', [
    ({'stream_picture': '/jing/files/some_login/image.jpg'}, True),
    ({'stream_picture': '12345'}, False),
    ({'stream_picture': '/not_jing/files/some_login/image.jpg'}, False),
    ({'stream_picture': ''}, True),
], ids=lambda x: str(x))
def test_validate_template_data(data, is_valid):
    if is_valid:
        validate_template_data(data)
    else:
        with pytest.raises(ValidationError):
            validate_template_data(data)
