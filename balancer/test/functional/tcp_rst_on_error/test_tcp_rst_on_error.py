# -*- coding: utf-8 -*-
import pytest

from configs import DefaultTcpRstOnErrorConfig, TcpRstOnErrorConfig

from balancer.test.util.predef import http
from balancer.test.util.stdlib.multirun import Multirun
from balancer.test.util.stream.ssl.stream import SSLClientOptions


def check_tcpdump(tcpdump, expect_rst):
    for run in Multirun(sum_delay=3):
        with run:
            tcpdump.read_all()
            sessions = tcpdump.get_sessions()
            assert(len(sessions) > 0)
            sess = sessions[0]
            assert(sess.reset == expect_rst)


def base_rst_on_error(ctx, expect_rst):
    tcpdump = ctx.manager.tcpdump.start(ctx.balancer.config.port)
    ctx.perform_request_xfail(http.request.get())
    check_tcpdump(tcpdump, expect_rst)


@pytest.mark.parametrize('default_rst', [None, True], ids=['nil', 'true'])
def test_default_rst_on(ctx, default_rst):
    """
    BALANCER-1060
    Setting default rst on error true (default is true too) makes balancer
    finish tcp connections with RST in case where request cannot be served.
    """
    ctx.start_balancer(DefaultTcpRstOnErrorConfig(default_tcp_rst_on_error=default_rst))
    base_rst_on_error(ctx, True)


def test_default_rst_off(ctx):
    """
    BALANCER-1060
    Setting default rst on error false makes balancer
    finish tcp connections with FIN-ACK sequence in case where request cannot be served.
    """
    ctx.start_balancer(DefaultTcpRstOnErrorConfig(default_tcp_rst_on_error=False))
    base_rst_on_error(ctx, False)


@pytest.mark.parametrize('default_rst', [False, True], ids=['instance_false', 'instance_true'])
@pytest.mark.parametrize('send_rst', [False, True], ids=['send_rst_false', 'send_rst_true'])
def test_tcp_rst_on_error_override_true(ctx, default_rst, send_rst):
    """
    BALANCER-1060
    tcp_rst_on_error/send_rst overrides default_tcp_rst_on_error
    """
    ctx.start_balancer(TcpRstOnErrorConfig(default_tcp_rst_on_error=False, send_rst=send_rst))
    base_rst_on_error(ctx, send_rst)


def test_no_rst_on_shutdown(ctx):
    """
    BALANCER-1198
    Idle connection shot out by graceful shutdown procedure should be completed
    with FIN-ACK sequence, not RST
    """
    ctx.start_balancer(DefaultTcpRstOnErrorConfig(default_tcp_rst_on_error=True))
    tcpdump = ctx.manager.tcpdump.start(ctx.balancer.config.port)

    with ctx.create_http_connection():
        ctx.graceful_shutdown(timeout='4s', close_timeout='4s')
        for run in Multirun(plan=[0.1] * 40):
            with run:
                assert not ctx.balancer.is_alive()

    ctx.balancer.set_finished()

    check_tcpdump(tcpdump, False)


def test_no_rst_on_shutdown_ssl(ctx):
    """
    BALANCER-1198
    Idle connection shot out by graceful shutdown procedure should be completed
    with FIN-ACK sequence, not RST if ssl handshake completed succesfully.
    Otherwise it is a bad client with possible RST.
    """
    ctx.start_balancer(DefaultTcpRstOnErrorConfig(default_tcp_rst_on_error=True, use_ssl=True, cert_dir=ctx.certs.root_dir))
    tcpdump = ctx.manager.tcpdump.start(ctx.balancer.config.port)

    with ctx.manager.connection.http.create_ssl(ctx.balancer.config.port, SSLClientOptions()):
        ctx.graceful_shutdown(timeout='4s', close_timeout='4s')
        for run in Multirun(plan=[0.1] * 40):
            with run:
                assert not ctx.balancer.is_alive()

    ctx.balancer.set_finished()

    check_tcpdump(tcpdump, False)


def test_rst_on_shutdown_without_close(ctx):
    ctx.start_balancer(DefaultTcpRstOnErrorConfig(default_tcp_rst_on_error=True))
    tcpdump = ctx.manager.tcpdump.start(ctx.balancer.config.port)

    with ctx.create_http_connection():
        ctx.graceful_shutdown(timeout='4s', close_timeout='0s')
        for run in Multirun(plan=[0.1] * 40):
            with run:
                assert not ctx.balancer.is_alive()

    ctx.balancer.set_finished()

    check_tcpdump(tcpdump, True)


def test_rst_on_shutdown_without_close_ssl(ctx):
    ctx.start_balancer(DefaultTcpRstOnErrorConfig(default_tcp_rst_on_error=True, use_ssl=True, cert_dir=ctx.certs.root_dir))
    tcpdump = ctx.manager.tcpdump.start(ctx.balancer.config.port)

    with ctx.manager.connection.http.create_ssl(ctx.balancer.config.port, SSLClientOptions()):
        ctx.graceful_shutdown(timeout='4s', close_timeout='0s')
        for run in Multirun(plan=[0.1] * 40):
            with run:
                assert not ctx.balancer.is_alive()

    ctx.balancer.set_finished()

    check_tcpdump(tcpdump, True)
