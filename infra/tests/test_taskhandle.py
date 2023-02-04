import sys
import time
import threading

import six
import mock

from ya.skynet.util.unittest import main
from ya.skynet.util import pickle

from ya.skynet.services.cqudp import msgpackutils as msgpack
from ya.skynet.services.cqudp.client import task
from ya.skynet.services.cqudp.client.task import Task
from ya.skynet.services.cqudp.eggs import serialize
from ya.skynet.services.cqudp.transport.protocol import Protocol
from ya.skynet.services.cqudp.transport import envelope
from ya.skynet.services.cqudp.utils import genuuid, run_daemon, sleep
from ya.skynet.services.cqudp.server.taskhandle import TaskHandle
from ya.skynet.services.cqudp.server import taskhandle
from ya.skynet.services.cqudp.server.binaryprocess import __file__ as test_binary_path

from common import CQTest, UseNetlibus


is_arcadia = bool(getattr(sys, 'is_standalone_binary', False))


def make_runnable(obj):
    runnable = serialize(pickle.dumps(obj),
                         modules=['compat', 'common', __name__],
                         arcadia_binary=is_arcadia)
    options = {'exec_fn': 'arcadia_binary'} if is_arcadia else {}
    return runnable, options


class TestTaskHandle(CQTest):
    no_server = True

    def test_task_class(self):
        # check that we can still execute old Task object
        # TODO (torkve) remove later
        server = Protocol(port=0)

        th = TaskHandle(
            genuuid(),
            hostid=None,
            task=Task(*make_runnable(Runnable())),
            path=[('C', server.listenaddr())]
        )
        run_daemon(th.run)

        try:
            server.receive(block=True, timeout=20)  # 42
            server.receive(block=True, timeout=20)  # StopIteration
        except Protocol.Timeout:
            self.fail('Timed out')

    def test_simple(self):
        server = Protocol(port=0)

        th = TaskHandle(
            genuuid(),
            hostid=None,
            task=task(*make_runnable(Runnable())),
            path=[('C', server.listenaddr())]
        )
        run_daemon(th.run)

        try:
            server.receive(block=True, timeout=20)  # 42
            server.receive(block=True, timeout=20)  # StopIteration
        except Protocol.Timeout:
            self.fail('Timed out')

    def test_broken_runnable(self):
        server = Protocol(port=0)

        th = TaskHandle(
            genuuid(),
            hostid=None,
            task=task(*make_runnable(BrokenRunnable())),
            path=[('C', server.listenaddr())]
        )
        run_daemon(th.run)

        for _ in six.moves.xrange(5):
            try:
                cmd, env, iface = server.receive(block=True, timeout=20)
                msg = env.msgs[0]
                if msg['type'] == 'heartbeat':
                    continue
                self.assertEqual(msg['type'], 'result')
                result = pickle.loads(msg['result'])
                self.assertIsNone(result[0], result)
                self.assertIsInstance(result[1], Exception)
                return
            except Protocol.Timeout:
                self.fail('Timed out')

        self.fail('Timed out')

    @mock.patch('ya.skynet.services.cqudp.server.taskhandle.MIN_ORPHAN_TIMEOUT', new=1)
    @mock.patch('ya.skynet.services.cqudp.server.taskhandle.MIN_WAIT_MIN', new=0)
    @mock.patch('ya.skynet.services.cqudp.server.taskhandle.MIN_WAIT_MAX', new=0)
    def test_orphan(self):
        server = Protocol(port=0)

        tsk = task(*make_runnable(Infinite()))
        tsk['params'] = {'orphan_timeout': 1, 'wait_min': 0.1, 'wait_max': 0.2}
        th = TaskHandle(
            genuuid(),
            hostid=None,
            task=tsk,
            path=[('C', server.listenaddr())]
        )
        t = run_daemon(th.run)
        t.join(2.2)
        self.assertFalse(t.is_alive())

    @mock.patch('ya.skynet.services.cqudp.server.taskhandle.MIN_ORPHAN_TIMEOUT', new=3)
    def test_orphan_many_results(self):
        server = Protocol(port=0)
        tsk = task(*make_runnable(Thousands()))
        tsk['params'] = {'orphan_timeout': 3}
        th = TaskHandle(
            genuuid(),
            hostid=None,
            task=tsk,
            path=[('C', server.listenaddr())]
        )
        t = run_daemon(th.run)
        t.join(5.2)
        self.assertFalse(t.is_alive())

    def test_pipeline(self):
        server = Protocol(port=0)

        tsk = task(*make_runnable(Runnable()))
        tsk['params'] = {'pipeline': True}
        th = TaskHandle(
            genuuid(),
            hostid=None,
            task=tsk,
            path=[('C', server.listenaddr())]
        )
        run_daemon(th.run)

        for _ in six.moves.xrange(5):
            try:
                cmd, env, iface = server.receive(block=True, timeout=20)
                msg = env.msgs[0]
                if msg['type'] == 'heartbeat':
                    continue
                self.assertEqual(msg['type'], 'response')
                return
            except Protocol.Timeout:
                self.fail('Timed out')

        self.fail('Timed out')

    @mock.patch.object(Protocol, 'route', lambda *args, **kwargs: None)
    @mock.patch.object(Protocol, 'receive', autospec=True)
    def test_add_path(self, receive):
        tsk = task(*make_runnable(Runnable()))
        old_path = (('C', ('none', 12345)),)
        new_path = (('S', ('none', 12346)),)
        th = TaskHandle(
            genuuid(),
            hostid=None,
            task=tsk,
            path=old_path,
        )
        old_addpath = th._addpath
        with mock.patch.object(th, '_addpath') as addpath:
            try:
                msg = taskhandle.addpath_msg(th.uuid, new_path)
                env = envelope.Envelope(msg, [])
                ev = threading.Event()

                def receive_fn(mself, *args, **kwargs):
                    return 'route', env, None
                receive.side_effect = receive_fn

                def addpath_fn(*args, **kwargs):
                    old_addpath(*args, **kwargs)
                    ev.set()

                addpath.side_effect = addpath_fn

                run_daemon(th.run)

                ev.wait(25.0)
                self.assertEqual(len(th.paths), 2)
                self.assertIn(new_path, th.paths)
            finally:
                th._stop()
                if th.protocol is not None:
                    th.protocol.shutdown()

    @mock.patch.object(Protocol, 'route', lambda *args, **kwargs: None)
    @mock.patch.object(Protocol, 'receive', autospec=True)
    @mock.patch.object(TaskHandle, '_is_about_to_orphan', lambda *args: True)
    def test_add_direct_path(self, receive):
        tsk = task(*make_runnable(Runnable()))
        addr = ('C', ('none', 12345))
        intr = ('I', ('none-intermediate', 12345))
        old_path = (addr, intr)
        th = TaskHandle(
            genuuid(),
            hostid=None,
            task=tsk,
            path=old_path,
        )
        try:
            def receive_fn(mself, *args, **kwargs):
                time.sleep(3.0)
                raise Protocol.Timeout()

            receive.side_effect = receive_fn

            t = run_daemon(th.run)
            t.join(15 if is_arcadia else 1.2)
            self.assertIn(old_path, th.paths)
            self.assertIn((addr,), th.paths)
        finally:
            th._stop()
            if th.protocol is not None:
                th.protocol.shutdown()

    @mock.patch('ya.skynet.services.cqudp.server.exec_functions.arcadia._download_binary')
    def test_proc_with_exec(self, download_binary):
        path = sys.executable if is_arcadia else test_binary_path
        if path.endswith('pyc'):
            path = path[:-1]
        download_binary.side_effect = lambda rbtorrent: path
        tsk = task(msgpack.dumps({
            'entry_point': 'ya.skynet.services.cqudp.server.binaryprocess:main' if is_arcadia else '',
            'resource_id': '',
            'object': pickle.dumps(lambda: 42),
        }), {
            'exec_fn': 'arcadia_binary'
        })

        server = Protocol(port=0)

        th = TaskHandle(
            genuuid(),
            hostid=None,
            task=tsk,
            path=[('C', server.listenaddr())]
        )
        run_daemon(th.run)

        try:
            server.receive(block=True, timeout=20)  # 42
            server.receive(block=True, timeout=20)  # StopIteration
        except Protocol.Timeout:
            self.fail('Timed out')


class TestTaskHandleNetlibus(UseNetlibus, TestTaskHandle):
    pass


class Runnable:
    def __init__(self):
        self.forty_two = 42

    def __call__(self):
        yield self.forty_two


class Infinite(object):
    def __call__(self):
        while True:
            sleep(1000)


class Thousands(object):
    def __call__(self):
        for i in six.moves.xrange(5000):
            yield i


class BrokenRunnable:
    def __init__(self):
        self.options = {}

    def __call__(self):
        raise Exception()


if __name__ == '__main__':
    main()
