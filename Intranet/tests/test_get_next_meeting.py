# coding: utf-8

from __future__ import unicode_literals

from freezegun import freeze_time
from mock import patch

from utils import handle_utterance, get_event_example

EVENT, RENDERED_EVENT = get_event_example()


def test_get_next_meeting_error(uid, tg_app):
    with patch('uhura.lib.callbacks.get_next_meeting') as m:
        m.return_value = None
        handle_utterance(
            tg_app,
            uid,
            'следующая встреча',
            'Прости, но у меня не получилось запросить информацию о твоих встречах. Попробуй еще раз через минуту!'
        )


@freeze_time('2017-01-01')
def test_get_next_meeting_succees(uid, tg_app):
    with patch('uhura.lib.callbacks.get_next_meeting') as m:
        m.return_value = [EVENT]
        handle_utterance(tg_app, uid, 'следующая встреча', RENDERED_EVENT)


def test_get_next_meeting_no_meetings(uid, tg_app):
    with patch('uhura.lib.callbacks.get_next_meeting') as m:
        m.return_value = {}
        handle_utterance(
            tg_app,
            uid,
            'следующая встреча',
            'Кажется, у тебя пока нет встреч на этой неделе! (не то что у меня)'
        )
