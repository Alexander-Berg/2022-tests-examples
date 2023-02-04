# coding: utf-8

from __future__ import unicode_literals

import pytest

from uhura import models
from uhura.tests.utils import handle_utterance, create_inline_keyboard


@pytest.fixture
def test_task(uid, ):
    user = models.User.objects.create(uid=1, username='username1')
    subscription = models.Subscription.objects.create(
        name='name', description='description', description_template='template', task_name='task'
    )
    models.PeriodicNotification.objects.create(user=user, subscription=subscription, time='12:00', params={})


def test_get_all_subscriptions__error(uid, tg_app):
    handle_utterance(
        tg_app,
        uid,
        'подписки',
        'Я могу регулярно отправлять тебе сообщения по выбранным тобой темам '
        '(например, дайджест твоих встреч). Ты также можешь настроить регулярность'
        ' подписок или отказаться от них в любой момент.',
        ['Создать подписку'],
        cancel_button=True
    )
    handle_utterance(
        tg_app,
        uid,
        'Создать подписку',
        'Не могу получить список доступных подписок. Попробуй через минуту',
        inline_keyboards=[None]
    )


def test_get_my_subscriptions__no_subscriptions(uid, tg_app):
    handle_utterance(
        tg_app,
        uid,
        'подписки',
        'Я могу регулярно отправлять тебе сообщения по выбранным тобой темам '
        '(например, дайджест твоих встреч). Ты также можешь настроить регулярность'
        ' подписок или отказаться от них в любой момент.',
        ['Создать подписку'],
        cancel_button=True
    )
    handle_utterance(
        tg_app,
        uid,
        'Удалить подписку',
        'У тебя нет ни одной подписки',
        inline_keyboards=[None]
    )


def test_get_my_subscriptions__ok(uid, tg_app, test_task):
    handle_utterance(
        tg_app,
        uid,
        'Создать подписку',
        ['Сейчас ты можешь подписаться на:', 'name\n\ndescription'],
        inline_keyboards=[None, create_inline_keyboard('Добавить', 'Подписка на name')]
    )
