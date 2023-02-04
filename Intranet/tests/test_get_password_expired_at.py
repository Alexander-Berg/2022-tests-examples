# coding: utf-8

from __future__ import unicode_literals

from mock import patch

from utils import handle_utterance, get_profile_data


def test_get_password_expired_at_success(uid, tg_app):
    with patch('uhura.external.staff.get_profile') as m:
        m.return_value = get_profile_data()
        handle_utterance(
            tg_app,
            uid,
            'доменный пароль',
            'Действие доменного пароля истекает 3 Января 2018. Его необходимо будет сменить через 27 дней.'
        )


def test_get_password_expired_at_error(uid, tg_app):
    with patch('uhura.external.staff.get_profile') as m:
        m.return_value = None
        handle_utterance(
            tg_app,
            uid,
            'доменный пароль',
            'Не могу связаться со стаффом. Попробуй через минуту'
        )
