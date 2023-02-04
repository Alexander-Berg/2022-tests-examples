# -*- coding: utf-8 -*-
import pytest
import time
import socket

from configs import ModBalancerConfig, ModBalancer2OnErrorConfig, TwoBackendsConfig

from balancer.test.util.sync import Counter
from balancer.test.util.proto.handler.server.http import HTTPConfig, HTTPServerHandler
from balancer.test.util.proto.http.stream import HTTPReaderException
from balancer.test.util.predef.handler.server.http import SimpleConfig, BrokenConfig, DummyConfig, \
    SimpleDelayedConfig, PreparseHandler, State
from balancer.test.util.predef import http
from balancer.test.util import asserts
from balancer.test.util.sanitizers import sanitizers


class NoParseCloseHandler(HTTPServerHandler):
    def handle_request(self, stream):
        time.sleep(self.config.timeout)
        self.force_close()


class NoParseCloseConfig(HTTPConfig):
    HANDLER_TYPE = NoParseCloseHandler

    def __init__(self, timeout):
        super(NoParseCloseConfig, self).__init__()
        self.timeout = timeout


def base_buffering_input_test(ctx, balancer_config):
    chunk = 'A' * 10000
    num_chunks = 10000

    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(balancer_config)

    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write_line('POST / HTTP/1.1')
        stream.write_header('Transfer-Encoding', 'chunked')
        stream.end_headers()
        for _ in xrange(num_chunks):
            stream.write_chunk(chunk)
        time.sleep(1)
        mem = ctx.balancer.get_master_memory_usage()
        stream.write_chunk('')
        resp = stream.read_response()

    asserts.status(resp, 200)
    return mem


def test_attempts_buffering_input(ctx):
    """
    BALANCER-116
    Если attempts > 1, то балансер должен буферизовать запрос клиента, пока backend не начал отвечать
    """
    mem = base_buffering_input_test(ctx, ModBalancerConfig(attempts=2, rewind_limit=10 ** 8))
    assert mem > 10 ** 8


def test_on_error_buffering_input(ctx):
    """
    BALANCER-116
    Если присутствует секция on_error,
    то балансер должен буферизовать запрос клиента, пока backend не начал отвечать
    """
    mem = base_buffering_input_test(ctx, ModBalancer2OnErrorConfig(attempts=1, backend_timeout='100500s', rewind_limit=10 ** 8))
    assert mem > 10 ** 8


def test_disable_on_error_buffering_input(ctx):
    """
    BALANCER-116
    Балансер должен буферизовать запрос клиента при наличии секции on_error,
    даже если эта секция выключена event-ом
    """
    chunk = 'A' * 10000
    num_chunks = 10000

    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ModBalancer2OnErrorConfig(attempts=1, backend_timeout='100500s', rewind_limit=10 ** 8))
    ctx.perform_request(http.request.get('/admin/events/call/disable'), port=ctx.balancer.config.admin_port)

    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write_line('POST / HTTP/1.1')
        stream.write_header('Transfer-Encoding', 'chunked')
        stream.end_headers()
        for _ in xrange(num_chunks):
            stream.write_chunk(chunk)
        time.sleep(1)
        mem = ctx.balancer.get_master_memory_usage()
        stream.write_chunk('')
        resp = stream.read_response()

    asserts.status(resp, 200)
    assert mem > 10 ** 8


def test_no_buffering_input(ctx):
    """
    BALANCER-116
    Если attempts = 1 и отсутствует секция on_error,
    то балансер не должен буферизовать запрос клиента
    """
    limit = 10 ** 8
    mem = base_buffering_input_test(ctx, ModBalancerConfig(attempts=1, rewind_limit=limit))

    if sanitizers.asan_enabled():
        limit = 5 * limit
    assert mem < limit


