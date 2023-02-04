# -*- coding: utf-8 -*-

import pytest
import common
from balancer.test.util.balancer import asserts
from balancer.test.util.predef.handler.server.http import SimpleConfig
from balancer.test.util.proto.http2 import errors
from balancer.test.util.proto.http2 import message as mod_msg
from balancer.test.util.proto.http2.framing import frames
from balancer.test.util.proto.http2.framing import flags
import balancer.test.util.proto.http2.hpack._hpack as hpack
from balancer.test.util.predef import http
from balancer.test.util.predef import http2
from balancer.test.util.proto.http2.message import HeaderField, Indexing, HName

BALANCER_TABLE_SIZE = 2 ** 14


def perform_hpack_request(ctx, conn, headers):
    resp = conn.perform_request(http2.request.raw_custom(headers))
    asserts.status(resp, 200)
    return ctx.backend.state.get_request()


def simple_hpack_test(ctx, headers):
    conn = common.start_and_connect(ctx)
    return perform_hpack_request(ctx, conn, headers)


def perform_multi_hpack_requests(ctx, conn, *headers):
    reqs = list()
    for h in headers:
        reqs.append(perform_hpack_request(ctx, conn, h))
    return reqs


def multi_reqs_hpack_test(ctx, *headers):
    conn = common.start_and_connect(ctx)
    return perform_multi_hpack_requests(ctx, conn, *headers)


def test_hpack_indexed_field(ctx):
    """
    Client request with indexed header fields from static table
    """
    req = simple_hpack_test(ctx, [
        common.GET_METHOD,
        5,   # :path: /index.html
        (':authority', 'localhost'),
        common.HTTPS_SCHEME,
        16,  # accept-encoding: gzip, deflate
    ])

    asserts.method(req, 'GET')
    asserts.path(req, '/index.html')
    asserts.header_value(req, 'accept-encoding', 'gzip, deflate')


def test_hpack_indexed_name(ctx):
    """
    Client request with indexed header field names from static table
    """
    req = simple_hpack_test(ctx, [
        common.GET_METHOD,
        (common.ROOT_PATH, common.PATH),
        common.AUTH_LOCAL,
        common.HTTPS_SCHEME,
        (58, 'James Bond'),  # user-agent
    ])

    asserts.method(req, 'GET')
    asserts.path(req, common.PATH)
    asserts.header_value(req, 'user-agent', 'James Bond')


def test_hpack_dynamic_table_indexed_field(ctx):
    """
    First request: client adds new header fields to dynamic table
    Second request: client uses field indicies from dynamic table
    """
    name = 'header'
    value1 = 'value1'
    value2 = 'value2'
    req1, req2 = multi_reqs_hpack_test(
        ctx,
        [
            common.GET_METHOD,
            mod_msg.HeaderField(common.ROOT_PATH, common.PATH, mod_msg.Indexing.YES),
            common.AUTH_LOCAL,
            common.HTTPS_SCHEME,
            mod_msg.HeaderField(common.HPACK_NAME, common.HPACK_VALUE, mod_msg.Indexing.YES),
            mod_msg.HeaderField(name, value1, mod_msg.Indexing.YES),
        ],
        [
            common.GET_METHOD,
            common.STATIC_TABLE_SIZE + 3,  # :path: /Led/Zeppelin
            common.AUTH_LOCAL,
            common.HTTPS_SCHEME,
            common.STATIC_TABLE_SIZE + 2,  # led: Zeppelin
            (common.STATIC_TABLE_SIZE + 1, value2),  # header: value2
        ],
    )

    asserts.path(req1, common.PATH)
    asserts.header_value(req1, common.HPACK_NAME, common.HPACK_VALUE)
    asserts.path(req2, common.PATH)
    asserts.header_value(req2, common.HPACK_NAME, common.HPACK_VALUE)
    asserts.header_value(req2, name, value2)


def test_hpack_huffman(ctx):
    """
    Client request with huffman-encoded header fields
    """
    req = simple_hpack_test(ctx, [
        common.GET_METHOD,
        (mod_msg.HName(':path', compressed=True), mod_msg.HValue(common.PATH, compressed=True)),
        common.AUTH_LOCAL,
        common.HTTPS_SCHEME,
        (mod_msg.HName(common.HPACK_NAME, compressed=True), mod_msg.HValue(common.HPACK_VALUE, compressed=True)),
    ])

    asserts.method(req, 'GET')
    asserts.path(req, common.PATH)
    asserts.header_value(req, common.HPACK_NAME, common.HPACK_VALUE)


