# coding: utf-8

from __future__ import unicode_literals

import json

import pytest
from freezegun import freeze_time

from utils import handle_utterance

FIRST_PHRASE = 'парковка'  # any phrase that starts submit_form intent
FIRST_ANSWER = 'Вопрос\n\n<i>Подсказка</i>'
WRONG_INPUT_ANSWER = 'Некорректный ввод. Хочешь попробовать еще?'
SUCCESS_ANSWER_TEMPLATE = 'Отправить форму?\n\nВопрос: %s'
YES_NO_KEYBOARD = ['Да', 'Нет']


def _get_input_json(file_name):
    with open('uhura/tests/data/forms/inputs/' + file_name, 'r') as f:
        return json.load(f)


@pytest.fixture
def base_form_json():
    with open('uhura/tests/data/forms/form_base.json', 'r') as f:
        return json.load(f)


@freeze_time('2017-01-01')
class TestDateInput(object):
    @pytest.fixture(autouse=True)
    def monkeypatch_get_form_method(self, base_form_json, monkeypatch):
        base_form_json['fields'] = _get_input_json('DateInput.json')
        monkeypatch.setattr('uhura.external.intranet.get_request', lambda *args, **kwargs: json.dumps(base_form_json))

    def test_date_as_number_and_month_name(self, uid, tg_app):
        handle_utterance(tg_app, uid, FIRST_PHRASE, FIRST_ANSWER, cancel_button=True)
        handle_utterance(tg_app, uid, '5 мая', SUCCESS_ANSWER_TEMPLATE % '2017-05-05', YES_NO_KEYBOARD)

    def test_date_as_dd_mm_yyyy(self, uid, tg_app):
        handle_utterance(tg_app, uid, FIRST_PHRASE, FIRST_ANSWER, cancel_button=True)
        handle_utterance(tg_app, uid, '12.12.2017', SUCCESS_ANSWER_TEMPLATE % '2017-12-12', YES_NO_KEYBOARD)

    def test_date_today(self, uid, tg_app):
        handle_utterance(tg_app, uid, FIRST_PHRASE, FIRST_ANSWER, cancel_button=True)
        handle_utterance(tg_app, uid, 'сегодня', SUCCESS_ANSWER_TEMPLATE % '2017-01-01', YES_NO_KEYBOARD)

    def test_wrong_format(self, uid, tg_app):
        handle_utterance(tg_app, uid, FIRST_PHRASE, FIRST_ANSWER, cancel_button=True)
        handle_utterance(tg_app, uid, 'случайный текст', WRONG_INPUT_ANSWER, YES_NO_KEYBOARD)


class TestNumberInput(object):
    FIRST_ANSWER = 'Вопрос\n\n<i>Подсказка\n\nЗначение должно быть от 1 до 10</i>'

    @pytest.fixture(autouse=True)
    def monkeypatch_get_form_method(self, base_form_json, monkeypatch):
        base_form_json['fields'] = _get_input_json('NumberInput.json')
        monkeypatch.setattr('uhura.external.intranet.get_request', lambda *args, **kwargs: json.dumps(base_form_json))

    def test_number(self, uid, tg_app):
        handle_utterance(tg_app, uid, FIRST_PHRASE, self.FIRST_ANSWER, cancel_button=True)
        handle_utterance(tg_app, uid, '9', SUCCESS_ANSWER_TEMPLATE % '9', YES_NO_KEYBOARD)

    def test_wrong_number(self, uid, tg_app):
        handle_utterance(tg_app, uid, FIRST_PHRASE, self.FIRST_ANSWER, cancel_button=True)
        handle_utterance(tg_app, uid, '11', WRONG_INPUT_ANSWER, YES_NO_KEYBOARD)

    def test_not_a_number(self, uid, tg_app):
        handle_utterance(tg_app, uid, FIRST_PHRASE, self.FIRST_ANSWER, cancel_button=True)
        handle_utterance(tg_app, uid, 'случайный текст', WRONG_INPUT_ANSWER, YES_NO_KEYBOARD)


