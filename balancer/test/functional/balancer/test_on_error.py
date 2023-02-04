from configs import OnFastErrorConfig

from balancer.test.util import asserts
from balancer.test.util.predef import http
from balancer.test.util.predef.handler.server.http import SimpleConfig


def test_on_fast_error(ctx):
    """
    BALANCER-3155
    Запрос попадает в секцию on_fast_error если случилась ошибка соединения с бекендами
    """
    ctx.start_fake_backend()
    ctx.start_balancer(OnFastErrorConfig())

    resp = ctx.perform_request(http.request.get())
    asserts.status(resp, 405)
    asserts.content(resp, 'on_fast_error')


def test_on_fast_error_backend_fast_error(ctx):
    """
    BALANCER-3155
    Запрос попадает в секцию on_fast_error если бекенд ответил 503 и включен fast_503
    """
    ctx.start_backend(SimpleConfig(response=http.response.service_unavailable()))
    ctx.start_balancer(OnFastErrorConfig(fast_503=True))

    resp = ctx.perform_request(http.request.get())
    asserts.status(resp, 405)
    asserts.content(resp, 'on_fast_error')


def test_on_fast_error_no_backends(ctx):
    """
    BALANCER-3155
    Запрос попадает в секцию on_fast_error если под балансером нет бекендов
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(OnFastErrorConfig(backend_weight=0))

    resp = ctx.perform_request(http.request.get())
    asserts.status(resp, 405)
    asserts.content(resp, 'on_fast_error')


def test_on_fast_error_not_fast_error(ctx):
    """
    BALANCER-3155
    Запрос не попадает в секцию on_fast_error если бекенд ответил 500
    """
    ctx.start_backend(SimpleConfig(response=http.response.service_unavailable()))
    ctx.start_balancer(OnFastErrorConfig())

    resp = ctx.perform_request(http.request.get())
    asserts.status(resp, 404)
    asserts.content(resp, 'on_error')