def test_continuation_huffman(ctx):
    """
    Client request with huffman-encoded header fields in multiple continuation frames
    """
    conn = common.start_and_connect(ctx)
    conn.set_max_length(1)
    req = perform_hpack_request(ctx, conn, [
        common.GET_METHOD,
        (mod_msg.HName(':path', compressed=True), mod_msg.HValue(common.PATH, compressed=True)),
        common.AUTH_LOCAL,
        common.HTTPS_SCHEME,
        (mod_msg.HName(common.HPACK_NAME, compressed=True), mod_msg.HValue(common.HPACK_VALUE, compressed=True)),
    ])

    asserts.method(req, 'GET')
    asserts.path(req, common.PATH)
    asserts.header_value(req, common.HPACK_NAME, common.HPACK_VALUE)


def simple_raw_response_test(ctx, name, value):
    conn = common.start_and_connect(ctx, http.response.ok(headers={name: value}))
    resp = conn.perform_request_raw_response(http2.request.get())

    asserts.header_value(resp, name, value)
    return resp.headers.get_one_field(name)


def test_huffman_response(ctx):
    """
    Balancer must send huffman-encoded string if
    the length of encoded string is lesser than the original one
    """
    header = simple_raw_response_test(ctx, 'some_long_header', 'very_long_value')
    assert header.name_element.compressed
    assert header.value_element.compressed


def test_not_huffman_response(ctx):
    """
    Balancer must not send huffman-encoded string if
    the length of encoded string is greater or equal to the original one
    """
    header = simple_raw_response_test(ctx, 'vwxyz', '!' * 10)
    assert not header.name_element.compressed
    assert not header.value_element.compressed


def test_indexed_header_name(ctx):
    """
    If backend response contains a header which name is in headers table
    then balancer must use indexed name representation
    """
    header = simple_raw_response_test(ctx, 'last-modified', 'Tue, 21 Feb 2017 00:00:00 GMT')
    assert header.name_element.index is not None


def test_indexed_header_field(ctx):
    """
    If backend response contains a header which is in headers table
    then balancer must use indexed header field representation
    """
    header = simple_raw_response_test(ctx, 'accept-encoding', 'gzip, deflate')
    assert header.index is not None


def test_hpack_never_indexed_not_in_dynamic_table(ctx):
    """
    Balancer must not add never indexed field into dynamic table
    """
    req1, req2 = multi_reqs_hpack_test(
        ctx,
        [
            common.GET_METHOD,
            mod_msg.HeaderField(common.ROOT_PATH, common.PATH, mod_msg.Indexing.NEVER),
            common.AUTH_LOCAL,
            common.HTTPS_SCHEME,
            mod_msg.HeaderField(common.SECURE_NAME, common.SECURE_VALUE, mod_msg.Indexing.NEVER),
            mod_msg.HeaderField(common.HPACK_NAME, common.HPACK_VALUE, mod_msg.Indexing.YES),
        ],
        common.SIMPLE_REQ + [
            common.STATIC_TABLE_SIZE + 1,  # led: Zeppelin
        ],
    )

    asserts.path(req1, common.PATH)
    asserts.header_value(req1, common.HPACK_NAME, common.HPACK_VALUE)
    asserts.path(req2, '/')
    asserts.header_value(req2, common.HPACK_NAME, common.HPACK_VALUE)


# TODO: http2 to backend is needed
# def test_hpack_never_indexed_forwarding(ctx):
#     """
#     Balancer must forward never indexed field to backend
#     using the same representation
#     """


def assert_compression_error(ctx, conn, reason):
    common.assert_conn_error(ctx, conn, errors.COMPRESSION_ERROR, reason)
    unistat = ctx.get_unistat()
    assert unistat["http2-h2_conn_open_summ"] == 1
    assert unistat["http2-h2_conn_inprog_ammv"] == 0


def assert_no_index(ctx, conn, index):
    stream = conn.create_stream()
    stream.write_message(http2.request.raw_custom(common.SIMPLE_REQ + [common.STATIC_TABLE_SIZE + index]))
    assert_compression_error(ctx, conn, 'InvalidIndex')


