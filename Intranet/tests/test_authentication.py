# coding: utf-8

from __future__ import unicode_literals

from uhura import models
from utils import handle_utterance


def test_username_authorization(uid, tg_app, monkeypatch):
    old_username = models.TelegramUsername.objects.get(telegram_id=uid)
    old_username.telegram_id = None
    old_username.save()
    expected = (
        'Привет.'
    )
    handle_utterance(tg_app, uid, 'привет', expected)
    old_username.refresh_from_db()
    assert old_username.telegram_id == int(uid)


def test_username_authorization_another_telegram_id_exists(uid, tg_app, monkeypatch):
    models.TelegramUsername.objects.filter(telegram_id=uid).update(username='finn', user=None)
    user = models.User.objects.create(
        username='uhuraappbot',
        uid=uid + '1',
    )
    models.TelegramUsername.objects.create(
        user=user,
        username='uhuraappbot',
    )
    expected = 'Привет.'
    handle_utterance(tg_app, uid, 'привет', expected)
    assert models.TelegramUsername.objects.get(user=user).telegram_id == int(uid)
