# -*- coding: utf-8 -*-
import socket

import pytest
import time
import common
from balancer.test.util.process import BalancerStartError
from balancer.test.util.proto.handler.server.http import HTTPConfig
from configs import HTTP2Config, HTTP2BadConfig
from balancer.test.util.balancer import asserts
from balancer.test.util.proto.http2 import errors, message
import balancer.test.util.proto.http2.hpack._hpack as hpack
from balancer.test.util.proto.http2.framing import frames
from balancer.test.util.proto.http2.framing import flags
from balancer.test.util.proto.http2.framing.stream import NoHTTP2FrameException, serialize
from balancer.test.util.predef.handler.server.http import SimpleDelayedConfig, DummyConfig,\
    CloseConfig, ChunkedConfig, ThreeModeConfig, NoReadConfig, HTTPServerHandler, ContinueConfig, SimpleConfig
from balancer.test.util.predef import http
from balancer.test.util.predef import http2


def test_bad_config(ctx):
    """
    BALANCER-1816
    http2 inside http2 is not allowed. As well as nesting http2 inside anything else besides
    errorlog, ipdispatch, regexp, report, ssl_sni.
    """
    with pytest.raises(BalancerStartError):
        # The balancer should not start if there are two http2 modules nested in one another
        ctx.start_balancer(HTTP2BadConfig(certs_dir=ctx.certs.root_dir, mod='http2'))
    # The balancer should start if the http2 module is nested inside any or all the allowed modules
    ctx.start_balancer(HTTP2BadConfig(certs_dir=ctx.certs.root_dir, mod='errorlog'))


def test_server_preface(ctx):
    """
    Balancer must send SETTINGS frame as connection preface
    """
    common.start_all(ctx)
    conn = common.create_conn(ctx, setup=False)
    frame = conn.read_frame()

    assert isinstance(frame, frames.Settings)


def test_no_client_preface(ctx):
    """
    If client does not send preface before anything else balancer must send GOAWAY(PROTOCOL_ERROR) and close connection.
    TODO(velavokr): The implementation reads the whole preface string before validating it.
    """
    common.start_all(ctx)
    conn = common.create_conn(ctx, setup=False)
    conn.write_frame(common.build_settings([]))
    stream = conn.create_stream()
    stream.write_headers(http2.request.raw_custom(common.SIMPLE_REQ).headers, end_stream=True)
    common.assert_conn_protocol_error(ctx, conn, "InvalidPreface", 0)


def base_simple_test(ctx, req, resp, ssl=True):
    balancer_kwargs = {}

    if not ssl:
        balancer_kwargs['force_ssl'] = False
        balancer_kwargs['allow_http2_without_ssl'] = True

    conn = common.start_and_connect(ctx, resp, ssl=ssl, **balancer_kwargs)
    backend_resp = conn.perform_request(req)

    assert len(backend_resp.frames) > 0
    last = backend_resp.frames[-1]

    if 'content-length' in backend_resp.headers:
        if isinstance(last, frames.Data):
            assert len(last.data) > 0

    backend_req = ctx.backend.state.get_request()
    return backend_req, backend_resp


def base_simple_test_request(ctx, req):
    backend_req, backend_resp = base_simple_test(ctx, req, http.response.ok())
    asserts.status(backend_resp, 200)
    return backend_req


def base_simple_test_response(ctx, resp, ssl=True):
    backend_req, backend_resp = base_simple_test(ctx, http2.request.get(), resp, ssl=ssl)
    asserts.path(backend_req, '/')
    return backend_resp


@pytest.mark.parametrize('use_http2', [True, False], ids=['', 'http1x'])
def test_request_without_tls(ctx, use_http2):
    balancer_kwargs = {'allow_http2_without_ssl': True, 'force_ssl': False}
    if use_http2 is True:
        conn = common.start_and_connect(ctx, http.response.ok(), ssl=False, **balancer_kwargs)
        response = conn.perform_request(http2.request.get())
    else:
        common.start_all(ctx, http.response.ok(), **balancer_kwargs)
        response = ctx.perform_request(http.request.get())
    asserts.status(response, 200)


def test_request_path(ctx):
    """
    Balancer must send client request to backend and return backend response to client
    """
    req = base_simple_test_request(
        ctx,
        http2.request.get(common.PATH),
    )
    asserts.path(req, common.PATH)


def test_request_headers(ctx):
    """
    Balancer must send client request with custom headers to backend
    """
    req = base_simple_test_request(
        ctx,
        http2.request.get(headers=common.HEADERS),
    )
    asserts.headers_values(req, common.HEADERS)


@pytest.mark.parametrize('value', ["compress", "deflate", "gzip", "trailers"])
def test_request_headers_te(ctx, value):
    """
    BALANCER-1471
    Balancer must allow te: trailers
    Since we have not implemented trailers yet we do not expect 'te' in backend requests
    """
    if "trailers" == value:
        req = base_simple_test_request(
            ctx,
            http2.request.get(headers=dict(te=value, **common.HEADERS))
        )
        asserts.headers_values(req, common.HEADERS)
        asserts.no_header(req, 'te')
    else:
        base_stream_error_test(
            ctx,
            http2.request.get(headers=dict(te=value, **common.HEADERS))
        )


def test_single_data_frame_length_request(ctx):
    """
    Balancer must send client request with single DATA frame and content-length header to backend
    """
    req = base_simple_test_request(
        ctx,
        http2.request.post(data=common.SINGLE_DATA, headers={'content-length': len(common.SINGLE_DATA)}),
    )
    asserts.content(req, common.SINGLE_DATA)


def test_multiple_data_frames_length_request(ctx):
    """
    Balancer must send client request with multiple DATA frames and content-length header to backend
    """
    req = base_simple_test_request(
        ctx,
        http2.request.post(data=common.DATA, headers={'content-length': len(common.CONTENT)}),
    )
    asserts.content(req, common.CONTENT)


