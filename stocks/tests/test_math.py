#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Тестируем основные математические функции.
"""

__author__ = "Zasimov Alexey"
__email__ = "zasimov-a@yandex-team.ru"

import unittest
import sys

sys.path.append(".")
from stocks3t import *
from stocks3.core.stock import safeRelativePriceDelta


class TestMath(TestS3, unittest.TestCase):
    def test_costs(self):
        """
        Тестируем расчет стоимости.
        """
        price = self.q1.make_price(today(), 40)
        assert price.numeratorCost() == 40
        assert price.denominatorCost() == (1 / 40.0)
        dualPrice = self.q2.make_dual_price(today(), 30.0, 40.0)
        assert dualPrice.buy.numeratorCost() == 30.0 * 10.0 / 3.0
        assert dualPrice.buy.denominatorCost() == 3.0 / (10.0 * 30.0)
        assert dualPrice.sell.numeratorCost() == 40.0 * 10.0 / 3.0
        assert dualPrice.sell.denominatorCost() == 3.0 / (40.0 * 10.0)

    def test_delta(self):
        """
        Рассчет дельты
        """
        current = self.q1.make_price(today(), 40)
        previous = self.q1.make_price(today(), 20)
        assert current.absoluteDelta(previous) == 20
        assert current.relativeDelta(previous) == 100

        try:
            assert current.absoluteDelta(None) == 100
            raise RuntimeError("absoluteDelta: previous == None!")
        except AssertionError:
            pass

        try:
            assert current.relativeDelta(None) == 100
            raise RuntimeError("relativeDelta: previous == None!")
        except AssertionError:
            pass

        # Безопасные расчет relativeDelta
        assert safeRelativePriceDelta(current, previous, 1) == 100
        assert safeRelativePriceDelta(current, None, 1) == 1

        previousBad = self.q2.make_price(today(), 11)

        # В расчетах должны участвовать одни и те же котировки
        try:
            current.absoluteDelta(previousBad)
            raise RuntimeError("Different quotes test failed.")
        except AssertionError:
            pass
        try:
            current.relativeDelta(previousBad)
            raise RuntimeError("Different quotes test failed.")
        except AssertionError:
            pass


if __name__ == "__main__":
    suite = unittest.TestLoader().loadTestsFromTestCase(TestMath)
    result = unittest.TestResult()
    suite.run(result)
    failure = 0 if len(result.errors) == 0 and len(result.failures) == 0 else 2
    message = 'Tests run: {0}, Errors: {1}, Failures: {2}'.format(result.testsRun, len(result.errors), len(result.failures))
    print('PASSIVE-CHECK:{0};{1};{2}'.format('StocksMathTests', failure, message))