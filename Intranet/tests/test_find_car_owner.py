# coding: utf-8

from __future__ import unicode_literals

from mock import patch

from utils import handle_utterance, get_car_owner


def test_find_car_owner_with_ask(uid, tg_app):
    with patch('uhura.external.suggest.find_car_owner') as m:
        m.return_value = None
        expected = (
            'Напиши мне, пожалуйста, номер транспортного средства (можно только цифры) или пришли фотографию номера'
        )
        handle_utterance(tg_app, uid, 'чей велосипед', expected, cancel_button=True)
        handle_utterance(
            tg_app,
            uid,
            'а123аа',
            'При запросе номера что-то пошло не так, спроси, пожалуйста, меня еще раз попозже'
        )


def test_find_car_owner_error(uid, tg_app):
    with patch('uhura.external.suggest.find_car_owner') as m:
        m.return_value = None
        handle_utterance(
            tg_app,
            uid,
            'чья машина а001аа',
            'При запросе номера что-то пошло не так, спроси, пожалуйста, меня еще раз попозже'
        )


def test_find_car_owner__bicycle(uid, tg_app):
    with patch('uhura.external.suggest.find_car_owner') as m:
        m.return_value = None
        handle_utterance(
            tg_app,
            uid,
            'чей велосипед 123456',
            'При запросе номера что-то пошло не так, спроси, пожалуйста, меня еще раз попозже'
        )


def test_find_car_owner_not_found(uid, tg_app):
    with patch('uhura.external.suggest.find_car_owner') as m:
        m.return_value = {}
        handle_utterance(
            tg_app, uid, 'чья машина а123аа', 'Извини, не нашла никаких транспортных средств по такому запросу:('
        )


def test_find_car_owner_found_one(uid, tg_app):
    with patch('uhura.external.suggest.find_car_owner') as m,\
            patch('uhura.external.staff.get_last_online') as m1, \
            patch('uhura.lib.callbacks.get_person_info') as m2:
        person = get_car_owner()
        m.return_value = [person]
        m1.return_value = 'текст'
        m2.return_value = person
        result = '''<a href="https://staff.yandex-team.ru/login">Имя Фамилия</a> login@
Транспортное средство: А123АА (model)
+79990008877
Телеграм: @telegram
Офис
Этаж
Стол: <a href="https://staff.yandex-team.ru/map/#/table/1234/">1234</a>
текст'''
        handle_utterance(tg_app, uid, 'чья машина 123', ['По запросу "123" нашла:', result])


def test_find_car_owner_found_some(uid, tg_app):
    with patch('uhura.external.suggest.find_car_owner') as m,\
            patch('uhura.external.staff.get_last_online') as m1, \
            patch('uhura.lib.callbacks.get_person_info') as m2:
        person1 = get_car_owner()
        person2 = get_car_owner(plate='В456ВВ')
        m.return_value = [person1, person2]
        result = 'Нашла несколько транспортных средств по такому запросу, выбери номер, пожалуйста:\n\nА123АА ' \
                 '(model)\nИмя Фамилия @telegram +79990008877\n\nВ456ВВ (model)\nИмя Фамилия @telegram +79990008877'
        handle_utterance(tg_app, uid, 'чья машина 123', result, ['А123АА', 'В456ВВ'], cancel_button=True)
        m.return_value = [person1]
        m1.return_value = 'текст'
        m2.return_value = person1
        result = '''<a href="https://staff.yandex-team.ru/login">Имя Фамилия</a> login@
Транспортное средство: А123АА (model)
+79990008877
Телеграм: @telegram
Офис
Этаж
Стол: <a href="https://staff.yandex-team.ru/map/#/table/1234/">1234</a>
текст'''
        handle_utterance(tg_app, uid, 'а123аа', ['По запросу "а123аа" нашла:', result])
