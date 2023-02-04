import socket
import os
import pwd
import six
import sys
import signal

from common import CQTest, UseNetlibus

from ya.skynet.util.unittest import main, skipIf
from ya.skynet.util.logging import getLogger, ERROR, Handler
from ya.skynet.util.sys.user import getUserName
from ya.skynet.library.auth.tempkey import TempKey

from ya.skynet.services.cqudp.server.processhandle import Signalled, CommunicationError
from ya.skynet.services.cqudp.client import Timeout
from ya.skynet.services.cqudp.utils import sleep
from ya.skynet.services.cqudp.client.cqueue import CqueueSession
from ya.skynet.services.cqudp.rpc.loghandler import CQRemoteLogHandler


class TestExecution(CQTest):
    def testIter(self):
        for i in six.moves.xrange(iterations):
            client = self.create_client()
            with TempKey(client.signer, self.server.taskmgr.auth):
                dst = (socket.getfqdn(), self.port)
                with client.iter([dst], task_iter) as session:
                    results = list(session.wait(10))
                    self.assertEqual(len(results), 2)
                    h, r, e = results[0]
                    self.assertEqual(h, dst)
                    self.assertEqual(r, next(task_iter()))
                    self.assertIs(e, None)
                    h, r, e = results[1]
                    self.assertEqual(h, dst)
                    self.assertIs(r, None)
                    self.assertIsInstance(e, StopIteration)
                    self.assertFalse(session.running)

    def testRun(self):
        for i in six.moves.xrange(iterations):
            client = self.create_client()
            with TempKey(client.signer, self.server.taskmgr.auth):
                dst = '{}:{}'.format(socket.getfqdn(), self.port)
                with client.run(dst, task_run) as session:
                    results = list(session.wait())
                    self.assertEqual(len(results), 1)
                    h, r, e = results[0]
                    self.assertEqual(h, dst)
                    self.assertIs(e, None)
                    self.assertEqual(r, task_run())
                    self.assertFalse(session.running)

    def testRunMultipart(self):
        client = self.create_client(multipart=True, multipart_size=100000)
        with TempKey(client.signer, self.server.taskmgr.auth):
            dst = (socket.getfqdn(), self.port)
            with client.run([dst], task_run) as session:
                results = list(session.wait())
                self.assertEqual(len(results), 1)
                h, r, e = results[0]
                self.assertEqual(h, dst)
                self.assertIs(e, None)
                self.assertEqual(r, task_run())
                self.assertFalse(session.running)

    def testRunMultipartWithArguments(self):
        client = self.create_client(multipart=True, multipart_size=100000)
        with TempKey(client.signer, self.server.taskmgr.auth):
            host = socket.getfqdn(), self.port

            results = list(client.run([host], task_plus_42, [42]).wait())
            self.assertEqual(results[0][1], 84)

            results = list(client.iter([host], task_plus_42_iter, [SomeInnerClass()]).wait())
            self.assertEqual(results[0][1], 84)

    def testRunMultipartWithLargePartSize(self):
        client = self.create_client(multipart=True, multipart_size=1000000000)
        with TempKey(client.signer, self.server.taskmgr.auth):
            host = socket.getfqdn(), self.port

            results = list(client.run([host], task_run).wait())
            self.assertEqual(results[0][1], task_run())
            self.assertEqual(results[0][0], host)
            self.assertIsNone(results[0][2])

    def testLogs(self):
        log = getLogger('test.remotelog')
        log.setLevel(ERROR)
        log.propagate = False
        log_handler = StringIOLogHandler()
        log.addHandler(log_handler)

        for x in six.moves.xrange(iterations):
            client = self.create_client()
            with TempKey(client.signer, self.server.taskmgr.auth):
                dst = (socket.getfqdn(), self.port)
                with client.iter([dst], task_log) as session:
                    results = list(session.wait())
                    self.assertEqual(len(results), 3, results)
                    h, r, e = results[0]
                    self.assertEqual(h, dst)
                    self.assertIs(e, None)
                    self.assertEqual(r, 0)
                    h, r, e = results[1]
                    self.assertEqual(h, dst)
                    self.assertIs(e, None)
                    self.assertEqual(r, 1)
                    h, r, e = results[2]
                    self.assertEqual(h, dst)
                    self.assertIs(r, None)
                    self.assertIsInstance(e, StopIteration)
                    self.assertFalse(session.running)
                    self.assertEqual(log_handler.val(), '79A')
                    log_handler.clear()

    def testFork(self):
        for i in range(iterations):
            client = self.create_client()
            with TempKey(client.signer, self.server.taskmgr.auth):
                dst = (socket.getfqdn(), self.port)
                with client.iter([dst], task_fork) as session:
                    results = list(session.wait())
                    self.assertEqual(len(results), 3, results)

                    h, r, e = results[0]
                    self.assertEqual(h, dst)
                    self.assertIs(e, None)
                    self.assertEqual(r, 1)

                    h, r, e = results[1]
                    self.assertEqual(h, dst)
                    self.assertIs(e, None)
                    self.assertEqual(r, 2)

                    h, r, e = results[2]
                    self.assertEqual(h, dst)
                    self.assertIs(r, None)
                    self.assertIsInstance(e, StopIteration)

                    self.assertFalse(session.running)

    def testNonFqdn(self):
        client = self.create_client()
        with TempKey(client.signer, self.server.taskmgr.auth):
            hosts = ['127.0.0.1']
            dsts = [(host, self.port) for host in hosts]
            with client.run(dsts, task_run) as session:
                results = list(session.wait())
                self.assertEqual(len(results), len(hosts))
                for h, r, e in results:
                    self.assertIn(h, dsts)
                    self.assertIs(e, None)
                    self.assertEqual(r, task_run())
                self.assertFalse(session.running)

    def testShellSession(self):
        client = self.create_client()
        with TempKey(client.signer, self.server.taskmgr.auth):
            dst = '{}:{}'.format(socket.getfqdn(), self.port)
            hosts = [dst]
            session = CqueueSession(hosts, client, client._create_custom_session(hosts, 'whoami', None, getUserName(), None), remote_object=None)
            with session:
                results = list(session.wait())
                self.assertEqual(len(results), 1)
                h, r, e = results[0]
                self.assertEqual(h, dst)
                self.assertIsNone(e)
                self.assertEqual(len(r), 3)
                self.assertEqual(r[0].strip(), six.b(getUserName()))  # stdout
                self.assertFalse(r[1])  # stderr
                self.assertEqual(r[2], 0)  # returncode
                self.assertFalse(session.running)

    def testCustomFunction(self):
        client = self.create_client()
        with TempKey(client.signer, self.server.taskmgr.auth):
            dst = '{}:{}'.format(socket.getfqdn(), self.port)
            hosts = [dst]
            session = CqueueSession(hosts, client, client._create_custom_session(hosts, None, None, None, None, session_type='dummy'), remote_object=None)
            with session:
                results = list(session.wait())
                self.assertEqual(len(results), 1)
                h, r, e = results[0]
                self.assertEqual(h, dst)
                self.assertIsNone(e)
                self.assertIs(r, True)
                self.assertFalse(session.running)

    @skipIf(os.getuid() != 0, "Can be tested only with root privileges")
    def testCustomUser(self):
        client = self.create_client()
        self.server.taskmgr.verify = False
        with TempKey(client.signer, self.server.taskmgr.auth):
            dst = (socket.getfqdn(), self.port)
            with client.run([dst], task_user) as session:
                results = list(session.wait())
                self.assertEqual(len(results), 1)
                h, r, e = results[0]
                self.assertEqual(h, dst)
                self.assertIs(e, None)
                self.assertEqual(r, pwd.getpwnam(task_user.osUser).pw_uid)
                self.assertFalse(session.running)

    def testCustomResultType(self):
        client = self.create_client()
        self.server.taskmgr.verify = False
        with TempKey(client.signer, self.server.taskmgr.auth):
            dst = (socket.getfqdn(), self.port)
            with client.run([dst], task_custom_type) as session:
                results = list(session.wait())
                self.assertEqual(len(results), 1)
                h, r, e = results[0]
                self.assertEqual(h, dst)
                self.assertIs(e, None)
                self.assertIsInstance(r, CustomResultType)
                self.assertFalse(session.running)

    def testInternalException(self):
        client = self.create_client()
        self.server.taskmgr.verify = False
        with TempKey(client.signer, self.server.taskmgr.auth):
            dst = (socket.getfqdn(), self.port)
            with client.run([dst], task_self_killing) as session:
                results = list(session.wait())
                self.assertEqual(len(results), 1)
                h, r, e = results[0]
                self.assertEqual(h, dst)
                self.assertIs(r, None)
                self.assertIsInstance(e, Signalled)
                self.assertEqual(e.signal, signal.SIGKILL)
                self.assertFalse(session.running)

    def testBaseException(self):
        client = self.create_client()
        self.server.taskmgr.verify = False
        with TempKey(client.signer, self.server.taskmgr.auth):
            dst = (socket.getfqdn(), self.port)
            with client.run([dst], task_base_exception) as session:
                results = list(session.wait())
                self.assertEqual(len(results), 1)
                h, r, e = results[0]
                self.assertEqual(h, dst)
                self.assertIs(r, None)
                self.assertIsInstance(e, _TestBaseException)
                self.assertFalse(session.running)

    def testChildSocketError(self):
        client = self.create_client()
        self.server.taskmgr.verify = False
        with TempKey(client.signer, self.server.taskmgr.auth):
            dst = (socket.getfqdn(), self.port)
            with client.iter([dst], task_self_socket_killing) as session:
                results = list(session.wait())
                self.assertEqual(len(results), 2, results)
                h, r, e = results[0]
                self.assertEqual(h, dst)
                self.assertEqual(r, 1)
                self.assertIsNone(e)
                h, r, e = results[1]
                self.assertEqual(h, dst)
                self.assertIsNone(r)
                self.assertIsInstance(e, CommunicationError)
                self.assertFalse(session.running)

    def testBuiltinFunction(self):
        client = self.create_client()
        self.server.taskmgr.verify = False
        with TempKey(client.signer, self.server.taskmgr.auth):
            dst = (socket.getfqdn(), self.port)
            with client.run([dst], socket.getfqdn) as session:
                results = list(session.wait())
                self.assertEqual(len(results), 1)
                h, r, e = results[0]
                self.assertEqual(h, dst)
                self.assertIs(e, None)
                self.assertEqual(r, socket.getfqdn())
                self.assertFalse(session.running)

    @skipIf(sys.version_info >= (3, 4)
            and sys.version_info <= (3, 6),
            'Python has broken cpickle function serialization in this version')
    def testLambda(self):
        client = self.create_client()
        self.server.taskmgr.verify = False
        with TempKey(client.signer, self.server.taskmgr.auth):
            dst = (socket.getfqdn(), self.port)
            with client.run([dst], lambda: socket.getfqdn()) as session:
                results = list(session.wait())
                self.assertEqual(len(results), 1)
                h, r, e = results[0]
                self.assertEqual(h, dst)
                self.assertIs(e, None)
                self.assertEqual(r, socket.getfqdn())
                self.assertFalse(session.running)

    def testSessionTimeout(self):
        client = self.create_client()
        self.server.taskmgr.verify = False
        with TempKey(client.signer, self.server.taskmgr.auth):
            dst = (socket.getfqdn(), self.port)
            with client.run([dst], task_timing_out) as session:
                results = list(session.wait())
                self.assertEqual(len(results), 1)
                h, r, e = results[0]
                self.assertEqual(h, dst)
                self.assertIsNone(r)
                self.assertIsInstance(e, Timeout)
                self.assertFalse(session.running)

    def testOrphanTimeout(self):
        client = self.create_client()
        self.server.taskmgr.verify = False
        with TempKey(client.signer, self.server.taskmgr.auth):
            dst = (socket.getfqdn(), 1)
            with client.run([dst], task_orphaning) as session:
                results = list(session.wait())
                self.assertEqual(len(results), 1)
                h, r, e = results[0]
                self.assertEqual(h, dst)
                self.assertIsNone(r)
                self.assertIsInstance(e, Timeout)
                self.assertFalse(session.running)

    def testNoKeys(self):
        client = self.create_client()
        self.assertRaises(RuntimeError, client.run, [('localhost', self.port)], task_run)

    @skipIf(not socket.has_ipv6, "Can be tested only if at least two interfaces available")
    def testUnavailableInterface(self):
        for ip in ('127.0.0.1', '::1'):
            server = None
            try:
                port, server = self.create_server(ip)
                server.taskmgr.verify = False

                client = self.create_client()
                with TempKey(client.signer, server.taskmgr.auth):
                    dst = 'localhost:{}'.format(port)
                    with client.run([dst], task_run) as session:
                        results = list(session.wait())
                        self.assertEqual(len(results), 1)
                        h, r, e = results[0]
                        self.assertEqual(h, dst)
                        self.assertIsNone(e)
                        self.assertEqual(r, task_run())
                        self.assertFalse(session.running)
            finally:
                if server:
                    server.shutdown()

    def testSshAgent(self):
        client = self.create_client()
        with TempKey(client.signer, self.server.taskmgr.auth):
            dst = (socket.getfqdn(), self.port)
            with client.run([dst], task_ssh_agent) as session:
                results = list(session.wait(15))
                self.assertEqual(len(results), 1)
                h, r, e = results[0]
                self.assertEqual(h, dst)
                self.assertIsNone(e)
                self.assertEqual(r, 0)

    def testPipe(self):
        client = self.create_client(rpc_confirmations=True)
        with TempKey(client.signer, self.server.taskmgr.auth):
            dst = (socket.getfqdn(), self.port)
            task = PipeTask(client.createPipe())
            with client.run([dst], task) as session:
                for i in range(3):
                    task.pipe.put([dst], i * 10)

                results = list(session.wait())
                self.assertEqual(len(results), 1)
                h, r, e = results[0]
                self.assertEqual(h, dst)
                self.assertIsNone(e)
                self.assertIsNone(r)

                for i in range(3):
                    self.assertEqual(task.pipe.get(timeout=2), (dst, i * 20))

    def testPipeline(self):
        client = self.create_client(pipeline=True)
        with TempKey(client.signer, self.server.taskmgr.auth):
            with client.iter([(socket.getfqdn(), self.port)], task_100_iter) as session:
                results = list(session.wait())
            self.assertEqual(len(results), 101)

    def testHostdata(self):
        client = self.create_client()
        with TempKey(client.signer, self.server.taskmgr.auth):
            host = socket.getfqdn(), self.port

            with client.run([host], task_plus_42, [42]) as session:
                results = list(session.wait())
            self.assertEqual(results[0][1], 84)

            with client.iter([host], task_plus_42_iter, [SomeInnerClass()]) as session:
                results = list(session.wait())
            self.assertEqual(results[0][1], 84)

    def testNonPicklable(self):
        client = self.create_client()
        with TempKey(client.signer, self.server.taskmgr.auth):
            host = socket.getfqdn(), self.port

            with client.run([host], task_nonpicklable) as session:
                results = list(session.wait())

            self.assertIsNone(results[0][1])
            self.assertIsInstance(results[0][2], Exception)
            self.assertEqual(results[0][2].__class__.__name__, 'CQueueRuntimeError')

    def testNonUnpicklable(self):
        client = self.create_client(allow_unsafe_unpickle_i_am_sure=False)
        with TempKey(client.signer, self.server.taskmgr.auth):
            host = socket.getfqdn(), self.port

            with client.run([host], task_nonunpicklable) as session:
                results = list(session.wait())

            self.assertIsNone(results[0][1])
            self.assertIsInstance(results[0][2], Exception)
            self.assertEqual(results[0][2].__class__.__name__, 'CQueueRuntimeError')

            with client.run([host], task_nonunpicklable2) as session:
                results = list(session.wait())

            self.assertIsNone(results[0][1])
            self.assertIsInstance(results[0][2], Exception)
            self.assertEqual(results[0][2].__class__.__name__, 'CQueueRuntimeError')

        client = self.create_client(allow_unsafe_unpickle_i_am_sure=True)
        with TempKey(client.signer, self.server.taskmgr.auth):
            host = socket.getfqdn(), self.port

            with client.run([host], task_nonunpicklable) as session:
                results = list(session.wait())

            self.assertIsInstance(results[0][1], NonUnpicklable)
            self.assertIsNone(results[0][2])

            with client.run([host], task_nonunpicklable2) as session:
                results = list(session.wait())

            self.assertEqual(results[0][1], os.path.join("a", "b"))
            self.assertIsNone(results[0][2])

        client = self.create_client(allow_unsafe_unpickle_i_am_sure=False)
        with TempKey(client.signer, self.server.taskmgr.auth):
            host = socket.getfqdn(), self.port
            client.register_safe_unpickle(NonUnpicklable.__module__, NonUnpicklable.__name__)

            with client.run([host], task_nonunpicklable) as session:
                results = list(session.wait())

            self.assertIsInstance(results[0][1], NonUnpicklable)
            self.assertIsNone(results[0][2])

            client.register_safe_unpickle('posixpath', 'join')

            with client.run([host], task_nonunpicklable2) as session:
                results = list(session.wait())

            self.assertEqual(results[0][1], os.path.join("a", "b"))
            self.assertIsNone(results[0][2])

        client = self.create_client(allow_unsafe_unpickle_i_am_sure=False)
        with TempKey(client.signer, self.server.taskmgr.auth):
            host = socket.getfqdn(), self.port
            client.register_safe_unpickle(obj=NonUnpicklable)

            with client.run([host], task_nonunpicklable) as session:
                results = list(session.wait())

            self.assertIsInstance(results[0][1], NonUnpicklable)
            self.assertIsNone(results[0][2])

            client.register_safe_unpickle(obj=os.path.join)

            with client.run([host], task_nonunpicklable2) as session:
                results = list(session.wait())

            self.assertEqual(results[0][1], os.path.join("a", "b"))
            self.assertIsNone(results[0][2])


