from ya.skynet.util.unittest import TestCase, main

from ya.skynet.services.cqudp.client import MultiTaskSession
from ya.skynet.util.pickle import dumps


class ClientMock(object):
    def __getattr__(self, obj):
        return self

    def __call__(self, *args, **kwargs):
        return self


class TestSession(TestCase):
    def test_multi_session_result(self):
        hosts = [
            ('host1', 'cont1'),
            ('host1', 'cont2'),
            ('host1', 'cont3'),
            ('host1', 'cont4'),
            ('host2', 'cont1'),
        ]
        s = MultiTaskSession(ClientMock(), None, hosts, task)
        self.assertEqual(len(hosts), len(s.scheduler.status))
        s._check_result(
            (0, 'host1'),
            {
                'index': 0,
                'result': dumps((None, 'done')),
            },
        )
        self.assertEqual(len(hosts) - 1, len(s.scheduler.get_unfinished_hosts(0)))

        s._check_result(
            ([1, 2], 'host1'),
            {
                'index': 0,
                'result': dumps((None, 'done')),
            },
        )
        self.assertEqual(len(hosts) - 3, len(s.scheduler.get_unfinished_hosts(0)))


def task():
    pass


if __name__ == '__main__':
    raise SystemExit(main())