def test_hpack_entry_eviction_on_add(ctx):
    """
    Balancer must replace the first added entry in dynamic table with the new one
    if there is not enough space left in dynamic table
    """
    name = 'header'
    value = 'A' * (BALANCER_TABLE_SIZE / 4 - len(name) - 32)
    conn = common.start_and_connect(ctx, header_table_size=BALANCER_TABLE_SIZE)
    conn.write_frame(common.ACK_SETTINGS_FRAME)
    conn.update_encoder_table_size(BALANCER_TABLE_SIZE)
    last_req = perform_multi_hpack_requests(
        ctx, conn,
        common.SIMPLE_REQ + [
            mod_msg.HeaderField(name + '0', value, mod_msg.Indexing.YES),
            mod_msg.HeaderField(name + '1', value, mod_msg.Indexing.YES),
            mod_msg.HeaderField(name + '2', value, mod_msg.Indexing.YES),
        ],
        common.SIMPLE_REQ + [
            mod_msg.HeaderField(name + '3', value, mod_msg.Indexing.YES),
        ],
        common.SIMPLE_REQ + [
            common.STATIC_TABLE_SIZE + 1,  # header3
            common.STATIC_TABLE_SIZE + 2,  # header2
            common.STATIC_TABLE_SIZE + 3,  # header1
        ],
    )[2]

    asserts.header_value(last_req, name + '1', value)
    asserts.header_value(last_req, name + '2', value)
    asserts.header_value(last_req, name + '3', value)
    assert_no_index(ctx, conn, 4)


def test_hpack_entry_eviction_on_add_single_request(ctx):
    """
    Balancer must replace the first added entry in dynamic table with the new one
    if there is not enough space left in dynamic table
    All indexing fields are in single request
    """
    name = 'header'
    value = 'A' * (BALANCER_TABLE_SIZE / 4 - len(name) - 32)
    conn = common.start_and_connect(ctx, header_table_size=BALANCER_TABLE_SIZE)
    conn.write_frame(common.ACK_SETTINGS_FRAME)
    conn.update_encoder_table_size(BALANCER_TABLE_SIZE)
    last_req = perform_multi_hpack_requests(
        ctx, conn,
        common.SIMPLE_REQ + [
            mod_msg.HeaderField(name + '0', value, mod_msg.Indexing.YES),
            mod_msg.HeaderField(name + '1', value, mod_msg.Indexing.YES),
            mod_msg.HeaderField(name + '2', value, mod_msg.Indexing.YES),
            mod_msg.HeaderField(name + '3', value, mod_msg.Indexing.YES),
        ],
        common.SIMPLE_REQ + [
            common.STATIC_TABLE_SIZE + 1,  # header3
            common.STATIC_TABLE_SIZE + 2,  # header2
            common.STATIC_TABLE_SIZE + 3,  # header1
        ],
    )[1]

    asserts.header_value(last_req, name + '1', value)
    asserts.header_value(last_req, name + '2', value)
    asserts.header_value(last_req, name + '3', value)
    assert_no_index(ctx, conn, 4)


def test_default_header_table_size(ctx):
    """
    Default header table size must be 4096
    """
    name = 'header'
    value = 'A' * (common.DEFAULT_HEADER_TABLE_SIZE - len(name) - 32)
    conn = common.start_and_connect(ctx, header_table_size=common.DEFAULT_HEADER_TABLE_SIZE)
    req = perform_multi_hpack_requests(
        ctx, conn,
        common.SIMPLE_REQ + [
            mod_msg.HeaderField(name, value, mod_msg.Indexing.YES),
        ],
        common.SIMPLE_REQ + [common.STATIC_TABLE_SIZE + 1],
        common.SIMPLE_REQ + [
            mod_msg.HeaderField(name, value + 'A', mod_msg.Indexing.YES),
        ],
    )[1]

    asserts.header_value(req, name, value)
    assert_no_index(ctx, conn, 1)


