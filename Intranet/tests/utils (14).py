# coding: utf-8

from __future__ import unicode_literals

import datetime
import itertools
import json
from collections import OrderedDict
from random import randint

from vins_core.common.utterance import Utterance
from vins_core.dm.request import ReqInfo, AppInfo

ASSERT_LIST_RESULT_MESSAGE = 'Result message [%r] not in expected messages [%r]'
ASSERT_RESULT_MESSAGE = 'Result message [%r] not equal expected message [%r]'
ASSERT_RESULT_BUTTONS = 'Result buttons [%r] not equal expected buttons [%r]'
ASSERT_RESULT_IMAGES = 'Result images [%r] not equal expected images [%r]'
ASSERT_RESULT_INLINE_KEYBOARDS = 'Result inline_keyboards [%r] not equal expected inline_keyboards [%r]'
ASSERT_RESULT_RUC = 'Result request_user_contact [%r] not equal expected request_user_contact [%r]'
ASSERT_RESULT_FWD = 'Result forward_messages [%r] not equal expected forward_messages [%r]'
ASSERT_RESULT_CANCEL = 'Result cancel_button [%r] not equal expected cancel_button [%r]'


UHURA_UID = 1120000000004551
YES_NO_KEYBOARD = ['Да', 'Нет']


def create_req_info(uid, text):
    req_info = ReqInfo(
        uuid=uid,
        utterance=Utterance(text),
        client_time=datetime.datetime.now(),
        app_info=AppInfo(**{'app_id': 'uhura_unittest'}),
    )
    req_info.additional_options['telegram_username'] = 'UhuraAppBot'
    return req_info


def handle_utterance(app, uid, text, messages=None, buttons=None, images=None, request_user_contact=False,
                     forward_messages=None, cancel_button=False, inline_keyboards=None):
    req_info = create_req_info(uid, text)
    result_response = app.handle_request(req_info=req_info)
    if not result_response.vins_response_overrided:
        response_text = '\n'.join(c.text for c in result_response.cards)
        if isinstance(messages, list):
            assert response_text in messages, ASSERT_LIST_RESULT_MESSAGE % (response_text, messages)
        else:
            assert response_text == messages, ASSERT_RESULT_MESSAGE % (response_text, messages)
    else:
        if messages is None or isinstance(messages, (str, unicode)):
            messages = [messages]
        for (result_message, expected_messages) in itertools.izip_longest(result_response.messages, messages):
            if isinstance(expected_messages, list):
                assert result_message in expected_messages, ASSERT_LIST_RESULT_MESSAGE % (
                    result_message, expected_messages
                )
            else:
                assert result_message == expected_messages, ASSERT_RESULT_MESSAGE % (
                    result_message, expected_messages
                )

        buttons = buttons or []
        images = images or []
        forward_messages = forward_messages or []
        inline_keyboards = inline_keyboards or []
        assert result_response.buttons == buttons, ASSERT_RESULT_BUTTONS % (result_response.buttons, buttons)
        assert result_response.images == images, ASSERT_RESULT_IMAGES % (result_response.images, images)
        assert result_response.request_user_contact == request_user_contact, ASSERT_RESULT_RUC % (
            result_response.request_user_contact, request_user_contact
        )
        assert result_response.forward_messages == forward_messages, ASSERT_RESULT_FWD % (
            result_response.forward_messages, forward_messages
        )
        assert result_response.cancel_button == cancel_button, ASSERT_RESULT_CANCEL % (
            result_response.cancel_button, cancel_button
        )
        assert result_response.inline_keyboards == inline_keyboards, ASSERT_RESULT_INLINE_KEYBOARDS % (
            result_response.inline_keyboards, inline_keyboards
        )


def get_uid():
    return str(randint(1, 1e6))


def _staff_get_person_data():
    with open('uhura/tests/data/person_data.json') as person_data:
        return json.loads(person_data.read().decode('utf-8'))


def get_person_data_by_telegram_username(username):
    return _staff_get_person_data()


def get_person_data_by_userphone(phone):
    return _staff_get_person_data()


