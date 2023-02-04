# coding: utf-8
from __future__ import print_function

import errno
import socket
import contextlib
import concurrent.futures
import itertools

import tornado.gen
import tornado.web
import tornado.httpserver
import pytest

import pyneh

OK = "OK"


class EchoHandler(tornado.web.RequestHandler):

    def get(self):
        self.set_header("X-Yandex", "test")
        self.write(OK)
        self.finish()

    def post(self):
        self.write(self.request.body)
        self.finish()


class HeaderHandler(tornado.web.RequestHandler):

    def post(self):
        assert self.request.headers.get("X-Yandex") == "something"
        assert self.request.headers.get("Content-Type") == "application/json"
        self.write("bob")
        self.finish()


class SleepHandler(tornado.web.RequestHandler):

    @tornado.gen.coroutine
    def get(self):
        yield tornado.gen.sleep(int(self.get_argument("timeout", default="0")))
        self.write(OK)
        self.finish()


class BadHandler(tornado.web.RequestHandler):

    def get(self):
        self.set_status(503)
        self.finish()


@pytest.yield_fixture
def server_address(io_loop):
    app = tornado.web.Application([
        (r"/post", EchoHandler),
        (r"/get", EchoHandler),
        (r"/header", HeaderHandler),
        (r"/sleep", SleepHandler),
        (r"/error", BadHandler)
    ])
    server = tornado.httpserver.HTTPServer(app, io_loop=io_loop)

    sock = socket.socket(socket.AF_INET6)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    sock.setsockopt(socket.IPPROTO_IPV6, socket.IPV6_V6ONLY, 1)
    sock.setblocking(0)
    sock.bind(("::", 0))
    sock.listen(128)
    server.add_sockets([sock])

    try:
        yield "[::1]:{0}".format(sock.getsockname()[1])
    finally:
        server.stop()
        sock.close()


@pytest.fixture
def thread_pool():
    return concurrent.futures.ThreadPoolExecutor(max_workers=1)


@pytest.fixture
def requester():
    return pyneh.Requester()


def assert_that_response_is_good(response):
    assert response is not None and response.ready and not response.failed


def assert_that_response_data_equal_to(response, data):
    assert response.data == data and response.status_code == 0 and response.error_code == 0


def test_empty_requester(requester):
    assert not list(requester.iterate(0.001))


def test_bad_schema(requester):
    with pytest.raises(Exception):
        requester.add("wrong://schema")


def test_options(requester):
    requester.set_connect_timeout("5s")
    requester.set_slow_connect("100ms")

    with pytest.raises(Exception):
        requester.set_connect_timeout("wrong")

    with pytest.raises(Exception):
        requester.set_slow_connect("wrong")


@pytest.mark.gen_test
def test_get_request(requester, server_address, thread_pool):
    requester.add("http://{}/get".format(server_address))
    response = yield thread_pool.submit(requester.wait)
    assert_that_response_is_good(response)
    assert_that_response_data_equal_to(response, OK)
    assert response.duration
    assert dict(response.headers).get("X-Yandex") == "test"


@pytest.mark.gen_test
def test_post_request(requester, server_address, thread_pool):
    body = b"something\x00somewhere"
    requester.add("post://{}/post".format(server_address), data=body)
    response = yield thread_pool.submit(requester.wait)
    assert_that_response_is_good(response)
    assert_that_response_data_equal_to(response, body)


@pytest.mark.gen_test
def test_http_request(requester, server_address, thread_pool):
    request = pyneh.HttpRequest("http://{}/header".format(server_address))
    requester.add_request(request.set_content("alice").set_content_type("application/json").add_header("X-Yandex", "something"))
    response = yield thread_pool.submit(requester.wait)
    assert_that_response_is_good(response)
    assert_that_response_data_equal_to(response, "bob")


@pytest.mark.gen_test
def test_wait_timeout(requester, server_address, thread_pool):
    requester.add("http://{}/sleep?timeout=10".format(server_address))
    response = yield thread_pool.submit(requester.wait, 0.1)
    assert response is None


@pytest.mark.gen_test
def test_request_timeout(requester, server_address, thread_pool):
    requester.add("http://{}/sleep?timeout=10".format(server_address), timeout=0.1)
    response = yield thread_pool.submit(requester.wait)
    assert response is not None and response.ready
    assert response.failed and response.cancelled
    assert response.error_text


@pytest.mark.gen_test
def test_request_cancel(requester, server_address, thread_pool):
    group_id = requester.reserve_group_id()
    requester.add_to_group("http://{}/sleep?timeout=10".format(server_address), group_id, timeout=10)
    response = yield thread_pool.submit(requester.wait, 0.1)
    assert response is None
    requester.cancel_group(group_id)
    response = yield thread_pool.submit(requester.wait)
    assert response is not None and response.ready
    assert response.failed and response.cancelled
    assert response.error_text


@pytest.mark.gen_test
def test_failing_request(requester, server_address, thread_pool):
    requester.add("http://{}/error".format(server_address))
    response = yield thread_pool.submit(requester.wait)
    assert response is not None and response.ready
    assert response.failed and response.status_code == 503 and response.error_text


@pytest.mark.gen_test
def test_payload(requester, server_address, thread_pool):
    payload = object()
    requester.add("http://{}/get".format(server_address), payload=payload)
    response = yield thread_pool.submit(requester.wait)
    assert_that_response_is_good(response)
    assert response.payload is payload


@pytest.mark.gen_test
def test_multiple_requests(requester, server_address, thread_pool):
    count = 10
    for idx in xrange(count):
        requester.add("http://{}/get".format(server_address), payload=idx)
    responses = yield thread_pool.submit(lambda it: list(itertools.islice(it, count)), requester.iterate())
    assert {resp.payload for resp in responses} == {idx for idx in xrange(count)}
    for resp in responses:
        assert_that_response_is_good(resp)


@pytest.mark.gen_test
def test_reusing(requester, server_address, thread_pool):
    for _ in xrange(3):
        requester.add("http://{}/get".format(server_address))
        response = yield thread_pool.submit(requester.wait)
        assert_that_response_is_good(response)


def test_error_code(requester):
    with contextlib.closing(socket.socket(socket.AF_INET6)) as sock:
        sock.setsockopt(socket.IPPROTO_IPV6, socket.IPV6_V6ONLY, 1)
        sock.bind(("::", 0))
        port = sock.getsockname()[1]
    requester.add("http://[::1]:{}/".format(port))
    response = requester.wait()
    assert response is not None and response.ready and response.failed
    assert response.error_code == errno.ECONNREFUSED and not response.status_code


def test_mlock():
    try:
        pyneh.lock_all_memory()
    except RuntimeError:
        # TODO: test it properly
        pass
