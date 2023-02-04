# coding: utf-8

from __future__ import unicode_literals

from mock import patch

from utils import handle_utterance


def test_get_mobcert_password_1(uid, tg_app):
    with patch('uhura.external.bot.get_mobcert_password') as m1:
        m1.return_value = 'abcdefgh'
        handle_utterance(tg_app, uid, 'пароль от mobcert', ['Пароль от mobcert:', 'abcdefgh'])


def test_get_mobcert_password_2(uid, tg_app):
    with patch('uhura.external.bot.get_mobcert_password') as m1:
        m1.side_effect = Exception
        handle_utterance(
            tg_app, uid, 'пароль от mobcert',
            'Не получилось запросить пароль, попробуй, пожалуйста, еще раз'
        )
