# -*- coding: utf-8 -*-
import pytest
import time
import datetime
import re

from configs import ModBalancerDefaultConfig, ModBalancerFailConfig, ModBalancerDelayConfig, \
    ModBalancerErrorConfig, ModBalancerHedgedConfig, ModBalancerTwoLevelConfig, ModBalancerTwoLevelReturnLast5xxConfig, \
    ModBalancerRrWeightsFile, ModBalancerRendezvousHashingWeighsFile

from balancer.test.util.sync import Counter
from balancer.test.util.proto.http.stream import HTTPReaderException
from balancer.test.util.predef.handler.server.http import SimpleConfig, SimpleDelayedConfig, \
    ThreeModeConfig, DummyConfig, TwoModeHandler, TwoModeConfig, State
from balancer.test.util.predef import http
from balancer.test.util import asserts
from balancer.test.util.sanitizers import sanitizers
from balancer.test.util.stdlib.multirun import Multirun


class FirstCloseHandler(TwoModeHandler):
    def handle_first(self, stream):
        stream.read_request_line()
        stream.read_headers()
        stream.read_chunk()
        if self.config.write_partial:
            stream.write_line("HTTP/1.1 200 OK")
            stream.write_header('Content-Length', '10')
            stream.end_headers()
            stream.write_line("A")
        self.force_close()

    def handle_second(self, stream):
        self.append_request(stream.read_request())
        stream.write_response(http.response.ok().to_raw_response())


class FirstCloseState(State):
    def __init__(self, config, counter):
        super(FirstCloseState, self).__init__(config)
        self.counter = counter


class FirstCloseConfig(TwoModeConfig):
    HANDLER_TYPE = FirstCloseHandler
    STATE_TYPE = FirstCloseState

    def __init__(self, write_partial=False):
        super(FirstCloseConfig, self).__init__(prefix=1)
        self.write_partial = write_partial


def test_default_rewind_limit_30_megabytes(ctx):
    """
    BALANCER-1480:
    На текущий момент(10.07.20) бинарник с дефолтным конфигом потребляет ~57МБ.
    Делаем запрос на 100МБ и ожидаем, что он не превысит лимит в 30МБ. Лимит
    в 100МБ с запасом на будущее.
    """
    limit = 100 * (1 << 20)
    if sanitizers.san_enabled():
        limit = limit * 10
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ModBalancerDefaultConfig())
    request = http.request.get(data='A' * 100 * (1 << 20)).to_raw_request()
    with ctx.create_http_connection(timeout=20) as conn:
        stream = conn.create_stream()
        stream.write_request_line(request.request_line)
        stream.write_headers(request.headers)
        stream.write(request.data.content[:-1])
        time.sleep(2)
        assert ctx.balancer.get_master_memory_usage() < limit
        stream.write(request.data.content[-1:])
        response = stream.read_response()

    asserts.status(response, 200)


def test_default_rewind_limit_fail_idempotent(ctx):
    """
    BALANCER-1480:
    Отправляем идемпотентный GET и ждем перезапроса на второй бекенд
    """
    ctx.start_fake_backend(name='backend1')
    ctx.start_backend(SimpleConfig(response=http.response.ok(data="ok")), name='backend2')
    ctx.start_balancer(
        ModBalancerFailConfig(
            attempts=2,
            connection_attempts=1,
            retry_non_idempotent=False))
    request = http.request.get(path='/idempotent', data='A' * 100).to_raw_request()
    with ctx.create_http_connection(timeout=20) as conn:
        response = conn.perform_request(request)
        asserts.status(response, 200)
    assert ctx.backend2.state.accepted.value == 1


def test_default_rewind_limit_fail_nonidempotent_read_partial(ctx):
    """
    BALANCER-1480:
    Отправляем POST, бекэнд читает только первый чанк и дальше ретраимся.
    """
    counter = Counter(0)
    config1 = FirstCloseConfig(write_partial=False)
    config2 = FirstCloseConfig()
    ctx.start_backend(config1, state=FirstCloseState(config1, counter), name='backend1')
    ctx.start_backend(config2, state=FirstCloseState(config2, counter), name='backend2')
    ctx.start_balancer(
        ModBalancerFailConfig(
            attempts=2,
            connection_attempts=1,
            retry_non_idempotent=False))

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


