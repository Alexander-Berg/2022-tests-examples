# coding: utf-8
from __future__ import unicode_literals

from mock import patch

from utils import handle_utterance


def test_get_last_online_1(uid, tg_app):
    with patch('uhura.external.suggest.find_person') as m:
        m.return_value = [{'login': 'login'}], 'person_answer'
        with patch('uhura.external.staff.get_last_online') as m1:
            m1.return_value = 'текст'
            handle_utterance(tg_app, uid, 'последняя активность login', 'текст')


def test_get_last_online_2(uid, tg_app):
    with patch('uhura.external.suggest.find_person') as m:
        m.return_value = [{'login': 'login'}], 'person_answer'
        with patch('uhura.external.intranet.get_request') as m1:
            m1.return_value = 'jsonp({});'
            handle_utterance(
                tg_app,
                uid,
                'последний онлайн Николай Третьяк',
                'Нет данных о последней активности сотрудника'
            )


def test_get_last_online_3(uid, tg_app):
    with patch('uhura.external.suggest.find_person') as m:
        m.return_value = [{'login': 'login'}], 'person_answer'
        with patch('uhura.external.staff.get_last_online') as m1:
            m1.side_effect = Exception
            handle_utterance(
                tg_app,
                uid,
                'последний онлайн фамилия',
                'Ой все! ничего не вышло. Попробуй пожалуйста еще разок'
            )


def test_get_last_online_4(uid, tg_app):
    with patch('uhura.external.suggest.find_person') as m:
        m.return_value = ([], 'person_specify')
        handle_utterance(
            tg_app,
            uid,
            'последняя активность непонятно',
            'Ох! нашла слишком много сотрудников. Можешь сказать точнее, кто тебе нужен?'
            ' Мне поможет сочетание имени и фамилии или логин!'
        )


def test_get_last_online_5(uid, tg_app):
    with patch('uhura.external.suggest.find_person') as m:
        m.return_value = ([], 'person_not_found')
        handle_utterance(tg_app, uid, 'последний онлайн имя', 'Я не нашла такого сотрудника')


def test_get_last_online_6(uid, tg_app):
    handle_utterance(
        tg_app,
        uid,
        'последний онлайн любые три слова',
        'Я так не умею искать. Дай мне либо имя и фамилию, либо логин, пожалуйста'
    )