def test_single_data_frame_no_length_request(ctx):
    """
    Balancer must send client request with single DATA frame and without content-length header to backend
    """
    req = base_simple_test_request(
        ctx,
        http2.request.post(data=common.SINGLE_DATA),
    )
    asserts.content(req, common.SINGLE_DATA)


def test_multiple_data_frames_no_length_request(ctx):
    """
    Balancer must send client request with multiple DATA frames and without content-length header to backend
    """
    req = base_simple_test_request(
        ctx,
        http2.request.post(data=common.DATA),
    )
    asserts.content(req, common.CONTENT)


def test_duplicate_headers_request(ctx):
    """
    Balancer must proxy request to backend even if request contains duplicate headers
    """
    headers = [('learn', 'ing to fly'), ('learn', 'you a haskell')]
    req = base_simple_test_request(
        ctx,
        http2.request.raw_get(headers=headers),
    )
    asserts.headers_values(req, headers)


@pytest.mark.parametrize(
    'content_length', [
        None,
        # TODO(velavokr): BALANCER-2156 balancer mishandles content-length in 304 responses
        # 0, 1,
    ]
)
def test_response_status_304(ctx, content_length):
    """
    Balancer must send response with the same status as backend response to client
    """
    resp = base_simple_test_response(
        ctx,
        http.response.not_modified(headers=(
            {'content-length': content_length} if content_length is not None else {}
        )),
    )
    asserts.status(resp, 304)
    if content_length is None:
        asserts.no_header(resp, 'content-length')
    if content_length is not None:
        asserts.header_value(resp, 'content-length', content_length)


def test_response_headers(ctx):
    """
    Balancer must return backend response with custom headers to client
    """
    resp = base_simple_test_response(
        ctx,
        http.response.ok(headers=common.HEADERS),
    )
    asserts.headers_values(resp, common.HEADERS)


def test_unencrypted_request(ctx):
    """
    Balancer must response for unencrypted request
    """
    resp = base_simple_test_response(
        ctx,
        http.response.ok(data=common.SINGLE_DATA),
        ssl=False
    )
    asserts.content(resp, common.SINGLE_DATA)


def test_length_response(ctx):
    """
    Balancer must return backend response with body to client
    """
    resp = base_simple_test_response(
        ctx,
        http.response.ok(data=common.SINGLE_DATA),
    )
    asserts.content(resp, common.SINGLE_DATA)


def test_chunked_response(ctx):
    """
    Balancer must return backend response with chunked body to client
    """
    resp = base_simple_test_response(
        ctx,
        http.response.ok(data=common.DATA),
    )
    asserts.content(resp, common.CONTENT)


def test_long_length_response(ctx):
    """
    If backend's response body does not fit in single DATA frame
    then balancer must send it in multiple DATA frames
    """
    data = 'A' * common.DEFAULT_MAX_FRAME_SIZE * 3
    resp = base_simple_test_response(
        ctx,
        http.response.ok(data=data),
    )
    asserts.is_chunked(resp)
    assert len(resp.data.chunks) > 1
    for chunk in resp.data.chunks:
        assert len(chunk.data) <= common.DEFAULT_MAX_FRAME_SIZE
    asserts.content(resp, data)


@pytest.mark.parametrize(
    'content_length', [
        None,
        # TODO(velavokr): BALANCER-2156 balancer mishandles content-length in HEAD responses
        # 0, 1
    ],
)
def test_head_request(ctx, content_length):
    """
    Client sends HEAD request
    Backend send response with content-length header and without DATA frames
    Balancer must forward client request to backend and backend response to client
    """
    path = '/'
    backend_req, backend_resp = base_simple_test(
        ctx,
        http2.request.head(path),
        http.response.raw_ok(
            headers=(
                {'content-length': content_length} if content_length is not None else {}
            ),
            data=None
        ),
    )
    asserts.method(backend_req, 'HEAD')
    asserts.path(backend_req, path)
    if content_length is None:
        asserts.no_header(backend_resp, 'content-length')
    if content_length is not None:
        asserts.header_value(backend_resp, 'content-length', content_length)


def test_authority_to_host(ctx):
    """
    Balancer must create Host header with value from :authority pseudo-header
    when forwarding request to backend
    """
    auth = 'led.zeppelin'
    backend_req = base_simple_test_request(
        ctx,
        http2.request.get(authority=auth),
    )
    asserts.header_value(backend_req, 'Host', auth)


def test_ignore_authority_host_exists(ctx):
    """
    If request contains host header then balancer must not replace it's value with
    value from :authority pseudo-header
    """
    name = 'host'
    value = 'led.host'
    backend_req = base_simple_test_request(
        ctx,
        http2.request.get(authority='led.zeppelin', headers={name: value}),
    )
    asserts.single_header(backend_req, name)
    asserts.header_value(backend_req, name, value)


def stream_error_request(ctx, conn, req):
    with pytest.raises(errors.StreamError) as exc_info:
        conn.perform_request(req)
    assert exc_info.value.error_code == errors.PROTOCOL_ERROR
    assert ctx.backend.state.requests.empty()


def base_stream_error_test(ctx, req):
    conn = common.start_and_connect(ctx)
    stream_error_request(ctx, conn, req)


def test_no_authority_pseudo_header(ctx):
    """
    Client sends request without authority pseudo-header
    Balancer must forward it to backend without host header
    """
    headers = [
        (':method', 'GET'),
        (':scheme', common.SCHEME),
        (':path', '/'),
    ]
    base_stream_error_test(ctx, http2.request.raw_custom(headers))


def test_reorder_pseudo_headers(ctx):
    """
    If pseudo-headers are reordered in request
    then balancer must forward request to backend and return it's response to client
    """
    headers = [
        (':path', '/'),
        (':authority', common.CUR_AUTH),
        (':scheme', common.SCHEME),
        (':method', 'GET'),
    ]
    backend_req = base_simple_test_request(ctx, http2.request.raw_custom(headers))
    asserts.method(backend_req, 'GET')
    asserts.path(backend_req, '/')
    asserts.header_value(backend_req, 'Host', common.CUR_AUTH)


