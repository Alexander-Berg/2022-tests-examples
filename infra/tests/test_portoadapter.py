from __future__ import absolute_import

import os
import random
import select
import socket
import threading

from skycore.portoutils import PortoAdapter
from skycore.kernel_util.unittest import TestCase, main, skipIf


class ConnectionMock(object):
    class RPC(object):
        sock = 1
        sock_pid = 1

    def __init__(self, thread):
        self._thread = thread
        self.rpc = ConnectionMock.RPC()

    def _call(self, *args, **kwargs):
        assert threading.current_thread() is not self._thread, "Executed not in deblock!"

    connect = try_connect = TryConnect = disconnect = Create = _call

    def Get(self):
        assert threading.current_thread() is not self._thread, "Executed not in deblock!"
        ret = ConnectionMock(self._thread)
        ret.__module__ = 'porto.api'
        return ret


class PortoProxy(object):
    def __init__(self):
        self.thread = None
        self.stopped = True
        self.started = threading.Event()
        self.flapping = False
        self.path = None
        self.portosock = None

    def start(self):
        self.stopped = False
        self.thread = threading.Thread(target=self.loop)
        self.thread.daemon = True
        self.thread.start()

        if not self.started.wait(10) or not self.path:
            self.stopped = True
            raise RuntimeError("proxy not started")

    def _recv_hdr(self, sock):
        length = shift = 0
        hdr = ''
        while True:
            b = sock.recv(1)
            if not len(b):
                continue
            hdr += b
            length |= (ord(b) & 0x7f) << shift
            shift += 7
            if ord(b) <= 0x7f:
                break
        return hdr, length

    def _connect_to_porto(self):
        self.portosock = socket.socket(socket.AF_UNIX)
        self.portosock.settimeout(5)
        self.portosock.connect('/run/portod.socket')

    def _process_conn(self, conn):
        while not self.stopped:
            failed = self.flapping and random.randint(0, 2) == 2
            fail_step = random.randint(0, 5)

            hdr, length = self._recv_hdr(conn)
            if failed and fail_step == 0:
                return  # we intentionally do not close any socket, let 'em timeout

            req = conn.recv(length)
            self.portosock.sendall(hdr + req)
            if failed and fail_step == 1:
                self._connect_to_porto()
                return

            hdr, length = self._recv_hdr(self.portosock)
            if failed and fail_step == 2:
                self._connect_to_porto()
                return

            rsp = self.portosock.recv(length)
            assert len(rsp) == length

            if failed and fail_step == 3:
                return

            conn.sendall(hdr)

            if failed and fail_step == 4:
                return

            if failed and fail_step == 5:
                conn.sendall(rsp[:length / 2])
                return

            conn.sendall(rsp)

    def loop(self):
        path = '\0%s' % (''.join(chr(random.randint(33, 126)) for _ in range(10)))
        try:
            self._connect_to_porto()

            server_sock = socket.socket(socket.AF_UNIX)
            server_sock.bind(path)
            server_sock.listen(0)

            self.path = path
        finally:
            self.started.set()

        while not self.stopped:
            if server_sock not in select.select([server_sock], [], [], 1.)[0]:
                continue

            conn, _ = server_sock.accept()
            conn.setblocking(True)

            self._process_conn(conn)


class TestPortoAdapter(TestCase):
    def test_adapter(self):
        conn = ConnectionMock(threading.current_thread())
        conn = PortoAdapter(conn)

        conn.TryConnect()
        conn.connect()
        conn.Create()
        conn.disconnect()

        cont = conn.Get()
        cont.connect()
        cont.TryConnect()
        cont.Create()

    @skipIf(os.uname()[0].lower() != 'linux'
            or not os.path.exists('/run/portod.socket'),
            'porto is not available')
    @skipIf(not os.getenv('UNITTEST_SLOW'), 'slow tests disabled')
    def test_socket_timeout(self):
        from porto import Connection as PortoConnection
        from porto.exceptions import SocketTimeout

        proxy = PortoProxy()
        proxy.start()

        name1 = 'ut-%s-%s' % (os.getpid(), random.randint(0, 1 << 31))
        name2 = 'ut-%s-%s' % (os.getpid(), random.randint(1 << 100, 1 << 200))

        realconn = PortoConnection(timeout=20, auto_reconnect=False)
        realconn.connect()
        rcnt1 = realconn.CreateWeakContainer(name1)
        rcnt2 = realconn.CreateWeakContainer(name2)

        rcnt1.SetProperty('private', name1)
        rcnt2.SetProperty('private', name2 + 'x' * 256)

        try:
            conn = PortoConnection(proxy.path, timeout=1, auto_reconnect=False)
            conn = PortoAdapter(conn)

            cnt1 = conn.Find(name1)
            cnt2 = conn.Find(name2)

            proxy.flapping = True

            for _ in range(100):
                attempt = 0
                while True:
                    attempt += 1
                    if attempt > 100:
                        raise RuntimeError("too many disconnects in a row")
                    try:
                        assert cnt1.GetProperty('private') == name1
                    except SocketTimeout:
                        continue
                    break
                while True:
                    attempt += 1
                    if attempt > 100:
                        raise RuntimeError("too many disconnects in a row")
                    try:
                        assert cnt2.GetProperty('private') == name2 + 'x' * 256
                    except SocketTimeout:
                        continue
                    break
        finally:
            proxy.stopped = True


if __name__ == '__main__':
    main()
