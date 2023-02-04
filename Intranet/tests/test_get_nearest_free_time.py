# coding: utf-8

from __future__ import unicode_literals

from freezegun import freeze_time
from mock import patch

from utils import handle_utterance


def test_get_nearest_free_time__error(uid, tg_app):
    with patch('uhura.external.suggest.find_person') as m, \
            patch('uhura.external.calendar.get_meetings') as m1:
        m.return_value = [{'login': 'login', 'name': 'Николай Третьяк', 'uid': 1234567890}], 'person_answer'
        m1.return_value = None
        handle_utterance(
            tg_app, uid, 'когда свободен login', 'Не смогла связаться с календарем. Попробуй через минуту'
        )


def test_get_nearest_free_time__always_free(uid, tg_app):
    with patch('uhura.external.suggest.find_person') as m, \
            patch('uhura.external.calendar.get_meetings') as m1, \
            patch('uhura.lib.callbacks.get_person_info') as m2:
        m.return_value = [{'login': 'login', 'name': 'Николай Третьяк', 'uid': 1234567890}], 'person_answer'
        m1.return_value = []
        m2.return_value = {'gender': 'male'}
        handle_utterance(
            tg_app, uid, 'когда свободен login', 'В ближайшие сутки Николай Третьяк не занят'
        )


@freeze_time('2017-01-01T12:00:00')
def test_get_nearest_free_time__success_only_start(uid, tg_app):
    with patch('uhura.external.suggest.find_person') as m, \
            patch('uhura.external.calendar.get_meetings') as m1, \
            patch('uhura.lib.callbacks.get_person_info') as m2:
        m.return_value = [{'login': 'login', 'name': 'Марина Камалова', 'uid': 1234567890}], 'person_answer'
        m1.return_value = [{'startTs': '2017-01-01T11:30:00', 'endTs': '2017-01-01T12:30:00'}]
        m2.return_value = {'gender': 'female'}
        handle_utterance(
            tg_app, uid, 'когда свободен login', 'В ближайшее время Марина Камалова будет свободна с 12:30'
        )


@freeze_time('2017-01-01T12:00:00')
def test_get_nearest_free_time__success_start_and_end(uid, tg_app):
    with patch('uhura.external.suggest.find_person') as m, \
            patch('uhura.external.calendar.get_meetings') as m1, \
            patch('uhura.lib.callbacks.get_person_info') as m2:
        m.return_value = [{'login': 'login', 'name': 'Александр Кошелев', 'uid': 1234567890}], 'person_answer'
        m1.return_value = [
            {'startTs': '2017-01-01T11:30:00', 'endTs': '2017-01-01T12:30:00'},
            {'startTs': '2017-01-01T13:30:00', 'endTs': '2017-01-01T14:00:00'}
        ]
        m2.return_value = {'gender': 'male'}
        handle_utterance(
            tg_app,
            uid,
            'когда свободен login',
            'В ближайшее время Александр Кошелев будет свободен с 12:30 до 13:30'
        )