class TestExecutionNetlibus(UseNetlibus, TestExecution):
    pass


def task_nonpicklable():
    raise NonPicklable(42)
task_nonpicklable.marshaledModules = [__name__, 'common', 'compat']


class NonPicklable(Exception):
    def __init__(self, a):
        self.a = a

    def __reduce__(self):
        return self.__class__, ()


class NonUnpicklable(object):
    pass


def task_nonunpicklable():
    return NonUnpicklable()
task_nonunpicklable.marshaledModules = [__name__, 'common', 'compat']


class NonUnpicklable2(object):
    def __reduce__(self, *args, **kwargs):
        return os.path.join, ("a", "b")


def task_nonunpicklable2():
    return NonUnpicklable2()
task_nonunpicklable2.marshaledModules = [__name__, 'common', 'compat']


def task_iter():
    yield 42
task_iter.marshaledModules = [__name__, 'common', 'compat']


def task_run():
    return 42
task_run.marshaledModules = [__name__, 'common', 'compat']


def task_plus_42(arg):
    return arg + 42
task_plus_42.marshaledModules = [__name__, 'common', 'compat']


class SomeInnerClass:
    def get(self):
        return 42


def task_plus_42_iter(arg):
    yield arg.get() + 42
task_plus_42_iter.marshaledModules = [__name__, 'common', 'compat']


