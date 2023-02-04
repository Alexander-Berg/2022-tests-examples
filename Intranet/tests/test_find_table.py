# coding: utf-8

from __future__ import unicode_literals

import ids
from mock import patch

from utils import handle_utterance


def test_find_table_staff_not_found(uid, tg_app):
    with patch('uhura.external.suggest.find_meeting_room_or_table') as m, \
            patch('uhura.external.staff.get_person_info') as m1:
        m.return_value = [{'login': 'login'}], 'person_answer'
        e = ids.exceptions.BackendError()
        e.status_code = 404
        m1.side_effect = e
        handle_utterance(
            tg_app, uid, 'где login', 'Извини, по запросу "login" я не нашла ни сотрудников, ни переговорок:('
        )


def test_find_table_error(uid, tg_app):
    with patch('uhura.external.suggest.find_meeting_room_or_table') as m, \
            patch('uhura.external.staff.get_person_info') as m1:
        m.return_value = [{'login': 'login'}], None
        m1.side_effect = Exception
        handle_utterance(
            tg_app,
            uid,
            'где Николай Третьяк',
            'У меня проблемы на линии связи:( попробуй еще раз in short time'
        )


def test_find_table_found_many(uid, tg_app):
    with patch('uhura.external.suggest.find_meeting_room_or_table') as m:
        m.return_value = ([], 'person_specify')
        handle_utterance(
            tg_app,
            uid,
            'где login',
            'Ох! нашла слишком много сотрудников. Можешь сказать точнее, кто тебе нужен?'
            ' Мне поможет сочетание имени и фамилии или логин!')


def test_find_table_suggest_not_found(uid, tg_app):
    with patch('uhura.external.suggest.find_meeting_room_or_table') as m:
        m.return_value = ([], 'not_found')
        handle_utterance(
            tg_app, uid, 'где ndtretyak', 'Извини, по запросу "ndtretyak" я не нашла ни сотрудников, ни переговорок:('
        )


def test_find_table_success(uid, tg_app):
    with patch('uhura.external.suggest.find_meeting_room_or_table') as m, \
            patch('uhura.external.staff.get_person_info') as m1, \
            patch('uhura.external.staff.get_last_online') as m2, \
            patch('uhura.external.gap.get_gaps') as m3, \
            patch('uhura.external.calendar.get_meetings') as m4, \
            patch('uhura.external.staff.get_uid_by_login') as m5:
        m.return_value = [{'login': 'login'}], 'person_answer'
        m1.return_value = {
            'name': 'Имя Фамилия',
            'login': 'login',
            'phone': '+79999999999',
            'telegram_username': ['telegram'],
            'table': {
                'id': 1111,
                'floor': {
                    'office': {
                        'name': {
                            'ru': 'Офис'
                        }
                    },
                    'name': {
                        'ru': 'Этаж'
                    }
                }
            },
            'work_phone': 1234
        }
        rendered_text = '''<a href="https://staff.yandex-team.ru/login">Имя Фамилия</a> login@
+79999999999
Телеграм: @telegram
Внутренний телефон: 1234
Офис
Этаж
Стол: <a href="https://staff.yandex-team.ru/map/#/table/1111/">1111</a>
онлайн'''
        m2.return_value = 'онлайн'
        m3.return_value = []
        m4.return_value = []
        m5.return_value = 'login'
        handle_utterance(tg_app, uid, 'где login', rendered_text)
