# -*- coding: utf-8 -*-

import json
import decimal
import datetime
import unittest

from billing.dcs.dcs.utils.common import override_locale, parse_json, dump_json


class OverrideLocaleTestCase(unittest.TestCase):
    def test_month_name(self):
        with override_locale('en_US.UTF-8'):
            with override_locale('ru_RU.UTF-8'):
                january = datetime.date(2000, 1, 1).strftime(u'%B')
                january = january.decode('utf-8')

        expected = u'январ'
        self.assertTrue(january.lower().startswith(expected))


class DumpAndLoadJsonTestCase(unittest.TestCase):
    def test_parse_simple_json(self):
        value = '{"a": "a", "b": 6}'
        expected = {'a': 'a', 'b': 6}

        result = parse_json(value)
        assert result == expected

    def test_parse_json_complex_json(self):
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
            },
            "value": {
                "__type__": "decimal",
                "value": "1000"
            }
        }
        """.strip()

        expected = {
            u'id': 123456,
            u'creation_dt': datetime.datetime(2000, 1, 1),
            u'update_dt': datetime.datetime(2000, 1, 1, 12, 12, 12),
            u'value': decimal.Decimal('1000'),
        }

        result = parse_json(value)
        assert result == expected

    def test_dump_simple_json(self):
        value = {'a': 'a', 'b': 6}
        expected = '{"a": "a", "b": 6}'

        result = dump_json(value)
        assert result == expected

    def test_dump_complex_json(self):
        value = {
            u'id': 123456,
            u'creation_dt': datetime.datetime(2000, 1, 1),
            u'update_dt': datetime.datetime(2000, 1, 1, 12, 12, 12),
            u'value': decimal.Decimal('1000'),
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
            'value': {
                '__type__': 'decimal',
                'value': '1000',
            },
        })

        result = dump_json(value)
        assert result == expected

# vim:ts=4:sts=4:sw=4:tw=79:et:
