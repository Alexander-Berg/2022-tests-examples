#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Тестируем qinfo
"""

import unittest
import sys

sys.path.append(".")
from qinfo import main


class TestQinfo(unittest.TestCase):
    def test_main(self):
        main(['qinfo.py', '-vt', '2000'])


if __name__ == "__main__":
    suite = unittest.TestLoader().loadTestsFromTestCase(TestQinfo)
    result = unittest.TestResult()
    suite.run(result)
    failure = 0 if len(result.errors) == 0 and len(result.failures) == 0 else 2
    message = 'Tests run: {0}, Errors: {1}, Failures: {2}'.format(result.testsRun, len(result.errors), len(result.failures))
    print('PASSIVE-CHECK:{0};{1};{2}'.format('StocksMathTests', failure, message))