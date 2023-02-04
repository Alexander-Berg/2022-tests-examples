import pytest

import yt.yson.yson_types as yson_types

import maps.analyzer.pylibs.schema as s

from maps.analyzer.pylibs.test_tools.schema import infer_schema


# short synonyms for types
u = yson_types.YsonUint64
l = yson_types.YsonInt64


def test_infer():
    assert infer_schema([
        {'foo': "str", 'bar': u(8)},
        {'foo': "str2", 'bar': u(10)},
    ]) == table([column('foo', s.String), column('bar', s.Uint64)])

    assert infer_schema([
        {'foo': "str", 'bar': u(8)},
        {'bar': u(8)},
    ]) == table([column('foo', s.Optional(s.String)), column('bar', s.Uint64)])

    assert infer_schema([
        {'foo': "str", 'bar': u(8)},
        {'foo': u(10), 'bar': None},
    ]) == table([column('foo', s.Any), column('bar', s.Optional(s.Uint64))])


def test_infer_hints():
    assert infer_schema(
        [
            {'foo': 'str', 'bar': None},
            {'foo': 'str2'},
        ],
        hints=[column('bar', s.Optional(s.String))],
    ) == table([column('foo', s.String), column('bar', s.Optional(s.String))])

    # non-strict schema not validated
    assert infer_schema(
        [
            {'foo': 'str', 'bar': u(8)},
            {'foo': 'str2', 'bar': u(10)},
        ],
        hints=[column('bar', s.String)],
    ) == table([column('foo', s.String), column('bar', s.Uint64)])

    # strict validated and fails
    with pytest.raises(RuntimeError):
        infer_schema(
            [
                {'foo': 'str', 'bar': u(8)},
                {'foo': 'str2', 'bar': u(10)},
            ],
            hints=[column('bar', s.String)],
            strict=True,
        )


def test_infer_narrow():
    assert infer_schema(
        [
            {'foo': 'str', 'bar': u(8), 'dict': [['key', u(10)]]},
            {'foo': None, 'bar': u(10), 'dict': [['key2', u(12)]]},
        ],
        hints=[column('foo', s.Optional(s.String)), column('bar', s.Uint8), column('dict', s.Dict(s.String, s.Uint16))],
        strict=True,
        narrow=True,
    ) == table([column('foo', s.Optional(s.String)), column('bar', s.Uint8), column('dict', s.Dict(s.String, s.Uint16))])

    assert infer_schema(
        [
            {'dict': [['key', u(10)]]},
            {'dict': None},
        ],
        hints=[column('dict', s.Optional(s.Dict(s.String, s.Uint16)))],
        strict=True,
        narrow=True,
    ) == table([column('dict', s.Optional(s.Dict(s.String, s.Uint16)))])

    with pytest.raises(RuntimeError):
        infer_schema(
            [
                {'dict': [['key', u(10)]]},
                {'dict': None},
            ],
            hints=[column('dict', s.Dict(s.String, s.Uint16))],
            strict=True,
            narrow=True,
        )


def test_infer_hint_optional():
    assert infer_schema(
        [{'foo': 'str'}, {'foo': 'str2'}],
        hints=[column('foo', s.Optional(s.String))],
        strict=True,
    ) == table([column('foo', s.Optional(s.String))])

    with pytest.raises(RuntimeError):
        infer_schema(
            [{'foo': 'str'}, {'foo': None}],
            hints=[column('foo', s.String)],
            strict=True,
        )


def test_infer_complex_narrow():
    # inferred value only may be yson in case of complex type
    # without narrow it will fail
    with pytest.raises(RuntimeError):
        infer_schema(
            [],
            hints=[column('foo', s.Dict(s.String, s.String))],
            strict=True,
        )

    # but should be ok for primitives
    assert infer_schema(
        [{'foo': 'str'}],
        hints=[column('foo', s.String)],
        strict=True,
    ) == table([column('foo', s.String)])


def table(cols):
    return s.table(cols, None)


def column(name, ty):
    return s.column(name, ty, None)
