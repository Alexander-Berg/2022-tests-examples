import unittest
import threading
import time

from .barrier import Barrier


class SequenceChecker:
    def __init__(self):
        self._lock = threading.Lock()
        self._list = []

    def push(self, value):
        with self._lock:
            self._list.append(value)
            # print >>sys.stderr, "Got {0}".format(value)

    def value(self):
        return self._list


def thread1(barrier, sequence):
    barrier.wait(2)
    time.sleep(0.05)
    sequence.push(2)
    barrier.wait(2)
    barrier.wait(2)
    time.sleep(0.05)
    sequence.push(4)


def thread2(barrier, sequence):
    time.sleep(0.05)
    sequence.push(1)
    barrier.wait(2)
    barrier.wait(2)
    time.sleep(0.05)
    sequence.push(3)
    barrier.wait(2)


class BarrierTest(unittest.TestCase):
    def setUp(self):
        pass

    def _run_thread_test(self, barrier):
        sequence = SequenceChecker()

        t1 = threading.Thread(target=thread1, args=(barrier, sequence))
        t2 = threading.Thread(target=thread2, args=(barrier, sequence))

        t1.start()
        t2.start()

        t1.join()
        t2.join()

        self.assertEqual(sequence.value(), [1, 2, 3, 4])

    def test_barrier(self):
        for i in range(4):
            barrier = Barrier(2)
            self._run_thread_test(barrier)

    def test_timeout(self):
        for i in range(4):
            barrier = Barrier(2)
            self.assertRaises(Barrier.Timeout, barrier.wait, 1)
            self._run_thread_test(barrier)

if __name__ == "__main__":
    unittest.main()
