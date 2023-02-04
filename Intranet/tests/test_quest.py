# coding: utf-8

from __future__ import unicode_literals

import mock

from utils import handle_utterance, YES_NO_KEYBOARD

FINISH_PHRASE = 'Поздравляю! Ты справился со всеми заданиями'
FIRST_PHRASE = 'Привет! Это квест по внутренним сервисам Яндекса. Тебе нужно будет отвечать на вопросы' \
               ' и выполнять задания: пройдя все задания ты получишь что-нибудь хорошее!'
TASK_PHRASE = 'Вот твое задание:'
NEXT_TASK_PHRASE = 'Правильно! Следующее задание:'
PAUSE_PHRASE = 'Ты можешь вернуться к этому вопросу в любое время, сказав "квест"'
RETRY_PHRASE = 'Тогда напомню тебе вопрос:'


class TestWhereIsHural(object):
    TASK_PHRASE = 'Выбери переговорку, в которой проходит Хурал в Москве'
    WRONG_ANSWER_PHRASE = 'Нет. Хочешь попробовать еще раз?'
    ANSWERS = ['Синий Кит', 'Седьмое Небо', 'Еще переговорка', 'Еще одна переговорка']

    def test_wrong_answer_dont_retry(self, uid, tg_app, monkeypatch):
        monkeypatch.setattr('django.conf.settings.QUEST_TASKS', ['quest_where_is_hural'])
        handle_utterance(tg_app, uid, 'квест', [FIRST_PHRASE, TASK_PHRASE, self.TASK_PHRASE], self.ANSWERS,
                         cancel_button=True)
        handle_utterance(tg_app, uid, 'какая-то переговорка', self.WRONG_ANSWER_PHRASE, YES_NO_KEYBOARD)
        handle_utterance(tg_app, uid, 'Нет', PAUSE_PHRASE)
        handle_utterance(tg_app, uid, 'привет', 'Привет.')

    def test_wrong_answer_retry(self, uid, tg_app, monkeypatch):
        monkeypatch.setattr('django.conf.settings.QUEST_TASKS', ['quest_where_is_hural'])
        handle_utterance(tg_app, uid, 'квест', [FIRST_PHRASE, TASK_PHRASE, self.TASK_PHRASE], self.ANSWERS,
                             cancel_button=True)
        handle_utterance(tg_app, uid, 'какая-то переговорка', self.WRONG_ANSWER_PHRASE, YES_NO_KEYBOARD)
        handle_utterance(tg_app, uid, 'Да', [RETRY_PHRASE, self.TASK_PHRASE], self.ANSWERS, cancel_button=True)

    def test_right_answer_next_task(self, uid, tg_app, monkeypatch):
        monkeypatch.setattr('django.conf.settings.QUEST_TASKS', ['quest_where_is_hural', 'quest_create_ticket'])
        handle_utterance(tg_app, uid, 'квест', [FIRST_PHRASE, TASK_PHRASE, self.TASK_PHRASE], self.ANSWERS,
                             cancel_button=True)
        handle_utterance(
            tg_app, uid, 'Синий кит', [NEXT_TASK_PHRASE, TestCreateTicket.TASK_PHRASE], cancel_button=True
        )

    def test_right_answer_finish(self, uid, tg_app, monkeypatch):
        monkeypatch.setattr('django.conf.settings.QUEST_TASKS', ['quest_where_is_hural'])
        handle_utterance(tg_app, uid, 'квест', [FIRST_PHRASE, TASK_PHRASE, self.TASK_PHRASE], self.ANSWERS,
                         cancel_button=True)
        handle_utterance(tg_app, uid, 'Синий кит', FINISH_PHRASE)


