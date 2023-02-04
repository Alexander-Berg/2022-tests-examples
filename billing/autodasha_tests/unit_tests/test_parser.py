# -*- coding: utf-8 -*-

from __future__ import unicode_literals
import datetime

import pytest

from autodasha.core.logic import parser
from autodasha.core.exc import *


def test_composite_ok():
    test_parse_manager = parser.ParseManager(
        parser.IntParser.create_parser('int', 'ИНТ'),
        parser.BooleanParser.create_parser('bool', 'БУЛЕАН', ' -'),
        parser.DateParser.create_parser('date', 'ДАТЕ'),
        parser.SingleLineParser.create_parser('str', 'СТР'),
        parser.MultilineParser.create_parser('multiline', 'МНОГОСТРОК'),
        parser.CompositeParser.create_parser(
            'composite', 'КАМПАЗИТ',
            parser.ParseManager(
                parser.IntParser.create_parser('int', 'КАМПАЗИТ ИНТ', ' -'),
                parser.BooleanParser.create_parser('bool', 'КАМПАЗИТ БУЛЕАН', ' -'),
                parser.DateParser.create_parser('date', 'КАМПАЗИТ ДАТЕ', ' -'),
                parser.SingleLineParser.create_parser('str', 'КАМПАЗИТ СТР', ' -'),
            )
        )
    )

    res = test_parse_manager.parse('''
    ИНТ:
       666635435

    МНОГОСТРОК:
    раз
    два
    три

    СТР: аляля
    КАМПАЗИТ:
    КАМПАЗИТ
    КАМПАЗИТ ИНТ - 1
    КАМПАЗИТ БУЛЕАН - да
    КАМПАЗИТ ДАТЕ - 2666-01-01

    КАМПАЗИТ
    КАМПАЗИТ СТР - ляля
    КАМПАЗИТ БУЛЕАН - нед

    БУЛЕАН - да

       ДАТЕ:
 2006-06-06
    ''')
    assert res == {
        'int': 666635435,
        'bool': True,
        'date': datetime.datetime(2006, 6, 6),
        'str': 'аляля',
        'composite': [
            {'int': 1, 'bool': True, 'date': datetime.datetime(2666, 1, 1)},
            {'bool': False, 'str': 'ляля'},
        ],
        'multiline': 'раз два три',
    }


def test_fail_single_line():
    test_parse_manager = parser.ParseManager(parser.SingleLineParser.create_parser('str', 'СТР'))

    with pytest.raises(ParseException) as exc_info:
        test_parse_manager.parse('''
        СТР: алала
        ла
        ''')

    assert exc_info.value.args == ('invalid_form', 'ла')


def test_fail_single_line_split():
    test_parse_manager = parser.ParseManager(parser.SingleLineParser.create_parser('str', 'СТР'))

    with pytest.raises(ParseException) as exc_info:
        test_parse_manager.parse('''
        СТР: 
          алала
        ла
        ''')

    assert exc_info.value.args == ('invalid_form', 'ла')


def test_fail_unknown_arg():
    test_parse_manager = parser.ParseManager(parser.SingleLineParser.create_parser('str', 'СТР'))

    with pytest.raises(ParseException) as exc_info:
        test_parse_manager.parse('''
        НЕ_СТР: алала
        СТР: алала
        ''')

    assert exc_info.value.args == ('invalid_form', 'НЕ_СТР: алала')


def test_fail_date():
    test_parse_manager = parser.ParseManager(parser.DateParser.create_parser('date', 'ДАТЕ'))

    with pytest.raises(ParseException) as exc_info:
        test_parse_manager.parse('''
        ДАТЕ: 111-11-111
        ''')

    assert exc_info.value.args == ('incorrect_parameter', 'ДАТЕ')


def test_choices():
    test_parse_manager = parser.ParseManager(
        parser.ChoiceParser.create_parser('single', 'ВЫБОР', {'А': 1, 'Б': 2}),
        parser.MultiChoiceParser.create_parser('multi', 'МНОГОВЫБОР', {'А': 1, 'Б': 2, 'В': 3}),
    )

    res = test_parse_manager.parse('''
    ВЫБОР: Б
    МНОГОВЫБОР: А, В
    ''')
    assert res == {'single': 2, 'multi': [1, 3]}


def test_single_choice_fail():
    test_parse_manager = parser.ParseManager(parser.ChoiceParser.create_parser('single', 'ВЫБОР', {'А': 1, 'Б': 2}))

    with pytest.raises(ParseException) as exc_info:
        test_parse_manager.parse('ВЫБОР: В')

    assert exc_info.value.args == ('incorrect_parameter', 'ВЫБОР')


def test_multi_choice_fail():
    test_parse_manager = parser.ParseManager(
        parser.MultiChoiceParser.create_parser('multi', 'МНОГОВЫБОР', {'А': 1, 'Б': 2, 'В': 3})
    )

    with pytest.raises(ParseException) as exc_info:
        test_parse_manager.parse('МНОГОВЫБОР: В, А, Г')

    assert exc_info.value.args == ('incorrect_parameter', 'МНОГОВЫБОР')
