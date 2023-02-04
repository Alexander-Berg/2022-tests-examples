import pytest

import math

import maps.analyzer.pylibs.schema.types as t


def test_integers():
    SIGNED = [
        t.Int64, t.Int32, t.Int16, t.Int8,
        t.Interval,
    ]
    UNSIGNED = [
        t.Uint64, t.Uint32, t.Uint16, t.Uint8,
        t.Date, t.Datetime, t.Timestamp,
    ]

    for ty in SIGNED + UNSIGNED:
        assert ty.cast(123) == 123
        assert ty.cast("123") == 123
        assert ty.cast("0") == 0

    for ty in SIGNED:
        assert ty.cast(-10) == -10
        assert ty.cast("-10") == -10

    for ty in UNSIGNED:
        with pytest.raises(ValueError):
            ty.cast(-10)
        with pytest.raises(ValueError):
            ty.cast("-10")

    for ty in SIGNED + UNSIGNED:
        assert ty.cast(ty.bounds[0]) == ty.bounds[0]
        assert ty.cast(ty.bounds[1]) == ty.bounds[1]

        with pytest.raises(ValueError):
            ty.cast(ty.bounds[0] - 1)

        with pytest.raises(ValueError):
            ty.cast(ty.bounds[1] + 1)


def test_double():
    assert t.Double.cast(123.0) == 123.0
    assert t.Double.cast("123.0") == 123.0
    assert t.Double.cast("1e3") == 1000.0
    assert t.Double.cast("-1e3") == -1000.0
    assert t.Double.cast("inf") == float("inf")
    assert t.Double.cast("-inf") == float("-inf")
    assert math.isnan(t.Double.cast("nan"))

    with pytest.raises(ValueError):
        t.Double.cast("n")


def test_string():
    STR = [t.String, t.Utf8]

    assert t.String.cast(b'foo') == b'foo'
    assert t.String.cast('foo') == b'foo'
    assert t.String.cast(u'foo') == b'foo'
    assert t.String.cast(123) == b'123'
    assert t.String.cast(float("nan")) == b'nan'

    assert t.Utf8.cast(b'foo') == u'foo'
    assert t.Utf8.cast('foo') == u'foo'
    assert t.Utf8.cast(u'foo') == u'foo'
    assert t.Utf8.cast(123) == u'123'
    assert t.Utf8.cast(float("nan")) == u'nan'

    # check text (synonym for utf8) works with default strings
    assert str(t.Text.cast(b'foo')) == 'foo'
    assert str(t.Text.cast('foo')) == 'foo'
    assert str(t.Text.cast(u'foo')) == 'foo'
    assert str(t.Text.cast(123)) == '123'
    assert str(t.Text.cast(float("nan"))) == 'nan'

    # check decode
    assert t.Text.decode(b'foo') == 'foo'
    assert t.Bytes.decode(b'foo') == b'foo'

    for ty in STR:
        with pytest.raises(ValueError):
            ty.cast(None)

        with pytest.raises(ValueError):
            ty.cast([123])

        with pytest.raises(ValueError):
            ty.cast({'foo': 123})

    VALID_UTF8 = b'\xC2\xA2'
    INVALID_UTF8 = b'\xC2\xC2\xA2'

    assert t.String.cast(VALID_UTF8) == VALID_UTF8
    assert t.String.cast(INVALID_UTF8) == INVALID_UTF8

    assert t.Utf8.cast(VALID_UTF8) == VALID_UTF8.decode('utf-8')

    with pytest.raises(ValueError):
        t.Utf8.cast(INVALID_UTF8)


def test_bool():
    assert t.Boolean.cast(True)
    assert not t.Boolean.cast(False)
    assert t.Boolean.cast(2)
    assert t.Boolean.cast(1)
    assert not t.Boolean.cast(0)

    with pytest.raises(ValueError):
        t.Boolean.cast('')

    with pytest.raises(ValueError):
        t.Boolean.cast('true')

    with pytest.raises(ValueError):
        t.Boolean.cast('false')


def test_optional():
    assert t.Optional(t.Int32).cast(None) is None
    assert t.Optional(t.Int32).cast(123) == 123

    with pytest.raises(ValueError):
        t.Optional(t.Int32).cast([123])

    # inner optional encoded as single-elem list
    OPT_OPT = t.Optional(t.Optional(t.Int32))
    assert OPT_OPT.cast(None) is None
    assert OPT_OPT.cast([None]) == [None]
    assert OPT_OPT.cast(["123"]) == [123]

    OPT_LIST = t.Optional(t.List(t.Int32))
    assert OPT_LIST.cast(None) is None
    assert OPT_LIST.cast([1, 2, "3"]) == [1, 2, 3]
    assert OPT_LIST.decode([1, 2, 3]) == [1, 2, 3]

    with pytest.raises(ValueError):
        OPT_LIST.cast([[1, 2, 3]])

    OPT_LIST_OPT = t.Optional(t.List(t.Optional(t.Int32)))
    assert OPT_LIST_OPT.cast([1, None, "2"]) == [1, None, 2]
    assert OPT_LIST_OPT.decode([1, None, 2]) == [1, None, 2]


