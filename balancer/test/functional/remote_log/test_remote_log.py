# -*- coding: utf-8 -*-
"""
BALANCER-30
"""
import json
import logging
import time

from configs import RemoteLogConfig, RemoteLogWithDelayConfig

from balancer.test.util import asserts
from balancer.test.util.predef.handler.server.http import ChunkedConfig, BrokenConfig, ContinueConfig
from balancer.test.util.predef import http
import zlib


HEADERS_WITH_ESC = http.request.raw_get(headers={
    'For-delete-1': '1',
    'For-delete-2': '2',
    'For-delete': '3',
    'For-Delete': '',
    'for-Delete': '4',
    'X-Forwarded-For-Y': '321.333.4.3',
    'XXX-Forwarded-For-YYY': '321.333.4.3',
    'Led': 'Zeppelin',
    'Pink': 'Floyd',
    'uSer-aGeNt': 'dwew\trfer',
    'host': 's"""s',
    'Content-Length': 0,
    'X-Yandex-ExpFlags-Pre': 'X'*42,
    'X-Yandex-ExpFlags': 'Y'*30000,
    'X-Yandex-Internal-Request': '1',
    'X-Yandex-Suspected-Robot': '0',
    'X-Yandex-TCP-Info': 'v=2; rtt=0.083766s; rttvar=0.002507s; snd_cwnd=10; total_retrans=0:'
})


def get_value(compress, content):
    if compress:
        data = zlib.decompress(content)
    else:
        data = content
    return json.loads(data[data.find(' ') + 1:])


def test_json(ctx):
    """
    Проверяет корректность записи в лог
    """
    ctx.start_backend(ChunkedConfig(), name='storage')
    ctx.start_backend(ChunkedConfig())

    ctx.start_balancer(RemoteLogConfig())
    response = ctx.perform_request(HEADERS_WITH_ESC)
    asserts.status(response, 200)
    req = ctx.storage.state.get_request()
    value = get_value(False, req.data.content)
    assert set(value.keys()) == {'addr', 'method', 'request', 'protocol', 'status',
                                 'headers', 'timestamp', 'resptime_us', 'hdrs_len', 'backend_errors'}
    addr = value[u'addr']
    assert value[u'method'] == u'GET'
    assert value[u'request'] == u'/'
    assert value[u'protocol'] == u'HTTP/1.1'
    assert value[u'status'] == 200
    assert value[u'hdrs_len'] == 30445
    assert isinstance(value[u'resptime_us'], int)
    assert isinstance(value[u'timestamp'], int)
    assert addr.startswith(u'127.0.0.1') or addr.startswith(u'[::1]')
    assert [u'host', u's"""s'] in value[u'headers']
    assert u'timestamp' in value
    assert u'resptime_us' in value
    assert u'uaas_mode' not in value


def test_uaas_mode_flag(ctx):
    """
    Работоспособность флага uaas_mode USEREXP-3733
    """
    ctx.start_backend(ChunkedConfig(), name='storage')
    ctx.start_backend(ChunkedConfig())
    ctx.start_balancer(RemoteLogConfig(uaas_mode=True))
    response = ctx.perform_request(HEADERS_WITH_ESC)
    asserts.status(response, 200)
    req = ctx.storage.state.get_request()
    value = get_value(False, req.data.content)
    assert value[u'status'] == 200
    assert value[u'uaas_mode'] == 'true'


def test_gzip_json(ctx):
    """
    Проверяет корректность записи в лог
    """
    level_compress_file = ctx.manager.fs.create_file('level_compress')
    ctx.start_backend(ChunkedConfig(), name='storage')
    ctx.start_backend(ChunkedConfig())
    ctx.manager.fs.rewrite(level_compress_file, "1")

    ctx.start_balancer(RemoteLogConfig(level_compress_file=level_compress_file))
    response = ctx.perform_request(HEADERS_WITH_ESC)
    asserts.status(response, 200)
    req = ctx.storage.state.get_request()
    value = get_value(True, req.data.content)
    addr = value[u'addr']
    assert value[u'method'] == u'GET'
    assert value[u'request'] == u'/'
    assert value[u'protocol'] == u'HTTP/1.1'
    assert value[u'status'] == 200
    assert addr.startswith(u'127.0.0.1') or addr.startswith(u'[::1]')
    assert [u'host', u's"""s'] in value[u'headers']


