# coding: utf-8

from __future__ import unicode_literals

from mock import patch, call

from uhura import models
from utils import handle_utterance, get_uid, create_req_info

CANCEL_KEYBOARD = ['Отмена']


def test_who_is_it_not_forwarded(uid, tg_app):
    handle_utterance(tg_app, uid, 'кто это?', 'Перешли мне сообщение нужного человека', cancel_button=True)
    handle_utterance(tg_app, uid, 'сообщение', 'Нужно было переслать сообщение')


def test_who_is_it_uid_not_found_db_not_found(uid, tg_app):
    handle_utterance(tg_app, uid, 'кто это?', 'Перешли мне сообщение нужного человека', cancel_button=True)
    req_info = create_req_info(uid, '')
    req_info.additional_options['forward_from'] = {
        'id': 123456789,
    }
    response = tg_app.handle_request(req_info)
    assert response.messages == [
        'Не нашла такого сотрудника на Стаффе'
    ]


def test_who_is_it_uid_staff_not_found(uid, tg_app):
    with patch('uhura.external.staff.get_person_data_by_userphone') as m:
        m.return_value = None
        another_uid = get_uid()
        handle_utterance(tg_app, uid, 'кто это?', 'Перешли мне сообщение нужного человека', cancel_button=True)
        req_info = create_req_info(uid, '')
        req_info.additional_options['forward_from'] = {
            'id': int(another_uid),
        }
        user = models.User.objects.create(uid=1, phone='+79999999999', username='username_1')
        models.TelegramUsername.objects.create(
            telegram_id=another_uid,
            user=user
        )
        response = tg_app.handle_request(req_info)
        m.assert_has_calls([call('+79999999999')])
        assert response.messages == [
            'Не нашла такого сотрудника на Стаффе'
        ]


def test_who_is_it_username(uid, tg_app):
    with patch('uhura.external.staff.get_last_online') as m1, patch('uhura.lib.callbacks.get_person_info') as m2:
        m1.return_value = 'онлайн'
        m2.return_value = {
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
        handle_utterance(tg_app, uid, 'кто это?', 'Перешли мне сообщение нужного человека', cancel_button=True)
        req_info = create_req_info(uid, '')
        req_info.additional_options['forward_from'] = {
            'id': 123456789,
            'username': 'login'
        }
        response = tg_app.handle_request(req_info)
        assert response.messages == [
            rendered_text
        ]
