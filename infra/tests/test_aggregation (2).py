import weakref

from ya.skynet.util.unittest import TestCase
from ya.skynet.services.cqudp.transport.protocol import Protocol
from ya.skynet.services.cqudp.transport.protocol import Node
from ya.skynet.services.cqudp.utils import genuuid

import six


class BusMock(object):
    Timeout = six.moves.queue.Empty
    inode = None

    def __init__(self, listenaddr):
        self._incoming = six.moves.queue.Queue()
        self._listenaddr = listenaddr
        self._other_buses = weakref.WeakValueDictionary()

    def listenaddr(self):
        return self._listenaddr

    def receive(self, block=True, timeout=None):
        return self._incoming.get(block, timeout)

    def send(self, data, addr):
        addr = addr[1]
        dest = self._other_buses.get(addr)

        if dest is not None:
            dest._incoming.put((data, self.listenaddr(), None))

    def attach(self, other):
        self._other_buses[other.listenaddr()] = other
        other._other_buses[self.listenaddr()] = self

    def shutdown(self):
        pass


class TestProtocolAggregation(TestCase):
    def test_simple(self):
        destination_bus = BusMock(('D', 0))
        destination = Protocol(bus=destination_bus)
        intermediate = None

        try:
            intermediate_bus = BusMock(('I', 0))
            destination_bus.attach(intermediate_bus)
            intermediate_protocol = Protocol(bus=intermediate_bus)
            intermediate = Node(protocol=intermediate_protocol)

            sources = [Protocol(bus=BusMock(('S', i))) for i in range(20)]
            for s in sources:
                s.bus.attach(intermediate_bus)

            for i, s in enumerate(sources):
                path = [
                    ('C', destination.listenaddr()),
                    ('I', intermediate.protocol.listenaddr()),
                    (i, s.listenaddr())
                ]
                s.route_back({'type': 'result', 'uuid': genuuid()}, i, path)

            for _ in sources:
                intermediate._receive_step()

            intermediate._dispatch_step()
            intermediate._aggregate_step()

            try:
                cmd, env, iface = destination.receive(block=True, timeout=4)
            except Protocol.Timeout:
                self.fail('Timed out')

            self.assertIsInstance(env.msgs, list)
            self.assertEqual(20, len(env.msgs))
        finally:
            if intermediate is not None:
                intermediate.shutdown()
