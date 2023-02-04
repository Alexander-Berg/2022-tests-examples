# -*- coding: utf-8 -*-
"""
SEPE-7112
"""
import pytest
from configs import ThumbHashConfig

from balancer.test.util import asserts
from balancer.test.util.process import BalancerStartError
from balancer.test.util.predef import http


FIRST = ('00', [
    '/i?id=c5b3067c442d6521aef2e88a33f82253&n=e992b6663f88523f7f89fa8d77644e6b',
    '/i?id=4cad6465bcc46f15aadfdbec418adb1c&n=2655f54c2fd16e8403ee03ac3dddedd8',
    '/i?id=40119e780f694ef73cc328faccefe3aa&n=c59c57700986c9de9d3a5162fe28a73d',
])


SECOND = ('01', [
    '/i?id=e992b6663f88523f7f89fa8d77644e6b&n=c5b3067c442d6521aef2e88a33f82253',
    '/i?id=2655f54c2fd16e8403ee03ac3dddedd8&n=4cad6465bcc46f15aadfdbec418adb1c',
    '/i?id=c59c57700986c9de9d3a5162fe28a73d&n=40119e780f694ef73cc328faccefe3aa',
])


def base_hash_test(ctx, **balancer_kwargs):
    ctx.start_balancer(ThumbHashConfig(**balancer_kwargs))
    for num, req_list in [FIRST, SECOND]:
        for req in req_list:
            response = ctx.perform_request(http.request.get(path=req))
            asserts.content(response, num)


def test_hash(ctx):
    """
    Проверка, что запросы распределяются по подмодулям в зависимости от запроса и номера модуля
    """
    base_hash_test(ctx, id_regexp='id=([a-fA-F0-9]+)', first='00', second='01')


def test_hash_reorder(ctx):
    """
    Проверка, что запросы распределяются по подмодулям в зависимости от запроса и номера модуля,
    даже если модули не отсортированы
    """
    base_hash_test(ctx, id_regexp='id=([a-fA-F0-9]+)', first='01', second='00')


def test_multiple_groups(ctx):
    """
    Если в регулярке присутствует несколько групп, то для определения модуля должна использоваться первая
    """
    base_hash_test(ctx, id_regexp='id=([a-fA-F0-9]+)&n=([a-fA-F0-9]+)', first='00', second='01')


def test_not_match(ctx):
    """
    Если запрос не матчится регуляркой, то его надо переслать в секцию default
    """
    ctx.start_balancer(ThumbHashConfig(id_regexp='id=([a-fA-F0-9]+)', first='00', second='01'))
    response = ctx.perform_request(http.request.get(path='/abc'))
    asserts.content(response, 'default')


def test_invalid_hash(ctx):
    """
    Если сматчившийся хеш невалиден, то запрос должен пойти в секцию default
    """
    ctx.start_balancer(ThumbHashConfig(id_regexp='id=([a-fA-F0-9]+)', first='00', second='01'))
    response = ctx.perform_request(http.request.get(path='/i?id=abc&n=def'))
    asserts.content(response, 'default')


def test_missed_id(ctx):
    """
    Если секции пронумерованы не от 0 до N, то балансер не должен запуститься
    """
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(ThumbHashConfig(id_regexp='id=([a-fA-F0-9]+)', first='10', second='01'))


def test_bad_id(ctx):
    """
    Если номер секции -- не число, то балансер не должен запуститься
    """
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(ThumbHashConfig(id_regexp='id=([a-fA-F0-9]+)', first='00', second='aa'))
