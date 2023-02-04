# -*- coding: utf-8 -*-
import time
import math

import pytest

import balancer.test.plugin.context as mod_ctx

from balancer.test.util.predef import http
from balancer.test.util import asserts
from balancer.test.util.stdlib.multirun import Multirun
from balancer.test.util.predef.handler.server.http import SimpleConfig, SimpleDelayedConfig, DummyConfig

from configs import RequestReplierConfig, RequestReplierHashConfig


class RequestReplierContext(object):
    def __init__(self):
        super(RequestReplierContext, self).__init__()
        self.__workers = self.request.param
        self.__rate_file = self.manager.fs.create_file('rate_file')  # FIXME BALANCER-830

    @property
    def rate_file(self):
        return self.__rate_file

    def start_all(self, main_backend_confg, sink_backend_config, **balancer_kwargs):
        self.start_backend(main_backend_confg, name='main_backend')
        self.start_backend(sink_backend_config, name='sink_backend')
        self.start_balancer(RequestReplierConfig(
            **balancer_kwargs
        ))

    def write_rate_file(self, contents):
        self.manager.fs.rewrite(self.rate_file, contents)
        time.sleep(1.3)  # make sure the file was re-read

    def erase_rate_file(self):
        self.manager.fs.remove(self.rate_file)


replier_ctx = mod_ctx.create_fixture(RequestReplierContext, params=[None, 1], ids=['no_workers', 'single_worker'])


def test_main_backend_response(replier_ctx):
    """
    BALANCER-759
    Request passing from client through request_replier should
    return main backend answer
    """
    main_backend_confg = SimpleConfig(http.response.ok(data='main'))
    sink_backend_config = SimpleConfig()
    replier_ctx.start_all(main_backend_confg, sink_backend_config)
    response = replier_ctx.perform_request(http.request.get())
    asserts.status(response, 200)
    asserts.content(response, 'main')


def test_rate_zero(replier_ctx):
    """
    BALANCER-759
    When rate is set to 0.0, no requests to sink are performed.
    """
    main_backend_confg = SimpleConfig(http.response.ok(data='main'))
    sink_backend_config = SimpleConfig()
    replier_ctx.start_all(main_backend_confg, sink_backend_config, rate=0.0)
    response = replier_ctx.perform_request(http.request.get())
    asserts.status(response, 200)
    asserts.content(response, 'main')
    assert replier_ctx.sink_backend.state.accepted.value == 0


def test_rate_zero_with_file(replier_ctx):
    """
    BALANCER-759
    When there is rate_file option is set, then the contents of that file represent
    the actual rate of requests sent to sink.
    When rate is set to 0.0, no requests to sink are performed.
    """
    main_backend_confg = SimpleConfig(http.response.ok(data='main'))
    sink_backend_config = SimpleConfig()
    replier_ctx.write_rate_file('0.0')
    replier_ctx.start_all(main_backend_confg, sink_backend_config, rate=1.0, rate_file=replier_ctx.rate_file)
    time.sleep(1.3)
    response = replier_ctx.perform_request(http.request.get())
    asserts.status(response, 200)
    asserts.content(response, 'main')
    assert replier_ctx.sink_backend.state.accepted.value == 0


def test_rate_one(replier_ctx):
    """
    BALANCER-759
    When rate is set to 1.0, sink gets a copy of original request
    """
    main_backend_confg = SimpleConfig(http.response.ok(data='main'))
    sink_backend_config = SimpleConfig()
    replier_ctx.start_all(main_backend_confg, sink_backend_config, rate=1.0)
    path = '/scorpions'
    headers = {
        'Led': 'Zeppelin',
        'Black': 'Sabbath',
    }
    response = replier_ctx.perform_request(http.request.get(path=path, headers=headers))
    asserts.status(response, 200)
    asserts.content(response, 'main')

    for run in Multirun():
        with run:
            assert replier_ctx.sink_backend.state.accepted.value == 1

    for request in [replier_ctx.main_backend.state.get_request(), replier_ctx.sink_backend.state.get_request()]:
        asserts.path(request, path)
        asserts.headers_values(request, headers)


def test_rate_one_with_file(replier_ctx):
    """
    BALANCER-759
    When there is rate_file option is set, then the contents of that file represent
    the actual rate of requests sent to sink.
    When rate is set to 1.0, sink gets a copy of original request
    """
    main_backend_confg = SimpleConfig(http.response.ok(data='main'))
    sink_backend_config = SimpleConfig()
    replier_ctx.write_rate_file('1.0')
    replier_ctx.start_all(main_backend_confg, sink_backend_config, rate=0.0, rate_file=replier_ctx.rate_file)
    time.sleep(1.3)
    path = '/scorpions'
    headers = {
        'Led': 'Zeppelin',
        'Black': 'Sabbath',
    }
    response = replier_ctx.perform_request(http.request.get(path=path, headers=headers))
    asserts.status(response, 200)
    asserts.content(response, 'main')

    for run in Multirun():
        with run:
            assert replier_ctx.sink_backend.state.accepted.value == 1

    for request in [replier_ctx.main_backend.state.get_request(), replier_ctx.sink_backend.state.get_request()]:
        asserts.path(request, path)
        asserts.headers_values(request, headers)


def test_request_with_body_no_sink(replier_ctx):
    """
    BALANCER-759
    With rate = 0.0 requests with body are passed to main backend completely
    """
    main_backend_confg = SimpleConfig(http.response.ok(data='main'))
    sink_backend_config = SimpleConfig()
    replier_ctx.start_all(main_backend_confg, sink_backend_config, rate=0.0)
    path = '/scorpions'
    headers = {
        'Led': 'Zeppelin',
        'Black': 'Sabbath',
    }
    data = 'Metallica'
    response = replier_ctx.perform_request(http.request.post(path=path, headers=headers, data=data))
    asserts.status(response, 200)
    asserts.content(response, 'main')
    time.sleep(1.0)
    assert replier_ctx.sink_backend.state.accepted.value == 0

    for request in [replier_ctx.main_backend.state.get_request()]:
        asserts.path(request, path)
        asserts.headers_values(request, headers)
        asserts.content(request, data)


@pytest.mark.parametrize('copies', [1, 4])
@pytest.mark.parametrize('http_ver', ["1.0", "1.1"])
def test_request_with_body_and_sink(replier_ctx, http_ver, copies):
    """
    BALANCER-759
    With rate = 1.0 requests with body are passed to both main and sink backends completely
    """
    main_backend_confg = SimpleConfig(http.response.ok(data='main'))
    sink_backend_config = SimpleConfig()
    replier_ctx.start_all(main_backend_confg, sink_backend_config, rate=1.0*copies)
    path = '/scorpions'
    headers = {
        'Led': 'Zeppelin',
        'Black': 'Sabbath',
    }
    data = 'Metallica'
    response = replier_ctx.perform_request(
        http.request.post(
            path=path,
            version="HTTP/{}".format(http_ver),
            headers=headers,
            data=data
        )
    )
    asserts.status(response, 200)
    asserts.content(response, 'main')
    for run in Multirun():
        with run:
            assert replier_ctx.sink_backend.state.accepted.value == copies

    def check_request(request):
        asserts.path(request, path)
        asserts.headers_values(request, headers)
        asserts.content(request, data)

    check_request(replier_ctx.main_backend.state.get_request())
    for i in range(copies):
        check_request(replier_ctx.sink_backend.state.get_request())


def test_no_sink_request_after_main_fail(replier_ctx):
    """
    BALANCER-759
    If main backend failed to answer, then no request is sent to sink
    """
    main_backend_confg = DummyConfig()
    sink_backend_config = SimpleConfig()
    replier_ctx.start_all(main_backend_confg, sink_backend_config, rate=1.0)
    path = '/scorpions'
    headers = {
        'Led': 'Zeppelin',
        'Black': 'Sabbath',
    }
    data = 'Metallica'
    replier_ctx.perform_request_xfail(http.request.post(path=path, headers=headers, data=data))
    time.sleep(1.0)
    assert replier_ctx.sink_backend.state.accepted.value == 0


def test_sink_request_after_main_fail_with_enabled_fails(replier_ctx):
    """
    BALANCER-891
    With enable_failed_requests_replication option set to true,
    request gets to sink section even after fail of main
    """
    main_backend_confg = DummyConfig()
    sink_backend_config = SimpleConfig()
    replier_ctx.start_all(main_backend_confg, sink_backend_config, rate=1.0, enable_failed_requests_replication=True)
    path = '/scorpions'
    headers = {
        'Led': 'Zeppelin',
        'Black': 'Sabbath',
    }
    data = 'Metallica'

    replier_ctx.perform_request_xfail(http.request.post(path=path, headers=headers, data=data))
    time.sleep(1.0)

    for run in Multirun():
        with run:
            assert replier_ctx.sink_backend.state.accepted.value == 1

    request = replier_ctx.sink_backend.state.get_request()
    asserts.path(request, path)
    asserts.headers_values(request, headers)
    asserts.content(request, data)