def test_default_rewind_limit_fail_nonidempotent_written(ctx):
    """
    BALANCER-1480:
    Отправляем POST, но бекэнд отдает не полные данные. Надо бы тут сломаться и
    не перезапрашивать.
    """
    counter = Counter(0)
    config1 = FirstCloseConfig(write_partial=True)
    config2 = FirstCloseConfig()
    ctx.start_backend(config1, state=FirstCloseState(config1, counter), name='backend1')
    ctx.start_backend(config2, state=FirstCloseState(config2, counter), name='backend2')
    ctx.start_balancer(
        ModBalancerFailConfig(
            attempts=2,
            connection_attempts=1,
            retry_non_idempotent=False))

    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write_line('POST / HTTP/1.1')
        stream.write_header('Transfer-Encoding', 'chunked')
        stream.end_headers()
        stream.write_chunk('A' * 10)
        time.sleep(1)
        stream.write_chunk('B' * 10)
        stream.write_chunk('')
        with pytest.raises(HTTPReaderException):
            stream.read_response()


def test_default_rewind_limit_fail_nonidempotent_backward(ctx):
    """
    BALANCER-1480:
    Отправляем неидемпотентный POST и ждем ответа, проверяя прежнее поведение
    """
    ctx.start_fake_backend(name='backend1')
    ctx.start_backend(SimpleConfig(), name='backend2')
    ctx.start_balancer(
        ModBalancerFailConfig(
            attempts=2,
            connection_attempts=1,
            retry_non_idempotent=True))
    time.sleep(2)
    request = http.request.post(path='/non_idempotent', data='A' * 100).to_raw_request()
    with ctx.create_http_connection(timeout=20) as conn:
        response = conn.perform_request(request)
        asserts.status(response, 200)
    assert ctx.backend2.state.accepted.value == 1


def queue_to_array(queue):
    result = []
    while queue.qsize() > 0:
        result.append(queue.get())
    return result


@pytest.mark.parametrize('delay_multiplier', [1, 1.5])
@pytest.mark.parametrize('delay_on_fast', [False, True])
@pytest.mark.parametrize(['attempts', 'fast_attempts'], [(5, 0), (1, 5), (3, 3)])
def test_exponential_delay(ctx, delay_multiplier, delay_on_fast, attempts, fast_attempts):
    """
    BALANCER-2129:
    Отправляем несколько раз запрос на мертвый бэкенд,
    проверям, что задержка экспоненциально растет
    """

    first_delay_seconds = 0.5

    ctx.start_backend(ThreeModeConfig(0, fast_attempts, attempts, response=http.response.service_unavailable()))
    ctx.start_balancer(
        ModBalancerDelayConfig(
            attempts=attempts,
            fast_attempts=fast_attempts,
            first_delay=first_delay_seconds,
            delay_multiplier=delay_multiplier,
            delay_on_fast=delay_on_fast,
            max_random_delay='0s',
            backend_timeout='1s',
            fast_503=True,
        ))
    ctx.perform_request_xfail(http.request.get())

    times = queue_to_array(ctx.backend.state.requests)
    expected = first_delay_seconds

    for i in range(1, len(times)):
        current = times[i].start_time - times[i - 1].start_time

        need_delay = delay_on_fast or i > fast_attempts
        if need_delay:
            assert current >= datetime.timedelta(seconds=expected * 0.99)
            expected *= delay_multiplier


