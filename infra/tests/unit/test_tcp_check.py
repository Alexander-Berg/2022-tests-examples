import errno
import socket

import gevent

from instancectl import constants
from instancectl.status.prober import tcp_socket


class Spec(object):
    def __init__(self, host, port):
        self.host = host
        self.port = port


class Sock(object):
    def __init__(self):
        self.closed = False

    def close(self):
        self.closed = True


def test_connect():
    def connect_gaierror(address):
        raise socket.gaierror(socket.EAI_NODATA, 'No address associated with hostname')

    def connect_timeout(address):
        raise gevent.Timeout(10)

    def connect_reject(address):
        raise socket.error(errno.ECONNREFUSED, 'Connection refused')

    sock = Sock()

    def connect_ok(address):
        return sock

    cases = [
            (connect_gaierror, False),
            (connect_timeout, False),
            (connect_reject, False),
            (connect_ok, True),
    ]
    for fun, ok in cases:
        p = tcp_socket.TcpSocketProber(fun)
        cond = p.probe(Spec('host', 80))
        if not ok:
            assert cond.status == constants.CONDITION_FALSE
            assert cond.reason
            assert cond.message
        else:
            assert cond.status == constants.CONDITION_TRUE
            assert sock.closed

