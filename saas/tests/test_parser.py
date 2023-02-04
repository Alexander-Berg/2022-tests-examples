# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import unittest
import yatest.common

from saas.library.python.ydb_filter_parser import Parser, Operator


class TestNannyService(unittest.TestCase):

    def test_parse(self):
        test_filters = []
        with open(yatest.common.source_path('saas/library/python/ydb_filter_parser/tests/data/filters.txt')) as test_data:
            for tid in test_data:
                if tid.isspace():
                    continue
                test_filters.append(tid.strip())
        for f in test_filters:
            res = Parser.parse(f)
            self.assertTrue(len(res) > 0)

    def test_labels(self):
        test_filter = '[/labels/shard] = "test_shard_label" and [/labels/disable_search] != "True"'
        parsed = Parser.parse(test_filter)
        shard_labels = [f.value for f in parsed if f.attribute == '/labels/shard' and f.operator == Operator.equals]
        self.assertEqual(len(shard_labels), 1)
        self.assertEqual(shard_labels[0], 'test_shard_label')