MANDATORY_PSEUDO_HEADERS = [
    ':method',
    ':scheme',
    ':path',
]


@pytest.mark.parametrize('name', MANDATORY_PSEUDO_HEADERS, ids=[name[1:] for name in MANDATORY_PSEUDO_HEADERS])
def test_no_required_headers(ctx, name):
    """
    If client sends request without required pseudo-header field then balancer must
    reset stream with PROTOCOL_ERROR and must not send request to backend
    """
    headers = {
        ':method': 'GET',
        ':scheme': common.SCHEME,
        ':authority': common.CUR_AUTH,
        ':path': '/',
    }
    del headers[name]
    base_stream_error_test(
        ctx,
        http2.request.raw_custom(headers=headers),
    )


def test_pseudo_header_after_regular(ctx):
    """
    If pseudo-header field appears after regular header field in request
    than balancer must reset stream with PROTOCOL_ERROR and must not send request to backend
    """
    headers = [
        (':method', 'GET'),
        (':scheme', common.SCHEME),
        (':authority', common.CUR_AUTH),
        ('header', 'value'),
        (':path', '/'),
    ]
    base_stream_error_test(
        ctx,
        http2.request.raw_custom(headers=headers),
    )


@pytest.mark.parametrize(
    ['name', 'value'],
    [
        ('upgrade', 'h2c'),
        ('connection', 'keep-alive'),
        ('connection', 'close'),
        ('keep-alive', 'timeout=1200'),
        ('proxy-connection', 'keep-alive'),
        ('transfer-encoding', 'chunked'),
    ],
)
@pytest.mark.parametrize('http_ver', [0, 1])
def test_banned_response_headers(ctx, name, value, http_ver):
    """
    If backend response contains connection-specific header
    then balancer must remove it
    """
    content = 'a' * 10
    has_content_length = False

    if (http_ver == 1 and value not in ('chunked', 'close')) \
            or (http_ver == 0 and value == 'keep-alive'):
        has_content_length = True

    resp = base_simple_test_response(
        ctx,
        http.response.raw_ok(
            version="HTTP/1.{}".format(http_ver),
            headers={name: value, 'content-length': len(content)} if has_content_length else {name: value},
            data=content if value != 'chunked' else [content, '']
        ),
    )
    asserts.no_header(resp, name)
    asserts.content(resp, content)


def test_uppercase_response_header_name(ctx):
    """
    If backend response contains uppercase header name
    then balancer must convert it to lowercase
    """
    conn = common.start_and_connect(ctx, http.response.ok(headers={'Led': 'Zeppelin'}))
    resp = conn.perform_request_raw_response(http2.request.get())

    names = [name for (name, value) in resp.headers.items()]
    assert 'led' in names
    assert 'Led' not in names
    asserts.header_value(resp, 'led', 'Zeppelin')


@pytest.mark.parametrize(
    ['name', 'value'],
    [
        ('upgrade', 'HTTP/2.0'),
        ('connection', 'keep-alive'),
        ('keep-alive', 'timeout=1200'),
        ('proxy-connection', 'keep-alive'),
        ('transfer-encoding', 'chunked'),
        ('te', 'whatever'),
    ],
)
def test_banned_request_headers(ctx, name, value):
    """
    If client sends request with connection-specific headers then balancer must
    reset stream with PROTOCOL_ERROR and must not send request to backend
    """
    base_stream_error_test(
        ctx,
        http2.request.get(headers={name: value}),
    )


@pytest.mark.parametrize(
    ['bad_headers', 'action'],
    [
        ([
            ('Pink', 'Floyd')
        ], "drop"),
        ([
            ('pink:', 'floyd'),
            ('wish<you>', 'were here'),
            ('run like', 'hell'),
            ('hey\x00', 'you')
        ], "drop"),
        ([
            ('owl', 'o\rly'),
            ('header', 'vvv\nwww'),
            ('a', ' \tb'),
            ('c', '\t d'),
            ('e', 'f \t'),
            ('g', 'h\t ')
        ], "drop"),
        ([
            ('', 'floyd')
        ], "error"),
    ],
    ids=[
        'capital_letters',
        'bad_symbols',
        'bad_value',
        'empty_name',
    ]
)
def test_bad_request_headers(ctx, bad_headers, action):
    """
    If request header name or value contains forbidden symbols then balancer must
    reset stream with PROTOCOL_ERROR and must not send request to backend
    """
    conn = common.start_and_connect(ctx)
    cnt = 0
    for name, value in bad_headers:
        cnt += 1
        if action == "drop":
            conn.perform_request(
                http2.request.raw_get(headers={name: value})
            )
            req = ctx.backend.state.get_request()
            asserts.no_header(req, name)
            unistat = ctx.get_unistat()
            assert unistat["report-service_total-inprog_ammv"] == 0
            assert unistat["report-service_total-succ_summ"] == cnt
            assert unistat["http2-h2_conn_open_summ"] == 1
            assert unistat["http2-h2_conn_inprog_ammv"] == 1
            assert unistat["http2-h2_conn_active_ammv"] == 0
            assert unistat["http2-stream_client_open_summ"] == cnt
            assert unistat["http2-stream_dispose_summ"] == cnt
            assert unistat["http2-stream_success_summ"] == cnt
            assert unistat["http2-stream_inprog_ammv"] == 0
            assert unistat["http2-reqs_with_headers_dropped_summ"] == cnt
        else:
            stream_error_request(
                ctx, conn,
                http2.request.raw_get(headers={name: value}),
            )


@pytest.mark.parametrize(
    ['data', 'length'],
    [
        (common.SINGLE_DATA, len(common.SINGLE_DATA) - 1),
        (common.SINGLE_DATA, len(common.SINGLE_DATA) + 1),
        (common.DATA, len(common.CONTENT) - 1),
        (common.DATA, len(common.CONTENT) + 1),
    ],
    ids=[
        'single_lt',
        'single_gt',
        'mult_lt',
        'mult_gt',
    ]
)
def test_wrong_content_length(ctx, data, length):
    """
    If client sends request with content-length header and its value
    does not equal the sum of DATA frame payload lengths
    then balancer must reset stream with PROTOCOL_ERROR and must not send request to backend
    """
    base_stream_error_test(
        ctx,
        http2.request.post(
            data=data,
            headers={'content-length': length}
        ),
    )


