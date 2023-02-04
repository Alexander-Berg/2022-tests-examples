# coding: utf-8

from __future__ import unicode_literals

from mock import patch

from utils import handle_utterance, get_profile_data


def test_get_vacation__success(uid, tg_app):
    with patch('uhura.external.staff.get_profile') as m:
        m.return_value = get_profile_data()
        handle_utterance(
            tg_app,
            uid,
            'сколько отпуска',
            'У тебя 2 дня отпуска и 0 отгулов'
        )


def test_get_vacation__error(uid, tg_app):
    with patch('uhura.external.staff.get_profile') as m:
        m.return_value = None
        handle_utterance(
            tg_app,
            uid,
            'сколько отпуска',
            'Не могу связаться со стаффом. Попробуй через минуту'
        )


def test_get_vacation__negative_number(uid, tg_app):
    with patch('uhura.external.staff.get_profile') as m:
        profile_data = get_profile_data()
        profile_data['vacation'] = -100
        m.return_value = profile_data
        handle_utterance(
            tg_app,
            uid,
            'сколько отпуска',
            'У тебя -100 дней отпуска и 0 отгулов'
        )
