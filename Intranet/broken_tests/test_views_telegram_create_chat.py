# coding: utf-8

import json
from unittest import mock

import pytest

from tasha.external import staff
from tasha.tests import factories


pytestmark = pytest.mark.django_db
URL = '/internal/telegram/createchat/'
HEADERS = {
    'HTTP_HOST': 'localhost',
    'content_type': 'application/json'
}


def _mock_external_staff_get_person_data(username_or_phone_or_uid):
    if username_or_phone_or_uid == 'uid_3':
        return {
            'login': 'user3',
            'phones': [],
        }
    return None


def test_host_is_not_in_allowed(client):
    response = client.post(URL)
    assert response.status_code == 403
    assert response.content.decode('utf-8') == 'Request host is not in allowed hosts'


def test_bad_request_no_params(client):
    response = client.post(URL, **HEADERS)
    assert response.status_code == 400
    assert response.content.decode('utf-8') == '{"telegram_id": "is required", "title": "is required", "telegram_username": "is required"}'


def test_bad_request_no_telegram_id(client):
    data = {'title': '1234'}
    response = client.post(URL, json.dumps(data), **HEADERS)
    assert response.status_code == 400
    assert response.content.decode('utf-8') == '{"telegram_id": "is required", "telegram_username": "is required"}'


def test_bad_request_no_title(client):
    data = {'telegram_id': '1234'}
    response = client.post(URL, json.dumps(data), **HEADERS)
    assert response.status_code == 400
    assert response.content.decode('utf-8') == '{"title": "is required", "telegram_username": "is required"}'


def test_unknown_telegram_id(client):
    data = {'title': '1234', 'telegram_id': -100, 'telegram_username': 'user1'}
    response = client.post(URL, json.dumps(data), **HEADERS)
    assert response.status_code == 403
    assert response.content.decode('utf-8') == 'Unknown telegram_id for UhuraToolsBot'


def test_dismissed_uid(client):
    user = factories.UserFactory(is_active=False)
    telegram_username = factories.TelegramAccountFactory(user=user)
    data = {
        'title': '1234', 'telegram_id': telegram_username.telegram_id, 'telegram_username': telegram_username.username
    }
    response = client.post(URL, json.dumps(data), **HEADERS)
    assert response.status_code == 403
    assert response.content.decode('utf-8') == 'Unknown telegram_id for UhuraToolsBot'


@pytest.mark.xfail
def test_valid_uid(monkeypatch, client):
    user = factories.UserFactory()
    telegram_username = factories.TelegramAccountFactory(user=user)
    monkeypatch.setattr(staff, 'get_person_data_by_userphone', _mock_external_staff_get_person_data)
    monkeypatch.setattr(staff, 'get_person_data_by_uid', _mock_external_staff_get_person_data)
    create_supergroup_patch = mock.patch('tasha.external.telethon.api_sync.create_supergroup')
    invite_user_to_group_patch = mock.patch(
        'tasha.external.telethon.api_sync.invite_user_to_group'
    )

    data = {
        'title': '1234', 'telegram_id': telegram_username.telegram_id, 'telegram_username': telegram_username.username
    }
    with create_supergroup_patch as cs, invite_user_to_group_patch as iu:
        chat_obj = {'title': data['title'], 'id': 1234, 'access_hash': 1234}
        cs.return_value = chat_obj
        response = client.post(URL, json.dumps(data), **HEADERS)
        assert response.status_code == 200
        assert mock.call(data['title']) in cs.call_args_list
        assert mock.call(
            chat_obj,
            telegram_id=int(data['telegram_id']),
            username=data['telegram_username'],
            phones=[],
            make_admin=True
        ) in iu.call_args_list
