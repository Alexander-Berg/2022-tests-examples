import six
import random

from ya.skynet.util.unittest import TestCase, main

from ya.skynet.services.cqudp.server.taskhandle import result
from ya.skynet.services.cqudp.client.client import Client
from ya.skynet.services.cqudp.client import Session, heartbeat
from ya.skynet.services.cqudp.transport.envelope import build_tree, Envelope


# TODO: This test uses too much knowledge of internals, e.g.:
# TODO:    session.scheduler.status[host].window.window.index
class TestResending(TestCase):
    def testTreeBuilder(self):
        for i in six.moves.xrange(1, 512, 10):
            hosts = {('host' + str(j), 12345) for j in six.moves.xrange(i)}

            try:
                client = Client()
                session = Session(client, None, hosts, None)

                not_feeded_size = random.randint(0, len(session.hosts))
                not_feeded = set(random.sample(list(session.hosts.keys()), not_feeded_size))

                half_feeded_size = random.randint(0, len(session.hosts) - not_feeded_size)
                half_feeded = set(random.sample(set(session.hosts) - not_feeded, half_feeded_size))

                feeded = set(session.hosts) - not_feeded - half_feeded

                for host in half_feeded:
                    session._check_result(
                        (host, session.hosts[host]),
                        result(session.taskid, 0, (0, None)),
                    )

                for host in feeded:
                    session._check_result(
                        (host, session.hosts[host]),
                        result(session.taskid, 0, (0, None)),
                    )
                    session._check_result(
                        (host, session.hosts[host]),
                        result(session.taskid, 1, (None, StopIteration())),
                    )

                # Check, that feeding is correct
                for host in not_feeded:
                    self.assertIn(host, session.scheduler.status)
                    self.assertEqual(session.scheduler.status[host].window.window.index, 0)

                for host in half_feeded:
                    self.assertIn(host, session.scheduler.status)
                    self.assertEqual(session.scheduler.status[host].window.window.index, 1)

                # Doesn't work because of delayed pop in the Session.
                # for host in feeded:
                #     self.assertNotIn(host, session.scheduler.status)

                # FIXME don't imitate client, use mock
                addrs = [(host, session.hosts[host].actual_addr) for host in session.scheduler.status]
                tree_data = {host: session.scheduler.status[host].window.window.index for host in session.scheduler.status}
                hbs = [(None, Envelope(
                    msgs=[heartbeat(session.taskid)],
                    tree=build_tree(addrs),
                    tree_data=tree_data,
                    path=[('C', ('localhost', 12345))],
                ))]

                # check that each host receives proper 'next' value
                hosts -= {session.hosts[i] for i in feeded}
                # print addrs
                # print hosts
                while hbs:
                    host, hb = hbs.pop(0)
                    # print host, hb, list(hb.next())
                    hbs.extend(hb.next())
                    if host is None:
                        continue
                    hosts.discard(host[1])
                    self.assertEqual(host[0], hb.hostid)
                    self.assertEqual(host[1], session.hosts[hb.hostid].actual_addr)
                    self.assertEqual(hb.data, session.scheduler.status[hb.hostid].window.window.index)
                self.assertFalse(hosts, str(i))
            finally:
                client.shutdown()


if __name__ == '__main__':
    main()
