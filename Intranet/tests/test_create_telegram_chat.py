# coding: utf-8

from __future__ import unicode_literals

import mock
import pytest

from utils import handle_utterance

CONFIRM_KEYBOARD = ['Да', 'Нет']


def test_create_chat_success(uid, tg_app):
    # create chat test
    handle_utterance(tg_app, uid, 'Создай чат  пожалуйста', 'Введи, пожалуйста, название чата', cancel_button=True)
    title = '1234'
    expected_message = (
        'Создать чат "%s"? Пожалуйста, нажимай "Да", только если действительно собираешься '
        'пользоваться созданным чатом для обсуждения рабочих вопросов: количество чатов, за '
        'которыми бот безопасности может следить единовременно, ограничено.'
    ) % title
    handle_utterance(tg_app, uid, title, expected_message, CONFIRM_KEYBOARD)
    expected_message = (
        'Я создала чат "%s" и сделала тебя в нём админом.😊 Теперь он отображается в твоем списке '
        'чатов.'
    ) % title
    expected_url = 'https://tasha.test.yandex-team.ru/tashatesttoolsbot/telegram/createchat/'
    expected_data = {
        'telegram_id': str(uid),
        'title': '1234',
        'telegram_username': 'UhuraAppBot',
    }
    with mock.patch('uhura.external.intranet.post_request') as pr_post,\
         mock.patch('uhura.lib.tasha.get_request') as pr_get,\
         mock.patch('uhura.lib.tasha.get_tvm_service_headers') as tvm_mock:
        tvm_mock.return_value = {}
        pr_get.return_value = ['TashaTestToolsBot']
        pr_post.return_value = {'status': 'success'}
        handle_utterance(tg_app, uid, 'Да', expected_message)
        pr_post.assert_called_with(expected_url, data=expected_data, timeout=10, headers={})


@pytest.mark.parametrize('bots_banned', ['single', 'all'])
def test_create_chat_choose_min_not_overflow_bot(uid, tg_app, bots_banned):
    def tuvok_fails_response(url, data, timeout, **kwargs):
        if url == 'https://tasha.test.yandex-team.ru/tuvoktoolsbot/telegram/createchat/':
            return None
        else:
            return {'status': 'success'}
    # create chat test
    handle_utterance(tg_app, uid, 'Создай чат  пожалуйста', 'Введи, пожалуйста, название чата', cancel_button=True)
    title = '1234'
    expected_message = (
        'Создать чат "%s"? Пожалуйста, нажимай "Да", только если действительно собираешься '
        'пользоваться созданным чатом для обсуждения рабочих вопросов: количество чатов, за '
        'которыми бот безопасности может следить единовременно, ограничено.'
    ) % title
    handle_utterance(tg_app, uid, title, expected_message, CONFIRM_KEYBOARD)
    if bots_banned == 'single':
        expected_message = (
            'Я создала чат "%s" и сделала тебя в нём админом.😊 Теперь он отображается в твоем списке '
            'чатов.'
        ) % title
    else:
        expected_message = 'При создании чата произошла ошибка,🤔 попробуй пожалуйста еще раз через минуту.'
    # bot with minimum of chats but not overflowed
    expected_url = 'https://tasha.test.yandex-team.ru/worftoolsbot/telegram/createchat/'
    expected_data = {
        'telegram_id': str(uid),
        'title': '1234',
        'telegram_username': 'UhuraAppBot',
    }
    with mock.patch('uhura.external.intranet.post_request') as pr_post, \
         mock.patch('uhura.lib.tasha.get_request') as pr_get, \
         mock.patch('uhura.lib.tasha.get_tvm_service_headers') as tvm_mock:
        tvm_mock.return_value = {}
        pr_get.return_value = ['TuvokToolsBot', 'WorfToolsBot']
        if bots_banned == 'single':
            pr_post.side_effect = tuvok_fails_response
        else:
            pr_post.return_value = None
        handle_utterance(tg_app, uid, 'Да', expected_message)
        assert pr_post.call_count == 2
        pr_post.assert_called_with(expected_url, data=expected_data, timeout=10, headers={})


def test_create_chat_no_tasha_statistics(uid, tg_app):
    title = '1234'
    handle_utterance(tg_app, uid, 'Создай чат  пожалуйста', 'Введи, пожалуйста, название чата', cancel_button=True)
    expected_message = (
        'Создать чат "%s"? Пожалуйста, нажимай "Да", только если действительно собираешься '
        'пользоваться созданным чатом для обсуждения рабочих вопросов: количество чатов, за '
        'которыми бот безопасности может следить единовременно, ограничено.'
    ) % title
    handle_utterance(tg_app, uid, title, expected_message, CONFIRM_KEYBOARD)
    expected_message = 'При создании чата произошла ошибка,🤔 попробуй пожалуйста еще раз через минуту.'
    with mock.patch('uhura.lib.tasha.get_request') as pr_get,\
         mock.patch('uhura.lib.tasha.get_tvm_service_headers'):
        pr_get.return_value = ['TuvokToolsBot', 'WorfToolsBot']
        handle_utterance(tg_app, uid, 'Да', expected_message)


def test_do_not_create_chat(uid, tg_app):
    title = '12345'
    handle_utterance(tg_app, uid, 'Создай чат  пожалуйста', 'Введи, пожалуйста, название чата', cancel_button=True)
    expected_message = (
        'Создать чат "%s"? Пожалуйста, нажимай "Да", только если действительно собираешься '
        'пользоваться созданным чатом для обсуждения рабочих вопросов: количество чатов, за '
        'которыми бот безопасности может следить единовременно, ограничено.'
    ) % title
    handle_utterance(tg_app, uid, title, expected_message, CONFIRM_KEYBOARD)
    expected_message = 'Как скажешь!'
    handle_utterance(tg_app, uid, 'Нет', expected_message)


def test_privacy_error(uid, tg_app):
    # create chat test
    handle_utterance(tg_app, uid, 'Создай чат  пожалуйста', 'Введи, пожалуйста, название чата', cancel_button=True)
    title = '1234'
    expected_message = (
                           'Создать чат "%s"? Пожалуйста, нажимай "Да", только если действительно собираешься '
                           'пользоваться созданным чатом для обсуждения рабочих вопросов: количество чатов, за '
                           'которыми бот безопасности может следить единовременно, ограничено.'
                       ) % title
    handle_utterance(tg_app, uid, title, expected_message, CONFIRM_KEYBOARD)
    expected_message = (
                           'У тебя слишком строгие настройки приватности, поэтому я не могу добавить тебя в чат. '
                           'Пожалуйста, ослабь настройки приватности, а затем попробуй создать чат еще раз. '
                           'После того, как чат будет создан, ты сможешь поменять настройки обратно.'
                       )
    expected_url = 'https://tasha.test.yandex-team.ru/tashatesttoolsbot/telegram/createchat/'
    expected_data = {
        'telegram_id': str(uid),
        'title': '1234',
        'telegram_username': 'UhuraAppBot',
    }
    with mock.patch('uhura.external.intranet.post_request') as pr_post,\
         mock.patch('uhura.lib.tasha.get_request') as pr_get,\
         mock.patch('uhura.lib.tasha.get_tvm_service_headers') as tvm_mock:
        tvm_mock.return_value = {}
        pr_get.return_value = ['TashaTestToolsBot']
        pr_post.return_value = {'status': 'privacy_error'}
        handle_utterance(tg_app, uid, 'Да', expected_message)
        pr_post.assert_called_with(expected_url, data=expected_data, timeout=10, headers={})