def task_user():
    return os.getuid()
task_user.osUser = 'daemon'  # uid 1 in common system
task_user.marshaledModules = [__name__, 'common', 'compat']


def task_self_killing():
    os.kill(os.getpid(), signal.SIGKILL)
task_self_killing.marshaledModules = [__name__, 'common', 'compat']


def task_self_socket_killing():
    yield 1
    os.closerange(0, 50)
    sleep(100)
task_self_socket_killing.marshaledModules = [__name__, 'common', 'compat']

iterations = 10


def task_timing_out():
    while True:
        sleep(1000)
task_timing_out.sessionTimeout = 0.5
task_timing_out.marshaledModules = [__name__, 'common', 'compat']


def task_orphaning():
    return 42
task_orphaning.orphanTimeout = 1
task_orphaning.marshaledModules = [__name__, 'common', 'compat']


class CustomResultType(object):
    allow_unpickle = True


def task_custom_type():
    return CustomResultType()
task_custom_type.marshaledModules = [__name__, 'common', 'compat']


def task_fork():
    yield 1
    os.fork()
    yield 2
task_fork.marshaledModules = [__name__, 'common', 'compat']


def task_ssh_agent():
    return os.system("ssh-add -l >/dev/null")
task_ssh_agent.forwardSshAgent = True
task_ssh_agent.marshaledModules = [__name__, 'common', 'compat']


