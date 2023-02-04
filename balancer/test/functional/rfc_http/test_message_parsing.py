# -*- coding: utf-8 -*-
import pytest
import time
import multiprocessing

from balancer.test.util.proto.http.stream import HTTPReaderException
from balancer.test.util.proto.handler.server.http import HTTPServerHandler
from balancer.test.util import asserts
from balancer.test.util.stdlib.multirun import Multirun
from balancer.test.util.predef.handler.server.http import SimpleHandler, SimpleConfig, CloseConfig, ChunkedConfig,\
    ContinueConfig, NoReadConfig
from balancer.test.util.predef import http


INVALID_SYMBOL_IN_LENGTH = 'invalid symbol in chunk length'
INVALID_SYMBOL = 'invalid symbol'
NOT_ALL_DATA = 'not all data received'


DATA = 'REPLY'
CHUNKED_DATA = ['Guns', 'N\'', 'Roses']
CHUNKED_WRONG_LENGTH = 6
CHUNKED_PREFIX = '4\r\nGun'


HTTP10_GET = http.request.get(version='HTTP/1.0').to_raw_request()
HTTP10_GET_NO_HEADERS = http.request.raw_get(version='HTTP/1.0')
HTTP11_GET = http.request.get().to_raw_request()
HTTP11_GET_NO_HEADERS = http.request.raw_get()
HTTP11_HEAD = http.request.raw_head()


HTTP10_OK = http.response.raw_ok(version='HTTP/1.0')
HTTP10_OK_LENGTH = http.response.ok(version='HTTP/1.0', data=DATA).to_raw_response()
HTTP10_OK_CHUNKED = http.response.ok(version='HTTP/1.0', data=CHUNKED_DATA).to_raw_response()
HTTP10_OK_LENGTH_CHUNKED = http.response.raw_ok(
    version='HTTP/1.0',
    headers={'content-length': len(DATA), 'transfer-encoding': 'chunked'},
    data=DATA
)
HTTP10_NO_CONTENT = http.response.raw_no_content(version='HTTP/1.0')
HTTP10_NOT_MODIFIED = http.response.raw_not_modified(version='HTTP/1.0')
HTTP10_OK_LENGTH_TOO_LONG_BODY = http.response.raw_ok(
    version='HTTP/1.0',
    headers={'content-length': len(DATA), 'connection': 'Keep-Alive'},
    data=DATA + 'A' * 10
)
HTTP10_OK_LENGTH_LIKE_CHUNKED = http.response.raw_ok(
    version='HTTP/1.0',
    headers={'content-length': CHUNKED_WRONG_LENGTH, 'connection': 'Keep-Alive'},
    data=CHUNKED_DATA
)
HTTP10_OK_NO_HEADERS = http.response.raw_ok(version='HTTP/1.0', data=DATA)
HTTP10_OK_NO_HEADERS_CHUNKED_LIKE = http.response.raw_ok(version='HTTP/1.0', data='5\r\nREPLY\r\n0\r\n\r\n')

HTTP11_OK = http.response.raw_ok()
HTTP11_OK_LENGTH = http.response.ok(data=DATA).to_raw_response()
HTTP11_OK_CHUNKED = http.response.ok(data=CHUNKED_DATA).to_raw_response()
HTTP11_OK_CHUNKED_HEX = http.response.ok(data=['12345678901112']).to_raw_response()
HTTP11_OK_LENGTH_CHUNKED = http.response.raw_ok(
    headers={'content-length': CHUNKED_WRONG_LENGTH, 'transfer-encoding': 'chunked'},
    data=CHUNKED_DATA
)
HTTP11_NO_CONTENT = http.response.raw_no_content()
HTTP11_NOT_MODIFIED = http.response.raw_not_modified()
HTTP11_OK_NO_DATA = http.response.raw_ok()
HTTP11_OK_LENGTH_NO_DATA = http.response.raw_ok(
    headers={'content-length': len(DATA)})
HTTP11_OK_CHUNKED_NO_DATA = http.response.raw_ok(
    headers={'transfer-encoding': 'chunked'})