def test_randomized_delay(ctx):
    """
    BALANCER-2129:
    Отправляем несколько запросов, проверяем, что они
    достаточно равномерно распределены по задержкам
    """
    n_blocks = 10
    attempts = 500
    # fail probability < (n_blocks - 1) ^ attempts / n_blocks ^ (attempts - 1)

    max_delay_seconds = 0.1
    max_random_delay = str(max_delay_seconds) + 's'

    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(
        ModBalancerDelayConfig(
            attempts=1,
            fast_attempts=0,
            first_delay='0s',
            delay_multiplier=1,
            delay_on_fast=True,
            max_random_delay=max_random_delay,
            backend_timeout='1s',
            fast_503=True,
        ))
    request = http.request.get()

    balancer_times = []

    for _ in xrange(attempts):
        balancer_times.append(datetime.datetime.now())
        ctx.perform_request(request)

    time.sleep(1)

    requests = queue_to_array(ctx.backend.state.requests)

    backend_times = []
    for request in requests:
        backend_times.append(request.start_time)

    delays = []
    for i in range(len(requests)):
        delays.append((backend_times[i] - balancer_times[i]).total_seconds())

    satisfied_blocks = [False] * n_blocks
    for delay in delays:
        current_block = int((delay / max_delay_seconds) * n_blocks)
        satisfied_blocks[current_block % n_blocks] = True

    assert False not in satisfied_blocks, 'Delays distribution is not uniform enough'


@pytest.mark.parametrize(
    ['response_status', 'status_code_blacklist'],
    [
        (504, '5xx'),
        (504, '504'),
        (504, '5xx,404'),
        (404, '5xx,4xx'),
    ]
)
def test_status_code_blacklist(ctx, response_status, status_code_blacklist):
    ctx.start_balancer(ModBalancerErrorConfig(status_code_blacklist=status_code_blacklist, status=response_status))
    response = ctx.perform_request(http.request.get())
    asserts.status(response, 200)
    asserts.content(response, 'on_error')


@pytest.mark.parametrize(
    ['response_status', 'status_code_blacklist', 'status_code_blacklist_exceptions'],
    [
        (504, '5xx', '504'),
        (404, '5xx,4xx', '404'),
    ]
)
def test_status_code_blacklist_exceptions(ctx, response_status, status_code_blacklist, status_code_blacklist_exceptions):
    ctx.start_balancer(ModBalancerErrorConfig(status_code_blacklist=status_code_blacklist, status=response_status, status_code_blacklist_exceptions=status_code_blacklist_exceptions))
    response = ctx.perform_request(http.request.get())
    asserts.status(response, response_status)
    asserts.content(response, ':(')


@pytest.mark.parametrize(
    ['response_status', 'status_code_blacklist', 'status_code_blacklist_exceptions'],
    [
        (504, '5xx', '503'),
        (504, '5xx,404', '500'),
        (404, '5xx,4xx', '500'),
    ]
)
def test_status_code_blacklist_exceptions_miss(ctx, response_status, status_code_blacklist, status_code_blacklist_exceptions):
    ctx.start_balancer(ModBalancerErrorConfig(status_code_blacklist=status_code_blacklist, status=response_status, status_code_blacklist_exceptions=status_code_blacklist_exceptions))
    response = ctx.perform_request(http.request.get())
    asserts.status(response, 200)
    asserts.content(response, 'on_error')


@pytest.mark.parametrize(
    ['response_status', 'status_code_blacklist'],
    [
        (504, '5xx'),
        (504, '504'),
        (404, '5xx,404'),
        (404, '5xx,4xx'),
    ]
)
def test_status_code_blacklist_on_error_disabled(ctx, response_status, status_code_blacklist):
    ctx.start_balancer(ModBalancerErrorConfig(status=response_status, status_code_blacklist=status_code_blacklist, on_error_status=response_status))
    response = ctx.perform_request(http.request.get())
    asserts.status(response, response_status)
    asserts.content(response, 'on_error')


@pytest.mark.parametrize('policy', ['simple_policy', 'unique_policy', 'unique_retry_policy'])
@pytest.mark.parametrize(
    ['response_status', 'status_code_blacklist'],
    [
        (504, '5xx'),
        (504, '504'),
        (503, '503'),
        (504, '5xx,404'),
        (404, '5xx,404'),
    ]
)
@pytest.mark.parametrize('last_5xx_only', [True, False])
def test_return_last_5xx(ctx, policy, response_status, status_code_blacklist, last_5xx_only):
    ctx.start_balancer(ModBalancerErrorConfig(
        status=response_status,
        policy=policy,
        status_code_blacklist=status_code_blacklist,
        return_last_5xx=last_5xx_only,
        return_last_blacklisted_http_code=not last_5xx_only,
        fast_503=True))
    response = ctx.perform_request(http.request.get())
    if not last_5xx_only or response_status >= 500:
        asserts.status(response, response_status)
        asserts.content(response, ':(')
    else:
        asserts.status(response, 200)
        asserts.content(response, 'on_error')


