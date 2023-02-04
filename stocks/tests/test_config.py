#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Тестируем тестовую конфигурацию :)
В тестовой конфигурации имеется:
    - источник - одна штука. Имя Test Source, идентификатор test, внутренний идентификатор 989898.
    - две котировки (Q1 и Q2).
"""

__authors__ = "Zasimov Alexey, Bogdanov Evgeny"
__email__ = "zasimov-a@yandex-team.ru"

import unittest
import sys

sys.path.append(".")
from stocks3t import *


class ConfigTest(unittest.TestCase):
    def setUp(self):
        self.source = load_source()

    def test_source(self):
        # Тестируем source
        assert self.source.sourceId == "test"
        assert self.source.source_inner_id == 989898
        assert self.source.name == "Test Source"
        assert self.source.weight == 1255

    def test_quotes(self):
        # Тестируем котировки
        q1 = get_q1(self.source)
        q2 = get_q2(self.source)
        # Q1
        assert q1.quote_id == Q1_ID
        assert q2.quote_id == Q2_ID
        assert q1.numerator.stock == "RUB"
        assert q1.denominator.stock == "USD"
        assert q1.numerator.scale == 1
        assert q1.denominator.scale == 1
        # Q2
        assert q2.numerator.stock == "USD"
        assert q2.denominator.stock == "RUB"
        assert q2.numerator.scale == 3
        assert q2.denominator.scale == 10

    def test_id_map(self):
        # Тестируем ID-MAP
        q1price = self.source.parser.default_map.make_price('SQ1', today(), 28.0)
        assert q1price.quote.quote_id == Q1_ID
        assert q1price.tz is None
        assert not q1price.is_dual_price

        q2price = self.source.parser.default_map.make_dual_price('SQ2', today(), 24.0, 8.0)
        assert q2price.quote.quote_id == Q2_ID
        assert q2price.tz == "Europe/London"
        assert q2price.is_dual_price


if __name__ == "__main__":
    suite = unittest.TestLoader().loadTestsFromTestCase(ConfigTest)
    result = unittest.TestResult()
    suite.run(result)
    for error in result.errors:
        for e in error:
            print(e)
    failure = 0 if len(result.errors) == 0 and len(result.failures) == 0 else 2
    message = 'Tests run: {0}, Errors: {1}, Failures: {2}'.format(result.testsRun, len(result.errors), len(result.failures))
    print('PASSIVE-CHECK:{0};{1};{2}'.format('StocksConfigTests', failure, message))
