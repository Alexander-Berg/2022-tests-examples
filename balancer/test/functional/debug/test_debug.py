# -*- coding: utf-8 -*-

import datetime
import time

from configs import DebugConfig

from balancer.test.util.balancer import asserts
from balancer.test.util.predef.handler.server.http import SimpleConfig
from balancer.test.util.predef import http
from balancer.test.util.sanitizers import sanitizers
from balancer.test.util.stdlib.multirun import Multirun


def test_client_no_delay(ctx):
    backend_response = http.response.ok(data="A" * (2**10))
    ctx.start_backend(SimpleConfig(response=backend_response))
    ctx.start_balancer(DebugConfig())

    start = datetime.datetime.now()
    resp = ctx.perform_request(http.request.post(data="A" * (2**10)))
    asserts.status(resp, 200)
    finish = datetime.datetime.now()
    assert (finish - start).seconds < 5


def test_client_read_delay(ctx):
    backend_response = http.response.ok(data="A" * (2**10))
    ctx.start_backend(SimpleConfig(response=backend_response))
    ctx.start_balancer(DebugConfig(
        client_read_delay="5ms",
        client_read_size=1,
        backend_timeout="100s"
    ))

    start = datetime.datetime.now()
    resp = ctx.perform_request(http.request.post(data="A" * (2**10)))
    asserts.status(resp, 200)
    finish = datetime.datetime.now()
    assert (finish - start).seconds > 3
    assert (finish - start).seconds < 15


def test_client_write_delay(ctx):
    backend_response = http.response.ok(data="A" * (2**10))
    ctx.start_backend(SimpleConfig(response=backend_response))
    ctx.start_balancer(DebugConfig(
        client_write_delay="5ms",
        client_write_size=1,
        backend_timeout="100s"
    ))

    start = datetime.datetime.now()
    resp = ctx.perform_request(http.request.post(data="A" * (2**10)))
    asserts.status(resp, 200)
    finish = datetime.datetime.now()
    assert (finish - start).seconds > 3
    assert (finish - start).seconds < 15


def test_client_read_write_delay(ctx):
    backend_response = http.response.ok(data="A" * (2**10))
    ctx.start_backend(SimpleConfig(response=backend_response))
    ctx.start_balancer(DebugConfig(
        client_read_delay="5ms",
        client_read_size=1,
        client_write_delay="5ms",
        client_write_size=1,
        backend_timeout="100s"
    ))

    start = datetime.datetime.now()
    resp = ctx.perform_request(http.request.post(data="A" * (2**10)))
    asserts.status(resp, 200)
    finish = datetime.datetime.now()
    assert (finish - start).seconds > 6
    assert (finish - start).seconds < 30


def test_client_start_delay(ctx):
    backend_response = http.response.ok(data="A" * (2**10))
    ctx.start_backend(SimpleConfig(response=backend_response))
    ctx.start_balancer(DebugConfig(
        delay="5s",
    ))

    start = datetime.datetime.now()
    resp = ctx.perform_request(http.request.post(data="A" * (2**10)))
    asserts.status(resp, 200)
    finish = datetime.datetime.now()
    assert (finish - start).seconds > 3
    assert (finish - start).seconds < 15


def test_thread_freeze(ctx):
    if sanitizers.asan_enabled():
        return
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(DebugConfig(backend_timeout="1s", freeze_on_run=True))
    ctx.perform_request_xfail(http.request.get())

    time.sleep(5)

    for run in Multirun():
        with run:
            assert ctx.get_unistat()['threads-froze_ammv']

    time.sleep(55)

    for run in Multirun():
        with run:
            stderr = ctx.manager.fs.read_file(ctx.balancer.stderr_file)
            lines = stderr.split('\n')
            assert lines[0] == "Threads freeze detecting, master is about to abort"

    ctx.balancer.set_finished()
