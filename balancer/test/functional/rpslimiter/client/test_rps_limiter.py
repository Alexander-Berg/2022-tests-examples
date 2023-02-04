# coding=utf-8
from configs import RpsLimiterConfig, RpsLimiterOnErrorConfig, RpsLimiterAsyncConfig, RpsLimiterBalancerConfig, RpsLimiterOnLimitedConfig

import pytest
import time
from balancer.test.util import asserts, sync
from balancer.test.util.predef import http
from balancer.test.util.predef.handler.server.http import SimpleConfig, StaticResponseHandler, StaticResponseConfig
from balancer.test.util.proto.handler.server import State
from balancer.test.util.process import BalancerStartError


def test_allow(ctx):
    """
    BALANCER-1733
    Модуль пробрасывает содержимое запроса в бекенд,
    если в ответе бекенда нет заголовка X-ForwardToUser-Y,
    то пропускает запрос дальше.
    """
    checker = ctx.start_backend(SimpleConfig(http.response.ok()), name='checker')

    ctx.start_balancer(RpsLimiterConfig(log_quota=False))
    response = ctx.perform_request(http.request.get('/test'))

    asserts.status(response, 200)
    asserts.content(response, 'module')

    request = checker.state.get_request()
    asserts.method(request, 'POST')
    asserts.path(request, '/quota.acquire')
    assert request.data.content == 'GET /test HTTP/1.1\r\n\r\n'


def test_post(ctx):
    """
    BALANCER-1733
    Модуль пробрасывает заголовки post запроса в бекенд,
    но тело не пробрасывает.
    """
    checker = ctx.start_backend(SimpleConfig(http.response.ok()), name='checker')

    ctx.start_balancer(RpsLimiterConfig(log_quota=False))
    response = ctx.perform_request(http.request.post(path='/post', data='aaaaa'))

    asserts.status(response, 200)
    asserts.content(response, 'module')

    request = checker.state.get_request()
    asserts.method(request, 'POST')
    asserts.path(request, '/quota.acquire')
    assert request.data.content == 'POST /post HTTP/1.1\r\n\r\n'


def test_not_forward(ctx):
    """
    BALANCER-1733
    Модуль пропускает запрос дальше,
    если в ответе бекенда заголовок X-ForwardToUser-Y не совпадает с TTrueFsm.
    """
    response = http.response.forbidden(data='denied', headers={'X-ForwardToUser-Y': 'false'})
    ctx.start_backend(SimpleConfig(response), name='checker')

    ctx.start_balancer(RpsLimiterConfig())
    response = ctx.perform_request(http.request.get())

    asserts.status(response, 200)
    asserts.content(response, 'module')


def test_deny(ctx):
    """
    BALANCER-1733
    Модуль отправляет ответ бекенда клиенту,
    если в ответе бекенда заголовок X-ForwardToUser-Y совпадает с TTrueFsm.
    """
    response = http.response.forbidden(data='denied', headers={'X-ForwardToUser-Y': 'true'})
    ctx.start_backend(SimpleConfig(response), name='checker')

    ctx.start_balancer(RpsLimiterConfig())
    response = ctx.perform_request(http.request.get())

    asserts.status(response, 403)
    asserts.content(response, 'denied')


def test_disable_file(ctx):
    """
    BALANCER-1733
    Модуль пропускает все запросы, если присутствует файл disable_file.
    """
    disable_file = ctx.manager.fs.create_file('disable_file')

    response = http.response.forbidden(data='denied', headers={'X-ForwardToUser-Y': 'true'})
    ctx.start_backend(SimpleConfig(response), name='checker')

    ctx.start_balancer(RpsLimiterConfig(disable_file=disable_file))
    response = ctx.perform_request(http.request.get())

    asserts.status(response, 200)
    asserts.content(response, 'module')


def test_on_error(ctx):
    """
    BALANCER-1733
    Модуль отправяет запрос в секцию on_error, если бекенд не отвечает и on_error есть в конфиге.
    """
    ctx.start_fake_backend(name='checker')

    ctx.start_balancer(RpsLimiterOnErrorConfig())
    response = ctx.perform_request(http.request.get())

    asserts.status(response, 400)
    asserts.content(response, 'on_error')


def test_on_limited_request(ctx):
    """
    BALANCER-3303
    Модуль отправяет запрос в секцию on_limited_request,
    если есть on_limited_request и rps limiter не разрешил запрос.
    """
    response = http.response.forbidden(data='denied', headers={'X-ForwardToUser-Y': 'true'})
    ctx.start_backend(SimpleConfig(response), name='checker')

    ctx.start_balancer(RpsLimiterOnLimitedConfig())
    response = ctx.perform_request(http.request.get())

    asserts.status(response, 429)
    asserts.content(response, 'too busy')