def test_exceed_rewind_limit_memory_usage(ctx):
    """
    Если длина запроса превышает rewind_limit, то балансер должен перестать сохранять его в памяти
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ModBalancer2OnErrorConfig(attempts=2, backend_timeout=50, rewind_limit=1024))
    request = http.request.get(data='A' * 31 * 1024 * 1024).to_raw_request()
    with ctx.create_http_connection(timeout=20) as conn:
        stream = conn.create_stream()
        stream.write_request_line(request.request_line)
        stream.write_headers(request.headers)
        stream.write(request.data.content[:-1])
        time.sleep(2)
        assert ctx.balancer.get_master_memory_usage() < 500 * 1024 * 1024
        stream.write(request.data.content[-1:])
        response = stream.read_response()

    asserts.status(response, 200)


def test_exceed_rewind_limit_no_attempts(ctx):
    """
    Если длина запроса первышает rewind_limit и бэкенд не ответил с первой попытки,
    то балансер не должен перезадавать запрос еще раз
    """
    ctx.start_backend(BrokenConfig())
    ctx.start_balancer(ModBalancer2OnErrorConfig(attempts=2, rewind_limit=1024))
    ctx.perform_request_xfail(http.request.get(data='A' * 1025))
    assert ctx.backend.state.requests.qsize() == 1


def test_exceed_rewind_limit_no_on_error(ctx):
    """
    Если длина запроса превышает rewind_limit при последней попытке запроса к бэкенду и если бэкенд не отвечает,
    то надо закрыть соединение с клиентом, а не отдавать ответ из on_error
    """
    ctx.start_backend(NoParseCloseConfig(timeout=3))
    ctx.start_balancer(ModBalancer2OnErrorConfig(attempts=2, backend_timeout=3.5, rewind_limit=1024))
    request = http.request.get(data='A' * 1025).to_raw_request()
    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write_request_line(request.request_line)
        stream.write_headers(request.headers)
        stream.write(request.data.content[:1023])
        time.sleep(3)
        stream.write(request.data.content[1023:])
        with pytest.raises(HTTPReaderException):
            stream.read_response()


def test_fit_rewind_limit(ctx):
    """
    Если длина запроса не превышает rewind_limit,
    то балансер должен перезадать запрос бэкенду
    и после оконачания попыток отдать ответ из on_error
    """
    data = 'A' * 1023
    ctx.start_backend(BrokenConfig())
    ctx.start_balancer(ModBalancer2OnErrorConfig(attempts=2, rewind_limit=1024))
    response = ctx.perform_request(http.request.get(data=data))
    assert ctx.backend.state.requests.qsize() == 2
    req1 = ctx.backend.state.get_request()
    req2 = ctx.backend.state.get_request()

    asserts.content(req1, data)
    asserts.content(req2, data)
    asserts.content(response, 'on_error')


def base_attempts_file_value_test(ctx, attempts_value, attempts_default_value, expected_value):
    attempts_file = ctx.manager.fs.create_file('attempts_file.txt')
    ctx.manager.fs.rewrite(attempts_file, str(attempts_value))
    backend = ctx.start_backend(BrokenConfig())
    ctx.start_balancer(ModBalancer2OnErrorConfig(attempts=attempts_default_value, attempts_file=attempts_file))
    ctx.perform_request(http.request.get())
    backend.state.accepted.reset()
    time.sleep(1.1)
    ctx.perform_request(http.request.get())
    assert backend.state.accepted.value == expected_value
    return backend


@pytest.mark.parametrize('value', [2, 5], ids=['lesser', 'greater'])
def test_attempts_file_ok_value(ctx, value):
    """
    Если в attempts_file указано целое число, большее нуля,
    то оно должно использоваться в качестве нового значения количества попыток
    """
    base_attempts_file_value_test(ctx, value, 3, value)


@pytest.mark.parametrize('value', [0, -42, 2**65, ':)'], ids=['zero', 'negative', 'huge', 'upyachka'])
def test_attempts_file_bad_value(ctx, value):
    """
    Если в attempts_file указано не целое число, большее нуля,
    то оно должно использоваться количество попыток, указанное в конфиге
    """
    base_attempts_file_value_test(ctx, value, 3, 3)


def base_new_value_test(ctx, attempts_value, attempts_default_value, expected_value, new_value):
    backend = base_attempts_file_value_test(ctx, attempts_value, attempts_default_value, expected_value)
    backend.state.accepted.reset()
    ctx.manager.fs.rewrite(ctx.balancer.config.attempts_file, str(new_value))
    time.sleep(1.1)
    ctx.perform_request(http.request.get())
    assert backend.state.accepted.value == new_value


def test_attempts_file_timeout(ctx):
    """
    attempts_file должен перечитываться раз в секунду
    """
    base_new_value_test(ctx, 2, 3, 2, 5)


def test_attempts_file_restore_value(ctx):
    """
    Если значение в attempts_file сменилось с невалидного на валидное,
    то балансер должен использовать новое значение вместо дефолтного
    """
    base_new_value_test(ctx, ':(', 3, 3, 5)


def base_attempts_file_deleted_test(ctx):
    default_value = 3
    backend = base_attempts_file_value_test(ctx, 2, default_value, 2)
    backend.state.accepted.reset()
    ctx.manager.fs.remove(ctx.balancer.config.attempts_file)
    time.sleep(1.1)
    ctx.perform_request(http.request.get())
    assert backend.state.accepted.value == default_value
    return backend


def test_attempts_file_deleted(ctx):
    """
    Если attempts_file был удален, то балансер должен использовать значение из конфига
    """
    base_attempts_file_deleted_test(ctx)


def test_attempts_file_restore_file(ctx):
    """
    Если attempts_file был удален а потом восстановлен с корректным значением,
    то балансер должен использовать это значение
    """
    restored = 5
    backend = base_attempts_file_deleted_test(ctx)
    backend.state.accepted.reset()
    ctx.manager.fs.rewrite(ctx.balancer.config.attempts_file, str(restored))
    time.sleep(1.1)
    ctx.perform_request(http.request.get())
    assert backend.state.accepted.value == restored


def test_attempts_file_read_during_request(ctx):
    """
    Если балансер перечитывает attempts_file во время запроса,
    то количество попыток для этого запроса должно остаться прежним
    """
    attempts_value = 10
    attempts_file = ctx.manager.fs.create_file('attempts_file.txt')
    backend = ctx.start_backend(DummyConfig(timeout=1))
    ctx.start_balancer(ModBalancer2OnErrorConfig(
        attempts=1,
        attempts_file=attempts_file,
        backend_timeout=0.5))
    time.sleep(1)  # file modification time is stored with seconds precision
    ctx.perform_request(http.request.get())
    backend.state.accepted.reset()
    ctx.manager.fs.rewrite(attempts_file, str(attempts_value))
    time.sleep(1.1)
    stream = ctx.create_http_connection().create_stream()
    stream.write_request(http.request.raw_get())
    ctx.manager.fs.rewrite(attempts_file, '1')
    stream.read_response()
    assert backend.state.accepted.value == attempts_value


def test_client_half_closed(ctx):
    """
    BALANCER-513
    Если клиент после отправки запроса закрыл соединение на запись, но не закрыл на чтение,
    то балансер должен вернуть ему ответ бэкенда
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ModBalancerConfig())
    conn = ctx.create_http_connection()
    stream = conn.create_stream()
    stream.write_request(http.request.raw_get())
    conn.sock.shutdown(socket.SHUT_WR)
    response = stream.read_response()
    asserts.status(response, 200)


