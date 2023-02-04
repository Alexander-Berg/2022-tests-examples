# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import six
import unittest

from faker import Faker

from saas.library.python.common_functions.tests.fake import Provider as CommonProvider


class TestFakeCommonFunctions(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.fake = Faker()
        cls.fake.add_provider(CommonProvider)

    def test_random_string(self):
        test_len = self.fake.random.randint(1, 128)
        rs = self.fake.random_string(test_len)
        self.assertIsInstance(rs, six.string_types)
        self.assertEqual(test_len, len(rs), '{} should have invalid len'.format(rs))

    def test_random_hexadecimal_string(self):
        test_len = self.fake.random.randint(1, 128)
        s = self.fake.random_hexadecimal_string(test_len)
        self.assertIsInstance(s, six.string_types)
        self.assertEqual(len(s), test_len, '{} has invalid length'.format(s))
        self.assertRegexpMatches(s, r'^[0-9a-f]+$', '{} is not hexadecimal'.format(s))
