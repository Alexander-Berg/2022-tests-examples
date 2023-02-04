# coding: utf-8

import json
import decimal
import itertools
import datetime as dt

import pytest

from butils.decimal_unit import DecimalUnit

from balance.actions.dcs import constants
from balance.actions.dcs.utils import common as common_utils


class TestDumpAndParseJson(object):
    def test_parse_simple_json(self):
        value = '{"a": "a", "b": 6}'
        expected = {'a': 'a', 'b': 6}

        result = common_utils.parse_json(value)
        assert result == expected

    def test_parse_json_with_dates(self):
        value = """
        {
            "id": 123456,
            "creation_dt": {
                "__type__": "date",
                "value": "2000-01-01"
            },
            "update_dt": {
                "__type__": "date",
                "value": "2000-01-01 12:12:12"
            }
        }
        """.strip()

        expected = {
            u'id': 123456,
            u'creation_dt': dt.datetime(2000, 1, 1),
            u'update_dt': dt.datetime(2000, 1, 1, 12, 12, 12),
        }

        result = common_utils.parse_json(value)
        assert result == expected

    def test_dump_simple_json(self):
        value = {'a': 'a', 'b': 6}
        expected = '{"a": "a", "b": 6}'

        result = common_utils.dump_json(value)
        assert result == expected

    def test_dump_json_with_dates(self):
        value = {
            u'id': 123456,
            u'creation_dt': dt.datetime(2000, 1, 1),
            u'update_dt': dt.datetime(2000, 1, 1, 12, 12, 12),
        }
        expected = json.dumps({
            'id': 123456,
            'creation_dt': {
                '__type__': 'date',
                'value': '2000-01-01',
            },
            'update_dt': {
                '__type__': 'date',
                'value': '2000-01-01 12:12:12',
            },
        })

        result = common_utils.dump_json(value)
        assert result == expected


def test_is_last_day_of_month():
    cases = [
        (dt.datetime(2000, 1, 1), False),
        (dt.datetime(2000, 1, 31), True),
        (dt.datetime(2000, 2, 29), True),
    ]
    for (case, expected) in cases:
        result = common_utils.is_last_day_of_month(case)
        assert result is expected


def test_scalar_list():
    value = ((1, ), (2, ), (3, ))
    expected = (1, 2, 3)

    result = tuple(common_utils.scalar_list(value))
    assert result == expected


def test_is_date():
    cases = [
        (dt.datetime.today(), False),
        (dt.date(2000, 1, 1), True),
        (dt.datetime(2000, 1, 1), True),
    ]

    for (case, expected) in cases:
        result = common_utils.is_date(case)
        assert result is expected


def test_to_literal():
    cases = [
        ('string', "'string'"),
        (123456, '123456'),
        (decimal.Decimal('12.3456'), '12.3456'),
        (dt.date(2000, 1, 1), "date '2000-01-01'"),
        (dt.datetime(2000, 1, 1), "date '2000-01-01'"),
        (
            dt.datetime(2000, 1, 1, 12, 12, 12),
            "to_date('2000-01-01 12:12:12', 'YYYY-MM-DD HH24:MI:SS')",
            constants.Dialects.ORACLE,
        ),
        (
            dt.datetime(2000, 1, 1, 12, 12, 12),
            "to_timestamp('2000-01-01 12:12:12', 'YYYY-MM-DD HH24:MI:SS')",
            constants.Dialects.PG,
        ),
        (None, 'null'),
        ([1, 'string'], "(1, 'string')")
    ]
    for case in cases:
        case = list(case)

        value = case.pop(0)
        expected = case.pop(0)
        param = case and case.pop(0) or None

        result = common_utils.to_literal(value, param)
        assert result == expected


def test_to_string_value():
    cases = [
        ('string', 'string'),
        (123456, '123456'),
        (decimal.Decimal('12.0000'), '12'),
        (decimal.Decimal('12.3456'), '12.3456'),
        (decimal.Decimal('12.3456'), '12.3456'),
        (DecimalUnit('12.34', 'RUB'), '12.34'),
        (dt.date(2000, 1, 1), '2000-01-01'),
        (dt.datetime(2000, 1, 1), '2000-01-01'),
        (dt.datetime(2000, 1, 1, 12, 12, 12), '2000-01-01 12:12:12'),
        (None, None),
    ]
    for case in cases:
        value, expected = case

        result = common_utils.to_str_value(value)
        assert result == expected

    with pytest.raises(TypeError, match='Unknown value type'):
        common_utils.to_str_value(3.2)


def test_list_to_map():
    value = [{'id': 1}, {'id': 2}]
    expected = {1: {'id': 1}, 2: {'id': 2}}

    result = common_utils.list_to_map(value, 'id')
    assert result == expected

    result = common_utils.list_to_map(value, lambda item: item['id'])
    assert result == expected


def test_simple_cache():
    initial = value = 123456

    @common_utils.simple_cache()
    def test_func():
        return value

    assert test_func() == value

    value = "another value"
    assert test_func() == initial


def test_safe_iter():
    values = ['1', '2', '3', 'hello', '4']
    expected = [1, 2, 3, 4]
    result = list(common_utils.safe_iter(itertools.imap(int, values)))
    assert result == expected


class TestExtractKeys(object):
    """
    Тестируем логику функции `extract_keys`
    """

    @pytest.fixture
    def diff(self):
        return {
            # Ключ
            'product_id': 123456,
            'service_id': 777,

            # Информация о расхождении
            'column_name': 'amount',
            't1_value': '12.3456',
            't2_value': '12.3457',
            'diff_type': 1,
        }

    @pytest.fixture
    def expected_diff(self):
        return {
            'key1_name': 'product_id',
            'key1_value': 123456,
            'key2_name': 'service_id',
            'key2_value': 777,

            'column_name': 'amount',
            'column_value1': '12.3456',
            'column_value2': '12.3457',
            'type': 1,
            'issue_key': None,
            'status': None,
        }

    @pytest.mark.parametrize('keys', [
        ['product_id', 'service_id'],
        'product_id service_id'
    ], ids=['list', 'string'])
    def test_different_keys_types(self, diff, expected_diff, keys):
        """
        Тестируем логику с разными типами ключей
        """
        result = common_utils.extract_keys(keys, diff)
        assert result == expected_diff

