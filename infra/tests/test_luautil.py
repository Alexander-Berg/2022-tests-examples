# coding: utf-8
from __future__ import unicode_literals
import random

import pytest
import six
from six.moves import range as xrange

from awacs.wrappers.luautil import read_string, dump_string


def test_read_string():
    assert read_string(r'pri\avet') == 'pri\avet'
    assert read_string(r'pri\bvet') == 'pri\bvet'
    assert read_string(r'pri\fvet') == 'pri\fvet'
    assert read_string(r'pri\nvet') == 'pri\nvet'
    assert read_string(r'pri\rvet') == 'pri\rvet'
    assert read_string(r'pri\tvet') == 'pri\tvet'
    assert read_string(r'pri\bvet') == 'pri\bvet'
    assert read_string(r'\x7a') == 'z'
    assert read_string(r'\\x7a') == r'\x7a'
    assert read_string(r'\112') == 'p'
    assert read_string(r'\\') == '\\'
    assert read_string(r'привет') == 'привет'

    with pytest.raises(ValueError) as e:
        read_string('\n')
    assert e.match('unfinished string')

    with pytest.raises(ValueError) as e:
        read_string('\\u{2343}')
    assert e.match('invalid escape sequence')

    with pytest.raises(ValueError) as e:
        read_string(r'\999')
    assert e.match('decimal escape too large')


def test_dump_string():
    assert dump_string('pri\avet') == r'pri\avet'
    assert dump_string('pri\bvet') == r'pri\bvet'
    assert dump_string('pri\fvet') == r'pri\fvet'
    assert dump_string('pri\nvet') == r'pri\nvet'
    assert dump_string('pri\rvet') == r'pri\rvet'
    assert dump_string('pri\tvet') == r'pri\tvet'
    assert dump_string('pri\bvet') == r'pri\bvet'
    assert dump_string(r'"kuku\"') == r'\"kuku\\\"'
    assert dump_string('привет') == 'привет'
    assert dump_string('привет\0') == r'привет\x00'
    assert dump_string(r'!@#$%^&*()\\') == r'!@#$%^&*()\\\\'


def test_roundtrips():
    """
    Lua 5.1.4  Copyright (C) 1994-2008 Lua.org, PUC-Rio
    > print('\\"' == "\\\"")
    true
    > print('"' == "\"")
    true
    """
    suite = {
        r'pri\avet': 'pri\avet',
        r'"': '"',
        r'\"': '"',
        r'\\': '\\',
        r'\\\"': '\\"',
        r'\\"': r'\"',
        r'(exp|exp-beta|ab)\\.test\\.yandex-team\\.ru': '(exp|exp-beta|ab)\.test\.yandex-team\.ru',  # noqa
        r'exp\\.test': r'exp\.test',  # noqa
    }
    for x, y in six.iteritems(suite):
        assert read_string(x) == y
        assert read_string(dump_string(read_string(x))) == y


def test_randomroundtrips():
    for i in xrange(1000):
        x = ''.join([six.unichr(random.randint(0, 300)) for j in xrange(100)])
        assert read_string(dump_string(read_string(dump_string(x)))) == x
