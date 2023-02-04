# -*- coding: utf-8 -*-

from contextlib import contextmanager

import pytest
from yt.wrapper.mappings import FrozenDict

import balance.exc as exc
from balance.utils.partner_processing.store_logic import SubstituteAccessor


@pytest.mark.parametrize(
    'real_store, substitute_mapping, expected_data',
    [
        ({}, {}, {'b': 1, 'c': 2, 'd': 3}),
        ({'a': 0}, {}, {'a': 0, 'b': 1, 'c': 2, 'd': 3}),
        ({'a': 0}, {'b': 'e', 'c': 'f', 'd': 'g'}, {'a': 0, 'e': 1, 'f': 2, 'd': 3}),
        ({'a': 0}, {'b': 'a'}, {'a': 1, 'c': 2, 'd': 3}),
    ],
)
def test_accessor_write(real_store, substitute_mapping, expected_data):
    sa = SubstituteAccessor(real_store, substitute_mapping)
    sa.b = 1
    sa['c'] = 2
    sa.real_store['d'] = 3
    assert FrozenDict(real_store) == FrozenDict(expected_data)


@pytest.mark.parametrize(
    'real_store, substitute_mapping, expected_data',
    [
        ({'b': 1, 'c': 2, 'd': 3}, {}, (1, 2, 3)),
        ({'b': 1, 'c': 2, 'd': 3}, {'b': 'c', 'c': 'd', 'd': 'b'}, (2, 3, 3)),
    ],
)
def test_accessor_read(real_store, substitute_mapping, expected_data):
    sa = SubstituteAccessor(real_store, substitute_mapping)
    read_result = (
        sa.b, sa['c'], sa.real_store['d']
    )
    assert read_result == expected_data

@pytest.mark.parametrize(
    'real_store, substitute_mapping, expected_data',
    [
        ({}, {},
         (
            [exc.PARAM_NOT_IN_STORE_EXCEPTION, 'Param b not in store'],
            [exc.PARAM_NOT_IN_STORE_EXCEPTION, 'Param c not in store'],
            [KeyError, 'd']
         )
        ),
        ({'b': 1, 'c': 2}, {'b': 'e', 'c': 'f', 'd': 'g'},
         (
             [exc.PARAM_NOT_IN_STORE_EXCEPTION, 'Param e not in store'],
             [exc.PARAM_NOT_IN_STORE_EXCEPTION, 'Param f not in store'],
             [KeyError, 'd']
         )
        ),
    ],
)
def test_accessor_read_no_value(real_store, substitute_mapping, expected_data):
    sa = SubstituteAccessor(real_store, substitute_mapping)
    with pytest.raises(expected_data[0][0], match=expected_data[0][1]):
        sa.b
    with pytest.raises(expected_data[1][0], match=expected_data[1][1]):
        sa['c']
    with pytest.raises(expected_data[2][0], match=expected_data[2][1]):
        sa.real_store['d']
