# -*- coding: utf-8 -*-
import math
import md5
import pytest

import balancer.test.plugin.context as mod_ctx

from configs import ConsisitentHashingConfig

from balancer.test.util.predef.handler.server.http import SimpleConfig
from balancer.test.util.process import BalancerStartError
from balancer.test.util.predef import http


class ConsistentHashingCtx(object):
    def start_backends(self):
        self.start_backend(SimpleConfig(), name='backend1')
        self.start_backend(SimpleConfig(), name='backend2')
        self.start_backend(SimpleConfig(), name='backend3')

    def start_balancer_and_backends(self, **balancer_kwargs):
        self.start_backends()
        return self.start_balancer(ConsisitentHashingConfig(
            self.backend1.server_config.port,
            self.backend2.server_config.port,
            self.backend3.server_config.port,
            **balancer_kwargs))

    def assert_balancer_wont_start(self, **balancer_kwargs):
        with pytest.raises(BalancerStartError):
            self.start_balancer(ConsisitentHashingConfig(
                42, 43, 44,  # fake backend ports
                **balancer_kwargs))


ch_ctx = mod_ctx.create_fixture(ConsistentHashingCtx)


def test_invalid_vnode_count(ch_ctx):
    """
    Проверяем, что балансер не запускается с невалидным значением virtual_nodes
    """
    ch_ctx.assert_balancer_wont_start(virtual_nodes=0)


@pytest.mark.parametrize('weight', [-1, 0, 0.5, 0xFFFFFFFF])
def test_invalid_weight(ch_ctx, weight):
    """
    Проверяем, что балансер не запускается с невалидным значением weight
    """
    ch_ctx.assert_balancer_wont_start(weight=weight)


def test_empty_balancer(ch_ctx):
    """
    Проверяем, что балансер не запускается с пустым списком бэкендов
    """
    ch_ctx.assert_balancer_wont_start(empty_balancer=True)


def send_requests(connetion, request_count):
    for i in range(0, request_count):
        m = md5.new(str(i))
        id = m.hexdigest()
        request = http.request.get(path='/i?id=%sn=21' % id)
        connetion.perform_request(request)


def get_request_distribution(ch_ctx, vnode_count, request_count):
    balancer = ch_ctx.start_balancer_and_backends(virtual_nodes=vnode_count)

    with ch_ctx.create_http_connection(balancer.config.port) as conn:
        send_requests(conn, request_count)

    size_list = [ch_ctx.backend1.state.requests.qsize(), ch_ctx.backend2.state.requests.qsize(), ch_ctx.backend3.state.requests.qsize()]

    assert request_count == sum(size_list)

    return size_list


def test_distribution(ch_ctx):
    """
    Проверяем, что множество запросов равномерно распределяется по бэкендам
    """
    request_count = 100
    vnode_count = 1000

    size_list = get_request_distribution(ch_ctx, vnode_count, request_count)

    backend_count = 3

    median = request_count/backend_count

    """
    Проверяем что разброс не более 20% (т.к. запросов не очень много)
    """
    for size in size_list:
        assert math.fabs(median - size)/median <= 0.2


def test_vnodes(ch_ctx):
    """
    Проверяем, что при изменении количества виртуальных нод меняется распределение по бэкендам
    """
    request_count = 50
    vnode_count1 = 1000
    vnode_count2 = 2000

    size_list1 = get_request_distribution(ch_ctx, vnode_count1, request_count)
    size_list2 = get_request_distribution(ch_ctx, vnode_count2, request_count)

    assert size_list1 != size_list2


def assert_req_num_in_one_backend(backends, req_num):
    found = False
    for backend in backends:
        if found:
            assert backend.state.requests.empty()
        elif not backend.state.requests.empty():
            found = True
            assert backend.state.requests.qsize() == req_num
            for _ in range(req_num):
                backend.state.get_request()


def test_stability(ch_ctx):
    """
    Проверяем, что одинаковые запросы раскидываются по одним и тем же бэкендам
    """
    balancer = ch_ctx.start_balancer_and_backends()

    with ch_ctx.create_http_connection(balancer.config.port) as conn:
        request1 = http.request.get(path='/i?id=7de5b0222a5e64fa2519b362a0d1288b&n=21')
        request2 = http.request.get(path='/i?id=82f3d9dfbad59b54596e36e335601286&n=21')

        conn.perform_request(request1)
        conn.perform_request(request1)

        all_backends = (ch_ctx.backend1, ch_ctx.backend2, ch_ctx.backend3)
        assert_req_num_in_one_backend(all_backends, 2)

        conn.perform_request(request2)
        conn.perform_request(request2)

        assert_req_num_in_one_backend(all_backends, 2)


def test_rebalance(ch_ctx):
    """
    Проверяем, что если бэкенд не отвечает, то идём в следующий
    """
    balancer = ch_ctx.start_balancer_and_backends()
    ch_ctx.backend3.stop()

    request_count = 10

    with ch_ctx.create_http_connection(balancer.config.port) as conn:
        send_requests(conn, request_count)
        assert ch_ctx.backend1.state.requests.qsize() + ch_ctx.backend2.state.requests.qsize() == request_count
