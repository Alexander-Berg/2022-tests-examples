# coding: utf-8

from __future__ import unicode_literals

from mock import patch

from utils import handle_utterance, get_tickets_example

tickets_jsons, tickets_text = get_tickets_example()


def test_get_tickets_inProgress_1(uid, tg_app):
    with patch('uhura.lib.callbacks.get_tickets_inProgress') as m:
        m.return_value = []
        handle_utterance(tg_app, uid, 'что у меня в работе', 'Тикетов в работе не нашла')


def test_get_tickets_inProgress_2(uid, tg_app):
    with patch('uhura.lib.callbacks.get_tickets_inProgress') as m:
        m.return_value = None
        handle_utterance(
            tg_app,
            uid,
            'что у меня в работе',
            'У меня что-то пошло не так при запросе к стартреку... Ох, давай попробуем еще раз чуток попозже?'
        )


def test_get_tickets_inProgress_3(uid, tg_app):
    with patch('uhura.lib.callbacks.get_tickets_inProgress') as m:
        m.return_value = tickets_jsons
        handle_utterance(tg_app, uid, 'что у меня в работе', tickets_text)
