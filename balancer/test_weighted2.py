# -*- coding: utf-8 -*-
import time

from configs import Weighted2Config

from balancer.test.util import asserts
from balancer.test.util.predef import http


def test_weighted2(ctx):
    """
    Проверка балансировки weighted2
    Если веса backend-ов, заданные в конфиге, одинаковы,
    то запросы распределяются по backend-ам равномерно
    """
    request_num = 10
    led_count = 0
    zeppelin_count = 0
    ctx.start_balancer(Weighted2Config())

    for _ in range(request_num):
        response = ctx.perform_request(http.request.get())
        asserts.status(response, 200)
        if response.data.content == 'Led':
            led_count += 1
        elif response.data.content == 'Zeppelin':
            zeppelin_count += 1

    assert led_count == request_num / 2
    assert zeppelin_count == request_num / 2


def test_slow_reply_time_tduration_format(ctx):
    """
    BALANCER-124
    Параметр slow_reply_time должен понимать значение в формате TDuration
    """
    ctx.start_balancer(Weighted2Config(slow_reply_time='10s'))

    assert ctx.balancer.is_alive()


def test_slow_reply_time_microseconds_format(ctx):
    """
    BALANCER-124
    Параметр slow_reply_time должен понимать значение, заданное в микросекундах
    """
    ctx.start_balancer(Weighted2Config(slow_reply_time=10))

    assert ctx.balancer.is_alive()


def test_invalid_weights_file(ctx):
    """
    BALANCER-1643
    Если weight_file невалидный, то балансер пишет запись в errorlog
    """
    weights_file = ctx.manager.fs.create_file('weights_file')
    ctx.manager.fs.rewrite(weights_file, 'balalaika,')

    ctx.start_balancer(Weighted2Config(weights_file=weights_file))
    ctx.perform_request(http.request.get())

    time.sleep(3)

    errorlog = ctx.manager.fs.read_file(ctx.balancer.config.errorlog)
    assert 'ReReadWeights Error parsing weight for weights_file:' in errorlog
