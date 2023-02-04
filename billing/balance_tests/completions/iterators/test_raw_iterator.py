# -*- coding: utf-8 -*-

import pytest
import hamcrest
from balance.completions_fetcher.configurable_partner_completion import RawIterator, RawExtractor


def assert_dicts_equal(a, b):
    for actual_dict, expected_dict in zip(a, b):
        hamcrest.assert_that(actual_dict, hamcrest.has_entries(expected_dict))


def test_raw_iterator():
    expected = [
        {"amount": "-295.00", "contract_id": "666", "time": "2019-01-20 00:00:00"},
        {"amount": "-25.00", "contract_id": "666", "time": "2019-01-20 00:00:00"}
    ]

    iterator = RawIterator(RawExtractor())
    actual = list(iterator.process(expected))
    assert_dicts_equal(actual, expected)


@pytest.mark.parametrize('data', [1, None])
def test_raise_on_noniterable_data(data):
    iterator = RawIterator(RawExtractor())
    with pytest.raises(TypeError) as e:
        list(iterator.process(data))
    str(e.value).endswith('object is not iterable')

