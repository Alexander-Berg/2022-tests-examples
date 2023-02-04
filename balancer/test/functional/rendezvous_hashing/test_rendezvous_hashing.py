# -*- coding: utf-8 -*-
import math
import md5
import pytest
import time

import balancer.test.plugin.context as mod_ctx

from configs import RendezvousHashingConfig, RendezvousHashingConfigEmpty

from balancer.test.util.predef.handler.server.http import SimpleConfig
from balancer.test.util.process import BalancerStartError
from balancer.test.util.predef import http


class RendezvousHashingCtx(object):
    def __init__(self):
        super(RendezvousHashingCtx, self).__init__()
        self.__weights_file = self.manager.fs.create_file('weights')

    @property
    def weights_file(self):
        return self.__weights_file

    def start_backends(self):
        self.start_backend(SimpleConfig(), name='backend1')
        self.start_backend(SimpleConfig(), name='backend2')
        self.start_backend(SimpleConfig(), name='backend3')

    def start_balancer_and_backends(self, **balancer_kwargs):
        self.start_backends()
        return self.start_balancer(RendezvousHashingConfig(
            self.__weights_file,
            **balancer_kwargs))

    def assert_balancer_wont_start(self, **balancer_kwargs):
        with pytest.raises(BalancerStartError):
            self.start_balancer(RendezvousHashingConfigEmpty(
                None,  # without weight file
                **balancer_kwargs))


ch_ctx = mod_ctx.create_fixture(RendezvousHashingCtx)


def test_invalid_reload_duration(ch_ctx):
    """
    Проверяем, что балансер не запускается с невалидным значением reload_duration
    """
    ch_ctx.assert_balancer_wont_start(reload_duration="deadbeef")


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


def get_request_distribution(ch_ctx, request_count):
    balancer = ch_ctx.start_balancer_and_backends()

    with ch_ctx.create_http_connection(balancer.config.port) as conn:
        send_requests(conn, request_count)

    size_list = [ch_ctx.backend1.state.requests.qsize(), ch_ctx.backend2.state.requests.qsize(), ch_ctx.backend3.state.requests.qsize()]
    assert request_count == sum(size_list)
    return size_list


def test_distribution(ch_ctx):
    """
    Проверяем что разброс распределения запросов не более 25% (т.к. запросов не очень много)
    """
    request_count = 100
    size_list = get_request_distribution(ch_ctx, request_count)
    backend_count = 3
    median = request_count/backend_count

    for size in size_list:
        assert math.fabs(median - size)/median <= 0.25


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


def test_dead_backend(ch_ctx):
    """
    Проверяем, что если бэкенд не отвечает, то идём в следующий
    """
    ch_ctx.start_balancer_and_backends()
    ch_ctx.backend3.stop()

    request_count = 10

    with ch_ctx.create_http_connection() as conn:
        send_requests(conn, request_count)
        assert ch_ctx.backend1.state.requests.qsize() + ch_ctx.backend2.state.requests.qsize() == request_count


def update_weights_file(ch_ctx, weights):
    data = ""
    for i, weight in enumerate(weights):
        data += "{0},{1}\n".format(i+1, weight)
    ch_ctx.manager.fs.rewrite(ch_ctx.weights_file, data)
    time.sleep(1)


def check_weights_file(ch_ctx, weights):
    # Perform weights file read on first request
    with ch_ctx.create_http_connection() as conn:
        request = http.request.get(path='/empty')
        conn.perform_request(request)

    weights_map = {}
    for worker_result in ch_ctx.call_json_event("dump_backends"):
        assert len(worker_result) == 1
        for backend in worker_result[0]["backends"]:
            w = backend["weight"]
            if w in weights_map:
                weights_map[w] += 1
            else:
                weights_map[w] = 1

    for weight in weights:
        if weights in weights_map:
            weights_map[weight] -= 1
            if weights_map[w] < 0:
                raise Exception("Failed to check weights. Incorrect number of backends({0}) with weight {1}".format(weights_map[w], w))


def check_value_range(value, rng):
    return rng - 5 < value and value < rng + 5


def perform_requests(ch_ctx, weights, request_count, requests_by_backend, init_balancer=True, seq_number=1, req_number=None):
    update_weights_file(ch_ctx, weights)

    if req_number is None:
        req_number = request_count

    if init_balancer:
        ch_ctx.start_balancer_and_backends()

    check_weights_file(ch_ctx, weights)

    with ch_ctx.create_http_connection() as conn:
        send_requests(conn, request_count)

        assert check_value_range(ch_ctx.backend1.state.requests.qsize(), requests_by_backend[0])
        assert check_value_range(ch_ctx.backend2.state.requests.qsize(), requests_by_backend[1])
        assert check_value_range(ch_ctx.backend3.state.requests.qsize(), requests_by_backend[2])

        all_backends = ch_ctx.backend1.state.requests.qsize() + ch_ctx.backend2.state.requests.qsize() + ch_ctx.backend3.state.requests.qsize()
        assert all_backends - seq_number == req_number


def test_invalid_weights_file_path(ch_ctx):
    """
    Проверяем, что балансер стартует и принимает запросы с инвалидным конфигом весов,
    а также пишет сообщение в errorlog
    """
    weights = ('balalaika', 'vodka', 'matryoshka')
    requests_by_backend = [
        42,
        25,
        34]
    perform_requests(ch_ctx, weights, 100, requests_by_backend)

    time.sleep(3)

    errorlog = ch_ctx.manager.fs.read_file(ch_ctx.balancer.config.errorlog)
    assert 'UpdateWeights Error parsing weight for weights_file:' in errorlog


def test_equal_weights(ch_ctx):
    """
    Проверяем, что балансер правильно раскидывает запросы по бекэндам при равных весах
    """
    weights = (1, 1, 1)
    requests_by_backend = [
        42,
        25,
        34]
    perform_requests(ch_ctx, weights, 100, requests_by_backend)


def test_progression_weights(ch_ctx):
    """
    Проверяем, что балансер правильно раскидывает запросы по бекэндам при разных не нулевых весах
    """
    weights = (500, 100, 1)
    requests_by_backend = [
        87,
        14,
        0]
    perform_requests(ch_ctx, weights, 100, requests_by_backend)


def test_restore_weights(ch_ctx):
    """
    Проверяем, что балансер правильно обрабатывает ситуацию с удалением бекенда в файле весов
    """
    weights = (1, 1, 1)
    requests_by_backend = [
        42,
        25,  # +1 empty request
        34]
    perform_requests(ch_ctx, weights, 100, requests_by_backend)

    weights = (500, 0, 500)
    requests_by_backend = [
        94,
        25,  # +2 empty request
        83]
    perform_requests(ch_ctx, weights, 100, requests_by_backend, init_balancer=False, seq_number=2, req_number=200)

    weights = (1, 1, 1)
    requests_by_backend = [
        136,
        50,
        117]  # +1 empty request
    perform_requests(ch_ctx, weights, 100, requests_by_backend, init_balancer=False, seq_number=3, req_number=300)


def test_delete_weights(ch_ctx):
    """
    Проверяем, что балансер правильно обрабатывает ситуацию с удалением файла весов
    """
    weights = (500, 0, 500)
    requests_by_backend = [
        52,
        0,
        49]  # +1 empty request
    perform_requests(ch_ctx, weights, 100, requests_by_backend)

    ch_ctx.manager.fs.remove(ch_ctx.weights_file)

    requests_by_backend = [
        94,
        25,
        83]  # +1 empty request
    perform_requests(ch_ctx, (), 100, requests_by_backend, init_balancer=False, seq_number=2, req_number=200)
