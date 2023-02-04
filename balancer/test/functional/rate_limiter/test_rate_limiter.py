# -*- coding: utf-8 -*-
import threading
import time

from configs import RateLimiterConfig
from balancer.test.util.predef.handler.server.http import SimpleConfig
from balancer.test.util.predef import http


def test_no_queue(ctx):
    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(RateLimiterConfig(
        backend.server_config.port,
        max_requests=1,
        interval="1s",
        max_requests_in_queue=0,
    ))

    ctx.perform_request(http.request.get('/'))
    for _ in range(10):
        assert ctx.perform_request(http.request.get('/')).status == 429

    assert backend.state.requests.qsize() == 1


def make_parallel_requests(ctx, requests_count):
    threads = []
    responses = []

    def request():
        start_time = time.time()
        response = ctx.perform_request(http.request.get('/'))
        response.elapsed = time.time() - start_time
        responses.append(response)

    for _ in range(requests_count):
        thread = threading.Thread(target=request)
        thread.setDaemon(True)
        threads.append(thread)
        thread.start()

    for thread in threads:
        thread.join()

    return responses


def get_all_requests_from_backend(backend):
    requests = []
    for _ in range(backend.state.requests.qsize()):
        requests.append(backend.state.requests.get())

    requests.sort(key=lambda x: x.start_time)

    return requests


def check_request_time_diff_lower_bound(requests, diff_lower_bound):
    for request_id in range(len(requests) - 1):
        requests_time_diff = (requests[request_id + 1].start_time
                              - requests[request_id].start_time)
        assert requests_time_diff.total_seconds() >= diff_lower_bound


def test_queued(ctx):
    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(RateLimiterConfig(
        backend.server_config.port,
        max_requests=1,
        interval="1s",
        max_requests_in_queue=5,
    ))

    responses = make_parallel_requests(ctx, 20)
    ok = 0
    queued = 0
    for response in responses:
        if response.status == 200:
            ok += 1
        elif response.status != 429:
            assert False, "invalid status code: " + str(response.status)

        if response.elapsed >= 1:
            queued += 1

    # First request + 5 requests should be queued
    assert ok == 6
    assert queued == 5

    requests = get_all_requests_from_backend(backend)
    assert ok == len(requests)

    time_diff_lower_bound = 0.995
    check_request_time_diff_lower_bound(requests, time_diff_lower_bound)


def test_requests_after_delay(ctx):
    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(RateLimiterConfig(
        backend.server_config.port,
        max_requests=1,
        interval="1s",
        max_requests_in_queue=1
    ))

    ctx.perform_request(http.request.get('/'))

    time.sleep(3)

    make_parallel_requests(ctx, 2)

    requests = get_all_requests_from_backend(backend)

    time_diff_lower_bound = 0.995
    check_request_time_diff_lower_bound(requests, time_diff_lower_bound)
