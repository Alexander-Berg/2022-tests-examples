import os
import socket
import resource

from common import CQTest, UseNetlibus

from ya.skynet.util.unittest import main, skipIf

from ya.skynet.library.auth.tempkey import TempKey
from ya.skynet.services.cqudp.client.poll import Poll


class TestPoll(CQTest):
    def test_poll_run(self):
        client = self.create_client()
        with TempKey(client.signer, self.server.taskmgr.auth):
            dst = '{}:{}'.format(socket.getfqdn(), self.port)
            with client.run([dst], task_run) as session:
                poll = Poll()
                poll.add(session)
                ready = list(poll.poll(25))
                self.assertIn(session, ready)
                ready = list(poll.poll(0))
                self.assertIn(session, ready)

                results = list(session.poll(0))
                self.assertTrue(results)
                results.extend(session.wait(2))
                self.assertFalse(list(poll.poll(0)))

    def testPollPipe(self):
        client = self.create_client()
        with TempKey(client.signer, self.server.taskmgr.auth):
            dst = '{}:{}'.format(socket.getfqdn(), self.port)

            pipe = client.createPipe()
            task = PipeTask(pipe)
            with client.run([dst], task) as session:
                poll = Poll([pipe, session])
                ready = poll.poll(0)
                self.assertFalse(ready)
                ready = poll.poll(25)
                self.assertIn(pipe, ready)
                self.assertEqual(pipe.get(block=False)[1], 42)
                ready = poll.poll(0)
                self.assertNotIn(pipe, ready)
                if session not in ready:
                    ready = poll.poll(5)
                self.assertIn(session, ready)
                self.assertNotIn(pipe, ready)

                results = list(session.poll(0))
                self.assertTrue(results)
                results.extend(session.wait(2))
                self.assertFalse(poll.poll(0))

    @skipIf(resource.getrlimit(resource.RLIMIT_NOFILE)[0] < 1030, "To test poll we need maxfd >= 1024")
    def testHighFd(self):
        fds = []
        while not fds or fds[-1] < 1024:
            fds.extend(os.pipe())

        try:
            client = self.create_client()
            with TempKey(client.signer, self.server.taskmgr.auth):
                dst = '{}:{}'.format(socket.getfqdn(), self.port)
                with client.run([dst], task_run) as session:
                    self.assertTrue(list(session.wait(25)))
        finally:
            for fd in fds:
                os.close(fd)


class TestPollNetlibus(UseNetlibus, TestPoll):
    pass


def task_run():
    return 42
task_run.marshaledModules = [__name__, 'common', 'compat']


class PipeTask(object):
    marshaledModules = [__name__, 'common', 'compat']

    def __init__(self, pipe):
        self.pipe = pipe

    def __call__(self):
        self.pipe.put(42)
        return 43


if __name__ == '__main__':
    main()