def test_list():
    LIST = t.List(t.Double)

    assert LIST.cast([]) == []
    assert LIST.cast([1, "2", "3.3"]) == [1.0, 2.0, 3.3]

    with pytest.raises(ValueError):
        LIST.cast(None)


def test_tuple():
    TUPLE = t.Tuple(
        t.Int32,
        t.Optional(t.Int32),
        t.Optional(t.Optional(t.String)),
    )

    assert TUPLE.cast((1, "2", ["foo"])) == (1, 2, [b"foo"])
    assert TUPLE.cast((1, None, None)) == (1, None, None)
    assert TUPLE.cast((1, None, [None])) == (1, None, [None])
    assert TUPLE.decode((1, None, [b"foo"])) == (1, None, [b"foo"])

    with pytest.raises(ValueError):
        TUPLE.cast((1,))

    with pytest.raises(ValueError):
        TUPLE.cast((1, 2, ["foo"], "extra"))


def test_dict():
    DICT = t.Dict(t.Int32, t.String)

    def wrap_cast(d):
        return dict(DICT.cast(list(d.items())))

    assert DICT.cast([("0", "foo"), ("2", "hello")]) == [(0, b"foo"), (2, b"hello")]
    assert wrap_cast({0: "hi", "1": "blah"}) == {0: b"hi", 1: b"blah"}

    assert DICT.decode([(0, b"foo"), (1, b"bar")]) == {0: b"foo", 1: b"bar"}
    assert DICT.encode({0: b"foo", 1: b"bar"}) == [(0, b"foo"), (1, b"bar")]

    COMPLEX_DICT = t.Dict(t.List(t.String), t.Optional(t.Int32))
    assert COMPLEX_DICT.cast([(["foo"], None), ([], "123")]) == [([b"foo"], None), ([], 123)]


def test_struct():
    STRUCT = t.Struct(
        name=t.Text,
        age=t.Optional(t.Uint16),
    )

    assert STRUCT.cast({'name': u'Ivan'}) == {'name': u'Ivan', 'age': None}
    assert STRUCT.cast({'name': u'Ivan', 'age': '20'}) == {'name': u'Ivan', 'age': 20}
    assert STRUCT.decode({b'name': b'Ivan', b'age': 20}) == {'name': u'Ivan', 'age': 20}
    assert STRUCT.decode({'name': b'Ivan', 'age': 20}) == {'name': u'Ivan', 'age': 20}
    assert STRUCT.encode({'name': 'Ivan', 'age': 20}) == {'name': u'Ivan', 'age': 20}

    with pytest.raises(ValueError):
        STRUCT.cast({'age': 20})

    with pytest.raises(ValueError):
        STRUCT.cast({'name': u'Ivan', 'age': 20, 'extra': 123})


def test_variant():
    NAMED_VARIANT = t.Variant(
        lhs=t.String,
        rhs=t.Int32,
    )

    assert NAMED_VARIANT.cast(['lhs', "foo"]) == ('lhs', b"foo")
    assert NAMED_VARIANT.cast(['rhs', "123"]) == ('rhs', 123)
    assert NAMED_VARIANT.decode([b'lhs', b"foo"]) == ('lhs', b"foo")
    assert NAMED_VARIANT.decode([b'rhs', 123]) == ('rhs', 123)
    assert NAMED_VARIANT.encode(('lhs', b"foo")) == ('lhs', b"foo")
    assert NAMED_VARIANT.encode(('rhs', 123)) == ('rhs', 123)

    with pytest.raises(ValueError):
        NAMED_VARIANT.cast(('rhs', "error"))

    with pytest.raises(ValueError):
        NAMED_VARIANT.cast(('hs', "123"))

    with pytest.raises(ValueError):
        NAMED_VARIANT.cast(('rhs', "123", "321"))

    INDEXED_VARIANT = t.Variant(t.String, t.Int32)

    assert INDEXED_VARIANT.cast((0, "foo")) == (0, b"foo")
    assert INDEXED_VARIANT.cast((1, "123")) == (1, 123)
    assert INDEXED_VARIANT.decode((0, b"foo")) == (0, b"foo")
    assert INDEXED_VARIANT.decode((1, 123)) == (1, 123)
    assert INDEXED_VARIANT.encode((0, b"foo")) == (0, b"foo")
    assert INDEXED_VARIANT.encode((1, 123)) == (1, 123)

    with pytest.raises(ValueError):
        INDEXED_VARIANT.cast((2, "123"))

    with pytest.raises(ValueError):
        INDEXED_VARIANT.cast(('key', "123"))

    # no mixed content allowed
    with pytest.raises(ValueError):
        t.Variant(t.String, y=t.Double)


