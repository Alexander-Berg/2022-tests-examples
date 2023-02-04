# -*- coding: utf-8 -*-
import string
import json
import balancer.test.util.proto.http2.hpack._hpack as hpack
from balancer.test.util.proto.http2.message import HeaderField as HF
from balancer.test.util.proto.http2.message import HName, HValue
from balancer.test.util.proto.http2.message import Indexing as IDX


STATIC_TABLE_SIZE = 61
DEFAULT_SIZE = 4096
DYN_TABLE_SIZE = 2048
NAME = 'led'
VALUE = 'zeppelin'
HLEN = len(NAME) + len(VALUE) + 32
NAME2 = 'pink'
VALUE2 = 'floyd'
HLEN2 = len(NAME2) + len(VALUE2) + 32
VALUE2_SIZE = DYN_TABLE_SIZE - HLEN - len(NAME2) - 32
VALUE2_DEFAULT_SIZE = DEFAULT_SIZE - HLEN - len(NAME2) - 32
NAME3 = 'ac'
VALUE3 = 'dc'
HLEN3 = len(NAME3) + len(VALUE3) + 32


OK_SYMBOLS = [i for i in range(256) if chr(i) not in '\x00\r\n']
OK_STRICT_SYMBOLS = [i for i in range(256) if chr(i) not in '\x00\r\n\t ']
OK_NAME_SYMBOLS = [i for i in OK_STRICT_SYMBOLS if chr(i) not in string.ascii_uppercase + ':']


def encode_str(data):
    return data.encode('hex')


TESTS = {
    'simple': [[HF('name', 'value')]],
    'multiple_headers': [[HF('led', 'Zeppelin'), HF('pink', 'Floyd'), HF('ac', 'DC')]],
    'all_ascii_name': [[HF(chr(i), 'value{}'.format(i)) for i in OK_NAME_SYMBOLS]],
    'all_ascii_value': [[HF('header{}'.format(i), chr(i)) for i in OK_STRICT_SYMBOLS]],
    'all_ascii_suffix_value': [[HF('header{}'.format(i), 'a' + chr(i)) for i in OK_SYMBOLS]],
    'all_ascii_infix_value': [[HF('header{}'.format(i), 'a' + chr(i) + 'b') for i in OK_SYMBOLS]],
    'empty_value': [[HF('name', '')]],
    'indexed_field': [[HF(5)]],
    'indexed_name': [[HF(4, '/yandsearch')]],
    'indexing': [[HF('name', 'value', indexing=IDX.YES)]],
    'indexing_static_indexed_name': [[HF(4, '/yandsearch', indexing=IDX.YES)]],
    'indexed_field_dynamic_table': [[HF('name', 'value', indexing=IDX.YES)], [HF(STATIC_TABLE_SIZE + 1)]],
    'indexed_name_dynamic_table': [[HF('name', 'value1', indexing=IDX.YES)], [HF(STATIC_TABLE_SIZE + 1, 'value2')]],
    'indexing_dynamic_indexed_name': [[HF('name', 'value1', indexing=IDX.YES)],
                                      [HF(STATIC_TABLE_SIZE + 1, 'value2', indexing=IDX.YES)]],
    'indexing_same_block_indexed_field': [[HF('name', 'value', indexing=IDX.YES), HF(STATIC_TABLE_SIZE + 1)]],
    'indexing_same_block_indexed_name': [[HF('name', 'value1', indexing=IDX.YES), HF(STATIC_TABLE_SIZE + 1, 'value2')]],
    'indexing_multi': [[HF('name1', 'value1', indexing=IDX.YES)], [HF('name2', 'value2', indexing=IDX.YES)]],
    'indexing_multi_same_block': [[HF('name1', 'value1', indexing=IDX.YES), HF('name2', 'value2', indexing=IDX.YES)]],
    'indexed_field_multi': [[HF('name1', 'value2', indexing=IDX.YES), HF('name2', 'value2', indexing=IDX.YES),
                             HF(STATIC_TABLE_SIZE + 2)]],
    'never_indexed': [[HF('name', 'value', indexing=IDX.NEVER)]],
    'never_indexed_static_indexed_name': [[HF(4, '/yandsearch', indexing=IDX.NEVER)]],
    'never_indexed_dynamic_indexed_name': [[HF(4, '/yandsearch', indexing=IDX.NEVER)]],
    'indexed_field_wrong_index': [[HF(STATIC_TABLE_SIZE + 1)]],
    'indexed_name_wrong_index': [[HF(STATIC_TABLE_SIZE + 1, 'value')]],
    'indexed_field_wrong_index_not_empty_dynamic_table': [[HF('name', 'value', indexing=IDX.YES)],
                                                           [HF(STATIC_TABLE_SIZE + 2)]],
    'empty': [[]],
    'fit_default_size': [[HF(NAME, VALUE, indexing=IDX.YES)],
                         [HF(NAME2, 'A' * VALUE2_DEFAULT_SIZE, indexing=IDX.YES)]],
    'eviction_on_add': [[HF(NAME, VALUE, indexing=IDX.YES)],
                        [HF(NAME2, 'A' * (VALUE2_DEFAULT_SIZE + 1), indexing=IDX.YES)]],
    'eviction_on_add_same_block': [[
        HF(NAME, VALUE, indexing=IDX.YES),
        HF(NAME2, 'A' * (VALUE2_DEFAULT_SIZE + 1), indexing=IDX.YES),
    ]],
}

