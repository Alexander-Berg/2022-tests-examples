# encoding: utf-8
import msgpack
import cppzoom


def test_double_to_int_conv():
    record = cppzoom.ZRecord.from_dict({
        "double_summ": 1.23,
        "int_summ": 4,
        "bigint_summ": 1 << 53,
        "not_very_bigint_summ": (1 << 53) - 1,
        "negative_bigint_summ": -(1 << 53),
        "negative_not_very_bigint_summ": -(1 << 53) + 1
    })
    decoded = dict(msgpack.loads(record.dumps()))
    assert decoded == {
        'double_summ': 1.23,
        'int_summ': 4,
        'bigint_summ': 9007199254740992.0,
        'not_very_bigint_summ': 9007199254740991,
        'negative_bigint_summ': -9007199254740992.0,
        'negative_not_very_bigint_summ': -9007199254740991
    }
    assert isinstance(decoded["double_summ"], float)
    assert isinstance(decoded["int_summ"], int)
    assert isinstance(decoded["bigint_summ"], float)
    assert isinstance(decoded["not_very_bigint_summ"], int)
    assert isinstance(decoded["negative_bigint_summ"], float)
    assert isinstance(decoded["negative_not_very_bigint_summ"], int)


def assert_type_is(l, tp):
    assert all(isinstance(x, tp) for x in l)


def test_vector_vs_aver_conv():
    record = cppzoom.ZRecord.from_dict({
        "homonym_aver": [1, 2.0],  # count, summ
        "homonym_list": [1.0, 2.0],
        "plain_list": [1, 2, 3],
        "int_list": [1, 2],
        "float_list": [1.0, 2.0]
    })
    decoded = dict(msgpack.loads(record.dumps()))
    assert decoded == {
        'homonym_aver': [1, 2.0],
        'homonym_list': [1.0, 2.0],
        "plain_list": [1, 2, 3],
        'int_list': [1.0, 2.0],
        'float_list': [1.0, 2.0]
    }
    assert isinstance(decoded['homonym_aver'][0], int) and isinstance(decoded['homonym_aver'][1], float)
    assert_type_is(decoded['homonym_list'], float)
    assert_type_is(decoded['plain_list'], int)
    assert_type_is(decoded['int_list'], float)
    assert_type_is(decoded['float_list'], float)


def test_ugram_compress():
    record = cppzoom.ZRecord.from_dict({
        "big_hgram": ["ugram", [[i, i] for i in xrange(100)]]
    })
    decoded = dict(msgpack.loads(record.dumps()))
    assert len(decoded["big_hgram"][1]) == 51