def task_log():
    log = getLogger('test.remotelog')
    log.addHandler(CQRemoteLogHandler(ERROR))
    log.error('7')
    log.debug('8')
    yield 0
    log.error('9')
    yield 1
    log.error('A')
task_log.marshaledModules = [__name__, 'common', 'compat']


def task_100_iter():
    for i in six.moves.xrange(100):
        yield i
        # sleep(0.5)
task_100_iter.marshaledModules = [__name__, 'common', 'compat']


class StringIOLogHandler(Handler):
    def __init__(self):
        Handler.__init__(self)
        self._io = six.moves.cStringIO()

    def emit(self, record):
        msg = record.msg
        if record.args:
            msg = msg % record.args
        self._io.write(msg)

    def val(self):
        return self._io.getvalue()

    def clear(self):
        self._io.close()
        self._io = six.moves.cStringIO()


class PipeTask(object):
    marshaledModules = [__name__, 'common', 'compat']

    def __init__(self, pipe):
        self.pipe = pipe

    def __call__(self):
        for _ in six.moves.xrange(3):
            d = self.pipe.get(timeout=10)
            self.pipe.put(d * 2)


class _TestBaseException(BaseException):
    allow_unpickle = True


def task_base_exception():
    raise _TestBaseException()
task_base_exception.marshaledModules = [__name__, 'common', 'compat']


if __name__ == '__main__':
    main()
