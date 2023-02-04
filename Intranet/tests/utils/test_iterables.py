# coding: utf-8

from __future__ import unicode_literals

from cab.utils import iterables


def test_squash_by_key():
    iterable = [
        {
            'date': '2015-01-01',
            'value': 10,
        },
        {
            'date': '2015-02-01',
            'value': 5,
        },
        {
            'date': '2015-02-01',
            'value': 5,
        },
        {
            'date': '2015-03-01',
            'value': 10,
        },
    ]
    key = 'date'

    def squasher(accumulator, item):
        accumulator['value'] += item['value']

    squashed = iterables.squash_by_key(iterable, key, squasher)
    assert squashed == [
        {
            'date': '2015-01-01',
            'value': 10,
        },
        {
            'date': '2015-02-01',
            'value': 10,
        },
        {
            'date': '2015-03-01',
            'value': 10,
        },
    ]

