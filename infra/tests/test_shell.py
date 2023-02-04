import os
import sys
import select
import socket
import random
from cStringIO import StringIO
from unittest import TestCase, main

import msgpack
import mock

from ya.skynet.services.portoshell import proxy
proxy.register_proxies()  # noqa

from ya.skynet.util.unittest import skipIf
from ya.skynet.util.net.socketstream import SocketStream
from ya.skynet.services.portoshell import shell
from ya.skynet.services.portoshell import portotools


def make_name():
    return 'ut-%s-%s' % (os.getpid(), random.randint(0, sys.maxint))


class ChannelWrapper(object):
    def __init__(self, sock):
        self.sock = sock

    def recv(self, size):
        return self.sock.recv(size)

    eof_received = False
    closed = False

    def sendall(self, data):
        return self.sock.sendall(data)

    sendall_stderr = sendall

    def fileno(self):
        return self.sock.fileno()

    def settimeout(self, timeout):
        return self.sock.settimeout(timeout)


class TestShell(TestCase):
    def test_watch(self):
        data = StringIO()

        def add_data(d):
            data.write(d)

        conn = portotools.get_portoconn()
        c = conn.CreateWeakContainer(make_name())
        c.SetProperty('command', "/bin/bash -c 'read LINE; echo 1; echo 2; echo 3; echo 4; echo 5'")
        r, w = os.pipe()
        session = None
        try:
            c.SetProperty('stdin_path', '/dev/fd/%s' % (r,))
            c.Start()

            ctx = shell.Context(mock.Mock())
            ctx.user = 'root'
            ctx.log = mock.Mock()
            session = ctx.make_session(0)
            session.container_name = c.name

            out = mock.Mock()

            out.sendall.side_effect = add_data

            handler = session.watch_ssh(out)
            handler._check_output('stdout')
            handler._check_output('stderr')
            self.assertFalse(out.sendall.call_count)
            self.assertFalse(out.sendall_stderr.call_count)

            os.write(w, 'start\n')
            handler.run()

            self.assertFalse(out.sendall_stderr.call_count)
            self.assertEqual(data.getvalue(), '1\n2\n3\n4\n5\n')
        finally:
            os.close(r)
            os.close(w)

    @skipIf(os.getuid() != 0, 'we are not root, cannot use custom user')
    def test_simple(self):
        conn = portotools.get_portoconn()
        c = conn.CreateWeakContainer(make_name())
        c.SetProperty('isolate', True)
        c.SetProperty('memory_limit', 1 << 30)
        c.SetProperty('command', '/bin/sleep 1000')
        c.SetProperty('enable_porto', False)
        c.SetProperty('user', 'daemon')
        c.Start()

        s1, s2 = socket.socketpair()
        try:
            s1.setblocking(False)
            ctx = shell.Context(mock.Mock())
            ctx.user = 'root'
            ctx.log = mock.Mock()
            session = ctx.make_session(0)
            session.container_name = c.name + '/' + make_name()
            session.cwd = '/'
            session.tag = 'ut@ut'
            session.extra_env = []

            s1.sendall('ps axwww\nexit\n')

            server = session.start_server(s2)
            server.run()
            data = s1.recv(16384)
            self.assertTrue('/bin/sleep 1000' in data)
            self.assertTrue('portoinit' in data)
            self.assertTrue('ps axwww' in data)
            self.assertTrue('exit' in data)
            self.assertFalse(session.container.GetProperty('enable_porto'))
        finally:
            s1.close()
            s2.close()

    @skipIf(os.getuid() != 0, 'we are not root, cannot use custom user')
    def test_simple_apimode(self):
        conn = portotools.get_portoconn()
        c = conn.CreateWeakContainer(make_name())
        c.SetProperty('isolate', True)
        c.SetProperty('memory_limit', 1 << 30)
        c.SetProperty('command', '/bin/sleep 1000')
        c.Start()

        s1, s2 = socket.socketpair()
        try:
            s1.setblocking(False)
            ctx = shell.Context(mock.Mock())
            ctx.user = 'root'
            ctx.log = mock.Mock()
            ctx.api_mode = True
            session = ctx.make_session(0)
            session.container_name = c.name + '/' + make_name()
            session.tag = 'ut@ut'
            session.extra_env = []
            session.cmd = 'ps axwww'
            session.pty_params = {'height': 24, 'width': 80}

            server = session.start_server(s2)
            server.run()
            data = ''
            unpacker = msgpack.Unpacker(use_list=True)
            while s1 in select.select([s1], [], [], 0)[0]:
                unpacker.feed(SocketStream(s1).readBEStr())

            for obj in unpacker:
                self.assertFalse(obj.get('error'))
                self.assertEqual(obj['output'], 'stdout')
                data += obj['data']

            self.assertTrue('/bin/sleep 1000' in data)
            self.assertTrue('portoinit' in data)
            self.assertTrue('ps axwww' in data)
        finally:
            s1.close()
            s2.close()

    @skipIf(os.getuid() != 0, 'we are not root, cannot use custom user')
    def test_simple_apimode_without_cmd(self):
        conn = portotools.get_portoconn()
        c = conn.CreateWeakContainer(make_name())
        c.SetProperty('isolate', True)
        c.SetProperty('memory_limit', 1 << 30)
        c.SetProperty('command', '/bin/sleep 1000')
        c.Start()

        s1, s2 = socket.socketpair()
        try:
            ctx = shell.Context(mock.Mock())
            ctx.user = 'root'
            ctx.log = mock.Mock()
            ctx.api_mode = True
            session = ctx.make_session(0)
            session.container_name = c.name + '/' + make_name()
            session.cwd = '/'
            session.tag = 'ut@ut'
            session.extra_env = []
            session.pty_params = {'height': 24, 'width': 80}

            with self.assertRaises(RuntimeError):
                session.start_server(s2)
        finally:
            s1.close()
            s2.close()

    @skipIf(os.getuid() != 0, 'we are not root, cannot use custom user')
    def test_ssh_no_tty(self):
        conn = portotools.get_portoconn()
        c = conn.CreateWeakContainer(make_name())
        c.SetProperty('isolate', True)
        c.SetProperty('memory_limit', 1 << 30)
        c.SetProperty('command', '/bin/sleep 1000')
        c.SetProperty('enable_porto', False)
        c.SetProperty('user', 'daemon')
        c.Start()

        s1, s2 = socket.socketpair()

        try:
            s1.setblocking(False)
            ctx = shell.Context(mock.Mock())
            ctx.user = 'root'
            ctx.log = mock.Mock()
            session = ctx.make_session(0)
            session.container_name = c.name + '/' + make_name()
            session.cwd = '/'
            session.tag = 'ut@ut'
            session.extra_env = []

            s1.sendall('ps axwww\nexit\n')

            server = session.start_ssh(ChannelWrapper(s2))
            server.run()
            data = s1.recv(16384)
            self.assertTrue('/bin/sleep 1000' in data)
            self.assertTrue('portoinit' in data)
            self.assertTrue('ps axwww' in data)
            self.assertTrue('exit' in data)
            self.assertFalse(session.container.GetProperty('enable_porto'))
        finally:
            s1.close()
            s2.close()

    @skipIf(os.getuid() != 0, 'we are not root, cannot use custom user')
    def test_ssh_tty(self):
        conn = portotools.get_portoconn()
        c = conn.CreateWeakContainer(make_name())
        c.SetProperty('isolate', True)
        c.SetProperty('memory_limit', 1 << 30)
        c.SetProperty('command', '/bin/sleep 1000')
        c.SetProperty('enable_porto', False)
        c.SetProperty('user', 'daemon')
        c.Start()

        try:
            s1, s2 = socket.socketpair()

            s1.setblocking(False)
            ctx = shell.Context(mock.Mock())
            ctx.user = 'root'
            ctx.log = mock.Mock()
            session = ctx.make_session(0)
            session.container_name = c.name + '/' + make_name()
            session.cwd = '/'
            session.tag = 'ut@ut'
            session.cmd = 'ps axwww'
            session.pty_params = {'height': 24, 'width': 80}
            session.extra_env = []

            server = session.start_ssh(ChannelWrapper(s2))
            server.run()
            data = s1.recv(16384)
            self.assertTrue('/bin/sleep 1000' in data)
            self.assertTrue('portoinit' in data)
            self.assertTrue('ps axwww' in data)
            self.assertFalse(session.container.GetProperty('enable_porto'))
        finally:
            s1.close()
            s2.close()

    @skipIf(os.getuid() != 0, 'we are not root, cannot use custom user')
    def test_pty_rights(self):
        conn = portotools.get_portoconn()
        c = conn.CreateWeakContainer(make_name())
        c.SetProperty('isolate', True)
        c.SetProperty('memory_limit', 1 << 30)
        c.SetProperty('command', '/bin/sleep 1000')
        c.SetProperty('enable_porto', False)
        c.SetProperty('user', 'daemon')
        c.Start()

        s1, s2 = socket.socketpair()

        try:
            s1.setblocking(False)
            ctx = shell.Context(mock.Mock())
            ctx.user = 'daemon'
            ctx.log = mock.Mock()
            session = ctx.make_session(0)
            session.container_name = c.name + '/' + make_name()
            session.cwd = '/'
            session.tag = 'ut@ut'
            session.cmd = '/bin/sh'
            session.pty_params = {'height': 24, 'width': 80}
            session.extra_env = []

            for fn in ('/dev/stdin', '/dev/stdout', '/dev/stderr', '/proc/self/fd/0', '/proc/self/fd/1', '/proc/self/fd/2', '$SSH_TTY'):
                s1.sendall('stat -c "%n %U %G %a" ' + fn + '\n')
            s1.sendall('exit\n')
            server = session.start_ssh(ChannelWrapper(s2))
            pty = filter(lambda k: k[0] == 'SSH_TTY', session.extra_env)[0][1]
            server.run()
            data = s1.recv(65536)
            self.assertTrue('/dev/stdin root root 777' in data, data)
            self.assertTrue('/dev/stdout root root 777' in data, data)
            self.assertTrue('/dev/stderr root root 777' in data, data)
            self.assertTrue('/proc/self/fd/0 daemon daemon 700' in data, data)
            self.assertTrue('/proc/self/fd/1 daemon daemon 700' in data, data)
            self.assertTrue('/proc/self/fd/2 daemon daemon 700' in data, data)
            self.assertTrue(pty + ' daemon tty 620' in data, pty + "\n" + data)
        finally:
            s1.close()
            s2.close()


if __name__ == '__main__':
    main()
