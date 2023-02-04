# coding: utf-8

from datetime import datetime
import json

from tasha.lib import cache_storage


def json_dumps(obj):
    return json.dumps(obj, sort_keys=True, cls=cache_storage._CacheEncoder)


def test_datetime_encoding():
    assert json_dumps(datetime(2018, 1, 1)) == '"2018-01-01 00:00:00"'


def test_list_encoding():
    assert json_dumps([5, 4, 3, 1, 2]) == '["1", "2", "3", "4", "5"]'


def test_dict_encoding():
    assert json_dumps({1: 2}) == '{"1": 2}'


def test_list_of_list_dict_and_str_encoding():
    assert json_dumps([{}, [], 'str']) == '["OrderedDict()", "[]", "str"]'


def test_complex_encoding():
    result = r'''["OrderedDict([('1', ['4']), ('2', '3')])", "['123', \"OrderedDict([('1', ['2018-01-01 00:00:00'])])\"]"]'''  # noqa E501
    assert json_dumps([['123', {'1': [datetime(2018, 1, 1)]}], {'1': ['4'], '2': '3'}]) == result
