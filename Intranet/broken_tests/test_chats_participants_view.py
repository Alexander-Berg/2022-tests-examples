import json

import pytest

from tasha.tests import factories

pytestmark = pytest.mark.django_db

URL = '/internal/telegram/getchatparticipants/'


def test_no_title(client):
    response = client.get(URL)
    assert response.status_code == 400
    assert response.content.decode('utf-8') == '`title` or `chat_id` must be provided'


def test_no_group(client):
    response = client.get(URL, {'title': 'title'})
    assert response.status_code == 404


def test_ok(client):
    group = factories.TgGroupInfoFactory(title='title')

    active_user = factories.UserFactory(is_active=True, username='active')
    other_active_user = factories.UserFactory(is_active=True, username='other_active')
    inactive_user = factories.UserFactory(is_active=False, username='inactive')

    active_user_account = factories.TelegramAccountFactory(user=active_user)
    other_active_user_account = factories.TelegramAccountFactory(user=other_active_user)
    inactive_user_account = factories.TelegramAccountFactory(user=inactive_user)
    account_without_user = factories.TelegramAccountFactory(user=None)

    for account in (active_user_account, other_active_user_account, inactive_user_account, account_without_user):
        is_admin = account.user_id == other_active_user.id
        factories.TgMembershipFactory(group=group, account=account, is_admin=is_admin)

    response = client.get(URL, {'title': 'title'})
    assert response.status_code == 200
    assert json.loads(response.content.decode('utf-8')) == [
        {
            'login': 'active',
            'is_admin': False
        },
        {
            'login': 'other_active',
            'is_admin': True,
        }
    ]