def test_fail_on_error(ctx):
    """
    BALANCER-1733
    Модуль посылает клиенту rst, если бекенд не отвечает и skip_on_error выключен.
    """
    ctx.start_fake_backend(name='checker')

    ctx.start_balancer(RpsLimiterConfig())
    ctx.perform_request_xfail(http.request.get())


def test_skip_on_error(ctx):
    """
    BALANCER-1733
    Модуль пропускает запрос, если бекенд не отвечает и включен skip_on_error.
    """
    ctx.start_fake_backend(name='checker')

    ctx.start_balancer(RpsLimiterConfig(skip_on_error=1))
    response = ctx.perform_request(http.request.get())

    asserts.status(response, 200)
    asserts.content(response, 'module')


def test_skip_on_5xx(ctx):
    """
    Модуль пропускает запрос, если бэкенд отдал 5xx
    """
    ctx.start_backend(SimpleConfig(http.response.service_unavailable()), name='checker')

    ctx.start_balancer(RpsLimiterConfig(skip_on_error=1))
    response = ctx.perform_request(http.request.get())

    asserts.status(response, 200)
    asserts.content(response, 'module')


def test_quota_name(ctx):
    """
    BALANCER-1733
    Модуль пробрасывает имя квоты из конфига в бекенд.
    """
    checker = ctx.start_backend(SimpleConfig(http.response.ok()), name='checker')

    ctx.start_balancer(RpsLimiterConfig(quota_name='qqqqq'))
    response = ctx.perform_request(http.request.get())

    asserts.status(response, 200)
    asserts.content(response, 'module')

    request = checker.state.get_request()
    asserts.method(request, 'POST')
    asserts.path(request, '/quota.acquire?quota=qqqqq')


def test_invalid_quota_name(ctx):
    """
    BALANCER-1733
    Балансер не стартует, если имя квоты содержит запрещенные символы.
    """
    ctx.start_fake_backend(name='checker')
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(RpsLimiterConfig(quota_name='qqqqq@'))


class SlowRpsLimiterState(State):
    def __init__(self, config):
        super(SlowRpsLimiterState, self).__init__(config)
        self.counter = sync.Counter(0)


class SlowRpsLimiterHandler(StaticResponseHandler):
    def handle_parsed_request(self, raw_request, stream):
        self.state.counter.inc()
        if self.state.counter.value == 1:
            time.sleep(self.config.delay)
        stream.write_response(self.config.response)


class SlowRpsLimiterConfig(StaticResponseConfig):
    HANDLER_TYPE = SlowRpsLimiterHandler
    STATE_TYPE = SlowRpsLimiterState

    def __init__(self, delay=5, response=None):
        if response is None:
            response = http.response.custom(429, 'Limited', headers={'X-ForwardToUser-Y': 'true'})
        super(SlowRpsLimiterConfig, self).__init__(response)
        self.delay = delay


def test_async_request(ctx):
    """
    Если указан параметр register_only, то балансер не дожидается ответа от rpslimiter'а
    """
    DELAY = 5

    ctx.start_backend(SlowRpsLimiterConfig(delay=DELAY), name='checker')
    ctx.start_balancer(RpsLimiterAsyncConfig(namespace='test'))

    start_time = time.time()
    ctx.perform_request(http.request.get('/register'))
    end_time = time.time()

    req = ctx.checker.state.get_request()
    assert 'X-Rpslimiter-Balancer: test' in req.data.content

    assert end_time - start_time <= DELAY / 2

    time.sleep(DELAY)

    response = ctx.perform_request(http.request.get('/test'))

    asserts.status(response, 429)

    req = ctx.checker.state.get_request()
    assert 'X-Rpslimiter-Balancer: test' in req.data.content


def test_namespace(ctx):
    """
    Если указан параметр namespace, то при запросе в rpslimiter должен проставляться
    соответствующий заголовок X-Rpslimiter-Balancer
    """
    ctx.start_backend(SimpleConfig(), name='checker')

    ctx.start_balancer(RpsLimiterConfig(namespace='test'))
    ctx.perform_request(http.request.get())

    req = ctx.checker.state.get_request()
    assert 'X-Rpslimiter-Balancer: test' in req.data.content


def test_register_backend_attempts(ctx):
    """
    Если указан параметр register_backend_attempts, то на каждую
    попытку хождения в бэкенд будет послан запрос в rpslimiter
    """
    ATTEMPTS = 3

    ctx.start_fake_backend(name='backend')
    ctx.start_backend(SimpleConfig(), name='checker')

    ctx.start_balancer(RpsLimiterBalancerConfig(
        attempts=ATTEMPTS,
        register_only=True,
        register_backend_attempts=True,
        namespace='test',
    ))

    ctx.perform_request(http.request.get())

    time.sleep(1)

    assert ctx.checker.state.requests.qsize() == ATTEMPTS

    req = ctx.checker.state.get_request()
    assert 'X-Rpslimiter-Balancer: test' in req.data.content