def test_dns_failed_on_error(ctx):
    """
    BALANCER-570
    Если балансер не может отрезолвить бэкенд, то запросы должны попадать в секцию on_error.
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ModBalancer2OnErrorConfig(backend_host='invalid'))
    response = ctx.perform_request(http.request.get())
    asserts.content(response, 'on_error')


def test_many_attempts(ctx):
    """
    SEPE-8029
    Если в модуле balancer неразумно большое число attempts,
    то балансер не должен упасть при запросе
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ModBalancerConfig(attempts=100500))

    response = ctx.perform_request(http.request.get())

    assert ctx.balancer.is_alive()
    asserts.status(response, 200)


def start_all_timeout(ctx, attempts, backend_timeout, backend_delay, backend_data=None):
    if backend_data is None:
        backend_data = ['answer']

    ctx.start_backend(SimpleDelayedConfig(response=http.response.ok(data=backend_data), response_delay=backend_delay))
    ctx.start_balancer(ModBalancerConfig(backend_timeout=backend_timeout, attempts=attempts))


def test_backend_faster_than_timeout(ctx):
    """
    BALANCER-441
    Бэкэнд отвечает быстрее, чем таймаут proxy, от балансера к нему
    должен быть один запрос
    """
    start_all_timeout(ctx, 3, '1s', 0)
    response = ctx.perform_request(http.request.get())
    asserts.status(response, 200)
    assert ctx.backend.state.accepted.value == 1


def test_backend_slow_with_enough_timeout(ctx):
    """
    BALANCER-441
    Бэкэнд отвечает медленнее, чем таймаут proxy, от балансера к нему
    должно быть задано attempts запросов, потому что timeout в
    balancer больше суммы времён на все попытки
    """
    start_all_timeout(ctx, 3, '1s', 1.5)
    ctx.perform_request_xfail(http.request.get())
    assert ctx.backend.state.accepted.value == 3


class HeadersTimeoutHandler(PreparseHandler):
    def handle_parsed_request(self, raw_request, stream):
        stream.write_line('HTTP/1.1 200 OK')
        stream.write_header('Led', 'Zeppelin')
        time.sleep(self.config.timeout)
        self.force_close()


class HeadersTimeoutConfig(HTTPConfig):
    HANDLER_TYPE = HeadersTimeoutHandler

    def __init__(self, timeout):
        super(HeadersTimeoutConfig, self).__init__()
        self.timeout = timeout


def test_headers_timeout_on_error(ctx):
    """
    Если backend таймаутится во время передачи заголовков,
    то надо вернуть ответ из on_error
    """
    timeout = 3
    ctx.start_backend(HeadersTimeoutConfig(timeout))
    ctx.start_balancer(ModBalancer2OnErrorConfig(backend_timeout=timeout - 2))

    response = ctx.perform_request(http.request.get())

    asserts.status(response, 404)
    asserts.content(response, 'on_error')


