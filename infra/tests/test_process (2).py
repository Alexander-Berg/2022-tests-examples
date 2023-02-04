from __future__ import absolute_import

import os
import glob
import errno
import gevent
from cStringIO import StringIO

import mock

from skycore.kernel_util.unittest import TestCase, main, skipIf
from skycore.kernel_util import logging
from skycore.kernel_util.sys import TempDir
from skycore.kernel_util.sys.user import getUserName
from skycore.kernel_util.uuid import genuuid

from skycore.components.portowatcher import PortoWatcher
from skycore.procs import PortoProcess, LinerProcess, Subprocess, Stream, mailbox
from skycore import initialize_skycore


class TestStream(TestCase):
    def setUp(self):
        initialize_skycore()
        super(TestStream, self).setUp()

        log = logging.getLogger('tststrm')
        log.propagate = False
        log.setLevel(logging.DEBUG)
        self.io = StringIO()
        handler = logging.StreamHandler(self.io)
        log.addHandler(handler)
        self.stream = Stream(log, 'tst')

    def test_small_chunks(self):
        self.stream.feed('kokoko')
        self.assertEqual('', self.io.getvalue())
        self.stream.feed('\n')
        self.assertEqual('[tst] kokoko\n', self.io.getvalue())
        self.stream.feed('kekeke\n')
        self.assertEqual('[tst] kokoko\n[tst] kekeke\n', self.io.getvalue())

    def test_large_chunks(self):
        self.stream.feed('x' * (1 << 10))
        self.assertEqual(self.io.getvalue(), '')
        self.stream.feed('y' * (1 << 10))
        self.assertEqual(self.io.getvalue(), '')
        self.stream.feed('z' * (1 << 14))
        self.assertEqual(self.io.getvalue(),
                         '[tst] ' + 'x' * 1024 + 'y' * 1024 + '\n' +
                         '[tst] ' + 'z' * (1 << 14) + '\n'
                         )


@skipIf(os.uname()[0].lower() != 'linux' or not os.path.exists('/run/portod.socket'),
        "Porto is unavailable on this platform")
class TestPortoProcess(TestCase):
    def setUp(self):
        initialize_skycore()
        super(TestPortoProcess, self).setUp()

        self.log = logging.getLogger('tstproc')
        self.log.setLevel(logging.DEBUG)
        self.log.propagate = False
        self.io = StringIO()
        handler = logging.StreamHandler(self.io)
        self.log.addHandler(handler)

        from porto.api import Connection
        self.conn = Connection(timeout=20, auto_reconnect=False)
        self.conn.connect()

        self.portowatcher = PortoWatcher(self.conn, None)

    def tearDown(self):
        self.portowatcher.stop()
        super(TestPortoProcess, self).tearDown()

    def test_spawn_porto(self):
        with TempDir() as tempdir:
            try:
                uuid = genuuid()
                proc = PortoProcess(self.log, self.log, self.conn,
                                    watcher=self.portowatcher,
                                    args=['sh', '-c', 'sleep 1; echo 42'],
                                    cwd=tempdir,
                                    root_container='some/non/existent',
                                    uuid=uuid,
                                    username=getUserName(),
                                    )

                self.assertFalse(proc.wait(0))

                try:
                    proc.wait(5)
                finally:
                    proc.send_signal(9)
                    proc.wait(3)

                out = self.io.getvalue().split('\n')
                self.assertIn('[out] 42', out, out)
                self.assertEqual(proc.exitstatus['exited'], 1)
                self.assertEqual(proc.exitstatus['exitstatus'], 0)
            finally:
                self.conn.Destroy('some')


