import logging

from ya.skynet.util.unittest import main, TestCase
from ya.skynet.util.sys.user import getUserUID
from ya.skynet.services.cqudp.mocksoul_rpc import gevent_client, server, errors


class TestMocksoulRpc(TestCase):
    def setUp(self):
        super(TestMocksoulRpc, self).setUp()
        self.rpc = server.RPC(logging.getLogger('unittest'))
        self.srv = server.Server(logging.getLogger('unittest'), backlog=1, max_conns=1, unix='\0unittest')
        self.srv.register_connection_handler(self.rpc.get_connection_handler())
        self.srv.start()
        self.client = gevent_client.RPCClientGevent('\0unittest', None)

    def tearDown(self):
        self.client.stop()
        self.srv.stop()
        self.rpc.stop()
        self.srv.join()
        self.rpc.join()

        super(TestMocksoulRpc, self).tearDown()

    def test_simple_call(self):
        self.rpc.mount(lambda: 42, name='simple_fun')

        job = self.client.call('simple_fun')
        res = job.wait(5)
        self.assertEqual(res, 42)

    def test_generator(self):
        def iter_fun():
            for i in range(5):
                yield i

        self.rpc.mount(iter_fun, name='generator_fun', typ='generator')
        job = self.client.call('generator_fun')
        results = []
        for r in job.iter(5):
            results.append(r)

        self.assertEqual(results, list(iter_fun()))

    def test_peer_uid(self):
        self.rpc.mount(lambda job: job.peer_id, name='peer_fun', typ='full')
        job = self.client.call('peer_fun')
        res = job.wait(5)

        self.assertEqual(res[0], getUserUID())

    def test_broken_connection(self):
        self.rpc.mount(lambda: 42, name='simple_fun')
        job = self.client.call('simple_fun')
        res = job.wait(5)
        self.assertEqual(res, 42)

        self.srv.stop()
        self.rpc.stop()
        self.srv.join()
        self.rpc.join()

        with self.assertRaises(errors.RPCError):
            job = self.client.call('simple_fun')
            res = job.wait(1)


if __name__ == '__main__':
    main()