def test_continuation(ctx):
    """
    Client sends request headers in multiple frames
    Balancer must decode headers and forward them to backend
    """
    conn = common.start_and_connect(ctx)
    conn.set_max_length(1)
    headers = {'long-header': '#' * 1024}  # to check encoded int splitting
    headers.update(common.HEADERS)
    conn.perform_request(http2.request.get(headers=headers))
    req = ctx.backend.state.get_request()

    asserts.headers_values(req, headers)


def test_request_continuation(ctx):
    """
    BALANCER-1957
    Balancer should not lose portions of the header block
    if there is a really big header block coming as a series of one or more than one CONTINUATIONs
    """
    data = 'A' * 10
    conn = common.start_and_connect(ctx, max_req=2**20, header_list_size=2**20)
    stream = conn.create_stream()
    encoder = hpack.Encoder()

    stream.write_frame(frames.Headers(
        length=None, flags=0,
        reserved=0, stream_id=None,
        data=encoder.encode(
            message.RawHTTP2Message.build_headers(common.SIMPLE_REQ)
        )
    ))

    total_continuations = 10
    headers = []

    for idx in range(total_continuations):
        sub_headers = {'h-' + str(idx): str(idx) * (common.DEFAULT_MAX_FRAME_SIZE - 15)}
        encoded_sub_headers = encoder.encode(
            message.RawHTTP2Message.build_headers(sub_headers)
        )
        assert len(encoded_sub_headers) < common.DEFAULT_MAX_FRAME_SIZE
        headers.append(sub_headers)
        stream.write_frame(frames.Continuation(
            length=None, flags=(0 if idx + 1 < total_continuations else flags.END_HEADERS),
            reserved=0, stream_id=None,
            data=encoded_sub_headers
        ))

    stream.write_chunk(data, end_stream=True)
    resp = stream.read_message().to_response()
    req = ctx.backend.state.get_request()

    asserts.status(resp, 200)

    for sub_headers in headers:
        asserts.headers_values(req, sub_headers, case_sensitive=False)

    asserts.content(req, data)


def test_response_continuation(ctx):
    """
    If backend's response headers do not fit in single HEADERS frame
    then balancer must send it in multiple CONTINUATION frames
    """
    frame_size = 2 ** 14
    name = 'name'
    value = '#' * (frame_size + 1)
    conn = common.start_and_connect(ctx, http.response.ok(headers={name: value}))
    resp = conn.perform_request_raw_response(http2.request.get())
    for frame in resp.frames:
        assert frame.length <= frame_size
    asserts.header_value(resp, name, value)


def test_headers_padded(ctx):
    """
    Balancer must not forward request headers padding to backend
    """
    conn = common.start_and_connect(ctx)
    stream = conn.create_stream()
    stream.write_frame(frames.Headers(
        length=None, flags=flags.END_STREAM | flags.END_HEADERS | flags.PADDED, reserved=0, stream_id=None,
        pad_length=None, data=common.SIMPLE_REQ_ENCODED, padding=common.HEADERS_ENCODED
    ))
    resp = stream.read_message().to_response()
    req = ctx.backend.state.get_request()

    asserts.status(resp, 200)
    asserts.no_headers(req, common.HEADERS.keys())


def test_data_padded(ctx):
    """
    Balancer must not forward request data padding to backend
    """
    data = 'A' * 10
    padding = 'B' * 10
    conn = common.start_and_connect(ctx)
    stream = conn.create_stream()
    stream.write_headers(http2.request.raw_custom(common.SIMPLE_REQ).headers, end_stream=False)
    stream.write_frame(frames.Data(
        length=None, flags=flags.END_STREAM | flags.PADDED, reserved=0, stream_id=None,
        pad_length=None, data=data, padding=padding,
    ))
    resp = stream.read_message().to_response()
    req = ctx.backend.state.get_request()

    asserts.status(resp, 200)
    asserts.content(req, data)


def test_join_request_cookies(ctx):
    """
    Balancer must join cookie headers when forwarding request to non-http2 backend
    """
    cookies = ['a=b', 'c=d', 'e=f']
    conn = common.start_and_connect(ctx)
    resp = conn.perform_request(http2.request.raw_get(
        headers=[('cookie', value) for value in cookies]
    ))
    req = ctx.backend.state.get_request()

    asserts.status(resp, 200)
    asserts.single_header(req, 'cookie')
    asserts.header_value(req, 'cookie', '; '.join(cookies))


def test_100_continue(ctx):
    """
    Balancer must forward 100 continue from backend in a separate headers block in the same stream
    before the main response
    See also BALANCER-1375
    """
    resp_data = 'A' * 20
    request = http2.request.post(headers={'expect': '100-continue'}, data=common.DATA).to_raw_request()
    conn = common.start_and_connect(ctx, ContinueConfig(
        continue_response=http.response.some(status=100, reason='Continue', data=None),
        response=http.response.ok(data=resp_data)
    ))

    stream = conn.create_stream()
    stream.write_headers(request.headers, end_stream=False)
    resp1_headers = stream.read_headers()
    stream.write_data(request.data)
    resp2 = stream.read_message().to_response()
    req = ctx.backend.state.get_request()

    assert resp1_headers.get_one(':status') == '100'
    asserts.status(resp2, 200)
    asserts.content(resp2, resp_data)
    asserts.header_value(req, 'expect', '100-continue')
    asserts.content(req, common.CONTENT)


def test_100_continue_broken(ctx):
    """
    Balancer must treat 100 continue not followed by a final response as malformed
    See also BALANCER-1375
    """
    request = http2.request.post(headers={'expect': '100-continue'}, data=common.DATA).to_raw_request()
    conn = common.start_and_connect(ctx, SimpleConfig(
        response=http.response.some(
            status=100, reason='Continue', headers={'connection': 'close'}, data=None
        )
    ))

    stream = conn.create_stream()
    stream.write_headers(request.headers, end_stream=False)
    common.assert_stream_error(ctx, stream, common.BACKEND_ERROR_PARTIAL_RESP, "ResponseSendError")