def test_no_remote_log_file(ctx):
    """
    USEREXP-904
    При появлении файла, указанного в no_remote_log_file,
    балансер должен всегда подавлять посылку данных в loger
    """
    no_remote_log_file = ctx.manager.fs.create_file('no_remote_log')
    ctx.manager.fs.remove(no_remote_log_file)
    ctx.start_backend(ChunkedConfig(), name='storage')
    ctx.start_backend(ChunkedConfig())

    ctx.start_balancer(RemoteLogConfig(no_remote_log_file=no_remote_log_file))
    time.sleep(2)
    response = ctx.perform_request(HEADERS_WITH_ESC)
    asserts.status(response, 200)
    req = ctx.storage.state.get_request()
    value = get_value(False, req.data.content)
    assert value[u'status'] == 200
    ctx.manager.fs.rewrite(no_remote_log_file, '')
    time.sleep(2)
    response = ctx.perform_request(HEADERS_WITH_ESC)
    asserts.status(response, 200)
    time.sleep(2)
    assert ctx.storage.state.requests.empty()


def test_no_remote_log_file_remove(ctx):
    """
    USEREXP-904
    При удалении файла, указанного в no_remote_log_file,
    балансер должен всегда возобновлять посылку данных в loger
    """
    no_remote_log_file = ctx.manager.fs.create_file('no_remote_log')
    ctx.start_backend(ChunkedConfig(), name='storage')
    ctx.start_backend(ChunkedConfig())

    ctx.start_balancer(RemoteLogConfig(no_remote_log_file=no_remote_log_file))
    response = ctx.perform_request(HEADERS_WITH_ESC)
    time.sleep(2)
    asserts.status(response, 200)
    assert ctx.storage.state.requests.empty()
    ctx.manager.fs.remove(no_remote_log_file)
    time.sleep(2)
    response = ctx.perform_request(HEADERS_WITH_ESC)
    asserts.status(response, 200)
    req = ctx.storage.state.get_request()
    value = get_value(False, req.data.content)
    assert value[u'status'] == 200


def test_many_requests(ctx):
    """
    Проверяет корректность записи в лог
    """
    ctx.start_backend(ChunkedConfig(), name='storage')
    ctx.start_backend(ChunkedConfig())
    ctx.start_balancer(RemoteLogConfig())
    count = 4
    for _ in xrange(count):
        response = ctx.perform_request(HEADERS_WITH_ESC)
        asserts.status(response, 200)
        req = ctx.storage.state.get_request()
        value = get_value(False, req.data.content)
        assert value[u'method'] == u'GET'
        assert value[u'request'] == u'/'
        assert value[u'protocol'] == u'HTTP/1.1'
        assert value[u'status'] == 200
        assert [u'host', u's"""s'] in value[u'headers']
        assert u'timestamp' in value
        assert u'uaas_mode' not in value


def test_log_backend_not_work(ctx):
    """
    Проверяет работу балансера без логирующего беканда
    """
    ctx.start_fake_backend(name='storage')
    ctx.start_backend(ChunkedConfig())
    ctx.start_balancer(RemoteLogConfig())
    response = ctx.perform_request(HEADERS_WITH_ESC)
    asserts.status(response, 200)


def test_log_backend_ugly(ctx):
    """
    Проверяет ответ если ответ log-бекенда запаздывает
    """
    log_backend_resp = http.response.ok(data=['lost', 'highway'])
    ctx.start_backend(ChunkedConfig(chunk_timeout=10, response=log_backend_resp), name='storage')
    ctx.start_backend(ChunkedConfig())

    ctx.start_balancer(RemoteLogConfig())
    response = ctx.perform_request(HEADERS_WITH_ESC)
    asserts.status(response, 200)