HTTP11_OK_LENGTH_TOO_LONG_BODY = http.response.raw_ok(
    headers={'content-length': len(DATA)},
    data=DATA + 'A' * 10
)
HTTP11_OK_CHUNKED_TRAILING_CHUNKS = http.response.ok(data=[DATA, '', ':)']).to_raw_response()
HTTP11_OK_LENGTH_LIKE_CHUNKED = http.response.raw_ok(
    headers={'content-length': CHUNKED_WRONG_LENGTH},
    data=CHUNKED_DATA
)
HTTP11_OK_LENGTH_CHUNKED_INVALID_DATA = http.response.raw_ok(
    headers={'transfer-encoding': 'chunked', 'content-length': len(DATA)},
    data=DATA
)
HTTP11_OK_NO_HEADERS = http.response.raw_ok(data=DATA)
HTTP11_OK_NO_HEADERS_CHUNKED_LIKE = http.response.raw_ok(data='5\r\nREPLY\r\n0\r\n\r\n')


@pytest.mark.parametrize(
    ['req', 'backend_resp'],
    [
        (HTTP10_GET, HTTP10_OK_LENGTH),
        (HTTP10_GET, HTTP10_OK_LENGTH_CHUNKED),
        (HTTP10_GET, HTTP10_OK_CHUNKED),
        (HTTP10_GET, HTTP11_OK_LENGTH),
        (HTTP10_GET, HTTP11_OK_CHUNKED),  # SEPE-4171
        (HTTP11_GET, HTTP10_OK_LENGTH),
        (HTTP11_GET, HTTP10_OK_LENGTH_CHUNKED),  # SEPE-4171
        (HTTP11_GET, HTTP10_OK_CHUNKED),
        (HTTP11_GET, HTTP11_OK_LENGTH),
    ],
    ids=[
        'client_http10,backend_http10_length',
        'client_http10,backend_http10_length_chunked',
        'client_http10,backend_http10_chunked',
        'client_http10,backend_http11_length',
        'client_http10,backend_http11_chunked',
        'client_http11,backend_http10_length',
        'client_http11,backend_http10_length_chunked',
        'client_http11,backend_http10_chunked',
        'client_http11,backend_http11_length',
    ]
)
def test_length_response_content(rfc_ctx, req, backend_resp):
    """
    Балансер должен вернуть ответ бэкенда.
    Длина тела должна определяться значением заголовка content-length, если клиент не HTTP/1.0 .
    """
    rfc_ctx.start_rfc_backend(SimpleConfig(response=backend_resp))
    rfc_ctx.start_rfc_balancer()

    conn = rfc_ctx.create_http_connection()
    resp = conn.perform_request_raw_response(req)

    if 'content-length' in backend_resp.headers:
        asserts.is_content_length(resp)
    elif req.request_line.version != 'HTTP/1.0':
        asserts.is_chunked(resp)
    asserts.content(resp, backend_resp.data.content)


@pytest.mark.parametrize(
    ['req', 'backend_resp'],
    [
        (HTTP10_GET, HTTP10_OK_NO_HEADERS),
        (HTTP10_GET, HTTP11_OK_NO_HEADERS),
        (HTTP10_GET, HTTP11_OK_NO_HEADERS_CHUNKED_LIKE),
        (HTTP10_GET, HTTP10_OK),
        (HTTP10_GET, HTTP11_OK),
        (HTTP11_GET, HTTP10_OK),
        (HTTP11_GET, HTTP10_OK_NO_HEADERS),
    ],
    ids=[
        'client_http10,backend_http10',
        'client_http10,backend_http11',
        'client_http10,backend_http11_chunked_like',
        'client_http10,backend_http10_empty',
        'client_http10,backend_http11_empty',
        'client_http11,backend_http10_empty',
        'client_http11,backend_http10',
    ],
)
def test_length_response_content_backend_close(rfc_ctx, req, backend_resp):
    """
    Окончание тела ответа бэкенда определяется концом соединения.
    Балансер должен вернуть ответ бэкенда.
    Длина тела должна определяться значением заголовка content-length, если клиент не HTTP/1.0 .
    """
    rfc_ctx.start_rfc_backend(CloseConfig(response=backend_resp))
    rfc_ctx.start_rfc_balancer()

    conn = rfc_ctx.create_http_connection()
    resp = conn.perform_request_raw_response(req)

    if req.request_line.version != 'HTTP/1.0':
        asserts.is_chunked(resp)
    asserts.content(resp, backend_resp.data.content)


