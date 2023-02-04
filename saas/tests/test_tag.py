# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import unittest
from random import shuffle

from saas.library.python.gencfg import GencfgTag
from saas.library.python.gencfg.errors import InvalidTag


class TestGencfgTag(unittest.TestCase):
    test_tag = 'stable-123-r1234'
    test_tag2 = 'tags\\/stable-123-r1234'

    @classmethod
    def setUpClass(cls):
        pass

    def test_from_string(self):
        tag = GencfgTag(self.test_tag)
        tag2 = GencfgTag(self.test_tag2)
        self.assertEqual(tag.tag, self.test_tag)
        self.assertEqual(tag, tag2)

    def test_invalid_tag(self):
        invalid_tags = ['stable', 'stable-1', 'stable1r1', 'stable-r1', 'stable-0-r1', 'stable-1-r0', 'stable-l-r1', 'stable-0-r1', 'stable-0-1', ]
        for tag in invalid_tags:
            with self.assertRaises(InvalidTag):
                GencfgTag(tag)

    def test_order(self):
        tags = ['stable-1-r1', 'stable-1-r2', 'stable-1-r11', 'stable-2-r1', 'stable-11-r1', 'stable-11-r11', 'trunk']
        ordered_tags = [GencfgTag(t) for t in tags]
        shuffled_tags = [GencfgTag(t) for t in tags]
        shuffle(shuffled_tags)
        sorted_tags = sorted(shuffled_tags)
        self.assertListEqual(ordered_tags, sorted_tags)
        self.assertNotEqual(ordered_tags, shuffled_tags)

    def test_from_tag(self):
        tag = GencfgTag('stable-1-r1')
        tag2 = GencfgTag(tag)

        self.assertEqual(tag.tag, tag2.tag)
        self.assertEqual(tag.revision, tag2.revision)
        self.assertEqual(tag.release, tag2.release)

    def test_to_bool(self):
        self.assertFalse(GencfgTag(None))
        self.assertFalse(GencfgTag('trunk'))
        self.assertTrue(GencfgTag(self.test_tag))