def test_return_last_5xx_after_connection_timeout(ctx):
    ctx.start_backend(SimpleConfig(response=http.response.gateway_timeout()), name='backend1')
    ctx.start_backend(DummyConfig(), name='backend2')
    ctx.start_balancer(ModBalancerFailConfig(status_code_blacklist='5xx', return_last_5xx=True))
    response = ctx.perform_request(http.request.get())
    asserts.status(response, 504)


def test_fail_nonidempotent_and_return_last_5xx(ctx):
    ctx.start_backend(SimpleConfig(response=http.response.gateway_timeout()), name='backend1')
    ctx.start_backend(SimpleConfig(), name='backend2')
    ctx.start_balancer(
        ModBalancerFailConfig(
            attempts=2,
            connection_attempts=1,
            retry_non_idempotent=False,
            status_code_blacklist='5xx',
            return_last_5xx=True))
    response = ctx.perform_request(http.request.post())
    asserts.status(response, 504)


@pytest.mark.parametrize(
    ['inner_return_last_5xx', 'outer_return_last_5xx'],
    [
        (True, False),
        (False, True),
        (True, True),
    ]
)
def test_two_level_return_last_5xx(ctx, inner_return_last_5xx, outer_return_last_5xx):
    ctx.start_balancer(ModBalancerTwoLevelReturnLast5xxConfig(
        inner_status_code_blacklist='5xx',
        inner_return_last_5xx=inner_return_last_5xx,
        outer_status_code_blacklist='5xx',
        outer_return_last_5xx=outer_return_last_5xx))

    if not outer_return_last_5xx:
        # outer balancer2 fail on 5xx from inner balancer2 and return rst
        ctx.perform_request_xfail(http.request.get())
    elif not inner_return_last_5xx:
        # outer balancer2 fail on rst from inner balancer2 and return rst
        ctx.perform_request_xfail(http.request.get())
    else:
        # outer balancer2 return last 5xx from last inner balancer2
        response = ctx.perform_request(http.request.get())
        asserts.status(response, 501)


def test_two_level_return_last_5xx_fail_outer_only(ctx):
    ctx.start_balancer(ModBalancerTwoLevelReturnLast5xxConfig(
        outer_status_code_blacklist='5xx',
        outer_return_last_5xx=True))
    # outer balancer2 return last 5xx from last inner
    response = ctx.perform_request(http.request.get())
    asserts.status(response, 501)


def test_two_level_return_last_5xx_fail_inner_only(ctx):
    ctx.start_balancer(ModBalancerTwoLevelReturnLast5xxConfig(
        inner_status_code_blacklist='5xx',
        inner_return_last_5xx=True))
    # outer balancer2 return first 5xx from first inner
    response = ctx.perform_request(http.request.get())
    asserts.status(response, 500)


def test_no_valid_backends_conn_fail(ctx):
    ctx.start_fake_backend(name='backend1')
    ctx.start_fake_backend(name='backend2')
    ctx.start_balancer(ModBalancerFailConfig(connection_attempts=2, backend_count=1))

    with pytest.raises(HTTPReaderException):
        ctx.perform_request(http.request.get())

    unistat = ctx.get_unistat()
    assert unistat['report-default-backend_attempt_summ'] == 1
    assert unistat['report-default-conn_refused_summ'] == 1
    assert unistat['report-default-backend_error_summ'] == 0
    assert unistat['report-default-no_backends_error_summ'] == 1
    assert unistat['report-default-fail_summ'] == 1
    assert unistat['report-default-conn_fail_summ'] == 1
    assert unistat['report-default-backend_fail_summ'] == 1