@pytest.mark.parametrize(
    ['req', 'backend_resp'],
    [
        (HTTP11_GET, HTTP11_OK_CHUNKED),
        (HTTP11_GET, HTTP11_OK_LENGTH_CHUNKED),  # SEPE-4170
        (HTTP11_GET, HTTP11_OK_CHUNKED_HEX),
    ],
    ids=[
        'client_http11,backend_http11_chunked',
        'client_http11,backend_http11_length_chunked',
        'hex_chunk_length',
    ]
)
def test_chunked_response_content(rfc_ctx, req, backend_resp):
    """
    Балансер должен вернуть ответ бэкенда.
    Тело ответа должно быть в chunked transfer encoding.
    """
    rfc_ctx.start_rfc_backend(SimpleConfig(response=backend_resp))
    rfc_ctx.start_rfc_balancer()

    conn = rfc_ctx.create_http_connection()
    resp = conn.perform_request_raw_response(req)

    asserts.is_chunked(resp)
    asserts.content(resp, backend_resp.data.content)


@pytest.mark.parametrize(
    ['req', 'backend_resp'],
    [
        (HTTP11_GET, HTTP11_OK_NO_HEADERS),
        (HTTP11_GET, HTTP11_OK_NO_HEADERS_CHUNKED_LIKE),
        (HTTP11_GET, HTTP11_OK),
    ],
    ids=[
        'client_http11,backend_http11',
        'client_http11,backend_http11_chunked_like',
        'client_http11,backend_http11_empty',
    ]
)
def test_chunked_response_content_backend_close(rfc_ctx, req, backend_resp):
    """
    Балансер должен вернуть ответ бэкенда.
    Тело ответа должно быть в chunked transfer encoding.
    """
    rfc_ctx.start_rfc_backend(CloseConfig(response=backend_resp))
    rfc_ctx.start_rfc_balancer()

    conn = rfc_ctx.create_http_connection()
    resp = conn.perform_request_raw_response(req)

    asserts.is_chunked(resp)
    asserts.content(resp, backend_resp.data.content)


# not using parametrization because of memory usage
# TODO: generators for message bodies
def test_big_chunked(rfc_ctx):
    """
    Балансер должен полностью передать чанку большого размера клиенту.
    """
    backend_resp = http.response.ok(data=['A' * 1073741824]).to_raw_response()
    test_chunked_response_content(rfc_ctx, HTTP11_GET, backend_resp)


@pytest.mark.parametrize(
    ['req', 'backend_resp'],
    [
        (HTTP11_GET, HTTP10_NO_CONTENT),
        (HTTP11_GET, HTTP10_NOT_MODIFIED),
        (HTTP11_GET, HTTP11_NO_CONTENT),
        (HTTP11_GET, HTTP11_NOT_MODIFIED),
        (HTTP11_HEAD, HTTP11_OK_NO_DATA),
        (HTTP11_HEAD, HTTP11_OK_LENGTH_NO_DATA),
        (HTTP11_HEAD, HTTP11_OK_CHUNKED_NO_DATA),
    ],
    ids=[
        'http10_204_response',
        'http10_304_response',
        'http11_204_response',
        'http11_304_response',
        'head_request',
        'head_request_length_response',
        'head_request_chunked_response',
    ]
)
def test_empty_response_body(rfc_ctx, req, backend_resp):
    """
    SEPE-6035
    Балансер должен передать ответ бэкенда с пустым телом.
    """
    rfc_ctx.start_rfc_backend(SimpleConfig(response=backend_resp))
    rfc_ctx.start_rfc_balancer()

    conn = rfc_ctx.create_http_connection()
    resp = conn.perform_request_raw_response(req)

    assert resp.data.raw == ''


