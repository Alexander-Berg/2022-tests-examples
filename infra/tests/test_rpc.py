import select
from common import CQTest, UseNetlibus

from ya.skynet.services.cqudp.rpc.pipe import Pipe
from ya.skynet.services.cqudp.client.session import HostEntry, ResultsQueue
from ya.skynet.util.pickle import dumps


class SessionMock(object):
    def __init__(self, *hosts):
        self.hosts = {
            n: HostEntry(h, 0)
            for n, h in enumerate(hosts)
        }

    def get_host_by_id(self, hostid):
        return self.hosts[hostid].user_host


class TestRPC(CQTest):
    no_server = True

    def test_client_pipe(self):
        p = Pipe(ResultsQueue(select.select))
        sess = SessionMock('a', 'b')
        p._session = sess
        p.process(1, 'b', {'method': 'push', 'index': 1, 'data': dumps('b1')})
        p.process(1, 'b', {'method': 'push', 'index': 0, 'data': dumps('b0')})
        p.process(0, 'a', {'method': 'push', 'index': 0, 'data': dumps('a0')})

        results = set(p.get(block=False) for _ in range(3))
        self.assertIn(('a', 'a0'), results)
        self.assertIn(('b', 'b0'), results)
        self.assertIn(('b', 'b1'), results)


class TestRPCNetlibus(UseNetlibus, TestRPC):
    pass
