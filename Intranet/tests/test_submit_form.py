# coding: utf-8

from __future__ import unicode_literals

from freezegun import freeze_time
from mock import patch

from utils import handle_utterance, get_form_json

CONFIRM_KEYBOARD = ['Да', 'Нет']


@freeze_time('2017-12-27')
def test_submit__form(uid, tg_app):
    with patch('uhura.external.forms.Form.submit') as m1, patch('uhura.external.intranet.get_request') as m2:
        m2.return_value = get_form_json()
        m1.return_value = 'ok'
        handle_utterance(
            tg_app, uid, 'парковка',
            'БЦ парковки\n\n<i>В Мамонтове используется парковка только на разгрузку</i>',
            ['Морозов', 'Мамонтов', 'Строганов', 'Аврора'],
            cancel_button=True
        )
        handle_utterance(tg_app, uid, 'Мамонтов', 'Дата парковки', cancel_button=True)
        handle_utterance(tg_app, uid, 'сегодня', 'Время начала и окончания парковки', cancel_button=True)
        handle_utterance(
            tg_app, uid, 'анпгршщозл', 'Марка, номер автомобиля', ['Перейти к следующему вопросу'], cancel_button=True
        )


def test_submit_form__cancel(uid, tg_app):
    with patch('uhura.external.forms.Form.submit') as m1, patch('uhura.external.intranet.get_request') as m2:
        m2.return_value = get_form_json()
        m1.return_value = 'ok'
        handle_utterance(
            tg_app, uid, 'парковка',
            'БЦ парковки\n\n<i>В Мамонтове используется парковка только на разгрузку</i>',
            ['Морозов', 'Мамонтов', 'Строганов', 'Аврора'],
            cancel_button=True
        )
        handle_utterance(tg_app, uid, 'Мамонтов', 'Дата парковки', cancel_button=True)
        handle_utterance(tg_app, uid, 'Отмена', 'Как скажешь!')


@freeze_time('2017-12-27')
def test_submit_form__skip(uid, tg_app):
    with patch('uhura.external.forms.Form.submit') as m1, patch('uhura.external.intranet.get_request') as m2:
        m2.return_value = get_form_json()
        m1.return_value = 'ok'
        handle_utterance(
            tg_app,
            uid,
            'парковка',
            'БЦ парковки\n\n<i>В Мамонтове используется парковка только на разгрузку</i>',
            ['Морозов', 'Мамонтов', 'Строганов', 'Аврора'],
            cancel_button=True
        )
        handle_utterance(tg_app, uid, 'Мамонтов', 'Дата парковки', cancel_button=True)
        handle_utterance(tg_app, uid, '12.05.2018', 'Время начала и окончания парковки', cancel_button=True)
        handle_utterance(
            tg_app, uid, 'хзлзхлхз', 'Марка, номер автомобиля', ['Перейти к следующему вопросу'], cancel_button=True
        )
        handle_utterance(tg_app, uid, 'Перейти к следующему вопросу', '''Отправить форму?

БЦ парковки: Мамонтов
Дата парковки: 2018-05-12
Время начала и окончания парковки: хзлзхлхз''', ['Да', 'Нет'])


@freeze_time('2017-12-27')
def test_submit_form__wrong_input(uid, tg_app):
    with patch('uhura.external.forms.Form.submit') as m1, patch('uhura.external.intranet.get_request') as m2:
        m2.return_value = get_form_json()
        m1.return_value = 'ok'
        handle_utterance(
            tg_app,
            uid,
            'парковка',
            'БЦ парковки\n\n<i>В Мамонтове используется парковка только на разгрузку</i>',
            ['Морозов', 'Мамонтов', 'Строганов', 'Аврора'],
            cancel_button=True
        )
        handle_utterance(tg_app, uid, 'Мамонтов', 'Дата парковки', cancel_button=True)
        handle_utterance(tg_app, uid, 'хзлзхлхз', 'Некорректный ввод. Хочешь попробовать еще?', ['Да', 'Нет'])
        handle_utterance(tg_app, uid, 'Да', 'Дата парковки', cancel_button=True)


@freeze_time('2017-12-27')
def test_submit_form__wrong_input__cancel(uid, tg_app):
    with patch('uhura.external.forms.Form.submit') as m1, patch('uhura.external.intranet.get_request') as m2:
        m2.return_value = get_form_json()
        m1.return_value = 'ok'
        handle_utterance(
            tg_app,
            uid,
            'парковка',
            'БЦ парковки\n\n<i>В Мамонтове используется парковка только на разгрузку</i>',
            ['Морозов', 'Мамонтов', 'Строганов', 'Аврора'],
            cancel_button=True
        )
        handle_utterance(tg_app, uid, 'Мамонтов', 'Дата парковки', cancel_button=True)
        handle_utterance(tg_app, uid, 'хзлзхлхз', 'Некорректный ввод. Хочешь попробовать еще?', ['Да', 'Нет'])
        handle_utterance(tg_app, uid, 'Нет', 'Не стала отправлять форму')
        handle_utterance(tg_app, uid, 'Нет', 'ОК')
