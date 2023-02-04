#!/usr/bin/python
import unittest

from yandex.maps import tileutils5 as tu


class TestTileutils(unittest.TestCase):

    def test_hash_and_eq(self):
        """
        Tests if Tile objects can be hashed and equality compared
        """
        t1 = tu.Tile(0, 0, 4)
        t2 = tu.Tile(1, 1, 4)
        self.assertTrue(t1 != t2)
        self.assertEquals(t1, t1)

        d = {t1: 31337}
        t3 = tu.Tile(0, 0, 4)
        self.assertTrue(t2 not in d)
        self.assertTrue(t1 in d)
        self.assertTrue(t3 in d)
        self.assertEquals(d[t3], 31337)

    def test_repr(self):
        t1 = tu.Tile(31, 33, 7)
        self.assertEquals(repr(t1), "(31, 33, 7)")


if __name__ == '__main__':
    unittest.main()

