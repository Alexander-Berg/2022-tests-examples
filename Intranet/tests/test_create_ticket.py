# coding: utf-8

from __future__ import unicode_literals

from mock import patch

from utils import handle_utterance

CONFIRM_KEYBOARD = ['Да', 'Нет']


def test_create_ticket_success(uid, tg_app):
    with patch('uhura.external.startrek.check_queue') as m:
        m.return_value = True
        with patch('uhura.external.startrek.create_ticket') as m1:
            m1.return_value = 'https://link.com'
            handle_utterance(tg_app, uid, 'создай тикет', 'Введи название очереди', cancel_button=True)
            handle_utterance(tg_app, uid, 'uhura', 'Введи название тикета', cancel_button=True)
            handle_utterance(tg_app, uid, 'сегодня', 'Введи описание тикета', cancel_button=True)
            handle_utterance(
                tg_app,
                uid,
                'завтра',
                'Очередь: UHURA\nНазвание: сегодня\nОписание: завтра\n\nХочешь создать такой тикет?',
                CONFIRM_KEYBOARD
            )
            handle_utterance(tg_app, uid, 'да', 'Тикет успешно создан: https://link.com')


def test_create_ticket__wrong_input(uid, tg_app):
    with patch('uhura.external.startrek.check_queue') as m:
        m.return_value = False
        handle_utterance(tg_app, uid, 'создай тикет', 'Введи название очереди', cancel_button=True)
        handle_utterance(
            tg_app,
            uid,
            'uhura',
            'К сожалению, я не могу создавать тикеты в этой очереди. Хочешь ввести другую?',
            CONFIRM_KEYBOARD
        )
        handle_utterance(tg_app, uid, 'if', 'Тогда попроси что-нибудь еще')


def test_create_ticket_last_step_error(uid, tg_app):
    with patch('uhura.external.startrek.check_queue') as m:
        m.return_value = True
        with patch('uhura.external.startrek.create_ticket') as m1:
            m1.return_value = None
            handle_utterance(tg_app, uid, 'создай тикет uhura', 'Введи название тикета', cancel_button=True)
            handle_utterance(tg_app, uid, 'сегодня', 'Введи описание тикета', cancel_button=True)
            handle_utterance(
                tg_app,
                uid,
                'завтра',
                'Очередь: UHURA\nНазвание: сегодня\nОписание: завтра\n\nХочешь создать такой тикет?',
                CONFIRM_KEYBOARD
            )
            handle_utterance(tg_app, uid, 'да', 'Не могу связаться со стартреком. Попробуй через минуту')


def test_create_ticket_not_confirmed(uid, tg_app):
    with patch('uhura.external.startrek.check_queue') as m:
        m.return_value = True
        handle_utterance(tg_app, uid, 'создай тикет', 'Введи название очереди', cancel_button=True)
        handle_utterance(tg_app, uid, 'uhura', 'Введи название тикета', cancel_button=True)
        handle_utterance(tg_app, uid, 'сегодня', 'Введи описание тикета', cancel_button=True)
        handle_utterance(
            tg_app,
            uid,
            'завтра',
            'Очередь: UHURA\nНазвание: сегодня\nОписание: завтра\n\nХочешь создать такой тикет?',
            CONFIRM_KEYBOARD
        )
        handle_utterance(tg_app, uid, 'нет', 'Не стала создавать тикет')


def test_create_ticket_not_permitted(uid, tg_app):
    with patch('uhura.external.startrek.check_queue') as m:
        m.return_value = False
        handle_utterance(tg_app, uid, 'создай тикет', 'Введи название очереди', cancel_button=True)
        handle_utterance(
            tg_app,
            uid,
            'uhura',
            'К сожалению, я не могу создавать тикеты в этой очереди. Хочешь ввести другую?',
            CONFIRM_KEYBOARD
        )
        handle_utterance(tg_app, uid, 'нет', 'Тогда попроси что-нибудь еще')


def test_create_ticket_not_permitted_retry(uid, tg_app):
    with patch('uhura.external.startrek.check_queue') as m:
        m.return_value = False
        handle_utterance(tg_app, uid, 'создай тикет', 'Введи название очереди', cancel_button=True)
        handle_utterance(
            tg_app,
            uid,
            'uhura',
            'К сожалению, я не могу создавать тикеты в этой очереди. Хочешь ввести другую?',
            CONFIRM_KEYBOARD
        )
        handle_utterance(tg_app, uid, 'да', 'Введи название очереди', cancel_button=True)
        handle_utterance(
            tg_app,
            uid,
            'uhura',
            'К сожалению, я не могу создавать тикеты в этой очереди. Хочешь ввести другую?',
            CONFIRM_KEYBOARD
        )
        handle_utterance(tg_app, uid, 'нет', 'Тогда попроси что-нибудь еще')


def test_create_ticket_queue_not_found(uid, tg_app):
    with patch('uhura.external.startrek.check_queue') as m:
        m.return_value = None
        handle_utterance(tg_app, uid, 'создай тикет', 'Введи название очереди', cancel_button=True)
        handle_utterance(
            tg_app, uid, 'uhura',
            'Не нашла такую очередь. Попробуешь ввести название еще раз?',
            CONFIRM_KEYBOARD
        )
        handle_utterance(tg_app, uid, 'нет', 'Тогда попроси что-нибудь еще')


def test_create_ticket_first_step_error(uid, tg_app):
    with patch('uhura.external.startrek.check_queue') as m:
        m.side_effect = Exception
        handle_utterance(tg_app, uid, 'создай тикет', 'Введи название очереди', cancel_button=True)
        handle_utterance(
            tg_app, uid, 'uhura',
            'Не могу связаться со стартреком. Хочешь попробовать еще раз?',
            CONFIRM_KEYBOARD
        )
        handle_utterance(tg_app, uid, 'нет', 'Тогда попроси что-нибудь еще')


def test_create_ticket_cancel(uid, tg_app):
    with patch('uhura.external.startrek.check_queue') as m:
        m.return_value = True
        handle_utterance(tg_app, uid, 'создай тикет', 'Введи название очереди', cancel_button=True)
        handle_utterance(tg_app, uid, 'uhura', 'Введи название тикета', cancel_button=True)
        handle_utterance(tg_app, uid, 'отмена', 'Как скажешь!')
