# coding: utf-8

from __future__ import unicode_literals

import mock
import pytest
from mock import patch

from uhura import models
from utils import handle_utterance, create_req_info, _staff_get_person_data

CANCEL_KEYBOARD = ['Отмена']


def _patch_staff_get_person_data(phone):
    data = _staff_get_person_data()
    data['official']['affiliation'] = 'external'
    return data


def test_not_authorized_cancel(uid, tg_app):
    models.User.objects.all().delete()
    expected = (
        'Добавь, пожалуйста, свой telegram username в профиль на Стаффе или поделись со мной номером '
        'телефона, который известен Стаффу. Мои разработчики запретили мне разговаривать с незнакомцами!'
    )
    handle_utterance(tg_app, uid, 'привет', expected, request_user_contact=True, cancel_button=True)
    expected2 = 'Окей, если снова захочешь попробовать авторизоваться, напиши мне;)'
    handle_utterance(tg_app, uid, 'отмена', expected2)


def test_not_authorized_cool_hacker(uid, tg_app):
    models.User.objects.all().delete()
    expected = (
        'Добавь, пожалуйста, свой telegram username в профиль на Стаффе или поделись со мной номером '
        'телефона, который известен Стаффу. Мои разработчики запретили мне разговаривать с незнакомцами!'
    )
    handle_utterance(tg_app, uid, 'привет', expected, request_user_contact=True, cancel_button=True)

    req_info = create_req_info(uid, '')
    req_info.additional_options['contact_info'] = {
        'first_name': u'Natasha',
        'last_name': u'TestYar',
        'phone_number': '79852038971',
        'user_id': int(uid) + 1,  # another uid
    }
    response = tg_app.handle_request(req_info)
    assert response.messages == [
        'Ты, конечно, супер-хакер!:) Но, чтобы авторизоваться через номер телефона, '
        'нужно поделиться своим номером телефона!'
    ]


def test_not_authorized_unknown_user(uid, tg_app):
    models.User.objects.all().delete()
    expected = (
        'Добавь, пожалуйста, свой telegram username в профиль на Стаффе или поделись со мной номером '
        'телефона, который известен Стаффу. Мои разработчики запретили мне разговаривать с незнакомцами!'
    )
    handle_utterance(tg_app, uid, 'привет', expected, request_user_contact=True, cancel_button=True)

    req_info = create_req_info(uid, '')
    req_info.additional_options['contact_info'] = {
        'first_name': u'Natasha',
        'last_name': u'TestYar',
        'phone_number': '79852038971',
        'user_id': int(uid),  # same uid
    }
    response = tg_app.handle_request(req_info)
    assert response.messages == [
        'Прости, но все еще не могу понять кто ты. Мои разработчики запретили мне разговаривать с незнакомцами!'
    ]


@pytest.mark.parametrize('phone', ['79852038971', '+79-852-03-89 7-1'])
def test_ok_authorization(uid, tg_app, monkeypatch, phone):
    old_username = models.TelegramUsername.objects.get(telegram_id=uid)
    old_username.user = None
    old_username.save()
    user = models.User.objects.get()
    user.phone = '+79852038971'
    user.save()
    expected = (
        'Добавь, пожалуйста, свой telegram username в профиль на Стаффе или поделись со мной номером '
        'телефона, который известен Стаффу. Мои разработчики запретили мне разговаривать с незнакомцами!'
    )
    handle_utterance(tg_app, uid, 'привет', expected, request_user_contact=True, cancel_button=True)
    db_objects = models.User.objects.filter(phone='+79852038971', telegram_usernames__telegram_id=uid).count()
    assert db_objects == 0

    req_info = create_req_info(uid, '')
    req_info.additional_options['contact_info'] = {
        'first_name': u'Natasha',
        'last_name': u'TestYar',
        'phone_number': phone,
        'user_id': int(uid),  # same uid
        'official': {
            'is_dismissed': False,
        },
    }
    with mock.patch('uhura.models.post_request'), mock.patch('uhura.models.get_tvm_service_headers'):
        response = tg_app.handle_request(req_info)
    assert response.messages == [
        'Ура! Теперь я тебя узнала;)'
    ]
    old_username.refresh_from_db()
    assert old_username.user is not None
    assert old_username.created_with_uhura


def test_not_permitted_auth_request(uid, tg_app):
    expected = 'Поделись, пожалуйста, со мной номером телефона, который известен Стаффу'
    handle_utterance(tg_app, uid, 'привязать телефон', expected, request_user_contact=True, cancel_button=True)
    handle_utterance(tg_app, uid, 'отмена', 'Ну ок!')


def test_affiliation_external(uid, tg_app, monkeypatch):
    old_username = models.TelegramUsername.objects.get(telegram_id=uid)
    old_username.user = None
    old_username.save()
    user = models.User.objects.get()
    user.phone = '+79852038971'
    user.save()
    expected = (
        'Добавь, пожалуйста, свой telegram username в профиль на Стаффе или поделись со мной номером '
        'телефона, который известен Стаффу. Мои разработчики запретили мне разговаривать с незнакомцами!'
    )
    handle_utterance(tg_app, uid, 'привет', expected, request_user_contact=True, cancel_button=True)
    db_objects = models.User.objects.filter(telegram_usernames__telegram_id=uid, phone='+79852038971').count()
    assert db_objects == 0

    req_info = create_req_info(uid, '')
    req_info.additional_options['contact_info'] = {
        'first_name': u'Natasha',
        'last_name': u'TestYar',
        'phone_number': '79852038971',
        'user_id': int(uid),  # same uid
        'official': {
            'is_dismissed': False,
        },
    }
    with mock.patch('uhura.models.post_request'), mock.patch('uhura.models.get_tvm_service_headers'):
        response = tg_app.handle_request(req_info)
    assert response.messages == [
        'Ура! Теперь я тебя узнала;)'
    ]
    db_objects = models.User.objects.filter(telegram_usernames__telegram_id=uid, phone='+79852038971').count()
    assert db_objects == 1

    expected = (
        'Твой номер уже аутентифицирован. Пока я не могу больше ничего рассказывать '
        'outstaff-сотрудникам, но надеюсь, что меня научат:)'
    )
    with patch('uhura.app.can_use_uhura') as can_use_uhura:
        can_use_uhura.return_value = False
        handle_utterance(tg_app, uid, 'привет', expected, request_user_contact=True, cancel_button=True)
