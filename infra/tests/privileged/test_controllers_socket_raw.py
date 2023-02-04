# coding: utf-8
from __future__ import print_function

import _netmon

import socket

import tornado.ioloop
import tornado.gen

from agent import application

from yatest.common import network

from infra.netmon.agent.idl import common_pb2


def test_address_change():
    app = application.Application()

    with network.PortManager() as pm:
        @tornado.gen.coroutine
        def sync_addresses(addresses):
            yield app[application.EchoService].on_address_sync(addresses)
            yield app[application.UdpService].on_address_sync(addresses)
            yield app[application.IcmpService].on_address_sync(addresses)
            yield app[application.TcpService].on_address_sync(addresses)

        @tornado.gen.coroutine
        def do_work():
            addresses = [(socket.AF_INET6, "::1"), (socket.AF_INET, "127.0.0.1")]
            yield sync_addresses(addresses)

            new_addresses = [(socket.AF_INET6, "::1")]
            yield sync_addresses(new_addresses)

        port = pm.get_port()

        app.register(application.EchoService(listen_ports=[port]))
        app.register(application.UdpService())
        app.register(application.IcmpService())
        app.register(application.TcpService(listen_ports=[port]))

        app.run_sync(do_work)


def test_icmp_controller():
    app = application.Application()

    addresses = [(socket.AF_INET6, "::1"), (socket.AF_INET, "127.0.0.1")]

    @tornado.gen.coroutine
    def do_work():
        yield app[application.IcmpService].on_address_sync(addresses)

        reports = yield app[application.IcmpService].schedule_checks(
            [
                _netmon.ProbeConfig(
                    common_pb2.REGULAR_PROBE,
                    _netmon.Address(family, address, 0),
                    _netmon.Address(family, address, 0),
                    0,
                    10
                )
                for family, address in addresses
            ]
        )
        raise tornado.gen.Return(reports)

    app.register(application.IcmpService())

    reports = app.run_sync(do_work)

    assert reports
    assert all(not x.failed for x in reports)
    assert all(not x.lost and x.received for x in reports)
    assert all(x.protocol == 1 for x in reports)


def test_tcp_controller():
    app = application.Application()

    addresses = [(socket.AF_INET6, "::1"), (socket.AF_INET, "127.0.0.1")]

    with network.PortManager() as pm:
        port = pm.get_port()

        @tornado.gen.coroutine
        def do_work():
            yield app[application.TcpService].on_address_sync(addresses)

            reports = yield app[application.TcpService].schedule_checks(
                [
                    _netmon.ProbeConfig(
                        common_pb2.REGULAR_PROBE,
                        _netmon.Address(family, address, port),
                        _netmon.Address(family, address, port),
                        0,
                        10
                    )
                    for family, address in addresses
                ]
            )
            raise tornado.gen.Return(reports)

        app.register(application.TcpService(listen_ports=[port]))
        reports = app.run_sync(do_work)

    assert reports
    assert all(not x.failed for x in reports)
    assert all(not x.lost and x.received for x in reports)
    assert all(x.protocol == 3 for x in reports)
