# -*- coding: utf-8 -*-
from configs import DefaultConfig, TwoBackends

import pytest
import socket
from balancer.test.util import asserts
from balancer.test.util.predef import http
from balancer.test.util.predef.handler.server.http import DummyConfig, SimpleConfig
from balancer.test.util.proto.http.stream import HTTPReaderException


def test_client_conn_close(ctx):
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(DefaultConfig())

    resp = ctx.perform_request(http.request.get())
    asserts.status(resp, 200)


def test_connection_attempts(ctx):
    ctx.start_fake_backend(name='backend1')
    ctx.start_backend(SimpleConfig(), name='backend2')
    ctx.start_balancer(TwoBackends())

    resp = ctx.perform_request(http.request.get())
    asserts.status(resp, 200)


def test_no_retries(ctx):
    ctx.start_backend(DummyConfig(), name='backend1')
    ctx.start_backend(SimpleConfig(), name='backend2')
    ctx.start_balancer(TwoBackends())

    with pytest.raises((socket.error, HTTPReaderException)):
        ctx.perform_request(http.request.get())