@pytest.mark.parametrize(
    'frame',
    [
        common.build_settings([(frames.Parameter.MAX_FRAME_SIZE, 2 ** 15)]),
        frames.Ping(flags=0, reserved=0, data='01234567'),
        frames.WindowUpdate(flags=0, reserved=0, stream_id=0, window_update_reserved=0, window_size_increment=1),
        frames.Goaway(
            length=None, flags=0, reserved=0, goaway_reserved=0,
            last_stream_id=0, error_code=errors.PROTOCOL_ERROR, data='',
        ),
        frames.Unknown(length=None, frame_type=0xA, flags=0, reserved=0, stream_id=0, data='12345'),
    ],
    ids=[
        'settings',
        'ping',
        'window_update',
        'goaway',
        'unknown',
    ]
)
def test_conn_frame_opened_stream(ctx, frame):
    """
    Client sends connection frame in the middle of data transfer of currently opened stream
    Balancer must not treat it as an error
    """
    data = 'A' * 10
    conn = common.start_and_connect(ctx)
    stream = conn.create_stream()
    stream.write_headers(http2.request.raw_custom(common.SIMPLE_REQ).headers, end_stream=False)
    stream.write_chunk(data)
    conn.write_frame(frame, force=True)
    stream.write_chunk(data, end_stream=True)
    req = ctx.backend.state.get_request()

    common.assert_ok_streams([stream])
    asserts.content(req, 2 * data)


def test_parallel_streams(ctx):
    """
    Client sends requests in parallel opened streams
    """
    num_streams = 3
    reqs_data = ['Electric', 'Light', 'Orchestra']
    reqs = [
        http2.request.post(
            '/{}'.format(i),
            data=reqs_data[i]
        ).to_raw_request() for i in range(num_streams)
    ]
    conn = common.start_and_connect(ctx)

    streams = list()
    resps = list()
    backend_reqs = list()
    for i in range(num_streams):
        stream = conn.create_stream()
        stream.write_headers(reqs[i].headers, end_stream=False)
        streams.append(stream)
    for i in range(num_streams):
        streams[i].write_data(reqs[i].data)
        resps.append(streams[i].read_message().to_response())
        backend_reqs.append(ctx.backend.state.get_request())

    for i in range(num_streams):
        resp = resps[i]
        backend_req = backend_reqs[i]
        asserts.status(resp, 200)
        asserts.path(backend_req, '/{}'.format(i))
        asserts.content(backend_req, reqs_data[i])


class ParallelHandler(HTTPServerHandler):
    def handle_request(self, stream):
        stream.read_request_line()
        stream.read_headers()
        stream.write_response_line(self.config.response.response_line)
        stream.write_headers(self.config.response.headers)
        for chunk in self.config.response.data.chunks:
            stream.read_chunk()
            time.sleep(self.config.chunk_timeout)
            stream.write_chunk(chunk)
        self.append_request(stream.request)


class ParallelConfig(ChunkedConfig):
    HANDLER_TYPE = ParallelHandler


def test_parallel_data_client_backend(ctx):
    """
    Client and backend send data to each other in parallel
    Balancer must forward data chunks from client to backend and from backend to client without buffering
    """
    req_data = ['A' * 10] * 10
    resp_data = ['B' * 10] * 10
    chunk_timeout = 0.2
    conn = common.start_and_connect(ctx, ParallelConfig(
        response=http.response.ok(data=resp_data), chunk_timeout=chunk_timeout,
    ))
    stream = conn.create_stream()
    stream.write_headers(http2.request.raw_custom(common.SIMPLE_REQ).headers, end_stream=False)
    got_data = list()
    for chunk in req_data[:-1]:
        stream.write_chunk(chunk)
        time.sleep(chunk_timeout)
        got_data.append(stream.read_chunk())
    stream.write_chunk(req_data[-1], end_stream=True)
    time.sleep(chunk_timeout)
    got_data.append(stream.read_chunk())

    req = ctx.backend.state.get_request()
    asserts.content(req, ''.join(req_data))
    assert ''.join(got_data) == ''.join(resp_data)


def test_send_full_response_before_request_is_finished(ctx):
    """
    Client sends headers without END_STREAM flags set
    Backend sends full response before reading full request
    Balancer must forward response to client with END_STREAM flag
    """
    data = 'A' * 10
    conn = common.start_and_connect(ctx, NoReadConfig(
        force_close=False, response=http.response.raw_ok(headers={'content-length': len(data)}, data=data),
    ))
    stream = conn.create_stream()
    stream.write_headers(http2.request.raw_custom(common.SIMPLE_REQ).headers, end_stream=False)
    resp = stream.read_message().to_response()

    asserts.status(resp, 200)
    asserts.content(resp, data)


@pytest.mark.parametrize(
    'stream_id',
    [5, 101, 2 ** 31 - 1],
    ids=['skip_one', 'skip_multiple', 'skip_all'],
)
def test_client_skip_stream_id(ctx, stream_id):
    """
    Client skips valid stream ids when opening new stream
    Balancer must not treat it as an error
    """
    conn = common.start_and_connect(ctx)
    stream1 = conn.create_stream()
    stream1.write_message(http2.request.get().to_raw_request())
    stream1.read_message()
    stream2 = conn.create_stream(stream_id)
    stream2.write_message(http2.request.get().to_raw_request())
    resp = stream2.read_message().to_response()
    asserts.status(resp, 200)


def test_client_skip_first_stream_id(ctx):
    """
    Client skips the first valid stream id when opening the first stream
    Balancer must not treat it as an error
    """
    conn = common.start_and_connect(ctx)
    stream = conn.create_stream(3)
    stream.write_message(http2.request.get().to_raw_request())
    resp = stream.read_message().to_response()
    asserts.status(resp, 200)


