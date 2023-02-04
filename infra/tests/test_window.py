import six

from unittest import TestCase
from random import shuffle
from ya.skynet.services.cqudp.window import IncomingWindow, IndexedWindow, MultiWindow


class TestIncomingWindow(TestCase):
    def test_1(self):
        w = IncomingWindow()

        w.put(0, 0)
        self.assertEqual([(0, 0)], w.pop())
        self.assertEqual([], w.pop())

    def test_2(self):
        w = IncomingWindow()

        w.put(1, 1)
        self.assertEqual([], w.pop())

    def test_3(self):
        w = IncomingWindow()

        w.put(1, 1)
        self.assertEqual([], w.pop())

        w.put(0, 0)
        self.assertEqual([(0, 0), (1, 1)], w.pop())
        self.assertEqual([], w.pop())

    def test_4(self):
        w = IncomingWindow()

        w.put(2, 2)
        w.put(0, 0)
        self.assertEqual([(0, 0)], w.pop())
        self.assertEqual([], w.pop())

        w.put(1, 1)
        self.assertEqual([(1, 1), (2, 2)], w.pop())
        self.assertEqual([], w.pop())

    def test_5(self):
        w = IncomingWindow()

        w.put(2, 2)
        w.put(0, 0)
        self.assertEqual([(0, 0)], w.pop())
        self.assertEqual([], w.pop())

        w.put(1, 1)
        self.assertEqual([(1, 1), (2, 2)], w.pop())
        self.assertEqual([], w.pop())

        w.put(5, 5)
        w.put(4, 4)
        self.assertEqual([], w.pop())

        w.put(3, 3)
        self.assertEqual([(3, 3), (4, 4), (5, 5)], w.pop())
        self.assertEqual([], w.pop())

    def test_6(self):
        w = IncomingWindow()

        for i in [10, 9]:
            w.put(i, i)
            self.assertEqual([], w.pop())

        w.put(0, 0)
        self.assertEqual([(0, 0)], w.pop())
        self.assertEqual([], w.pop())

        for i in range(2, 8):
            w.put(i, i)
            self.assertEqual([], w.pop())

        w.put(1, 1)
        self.assertEqual([(i, i) for i in range(1, 8)], w.pop())
        self.assertEqual([], w.pop())

        w.put(11, 11)
        self.assertEqual([], w.pop())

        w.put(8, 8)
        self.assertEqual([(i, i) for i in range(8, 12)], w.pop())
        self.assertEqual([], w.pop())

    def test_7(self):
        limit = 300000
        d = list(range(0, limit))
        shuffle(d)

        w = IncomingWindow()
        r = []
        for x in d:
            w.put(x, x)
            r.extend(w.pop())

        self.assertEqual([(i, i) for i in range(0, limit)], r)

    def test_double(self):
        pass  # TODO


class TestIndexedWindow(TestCase):
    N = 32

    def testStraightSeek(self):
        win = IndexedWindow(self.N)
        for i in six.moves.xrange(self.N):
            win.enqueue(i * 2)
        for i in six.moves.xrange(self.N):
            x = win.seek(i)
            self.assertEqual(x[0], i)
            self.assertEqual(x[1], i * 2)
        self.assertIsNone(win.seek(None))

    def testSkipSeek(self):
        win = IndexedWindow(self.N)
        for i in six.moves.xrange(self.N):
            win.enqueue(i * 2)

        for i in six.moves.xrange(0, self.N, 2):
            x = win.seek(i)
            self.assertEqual(x[0], i)
            self.assertEqual(x[1], i * 2)

        self.assertIsNone(win.seek(None))

    def testSeekNone(self):
        win = IndexedWindow(self.N)
        for i in six.moves.xrange(self.N):
            win.enqueue(i * 2)

        self.assertIsNone(win.seek(None))


class TestMultiWindow(TestCase):
    def test_simple(self):
        w = MultiWindow()

        w.put(0, 'lab1', '0')
        w.put(1, 'lab2', '1')
        w.put(2, 'lab1', '2')
        w.put(3, 'lab2', '3')

        self.assertListEqual(
            w.pop(),
            [
                (0, 'lab1', 0, '0'),
                (1, 'lab2', 0, '1'),
                (2, 'lab1', 1, '2'),
                (3, 'lab2', 1, '3'),
            ]
        )
