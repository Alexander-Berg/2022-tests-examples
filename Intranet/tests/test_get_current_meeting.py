# coding: utf-8
from __future__ import unicode_literals

from freezegun import freeze_time
from mock import patch

from utils import handle_utterance, get_event_example


def test_get_current_meeting_1(uid, tg_app):
    with patch('uhura.lib.callbacks.get_current_meeting') as m:
        m.return_value = None
        handle_utterance(tg_app, uid, 'где встреча?', 'Не получилось запросить встречи, попробуй еще раз через минуту')


def test_get_current_meeting_2(uid, tg_app):
    with patch('uhura.lib.callbacks.get_current_meeting') as m:
        m.return_value = {}
        with patch('uhura.lib.callbacks.get_next_meeting') as m1:
            m1.return_value = None
            handle_utterance(
                tg_app,
                uid,
                'где встреча?',
                'Не получилось запросить встречи, попробуй еще раз через минуту'
            )


def test_get_current_meeting_3(uid, tg_app):
    with patch('uhura.lib.callbacks.get_current_meeting') as m:
        m.return_value = {}
        with patch('uhura.lib.callbacks.get_next_meeting') as m1:
            m1.return_value = {}
            handle_utterance(tg_app, uid, 'где встреча?', 'Встреч в течение недели нет')


@freeze_time('2017-01-01')
def test_get_current_meeting_4(uid, tg_app):
    with patch('uhura.lib.callbacks.get_current_meeting') as m:
        m.return_value = {}
        with patch('uhura.lib.callbacks.get_next_meeting') as m1:
            event, rendered_event = get_event_example()
            m1.return_value = [event]
            handle_utterance(tg_app, uid, 'где встреча', 'Сейчас встреч нет. Следующая встреча:\n\n' + rendered_event)


@freeze_time('2017-01-01')
def test_get_current_meeting_5(uid, tg_app):
    with patch('uhura.lib.callbacks.get_current_meeting') as m:
        event, rendered_event = get_event_example()
        m.return_value = [event]
        handle_utterance(tg_app, uid, 'где встреча', rendered_event)