def test_client_even_stream_id(ctx):
    """
    Client opens a new stream with even id
    Balancer must send GOAWAY(PROTOCOL_ERROR) to client and close connection
    """
    conn = common.start_and_connect(ctx)
    stream = conn.create_stream(2)
    stream.write_frame(frames.Headers(
        length=None, flags=0, reserved=0, stream_id=None, data=common.SIMPLE_REQ_ENCODED
    ), force=True)
    common.assert_conn_protocol_error(ctx, conn, 'InvalidFrame')


def test_client_lesser_stream_id(ctx):
    """
    Client opens a new stream with stream id lesser than previously opened stream id
    Balancer must send GOAWAY(PROTOCOL_ERROR) to client and close connection
    """
    conn = common.start_and_connect(ctx)
    stream1 = conn.create_stream(3)
    stream1.write_message(http2.request.get().to_raw_request())
    stream1.read_message()
    stream2 = conn.create_stream(1)
    stream2.write_frame(frames.Headers(
        length=None, flags=0, reserved=0, stream_id=None, data=common.SIMPLE_REQ_ENCODED
    ), force=True)
    common.assert_conn_protocol_error(ctx, conn, 'UnexpectedFrame')


def test_client_exceed_max_concurrent_streams(ctx):
    """
    Client opens more streams than defined in SETTINGS_MAX_CONCURRENT_STREAMS
    Balancer must send RST_STREAM(REFUSED_STREAM) for all streams
    that were opened after SETTINGS_MAX_CONCURRENT_STREAMS was reached
    """
    max_streams = 3
    bad_streams_count = 2
    ctx.start_backend(SimpleDelayedConfig(http.response.ok(), response_delay=2))
    ctx.start_balancer(HTTP2Config(certs_dir=ctx.certs.root_dir, max_concurrent_streams=max_streams))
    conn = common.create_conn(ctx)
    ok_streams = list()
    for _ in range(max_streams):
        stream = conn.create_stream()
        stream.write_message(http2.request.get().to_raw_request())
        ok_streams.append(stream)
    bad_streams = list()
    for _ in range(bad_streams_count):
        stream = conn.create_stream()
        stream.write_message(http2.request.get().to_raw_request())
        bad_streams.append(stream)

    for stream in bad_streams:
        common.assert_stream_error(ctx, stream, errors.REFUSED_STREAM, "MaxStreams", 2)
    for stream in ok_streams:
        resp = stream.read_message().to_response()
        asserts.status(resp, 200)


@pytest.mark.parametrize('error_code', [errors.PROTOCOL_ERROR, 2 ** 32 - 1], ids=['protocol_error', 'unknown_error'])
def test_client_reset_stream(ctx, error_code):
    """
    Balancer must not treat RST_STREAM as connection error
    """
    conn = common.start_and_connect(ctx)
    stream = conn.create_stream()
    stream.write_headers(http2.request.raw_custom(common.SIMPLE_REQ).headers, end_stream=False)
    stream.reset(error_code)
    resp = conn.perform_request(http2.request.get())
    asserts.status(resp, 200)


def test_client_reset_stream_stop_data_transfer_to_client(ctx):
    """
    Client sends request, backend starts sending response
    Client sends RST_STREAM
    Balancer must stop forwarding backend response to client
    and must not send END_STREAM flag
    """
    data = ['A' * 10] * 50
    conn = common.start_and_connect(ctx, ChunkedConfig(http.response.ok(data=data), chunk_timeout=0.1),
                                    backend_timeout=60)
    stream = conn.create_stream()
    stream.write_headers(http2.request.raw_custom(common.SIMPLE_REQ).headers, end_stream=True)
    stream.read_headers()
    chunks = list()
    for _ in range(5):
        chunks.append(stream.read_chunk())
    stream.reset(errors.PROTOCOL_ERROR)
    # wait for frames in transit
    with pytest.raises(NoHTTP2FrameException):
        while True:
            frame = stream.wait_frame(frames.Data)
            assert not frame.flags & flags.END_STREAM
            chunks.append(frame.data)

    content = ''.join(data)
    got_content = ''.join(chunks)
    assert len(content) > len(got_content)


def test_client_reset_stream_stop_data_transfer_to_backend(ctx):
    """
    Client sends request headers, a part of data and RST_STREAM
    Balancer must close connection to backend and must not send the final chunk
    """
    data = ['A' * 10] * 10
    conn = common.start_and_connect(ctx)
    stream = conn.create_stream()
    stream.write_headers(http2.request.raw_custom(common.SIMPLE_REQ).headers, end_stream=False)
    for chunk in data:
        stream.write_chunk(chunk)
        time.sleep(0.1)
    stream.reset(errors.PROTOCOL_ERROR)
    err_request = ctx.backend.state.read_errors.get().raw_message
    common.assert_no_frame(stream, frames.Headers)
    asserts.content(err_request, ''.join(data))


def assert_balancer_ok(ctx):
    conn = common.create_conn(ctx)
    resp = conn.perform_request(http2.request.get())
    asserts.status(resp, 200)


def test_client_conn_close_preface(ctx):
    """
    BALANCER-1152
    If client closes connection while sending preface
    then balancer must not die
    """
    common.start_all(ctx)
    conn = common.create_conn(ctx, setup=False)
    conn.sock.send(conn.PREFACE[:len(conn.PREFACE) / 2])
    conn.close()

    assert_balancer_ok(ctx)


def test_client_conn_close_settings(ctx):
    """
    BALANCER-1152
    If client closes connection while sending settings frame
    then balancer must not die
    """
    conn = common.start_and_connect(ctx)
    frame_str = serialize(common.build_settings([]))
    conn.sock.send(frame_str[:len(frame_str) / 2])
    conn.close()

    assert_balancer_ok(ctx)


