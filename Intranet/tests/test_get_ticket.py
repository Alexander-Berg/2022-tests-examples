# coding: utf-8

from __future__ import unicode_literals

from mock import patch, call

from utils import handle_utterance


def test_get_ticket_forbidden(uid, tg_app):
    with patch('uhura.external.startrek.get_ticket') as m:
        m.return_value = None
        handle_utterance(tg_app, uid, 'тикет uhura-100', 'У меня нет доступа к тикету или такого тикета не существует')
        m.assert_has_calls([call('UHURA-100')])


def test_get_ticket_success(uid, tg_app):
    with patch('uhura.external.startrek.get_ticket') as m:
        m.return_value = {'description': 'описание', 'summary': 'название'}
        handle_utterance(tg_app, uid, 'тикет uhura-100', 'название')
        m.assert_has_calls([call('UHURA-100')])


def test_get_ticket_error(uid, tg_app):
    with patch('uhura.external.startrek.get_ticket') as m:
        m.side_effect = Exception
        handle_utterance(tg_app, uid, 'тикет uhura-100', 'Не могу связаться со стартреком. Попробуй через минуту')
        m.assert_has_calls([call('UHURA-100')])


def test_get_ticket_with_ellipsis(uid, tg_app):
    with patch('uhura.external.startrek.get_ticket') as m:
        m.return_value = {'description': 'описание', 'summary': 'название'}
        handle_utterance(tg_app, uid, 'тикет', 'Скажи ключ тикета', cancel_button=True)
        handle_utterance(tg_app, uid, 'uhura-100', 'название')
        m.assert_has_calls([call('UHURA-100')])


def test_get_ticket_wrong_format(uid, tg_app):
    handle_utterance(tg_app, uid, 'тикет uhura', 'Это не похоже на ключ тикета')