def uhura_app_get_person_data(self, req_info):
    return _staff_get_person_data()


def get_event_example():
    event = {
        'othersCanView': True,
        'name': 'name',
        'startTs': '2017-01-01T12:50:00',
        'endTs': '2017-01-10T12:50:00',
        'description': 'описание',
        'id': '1',
        'url': 'https://calendar.tst.yandex-team.ru/event/?event_id=1',
        'resources': [
            {
                'name': 'room1',
                'id': 1,
                'office_code': 'redrose',
                'floor': 5,
                'email': 'y@y.y'
            },
            {
                'name': 'room2',
                'id': 1,
                'office_code': 'spb',
                'floor': 5,
                'email': 'y@y.y'
            }
        ],
        'attendees': [
            {
                'login': 'login1',
                'name': 'Имя1 фамилия1'
            },
            {
                'login': 'login2',
                'name': 'Имя2 фамилия2'
            }
        ]
    }
    event_template = '''<a href="https://calendar.tst.yandex-team.ru/event/?event_id=1">%s</a> %s %s - %s %s
описание
<a href="https://staff.yandex-team.ru/map/#/redrose/5/?conference_rooms=1&conference_room_id=1">%s</a>
<a href="https://staff.yandex-team.ru/map/#/spb/5/?conference_rooms=1&conference_room_id=1">%s</a>
- <a href="https://staff.yandex-team.ru/%s">%s</a> %s@
- <a href="https://staff.yandex-team.ru/%s">%s</a> %s@'''
    rendered_event = event_template % (
        event['name'], 'Сегодня (1 Января)', '12:50', '10 Января', '12:50', event['resources'][0]['name'],
        event['resources'][1]['name'], event['attendees'][0]['login'], event['attendees'][0]['name'],
        event['attendees'][0]['login'], event['attendees'][1]['login'], event['attendees'][1]['name'],
        event['attendees'][1]['login']
    )
    return event, rendered_event


def get_gap_example():
    gap = {
        'comment': 'отсутствую',
        'workflow': 'illness',
        'date_from': '2017-01-01T12:00:00',
        'date_to': '2017-01-05T13:00:00',
        'full_day': False,
        'work_in_absence': False
    }
    rendered_gap = '''Болезнь:
Сегодня (1 Января) 12:00 - 5 Января 13:00
<b>отсутствую</b>
Не будет работать'''
    return gap, rendered_gap


def get_tickets_example():
    tickets_jsons = [
        {'link': 'st.yandex-team.ru/abcd', 'summary': 'описание тикета'},
        {'link': 'st.yandex-team.ru/giubh', 'summary': 'описание другого тикета'}
    ]
    tickets_text = '''st.yandex-team.ru/abcd
описание тикета

st.yandex-team.ru/giubh
описание другого тикета'''
    return tickets_jsons, tickets_text


def get_canteen_menu_json():
    with open('uhura/tests/data/canteen_menu.json') as json_data:
        return json.load(json_data)


def get_form_json():
    with open('uhura/tests/data/form.json') as json_data:
        return json.dumps(json.load(json_data, object_pairs_hook=OrderedDict))


def get_meeting_room_example():
    data = {
        'url': 'https://staff.yandex-team.ru/map/#/redrose/5/?conference_room_id=1593',
        'id': get_uid(),
        'title': 'Комната N',
        'name_alternative': 'КР 1-2',
        'phone': '1234',
        'floor_name': 'Первый этаж',
        'office_name': 'БЦ Морозов',
    }
    return data


def get_profile_data():
    with open('uhura/tests/data/person_profile.json') as json_data:
        return json.load(json_data)['target']


def get_car_owner(**kwargs):
    with open('uhura/tests/data/car_owner.json') as json_data:
        car_owner = json.load(json_data)
        for k, v in kwargs.iteritems():
            car_owner[k] = v
        return car_owner


def create_inline_keyboard(text, callback_data):
    return {'inline_keyboard': [[{'callback_data': callback_data, 'text': text}]]}