def test_client_conn_close_headers_block(ctx):
    """
    BALANCER-1152
    If client closes connection in the middle of request headers block
    then balancer must not die
    """
    conn = common.start_and_connect(ctx)
    stream = conn.create_stream()
    stream.write_frame(frames.Headers(
        length=None, flags=0, reserved=0, stream_id=None, data=common.SIMPLE_REQ_ENCODED
    ))
    conn.close()
    assert ctx.backend.state.accepted.value == 0

    assert_balancer_ok(ctx)


def test_client_conn_close_stop_data_transfer_to_backend(ctx):
    """
    BALANCER-1152
    Client sends request headers, a part of data and closes connection
    Balancer must close connection to backend and must not send the final chunk
    """
    data = ['A' * 10] * 10
    conn = common.start_and_connect(ctx)
    stream = conn.create_stream()
    stream.write_headers(http2.request.raw_custom(common.SIMPLE_REQ).headers, end_stream=False)
    for chunk in data:
        stream.write_chunk(chunk)
        time.sleep(0.1)
    conn.close()
    err_request = ctx.backend.state.read_errors.get().raw_message
    asserts.content(err_request, ''.join(data))


@pytest.mark.parametrize('error_code', [errors.PROTOCOL_ERROR, 2 ** 32 - 1], ids=['protocol_error', 'unknown_error'])
def test_client_goaway(ctx, error_code):
    """
    Client sends GOAWAY frame
    Balancer must not die
    """
    conn = common.start_and_connect(ctx)
    conn.perform_request(http2.request.get(authority=common.CUR_AUTH))
    conn.goaway(1, errors.PROTOCOL_ERROR)
    assert ctx.balancer.is_alive()


# TODO: what should we do with server push?
def test_client_open_stream_after_goaway(ctx):
    """
    Client sends GOAWAY frame and then sends a new request
    Balancer must process the request as usual
    """
    conn = common.start_and_connect(ctx)
    conn.goaway(1, errors.PROTOCOL_ERROR)
    resp = conn.perform_request(http2.request.get(authority=common.CUR_AUTH))
    asserts.status(resp, 200)


@pytest.mark.parametrize(
    'backend',
    [DummyConfig(), DummyConfig(10)],
    ids=['close', 'timeout'],
)
def test_error_before_headers_backend(ctx, backend):
    """
    Backend reads balancer request and does not send response
    Balancer must send RST_STREAM to client
    """
    conn = common.start_and_connect(ctx, backend)
    stream = conn.create_stream()
    stream.write_message(http2.request.get().to_raw_request())
    common.assert_stream_error(
        ctx, stream, common.BACKEND_ERROR_EMPTY_RESP, "BackendError")


def base_broken_data_backend_test(ctx, data, backend, **balancer_kwargs):
    conn = common.start_and_connect(ctx, backend, **balancer_kwargs)
    stream = conn.create_stream()
    stream.write_message(http2.request.get().to_raw_request())
    headers = stream.read_headers()
    resp_data = list()
    with pytest.raises(errors.StreamError) as exc_info:
        while True:
            frame = stream.read_frame()
            if isinstance(frame, frames.Data):
                resp_data.append(frame.data)
                assert not frame.flags & flags.END_STREAM
    assert headers.get_one(':status') == '200'
    assert ''.join(resp_data) == data
    assert exc_info.value.error_code == common.BACKEND_ERROR_PARTIAL_RESP


def test_close_on_data_backend(ctx):
    """
    Backend reads balancer request, sends response headers and a part of body and closes connection
    Balancer must forward headers and received data to client and send RST_STREAM
    """
    data = 'A' * 10
    base_broken_data_backend_test(
        ctx, data,
        CloseConfig(http.response.raw_ok(headers={'content-length': 2 * len(data)}, data=data)),
    )


def test_timeout_on_data_backend(ctx):
    """
    Backend timeouts on sending response data
    Balancer must forward headers and received data to client and send RST_STREAM
    """
    data = 'A' * common.DEFAULT_WINDOW_SIZE
    base_broken_data_backend_test(
        ctx, data,
        ChunkedConfig(http.response.ok(data=[data] * 2), chunk_timeout=10),
        backend_timeout=5,
        stream_send_queue_size_max=common.DEFAULT_WINDOW_SIZE
    )


def test_not_closing_connection_on_backend_error(ctx):
    """
    Balancer must not close connection to client on backend error
    """
    conn = common.start_and_connect(
        ctx, ThreeModeConfig(
            prefix=1, first=1, second=0,
            response=http.response.ok(data=common.DATA)
        ),
    )
    stream = conn.create_stream()
    stream.write_message(http2.request.get().to_raw_request())
    common.assert_stream_error(ctx, stream, common.BACKEND_ERROR_EMPTY_RESP, "BackendError")
    resp = conn.perform_request(http2.request.get())
    asserts.status(resp, 200)
    asserts.content(resp, common.CONTENT)


def test_parallel_connections_one_broken(ctx):
    """
    Client breaks one http2 connection to balancer
    Balancer must continue processing requests in parallel connections without errors
    """
    req_data = 'A' * 10
    resp_data = 'B' * 10
    ok_conn = common.start_and_connect(ctx, http.response.ok(data=resp_data))
    err_conn = common.create_conn(ctx)
    ok_stream = ok_conn.create_stream()
    err_stream = err_conn.create_stream(2)

    ok_stream.write_headers(http2.request.post().to_raw_request().headers, end_stream=False)
    err_stream.write_frame(frames.Headers(
        length=None, flags=0, reserved=0, stream_id=None, data=common.SIMPLE_REQ_ENCODED
    ), force=True)
    common.assert_conn_protocol_error(ctx, err_conn, 'InvalidFrame')
    ok_stream.write_chunk(req_data, end_stream=True)
    resp = ok_stream.read_message().to_response()

    asserts.status(resp, 200)
    asserts.content(resp, resp_data)


