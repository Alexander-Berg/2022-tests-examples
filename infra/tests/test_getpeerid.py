"""
Test getpeerid function.
"""
import os
import unittest
import socket
import tempfile
try:
    import gevent
    import gevent.socket
except ImportError:
    gevent = None

from sepelib.util.net.peerid import getpeerid
from utils import get_sock_path, unlink


class TestGetPeerId(unittest.TestCase):
    def test_bad_socket(self):
        s = socket.socket()
        self.assertRaises(OSError, getpeerid, s)

    # don't want to mess with threads here
    # so don't run without gevent
    @unittest.skipIf(not gevent, "no gevent available")
    def test_ok_socket(self):
        # get uid
        uid = os.getuid()
        sock_path = get_sock_path()
        # setup server
        server = gevent.socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
        if os.path.exists(sock_path):
            unlink(sock_path)
        server.bind(sock_path)
        c = None
        try:
            server.listen(1)
            # client setup
            client = gevent.socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
            gevent.spawn(client.connect, sock_path)

            c, addr = server.accept()
            peerid = getpeerid(c)
            self.assertIsInstance(peerid, int)
            self.assertEqual(peerid, uid)
        finally:
            if c:
                c.close()
            server.close()
            unlink(sock_path)
