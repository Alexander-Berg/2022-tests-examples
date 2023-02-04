from common import CQTest, UseNetlibus

import os
import sys
import socket
import gevent
import random

from ya.skynet.services.cqudp.client.daemon import Daemon
from ya.skynet.services.cqudp.daemon_client import DaemonClient, DaemonSession
from ya.skynet.util.unittest import main, skipIf


def simple_task():
    return 42
simple_task.marshaledModules = ['compat', 'common', __name__]


class PipeTask(object):
    marshaledModules = ['compat', 'common', __name__]

    def __init__(self, pipe):
        self.pipe = pipe

    def __call__(self):
        self.pipe.put(42)
        return self.pipe.get()


@skipIf(getattr(sys, 'is_standalone_binary', False), 'not applicable to arcadia')
class TestClientDaemonClient(UseNetlibus, CQTest):
    def test_simple_run(self):
        path = '\0%s%d' % (''.join(random.choice('abcdefghijklmnopqrstuvwxyz')
                                   for _ in xrange(10)),
                           os.getpid())
        self.server.taskmgr.verify = False

        daemon = Daemon(path, allow_no_keys=True)

        try:
            gevent.spawn(daemon.serve_forever)
            dst = (socket.getfqdn(), self.port)

            with DaemonClient(path, gevent=True) as client:
                with client.run([dst], simple_task) as session:
                    self.assertIsInstance(session, DaemonSession)
                    results = list(session.wait(10))
                    self.assertEqual(len(results), 1)

                    res = results[0]
                    self.assertEqual(len(res), 3)
                    self.assertEqual(res[0], dst)
                    self.assertEqual(res[1], 42)
                    self.assertIsNone(res[2], res[2])
        finally:
            daemon.stop_server()

    def test_pipe_run(self):
        path = '\0%s%d' % (''.join(random.choice('abcdefghijklmnopqrstuvwxyz')
                                   for _ in xrange(10)),
                           os.getpid())
        self.server.taskmgr.verify = False

        daemon = Daemon(path, allow_no_keys=True)

        try:
            gevent.spawn(daemon.serve_forever)
            dst = (socket.getfqdn(), self.port)

            with DaemonClient(path, gevent=True) as client:
                pipe = client.createPipe()
                with client.run([dst], PipeTask(pipe)) as session:
                    self.assertIsInstance(session, DaemonSession)
                    self.assertEqual(pipe.get(block=True, timeout=10.0), (dst, 42))
                    pipe.put([dst], 33)
                    results = list(session.wait(10))
                    self.assertEqual(len(results), 1)

                    res = results[0]
                    self.assertEqual(len(res), 3)
                    self.assertEqual(res[0], dst)
                    self.assertEqual(res[1], 33)
                    self.assertIsNone(res[2], res[2])
        finally:
            daemon.stop_server()


if __name__ == '__main__':
    main()
