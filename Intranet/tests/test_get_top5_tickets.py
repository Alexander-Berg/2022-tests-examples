# coding: utf-8

from __future__ import unicode_literals

from mock import patch

from utils import handle_utterance, get_tickets_example

tickets_jsons, tickets_text = get_tickets_example()


def test_get_top5_tikcets_1(uid, tg_app):
    with patch('uhura.lib.callbacks.get_top5_tickets') as m:
        m.return_value = []
        handle_utterance(tg_app, uid, 'мои тикеты', 'Тикетов нет')


def test_get_top5_tikcets_2(uid, tg_app):
    with patch('uhura.lib.callbacks.get_top5_tickets') as m:
        m.return_value = None
        handle_utterance(
            tg_app,
            uid,
            'мои тикеты',
            'У меня что-то пошло не так при запросе к стартреку... Ох, давай попробуем еще раз чуток попозже?'
        )


def test_get_top5_tikcets_3(uid, tg_app):
    with patch('uhura.lib.callbacks.get_top5_tickets') as m:
        m.return_value = tickets_jsons
        text = '5 последних обновленных тикетов, где ты исполнитель:\n' + tickets_text
        handle_utterance(tg_app, uid, 'мои тикеты', text)