class TestSelectInput(object):
    ITEMS_KEYBOARD = ['Ответ 1', 'Ответ 2', 'Ответ 3']

    @pytest.fixture(autouse=True)
    def monkeypatch_get_form_method(self, base_form_json, monkeypatch):
        base_form_json['fields'] = _get_input_json('SelectInput.json')
        monkeypatch.setattr('uhura.external.intranet.get_request', lambda *args, **kwargs: json.dumps(base_form_json))

    def test_success(self, uid, tg_app):
        handle_utterance(tg_app, uid, FIRST_PHRASE, FIRST_ANSWER, self.ITEMS_KEYBOARD, cancel_button=True)
        handle_utterance(tg_app, uid, 'Ответ 1', SUCCESS_ANSWER_TEMPLATE % 'Ответ 1', YES_NO_KEYBOARD)

    def test_wrong_answer(self, uid, tg_app):
        handle_utterance(tg_app, uid, FIRST_PHRASE, FIRST_ANSWER, self.ITEMS_KEYBOARD, cancel_button=True)
        handle_utterance(tg_app, uid, 'случайный текст', WRONG_INPUT_ANSWER, YES_NO_KEYBOARD)


class TestSuggestInput(object):
    @pytest.fixture(autouse=True)
    def monkeypatch_get_form_method(self, base_form_json, monkeypatch):
        base_form_json['fields'] = _get_input_json('SuggestInput.json')
        monkeypatch.setattr('uhura.external.intranet.get_request', lambda *args, **kwargs: json.dumps(base_form_json))

    def test_success(self, uid, tg_app, monkeypatch):
        monkeypatch.setattr(
            'uhura.external.forms.SuggestInput._call_suggest', lambda *args, **kwargs: [{'text': 'Ответ', 'id': 1}]
        )
        handle_utterance(tg_app, uid, FIRST_PHRASE, FIRST_ANSWER, cancel_button=True)
        handle_utterance(tg_app, uid, 'Ответ', SUCCESS_ANSWER_TEMPLATE % 'Ответ', YES_NO_KEYBOARD)

    def test_not_found(self, uid, tg_app, monkeypatch):
        monkeypatch.setattr('uhura.external.forms.SuggestInput._call_suggest', lambda *args, **kwargs: [])
        handle_utterance(tg_app, uid, FIRST_PHRASE, FIRST_ANSWER, cancel_button=True)
        handle_utterance(tg_app, uid, 'Ответ 1', 'Такого варианта нет. Можешь попробовать еще раз', cancel_button=True)

    def test_suggest_items(self, uid, tg_app, monkeypatch):
        monkeypatch.setattr(
            'uhura.external.forms.SuggestInput._call_suggest',
            lambda *args, **kwargs: [{'text': 'Ответ 1', 'id': 1}, {'text': 'Ответ 2', 'id': 1}]
        )
        handle_utterance(tg_app, uid, FIRST_PHRASE, FIRST_ANSWER, cancel_button=True)
        handle_utterance(tg_app, uid, 'Ответ 1', 'Уточни, пожалуйста', ['Ответ 1', 'Ответ 2'], cancel_button=True)

    def test_multiple_choice(self, uid, tg_app, base_form_json, monkeypatch):
        monkeypatch.setattr(
            'uhura.external.forms.SuggestInput._call_suggest', lambda *args, **kwargs: [{'text': 'Ответ', 'id': 1}]
        )
        input_json = _get_input_json('SuggestInput.json')
        input_json[input_json.keys()[0]]['is_allow_multiple_choice'] = True
        base_form_json['fields'] = input_json
        monkeypatch.setattr('uhura.external.intranet.get_request', lambda *args, **kwargs: json.dumps(base_form_json))
        handle_utterance(tg_app, uid, FIRST_PHRASE, FIRST_ANSWER, cancel_button=True)
        handle_utterance(tg_app, uid, 'Ответ', FIRST_ANSWER, ['Перейти к следующему вопросу'], cancel_button=True)
        handle_utterance(
            tg_app, uid, 'Перейти к следующему вопросу', SUCCESS_ANSWER_TEMPLATE % 'Ответ', YES_NO_KEYBOARD
        )


class TestTextInput(object):
    @pytest.fixture(autouse=True)
    def monkeypatch_get_form_method(self, base_form_json, monkeypatch):
        base_form_json['fields'] = _get_input_json('TextInput.json')
        monkeypatch.setattr('uhura.external.intranet.get_request', lambda *args, **kwargs: json.dumps(base_form_json))

    def test_any_text(self, uid, tg_app):
        handle_utterance(tg_app, uid, FIRST_PHRASE, FIRST_ANSWER, cancel_button=True)
        handle_utterance(tg_app, uid, 'случайный текст', SUCCESS_ANSWER_TEMPLATE % 'случайный текст', YES_NO_KEYBOARD)
