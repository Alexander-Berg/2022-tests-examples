import os
import socket
from gevent import monkey

from common import CQTest

from ya.skynet.util.unittest import main, skipIf
from ya.skynet.library.auth.tempkey import TempKey


@skipIf(not os.getenv('USE_GEVENT', False), 'gevent is patched only if USE_GEVENT is set')
class TestGevent(CQTest):
    def setUp(self):
        # unfortunately gevent doesn't support unpatching
        monkey.patch_all()
        super(TestGevent, self).setUp()

    def test_pipe(self):
        client = self.create_client(loglevel='DEBUG')
        with TempKey(client.signer, self.server.taskmgr.auth):
            pipe = client.createPipe()
            task = PipeTask(pipe)
            dst = (socket.getfqdn(), self.port)
            with client.run([dst], task) as session:
                result = pipe.get()
                self.assertEqual(result[1], 0)
                pipe.put(dst, result[1] + 1)
                result = pipe.get()
                self.assertEqual(result[1], 1)

                results = list(session.wait(3))
                self.assertEqual(len(results), 1)
                h, r, e = results[0]
                self.assertEqual(h, socket.getfqdn())
                self.assertIsNone(r)
                self.assertIsNone(e)


class PipeTask(object):
    marshaledModules = ['compat', 'common', __name__]

    def __init__(self, pipe):
        self.pipe = pipe

    def __call__(self):
        self.pipe.put(0)
        r = self.pipe.get()
        self.pipe.put(r)

if __name__ == '__main__':
    main()
