# coding: utf-8
from __future__ import unicode_literals
import pytest
import six

from awacs.wrappers import luaparser
from awacs.wrappers.luaparser import read_string, Reader


def test_read_string():
    r = Reader('pri\\avet')
    assert r.current == 'p'

    assert read_string('pri\\avet') == 'pri\avet'
    assert read_string('\\xAA') == chr(0xaa)

    assert read_string('u{2343}') == 'u{2343}'
    assert read_string('\\u{2343}') == u'\u2343'

    lua_str = r'\\' + '\\u{2343}'
    parsed = read_string(lua_str)
    expected = u'\\' + u'\u2343'
    assert type(parsed) is six.text_type
    assert parsed == expected

    lua_str = r'\\' + '\\u2343'
    with pytest.raises(ValueError) as e:
        read_string(lua_str)
    assert e.match('missing "{"')

    lua_str = '\\'
    with pytest.raises(ValueError) as e:
        read_string(lua_str)
    assert e.match('unfinished string')

    for lua_str in (r'\n', '\n'):
        with pytest.raises(ValueError) as e:
            read_string(lua_str)
        assert e.match('multiline strings are not supported')


def test_parse_call():
    call = luaparser.parse_call('f(g(456), "123")')
    parsed_dumped_call = luaparser.parse_call(luaparser.dump_call(call))
    expected_call = luaparser.RawCall('f', [luaparser.RawCall('g', [456]), '123'])
    assert call == expected_call
    assert call == parsed_dumped_call

    call = luaparser.parse_call("f(123, '456', g())")
    parsed_dumped_call = luaparser.parse_call(luaparser.dump_call(call))
    expected_call = luaparser.RawCall('f', [123, '456', luaparser.RawCall('g', [])])
    assert call == expected_call
    assert call == parsed_dumped_call

    call = luaparser.parse_call("f(40, -123)")
    assert call == luaparser.RawCall('f', [40, -123])

    call = luaparser.parse_call("f(-123)")
    assert call == luaparser.RawCall('f', [-123])

    call = luaparser.parse_call("f(true)")
    assert call == luaparser.RawCall('f', [True])

    call = luaparser.parse_call("f(false)")
    assert call == luaparser.RawCall('f', [False])

    with pytest.raises(ValueError) as e:
        luaparser.parse_call("f(456, 123')")
    e.match('unexpected token u?"\'" at position 10')

    with pytest.raises(ValueError) as e:
        luaparser.parse_call("f() + 123")
    e.match("unexpected token u?'\+' at position 4")  # noqa

    with pytest.raises(ValueError) as e:
        luaparser.parse_call("f(")
    e.match('EOF in multi-line statement at position 2')
