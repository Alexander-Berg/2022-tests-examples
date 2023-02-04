# coding: utf-8

from __future__ import unicode_literals

from freezegun import freeze_time
from mock import patch

from uhura.external import gap
from utils import handle_utterance

CONFIRM_KEYBOARD = ['Да', 'Нет']


@freeze_time('2017-01-01')
def test_work_from_home_1(uid, tg_app):
    with patch('uhura.external.gap.create_gap') as m:
        m.return_value = 'ok'
        handle_utterance(tg_app, uid, 'работаю из дома', 'Создать отсутствие 2017-01-01?', CONFIRM_KEYBOARD)
        handle_utterance(tg_app, uid, 'да', 'Ура! Успех!')


@freeze_time('2017-01-01')
def test_work_from_home_2(uid, tg_app):
    with patch('uhura.external.gap.create_gap') as m:
        m.return_value = None
        handle_utterance(tg_app, uid, 'работаю из дома', 'Создать отсутствие 2017-01-01?', CONFIRM_KEYBOARD)
        handle_utterance(tg_app, uid, 'да', 'Что-то пошло не так, попробуй еще разок через минутку?')


@freeze_time('2017-01-01')
def test_work_from_home_3(uid, tg_app):
    with patch('uhura.external.gap.create_gap') as m:
        m.side_effect = gap.GapIntersectionError
        handle_utterance(tg_app, uid, 'работаю из дома', 'Создать отсутствие 2017-01-01?', CONFIRM_KEYBOARD)
        handle_utterance(tg_app, uid, 'да', 'У тебя уже есть отсутствие на этот день')


@freeze_time('2017-01-01')
def test_work_from_home_4(uid, tg_app):
    handle_utterance(tg_app, uid, 'работаю из дома', 'Создать отсутствие 2017-01-01?', CONFIRM_KEYBOARD)
    handle_utterance(tg_app, uid, 'нет', 'Ок, не буду создавать отсутствие')
