# coding: utf-8
import pytest

from review.lib import std


NESTED = {
    'a': 'b',
    'c': {
        'd': 'e',
    },
    'f': {
        'g': {
            'h': 'i',
        }
    }
}


@pytest.mark.parametrize('keys_chain,expected', [
    ('a', 'b'),
    ('c', {'d': 'e'}),
    ('c.d', 'e'),
    ('f', {'g': {'h': 'i'}}),
    ('f.g', {'h': 'i'}),
    ('f.g.h', 'i'),
    ('x', None),
    ('a.b', None),
    ('a.b.c', None),
    ('f.g.h.x', None),
])
def test_safe_itemgetter(keys_chain, expected):
    result = std.safe_itemgetter(
        dict_=NESTED,
        keys_chain=keys_chain
    )
    if expected is None:
        assert result is None
    else:
        assert result == expected


SOME_DICT = {
    'wat': 10,
    'wait__wat': 20,
}


@pytest.mark.parametrize('prefix,expected', [
    ('wow', {}),
    ('wait__', {'wat': 20}),
])
def test_subdict_by_key_prefix(prefix, expected):
    assert std.subdict_by_key_prefix(SOME_DICT, prefix) == expected
