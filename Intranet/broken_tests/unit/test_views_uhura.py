import json

import pytest

from tasha.models import TelegramAccount

from tasha.tests import factories

pytestmark = pytest.mark.django_db
bots_url = '/internal/uhura/bots/'
registration_url = '/internal/uhura/registration/'
HEADERS = {
    'HTTP_HOST': 'localhost',
    'content_type': 'application/json'
}


def test_get_bots(client):
    finn = factories.TelegramAccountFactory(username='finn', is_tasha=True)
    jake = factories.TelegramAccountFactory(username='jake', is_tasha=True)
    factories.TgMembershipFactory(account=finn)
    factories.TgMembershipFactory(account=jake)
    factories.TgMembershipFactory(account=jake)
    response = client.get(bots_url, **HEADERS)
    assert response.status_code == 200
    response_data = response.json()
    assert response_data == ['finn', 'jake']


def test_registration(client, registration_data):
    response = client.post(registration_url, json.dumps(registration_data), **HEADERS)
    assert response.status_code == 200
    account = TelegramAccount.objects.get(telegram_id=registration_data['telegram_id'])
    assert account.user.username == registration_data['user']
    assert account.created_with_uhura
    assert account.username is None


def test_registration_with_existing_account(client, registration_data):
    account = factories.TelegramAccountFactory(telegram_id=registration_data['telegram_id'])
    response = client.post(registration_url, json.dumps(registration_data), **HEADERS)
    account.refresh_from_db()
    assert response.status_code == 200
    assert account.user.username == registration_data['user']
    assert account.created_with_uhura
    assert account.username is None


def test_registration_without_user(client, registration_data):
    registration_data['user'] = 'jake'
    response = client.post(registration_url, json.dumps(registration_data), **HEADERS)
    assert response.status_code == 400


@pytest.fixture
def registration_data():
    factories.UserFactory(username='finn')
    telegram_id = 1
    user = 'finn'
    return {
        'telegram_id': telegram_id,
        'user': user,
    }