def test_no_valid_backends_backend_fail(ctx):
    ctx.start_backend(DummyConfig(), name='backend1')
    ctx.start_fake_backend(name='backend2')
    ctx.start_balancer(ModBalancerFailConfig(attempts=2, backend_count=1))

    with pytest.raises(HTTPReaderException):
        ctx.perform_request(http.request.get())

    unistat = ctx.get_unistat()
    assert unistat['report-default-backend_attempt_summ'] == 1
    assert unistat['report-default-conn_refused_summ'] == 0
    assert unistat['report-default-backend_error_summ'] == 1
    assert unistat['report-default-no_backends_error_summ'] == 1
    assert unistat['report-default-fail_summ'] == 1
    assert unistat['report-default-conn_fail_summ'] == 0
    assert unistat['report-default-backend_fail_summ'] == 1


@pytest.mark.parametrize('use_on_error_for_non_idempotent', [True, False])
@pytest.mark.parametrize('retry_non_idempotent', [True, False])
@pytest.mark.parametrize('on_error_status', [None, 504])
def test_idempotent_backends_conn_fail(ctx, retry_non_idempotent, on_error_status, use_on_error_for_non_idempotent):
    ctx.start_fake_backend(name='backend1')
    ctx.start_fake_backend(name='backend2')
    ctx.start_balancer(ModBalancerFailConfig(
        attempts=2,
        backend_count=2,
        retry_non_idempotent=retry_non_idempotent,
        on_error_status=on_error_status,
        use_on_error_for_non_idempotent=use_on_error_for_non_idempotent
    ))

    if on_error_status is None:
        with pytest.raises(HTTPReaderException):
            ctx.perform_request(http.request.post())
        expected_failures = 1
    else:
        response = ctx.perform_request(http.request.post())
        asserts.status(response, on_error_status)
        expected_failures = 0

    unistat = ctx.get_unistat()
    assert unistat['report-default-backend_attempt_summ'] == 2
    assert unistat['report-default-conn_refused_summ'] == 2
    assert unistat['report-default-backend_error_summ'] == 0
    assert unistat['report-default-no_backends_error_summ'] == 0
    assert unistat['report-default-fail_summ'] == expected_failures
    assert unistat['report-default-conn_fail_summ'] == expected_failures
    assert unistat['report-default-backend_fail_summ'] == expected_failures


@pytest.mark.parametrize('use_on_error_for_non_idempotent', [True, False])
@pytest.mark.parametrize('retry_non_idempotent', [True, False])
@pytest.mark.parametrize('on_error_status', [None, 504])
def test_idempotent_backends_fail(ctx, retry_non_idempotent, on_error_status, use_on_error_for_non_idempotent):
    ctx.start_backend(DummyConfig(), name='backend1')
    ctx.start_backend(DummyConfig(), name='backend2')
    ctx.start_balancer(ModBalancerFailConfig(
        attempts=2,
        backend_count=2,
        retry_non_idempotent=retry_non_idempotent,
        on_error_status=on_error_status,
        use_on_error_for_non_idempotent=use_on_error_for_non_idempotent
    ))

    use_on_error = retry_non_idempotent or use_on_error_for_non_idempotent
    if on_error_status is None or not use_on_error:
        with pytest.raises(HTTPReaderException):
            ctx.perform_request(http.request.post())
        expected_failures = 1
    else:
        response = ctx.perform_request(http.request.post())
        asserts.status(response, on_error_status)
        expected_failures = 0

    if retry_non_idempotent:
        expected_attempts = 2
    else:
        expected_attempts = 1

    unistat = ctx.get_unistat()
    assert unistat['report-default-backend_attempt_summ'] == expected_attempts
    assert unistat['report-default-conn_refused_summ'] == 0
    assert unistat['report-default-backend_error_summ'] == expected_attempts
    assert unistat['report-default-no_backends_error_summ'] == 0
    assert unistat['report-default-fail_summ'] == expected_failures
    assert unistat['report-default-conn_fail_summ'] == 0
    assert unistat['report-default-backend_fail_summ'] == expected_failures


@pytest.mark.parametrize(['response_status'], [(404,), (503,)])
def test_on_status_code(ctx, response_status):
    ctx.start_balancer(ModBalancerErrorConfig(status=response_status, on_status_code=response_status, on_status_code_content="on_status_code"))
    response = ctx.perform_request(http.request.get())
    asserts.status(response, 200)
    asserts.content(response, 'on_status_code')