@pytest.mark.parametrize(
    ['req', 'backend_resp', 'expected_content'],
    [
        (HTTP10_GET, HTTP10_OK_LENGTH_TOO_LONG_BODY, DATA),
        (HTTP11_GET, HTTP11_OK_LENGTH_TOO_LONG_BODY, DATA),
        (HTTP11_GET, HTTP11_OK_LENGTH_LIKE_CHUNKED, CHUNKED_PREFIX),
        (HTTP11_GET, HTTP11_OK_CHUNKED_TRAILING_CHUNKS, DATA),
    ],
    ids=[
        'http10',
        'http11',
        'chunked_like',
        'trailing_chunks',
    ]
)
def test_data_after_response_body(rfc_ctx, req, backend_resp, expected_content):
    """
    Если после тела ответа есть еще данные, то балансер не должен пересылать их клиенту.
    """
    rfc_ctx.start_rfc_backend(SimpleConfig(response=backend_resp))
    rfc_ctx.start_rfc_balancer(keepalive_count=1)

    resp = rfc_ctx.perform_request(req)
    # TODO
    # rfc_ctx.perform_request_xfail(req)

    asserts.content(resp, expected_content)


@pytest.mark.parametrize(
    ['req', 'backend_resp', 'raw_data', 'err_msg'],
    [
        (HTTP11_GET, HTTP11_OK_LENGTH_CHUNKED_INVALID_DATA, '', INVALID_SYMBOL_IN_LENGTH),
    ],
    ids=[
        'backend_http11_length_chunked,invalid_chunked_data',
    ]
)
def test_bad_backend_response_body(rfc_ctx, req, backend_resp, raw_data, err_msg):
    """
    В случае ошибки бэкенда балансер должен закрыть соединение с клиентом и записать об ошибке в лог.
    """
    rfc_ctx.start_rfc_backend(CloseConfig(response=backend_resp))
    rfc_ctx.start_rfc_balancer()

    read_err = rfc_ctx.perform_request_xfail(req)

    assert read_err.raw_message.data.raw == raw_data
    for run in Multirun():
        with run:
            errorlog = rfc_ctx.manager.fs.read_file(rfc_ctx.balancer.config.errorlog)
            assert err_msg in errorlog


@pytest.mark.parametrize(
    ['data', 'expected', 'err_msg'],
    [
        (DATA, '', INVALID_SYMBOL_IN_LENGTH),  # SEPE-4074

        ('2\r\nREPLY\r\n0\r\n\r\n', '', INVALID_SYMBOL),  # SEPE-4073
        ('5\r\nREPLY\r\n3\r\nDATA\r\n0\r\n\r\n', '5\r\nREPLY\r\n', INVALID_SYMBOL),
        ('3\r\nRE\r\n3\r\nPLY\r\n0\r\n\r\n', '', INVALID_SYMBOL),  # SEPE-4073

        ('22\r\nDATA', '4\r\nDATA\r\n', NOT_ALL_DATA),  # SEPE-4172

        ('-2\r\nREPLY\r\n0\r\n\r\n', '', INVALID_SYMBOL_IN_LENGTH),
        ('XYZ\r\nREPLY\r\n0\r\n\r\n', '', INVALID_SYMBOL_IN_LENGTH),
        ('A%\r\nREPLY\r\n0\r\n\r\n', '', INVALID_SYMBOL_IN_LENGTH),
        ('ы\r\nREPLY\r\n0\r\n\r\n', '', INVALID_SYMBOL_IN_LENGTH),

        ('5\r\nREPLY0\r\n0\r\n\r\n', '', INVALID_SYMBOL),
        ('\r\n\r\nREPLY\r\n0\r\n\r\n', '', INVALID_SYMBOL_IN_LENGTH),
        ('5\r\nREPLY\r\n\r\nDATA\r\n0\r\n\r\n', '5\r\nREPLY\r\n', INVALID_SYMBOL_IN_LENGTH),

        ('0\r\nREPLY0\r\n0\r\n\r\n', '', INVALID_SYMBOL),  # SEPE-6260
    ],
    ids=[
        'backend_http11_chunked,invalid_chunked_data',

        'lt_chunk_length',
        'lt_middle_chunk_length',
        'gt_chunk_length',

        'chunked_incomplete',

        'non_hex_letters',
        'negative_length',
        'invalid_symbol_in_length',
        'non_ascii_symbol_in_length',

        'missed_crlf',
        'missed_chunk_length',
        'missed_middle_chunk_length',

        'first_zero_chunk',
    ]
)
def test_incorrect_chunked_encoding(rfc_ctx, data, expected, err_msg):
    """
    Если бэкенд тело ответа бэкенда в chunked transfer encoding, и содержит ошибки,
    то балансер не должен отправлять клиенту нулевой чанк,
    должен закрыть соединение с клиентом и написать об ошибке в лог.
    """
    backend_resp = http.response.raw_ok(
        headers={'transfer-encoding': 'chunked'},
        data=data
    )
    rfc_ctx.start_rfc_backend(CloseConfig(response=backend_resp))
    rfc_ctx.start_rfc_balancer()

    read_err = rfc_ctx.perform_request_xfail(HTTP11_GET)

    assert read_err.raw_message.data.raw == expected
    for run in Multirun():
        with run:
            errorlog = rfc_ctx.manager.fs.read_file(rfc_ctx.balancer.config.errorlog)
            assert err_msg in errorlog


