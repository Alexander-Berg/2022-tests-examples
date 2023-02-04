# coding: utf-8

import json
from unittest import mock

import pytest
from telethon import errors

pytestmark = pytest.mark.django_db(transaction=True)
URL = '/internal/telegram/invitetochat/'
HEADERS = {
    'HTTP_HOST': 'localhost',
    'content_type': 'application/json'
}


def test_host_is_not_in_allowed(client):
    response = client.post(URL)
    assert response.status_code == 403
    assert response.content.decode('utf-8') == 'Request host is not in allowed hosts'


def test_invalid_json(client):
    data = '...'
    response = client.post(URL, data, **HEADERS)
    assert response.status_code == 400
    assert response.content.decode('utf-8') == ('Invalid json in request.body: %r' % data.encode('utf-8'))


def test_no_invite_link(client):
    data = {}
    response = client.post(URL, json.dumps(data), **HEADERS)
    assert response.status_code == 400
    assert response.content.decode('utf-8') == 'Parameter "invite_link" is required'


def test_chat_invite_already(client):
    data = {'invite_link': 'https://t.me/joinchat/AA-bb'}
    with mock.patch('tasha.external.telethon.api_sync.check_already_participants', return_value=True) as m:
        response = client.post(URL, json.dumps(data), **HEADERS)
        assert response.status_code == 200
        response_data = json.loads(response.content.decode('utf-8'))
        assert response_data == {'status': 'already_participant'}
        assert m.call_args_list == [mock.call(data['invite_link'])]


def test_join_chat(client):
    data = {'invite_link': 'https://t.me/joinchat/AA-bb'}
    with mock.patch('tasha.external.telethon.api_sync.check_already_participants', return_value=False) as already_participant_patch, \
            mock.patch('tasha.external.telethon.api_sync.join_chat', return_value=('success', {})) as join_patch:
        response = client.post(URL, json.dumps(data), **HEADERS)
        assert response.status_code == 200
        response_data = json.loads(response.content.decode('utf-8'))
        assert response_data == {'status': 'success'}
        assert already_participant_patch.call_args_list == [mock.call(data['invite_link'])]
        assert join_patch.call_args_list == [mock.call(data['invite_link'])]


def test_invite_hash_expired(client):
    data = {'invite_link': 'https://t.me/joinchat/AA-bb'}
    with mock.patch('tasha.external.telethon.api_sync.check_already_participants', return_value=False) as already_participant_patch, \
            mock.patch('tasha.external.telethon.api_sync.join_chat', side_effect=errors.InviteHashExpiredError(request=None)) as join_patch:
        response = client.post(URL, json.dumps(data), **HEADERS)
        assert response.status_code == 200
        response_data = json.loads(response.content.decode('utf-8'))
        assert response_data == {'status': 'expired_link_or_banned'}
        assert already_participant_patch.call_args_list == [mock.call(data['invite_link'])]
        assert join_patch.call_args_list == [mock.call(data['invite_link'])]