def test_slow_sink_does_not_affect_main(replier_ctx):
    """
    BALANCER-759
    If sink backend is slow, it does not affect performance of keepalive requests
    to main module - there is little or no delay for them
    """
    main_backend_confg = SimpleConfig(http.response.ok(data='main'))
    delay = 1.5
    sink_backend_config = SimpleDelayedConfig(response_delay=delay)
    replier_ctx.start_all(main_backend_confg, sink_backend_config, rate=1.0, sink_backend_timeout=2 * delay)
    with replier_ctx.create_http_connection():  # as conn
        start_time = time.time()
        for i in xrange(2):
            response = replier_ctx.perform_request(http.request.get())
            asserts.status(response, 200)
            asserts.content(response, 'main')
        finish_time = time.time()
        assert (finish_time - start_time) < delay

    for run in Multirun():
        with run:
            assert replier_ctx.sink_backend.state.accepted.value > 0


def test_failing_sink_does_not_affect_main(replier_ctx):
    """
    BALANCER-759
    If sink backend is failing to respond, it does not affect performance of keepalive requests
    to main module - there is little or no delay for them
    """
    main_backend_confg = SimpleConfig(http.response.ok(data='main'))
    sink_backend_config = DummyConfig()
    replier_ctx.start_all(main_backend_confg, sink_backend_config, rate=1.0)
    with replier_ctx.create_http_connection():  # as conn
        for i in xrange(2):
            response = replier_ctx.perform_request(http.request.get())
            asserts.status(response, 200)
            asserts.content(response, 'main')


@pytest.mark.parametrize('rate', [0.1, 0.5, 0.8, 1.5])
def test_rate(replier_ctx, rate):
    """
    BALANCER-759
    If rate = x, then approximately x of requests would be duplicated to sink
    """
    main_backend_confg = SimpleConfig(http.response.ok(data='main'))
    sink_backend_config = SimpleConfig()
    replier_ctx.start_all(main_backend_confg, sink_backend_config, rate=rate)

    requests_count = 100

    request = http.request.get()
    for i in xrange(requests_count):
        response = replier_ctx.perform_request(request)
        asserts.status(response, 200)
        asserts.content(response, 'main')

    # hello, matan
    fractional = math.modf(rate)[0]
    variance = math.sqrt(requests_count * fractional * (1 - fractional))
    abs_variance = 3 * variance
    expected_requests = requests_count * rate

    for run in Multirun():
        with run:
            sink_accepted = replier_ctx.sink_backend.state.accepted.value
            assert (expected_requests - abs_variance) <= sink_accepted <= (expected_requests + abs_variance)


def test_hash(ctx):
    """
    BALANCER-2804
    Hash-based balancing should work in sinks without recalculating the hashes
    """
    main_backend_confg = SimpleConfig(http.response.ok(data='main'))
    sink_backend_config = SimpleConfig()

    ctx.start_backend(main_backend_confg, name='main_backend')
    ctx.start_backend(sink_backend_config, name='sink_backend1')
    ctx.start_backend(sink_backend_config, name='sink_backend2')
    ctx.start_balancer(RequestReplierHashConfig())

    requests_count = 100

    for i in xrange(requests_count):
        response = ctx.perform_request(http.request.get('/?uid={}'.format(i)))
        asserts.status(response, 200)
        asserts.content(response, 'main')

    variance = math.sqrt(requests_count * 0.5 * 0.5)
    abs_variance = 3 * variance
    expected_requests = requests_count * 0.5

    for run in Multirun():
        with run:
            total = 0
            sink_accepted1 = ctx.sink_backend1.state.accepted.value
            total += sink_accepted1
            sink_accepted2 = ctx.sink_backend2.state.accepted.value
            total += sink_accepted2
            assert (expected_requests - abs_variance) <= sink_accepted1 <= (expected_requests + abs_variance)
            assert (expected_requests - abs_variance) <= sink_accepted2 <= (expected_requests + abs_variance)
            assert total == 100


def test_config_rate_restore_after_file_delete(replier_ctx):
    """
    BALANCER-759
    Balancer should restore the value of rate from its config after the rate_file has been deleted
    """
    main_backend_confg = SimpleConfig(http.response.ok(data='main'))
    sink_backend_config = SimpleConfig()
    replier_ctx.write_rate_file('0.0')
    replier_ctx.start_all(main_backend_confg, sink_backend_config, rate=1.0, rate_file=replier_ctx.rate_file)
    time.sleep(1.3)

    response = replier_ctx.perform_request(http.request.get())
    asserts.status(response, 200)
    asserts.content(response, 'main')
    assert replier_ctx.sink_backend.state.accepted.value == 0

    replier_ctx.erase_rate_file()
    time.sleep(1.3)

    response = replier_ctx.perform_request(http.request.get())
    asserts.status(response, 200)
    asserts.content(response, 'main')

    for run in Multirun():
        with run:
            assert replier_ctx.sink_backend.state.accepted.value == 1
