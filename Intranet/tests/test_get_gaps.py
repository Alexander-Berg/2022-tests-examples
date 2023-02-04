# coding: utf-8
from __future__ import unicode_literals

from freezegun import freeze_time
from mock import patch

from utils import handle_utterance, get_gap_example

GAP, RENDERED_GAP = get_gap_example()


@freeze_time('2017-01-01')
def test_get_gaps_2(uid, tg_app):
    with patch('uhura.external.suggest.find_person') as m:
        m.return_value = [{'login': 'login'}], 'person_answer'
        handle_utterance(
            tg_app,
            uid,
            'с 10 по 2 января',
            'Мне нужен корректный временной отрезок, а ты ввел некорректный. Я так не могу!'
        )


@freeze_time('2017-01-01')
def test_get_gaps_3(uid, tg_app):
    with patch('uhura.external.suggest.find_person') as m:
        m.return_value = [{'login': 'login'}], 'person_answer'
        with patch('uhura.lib.callbacks.get_gaps') as m1:
            m1.return_value = {}
            handle_utterance(tg_app, uid, 'отсутствия Александр Кошелев в понедельник', 'Отсутствий нет')


@freeze_time('2017-01-01')
def test_get_gaps_4(uid, tg_app):
    with patch('uhura.external.suggest.find_person') as m:
        m.return_value = [{'login': 'login'}], 'person_answer'
        with patch('uhura.lib.callbacks.get_gaps') as m1:
            m1.return_value = [GAP]
            handle_utterance(tg_app, uid, 'отсутствия Николай 5 января', RENDERED_GAP)


@freeze_time('2017-01-01')
def test_get_gaps_5(uid, tg_app):
    with patch('uhura.external.suggest.find_person') as m:
        m.return_value = ([], 'person_specify')
        handle_utterance(
            tg_app,
            uid,
            'отсутствия ndtretyak с понедельника по четверг',
            'Ох! нашла слишком много сотрудников. Можешь сказать точнее, кто тебе нужен?'
            ' Мне поможет сочетание имени и фамилии или логин!')


@freeze_time('2017-01-01')
def test_get_gaps_6(uid, tg_app):
    with patch('uhura.external.suggest.find_person') as m:
        m.return_value = ([], 'person_specify')
        handle_utterance(
            tg_app,
            uid,
            'отсутствия ndtretyak',
            'Введи период, за который нужно показать отсутствия (можно один день)',
            cancel_button=True
        )
        handle_utterance(
            tg_app,
            uid,
            '16 марта',
            'Ох! нашла слишком много сотрудников. Можешь сказать точнее, кто тебе нужен?'
            ' Мне поможет сочетание имени и фамилии или логин!')


@freeze_time('2017-01-01')
def test_get_gaps_7(uid, tg_app):
    with patch('uhura.external.suggest.find_person') as m:
        m.return_value = ([], 'person_not_found')
        handle_utterance(
            tg_app,
            uid,
            'отсутствия alexkoshelev',
            'Введи период, за который нужно показать отсутствия (можно один день)',
            cancel_button=True
        )
        handle_utterance(tg_app, uid, 'сегодня', 'Я не нашла такого сотрудника')


@freeze_time('2018-01-12')
def test_get_gaps_today_timezone(uid, tg_app):
    with patch('uhura.external.suggest.find_person') as m, patch('uhura.external.gap._get_gaps_utc') as m2:
        m.return_value = [{'login': 'mkamalova'}], 'person_answer'
        m2.return_value = [{
            u'comment': u'-', u'workflow': u'illness', u'work_in_absence': False,
            u'date_from': u'2018-01-12T00:00:00', u'to_notify': [], u'person_login': u'mkamalova',
            u'date_to': u'2018-01-13T00:00:00', u'has_sicklist': False, u'full_day': True, u'id': 433879
        }]
        expected = '''Болезнь:
Сегодня (12 Января)
Весь день
<b>-</b>
Не будет работать'''
        handle_utterance(tg_app, uid, 'отсутствия марина камалова сегодня', expected)
