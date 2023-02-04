# coding: utf-8
from __future__ import unicode_literals

from freezegun import freeze_time
from mock import patch

from utils import handle_utterance, get_event_example

(EVENT, _) = get_event_example()


@freeze_time('2017-01-01')
def test_get_meetings_by_date__error(uid, tg_app):
    with patch('uhura.lib.callbacks.get_meetings_by_date') as m:
        m.return_value = None
        handle_utterance(
            tg_app,
            uid,
            'встречи 10 августа',
            'Не получилось ничего узнать о встречах. Ты приходи еще, вместе попробуем еще раз:)'
        )


@freeze_time('2017-01-01')
def test_get_meetings_by_date__wrong_dates(uid, tg_app):
    handle_utterance(tg_app, uid, 'встречи с 15 по 10 декабря', 'Начало отрезка позже чем конец')


@freeze_time('2017-01-01')
def test_get_meetings_by_date__success_without_login(uid, tg_app):
    with patch('uhura.lib.callbacks.get_meetings_by_date') as m:
        m.return_value = {}
        handle_utterance(tg_app, uid, 'встречи', 'На какую дату?', cancel_button=True)
        handle_utterance(
            tg_app, uid, 'с понедельника по четверг', ['Твои встречи:', 'Встреч с 2017-01-02 до 2017-01-05 нет']
        )


@freeze_time('2017-01-01')
def test_get_meetings_by_date_success_with_login_hidden_layer(uid, tg_app):
    with patch('uhura.external.calendar.request_to_calendar') as m,\
            patch('uhura.external.suggest.find_person') as m1,\
            patch('uhura.external.staff.get_uid_by_login') as m2:
        event = EVENT.copy()
        event['primaryLayerClosed'] = True
        m.return_value = {'events': [event]}
        m1.return_value = [{'login': 'alexkoshelev'}], 'person_answer'
        m2.return_value = 1234567890
        handle_utterance(tg_app, uid, 'встречи alexkoshelev', 'На какую дату?', cancel_button=True)
        handle_utterance(
            tg_app,
            uid,
            'сегодня',
            ['Встречи alexkoshelev:', 'Скрытая встреча Сегодня (1 Января) 12:50 - 10 Января 12:50']
        )


@freeze_time('2017-01-01')
def test_get_meetings_by_date_success_with_login_hidden_meeting(uid, tg_app):
    with patch('uhura.external.calendar.request_to_calendar') as m,\
            patch('uhura.external.suggest.find_person') as m1,\
            patch('uhura.external.staff.get_uid_by_login') as m2:
        event = EVENT.copy()
        event['othersCanView'] = False
        m.return_value = {'events': [event]}
        m1.return_value = [{'login': 'alexkoshelev'}], 'person_answer'
        m2.return_value = 1234567890
        handle_utterance(tg_app, uid, 'встречи alexkoshelev', 'На какую дату?', cancel_button=True)
        handle_utterance(
            tg_app,
            uid,
            'сегодня',
            ['Встречи alexkoshelev:', 'Скрытая встреча Сегодня (1 Января) 12:50 - 10 Января 12:50']
        )


@freeze_time('2017-01-01')
def test_get_meetings_by_date_two_meetings(uid, tg_app):
    with patch('uhura.external.calendar.request_to_calendar') as m, \
            patch('uhura.external.suggest.find_person') as m1, \
            patch('uhura.external.staff.get_uid_by_login') as m2, \
            patch('uhura.external.staff.get_meeting_room_by_email') as m3:
        m.return_value = {'events': [EVENT.copy(), EVENT.copy()]}
        m1.return_value = [{'login': 'login'}], 'person_answer'
        m2.return_value = 1234567890
        m3.return_value = {'id': 1, 'floor': {'id': 2, 'office': {'code': 3}}}
        rendered_event = '''<a href="https://calendar.tst.yandex-team.ru/event/?event_id=1">name</a> Сегодня (1 Января) 12:50 - 10 Января 12:50
описание
<a href="https://staff.yandex-team.ru/map/#/3/2/?conference_rooms=1&conference_room_id=1">room1</a>
<a href="https://staff.yandex-team.ru/map/#/3/2/?conference_rooms=1&conference_room_id=1">room2</a>
- <a href="https://staff.yandex-team.ru/login1">Имя1 фамилия1</a> login1@
- <a href="https://staff.yandex-team.ru/login2">Имя2 фамилия2</a> login2@'''
        handle_utterance(tg_app, uid, 'встречи login', 'На какую дату?', cancel_button=True)
        handle_utterance(tg_app, uid, 'сегодня', ['Встречи login:', rendered_event, rendered_event])


@freeze_time('2017-01-01')
def test_get_meetings_by_date__meeting_room_not_found(uid, tg_app):
    with patch('uhura.external.calendar.request_to_calendar') as m, \
            patch('uhura.external.suggest.find_person') as m1, \
            patch('uhura.external.staff.get_uid_by_login') as m2, \
            patch('uhura.external.staff.get_meeting_room_by_email') as m3:
        m.return_value = {'events': [EVENT.copy(), EVENT.copy()]}
        m1.return_value = [{'login': 'login'}], 'person_answer'
        m2.return_value = 1234567890
        m3.side_effect = Exception
        rendered_event = '''<a href="https://calendar.tst.yandex-team.ru/event/?event_id=1">name</a> Сегодня (1 Января) 12:50 - 10 Января 12:50
описание
room1
room2
- <a href="https://staff.yandex-team.ru/login1">Имя1 фамилия1</a> login1@
- <a href="https://staff.yandex-team.ru/login2">Имя2 фамилия2</a> login2@'''
        handle_utterance(tg_app, uid, 'встречи login', 'На какую дату?', cancel_button=True)
        handle_utterance(tg_app, uid, 'сегодня', ['Встречи login:', rendered_event, rendered_event])
