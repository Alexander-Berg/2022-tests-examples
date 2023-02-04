import six
import random
import socket
from ya.skynet.services.cqudp.transport.messagebus import Netlibus
from ya.skynet.services.cqudp.utils import run_daemon, sleep
from ya.skynet.util.sys.gettime import monoTime
from ya.skynet.util import unittest


try:
    import netlibus  # noqa
    has_netlibus = True
except ImportError:
    has_netlibus = False


@unittest.skipIf(not has_netlibus, 'No netlibus available')
class TestMessagebus(unittest.TestCase):
    def test_fml_fuckup(self):
        self.assertLess(
            measure_traffic()[1] * 2000,
            1 * (10 ** 9)
        )


def measure_traffic():
    counter = Counter()

    sock = socket.socket(socket.AF_INET6, socket.SOCK_DGRAM)
    sock.bind(('localhost', 0))
    run_daemon(receive, sock, counter)

    port = sock.getsockname()[1]
    bus = Netlibus(0)
    try:
        bus.run()
        run_daemon(send, bus, port)

        for _ in six.moves.xrange(60):
            sleep(1)

        return counter.current()
    finally:
        bus.shutdown()


def receive(s, counter):
    counter.start()
    while True:
        data, addr = s.recvfrom(8 * 1024)
        counter.push(len(data))


def send(bus, port):
    data = random_data(1024)
    while True:
        bus.send(data, (0, ('localhost', port)))
        sleep(random.random() * 2)  # one second on the average


def random_data(size):
    return ''.join(chr(random.randint(0, 255)) for _ in six.moves.xrange(size))


class Counter(object):
    def __init__(self):
        self.time = 0
        self.total = 0
        self.local = 0
        self.max_speed = 0

    def start(self):
        self.time = monoTime()

    def push(self, value):
        self.total += value
        self.local += value

        t = monoTime()
        dt = t - self.time

        if dt > 0.5:
            speed = self.local / dt
            self.max_speed = max(self.max_speed, speed)
            self.local = 0
            self.time = t

    def current(self):
        return self.total, self.max_speed


if __name__ == '__main__':
    measure_traffic()
