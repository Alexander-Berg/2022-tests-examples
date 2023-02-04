#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Тестируем модули Finam.ru
"""

__author__ = "Zasimov Alexey"
__email__ = "zasimov-a@yandex-team.ru"

import unittest
import sys

sys.path.append(".")
from stocks3.config.finam import FinamConfig
from stocks3.share.curl import load_url_data_retry
from datetime import datetime


class TestFinam(unittest.TestCase):
    def test_finam_prepare_url(self):
        """
        Сборка адреса для Finam.
        """
        config = FinamConfig("config/finam.xml")
        today = datetime.today()
        url = config.prepareURL("сырье", "ICE.BRN", 19473, today, today)
        data = load_url_data_retry(url)
        self.assertTrue(len(data) != 0, 'finam not responding')


if __name__ == "__main__":
    suite = unittest.TestLoader().loadTestsFromTestCase(TestFinam)
    result = unittest.TestResult()
    suite.run(result)
    failure = 0 if len(result.errors) == 0 and len(result.failures) == 0 else 2
    message = 'Tests run: {0}, Errors: {1}, Failures: {2}'.format(result.testsRun, len(result.errors), len(result.failures))
    print('PASSIVE-CHECK:{0};{1};{2}'.format('StocksFinamTests', failure, message))