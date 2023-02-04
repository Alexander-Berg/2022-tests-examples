# coding: utf-8

from __future__ import unicode_literals

import mock
import pytest

from uhura.lib import tasha
from utils import handle_utterance


def test_cancel_invite(uid, tg_app):
    expected_message = (
        'Пришли, пожалуйста, ссылку на чат/канал, в который ты хочешь пригласить одного из ботов '
        'безопасности. Пожалуйста, приглашай бота, только если действительно собираешься пользоваться '
        'этим чатом/каналом для обсуждения рабочих вопросов: количество чатов/каналов, за которыми бот '
        'безопасности может следить одновременно, ограничено.'
    )
    handle_utterance(tg_app, uid, 'пригласи Ташу в чат', expected_message, cancel_button=True)
    expected_message = 'Как скажешь!'
    handle_utterance(tg_app, uid, 'отмена', expected_message)


def test_no_chats_slots(uid, tg_app):
    link = 'https://t.me/joinchat/Anc'
    with pytest.raises(ValueError, message='Chats slots overflow'),\
         mock.patch('uhura.lib.tasha.get_request'),\
         mock.patch('uhura.lib.tasha.get_tvm_service_headers'):
        tasha.invite_to_chat_by_link(link)

    expected_message = (
        'Пришли, пожалуйста, ссылку на чат/канал, в который ты хочешь пригласить одного из ботов '
        'безопасности. Пожалуйста, приглашай бота, только если действительно собираешься пользоваться '
        'этим чатом/каналом для обсуждения рабочих вопросов: количество чатов/каналов, за которыми бот '
        'безопасности может следить одновременно, ограничено.'
    )
    handle_utterance(tg_app, uid, 'пригласи Ташу в чат', expected_message, cancel_button=True)
    expected_message = 'Произошла ошибка,🤔 попробуй пожалуйста еще раз через минуту.'
    handle_utterance(tg_app, uid, link, expected_message)


def test_network_error(uid, tg_app):
    link = 'https://t.me/joinchat/Anc'
    expected_message = (
        'Пришли, пожалуйста, ссылку на чат/канал, в который ты хочешь пригласить одного из ботов '
        'безопасности. Пожалуйста, приглашай бота, только если действительно собираешься пользоваться '
        'этим чатом/каналом для обсуждения рабочих вопросов: количество чатов/каналов, за которыми бот '
        'безопасности может следить одновременно, ограничено.'
    )
    handle_utterance(tg_app, uid, 'пригласи Ташу в чат', expected_message, cancel_button=True)
    expected_message = 'Произошла ошибка,🤔 попробуй пожалуйста еще раз через минуту.'
    expected_url = 'https://tasha.test.yandex-team.ru/worftoolsbot/telegram/invitetochat/'
    expected_data = {'invite_link': link}
    with mock.patch('uhura.external.intranet.post_request') as pr_post, \
         mock.patch('uhura.lib.tasha.get_request') as pr_get, \
         mock.patch('uhura.lib.tasha.get_tvm_service_headers') as tvm_mock:
        tvm_mock.return_value = {}
        pr_get.return_value = ['WorfToolsBot']
        pr_post.return_value = None
        handle_utterance(tg_app, uid, link, expected_message)
        pr_post.assert_called_with(expected_url, data=expected_data, timeout=30, headers={})

        with pytest.raises(ValueError, message='failed to add any of bots'):
            tasha.invite_to_chat_by_link(link)


def test_success(uid, tg_app):
    link = 'https://t.me/joinchat/Anc'
    expected_message = (
        'Пришли, пожалуйста, ссылку на чат/канал, в который ты хочешь пригласить одного из ботов '
        'безопасности. Пожалуйста, приглашай бота, только если действительно собираешься пользоваться '
        'этим чатом/каналом для обсуждения рабочих вопросов: количество чатов/каналов, за которыми бот '
        'безопасности может следить одновременно, ограничено.'
    )
    handle_utterance(tg_app, uid, 'пригласи Ташу в чат', expected_message, cancel_button=True)
    expected_message = 'Успех! Бот безопасности @TuvokToolsBot приглашен в чат 🤓'
    expected_url = 'https://tasha.test.yandex-team.ru/tuvoktoolsbot/telegram/invitetochat/'
    expected_data = {'invite_link': link}
    with mock.patch('uhura.external.intranet.post_request') as pr_post,\
         mock.patch('uhura.lib.tasha.get_request') as pr_get,\
         mock.patch('uhura.lib.tasha.get_tvm_service_headers') as tvm_mock:
        tvm_mock.return_value = {}
        pr_get.return_value = ['TuvokToolsBot']
        pr_post.return_value = {'status': 'success'}
        handle_utterance(tg_app, uid, link, expected_message)
        pr_post.assert_called_with(expected_url, data=expected_data, timeout=30, headers={})


