# coding: utf-8

from __future__ import unicode_literals

from utils import handle_utterance

POSSIBLE_ANSWER = 'Не поняла тебя:( Ты можешь узнать, что я умею ("Help") ' \
                  'или отправить фидбэк моим разработчикам ("Feedback").'
FEEDBACK_KEYBOARD = ['Help', 'Feedback']
COMMANDS_LIST_KEYBOARD = [
    'Столовая', 'Парковка', 'Стартрек', 'Стафф', 'Wifi', 'Календарь/Гэп', 'Поиск', 'ДМС', 'Другое'
]


def test_feedback_feedback(uid, tg_app):
    handle_utterance(tg_app, uid, 'хзщшрдло', POSSIBLE_ANSWER, FEEDBACK_KEYBOARD, cancel_button=True)
    handle_utterance(tg_app, uid, 'Feedback', 'Введи сообщение', cancel_button=True)
    handle_utterance(
        tg_app,
        uid,
        'нмшгищштьз',
        'Ура! Отправила твоё сообщение. Его обязательно прочитают, а значит я научусь чему-то новому!'
    )


def test_feedback_help(uid, tg_app):
    handle_utterance(tg_app, uid, 'хзщшрдло', POSSIBLE_ANSWER, FEEDBACK_KEYBOARD, cancel_button=True)
    handle_utterance(tg_app, uid, 'Help', 'Про что хочешь узнать подробнее?', COMMANDS_LIST_KEYBOARD)