class TestLinerProcess(TestCase):
    def setUp(self):
        initialize_skycore()
        super(TestLinerProcess, self).setUp()

        self.log = logging.getLogger('tstproc')
        self.log.setLevel(logging.DEBUG)
        self.log.propagate = False
        self.io = StringIO()
        handler = logging.StreamHandler(self.io)
        self.log.addHandler(handler)

        def check_mailbox():
            for obj in mailbox().iterate():
                obj()

        self.mbcheck = gevent.spawn(check_mailbox)

    def tearDown(self):
        self.mbcheck.kill()
        super(TestLinerProcess, self).tearDown()

    def test_spawn_process(self):
        with TempDir() as tempdir:
            uuid = genuuid()
            proc = LinerProcess(self.log, self.log,
                                args=['sh', '-c', 'sleep 1; echo 42'],
                                cwd='/',
                                rundir=tempdir,
                                uuid=uuid,
                                )
            try:
                self.assertNotEqual(glob.glob(os.path.join(tempdir, '*' + uuid + '*')), [])
                proc.wait(5)
            finally:
                try:
                    proc.send_signal(9)
                except EnvironmentError as e:
                    if e.errno != errno.ESRCH:
                        raise
                proc.wait(3)

            out = self.io.getvalue()

            self.assertIn('[out] 42\n', out, out)
            self.assertEqual(proc.exitstatus['exited'], 1)
            self.assertEqual(proc.exitstatus['exitstatus'], 0)

    def test_kill_process(self):
        with TempDir() as tempdir:
            uuid = genuuid()
            proc = LinerProcess(self.log, self.log,
                                args=['sleep', '100'],
                                cwd='/',
                                rundir=tempdir,
                                uuid=uuid,
                                )
            try:
                proc.send_signal(9)
            except EnvironmentError as e:
                if e.errno != errno.ESRCH:
                    raise

            self.assertTrue(proc.wait(5), self.io.getvalue())

            self.assertEqual(proc.exitstatus['exited'], 0, self.io.getvalue())
            self.assertEqual(proc.exitstatus['signaled'], 1, self.io.getvalue())
            self.assertEqual(proc.exitstatus['termsig'], 9, self.io.getvalue())
            self.assertEqual(proc.exitstatus['liner_exited'], 1, self.io.getvalue())
            self.assertEqual(proc.exitstatus['liner_exitstatus'], 9, self.io.getvalue())

    def test_no_cwd(self):
        pids = []
        old_fork = os.fork

        def new_fork():
            pid = old_fork()
            if pid:
                pids.append(pid)
            return pid

        with TempDir() as tempdir:
            uuid = genuuid()
            cwd = os.path.join(tempdir, 'nonexistent')
            proc = None
            fork_mock = mock.patch('os.fork')
            with fork_mock as fm:
                fm.side_effect = new_fork
                try:
                    with self.assertRaises(Exception):
                        proc = LinerProcess(self.log, self.log,
                                            args=['sleep', '100'],
                                            cwd=cwd,
                                            rundir=tempdir,
                                            uuid=uuid,
                                            )
                    self.assertTrue(pids)
                    for pid in pids:
                        try:
                            os.waitpid(pid, os.WNOHANG)
                        except EnvironmentError as e:
                            if e.errno != errno.ECHILD:
                                raise
                finally:
                    if proc is not None:
                        proc.send_signal(9)

    def test_no_rundir(self):
        pids = []
        old_fork = os.fork

        def new_fork():
            pid = old_fork()
            if pid:
                pids.append(pid)
            return pid

        with TempDir() as tempdir:
            uuid = genuuid()
            rundir = os.path.join(tempdir, 'nonexistent')
            proc = None
            fork_mock = mock.patch('os.fork')
            with mock.patch.object(LinerProcess, 'LINER_START_TIMEOUT', 3), fork_mock as fm:
                fm.side_effect = new_fork
                try:
                    with self.assertRaises(Exception):
                        proc = LinerProcess(self.log, self.log,
                                            args=['sleep', '100'],
                                            cwd=tempdir,
                                            rundir=rundir,
                                            uuid=uuid,
                                            )
                    self.assertTrue(pids)
                    for pid in pids:
                        try:
                            os.waitpid(pid, 0)
                        except OSError as e:
                            if e.errno != errno.ECHILD:
                                raise
                finally:
                    if proc is not None:
                        proc.send_signal(9)


class TestSubprocess(TestCase):
    def setUp(self):
        initialize_skycore()
        super(TestSubprocess, self).setUp()

        self.log = logging.getLogger('tstproc')
        self.log.setLevel(logging.DEBUG)
        self.log.propagate = False

        def check_mailbox():
            for obj in mailbox().iterate():
                obj()

        self.mbcheck = gevent.spawn(check_mailbox)

    def tearDown(self):
        self.mbcheck.kill()
        super(TestSubprocess, self).tearDown()

    def test_spawn_subprocess(self):
        proc = Subprocess(self.log, self.log,
                          uuid='',
                          args=['/bin/sh', '-c', 'sleep 1; exit ${TESTVAR}'],
                          cwd='/',
                          env={'TESTVAR': '42'},
                          )
        try:
            proc.wait(5)
        finally:
            try:
                proc.send_signal(9)
            except EnvironmentError as e:
                if e.errno != errno.ESRCH:
                    raise
            proc.wait(3)

        self.assertEqual(proc.exitstatus['exited'], 1)
        self.assertEqual(proc.exitstatus['exitstatus'], 42)

    def test_kill_subprocess(self):
        proc = Subprocess(self.log, self.log,
                          uuid='',
                          args=['/bin/sleep', '100'],
                          cwd='/',
                          )
        try:
            proc.send_signal(9)
        except EnvironmentError as e:
            if e.errno != errno.ESRCH:
                raise

        self.assertTrue(proc.wait(5))
        self.assertTrue(proc.exitstatus)

        self.assertEqual(proc.exitstatus['exited'], 0, proc.exitstatus)
        self.assertEqual(proc.exitstatus['signaled'], 1, proc.exitstatus)
        self.assertEqual(proc.exitstatus['termsig'], 9, proc.exitstatus)


if __name__ == '__main__':
    main()
