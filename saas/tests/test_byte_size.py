from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import unittest

from saas.library.python.byte_size import ByteSize


class TestByteSize(unittest.TestCase):
    def test_parse(self):
        self.assertEqual(ByteSize(2), ByteSize.parse('2 b'))
        self.assertEqual(ByteSize(4), ByteSize.parse('4 B'))
        self.assertEqual(ByteSize(4 * 1024), ByteSize.parse('4 KiB'))
        self.assertEqual(ByteSize(4 * 1024), ByteSize.parse('4 kB', force_binary=True))
        self.assertEqual(ByteSize(4 * 1000), ByteSize.parse('4KB'))

    def test_str(self):
        self.assertEqual('16 GiB', str(ByteSize(16 * 2**30)))
        self.assertEqual('14.9 GiB', str(ByteSize.parse('16 GB')))
        self.assertEqual('-360 GiB', str(ByteSize(-386547056640)))

    def test_sum(self):
        self.assertEqual(ByteSize(8), ByteSize(2) + ByteSize(6))
        bs = ByteSize.parse('4 KiB')
        bs += ByteSize.parse('12 KB', force_binary=True)
        self.assertEqual(bs, ByteSize(16 * 2**10))

        with self.assertRaises(TypeError):
            ByteSize(2) + 3

        with self.assertRaises(TypeError):
            3 + ByteSize(2)

    def test_sub(self):
        self.assertEqual(ByteSize(4096), ByteSize(8192) - ByteSize(4096))
        bs = ByteSize(4096)
        bs -= ByteSize(2048)
        self.assertEqual(bs, ByteSize(2048))
        self.assertEqual(ByteSize(4) - ByteSize(5), ByteSize(-1))

        with self.assertRaises(TypeError):
            ByteSize(4) - 1

    def test_mul(self):
        self.assertEqual(ByteSize(4096), ByteSize(1024) * 4)

        bs = ByteSize(4)
        bs *= 4
        self.assertEqual(bs, ByteSize(16))

        with self.assertRaises(TypeError):
            ByteSize(1) * 0.5

        with self.assertRaises(TypeError):
            4 * ByteSize(1)

    def test_div(self):
        self.assertEqual(ByteSize(5)//2, ByteSize(2))
        with self.assertRaises(TypeError):
            ByteSize(4)/2

    def test_neg(self):
        bs = ByteSize(4096)
        neg_bs = - bs
        self.assertEqual(bs._value, 4096)
        self.assertEqual(neg_bs._value, -4096)
        self.assertEqual(- neg_bs, bs)


if __name__ == '__main__':
    unittest.main()
