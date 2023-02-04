# -*- coding: utf-8 -*-
import json
import os
import pytest
import yatest.common


def encode_str(data):
    return data.encode('hex')


def decode_str(data):
    return data.decode('hex')


DATA_DIR = os.path.abspath(yatest.common.source_path('balancer/test/hpack/data'))
DECODER_DATA_DIR = DATA_DIR + '/decoder/'
ENCODER_DATA_DIR = DATA_DIR + '/encoder/'


def get_binary(name):
    return yatest.common.binary_path('balancer/kernel/http2/server/hpack/tests/{name}/{name}'.format(name=name))


def build_params(data_dir):
    return [name[:-3] for name in os.listdir(data_dir) if name.endswith('.in')]


def read_json(path):
    with open(path) as f:
        return json.load(f)


def __encode_headers(headers):
    from balancer.test.util.proto.http2.message import HeaderField as HF
    result = list()
    for field in headers:
        if isinstance(field, HF):
            name = field.name
            value = field.value
            indexing = field.indexing
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


def __util_decoder(test_input, test_output):
    import balancer.test.util.proto.http2.hpack._hpack as hpack
    data = read_json(test_input)
    decoder = hpack.Decoder()
    result = list()
    for block in data:
        try:
            headers = decoder.decode(decode_str(block))
            result.append({
                'headers': __encode_headers(headers),
                'table': __encode_headers(decoder.table.dynamic_items()),
            })
        except hpack.DecoderError:
            result = ['COMPRESSION_ERROR']
            break
    with open(test_output, 'w') as f:
        json.dump(result, f)


def balancer_decoder(test_input, test_output):
    err_path = yatest.common.output_path('decoder.err')
    with open(test_input) as f_in:
        with open(test_output, 'w') as f_out:
            with open(err_path, 'w') as f_err:
                yatest.common.execute([get_binary('hpack_decoder_test')],
                                      check_exit_code=False, stdin=f_in, stdout=f_out, stderr=f_err)


def run_decoder(test_input, test_output):
    # __util_decoder(test_input, test_output)
    balancer_decoder(test_input, test_output)


def decode_headers(headers):
    if headers is None:
        return []
    result = list()
    for field in headers:
        name = decode_str(field['name'])
        value = decode_str(field['value'])
        if 'indexing' in field:
            result.append({'name': name, 'value': value, 'indexing': field['indexing']})
        else:
            result.append({'name': name, 'value': value})
    return result


def decode_result(data):  # for readability
    result = list()
    for item in data:
        if item in ('COMPRESSION_ERROR', 'PROTOCOL_ERROR'):
            result.append(item)
            break
        else:
            result.append({
                'headers': decode_headers(item['headers']),
                'table': decode_headers(item['table']),
            })
    return result


@pytest.mark.parametrize('name', build_params(DECODER_DATA_DIR))
def test_decoder(name):
    test_input = DECODER_DATA_DIR + name + '.in'
    test_output = yatest.common.output_path(name + '.out.test')
    run_decoder(test_input, test_output)
    result = decode_result(read_json(test_output))
    expected = decode_result(read_json(DECODER_DATA_DIR + name + '.out'))
    assert result == expected


def __util_encoder(test_input, test_output):
    import balancer.test.util.proto.http2.hpack._hpack as hpack
    from balancer.test.util.proto.http2.message import HeaderField as HF
    from balancer.test.util.proto.http2.message import HName, HValue

    def get_value(h_descr):
        return HValue(decode_str(h['value']))

    encoder = hpack.Encoder(greedy_huffman=True)
    result = list()
    for data in read_json(test_input):
        block = list()
        sizes = list()
        for h in data:
            h_type = h['type']
            if h_type == 'size_update':
                sizes.append(h['value'])
            elif h_type == 'indexed_field':
                block.append(HF(h['field_id']))
            elif h_type == 'indexed_name':
                block.append(HF(h['name_id'], get_value(h), indexing=h['indexing']))
            else:
                block.append(HF(HName(decode_str(h['name'])),
                                      get_value(h), indexing=h['indexing']))
        if len(sizes) > 0:
            min_size = min(sizes)
            last_size = sizes[-1]
            encoder.update_size(min_size)
            if min_size != last_size:
                encoder.update_size(last_size)
        result.append(encode_str(encoder.encode(block)))
    with open(test_output, 'w') as f:
        json.dump(result, f)


def balancer_encoder(test_input, test_output):
    with open(test_input) as f_in:
        with open(test_output, 'w') as f_out:
            yatest.common.execute([get_binary('hpack_encoder_test')], stdin=f_in, stdout=f_out)


def run_encoder(test_input, test_output):
    # __util_encoder(test_input, test_output)
    balancer_encoder(test_input, test_output)


@pytest.mark.parametrize('name', build_params(ENCODER_DATA_DIR))
def test_encoder(name):
    test_input = ENCODER_DATA_DIR + name + '.in'
    test_output = yatest.common.output_path(name + '.out.test')
    run_encoder(test_input, test_output)
    result = read_json(test_output)
    expected = read_json(ENCODER_DATA_DIR + name + '.out')
    assert result == expected
