from pickle import loads
import tempfile

try:
    from ya.skynet.util.unittest import TestCase
except ImportError:
    from kernel.util.unittest import TestCase

from ya.skynet.services.cqudp.server.processhandle import ProcessHandle
from ya.skynet.services.cqudp.utils import sleep


class TestProcessHandle(TestCase):
    def test_simple(self):
        proc = ProcessHandle('1', {}, tempfile.gettempdir())

        def fn():
            proc.send('init', True)
            yield 42
            raise Exception('43')

        proc.target = fn
        proc.fork()
        with proc:
            d = proc.recv(1)
            self.assertEqual((42, None), loads(d[1]))

            d = proc.recv(1)
            self.assertIsInstance(loads(d[1])[1], Exception)

    def test_terminate(self):
        proc = ProcessHandle('1', {}, tempfile.gettempdir())

        def fn():
            proc.send('init', True)
            while True:
                sleep(1000)

        proc.target = fn
        proc.fork()
        with proc:
            pass

        self.assertNotIn(proc.join(1.0), (None, 0))

        proc = ProcessHandle('1', {}, tempfile.gettempdir())

        def fn():
            proc.send('init', True)
            while True:
                sleep(1000)

        proc.target = fn
        proc.fork()
        with proc:
            sleep(1.0)  # let it definitely start sleeping

        self.assertNotIn(proc.join(1.0), (None, 0))
