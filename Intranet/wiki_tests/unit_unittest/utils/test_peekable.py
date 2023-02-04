import itertools

from wiki.utils.peekable import Peekable
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class PeekableTest(BaseApiTestCase):
    def test_iterator_protocol(self):
        iterator = Peekable([1, 2, 3])
        x = next(iterator)
        iterator2 = iter(iterator)
        y = next(iterator2)
        for z in iterator2:
            pass
        self.assertEqual(x, 1)
        self.assertEqual(y, 2)
        self.assertEqual(z, 3)

    def test_peek(self):
        iterator = Peekable([1, 2, 3])
        x = iterator.peek()
        xx = next(iterator)
        self.assertEqual(x, 1)
        self.assertEqual(x, xx)

    def test_has_next(self):
        iterator = Peekable([1])
        self.assertTrue(iterator.has_next())
        iterator2 = Peekable([])
        self.assertFalse(iterator2.has_next())

    def test_peek_returns_none_if_empty(self):
        iterator = Peekable([])
        self.assertEqual(iterator.peek(), None)

    def test_peek_is_idempotent(self):
        iterator = Peekable([1, 2, 3])
        self.assertEqual(iterator.peek(), 1)
        self.assertEqual(iterator.peek(), 1)

    def test_has_next_accepts_num_items(self):
        iterator = Peekable([11, 12, 13])
        self.assertTrue(iterator.has_next(3))
        self.assertFalse(iterator.has_next(4))

    def test_peek_many(self):
        iterator = Peekable([1, 2, 3])
        self.assertEqual(iterator.peek_many(1), [1])
        self.assertEqual(iterator.peek_many(2), [1, 2])
        self.assertEqual(iterator.peek_many(3), [1, 2, 3])
        self.assertEqual(iterator.peek_many(4), [1, 2, 3])
        self.assertEqual(iterator.peek_many(0), [])

    def test_peek_accepts_index(self):
        iterator = Peekable([1, 2, 3])
        self.assertEqual(iterator.peek(0), 1)
        self.assertEqual(iterator.peek(1), 2)
        self.assertEqual(iterator.peek(2), 3)
        self.assertEqual(iterator.peek(3), None)

    def test_takewhile(self):
        iterator = Peekable([1, 2, 3, 4, 5])
        taken = iterator.takewhile(lambda x: x < 3)
        self.assertEqual(list(taken), [1, 2])
        self.assertEqual(list(iterator), [3, 4, 5])

    def test_takewhile_keeps_last_tested(self):
        iterator = Peekable([1, 2, 3])
        list(itertools.takewhile(lambda x: x < 2, iterator))
        self.assertEqual(iterator.peek(), 3)
        iterator2 = Peekable([1, 2, 3])
        list(iterator2.takewhile(lambda x: x < 2))
        self.assertEqual(iterator2.peek(), 2)

    def test_takewhile_consumes_everything_if_all_items_test_true(self):
        iterator = Peekable([1, 2, 3])
        list(iterator.takewhile(lambda x: x < 200))
        self.assertFalse(iterator.has_next())
        self.assertEqual(iterator.peek(), None)

    def test_casting_from_peekable_is_idempotent(self):
        iterator = Peekable([1, 2, 3])
        iterator2 = Peekable(iterator)
        self.assertIs(iterator, iterator2)
        self.assertEqual(iterator2.peek(), 1)

    def test_undo(self):
        iterator = Peekable([1, 2, 3])
        iterator.undo(7)
        self.assertEqual(iterator.peek(), 7)
        self.assertEqual(next(iterator), 7)
