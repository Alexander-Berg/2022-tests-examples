# coding: utf-8

from __future__ import unicode_literals

from mock import patch

from utils import handle_utterance, get_car_owner

CONFIRM_KEYBOARD = ['Да', 'Нет']
ASK_ANOTHER_PHRASE = 'Если ты запер кого-либо еще, введи, пожалуйста, еще один номер машины:)'
CANCEL_PHRASE = 'Не стала отправлять письмо'
FIRST_PHRASE = 'Напиши мне, пожалуйста, номер транспортного средства (можно только цифры) или пришли фотографию номера'
SENT_MAIL_PHRASE = 'Отправила письмо'


def test_notify_car_owner_not_found(uid, tg_app):
    with patch('uhura.external.suggest.find_car_owner') as m:
        m.return_value = []
        handle_utterance(tg_app, uid, 'запер машину', FIRST_PHRASE, cancel_button=True)
        handle_utterance(tg_app, uid, 'а123аа', 'Извини, не нашла никаких транспортных средств по такому запросу:(')


def test_block_car__cancel(uid, tg_app):
    with patch('uhura.external.suggest.find_car_owner') as m:
        text = '''А123АА (model)
Имя Фамилия @telegram +79990008877

Отправить письмо?'''
        m.return_value = [get_car_owner()]
        handle_utterance(tg_app, uid, 'запер машину', FIRST_PHRASE, cancel_button=True)
        handle_utterance(tg_app, uid, 'а123аа', text, CONFIRM_KEYBOARD)
        handle_utterance(tg_app, uid, 'нет', CANCEL_PHRASE)


def test_block_car__success(uid, tg_app, mailoutbox):
    with patch('uhura.lib.callbacks.get_person_info') as m2, \
            patch('uhura.external.suggest.find_car_owner') as m1:
        m2.return_value = get_car_owner()
        text = '''А123АА (model)
Имя Фамилия @telegram +79990008877

Отправить письмо?'''
        m1.return_value = [get_car_owner()]
        handle_utterance(tg_app, uid, 'запер машину', FIRST_PHRASE, cancel_button=True)
        handle_utterance(tg_app, uid, 'а123аа', text, CONFIRM_KEYBOARD)
        handle_utterance(tg_app, uid, 'да', [SENT_MAIL_PHRASE, ASK_ANOTHER_PHRASE])
        m = mailoutbox[0]
        assert m.subject == '[DEVELOPMENT] Твою машину А123АА заперли на парковке'
        assert m.body == '''Привет!

Тебя на парковке сегодня запер Имя Фамилия login@
Ты можешь с ним связаться:
telegram: @telegram
телефон: +79990008877
почта: login@yandex-team.ru
или ответить на это письмо.

--------------------------
Ухура'''
        assert m.from_email == 'Uhura <robot-uhura@yandex-team.ru>'
        assert list(m.to) == ['login@yandex-team.ru']
        assert list(m.bcc) == ['uhura-bcc@yandex-team.ru']
        assert list(m.reply_to) == ['robot-uhura@yandex-team.ru']


def test_block_car__found_some(uid, tg_app):
    with patch('uhura.lib.callbacks.get_person_info') as m2, \
            patch('uhura.external.suggest.find_car_owner') as m:
        m2.return_value = get_car_owner()
        person1 = get_car_owner()
        person2 = get_car_owner(plate='В456ВВ')
        m.return_value = [person1, person2]
        result = 'Нашла несколько транспортных средств по такому запросу, выбери номер, пожалуйста:\n\nА123АА ' \
                 '(model)\nИмя Фамилия @telegram +79990008877\n\nВ456ВВ (model)\nИмя Фамилия @telegram +79990008877'
        handle_utterance(tg_app, uid, 'запер машину', FIRST_PHRASE, cancel_button=True)
        handle_utterance(tg_app, uid, 'а123аа', result, ['А123АА', 'В456ВВ'], cancel_button=True)
        m.return_value = [person1]
        result = '''А123АА (model)
Имя Фамилия @telegram +79990008877

Отправить письмо?'''
        handle_utterance(tg_app, uid, 'а123аа', result, CONFIRM_KEYBOARD)


def test_block_car__no_ellipsis(uid, tg_app):
    with patch('uhura.external.suggest.find_car_owner') as m, \
            patch('uhura.lib.callbacks.get_person_info') as m2:
        m.return_value = [get_car_owner()]
        m2.return_value = get_car_owner()

        expected_text = '''А123АА (model)
Имя Фамилия @telegram +79990008877

Отправить письмо?'''
        handle_utterance(tg_app, uid, 'я запер а123ее', expected_text, CONFIRM_KEYBOARD)
        handle_utterance(tg_app, uid, 'да', [SENT_MAIL_PHRASE, ASK_ANOTHER_PHRASE])


def test_block_car__two_cars(uid, tg_app):
    with patch('uhura.external.suggest.find_car_owner') as m, \
            patch('uhura.lib.callbacks.get_person_info') as m2:
        m.return_value = [get_car_owner()]
        m2.return_value = get_car_owner()

        expected_text = '''А123АА (model)
Имя Фамилия @telegram +79990008877

Отправить письмо?'''
        handle_utterance(tg_app, uid, 'я запер а123ее', expected_text, CONFIRM_KEYBOARD)
        handle_utterance(tg_app, uid, 'да', [SENT_MAIL_PHRASE, ASK_ANOTHER_PHRASE])
        handle_utterance(tg_app, uid, 'к456но', expected_text, CONFIRM_KEYBOARD)
        handle_utterance(tg_app, uid, 'Нет', CANCEL_PHRASE)


def test_block_car__two_owners(uid, tg_app):
    with patch('uhura.external.suggest.find_car_owner') as m, \
            patch('uhura.lib.callbacks.get_person_info') as m2:
        owner1 = get_car_owner()
        owner2 = get_car_owner(name='Фамилия Имя')
        m.return_value = [owner1, owner2]
        m2.return_value = get_car_owner()

        expected_text = '''У машины несколько владельцев:

А123АА (model)
Имя Фамилия @telegram +79990008877
Фамилия Имя @telegram +79990008877

Отправить им письмо?'''
        handle_utterance(tg_app, uid, 'я запер а123ее', expected_text, CONFIRM_KEYBOARD)
        handle_utterance(tg_app, uid, 'да', [SENT_MAIL_PHRASE, ASK_ANOTHER_PHRASE])
        handle_utterance(tg_app, uid, 'к456но', expected_text, CONFIRM_KEYBOARD)
        handle_utterance(tg_app, uid, 'Нет', CANCEL_PHRASE)
