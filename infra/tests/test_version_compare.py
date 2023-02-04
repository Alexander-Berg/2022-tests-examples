from __future__ import absolute_import

from skycore.kernel_util.unittest import TestCase, main

from skycore.framework.version import LooseVersion


class TestVersion(TestCase):
    def test_version_compare(self):
        self.assertEqual(LooseVersion('1.5.1'), LooseVersion('1.5.1'))
        self.assertEqual(LooseVersion('1.5.1'), '1.5.1')
        self.assertEqual('1.5.1', LooseVersion('1.5.1'))
        self.assertLess(LooseVersion('1.5.b'), LooseVersion('1.5.1'))
        self.assertLess(LooseVersion('1.5.1'), LooseVersion('1.5.2b2'))
        self.assertLess(LooseVersion('1.5.1'), '1.5.2b2')
        self.assertLess('1.5.1', LooseVersion('1.5.2b2'))
        self.assertLess('1.5', LooseVersion('1.5.2b2'))
        self.assertLessEqual(LooseVersion('1.5.1'), LooseVersion('1.5.2b2'))
        self.assertLessEqual(LooseVersion('1.5.1'), '1.5.2b2')
        self.assertLessEqual('1.5.1', LooseVersion('1.5.2b2'))
        self.assertGreater(LooseVersion('1.5.2b2'), LooseVersion('1.5.1'))
        self.assertGreater(LooseVersion('1.5.2b2'), '1.5.1')
        self.assertGreater('1.5.2b2', LooseVersion('1.5.1'))
        self.assertGreaterEqual(LooseVersion('1.5.2b2'), LooseVersion('1.5.1'))
        self.assertGreaterEqual(LooseVersion('1.5.2b2'), '1.5.1')
        self.assertGreaterEqual('1.5.2b2', LooseVersion('1.5.1'))
        self.assertNotEqual(LooseVersion('1.5.2b2'), LooseVersion('1.5.1'))
        self.assertNotEqual(LooseVersion('1.5.2b2'), '1.5.1')
        self.assertNotEqual('1.5.2b2', LooseVersion('1.5.1'))
        self.assertEqual(LooseVersion('-'), LooseVersion('-'))
        self.assertEqual(LooseVersion('0'), LooseVersion('0'))
        self.assertEqual(LooseVersion('0..0'), LooseVersion('0.0.0'))
        self.assertEqual(LooseVersion('0.0.0'), LooseVersion('0..0'))


if __name__ == '__main__':
    main()
