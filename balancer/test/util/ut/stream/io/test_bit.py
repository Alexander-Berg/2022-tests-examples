# -*- coding: utf-8 -*-
import pytest
import balancer.test.util.stream.io.stream as stream
import balancer.test.util.stream.io.bit as bit


DATA = [
    'a',
    'abc',
    'Pink Floyd',
    'x' * 100,
]


@pytest.mark.parametrize('data', DATA)
def test_write_bytes(data):
    bit_stream = stream.StringStream('')
    writer = bit.BitWriter(bit_stream)
    writer.write_bytes(data)
    assert str(bit_stream) == data


@pytest.mark.parametrize('data', DATA)
def test_read_bytes(data):
    bit_stream = stream.StringStream(data)
    reader = bit.BitReader(bit_stream)
    read_data = reader.read_bytes(len(data))
    assert read_data == data


@pytest.mark.parametrize(
    ('data', 'value'),
    [
        ('\x00\x00\x00\x00', 0),
        ('\x00\x00\x00\x01', 1),
        ('\x80\x00\x00\x00', 2 ** 31),
    ],
    ids=[
        'zero',
        'one',
        'unsigned',
    ]
)
def test_read_int(data, value):
    reader = bit.BitReader(stream.StringStream(data))
    assert reader.next_int() == value


@pytest.mark.parametrize(
    ('data', 'len1', 'value1', 'len2', 'value2'),
    [
        ('\x00\x00\x00\x00', 16, 0, 16, 0),
        ('\x00\x01\x00\x01', 16, 1, 16, 1),
        ('\x80\x00\x80\x00', 16, 2 ** 15, 16, 2 ** 15),
        ('\xc0\x00\x00\x01', 1, 1, 31, 2 ** 30 + 1),
    ],
    ids=[
        'zero,zero',
        'one,one',
        'unsigned,unsigned',
        'not_aligned',
    ],
)
def test_read_two_ints(data, len1, value1, len2, value2):
    reader = bit.BitReader(stream.StringStream(data))
    assert reader.next_int(len1) == value1
    assert reader.next_int(len2) == value2
