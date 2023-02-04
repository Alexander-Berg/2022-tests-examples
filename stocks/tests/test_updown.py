#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Тестируем модуль проверок L{UpDownChecker}.
"""
import unittest
import sys
import csv

sys.path.append(".")
from stocks3t import *


class TestUpDown(TestS3, unittest.TestCase):
    def test_updown(self):
        """
        Тест проверялки L{UpDownChecker}.
        """
        checker = load("tests/config/updown.xml", self.source)
        db = DatabaseWP([20.0])
        db_dual = DatabaseWPDual([(20.0, 20.0)])
        # Собственно тестируем
        # Данные берем из файла tests/rc/updown.csv
        reader = csv.reader(open("tests/rc/updown.csv", "r"), delimiter=",")
        for prev, cur, ok in [(float(row[0].replace(",", ".")),
                               float(row[1].replace(",", ".")),
                               row[2] == '1') for row in reader]:
            # Тестируем обычную котировку
            price = self.q1.make_price(today(), cur)
            self.source.saver.db = db
            self.assertEqual(checker.check(price), ok)

            # Тестируем двойную котировку.
            price = self.q1.make_dual_price(today(), prev, cur)
            self.source.saver.db = db_dual
            self.assertEqual(checker.check(price), ok)


if __name__ == "__main__":
    suite = unittest.TestLoader().loadTestsFromTestCase(TestUpDown)
    result = unittest.TestResult()
    suite.run(result)
    failure = 0 if len(result.errors) == 0 and len(result.failures) == 0 else 2
    message = 'Tests run: {0}, Errors: {1}, Failures: {2}'.format(result.testsRun, len(result.errors),
                                                                  len(result.failures))
    print('PASSIVE-CHECK:{0};{1};{2}'.format('StocksFinamTests', failure, message))
