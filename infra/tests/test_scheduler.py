import six

from ya.skynet.util.unittest import TestCase, main

from ya.skynet.services.cqudp.client import MultiTaskSession, Scheduler

host0, host1, host2 = 0, 1, 2
time0 = 12345
infinity = 1000


class TestScheduler(TestCase):
    def test_1(self):
        s = prepare_scheduler()

        self.assertEqual(s.get_unfinished_hosts(time0), [host0, host1])

        s.task_done(host0, time0)
        self.assertEqual(s.get_unfinished_hosts(time0 + 1), [host1])

        s.task_done(host1, time0)
        self.assertEqual(s.get_unfinished_hosts(time0 + 1), [])

    def test_out_of_order_results_1(self):
        s = prepare_scheduler([host0])

        s.result_received(host0, 1, None, time0 + 1)

        self.assert_eventually_equal(time0 + 2, s.schedule_heartbeats, {host0: 0})

    def test_out_of_order_results_2(self):
        s = prepare_scheduler([host0])

        s.result_received(host0, 1, None, time0 + 1)
        s.result_received(host0, 1, None, time0 + 2)

        self.assert_equal(time0 + 3, s.schedule_tasks, [])
        self.assert_eventually_equal(time0 + 2, s.schedule_heartbeats, {host0: 0})

    def test_task_resending(self):
        s = prepare_scheduler()

        for i in six.moves.xrange(s.retry_period + 1):
            self.assertFalse(list(s.schedule_tasks(time0 + i)))

        for i in six.moves.xrange(s.retry_period + 1, 150):
            self.assertEqual(
                list(sorted(s.schedule_tasks(time0 + i))),
                [(host0, 0), (host1, 0)]
            )

    def test_task_resending_after_heartbeats(self):
        s = prepare_scheduler()

        s.heartbeat_received(0, time0 + 0)
        for i in six.moves.xrange(s.retry_period + 1):
            self.assertFalse(list(s.schedule_tasks(time0 + i)))

        for i in six.moves.xrange(s.retry_period + 1, 150):
            self.assertEqual(list(s.schedule_tasks(time0 + i)), [(1, 0)])

    def test_heartbeats_sending(self):
        s = prepare_scheduler()

        self.assertFalse(s.schedule_heartbeats(time0 + 0))  # no heartbeats to silent hosts
        s.heartbeat_received(0, time0 + 0)  # host0 became not silent
        for i in range(s.heartbeat_period + 1):
            self.assertFalse(s.schedule_heartbeats(time0 + i))  # yet still no heartbeats until time >= hb period

        for i in range(s.heartbeat_period + 1, 150):
            self.assertEqual(s.schedule_heartbeats(time0 + i), {0: 0}, str(i))  # expect heartbeats

    def test_results_receiving(self):
        s = prepare_scheduler()

        s.result_received(host0, 0, 0, time0)

        for i in range(s.heartbeat_period + 1):
            self.assertFalse(s.schedule_heartbeats(time0 + i))  # yet still no heartbeats until time >= hb period

        for i in range(s.heartbeat_period + 1, 150):
            self.assertEqual(s.schedule_heartbeats(time0 + i), {0: 1})  # expect heartbeats

    def assert_equal(self, start, fn, const):
        for t in six.moves.xrange(start, start + infinity):
            self.assertEqual(list(sorted(fn(t))), const)

    def assert_eventually_equal(self, start, fn, const):
        in_equality = False
        for t in six.moves.xrange(start, start + infinity):
            if in_equality:
                self.assertEqual(fn(t), const)
            else:
                in_equality = fn(t) == const
        self.assertTrue(in_equality)

    def test_multi_task_aggregation(self):
        hosts = [
            ('host1', 'cont1'),
            ('host1', 'cont2'),
            ('host1', 'cont3'),
            ('host2', 'cont1'),
        ]
        params = [1, 2, 3, 4]
        h, u, d = MultiTaskSession.prepare_host_data(hosts, None, params)
        s = Scheduler(list(h.keys()), 0)

        st = [(host, h[host].actual_addr) for host, sn in s.schedule_tasks(s.retry_period + 1)]
        tasks, data = MultiTaskSession.aggregate_task(st, d)
        remaining = 4
        self.assertEqual(2, len(tasks))
        self.assertEqual(2, len(data))
        self.assertEqual(remaining, sum(len(x[0]) for x in tasks))
        self.assertEqual(remaining, sum(map(len, list(data.values()))))

        larger_list = tasks[0][0] if len(tasks[0][0]) == 3 else tasks[1][0]
        for hostid in larger_list:
            s.task_sent(hostid, 0)
            s.heartbeat_received(hostid, 0)
            remaining -= 1

            st = [(host, h[host].actual_addr) for host, sn in s.schedule_tasks(s.retry_period + 1)]
            tasks, data = MultiTaskSession.aggregate_task(st, d)
            required = 2 if remaining > 1 else 1
            self.assertEqual(required, len(tasks))
            self.assertEqual(remaining, sum(len(x[0]) for x in tasks))
            self.assertEqual(remaining, sum(map(len, list(data.values()))))


def prepare_scheduler(hosts=None):
    if hosts is None:
        hosts = [host0, host1]

    s = Scheduler(hosts, 0)

    for h in hosts:
        s.task_sent(h, time0)

    return s


if __name__ == '__main__':
    raise SystemExit(main())