def test_ack_ping_frame(ctx):
    """
    Client sends PING frame without ACK flag
    Balancer must send PING frame with ACK flag in response
    Balancer also prepends its own PING to track the RTT experienced by the client
    """
    data = '01234567'
    conn = common.start_and_connect(ctx)
    conn.write_frame(frames.Ping(
        flags=0, reserved=0, data=data,
    ))

    frame = conn.wait_frame(frames.Ping)
    assert not (frame.flags & flags.ACK)
    conn.write_frame(frames.Ping(flags=flags.ACK, reserved=0, data=frame.data))

    frame = conn.wait_frame(frames.Ping)
    assert frame.flags & flags.ACK
    assert frame.data == data


def test_stream_connection_termination_deadlock(ctx):
    """
    BALANCER-1955
    If the stream does not check if the output has terminated
    and the output does not signal its termination to all the streams waiting for it,
    it is possible to deadlock while joining the streams.
    """
    conn = common.start_and_connect(ctx, backend=SimpleConfig(http.response.ok(data='A' * 16 * 1024 * 1024)))
    conn.write_frame(common.build_settings([(frames.Parameter.INITIAL_WINDOW_SIZE, 16 * 1024 * 1024)]))
    time.sleep(1)
    conn.create_stream().write_headers(http2.request.get().to_raw_request().headers, end_stream=True)
    time.sleep(3)
    conn.sock.shutdown(socket.SHUT_WR)
    time.sleep(1)
    conn.sock.close()

    time.sleep(1)
    unistat = ctx.get_unistat()
    assert unistat["report-service_total-inprog_ammv"] == 0
    assert unistat["http2-h2_conn_open_summ"] == 1
    assert unistat["http2-h2_conn_abort_summ"] + unistat["http2-h2_conn_close_summ"] == 1
    assert unistat["http2-h2_conn_inprog_ammv"] == 0
    assert unistat["http2-h2_conn_active_ammv"] == 0
    assert unistat["http2-stream_client_open_summ"] == 1
    assert unistat["http2-stream_dispose_summ"] == 1
    assert unistat["http2-stream_inprog_ammv"] == 0


class BackendIgnoringRequestHandler(HTTPServerHandler):
    def __init__(self, state, sock, config):
        super(BackendIgnoringRequestHandler, self).__init__(state, sock, config)

    def handle_request(self, stream):
        stream.write_response(self.config.response)
        self.sock.shutdown(socket.SHUT_WR)


class BackendIgnoringRequestConfig(HTTPConfig):
    HANDLER_TYPE = BackendIgnoringRequestHandler

    def __init__(self, response):
        self.response = response.to_raw_response()
        super(BackendIgnoringRequestConfig, self).__init__()


def test_unread_closed_stream(ctx):
    """
    BALANCER-1982
    If the server finishes the stream without reading the request the balancer should send RST_STREAM=NO_ERROR
    and should not treat receiving the rest of the request as a connection error.
    """
    conn = common.start_and_connect(
        ctx,
        backend=BackendIgnoringRequestConfig(
            http.response.ok(headers={"connection": "close"})
        ),
    )
    stream = conn.create_stream()
    stream.write_headers(http2.request.get().to_raw_request().headers, end_stream=False)
    resp = stream.read_message().to_response()
    asserts.status(resp, 200)
    assert stream.read_frame().error_code == errors.NO_ERROR

    resp = conn.perform_request(http2.request.get())
    asserts.status(resp, 200)

    stream.write_chunk(chunk="A", end_stream=False)

    resp = conn.perform_request(http2.request.get())
    asserts.status(resp, 200)

    time.sleep(1)
    unistat = ctx.get_unistat()
    assert unistat["report-service_total-inprog_ammv"] == 0
    assert unistat["report-service_total_h1-succ_summ"] == 0
    assert unistat["report-service_total-succ_summ"] == 2
    assert unistat["report-service_total_h2-succ_summ"] == 2
    assert unistat["http2-h2_conn_open_summ"] == 1
    assert unistat["http2-h2_conn_abort_summ"] + unistat["http2-h2_conn_close_summ"] == 0
    assert unistat["http2-h2_conn_inprog_ammv"] == 1
    assert unistat["http2-h2_conn_active_ammv"] == 0
    assert unistat["http2-stream_client_open_summ"] == 3
    assert unistat["http2-end_stream_send_summ"] == 3
    assert unistat["http2-end_stream_recv_summ"] == 2
    assert unistat["http2-stream_success_summ"] == 2
    assert unistat["http2-stream_dispose_summ"] == 3
    assert unistat["http2-rst_stream_send-NO_ERROR_summ"] == 1


@pytest.mark.parametrize(
    'mode',
    ["ok_1", "ok_2", "http_413", "too_many_headers", "too_big_headers"]
)
def test_header_limits(ctx, mode):
    """
    BALANCER-1975
    Balancer should respect its header_list_size promises
    """
    conn = common.start_and_connect(ctx, max_req=2**16, header_list_size=2**18)

    if mode == "ok_1":
        headers = [("a", "A")] * (2 ** 7)
    elif mode == "ok_2":
        headers = [("a", "A" * ((2 ** 16) - len("GET / HTTP/1.1\r\nHost: localhost\r\na: \r\n")))]
    elif mode == "http_413":
        headers = [("a", "A" * ((2 ** 16) - len("GET / HTTP/1.1\r\nHost: localhost\r\na: \r\n") + 1))]
    elif mode == "too_many_headers":
        headers = [("a", "A")] * ((2 ** 9) - 3)
    else:
        headers = [("a", "A" * (2 ** 18))]

    stream = conn.create_stream()
    stream.write_headers(
        http2.request.get(headers=headers).to_raw_request().headers,
        end_stream=True
    )

    if mode.startswith("ok_"):
        resp = stream.read_message().to_response()
        asserts.status(resp, 200)
        req = ctx.backend.state.get_request()
        asserts.header_values(req, 'a', map(lambda x: x[1], headers))
    elif mode == "http_413":
        resp = stream.read_message().to_response()
        asserts.status(resp, 413)
    elif mode == "too_many_headers":
        common.assert_conn_protocol_error(ctx, conn, 'TooManyHeaders')
    elif mode == "too_big_headers":
        common.assert_conn_protocol_error(ctx, conn, 'TooBigHeaders')