@pytest.mark.parametrize(
    'req',
    [HTTP10_GET_NO_HEADERS, HTTP11_GET_NO_HEADERS],
    ids=['http10', 'http11']
)
def test_empty_request_body(rfc_ctx, req):
    """
    SEPE-6035
    Если клиент задает запрос без заголовков Content-Length и Transfer-Encoding,
    то балансер не должен добавлять эти заголовки.
    """
    rfc_ctx.start_rfc_backend(SimpleConfig())
    rfc_ctx.start_rfc_balancer()

    rfc_ctx.perform_request(req)

    backend_req = rfc_ctx.backend.state.get_request()
    assert backend_req.data.raw == ''
    asserts.no_header(backend_req, 'content-length')
    asserts.no_header(backend_req, 'transfer-encoding')


def test_chunked_response_timeout(rfc_ctx):
    """
    SEPE-3714
    Если бэкенд отправляет данные чанками и не успевает отправить все тело за backend_timeout,
    то балансер должен отправить полученные данные клиенту, не отправлять нулевой чанк и закрыть соединение с клиентом.
    """
    response = http.response.ok(data=['led', 'zeppelin'])
    chunk_timeout = 8
    rfc_ctx.start_rfc_backend(ChunkedConfig(response=response, chunk_timeout=chunk_timeout))

    rfc_ctx.start_rfc_balancer(backend_timeout=chunk_timeout/2)

    with rfc_ctx.create_http_connection() as conn:
        with pytest.raises(HTTPReaderException) as exc_info:
            conn.perform_request(HTTP11_GET)
        assert exc_info.value.read_error.raw_message.data.raw == '3\r\nled\r\n'
        asserts.is_closed(conn.sock)


class LengthDelayedHandler(SimpleHandler):

    def handle_parsed_request(self, raw_request, stream):
        stream.write_response_line(self.config.response.response_line)
        stream.write_headers(self.config.response.headers)
        data = self.config.response.data.content
        while data:
            stream.write(data[:self.config.data_len])
            data = data[self.config.data_len:]
            time.sleep(self.config.delay)
        self.finish_response()


class LengthDelayedConfig(SimpleConfig):
    HANDLER_TYPE = LengthDelayedHandler

    def __init__(self, response, data_len, delay):
        super(LengthDelayedConfig, self).__init__(response)
        self.data_len = data_len
        self.delay = delay


def test_length_response_timeout(rfc_ctx):
    """
    Если бэкенд не успевает отправить все тело за backend_timeout,
    то балансер должен отправить клиенту полученные данные и закрыть соединение.
    """
    response = http.response.ok(data='A' * 100)
    rfc_ctx.start_rfc_backend(LengthDelayedConfig(response=response, data_len=5, delay=4))

    rfc_ctx.start_rfc_balancer(backend_timeout=10)

    with rfc_ctx.create_http_connection() as conn:
        with pytest.raises(HTTPReaderException) as exc_info:
            conn.perform_request(HTTP11_GET)
        assert exc_info.value.read_error.raw_message.data.raw == 'A' * 15
        asserts.is_closed(conn.sock)


def test_no_backend(rfc_ctx):
    """
    SEPE-4677
    Если на указанном порту нет backend-а, то балансер закрывает соединение с клиентом и не падает
    """
    rfc_ctx.start_rfc_balancer()
    rfc_ctx.perform_request_xfail(HTTP11_GET)

    time.sleep(1)
    assert rfc_ctx.balancer.is_alive()


