import six
import socket
import gevent
import gevent.select

from common import CQTest, UseNetlibus

from ya.skynet.util.unittest import main
from ya.skynet.library.auth.tempkey import TempKey
from ya.skynet.services.cqudp.exceptions import CQueueRuntimeError
from ya.skynet.services.cqudp.utils import sleep


class TestApi(CQTest):
    def testGevent(self):
        client = self.create_client(select_function=gevent.select.select)
        with TempKey(client.signer, self.server.taskmgr.auth):
            with client.run([(socket.getfqdn(), self.port)], sleep_task) as session:
                greenlet = gevent.spawn(lambda: gevent.sleep(0.1))

                results = list(session.wait())
                self.assertEqual(len(results), 1)
                self.assertEqual(results[0][1], 0)
                self.assertIsNone(results[0][2])
                self.assertTrue(greenlet.ready())

    def testPoll(self):
        client = self.create_client()
        with TempKey(client.signer, self.server.taskmgr.auth):
            for rm, task in (
                ('run', sleep_task),
                ('iter', sleep_iter_task),
            ):
                session = getattr(client, rm)([(socket.getfqdn(), self.port)], task)

                self.assertIsInstance(session.id, str)
                self.assertGreater(len(session.id), 0)

                with session:
                    self.assertTrue(session.running)
                    self.assertListEqual(list(session.poll(0)), [])
                    self.assertGreater(len(list(session.poll())), 0)

                # This is how it works for now.
                # Not sure if this is good,
                # but tests should reflect actual status.
                sleep(1)

                self.assertFalse(session.running)

    def testPing(self):
        client = self.create_client()
        session = client.ping([(socket.getfqdn(), self.port)])

        self.assertIsInstance(session.id, str)
        self.assertGreater(len(session.id), 0)

        with session:
            self.assertTrue(session.running)
            results = list(session.wait())
            self.assertEqual(len(results), 1)

        self.assertFalse(session.running)

    def testUnicode(self):
        client = self.create_client()
        session = client.ping([(six.u(socket.getfqdn()), self.port)])
        self.assertIsInstance(session.id, str)
        self.assertGreater(len(session.id), 0)

        with session:
            self.assertTrue(session.running)
            results = list(session.wait())
            self.assertEqual(len(results), 1)

        self.assertFalse(session.running)

        session = client.ping([u'%s:%d' % (socket.getfqdn(), self.port)])
        self.assertIsInstance(session.id, str)
        self.assertGreater(len(session.id), 0)

        with session:
            self.assertTrue(session.running)
            results = list(session.wait())
            self.assertEqual(len(results), 1)

        self.assertFalse(session.running)

    def testUncallables(self):
        client = self.create_client()
        self.assertRaises(CQueueRuntimeError, client.run, [(socket.getfqdn(), self.port)], Uncallable())
        self.assertRaises(CQueueRuntimeError, client.iter, [(socket.getfqdn(), self.port)], Uncallable())

    def testNoHosts(self):
        with self.create_client() as client:
            with TempKey(client.signer, self.server.taskmgr.auth):
                self.assertRaises(CQueueRuntimeError, client.run, [], sleep_task)

    def testNoKeys(self):
        with self.create_client() as client:
            self.assertRaises(CQueueRuntimeError, client.run, [(socket.getfqdn(), self.port)], sleep_task)
            with TempKey(client.signer, self.server.taskmgr.auth):
                self.assertIsNotNone(client.run([(socket.getfqdn(), self.port)], sleep_task))


class TestApiNetlibus(UseNetlibus, TestApi):
    pass


class Uncallable(object):
    pass


def sleep_iter_task():
    sleep(1)
    yield 0
sleep_iter_task.marshaledModules = [__name__, 'common', 'compat']


def sleep_task():
    sleep(1)
    return 0
sleep_task.marshaledModules = [__name__, 'common', 'compat']


if __name__ == '__main__':
    main()