SIZE_UPDATE_TESTS = {key: [[DYN_TABLE_SIZE + 1, HF(2)]] + value for (key, value) in {
    'increase_size': [
        [DYN_TABLE_SIZE + 1, HF(NAME, VALUE, indexing=IDX.YES)],
        [HF(NAME2, 'A' * (VALUE2_SIZE + 1), indexing=IDX.YES)]
    ],
    'increase_size_same_block': [[
        DYN_TABLE_SIZE + 1, HF(NAME, VALUE, indexing=IDX.YES),
        HF(NAME2, 'A' * (VALUE2_SIZE + 1), indexing=IDX.YES),
    ]],
    'increase_size_eviction_on_add': [
        [DYN_TABLE_SIZE + 1, HF(NAME, VALUE, indexing=IDX.YES)],
        [HF(NAME2, 'A' * (VALUE2_SIZE + 2), indexing=IDX.YES)]
    ],
    'increase_size_eviction_on_add_same_block': [[
        DYN_TABLE_SIZE + 1, HF(NAME, VALUE, indexing=IDX.YES),
        HF(NAME2, 'A' * (VALUE2_SIZE + 2), indexing=IDX.YES),
    ]],
    'increase_size_header_does_not_fit': [[
        DYN_TABLE_SIZE + 1, HF(NAME, 'A' * (DYN_TABLE_SIZE - len(NAME) - 32 + 2)),
    ]],
    'decrease_size': [
        [DYN_TABLE_SIZE - 1, HF(NAME, VALUE, indexing=IDX.YES)],
        [HF(NAME2, 'A' * (VALUE2_SIZE - 1), indexing=IDX.YES)]
    ],
    'decrease_size_same_block': [[
        DYN_TABLE_SIZE - 1, HF(NAME, VALUE, indexing=IDX.YES),
        HF(NAME2, 'A' * (VALUE2_SIZE - 1), indexing=IDX.YES),
    ]],
    'decrease_size_eviction_on_add': [
        [DYN_TABLE_SIZE - 1, HF(NAME, VALUE, indexing=IDX.YES)],
        [HF(NAME2, 'A' * VALUE2_SIZE, indexing=IDX.YES)]
    ],
    'decrease_size_eviction_on_add_same_block': [[
        DYN_TABLE_SIZE - 1, HF(NAME, VALUE, indexing=IDX.YES),
        HF(NAME2, 'A' * VALUE2_SIZE, indexing=IDX.YES),
    ]],
    'decrease_size_header_does_not_fit': [[
        DYN_TABLE_SIZE - 1, HF(NAME, 'A' * (DYN_TABLE_SIZE - len(NAME) - 32)),
    ]],
    'decrease_size_eviction': [
        [HF(NAME, VALUE, indexing=IDX.YES), HF(NAME2, 'A' * (VALUE2_SIZE - HLEN3), indexing=IDX.YES)],
        [DYN_TABLE_SIZE - 1, HF(NAME3, VALUE3, indexing=IDX.YES)],
    ],
    'decrease_size_not_evict': [
        [HF(NAME, VALUE, indexing=IDX.YES), HF(NAME2, 'A' * (VALUE2_SIZE - HLEN3 - 1), indexing=IDX.YES)],
        [DYN_TABLE_SIZE - 1, HF(NAME3, VALUE3, indexing=IDX.YES)],
    ],
    'two_updates_increasing_eviction': [
        [HF(NAME, VALUE, indexing=IDX.YES), HF(NAME2, VALUE2, indexing=IDX.YES)],
        [HLEN + HLEN2 - 1, DYN_TABLE_SIZE - 1, HF(NAME3, VALUE3, indexing=IDX.YES)],
    ],
    'two_updates_increasing_not_evict': [
        [HF(NAME, VALUE, indexing=IDX.YES), HF(NAME2, VALUE2, indexing=IDX.YES)],
        [HLEN + HLEN2, DYN_TABLE_SIZE - 1, HF(NAME3, VALUE3, indexing=IDX.YES)],
    ],
    'two_updates_decreasing_eviction': [
        [HF(NAME, VALUE, indexing=IDX.YES), HF(NAME2, VALUE2, indexing=IDX.YES)],
        [DYN_TABLE_SIZE - 1, HLEN + HLEN2 + HLEN3 - 1, HF(NAME3, VALUE3, indexing=IDX.YES)],
    ],
    'two_updates_decreasing_not_evict': [
        [HF(NAME, VALUE, indexing=IDX.YES), HF(NAME2, VALUE2, indexing=IDX.YES)],
        [DYN_TABLE_SIZE - 1, HLEN + HLEN2 + HLEN3, HF(NAME3, VALUE3, indexing=IDX.YES)],
    ],
    'multiple_updates_eviction': [
        [HF(NAME, VALUE, indexing=IDX.YES), HF(NAME2, VALUE2, indexing=IDX.YES)],
        [HLEN + HLEN2, HLEN + HLEN2 + 1, HLEN + HLEN2 - 1, DYN_TABLE_SIZE - 1, HF(NAME3, VALUE3, indexing=IDX.YES)],
    ],
    'multiple_updates_not_evict': [
        [HF(NAME, VALUE, indexing=IDX.YES), HF(NAME2, VALUE2, indexing=IDX.YES)],
        [HLEN + HLEN2 + 2, HLEN + HLEN2 + 1, HLEN + HLEN2, DYN_TABLE_SIZE - 1, HF(NAME3, VALUE3, indexing=IDX.YES)],
    ],
    'update_in_the_middle': [[
        HF(NAME, VALUE), DYN_TABLE_SIZE + 1, HF(NAME2, VALUE2),
    ]],
}.iteritems()}