def test_chunked_post(rfc_ctx):
    """
    SEPE-3923
    Проверка поддержки chunked POST запросов
    """
    rfc_ctx.start_rfc_backend(SimpleConfig())
    rfc_ctx.start_rfc_balancer()
    response = rfc_ctx.perform_request(http.request.post(data=CHUNKED_DATA))
    req = rfc_ctx.backend.state.get_request()

    asserts.status(response, 200)
    asserts.content(req, ''.join(CHUNKED_DATA))


class NotAllHeadersHandler(SimpleHandler):

    def handle_parsed_request(self, raw_request, stream):
        stream.write_response_line(self.config.response.response_line)
        for name, value in self.config.response.headers.items():
            stream.write_header(name, value)
        self.force_close()


class NotAllHeadersConfig(SimpleConfig):
    HANDLER_TYPE = NotAllHeadersHandler


def test_not_all_headers(rfc_ctx):
    """
    SEPE-4203
    Если бэкенд закрыл соединение, не отправив все заголовки,
    то балансер должен сделать запись в лог и закрыть соединение
    """
    headers = {'Led': 'Zeppelin', 'Pink': 'Floyd'}
    rfc_ctx.start_rfc_backend(NotAllHeadersConfig(response=http.response.ok(headers=headers)))
    rfc_ctx.start_rfc_balancer()

    with rfc_ctx.create_http_connection() as conn:
        with pytest.raises(HTTPReaderException):  # as exc_info
            conn.perform_request(HTTP11_GET)
        asserts.is_closed(conn.sock)

    for run in Multirun():
        with run:
            errorlog = rfc_ctx.manager.fs.read_file(rfc_ctx.balancer.config.errorlog)
            assert 'http response parse error: 400 incomplete response input' in errorlog


class ParallelHandler(HTTPServerHandler):

    def handle_request(self, stream):
        def read_request():
            req = stream.read_request()
            self.append_request(req)
        process = multiprocessing.Process(target=read_request)
        process.start()
        stream.write_response_line(self.config.response.response_line)
        stream.write_headers(self.config.response.headers)
        for chunk in self.config.response.data.chunks:
            stream.write_chunk(chunk)
            time.sleep(self.config.chunk_timeout)
        process.join()


class ParallelConfig(ChunkedConfig):
    HANDLER_TYPE = ParallelHandler


def test_chunked_parallel(rfc_ctx):
    """
    Клиент и backend шлют друг другу данные параллельно, используя chunked transfer encoding
    Клиент должен получить все данные, отправленные backend-ом,
    backend должен получить все данные, отправленные клиентом
    """
    req = http.request.post(data=['A' * 50] * 50).to_raw_request()
    backend_resp = http.response.ok(data=['B' * 10] * 60)

    rfc_ctx.start_rfc_backend(ParallelConfig(response=backend_resp, chunk_timeout=0.1))
    rfc_ctx.start_rfc_balancer(backend_timeout='100500s')

    with rfc_ctx.create_http_connection() as conn:
        stream = conn.create_stream()

        def write_request_timeout():
            stream.write_request_line(req.request_line)
            stream.write_headers(req.headers)
            for chunk in req.data.chunks:
                time.sleep(0.15)
                stream.write_chunk(chunk)
        process = multiprocessing.Process(target=write_request_timeout)
        process.start()
        resp = stream.read_response()
        process.join()
        asserts.content(resp, backend_resp.data.content)

    backend_req = rfc_ctx.backend.state.get_request()
    asserts.content(backend_req, req.data.content)


def test_100_continue(rfc_ctx):
    """
    Клиент отправляет строку запроса и заголовком Expect: 100-continue
    backend отвечает 100 Continue
    клиент отправляет тело запроса
    backend отправляет ответ на запрос

    Балансер должен правильно передавать все данные в обе стороны
    """
    resp_data = 'A' * 20
    rfc_ctx.start_rfc_backend(ContinueConfig(
        continue_response=http.response.some(status=100, reason='Continue', data=None),
        response=http.response.ok(data=resp_data)))
    rfc_ctx.start_rfc_balancer()

    request = http.request.get(headers={'Expect': '100-continue'}, data=['12345']).to_raw_request()
    with rfc_ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write_request_line(request.request_line)
        stream.write_headers(request.headers)
        resp1 = stream.read_next_response()
        stream.write_data(request.data)
        resp2 = stream.read_next_response()
    req = rfc_ctx.backend.state.get_request()

    asserts.status(resp1, 100)
    asserts.status(resp2, 200)
    asserts.content(resp2, resp_data)
    asserts.header_value(req, 'expect', '100-continue')
    asserts.content(req, request.data.content)


