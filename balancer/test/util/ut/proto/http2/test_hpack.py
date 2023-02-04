# -*- coding: utf-8 -*-
import sys
import pytest
import balancer.test.util.proto.http2.hpack._hpack as hpack
import balancer.test.util.proto.http2.hpack.huffman as huffman
import balancer.test.util.proto.http2.message as mod_msg


def _build_params():
    pre_result = list()
    for n in range(1, 8):
        pre_result.append((n, 0))
        max_prefix = 2 ** (8 - n) - 1
        if max_prefix > 1:
            pre_result.append((n, max_prefix / 2))
        pre_result.append((n, max_prefix))
    result = set()
    for n, prefix in pre_result:
        result.add((n, prefix, 0))
        result.add((n, prefix, 1))
        result.add((n, prefix, 2 ** n - 2))
        result.add((n, prefix, 2 ** n - 1))
        result.add((n, prefix, 2 ** n))
        result.add((n, prefix, 2 ** n + 1))
        result.add((n, prefix, sys.maxint))
    return list(result)


PARAMS = _build_params()


@pytest.mark.parametrize(('n', 'prefix', 'value'), PARAMS)
def test_encode_decode_int(n, prefix, value):
    encoded = hpack.encode_int(n, prefix, value)
    proc_prefix, proc_value, data = hpack.decode_int(n, encoded)
    assert proc_prefix == prefix
    assert proc_value == value
    assert data == ''


@pytest.mark.parametrize('data', [
    'www.example.com',
    'Pink Floyd',
    'Led Zeppelin',
])
def test_huffman(data):
    assert huffman.decode(huffman.encode(data)) == data


@pytest.mark.parametrize(
    'field',
    [
        mod_msg.HeaderField('Led', 'Zeppelin'),
        mod_msg.HeaderField(mod_msg.HName('Led', compressed=True), 'Zeppelin'),
        mod_msg.HeaderField('Led', mod_msg.HValue('Zeppelin', compressed=True)),
        mod_msg.HeaderField(2, 'Zeppelin'),
    ],
    ids=[
        'simple',
        'compressed_name',
        'compressed_value',
        'indexed_name',
    ]
)
def test_hpack(field):
    encoder = hpack.Encoder()
    expected_field = encoder.table.resolve(field)
    parsed_field, data = hpack.Decoder().decode_one(encoder.encode_one(field))
    assert parsed_field == expected_field
    assert data == ''


def test_hpack_indexed_field():
    field = mod_msg.HeaderField(2)
    parsed_field, data = hpack.Decoder().decode_one(hpack.Encoder().encode_one(field))
    assert parsed_field == mod_msg.HeaderField(':method', 'GET')
    assert data == ''


def test_hpack_dynamic_table_indexed_name():
    led_name = 'Led'
    encoder = hpack.Encoder()
    data1 = encoder.encode_one(mod_msg.HeaderField(led_name, 'Zeppelin', indexing=mod_msg.Indexing.YES))
    data2 = encoder.encode_one(mod_msg.HeaderField('Pink', 'Floyd', indexing=mod_msg.Indexing.YES))
    led_index = encoder.table.find(led_name)
    data3 = encoder.encode_one(mod_msg.HeaderField(led_index, 'Lamp'))

    decoder = hpack.Decoder()
    decoder.decode_one(data1)
    decoder.decode_one(data2)
    parsed_field, data = decoder.decode_one(data3)
    assert parsed_field == mod_msg.HeaderField(led_name, 'Lamp')


def test_hpack_dynamic_table_indexed_field():
    led_name = 'Led'
    led_value = 'Zeppelin'
    encoder = hpack.Encoder()
    data1 = encoder.encode_one(mod_msg.HeaderField(led_name, led_value, indexing=mod_msg.Indexing.YES))
    data2 = encoder.encode_one(mod_msg.HeaderField('Pink', 'Floyd', indexing=mod_msg.Indexing.YES))
    led_index = encoder.table.find(led_name, led_value)
    data3 = encoder.encode_one(mod_msg.HeaderField(led_index))

    decoder = hpack.Decoder()
    decoder.decode_one(data1)
    decoder.decode_one(data2)
    parsed_field, data = decoder.decode_one(data3)
    assert parsed_field == mod_msg.HeaderField(led_name, led_value)
