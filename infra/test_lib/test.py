import netlibus
from threading import Thread, Event
from unittest import TestCase, main

N = 10000
done = Event()
exc = None


def sending(bus, addrs, amount=N):
    global exc
    for i in range(amount):
        bus.send_ex(b'request ' * 300 + str(i).encode(), addrs)

    if done.wait(5):
        bus.stop()
    else:
        exc = Exception('Failed to deliver all the messages')


def receiving(bus, amount=N):
    global exc
    counter = 0
    try:
        while counter < amount:
            msg, addr, host = bus.receive(timeout=5.0, block=True)
            counter = counter + 1
            assert isinstance(msg, bytes), "expected bytes for msg, got: %r" % type(msg).__name__
            assert isinstance(addr, str), "expected str for addr, got: %r" % type(addr).__name__
            assert isinstance(host, str), "expected str for host, got: %r" % type(host).__name__
    except Exception as e:
        exc = e

    done.set()
    bus.stop()


class TestNetlibus(TestCase):
    def setUp(self):
        global done, exc
        exc = None
        done = Event()

    def test_health(self):
        rbus = netlibus.MsgBus()
        rbus.start()
        tr = Thread(target=receiving, args=[rbus])
        tr.start()

        sbus = netlibus.MsgBus()
        sbus.start()
        ts = Thread(target=sending, args=[sbus, [('127.0.0.1', rbus.port())]])
        ts.start()

        tr.join()
        ts.join()
        if exc is not None:
            raise exc

    def test_ipv4_in_ipv6(self):
        bus = netlibus.MsgBus()
        try:
            bus.start()
            bus.send(b'test', '[::ffff:127.0.0.1]:12345')
            bus.send_ex(b'test', [('::ffff:127.0.0.1', 12345)])
        finally:
            bus.stop()

    def test_unresolver_addr(self):
        import socket
        bus = netlibus.MsgBus()
        try:
            bus.start()
            self.assertRaises(socket.gaierror, bus.send_ex, b"test", [("some-nonexistent-addr.kokoko", 12345)])
        finally:
            bus.stop()

    def test_occupied_port(self):
        import socket
        s = socket.socket(socket.AF_INET6, socket.SOCK_DGRAM)
        s.bind(('localhost', 0))
        port = s.getsockname()[1]

        with self.assertRaises(RuntimeError) as excinfo:
            netlibus.MsgBus(port)

        assert str(port) in excinfo.exception.args[0]

    def test_timeout_receive(self):
        bus = netlibus.MsgBus()
        try:
            bus.start()
            self.assertRaises(bus.Timeout, bus.receive, timeout=0.5)
            self.assertRaises(bus.Timeout, bus.receive, block=False)
        finally:
            bus.stop()

    def test_resending(self):
        rbus = netlibus.MsgBus(timeout=2.0)
        rbus.start()
        tr = Thread(target=receiving, args=[rbus, 10])
        tr.start()

        sbus = netlibus.MsgBus()
        sbus.start()
        ts = Thread(target=sending, args=[sbus, [('127.0.0.1', rbus.port() + 1000), ('127.0.0.1', rbus.port())], 10])
        ts.start()

        tr.join()
        ts.join()

        if exc is not None:
            raise exc

    def test_resending_wrong_addr(self):
        rbus = netlibus.MsgBus()
        rbus.start()
        tr = Thread(target=receiving, args=[rbus, 10])
        tr.start()

        sbus = netlibus.MsgBus(timeout=1.0)
        sbus.start()
        ts = Thread(target=sending, args=[sbus, [('2a02:6b8::a03e', rbus.port()), ('::1', rbus.port())], 10])
        ts.start()

        tr.join()
        ts.join()

        if exc is not None:
            raise exc

    def test_send_bad_args(self):
        sbus = netlibus.MsgBus()

        with self.assertRaises(TypeError):
            sbus.send(None, 'addr')

        with self.assertRaises(TypeError):
            sbus.send('msg', None)

        with self.assertRaises(TypeError):
            sbus.send(b'msg', None)

        with self.assertRaises(TypeError):
            sbus.send_ex(b'msg', None)

    def test_send_wrong_host_name(self):
        sbus = netlibus.MsgBus()
        sbus.start()
        try:
            sbus.send(b"test", "32625:12345")
            self.assertRaises(sbus.Timeout, sbus.receive, timeout=0.5)
        finally:
            sbus.stop()


if __name__ == '__main__':
    main()
