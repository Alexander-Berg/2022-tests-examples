# -*- coding: utf-8 -*-

import pytest
import hamcrest
from StringIO import StringIO
from balance.completions_fetcher.configurable_partner_completion import JSONIterator, RawExtractor


def assert_dicts_equal(a, b):
    for actual_dict, expected_dict in zip(a, b):
        hamcrest.assert_that(actual_dict, hamcrest.has_entries(expected_dict))


def test_json_iterator():
    data = """[
                {
                    "amount": "-295.00",
                    "contract_id": "666",
                    "time": "2019-01-20 00:00:00"
                },
                {
                    "amount": "-25.00",
                    "contract_id": "666",
                    "time": "2019-01-20 00:00:00"
                }
            ]"""

    expected = [
        {"amount": "-295.00", "contract_id": "666", "time": "2019-01-20 00:00:00"},
        {"amount": "-25.00", "contract_id": "666", "time": "2019-01-20 00:00:00"}
    ]

    data = StringIO(data)
    iterator = JSONIterator(RawExtractor())
    actual = list(iterator.process(data))
    assert_dicts_equal(actual, expected)


def test_raise_on_invalid_json():
    data = """{
                    "amount": "-295.00",
                    "contract_id": "666",
                    "time": "2019-01-20 00:00:00"
                }}"""

    data = StringIO(data)
    iterator = JSONIterator(RawExtractor())
    with pytest.raises(ValueError) as e:
        list(iterator.process(data))
    assert str(e.value).startswith('Invalid json data')