class TestCreateTicket(object):
    TASK_PHRASE = 'Трекер: создай тикет в очереди UHURAQUEST, и пришли мне его ключ'
    WRONG_AUTHOR_PHRASE = 'Этот тикет создал не ты. Хочешь попробовать еще раз?'
    WRONG_FORMAT_PHRASE = 'Это не похоже на ключ тикета в очереди UHURAQUEST. Хочешь попробовать еще?'

    def test_wrong_answer_dont_retry(self, uid, tg_app, monkeypatch):
        monkeypatch.setattr('django.conf.settings.QUEST_TASKS', ['quest_create_ticket'])
        monkeypatch.setattr('uhura.external.startrek.get_ticket', lambda args, **kwargs: {'author': 'xxx'})
        handle_utterance(tg_app, uid, 'квест', [FIRST_PHRASE, TASK_PHRASE, self.TASK_PHRASE], cancel_button=True)
        handle_utterance(tg_app, uid, 'UHURAQUEST-2', self.WRONG_AUTHOR_PHRASE, YES_NO_KEYBOARD)
        handle_utterance(tg_app, uid, 'Нет', PAUSE_PHRASE)
        handle_utterance(tg_app, uid, 'привет', 'Привет.')

    def test_wrong_answer_retry(self, uid, tg_app, monkeypatch):
        monkeypatch.setattr('django.conf.settings.QUEST_TASKS', ['quest_create_ticket'])
        monkeypatch.setattr('uhura.external.startrek.get_ticket', lambda args, **kwargs: {'author': 'xxx'})
        handle_utterance(tg_app, uid, 'квест', [FIRST_PHRASE, TASK_PHRASE, self.TASK_PHRASE], cancel_button=True)
        handle_utterance(tg_app, uid, 'UHURAQUEST-2', self.WRONG_AUTHOR_PHRASE, YES_NO_KEYBOARD)
        handle_utterance(tg_app, uid, 'Да', [RETRY_PHRASE, self.TASK_PHRASE], cancel_button=True)

    def test_right_answer_next_task(self, uid, tg_app, monkeypatch):
        monkeypatch.setattr('django.conf.settings.QUEST_TASKS', ['quest_create_ticket', 'quest_where_is_hural'])
        monkeypatch.setattr('uhura.external.startrek.get_ticket', lambda args, **kwargs: {'author': 'robot-uhura'})
        handle_utterance(tg_app, uid, 'квест', [FIRST_PHRASE, TASK_PHRASE, self.TASK_PHRASE], cancel_button=True)
        handle_utterance(
            tg_app,
            uid,
            'UHURAQUEST-2',
            [NEXT_TASK_PHRASE, TestWhereIsHural.TASK_PHRASE],
            TestWhereIsHural.ANSWERS,
            cancel_button=True
        )

    def test_right_answer_finish(self, uid, tg_app, monkeypatch):
        monkeypatch.setattr('django.conf.settings.QUEST_TASKS', ['quest_create_ticket'])
        monkeypatch.setattr('uhura.external.startrek.get_ticket', lambda args, **kwargs: {'author': 'robot-uhura'})
        handle_utterance(tg_app, uid, 'квест', [FIRST_PHRASE, TASK_PHRASE, self.TASK_PHRASE], cancel_button=True)
        handle_utterance(tg_app, uid, 'UHURAQUEST-2', FINISH_PHRASE)

    def test_wrong_format_dont_retry(self, uid, tg_app, monkeypatch):
        monkeypatch.setattr('django.conf.settings.QUEST_TASKS', ['quest_create_ticket'])
        handle_utterance(tg_app, uid, 'квест', [FIRST_PHRASE, TASK_PHRASE, self.TASK_PHRASE], cancel_button=True)
        handle_utterance(tg_app, uid, 'UHURAQUES-2', self.WRONG_FORMAT_PHRASE, YES_NO_KEYBOARD)
        handle_utterance(tg_app, uid, 'Нет', PAUSE_PHRASE)
        handle_utterance(tg_app, uid, 'привет', 'Привет.')

    def test_wrong_format_retry(self, uid, tg_app, monkeypatch):
        monkeypatch.setattr('django.conf.settings.QUEST_TASKS', ['quest_create_ticket'])
        handle_utterance(tg_app, uid, 'квест', [FIRST_PHRASE, TASK_PHRASE, self.TASK_PHRASE], cancel_button=True)
        handle_utterance(tg_app, uid, 'UHURAQUES-2', self.WRONG_FORMAT_PHRASE, YES_NO_KEYBOARD)
        handle_utterance(tg_app, uid, 'Да', [RETRY_PHRASE, self.TASK_PHRASE], cancel_button=True)