def test_default_rewind_limit_fail_nonidempotent_read_partial_second(ctx):
    """
    BALANCER-1480:
    Отправляем POST, бекэнд читатет только первый чанк и дальше ретраимся.
    """
    counter = Counter(0)
    config1 = FirstCloseConfig(write_partial=False)
    config2 = FirstCloseConfig()
    ctx.start_backend(config1, state=FirstCloseState(config1, counter), name='backend1')
    ctx.start_backend(config2, state=FirstCloseState(config2, counter), name='backend2')
    ctx.start_balancer(
        ModBalancerFailConfig(
            attempts=2,
            connection_attempts=1,
            not_retryable_methods='POST,PATCH'))

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


def test_default_rewind_limit_fail_idempotent_2(ctx):
    """
    BALANCER-1480:
    Отправляем идемпотентный GET и ждем перезапроса на второй бекенд
    """
    ctx.start_fake_backend(name='backend1')
    ctx.start_backend(SimpleConfig(response=http.response.ok(data="ok")), name='backend2')
    ctx.start_balancer(
        ModBalancerFailConfig(
            attempts=2,
            connection_attempts=1,
            not_retryable_methods='POST,PATCH'))
    request = http.request.get(path='/idempotent', data='A' * 100).to_raw_request()
    with ctx.create_http_connection(timeout=20) as conn:
        response = conn.perform_request(request)
        asserts.status(response, 200)
    assert ctx.backend2.state.accepted.value == 1


@pytest.mark.parametrize(
    ['req', 'not_retryable_methods'],
    [
        ('POST', 'POST,PATCH'),
        ('PATCH', 'PATCH'),
        ('PUT', 'POST,PATCH,PUT,DELETE'),
        ('DELETE', 'PUT,DELETE'),
    ]
)
def test_default_rewind_limit_fail_not_retry_methods_written(ctx, req, not_retryable_methods):
    """
    BALANCER-1480:
    Отправляем "not_retry" запрос, но бекэнд отдает не полные данные. Надо бы тут сломаться и
    не перезапрашивать.
    """
    counter = Counter(0)
    config1 = FirstCloseConfig(write_partial=True)
    config2 = FirstCloseConfig()
    ctx.start_backend(config1, state=FirstCloseState(config1, counter), name='backend1')
    ctx.start_backend(config2, state=FirstCloseState(config2, counter), name='backend2')
    ctx.start_balancer(
        ModBalancerFailConfig(
            attempts=2,
            connection_attempts=1,
            not_retryable_methods=not_retryable_methods))

    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write_line(req + ' / HTTP/1.1')
        stream.write_header('Transfer-Encoding', 'chunked')
        stream.end_headers()
        stream.write_chunk('A' * 10)
        time.sleep(1)
        stream.write_chunk('B' * 10)
        stream.write_chunk('')
        with pytest.raises(HTTPReaderException):
            stream.read_response()


def test_default_rewind_limit_fail_nonidempotent_backward_2(ctx):
    """
    BALANCER-1480:
    Отправляем неидемпотентный POST и ждем ответа, проверяя прежнее поведение
    """
    ctx.start_fake_backend(name='backend1')
    ctx.start_backend(SimpleConfig(), name='backend2')
    ctx.start_balancer(
        ModBalancerFailConfig(
            attempts=2,
            connection_attempts=1,
            not_retryable_methods='POST,PATCH'))
    time.sleep(2)
    request = http.request.post(path='/non_idempotent', data='A' * 100).to_raw_request()
    with ctx.create_http_connection(timeout=20) as conn:
        response = conn.perform_request(request)
        asserts.status(response, 200)
    assert ctx.backend2.state.accepted.value == 1