@pytest.mark.parametrize(
    'size_updates',
    [
        [1024],
        [1024, 2048],
        [2048, 1024],
    ],
    ids=[
        'single',
        'two_increasing',
        'two_decreasing',
    ]
)
def test_hpack_table_size_update_header_fits(ctx, size_updates):
    """
    Client sends table update at the beginning of the header block
    and header field that fits new table size
    Balancer must put header field to dynamic table
    """
    last_size = size_updates[-1]
    name = 'header'
    value = 'A' * (last_size - len(name) - 32)
    conn = common.start_and_connect(ctx)
    conn.update_encoder_table_size(size_updates)
    req = perform_multi_hpack_requests(
        ctx, conn,
        common.SIMPLE_REQ + [
            mod_msg.HeaderField(name, value, mod_msg.Indexing.YES),
        ],
        common.SIMPLE_REQ + [common.STATIC_TABLE_SIZE + 1],
    )[1]

    asserts.header_value(req, name, value)


def test_hpack_table_size_update_increase_size(ctx):
    """
    Client decreases dynamic table size in first header block
    and increases in second header block
    Balancer must use table size from the second header block
    """
    table_size = 1024
    name1 = 'header1'
    value1 = 'A' * 500
    name2 = 'header2'
    value2 = 'A' * (table_size - len(name1) - len(name2) - len(value1) - 2 * 32)
    name3 = 'header3'
    value3 = 'B' * (table_size - len(name3) - 32 + 1)
    conn = common.start_and_connect(ctx)
    conn.update_encoder_table_size(800)
    perform_hpack_request(
        ctx, conn,
        common.SIMPLE_REQ + [
            mod_msg.HeaderField(name1, value1, mod_msg.Indexing.YES),
        ],
    )
    conn.update_encoder_table_size(table_size)
    req = perform_multi_hpack_requests(
        ctx, conn,
        common.SIMPLE_REQ + [
            mod_msg.HeaderField(name2, value2, mod_msg.Indexing.YES),
        ],
        common.SIMPLE_REQ + [common.STATIC_TABLE_SIZE + 1, common.STATIC_TABLE_SIZE + 2],
        common.SIMPLE_REQ + [
            mod_msg.HeaderField(name3, value3, mod_msg.Indexing.YES),
        ],
    )[1]

    asserts.header_value(req, name1, value1)
    asserts.header_value(req, name2, value2)
    assert_no_index(ctx, conn, 1)


@pytest.mark.parametrize(
    'size_updates',
    [
        [1024],
        [1024, 2048],
        [2048, 1024],
    ],
    ids=[
        'single',
        'two_increasing',
        'two_decreasing',
    ]
)
@pytest.mark.parametrize('tail', [1, 4096], ids=['short_tail', 'long_tail'])
def test_hpack_table_size_update_header_does_not_fit(ctx, tail, size_updates):
    """
    Client sends table update at the beginning of the header block
    and header field that does not fit new table size
    Balancer must not put header field to dynamic table
    """
    last_size = size_updates[-1]
    name = 'header'
    value = 'A' * (last_size - len(name) - 32 + tail)
    conn = common.start_and_connect(ctx)
    conn.update_encoder_table_size(size_updates)
    perform_hpack_request(
        ctx, conn,
        common.SIMPLE_REQ + [
            mod_msg.HeaderField(name, value, mod_msg.Indexing.YES),
        ],
    )

    assert_no_index(ctx, conn, 1)


@pytest.mark.parametrize(
    'size_updates',
    [
        [1024],
        [1024, 2048],
        [2048, 1024],
    ],
    ids=[
        'single',
        'two_increasing',
        'two_decreasing',
    ]
)
def test_hpack_entry_eviction_on_table_size_update(ctx, size_updates):
    """
    Client sends table update at the beginning of the header block
    Balancer must evict entries from the end of dynamic table until
    the size of the dynamic table is less than or equal to the maximum size
    """
    min_size = min(size_updates)
    name1 = 'header1'
    value1 = 'A' * 500
    name2 = 'header2'
    value2 = 'A' * (min_size - len(name1) - len(name2) - len(value1) - 2 * 32 + 1)
    conn = common.start_and_connect(ctx)
    perform_hpack_request(
        ctx, conn,
        common.SIMPLE_REQ + [
            mod_msg.HeaderField(name1, value1, mod_msg.Indexing.YES),
            mod_msg.HeaderField(name2, value2, mod_msg.Indexing.YES),
        ],
    )
    conn.update_encoder_table_size(size_updates)

    assert_no_index(ctx, conn, 2)


