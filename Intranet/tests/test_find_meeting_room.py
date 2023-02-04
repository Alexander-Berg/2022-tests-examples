# coding: utf-8

from __future__ import unicode_literals

from random import randint

from mock import patch
from requests import ConnectTimeout

from utils import get_meeting_room_example, handle_utterance


def _get_expected_room_card(room):
    expected = '{title}, {name_alternative}\n<a href="{url}">{floor_name}, {office_name}</a>'
    if room['phone']:
        expected += '\nТелефон: {phone}'
    return expected.format(**room)


def test_with_ask(uid, tg_app):
    with patch('uhura.external.suggest._call_suggest') as m:
        m.return_value = {'invite': {'result': []}, 'people': {'result': []}}
        handle_utterance(
            tg_app,
            uid,
            'где переговорка',
            'Скажи название переговорки, которую ты хочешь найти.',
            cancel_button=True
        )
        handle_utterance(
            tg_app,
            uid,
            'зелёный горошек',
            'Не нашла такой переговорки.'
        )


def test_not_find(uid, tg_app):
    with patch('uhura.external.suggest._call_suggest') as m:
        m.return_value = {'invite': {'result': []}, 'people': {'result': []}}
        handle_utterance(
            tg_app, uid, 'где переговорка ххх', 'Не нашла такой переговорки.'
        )


def test_find_one(uid, tg_app):
    with patch('uhura.external.suggest._call_suggest') as m:
        room = get_meeting_room_example()
        m.return_value = {'invite': {'result': [room]}, 'people': {'result': []}}
        expected = _get_expected_room_card(room)
        handle_utterance(tg_app, uid, 'где переговорка xxx', expected)


def test_find_several(uid, tg_app):
    """ До пяти переговорок выводим сразу одним сообщением
    """
    with patch('uhura.external.suggest._call_suggest') as m:
        rooms = [get_meeting_room_example()] * randint(1, 4)
        # до пяти переговорок выводим сразу одним сообщением
        m.return_value = {'invite': {'result': rooms}, 'people': {'result': []}}
        expected = '\n\n'.join(_get_expected_room_card(room) for room in rooms)
        handle_utterance(tg_app, uid, 'где переговорка xxx', expected)


def test_find_too_many(uid, tg_app):
    """ Просим уточнить, если нашли слишком много переговорок
    """
    with patch('uhura.external.suggest._call_suggest') as m:
        room = get_meeting_room_example()
        m.return_value = {'invite': {'result': [room] * 10}, 'people': {'result': []}}
        expected = 'Нашла слишком много переговорок. Можешь сказать точнее, какая тебе нужна?'
        handle_utterance(tg_app, uid, 'где переговорка xxx', expected)


def test_exception(uid, tg_app):
    with patch('requests.get') as m:
        m.side_effect = ConnectTimeout()
        expected = 'Прости, но у меня не получилось поискать переговорки. Попробуй еще раз через минуту!'
        handle_utterance(tg_app, uid, 'где переговорка xxx', expected)