def test_hedged(ctx):
    """
    Отправляем запрос на тормозящий бэкенд, ждем hedged-запрос на второй
    """

    hedged_delay_seconds = 2

    ctx.start_backend(SimpleDelayedConfig(response_delay=10), name='backend1')
    ctx.start_backend(SimpleConfig(), name='backend2')

    ctx.start_balancer(
        ModBalancerFailConfig(
            attempts=2,
            connection_attempts=1,
            retry_non_idempotent=True,
            hedged_delay=hedged_delay_seconds
        )
    )

    request = http.request.get(path='/idempotent', data='A' * 100).to_raw_request()
    with ctx.create_http_connection(timeout=5) as conn:
        response = conn.perform_request(request)
        asserts.status(response, 200)

    assert ctx.backend2.state.accepted.value == 1

    request_delay = ctx.backend2.state.requests.get().start_time - ctx.backend1.state.requests.get().start_time
    expected_delay = datetime.timedelta(seconds=hedged_delay_seconds * 0.99)

    assert request_delay >= expected_delay, 'Too small delay before hedged request. Expected {}, found {}'\
                                                                                    .format(hedged_delay_seconds, request_delay)

    unistat = ctx.get_unistat()

    assert unistat['report-default-hedged_attempts_summ'] == 1 and unistat['report-default-hedged_succ_summ'] == 1


def test_no_hedged(ctx):
    """
    Если бэкенд успел ответить быстро, hedged-запроса быть не должно
    """

    ctx.start_backend(SimpleDelayedConfig(response_delay=1), name='backend1')
    ctx.start_backend(SimpleDelayedConfig(response_delay=0), name='backend2')
    ctx.start_balancer(
        ModBalancerFailConfig(
            attempts=2,
            connection_attempts=1,
            retry_non_idempotent=True,
            hedged_delay=2
        )
    )

    request = http.request.get(path='/idempotent', data='A' * 100).to_raw_request()
    with ctx.create_http_connection(timeout=5) as conn:
        response = conn.perform_request(request)
        asserts.status(response, 200)

    assert ctx.backend1.state.accepted.value == 1
    assert ctx.backend2.state.accepted.value == 0

    unistat = ctx.get_unistat()

    assert unistat['report-default-hedged_attempts_summ'] == 0 and unistat['report-default-hedged_succ_summ'] == 0


def test_useless_hedged(ctx):
    """
    Если основной запрос ответил быстрее hedged-запроса, то должен использоваться ответ от него
    """

    ctx.start_backend(SimpleDelayedConfig(response_delay=3), name='backend1')
    ctx.start_backend(SimpleDelayedConfig(response_delay=5), name='backend2')
    ctx.start_balancer(
        ModBalancerFailConfig(
            attempts=2,
            connection_attempts=1,
            retry_non_idempotent=True,
            hedged_delay=2
        )
    )

    request = http.request.get(path='/idempotent', data='A' * 100).to_raw_request()
    with ctx.create_http_connection(timeout=5) as conn:
        response = conn.perform_request(request)
        asserts.status(response, 200)

    assert ctx.backend1.state.accepted.value == 1
    assert ctx.backend2.state.accepted.value == 1

    unistat = ctx.get_unistat()

    assert unistat['report-default-hedged_attempts_summ'] == 1 and unistat['report-default-hedged_succ_summ'] == 0


def test_hedged_concurrent_writing(ctx):
    """
    Основной и hedged-запрос не должны вместе начинать писать в клиента
    """

    first_data = 'A' * 10 ** 6
    second_data = 'B' * 10 ** 6

    ctx.start_backend(SimpleConfig(response=http.response.ok(data=first_data)), name='backend1')
    ctx.start_backend(SimpleConfig(response=http.response.ok(data=second_data)), name='backend2')
    ctx.start_balancer(
        ModBalancerFailConfig(
            attempts=2,
            connection_attempts=1,
            retry_non_idempotent=True,
            hedged_delay='0.001ms',
        )
    )

    request = http.request.get().to_raw_request()
    for _ in xrange(100):
        with ctx.create_http_connection(timeout=5) as conn:
            response = conn.perform_request(request)
            assert response.data.content == first_data or response.data.content == second_data


def test_hedged_race(ctx):
    """
    Было бы здорово не устраивать гонок корутин между основным и hedged-запросом
    """

    attempts = 2

    ctx.start_backend(SimpleDelayedConfig(response_delay=1000000), name='backend1')
    ctx.start_backend(SimpleDelayedConfig(response_delay=1000000), name='backend2')

    ctx.start_balancer(
        ModBalancerHedgedConfig(
            attempts=attempts,
            hedged_delay='100ms',
        )
    )

    request = http.request.get().to_raw_request()
    ctx.perform_request_xfail(request)

    unistat = ctx.get_unistat()
    assert unistat['report-default-backend_attempt_summ'] == attempts