@pytest.mark.parametrize(
    'size_updates',
    [
        [1024],
        [1024, 2048],
        [2048, 1024],
    ],
    ids=[
        'single',
        'two_increasing',
        'two_decreasing',
    ]
)
def test_hpack_table_update_not_evict_fitting_entries(ctx, size_updates):
    """
    Client sends table update at the beginning of the header block
    Balancer must stop evicting entries from dynamic table when
    the size of dynamic table becomes less or equal to the maximum size
    """
    min_size = 1024
    name1 = 'header1'
    value1 = 'A' * 500
    name2 = 'header2'
    value2 = 'A' * (min_size - len(name2) - 2 * 32)
    conn = common.start_and_connect(ctx)
    perform_hpack_request(
        ctx, conn,
        common.SIMPLE_REQ + [
            mod_msg.HeaderField(name1, value1, mod_msg.Indexing.YES),
            mod_msg.HeaderField(name2, value2, mod_msg.Indexing.YES),
        ],
    )
    conn.update_encoder_table_size(size_updates)
    req = perform_hpack_request(
        ctx, conn,
        common.SIMPLE_REQ + [common.STATIC_TABLE_SIZE + 1],
    )

    asserts.header_value(req, name2, value2)


def test_hpack_table_update_in_the_middle_of_header_block(ctx):
    """
    Client sends table update in the middle of header block
    Balancer must send GOAWAY(COMPRESSION_ERROR) to client and close connection
    """
    conn = common.start_and_connect(ctx)
    prefix = conn.encoder.encode(mod_msg.RawHTTP2Message.build_headers(common.SIMPLE_REQ))
    update = conn.encoder.encode_max_size_update(1024)
    suffix = conn.encoder.encode_one(mod_msg.HeaderField(common.HPACK_NAME, common.HPACK_VALUE))
    stream = conn.create_stream()
    stream.write_encoded_headers(prefix + update + suffix, end_stream=True)

    assert_compression_error(ctx, conn, 'InvalidHeaderFieldType')


@pytest.mark.parametrize('padding', [None, '', 'padding'], ids=['no', 'empty', 'ok'])
@pytest.mark.parametrize('length', [len(common.HPACK_VALUE) - 1, len(common.HPACK_VALUE) + 1], ids=['lt', 'gt'])
def test_hpack_wrong_header_length(ctx, length, padding):
    """
    Client sends request with wrong last header value length
    Balancer must send GOAWAY(COMPRESSION_ERROR) to client and close connection
    """
    wrong_encoded = hpack.encode_int(4, 0, 0) + \
        hpack.encode_int(7, 0, len(common.HPACK_NAME)) + common.HPACK_NAME + \
        hpack.encode_int(7, 0, length) + common.HPACK_VALUE

    conn = common.start_and_connect(ctx)
    prefix = conn.encoder.encode(mod_msg.RawHTTP2Message.build_headers(common.SIMPLE_REQ))
    h_flags = flags.END_HEADERS | flags.END_STREAM
    if padding is not None:
        h_flags |= flags.PADDED
    headers = frames.Headers(
        length=None, flags=h_flags, reserved=0, stream_id=None,
        data=prefix + wrong_encoded, padding=padding,
    )
    stream = conn.create_stream()
    stream.write_frame(headers)

    assert_compression_error(ctx, conn, 'HeaderBlockEnd')


def build_encoded_header_extra_bytes(name, value, extra_bytes):
    return hpack.encode_int(4, 0, 0) + \
        hpack.encode_int(7, 0, len(name)) + name + \
        hpack.encode_int(7, 0, len(value), extra_bytes=extra_bytes) + value


def test_hpack_int_encoding_unnecessary_octets_ok(ctx):
    """
    Client encoded header value length using 4 bytes (with 1 unnecessary extra byte)
    Balancer must decode it without errors
    """
    name = 'header'
    value = 'A' * 1024
    encoded = build_encoded_header_extra_bytes(name, value, 1)
    conn = common.start_and_connect(ctx)
    prefix = conn.encoder.encode(mod_msg.RawHTTP2Message.build_headers(common.SIMPLE_REQ))
    stream = conn.create_stream()
    stream.write_encoded_headers(prefix + encoded, end_stream=True)
    resp = stream.read_message().to_response()
    req = ctx.backend.state.get_request()

    asserts.status(resp, 200)
    asserts.header_value(req, name, value)