def test_backend_not_work(ctx):
    """
    Проверяет запись в лог без работающего бакенда
    """
    ctx.start_backend(ChunkedConfig(), name='storage')
    ctx.start_fake_backend()

    ctx.start_balancer(RemoteLogConfig())
    ctx.perform_request_xfail(HEADERS_WITH_ESC)
    req = ctx.storage.state.get_request()
    value = get_value(False, req.data.content)
    assert value[u'protocol'] == u'HTTP/1.1'
    assert u'status' not in value


def test_backend_ugly(ctx):
    """
    Проверяет запись в лог для бекенда неуспевшего все отправить, в логе должен отсутствовать status
    """
    backend_resp = http.response.ok(data=['lost', 'highway'])
    ctx.start_backend(ChunkedConfig(), name='storage')
    ctx.start_backend(ChunkedConfig(chunk_timeout=10, response=backend_resp))

    ctx.start_balancer(RemoteLogConfig())
    ctx.perform_request_xfail(HEADERS_WITH_ESC)
    req = ctx.storage.state.get_request()
    value = get_value(False, req.data.content)
    assert u'status' not in value
    assert value[u'protocol'] == u'HTTP/1.1'


def test_backend_bad(ctx):
    """
    Проверяет запись в лог для бекенда отправившего мусор, в логе должна быть ошибка бекенда
    """
    ctx.start_backend(ChunkedConfig(), name='storage')
    ctx.start_backend(BrokenConfig())

    ctx.start_balancer(RemoteLogConfig())
    ctx.perform_request_xfail(HEADERS_WITH_ESC)
    req = ctx.storage.state.get_request()
    value = get_value(False, req.data.content)
    assert value[u'status'] == 400
    assert value[u'protocol'] == u'HTTP/1.1'


def test_100_continue(ctx):
    """
    Клиент отправляет строку запроса и заголовком Expect: 100-continue
    backend отвечает 100 Continue
    клиент отправляет тело запроса
    backend отправляет ответ на запрос

    Балансер должен правильно передавать все данные в обе стороны
    в лог должен писаться только один запрос со статусом 200
    """
    data = 'A' * 20
    ctx.start_backend(ChunkedConfig(), name='storage')
    ctx.start_backend(ContinueConfig(
        continue_response=http.response.some(status=100, reason='Continue', data=None),
        response=http.response.ok(data=data)))
    ctx.start_balancer(RemoteLogConfig())

    request = http.request.get(headers={'Expect': '100-continue'}, data=['12345']).to_raw_request()
    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write_request_line(request.request_line)
        stream.write_headers(request.headers)
        resp1 = stream.read_next_response()
        logging.info(str(resp1))
        stream.write_data(request.data)
        resp2 = stream.read_next_response()
        logging.info(str(resp2))

    asserts.status(resp1, 100)
    asserts.status(resp2, 200)
    req = ctx.storage.state.get_request()
    logging.info(str(req))
    value = get_value(False, req.data.content)
    assert ctx.storage.state.requests.empty()
    assert value[u'protocol'] == u'HTTP/1.1'
    assert value[u'status'] == 200


def test_non_utf_header_value(ctx):
    """
    BALANCER-1067
    Balancer must send header with non-utf value to backend
    """
    ctx.start_backend(ChunkedConfig(), name='storage')
    ctx.start_backend(ChunkedConfig())

    ctx.start_balancer(RemoteLogConfig())
    response = ctx.perform_request(http.request.raw_get(headers={
        'Non-Utf': '\xe9\xe3',
        'Content-Length': 0,
    }))
    assert ctx.balancer.is_alive()
    asserts.status(response, 200)