class FailFirstHandler(HTTPServerHandler):
    def handle_request(self, stream):
        counter = self.state.counter.inc()
        if counter == 0:
            self.handle_first(stream)
        else:
            self.handle_other(stream)

    def handle_first(self, stream):
        raise NotImplementedError()

    def handle_other(self, stream):
        raise NotImplementedError()


class FailFirstState(State):
    def __init__(self, config, counter):
        super(FailFirstState, self).__init__(config)
        self.counter = counter


class FailFirstConfig(HTTPConfig):
    HANDLER_TYPE = FailFirstHandler
    STATE_TYPE = FailFirstState


class First5xxHandler(FailFirstHandler):
    def handle_first(self, stream):
        self.append_request(stream.read_request())
        stream.write_response(http.response.service_unavailable().to_raw_response())

    def handle_other(self, stream):
        self.append_request(stream.read_request())
        stream.write_response(http.response.ok().to_raw_response())


class First5xxConfig(FailFirstConfig):
    HANDLER_TYPE = First5xxHandler


class FirstCloseHandler(FailFirstHandler):
    def handle_first(self, stream):
        stream.read_request_line()
        stream.read_headers()
        stream.read_chunk()
        self.force_close()

    def handle_other(self, stream):
        self.append_request(stream.read_request())
        stream.write_response(http.response.ok().to_raw_response())


class FirstCloseConfig(FailFirstConfig):
    HANDLER_TYPE = FirstCloseHandler


def test_503_rerequest(ctx):
    """
    Условия: один из бэкендов отвечает на запрос 503
    Поведение: балансер должен перезадать запрос на другой бэкенд
    """
    counter = Counter(0)
    config1 = First5xxConfig()
    config2 = First5xxConfig()
    ctx.start_backend(config1, state=FailFirstState(config1, counter), name='backend1')
    ctx.start_backend(config2, state=FailFirstState(config2, counter), name='backend2')

    ctx.start_balancer(TwoBackendsConfig(fail_on_5xx=1))

    response = ctx.perform_request(http.request.get())
    asserts.status(response, 200)

    assert ctx.backend1.state.requests.qsize() == 1
    assert ctx.backend2.state.requests.qsize() == 1


def test_no_duplicate_headers(ctx):
    """
    SEPE-5566
    Конфиг с двумя backend-ами, attempts = 2
    Балансер задает клиентский запрос с заголовками Connection и Content-Length
    на первый backend, и backend возвращает 500
    Балансер должен перезадать запрос на второй backend,
    заголовки Connection и Content-Length должны встречаться по одному разу
    """
    counter = Counter(0)
    config1 = First5xxConfig()
    config2 = First5xxConfig()
    ctx.start_backend(config1, state=FailFirstState(config1, counter), name='backend1')
    ctx.start_backend(config2, state=FailFirstState(config2, counter), name='backend2')
    ctx.start_balancer(TwoBackendsConfig(fail_on_5xx=1))

    response = ctx.perform_request(http.request.raw_post(
        headers={'connection': 'close', 'content-length': 10}, data='A' * 10))
    asserts.status(response, 200)

    req1 = ctx.backend1.state.get_raw_request()
    req2 = ctx.backend2.state.get_raw_request()
    asserts.single_header(req1, 'connection')
    asserts.single_header(req1, 'content-length')
    asserts.single_header(req2, 'connection')
    asserts.single_header(req2, 'content-length')


def test_post_broken_backend(ctx):
    """
    SEPE-5646
    В конфиге балансера указано attempts = 2
    На балансер приходит POST-запрос
    Пока балансер пересылает запрос, backend разрывает соединение
    Во второй попытке балансер должен переслать тело запроса полностью
    """
    counter = Counter(0)
    config1 = FirstCloseConfig()
    config2 = FirstCloseConfig()
    ctx.start_backend(config1, state=FailFirstState(config1, counter), name='backend1')
    ctx.start_backend(config2, state=FailFirstState(config2, counter), name='backend2')
    ctx.start_balancer(TwoBackendsConfig())

    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write_line('POST / HTTP/1.1')
        stream.write_header('Transfer-Encoding', 'chunked')
        stream.end_headers()
        stream.write_chunk('A' * 10)
        time.sleep(1)
        stream.write_chunk('B' * 10)
        stream.write_chunk('')
        response = stream.read_response()
    asserts.status(response, 200)

    assert (ctx.backend1.state.requests.qsize() == 1) != (ctx.backend2.state.requests.qsize() == 1)  # xor
    if ctx.backend1.state.requests.qsize() == 1:
        req = ctx.backend1.state.get_request()
    else:
        req = ctx.backend2.state.get_request()
    asserts.content(req, 'A' * 10 + 'B' * 10)