TESTS.update(SIZE_UPDATE_TESTS)


DECODER_ONLY_TESTS = {
    'pseudo_headers': [[HF(':method', 'GET'), HF(':scheme', 'https'), HF(':authority', 'localhost'),
                        HF(':path', '/'), HF(':status', '200')]],
    'huffman': [[HF(HName('name', compressed=True), HValue('value', compressed=True))]],
    'all_ascii_huffman_name': [[HF(HName(chr(i), compressed=True), 'value{}'.format(i)) for i in OK_NAME_SYMBOLS]],
    'all_ascii_huffman_value': [[HF('header{}'.format(i), HValue(chr(i), compressed=True)) for i in OK_STRICT_SYMBOLS]],
    'all_ascii_suffix_huffman_value': [[HF('header{}'.format(i), HValue('a' + chr(i), compressed=True)) for i in OK_SYMBOLS]],
    'never_indexed_huffman': [[HF(HName('name', compressed=True), HValue('value', compressed=True), indexing=IDX.NEVER)]],
}


def encode_headers(headers):
    result = list()
    for field in headers:
        if isinstance(field, HF):
            name = field.name
            value = field.value
            if field.index is None:
                indexing = field.indexing
            else:
                indexing = IDX.YES
        else:
            name, value = field
            indexing = None
        name = encode_str(name)
        value = encode_str(value)
        if indexing is not None:
            result.append({'name': name, 'value': value, 'indexing': indexing})
        else:
            result.append({'name': name, 'value': value})
    return result


