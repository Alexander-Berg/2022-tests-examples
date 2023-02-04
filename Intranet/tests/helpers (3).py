# coding: utf-8

from __future__ import unicode_literals

import re
import json

import attr

from easymeeting.lib import datetimes


def get_json(
    client,
    path,
    query_params=None,
    expect_status=200,
    json_response=True,
):
    if query_params is None:
        query_params = {}
    response = client.get(path, query_params)
    if expect_status:
        assert response.status_code == expect_status, (
            response.status_code,
            response.content,
        )
    if json_response:
        return response.json()


def post_json(
    client,
    path,
    json_data,
    expect_status=200,
    json_response=True,
):
    response = client.post(
        path,
        json.dumps(json_data),
        content_type="application/json",
    )
    if expect_status:
        assert response.status_code == expect_status, (
            response.status_code,
            response.content,
        )
    if json_response:
        return response.json()


@attr.s
class DatetimeNames(object):
    """
    Usage:
    from tests import helpers

    DN = helpers.DatetimeNames()
    >>> DN.T_12_30
    ... datetime(2020, 1, 1, 12, 30, tzinfo=pytz.utc)
    """

    year = attr.ib(default=2020)
    month = attr.ib(default=1)
    day = attr.ib(default=1)

    def __getattr__(self, item):
        match = re.match(r'T_(?P<hour>\d\d)_(?P<minute>\d\d)', item)
        if not match:
            raise AttributeError(item)
        return datetimes.utc_datetime(
            year=self.year,
            month=self.month,
            day=self.day,
            hour=int(match.groupdict()['hour']),
            minute=int(match.groupdict()['minute']),
        )


# TODO: вынести в библиотеку и подключить
class ANY(object):
    def __init__(self, type=None):
        self.type = type


def assert_is_subdict(first, second):
    """
    Если все ключи-значения первого есть во втором
    """
    is_first_dict = isinstance(first, dict)
    is_second_dict = isinstance(second, dict)
    err_msg = 'different structure %r \n %r' % (first, second)
    assert is_first_dict == is_second_dict, err_msg

    first_as_set = set(first.items())
    second_as_set = set(second.items())
    sets_diff = first_as_set ^ second_as_set
    for first_key, first_val in first.items():
        assert first_key in second, sets_diff
        if isinstance(first_val, ANY):
            if first_val.type:
                assert isinstance(second[first_key], first_val.type), sets_diff
        else:
            assert first_val == second[first_key], sets_diff


def is_flat_dict(dict_):
    if not isinstance(dict_, dict):
        return False
    return not any(
        isinstance(value, (dict, list, tuple))
        for value in dict_.values()
    )


def is_simple_type(obj):
    return not isinstance(obj, (dict, list, tuple))


def assert_is_substructure(first, second):
    err_msg = 'different structure %r \n %r' % (first, second)

    is_first_simple = is_simple_type(first)
    is_second_simple = is_simple_type(second)
    assert is_first_simple == is_second_simple, err_msg

    if is_first_simple:
        assert first == second
        return

    is_first_flat = is_flat_dict(first)
    is_second_flat = is_flat_dict(second)
    assert is_first_flat == is_second_flat, err_msg

    if is_first_flat:
        assert_is_subdict(first, second)
        return

    is_first_list = isinstance(first, (list, tuple))
    is_second_list = isinstance(second, (list, tuple))
    assert is_first_list == is_second_list, err_msg

    if is_first_list:
        assert len(first) <= len(second), (len(first), len(second))
        for first_item, second_item in zip(first, second):
            assert_is_substructure(first_item, second_item)
        return

    for key, first_value in first.items():
        assert key in second, (key, second)
        second_value = second[key]
        if isinstance(first_value, (list, dict)):
            assert_is_substructure(first_value, second_value)
            continue
        assert first_value == second_value