def test_already_participant(uid, tg_app):
    link = 'https://t.me/joinchat/Anc'
    expected_message = (
        'Пришли, пожалуйста, ссылку на чат/канал, в который ты хочешь пригласить одного из ботов '
        'безопасности. Пожалуйста, приглашай бота, только если действительно собираешься пользоваться '
        'этим чатом/каналом для обсуждения рабочих вопросов: количество чатов/каналов, за которыми бот '
        'безопасности может следить одновременно, ограничено.'
    )
    handle_utterance(tg_app, uid, 'пригласи Ташу в чат', expected_message, cancel_button=True)
    expected_message = 'Один из ботов безопасности уже есть в чате 👍'
    expected_url = 'https://tasha.test.yandex-team.ru/tuvoktoolsbot/telegram/invitetochat/'
    expected_data = {'invite_link': link}
    with mock.patch('uhura.external.intranet.post_request') as pr_post, \
         mock.patch('uhura.lib.tasha.get_request') as pr_get, \
         mock.patch('uhura.lib.tasha.get_tvm_service_headers') as tvm_mock:
        tvm_mock.return_value = {}
        pr_get.return_value = ['TuvokToolsBot']
        pr_post.return_value = {'status': 'already_participant'}
        handle_utterance(tg_app, uid, link, expected_message)
        pr_post.assert_called_with(expected_url, data=expected_data, timeout=30, headers={})


def test_expired_link_or_banned(uid, tg_app):
    link = 'https://t.me/joinchat/Anc'
    expected_message = (
        'Пришли, пожалуйста, ссылку на чат/канал, в который ты хочешь пригласить одного из ботов '
        'безопасности. Пожалуйста, приглашай бота, только если действительно собираешься пользоваться '
        'этим чатом/каналом для обсуждения рабочих вопросов: количество чатов/каналов, за которыми бот '
        'безопасности может следить одновременно, ограничено.'
    )
    handle_utterance(tg_app, uid, 'пригласи Ташу в чат', expected_message, cancel_button=True)
    expected_message = (
        'Invite-ссылка невалидна, либо @TuvokToolsBot забанен в чате/канале. Пожалуйста, обнови '
        'invite-ссылку на чат, и проверь, что @TuvokToolsBot не забанен в чате/канале (а если забанен, '
        'разбань пожалуйста) и попробуй еще раз.'
    )
    expected_url = 'https://tasha.test.yandex-team.ru/tuvoktoolsbot/telegram/invitetochat/'
    expected_data = {'invite_link': link}
    with mock.patch('uhura.external.intranet.post_request') as pr_post, \
         mock.patch('uhura.lib.tasha.get_request') as pr_get, \
         mock.patch('uhura.lib.tasha.get_tvm_service_headers') as tvm_mock:
        tvm_mock.return_value = {}
        pr_get.return_value = ['TuvokToolsBot']
        pr_post.return_value = {'status': 'expired_link_or_banned'}
        handle_utterance(tg_app, uid, link, expected_message)
        pr_post.assert_called_with(expected_url, data=expected_data, timeout=30, headers={})


@pytest.mark.parametrize('bots_banned', ['single', 'all'])
def test_create_chat_choose_min_not_overflow_bot(uid, tg_app, bots_banned):
    def tuvok_fails_response(url, data, timeout, **kwargs):
        if url == 'https://tasha.test.yandex-team.ru/tuvoktoolsbot/telegram/invitetochat/':
            return None
        else:
            return {'status': 'success'}

    # test success
    link = 'https://t.me/joinchat/Anc'
    expected_message = (
        'Пришли, пожалуйста, ссылку на чат/канал, в который ты хочешь пригласить одного из ботов '
        'безопасности. Пожалуйста, приглашай бота, только если действительно собираешься пользоваться '
        'этим чатом/каналом для обсуждения рабочих вопросов: количество чатов/каналов, за которыми бот '
        'безопасности может следить одновременно, ограничено.'
    )
    handle_utterance(tg_app, uid, 'пригласи Ташу в чат', expected_message, cancel_button=True)
    if bots_banned == 'single':
        expected_message = 'Успех! Бот безопасности @WorfToolsBot приглашен в чат 🤓'
        expected_url = 'https://tasha.test.yandex-team.ru/worftoolsbot/telegram/invitetochat/'
    else:
        expected_message = 'Произошла ошибка,🤔 попробуй пожалуйста еще раз через минуту.'
        expected_url = 'https://tasha.test.yandex-team.ru/paveltoolsbot/telegram/invitetochat/'
    expected_data = {'invite_link': link}
    with mock.patch('uhura.external.intranet.post_request') as pr_post, \
         mock.patch('uhura.lib.tasha.get_request') as pr_get, \
         mock.patch('uhura.lib.tasha.get_tvm_service_headers') as tvm_mock:
        tvm_mock.return_value = {}
        pr_get.return_value = ['TuvokToolsBot', 'WorfToolsBot', 'PavelToolsBot']
        if bots_banned == 'single':
            pr_post.side_effect = tuvok_fails_response
        else:
            pr_post.return_value = None
        handle_utterance(tg_app, uid, link, expected_message)

        assert pr_post.call_count == {'single': 2, 'all': 3}[bots_banned]

        pr_post.assert_called_with(expected_url, data=expected_data, timeout=30, headers={})