def dump(name, test_input, test_output):
    with open('decoder/{}.in'.format(name), 'w') as f:
        json.dump(test_input, f)
    with open('decoder/{}.out'.format(name), 'w') as f:
        json.dump(test_output, f)


def gen_decoder_test(name, blocks):
    encoder = hpack.Encoder()
    decoder = hpack.Decoder()
    test_input = list()
    test_output = list()
    for block in blocks:
        encoded_list = list()
        for elem in block:
            if isinstance(elem, int):
                encoded_list.append(encoder.encode_max_size_update(elem))
            else:
                encoded_list.append(encoder.encode_one(elem))
        encoded = ''.join(encoded_list)
        test_input.append(encode_str(encoded))
        try:
            decoded = decoder.decode(encoded)
            test_output.append({
                'headers': encode_headers(decoded),
                'table': encode_headers(decoder.table.dynamic_items()),
            })
        except hpack.DecoderError:
            test_output.append('COMPRESSION_ERROR')
            break
    dump(name, test_input, test_output)


def build_encoder_input(orig, decoded):
    result = list()
    decoded_id = 0
    for elem in orig:
        if isinstance(elem, int):
            value = {
                'type': 'size_update',
                'value': elem,
            }
        else:
            decoded_item = decoded[decoded_id]
            decoded_id += 1
            if elem.index is not None and elem.index <= STATIC_TABLE_SIZE:
                value = {
                    'type': 'indexed_field',
                    'field_id': elem.index,
                }
            elif elem.name_element.index is not None and elem.name_element.index <= STATIC_TABLE_SIZE:
                value = {
                    'type': 'indexed_name',
                    'name_id': elem.name_element.index,
                    'value': encode_str(elem.value),
                    'indexing': elem.indexing,
                }
            else:
                if elem.index is None:
                    indexing = elem.indexing
                else:
                    indexing = IDX.YES
                value = {
                    'type': 'literal',
                    'name': encode_str(decoded_item.name),
                    'value': encode_str(decoded_item.value),
                    'indexing': indexing,
                }
        result.append(value)
    return result


def gen_encoder_test(name, blocks):
    encoder = hpack.Encoder(greedy_huffman=True)
    decoder = hpack.Decoder()
    test_input = list()
    test_output = list()
    for block in blocks:
        sizes = list()
        for elem in block:
            if isinstance(elem, int):
                sizes.append(elem)
            else:
                break
        if len(sizes) > 0:
            min_size = min(sizes)
            last_size = sizes[-1]
            encoder.update_size(min_size)
            if min_size != last_size:
                encoder.update_size(last_size)
        try:
            encoded = encoder.encode(block[len(sizes):])
            test_output.append(encode_str(encoded))
            decoded = decoder.decode(encoded)
            test_input.append(build_encoder_input(block, decoded))
        except:
            return
    with open('encoder/{}.in'.format(name), 'w') as f:
        json.dump(test_input, f)
    with open('encoder/{}.out'.format(name), 'w') as f:
        json.dump(test_output, f)


