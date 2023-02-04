import json
import pytest
from unittest.mock import patch

from django.conf import settings
from django.test import Client
from django.urls import reverse

from intranet.vconf.src.call.constants import CALL_STATES
from intranet.vconf.src.lib.urls import reverse_querystring

from intranet.vconf.tests.call.factories import create_user, EventFactory, ConferenceCallFactory
from intranet.vconf.tests.call.mock import get_event_info_mock, get_next_event_mock


pytestmark = pytest.mark.django_db


def get_next_event_mock_with_user(*args, **kwargs):
    event = get_next_event_mock(*args, **kwargs)
    event['attendees'].append({'login': settings.AUTH_TEST_USER})
    return event


@patch('intranet.vconf.src.call.views.events.get_next_event', get_next_event_mock)
def test_event_generate_secret_403(ya_client):
    response = ya_client.post(
        path=reverse('frontapi:event_generate_secret', kwargs={'event_id': 1}),
        content_type='application/json',
    )
    assert response.status_code == 403


def generate_secret_mock(self):
    self.secret = '123'


@patch('intranet.vconf.src.call.views.events.get_next_event', get_next_event_mock_with_user)
@patch('intranet.vconf.src.call.models.Event.generate_secret', generate_secret_mock)
def test_event_generate_secret_200(ya_client):
    response = ya_client.post(
        path=reverse('frontapi:event_generate_secret', kwargs={'event_id': 1}),
        content_type='application/json',
    )
    assert response.status_code == 200

    data = json.loads(response.content)['response_text']
    assert data['event_id'] == 1
    assert data['master_id'] == 1
    assert data['secret'] == '123'


def _request_event_invite(client, params):
    response = client.get(
        path=reverse_querystring(
            view='frontapi:event_invite',
            kwargs={'event_id': 1},
            query_kwargs=params,
        ),
        content_type='application/json',
    )
    data = json.loads(response.content)['response_text']
    return data, response.status_code


def test_event_invite_missing_event(ya_client):
    data, status_code = _request_event_invite(ya_client, params={'master_id': 1, 'secret': '123'})
    assert status_code == 404


def test_event_invite_wrong_secret(ya_client):
    EventFactory(id=1)
    data, status_code = _request_event_invite(ya_client, params={'master_id': 1, 'secret': '123'})
    assert status_code == 403


@patch('intranet.vconf.src.call.views.events.get_event_info', get_event_info_mock)
@pytest.mark.parametrize('is_authenticated', [True, False])
def test_event_invite_no_call(is_authenticated):
    if is_authenticated:
        create_user(username=settings.AUTH_TEST_USER)
    client = Client()
    event = EventFactory(id=1)
    data, status_code = _request_event_invite(
        client,
        params={'master_id': 1, 'secret': event.secret},
    )
    assert status_code == 404
    assert data['details']['event'] is not None
    assert data['details']['is_authenticated'] is is_authenticated


@pytest.mark.parametrize('is_authenticated', [True, False])
def test_event_invite_active_call(is_authenticated):
    if is_authenticated:
        create_user(username=settings.AUTH_TEST_USER)
    client = Client()
    event = EventFactory(id=1)
    ConferenceCallFactory(
        secret=event.secret,
        next_event=event,
        state=CALL_STATES.active,
    )
    data, status_code = _request_event_invite(
        client,
        params={'master_id': 1, 'secret': event.secret},
    )
    assert status_code == 200
    assert data['invite_link'] is not None
    assert data['is_authenticated'] is is_authenticated
