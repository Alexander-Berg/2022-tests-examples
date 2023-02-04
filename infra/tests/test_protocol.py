import os
import logging

import mock

from common import UseNetlibus

from ya.skynet.util.unittest import TestCase
from ya.skynet.services.cqudp.transport.envelope import Envelope
from ya.skynet.services.cqudp.transport.protocol import Protocol, Node
from ya.skynet.services.cqudp.utils import genuuid


def nonexistent_fqdn():
    return 'kekeke--nonexistent'


class TestProtocol(TestCase):
    server_transport = 'msgpack'

    def setUp(self):
        super(TestProtocol, self).setUp()
        logging.getLogger().setLevel(os.getenv('TESTLOGGING', logging.INFO))

    @mock.patch('ya.skynet.services.cqudp.transport.messagebus.fqdn', nonexistent_fqdn)
    def test_sending_ips(self):
        for opt1 in (True, False):
            for opt2 in (False, True):
                for ip1 in (None, '::1'):
                    try:
                        proto1 = Protocol(ip=ip1, impl=self.server_transport, send_own_ip=opt1)
                        proto2 = Node(impl=self.server_transport, send_own_ip=opt2)
                        proto2.spawn(proto2.run, _daemon=True)

                        port2 = proto2.protocol.listenaddr()[1]
                        tree = [(
                            (0, ('::1', port2)),
                            [(proto1._make_pathitem(1), None)]
                        )]
                        msg = {
                            'type': 'test',
                            'uuid': genuuid(),
                        }
                        env = Envelope(msg, tree)
                        for addr, next_envelope in env.next():
                            proto1.route(next_envelope, addr)

                        if not opt1 and ip1 is None:
                            with self.assertRaises(Exception):
                                proto1.receive(timeout=5.0)
                        else:
                            cmd, env, iface = proto1.receive(timeout=5.0)
                            self.assertEqual(cmd, 'route')
                            path = env.path
                            self.assertTrue((len(path[1]) > 2 and path[1][2]) or ip1)
                            self.assertTrue((opt2 and len(path[0]) > 2 and path[0][2])
                                            or (not opt2 and len(path[0]) == 2)
                                            )
                    finally:
                        proto1.shutdown()
                        proto2.shutdown()


class TestProtoclNetlibus(UseNetlibus, TestProtocol):
    pass