def test_queue_limit_exceeded(ctx):
    """
    BALANCER-1619
    remote_log должен дропать новые данные, если превышен размер данных в очереди на отправку.
    """
    ctx.start_backend(ChunkedConfig(), name='storage')
    ctx.start_backend(ChunkedConfig())
    ctx.start_balancer(RemoteLogConfig(queue_limit=0))
    response = ctx.perform_request(HEADERS_WITH_ESC)
    asserts.status(response, 200)

    time.sleep(10)

    unistat = ctx.get_unistat()
    assert unistat['remote_log-in_queue_ammv'] == 0
    assert unistat['remote_log-dropped_ammv'] > 0


def test_header_whitelist(ctx):
    """
    USEREXP-6865
    remote_log должен логировать только самые необходимые заголовки
    """
    ctx.start_backend(ChunkedConfig(), name='storage')
    ctx.start_backend(ChunkedConfig())

    ctx.start_balancer(RemoteLogConfig())
    response = ctx.perform_request(HEADERS_WITH_ESC)
    asserts.status(response, 200)
    req = ctx.storage.state.get_request()
    value = get_value(False, req.data.content)
    headers = dict(value[u'headers'])

    junk_headers = ['For-delete-1', 'For-delete-2', 'For-delete', 'For-delete  ', 'for-Delete',
                    'XXX-Forwarded-For-YYY', 'Led', 'Pink']
    for header in junk_headers:
        assert header not in headers

    assert headers['X-Forwarded-For-Y'] == '321.333.4.3'
    assert headers['uSer-aGeNt'] == 'dwew\trfer'
    assert headers['host'] == 's"""s'
    assert headers[u'X-Yandex-Internal-Request'] == '1'
    assert headers[u'X-Yandex-Suspected-Robot'] == '0'
    assert 'X-Yandex-ExpFlags-Pre' in headers
    assert 'X-Yandex-ExpFlags' in headers


def test_flags_compactification(ctx):
    """
    USEREXP-6865
    remote_log должен заменять флаги на "#<len>", где len - длина флага
    """
    ctx.start_backend(ChunkedConfig(), name='storage')
    ctx.start_backend(ChunkedConfig())

    ctx.start_balancer(RemoteLogConfig())
    response = ctx.perform_request(HEADERS_WITH_ESC)
    asserts.status(response, 200)
    req = ctx.storage.state.get_request()
    value = get_value(False, req.data.content)
    headers = dict(value[u'headers'])

    assert headers['X-Yandex-ExpFlags-Pre'] == '#42'
    assert headers['X-Yandex-ExpFlags'] == '#30000'


def test_throttled_mode(ctx):
    ctx.start_backend(ChunkedConfig(), name='storage')
    ctx.start_backend(ChunkedConfig())
    ctx.start_balancer(RemoteLogWithDelayConfig(delay='5s'))

    for x in range(0, 15):
        response = ctx.perform_request(HEADERS_WITH_ESC)
    asserts.status(response, 200)

    assert ctx.storage.state.requests.qsize() < 3


def test_throttled_mode_getsheaders(ctx):
    ctx.start_backend(ChunkedConfig(), name='storage')
    ctx.start_backend(ChunkedConfig())
    ctx.start_balancer(RemoteLogWithDelayConfig(delay='5s'))

    for x in range(0, 15):
        response = ctx.perform_request(HEADERS_WITH_ESC)

    asserts.status(response, 200)
    assert ctx.storage.state.requests.qsize() < 3

    req = ctx.storage.state.get_request()
    value = get_value(False, req.data.content)
    headers = dict(value[u'headers'])

    junk_headers = ['For-delete-1', 'For-delete-2', 'For-delete', 'For-delete  ', 'for-Delete',
                    'XXX-Forwarded-For-YYY', 'Led', 'Pink']
    for header in junk_headers:
        assert header not in headers

    assert headers['X-Forwarded-For-Y'] == '321.333.4.3'
    assert headers['X-Yandex-TCP-Info'] == 'v=2; rtt=0.083766s; rttvar=0.002507s; snd_cwnd=10; total_retrans=0:'