def test_tagged():
    TAGGED = t.Tagged('url', t.String)
    assert TAGGED.cast(123) == t.String.cast(123)


def test_subtype():
    # sorted from specific to general
    SIGNED_INTS = [t.Int8, t.Int16, t.Int32, t.Int64]
    UNSIGNED_INTS = [t.Uint8, t.Uint16, t.Uint32, t.Uint64]
    STRS = [t.Utf8, t.String]

    def ensure_order(lst):
        for i, lhs in enumerate(lst):
            for j, rhs in enumerate(lst):
                assert lhs.is_subtype(rhs) == (i <= j)

    ensure_order(SIGNED_INTS)
    ensure_order(UNSIGNED_INTS)
    ensure_order(STRS)

    # no one subtype of another
    DIFFERENT_TYPES = [
        t.Int32, t.Uint32, t.String, t.Double, t.Boolean, t.Date, t.Datetime, t.Timestamp, t.Interval,
        t.List(t.String), t.Tuple(t.String, t.String), t.Dict(t.String, t.String),
        t.Struct(key=t.String, value=t.String), t.Variant(left=t.String, right=t.String),
        t.Tagged('url', t.String),
    ]

    for i, lhs in enumerate(DIFFERENT_TYPES):
        for j, rhs in enumerate(DIFFERENT_TYPES):
            assert lhs.is_subtype(rhs) == (i == j)

    for ty in DIFFERENT_TYPES:
        assert ty.is_subtype(t.Yson)


def test_optional_subtype():
    def opt_sub(ty):
        return ty.is_subtype(t.Optional(ty))

    assert opt_sub(t.Int32)
    # only primitives can be propagated
    assert not opt_sub(t.Optional(t.Int32))
    assert not opt_sub(t.List(t.Int32))

    # primitive types propagated under optionals
    assert t.Optional(t.Int16).is_subtype(t.Optional(t.Int32))


def test_yson_subtype():
    # anything is subtype
    assert t.Uint32.is_subtype(t.Yson)
    assert t.List(t.Uint32).is_subtype(t.Yson)
    assert t.List(t.Optional(t.Double)).is_subtype(t.Yson)

    # except top-level optionals
    assert not t.Optional(t.Double).is_subtype(t.Yson)
    # old `any` is optional yson, which is most general type
    assert t.Optional(t.Double).is_subtype(t.Any)
    assert t.Yson.is_subtype(t.Any)


def test_supertype():
    # same kind of types extended
    assert t.Int8 | t.Int32 == t.Int32
    assert t.Optional(t.Int8) | t.Int16 == t.Optional(t.Int16)
    # different types goes to yson/any
    assert t.Int8 | t.Uint64 == t.Yson
    assert t.Optional(t.Int8) | t.Uint64 == t.Any
    # supertype with self doesnt change type
    assert t.Int8 | t.Int8 == t.Int8
    assert t.Yson | t.Yson == t.Yson
    assert t.Any | t.Any == t.Any
    assert t.List(t.Int8) | t.List(t.Int8) == t.List(t.Int8)
    # complex types may become optional
    assert t.Optional(t.List(t.Double)) | t.List(t.Double) == t.Optional(t.List(t.Double))
    # but different complex types extends to yson/any
    assert t.List(t.Double) | t.Tuple(t.Double) == t.Yson
    assert t.Optional(t.List(t.Double)) | t.Tuple(t.Double) == t.Any


def test_supertype_narrow():
    def sup(lhs, rhs):
        return t.TypeV3.supertype(lhs, rhs, narrow_any=True)

    # any narrowed to complex type
    assert sup(t.Any, t.List(t.Double)) == t.List(t.Double)
    assert sup(t.Any, t.Optional(t.List(t.Double))) == t.Optional(t.List(t.Double))
    assert sup(t.Any, t.Optional(t.Optional(t.Int32))) == t.Optional(t.Optional(t.Int32))
    # but not to primitives
    assert sup(t.Any, t.Int32) == t.Any
    assert sup(t.Any, t.Optional(t.Int32)) == t.Any
