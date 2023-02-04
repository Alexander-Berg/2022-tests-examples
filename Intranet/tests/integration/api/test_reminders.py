from django.urls.base import reverse

from intranet.femida.src.communications.choices import REMINDER_STATUSES, MESSAGE_TYPES
from intranet.femida.src.utils.datetime import shifted_now

import pytest
from intranet.femida.tests import factories as f


def test_reminder_create_form(su_client):
    url = reverse('api:reminders:create-form')
    response = su_client.get(url)
    assert response.status_code == 200


@pytest.mark.parametrize('message_type', (
    MESSAGE_TYPES.note,
    MESSAGE_TYPES.internal,
))
def test_reminder_create(su_client, message_type):
    message = f.create_message(type=message_type)
    data = {
        'remind_at': (shifted_now(days=1)).strftime('%Y-%m-%d %H:%M:%S'),
        'message': message.id,
    }
    url = reverse('api:reminders:create')
    assert message.reminders.count() == 0
    response = su_client.post(url, data)
    assert response.status_code == 201
    assert set(response.data.keys()) == {
        'id',
        'user',
        'remind_at',
        'status',
        'message',
    }
    assert message.reminders.count() == 1


@pytest.mark.parametrize('message_type', (
    MESSAGE_TYPES.brief,
    MESSAGE_TYPES.incoming,
    MESSAGE_TYPES.outcoming,
))
def test_reminder_create_unsupported(su_client, message_type):
    message = f.create_message(type=message_type)
    data = {
        'remind_at': (shifted_now(days=1)).strftime('%Y-%m-%d %H:%M:%S'),
        'message': message.id,
    }
    url = reverse('api:reminders:create')
    response = su_client.post(url, data)
    assert response.status_code == 400


@pytest.mark.parametrize('data', (
    {'remind_at': (shifted_now(days=-1)).strftime('%Y-%m-%d %H:%M:%S')},
    {'remind_at': 'some text'},
    {},
))
def test_reminder_create_data_validation(su_client, data):
    message = f.create_note()
    data['message'] = message.id
    url = reverse('api:reminders:create')
    response = su_client.post(url, data)
    assert response.status_code == 400


def test_reminder_create_user_restriction(su_client):
    message = f.create_note()
    data = {
        'remind_at': (shifted_now(days=1)).strftime('%Y-%m-%d %H:%M:%S'),
        'message': message.id,
    }
    url = reverse('api:reminders:create')
    response = su_client.post(url, data)
    assert response.status_code == 201
    response = su_client.post(url, data)
    assert response.status_code == 400


def test_reminder_create_multiple_users(su_client):
    message = f.create_note()
    data = {
        'remind_at': (shifted_now(days=1)).strftime('%Y-%m-%d %H:%M:%S'),
        'message': message.id,
    }
    f.ReminderFactory(message=message)
    url = reverse('api:reminders:create')
    response = su_client.post(url, data)
    assert response.status_code == 201


@pytest.mark.parametrize('status, response_code', (
    (REMINDER_STATUSES.sent, 201),
    (REMINDER_STATUSES.scheduled, 400),
))
def test_reminder_create_ignoring_sent(su_client, status, response_code):
    user = f.get_superuser()
    message = f.create_note()
    f.ReminderFactory(
        message=message,
        user=user,
        status=status,
        remind_at=shifted_now(days=-1),
    )
    data = {
        'remind_at': (shifted_now(days=1)).strftime('%Y-%m-%d %H:%M:%S'),
        'message': message.id,
    }
    url = reverse('api:reminders:create')
    response = su_client.post(url, data)
    assert response.status_code == response_code


def test_reminder_delete(su_client):
    message = f.create_note()
    reminder = f.ReminderFactory(message=message, user=f.get_superuser())
    url = reverse('api:reminders:detail', kwargs={'pk': reminder.id})
    assert message.reminders.count() == 1
    response = su_client.delete(url)
    assert response.status_code == 200
    assert set(response.data.keys()) == {
        'id',
        'actions',
    }
    assert message.reminders.count() == 0


def test_reminder_delete_restriction(su_client):
    message = f.create_note()
    reminder = f.ReminderFactory(message=message, user=f.create_user())
    url = reverse('api:reminders:detail', kwargs={'pk': reminder.id})
    response = su_client.delete(url)
    assert response.status_code == 403


def test_reminder_update_form(su_client):
    message = f.create_note()
    reminder = f.ReminderFactory(message=message, user=f.get_superuser())
    url = reverse('api:reminders:update-form', kwargs={'pk': reminder.id})
    response = su_client.get(url)
    assert response.status_code == 200


def test_reminder_update(su_client):
    message = f.create_note()
    reminder = f.ReminderFactory(message=message, user=f.get_superuser())
    data = {
        'remind_at': (shifted_now(days=2)).strftime('%Y-%m-%d %H:%M:%S'),
    }
    url = reverse('api:reminders:detail', kwargs={'pk': reminder.id})
    response = su_client.put(url, data)
    assert response.status_code == 200
    assert set(response.data.keys()) == {
        'id',
        'user',
        'remind_at',
        'status',
        'message',
    }


@pytest.mark.parametrize('data', (
    {'remind_at': (shifted_now(days=-1)).strftime('%Y-%m-%d %H:%M:%S')},
    {'remind_at': 'some text'},
    {},
))
def test_reminder_update_data_validation(su_client, data):
    message = f.create_note()
    reminder = f.ReminderFactory(message=message, user=f.get_superuser())
    url = reverse('api:reminders:detail', kwargs={'pk': reminder.id})
    response = su_client.put(url, data)
    assert response.status_code == 400