def test_100_continue_http10_client(rfc_ctx):
    """
    Клиент отправлят запрос по HTTP/1.0 без заголовка Expect: 100-continue
    backend отвечает 100 Continue, затем отправляет ответ на запрос

    Балансер не должен отправлять клиенту сообщение 100-continue
    """
    resp_data = 'A' * 20
    rfc_ctx.start_rfc_backend(ContinueConfig(
        continue_response=http.response.some(status=100, reason='Continue', data=None),
        response=http.response.ok(data=resp_data)))
    rfc_ctx.start_rfc_balancer()

    with rfc_ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write_request(http.request.raw_get(version='HTTP/1.0'))
        response = stream.read_next_response()

    asserts.status(response, 200)
    asserts.content(response, resp_data)


def base_100_continue_final_status_test(rfc_ctx, status):
    rfc_ctx.start_rfc_balancer()

    request = http.request.get(headers={'Expect': '100-continue'}, data=['12345']).to_raw_request()
    with rfc_ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write_request_line(request.request_line)
        stream.write_headers(request.headers)
        resp = stream.read_next_response()
        time.sleep(0.5)
        stream.write_data(request.data)

    asserts.status(resp, status)
    time.sleep(3)
    errorlog = rfc_ctx.manager.fs.read_file(rfc_ctx.balancer.config.errorlog)
    accesslog = rfc_ctx.manager.fs.read_file(rfc_ctx.balancer.config.accesslog)
    assert 'backend error' not in errorlog
    assert 'backend system_error EIO' not in accesslog


def test_100_continue_final_status_close(rfc_ctx):
    """
    BALANCER-93
    Клиент отправляет стартовую строку и заголовки,
    среди которых есть Expect: 100-continue и Transfer-Encoding: chunked.
    Backend отвечает с final status code и закрывает соединение.
    Клиент получает ответ и отправляет тело запроса

    Балансер не должен считать такое поведение ошибкой бэкенда
    """
    status = 417
    reason = 'Expectation Failed'
    rfc_ctx.start_rfc_backend(NoReadConfig(force_close=True, response=http.response.some(status=status, reason=reason)))
    base_100_continue_final_status_test(rfc_ctx, status)


def test_100_continue_final_status_keepalive(rfc_ctx):
    """
    BALANCER-93
    Клиент отправляет стартовую строку и заголовки,
    среди которых есть Expect: 100-continue и Transfer-Encoding: chunked.
    Backend отвечает с final status code и не закрывает соединение.
    Клиент получает ответ и отправляет тело запроса

    Балансер не должен считать такое поведение ошибкой бэкенда
    """
    rfc_ctx.start_rfc_backend(NoReadConfig(
        force_close=False,
        response=http.response.raw_ok(headers={'Connection': 'Keep-Alive', 'Content-length': 0})))
    base_100_continue_final_status_test(rfc_ctx, 200)


def test_request_from_future(rfc_ctx):
    """
    Балансер должен передать заголовок Date от бэкенда без изменений,
    даже если в нем указано время из будущего
    """
    header = 'Date'
    date = "%s, %02d %3s %4d %02d:%02d:%02d GMT" % ('Tue', 21, 'Mar', 2111, 5, 43, 65)
    backend_resp = http.response.ok(headers={header: date})
    rfc_ctx.start_rfc_backend(SimpleConfig(response=backend_resp))
    rfc_ctx.start_rfc_balancer()

    balancer_resp = rfc_ctx.perform_request(http.request.get())
    asserts.header_value(balancer_resp, header, date)


def test_empty_reason_phrase(rfc_ctx):
    """
    SEPE-5661
    Backend отвечает с пустым reason phrase (HTTP/1.1 200)
    Балансер должен передать ответ клиенту и не упасть
    """
    rfc_ctx.start_rfc_backend(SimpleConfig(response=http.response.some(status=200, reason='')))
    rfc_ctx.start_rfc_balancer()

    response = rfc_ctx.perform_request(http.request.get())
    assert rfc_ctx.balancer.is_alive()
    asserts.status(response, 200)
    asserts.reason_phrase(response, '')