def test_hedged_accesslog(ctx):
    """
    BALANCER-3336
    Записи основного и hedged-запроса в логе не должны пересекаться, основной запрос должен идти перед hedged
    """

    attempts = 2

    ctx.start_backend(SimpleDelayedConfig(response_delay=1000000), name='backend1')
    ctx.start_backend(SimpleConfig(), name='backend2')

    accesslog = ctx.manager.fs.create_file('accesslog')

    ctx.start_balancer(
        ModBalancerHedgedConfig(
            attempts=attempts,
            hedged_delay='100ms',
            accesslog=accesslog,
        )
    )

    request = http.request.get()
    for run in Multirun():
        with run:
            ctx.perform_request(request)
            log = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
            assert re.search(r'\[proxy[^\]]+ECANCELED\] \[proxy[^\]]+succ 200\]', log) is not None


def test_count_backends_in_attempts(ctx):
    """
    BALANCER-2690
    Проверяем attempts = 'count_backends';
    """

    for i in range(1, 6):
        ctx.start_backend(DummyConfig(), name='backend{}'.format(i))

    ctx.start_balancer(
        ModBalancerFailConfig(
            attempts='count_backends',
            connection_attempts=1,
            backend_timeout='100ms',
            backend_count=5,
        )
    )

    unistat = ctx.get_unistat()
    prev = unistat['report-default-backend_attempt_summ']

    request = http.request.get().to_raw_request()
    ctx.perform_request_xfail(request)

    unistat = ctx.get_unistat()
    assert unistat['report-default-backend_attempt_summ'] - prev == 5
    assert unistat['report-default-fail_summ'] == 1
    assert unistat['report-default-backend_fail_summ'] == 1


def test_count_backends_in_connection_attempts(ctx):
    """
    BALANCER-2690
    Проверяем connection_attempts = 'count_backends';
    """

    for i in range(1, 6):
        ctx.start_fake_backend(name='backend{}'.format(i))

    ctx.start_balancer(
        ModBalancerFailConfig(
            attempts=1,
            connection_attempts='count_backends',
            backend_timeout='100ms',
            backend_count=5,
        )
    )

    unistat = ctx.get_unistat()
    prev = unistat['report-default-conn_refused_summ']
    request = http.request.get().to_raw_request()
    ctx.perform_request_xfail(request)

    unistat = ctx.get_unistat()
    assert unistat['report-default-conn_refused_summ'] - prev == 5
    assert unistat['report-default-fail_summ'] == 1
    assert unistat['report-default-backend_fail_summ'] == 1


def test_two_level_rewind(ctx):
    ctx.start_backend(SimpleConfig(http.response.service_unavailable()))
    ctx.start_balancer(ModBalancerTwoLevelConfig())
    ctx.perform_request_xfail(http.request.post(data='Make a retry'))

    assert ctx.backend.state.requests.qsize() == 3


@pytest.mark.parametrize('algo', [ModBalancerRrWeightsFile, ModBalancerRendezvousHashingWeighsFile])
def test_weights_file_tag(ctx, algo):
    ctx.start_backend(SimpleConfig(http.response.ok()))

    def check_tag(tag):
        time.sleep(2)
        ctx.perform_request(http.request.get())

        found = False
        result = ctx.call_json_event('dump_weights_file_tags')
        for worker_result in result:
            for tag_info in worker_result:
                assert tag_info.get('tag') == tag
                found = True

        if not found:
            assert False, "no weights file tag found"

    weights_file = ctx.manager.fs.create_file('weights_file')
    ctx.start_balancer(algo(
        weights_file=weights_file,
    ))

    check_tag(None)

    ctx.manager.fs.rewrite(weights_file, '__weights_file_tag_test_tag,0\n')
    check_tag('test_tag')

    ctx.manager.fs.rewrite(weights_file, '')
    check_tag(None)