BAD_SYMBOLS = [
    ('tab', '\t'),
    ('tab_prefix', '\tb'),
    ('space', ' '),
    ('space_prefix', ' b'),
    ('zero', '\x00'),
    ('zero_prefix', '\x00b'),
    ('zero_suffix', 'a\x00'),
    ('zero_infix', 'a\x00b'),
    ('carriage_return', '\r'),
    ('carriage_return_prefix', '\rb'),
    ('carriage_return_suffix', 'a\r'),
    ('carriage_return_infix', 'a\rb'),
    ('line_feed', '\n'),
    ('line_feed_prefix', '\nb'),
    ('line_feed_suffix', 'a\n'),
    ('line_feed_infix', 'a\nb'),
]
PROTOCOL_ERROR_TESTS = dict(
    [(test_name + '_value', HF('name', value)) for (test_name, value) in BAD_SYMBOLS] +
    [(test_name + '_name', HF(name, 'value')) for (test_name, name) in BAD_SYMBOLS + [
        ('empty', ''),
        ('colon', ':'),
        ('colon_prefix', ':b'),
        ('colon_suffix', 'a:'),
        ('colon_infix', 'a:b'),
        ('tab_suffix', 'a\t'),
        ('tab_infix', 'a\tb'),
        ('space_suffix', 'a '),
        ('space_infix', 'a b'),
    ] + [('upper_{}'.format(sym), sym) for sym in string.ascii_uppercase]]
)


def gen_protocol_error_test(name, header):
    encoder = hpack.Encoder()
    test_input = [
        encode_str(encoder.encode_one(header)),
    ]
    test_output = [
        'PROTOCOL_ERROR',
    ]
    dump(name, test_input, test_output)


def build_wrong_length(length):
    return hpack.encode_int(4, 0, 0) + \
        hpack.encode_int(7, 0, len(NAME)) + NAME + \
        hpack.encode_int(7, 0, length) + VALUE


def build_extra_bytes(extra_bytes):
    name = 'header'
    value = 'A' * 1024
    return hpack.encode_int(4, 0, 0) + \
        hpack.encode_int(7, 0, len(name)) + name + \
        hpack.encode_int(7, 0, len(value), extra_bytes=extra_bytes) + value


def build_broken_huffman():
    name = 'header'
    value = '\x00'  # 00000 (sym 48) + 000 (invalid suffix)
    return hpack.encode_int(4, 0, 0) + \
        hpack.encode_int(7, 0, len(name)) + name + \
        hpack.encode_int(7, 1, len(value)) + value


def build_size_update_exceed_max_size():
    encoder = hpack.Encoder()
    encoder.update_size(DEFAULT_SIZE + 1)
    return encoder.encode([HF('name', 'value')])


RAW_TESTS = {
    'extra_bytes_ok': build_extra_bytes(1),
}


def gen_raw_test(name, raw_data):
    decoder = hpack.Decoder()
    decoded = decoder.decode(raw_data)
    test_output = [{
        'headers': encode_headers(decoded),
        'table': encode_headers(decoder.table.dynamic_items()),
    }]
    dump(name, [encode_str(raw_data)], test_output)


RAW_ERR_TESTS = {
    'wrong_length_lt': build_wrong_length(len(VALUE) - 1),
    'wrong_length_gt': build_wrong_length(len(VALUE) + 1),
    'extra_bytes_fail': build_extra_bytes(2),
    'huffman_error': build_broken_huffman(),
    'size_update_exceed_max_size': build_size_update_exceed_max_size(),
}


def gen_raw_err_test(name, raw_data):
    dump(name, [encode_str(raw_data)], ['COMPRESSION_ERROR'])


def gen_all(data, func):
    for name, value in data.iteritems():
        func(name, value)


def main():
    gen_all(TESTS, gen_decoder_test)
    gen_all(DECODER_ONLY_TESTS, gen_decoder_test)
    gen_all(PROTOCOL_ERROR_TESTS, gen_protocol_error_test)
    gen_all(RAW_TESTS, gen_raw_test)
    gen_all(RAW_ERR_TESTS, gen_raw_err_test)

    gen_all(TESTS, gen_encoder_test)


if __name__ == '__main__':
    main()