def test_hpack_int_encoding_unnecessary_octets_fail(ctx):
    """
    Client encoded header value length using 5 bytes (with 2 unnecessary extra byte)
    Balancer must send GOAWAY(COMPRESSION_ERROR) to client and close connection
    """
    name = 'header'
    value = 'A' * 1024
    encoded = build_encoded_header_extra_bytes(name, value, 2)
    conn = common.start_and_connect(ctx)
    prefix = conn.encoder.encode(mod_msg.RawHTTP2Message.build_headers(common.SIMPLE_REQ))
    stream = conn.create_stream()
    stream.write_encoded_headers(prefix + encoded, end_stream=True)

    assert_compression_error(ctx, conn, 'InvalidInteger')


def test_huffman_error(ctx):
    """
    If client sends request with wrong huffman-encoded header then
    balancer must send GOAWAY(COMPRESSION_ERROR) to client and close connection
    """
    name = 'header'
    value = '\x00'  # 00000 (sym 48) + 000 (invalid suffix)
    wrong_encoded = hpack.encode_int(4, 0, 0) + \
        hpack.encode_int(7, 0, len(name)) + name + \
        hpack.encode_int(7, 1, len(value)) + value
    conn = common.start_and_connect(ctx)
    prefix = conn.encoder.encode(mod_msg.RawHTTP2Message.build_headers(common.SIMPLE_REQ))
    stream = conn.create_stream()
    stream.write_encoded_headers(prefix + wrong_encoded, end_stream=True)

    assert_compression_error(ctx, conn, 'InvalidHuffman')


def test_many_response_headers(ctx):
    """
    BALANCER-2075
    Encoding the headers not fitting one frame should not crash
    """
    headers = []
    for i in range(2**11 - 1):
        headers.append(("h{}".format(i), 'x'))

    conn = common.start_and_connect(ctx, backend=SimpleConfig(http.response.ok(headers=headers)))
    resp = conn.perform_request(http2.request.get())

    asserts.status(resp, 200)
    asserts.headers_values(resp, headers)


@pytest.mark.parametrize(
    'ack',
    [True, False],
    ids=["ack", "noack"]
)
@pytest.mark.parametrize(
    'size',
    [common.DEFAULT_HEADER_TABLE_SIZE - 1, common.DEFAULT_HEADER_TABLE_SIZE + 1],
    ids=["smaller", "bigger"]
)
def test_custom_server_header_table_size(ctx, ack, size):
    """
    BALANCER-1973
    BALANCER-2174
    Balancer should respect its header_table_size promises
    """
    to_send = max(common.DEFAULT_HEADER_TABLE_SIZE, size)

    conn = common.start_and_connect(
        ctx,
        header_table_size=size
    )

    if ack:
        conn.write_frame(common.ACK_SETTINGS_FRAME)

    need = to_send - (1 + 32)

    header_name = 'a' * need
    header_values = ['A', 'B']

    encoded = conn.encoder.encode_max_size_update(to_send) + conn.encoder.encode(
        mod_msg.RawHTTP2Message.build_headers(
            common.SIMPLE_REQ + [
                HeaderField(
                    header_name,
                    header_values[0],
                    indexing=Indexing.YES
                ),
                HeaderField(
                    HName(index=common.STATIC_TABLE_SIZE + 1),
                    header_values[1]
                )
            ]
        )
    )
    stream = conn.create_stream()
    stream.write_encoded_headers(encoded, end_stream=True)

    if ack:
        if size > common.DEFAULT_HEADER_TABLE_SIZE:
            resp = stream.read_message().to_response()
            asserts.status(resp, 200)
            req = ctx.backend.state.get_request()
            asserts.header_values(req, header_name, header_values)
        else:
            assert_compression_error(ctx, conn, 'InvalidSizeUpdate')
    else:
        # BALANCER-2174 We should always treat unacked settings in the client's favour
        resp = stream.read_message().to_response()
        asserts.status(resp, 200)
        req = ctx.backend.state.get_request()
        asserts.header_values(req, header_name, header_values)
