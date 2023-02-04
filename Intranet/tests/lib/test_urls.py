from __future__ import unicode_literals

import pytest

from review.lib.urls import remove_query_param


@pytest.mark.parametrize(
    "url, param, expected", [
        ('http://f.oo/bar?foo=bar', 'zzz', 'http://f.oo/bar?foo=bar'),
        ('http://f.oo/bar?foo=bar&baz=baz&fizz=fz', 'baz', 'http://f.oo/bar?foo=bar&fizz=fz'),
        ('http://f.oo/bar?foo=bar', 'foo', 'http://f.oo/bar'),
    ]
)
def test_remove_query_param(url, param, expected):
    assert remove_query_param(url, param) == expected
