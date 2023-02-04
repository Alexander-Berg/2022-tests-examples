# coding: utf-8

from __future__ import unicode_literals

import pytest
from mock import patch

from uhura import models
from utils import handle_utterance, get_car_owner

pytestmark = pytest.mark.django_db(transaction=True)


CONFIRM_KEYBOARD = ['Да', 'Нет']


def test_unblock_car__no_blocked_cars(uid, tg_app):
    handle_utterance(
        tg_app,
        uid,
        'отпер',
        'Сегодня на парковке ты никого не запирал! Чтобы сообщить о том, что ты кого-то запер,'
        ' напиши мне "я запер %номер машины%"'
    )


def test_unblock_car__error(uid, tg_app):
    with patch('uhura.external.suggest.find_car_owner') as m:
        m.return_value = [get_car_owner()]
        models.User.objects.update(blocked_cars_plates=['a123aa'])
        handle_utterance(tg_app, uid, 'отпер', '''Сегодня на парковке ты запирал:

А123АА (model)
Имя Фамилия @telegram +79990008877

Отправить этим сотрудникам уведомления о том, что ты их отпёр?''', ['Да', 'Нет'])
        handle_utterance(tg_app, uid, 'да', 'Что-то пошло не так. Попробуй через минуту')


def test_unblock_car__success(uid, tg_app, mailoutbox):
    with patch('uhura.external.suggest.find_car_owner') as m, \
            patch('uhura.lib.callbacks.get_person_info') as m1:
        m.return_value = [get_car_owner()]
        m1.return_value = get_car_owner()
        models.User.objects.update(blocked_cars_plates=['a123aa'])
        handle_utterance(
            tg_app,
            uid,
            'отпер',
            '''Сегодня на парковке ты запирал:

А123АА (model)
Имя Фамилия @telegram +79990008877

Отправить этим сотрудникам уведомления о том, что ты их отпёр?''', ['Да', 'Нет'])
        handle_utterance(tg_app, uid, 'да', 'Отправлено, можно ехать!')
        assert len(models.User.objects.get(uid=uid).blocked_cars_plates) == 0
        assert mailoutbox[0].subject == '[DEVELOPMENT] Твою машину a123aa отпёрли на парковке'
        assert mailoutbox[0].body == '''Привет!

Имя Фамилия login@ только что отпер твою машину на парковке, теперь ты можешь беспрепятственно уехать!

--------------------------
Ухура'''
        assert mailoutbox[0].to == ['login@yandex-team.ru']
