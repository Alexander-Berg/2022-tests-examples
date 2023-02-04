# coding: utf-8
from __future__ import print_function

import socket

import tornado.ioloop
import tornado.gen

import pytest

import _netmon

from agent import application

from infra.netmon.agent.idl import common_pb2


def get_port_list(count=1):
    port_list = []
    sock_list = [socket.socket(socket.AF_INET6, socket.SOCK_DGRAM) for _ in xrange(count)]
    for sock in sock_list:
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        sock.bind(("::", 0))
        port_list.append(sock.getsockname()[1])
    for sock in sock_list:
        sock.close()
    return port_list


def get_port():
    return get_port_list()[0]


def test_failing_calls():
    app = application.Application()

    @tornado.gen.coroutine
    def do_work():
        # this should not silently fail..
        with pytest.raises(Exception):
            yield app[application.UdpService].on_address_sync([(socket.AF_INET, "5.4.3.2")])

    app.register(application.UdpService())
    app.run_sync(do_work)


def test_udp_controller():
    app = application.Application()

    addresses = [
        (socket.AF_INET6, "::1"),
        (socket.AF_INET, "127.0.0.1")
    ]
    port = get_port()

    @tornado.gen.coroutine
    def do_work():
        yield app[application.EchoService].on_address_sync(addresses)
        yield app[application.UdpService].on_address_sync(addresses)

        reports = yield app[application.UdpService].schedule_checks(
            [
                _netmon.ProbeConfig(
                    common_pb2.REGULAR_PROBE,
                    _netmon.Address(source_family, source_address, 0),
                    _netmon.Address(target_family, target_address, port),
                    0,
                    10
                )
                for source_family, source_address in addresses
                for target_family, target_address in addresses
                if source_family == target_family
            ]
        )
        raise tornado.gen.Return(reports)

    app.register(application.EchoService(listen_ports=[port]))
    app.register(application.UdpService())

    reports = app.run_sync(do_work)
    assert reports
    assert all(not x.failed for x in reports)
    assert all(not x.lost and x.received for x in reports)


def test_udp_controller_with_specific_port():
    app = application.Application()

    addresses = [(socket.AF_INET6, "::1")]
    source_port, target_port = get_port_list(2)

    @tornado.gen.coroutine
    def do_work():
        yield app[application.EchoService].on_address_sync(addresses)
        yield app[application.UdpService].on_address_sync(addresses)

        reports = yield app[application.UdpService].schedule_checks(
            [
                _netmon.ProbeConfig(
                    common_pb2.REGULAR_PROBE,
                    _netmon.Address(family, address, source_port),
                    _netmon.Address(family, address, target_port),
                    0,
                    10
                )
                for family, address in addresses
            ]
        )
        raise tornado.gen.Return(reports)

    app.register(application.EchoService(listen_ports=[target_port]))
    app.register(application.UdpService())

    reports = app.run_sync(do_work)
    assert reports
    assert all(not x.failed for x in reports)
    assert all(not x.lost and x.received for x in reports)


def test_icmp_controller():
    app = application.Application()
    app.register(application.IcmpService())

    @tornado.gen.coroutine
    def do_work():
        yield app[application.IcmpService].on_address_sync([
            (socket.AF_INET6, "::1"),
            (socket.AF_INET, "127.0.0.1")
        ])

    with pytest.raises(Exception):
        # tests can't have needed permissions to create raw sockets
        app.run_sync(do_work)


def test_tcp_controller():
    app = application.Application()
    port = get_port()

    app.register(application.TcpService(listen_ports=[port]))

    @tornado.gen.coroutine
    def do_work():
        yield app[application.IcmpService].on_address_sync([
            (socket.AF_INET6, "::1"),
            (socket.AF_INET, "127.0.0.1")
        ])

    with pytest.raises(Exception):
        # tests can't have needed permissions to create raw sockets
        app.run_sync(do_work)


def test_unknown_address():
    app = application.Application()

    @tornado.gen.coroutine
    def do_work():
        yield app[application.UdpService].on_address_sync([
            (socket.AF_INET, "127.0.0.1")
        ])

        reports = yield app[application.UdpService].schedule_checks(
            [
                _netmon.ProbeConfig(
                    common_pb2.REGULAR_PROBE,
                    _netmon.Address(socket.AF_INET6, "::1", 0),
                    _netmon.Address(socket.AF_INET6, "::1", 1234),
                    0,
                    10
                )
            ]
        )

        raise tornado.gen.Return(reports)

    app.register(application.UdpService())

    [report] = app.run_sync(do_work)
    assert report.failed
    assert report.error
    assert not report.received
    assert not report.lost


def test_lost_packages():
    app = application.Application()

    port = get_port()

    @tornado.gen.coroutine
    def do_work():
        yield app[application.UdpService].on_address_sync([
            (socket.AF_INET6, "::1")
        ])

        reports = yield app[application.UdpService].schedule_checks(
            [
                _netmon.ProbeConfig(
                    common_pb2.REGULAR_PROBE,
                    _netmon.Address(socket.AF_INET6, "::1", 0),
                    _netmon.Address(socket.AF_INET6, "::1", port),
                    0,
                    10
                )
            ]
        )

        raise tornado.gen.Return(reports)

    app.register(application.UdpService())

    [report] = app.run_sync(do_work)
    assert not report.failed
    assert not report.error
    assert not report.received
    assert report.lost


def test_resolver():
    app = application.Application()

    @tornado.gen.coroutine
    def resolve(host):
        addrs = yield app[application.ResolverService].try_resolve(host, ignore_errors=False)
        raise tornado.gen.Return(addrs)

    addrs = app.run_sync(lambda: resolve('localhost'))
    assert addrs == [(socket.AF_INET6, '::1')]

    # Per RFC 7686, reject queries for ".onion" domain names with NXDOMAIN
    addrs = app.run_sync(lambda: resolve('ya.onion'))
    assert len(addrs) == 0

    # error ARES_EBADNAME
    addrs = app.run_sync(lambda: resolve('a.a123456789b123456789c123456789d123456789e123456789f123456789g123456789.org'))
    assert addrs is None
