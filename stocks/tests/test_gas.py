#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Тестируем импорт топливных котировок - бензин, солярка.
Тестируем в  два прохода - 1) проверяем актуальность данных, отдаваемых поставщиком
                           2) проверяем актуальность данных в БД
ID и названия тестируемых котировок
        id               название             регион
        20000            АИ-80                213
        20001            АИ-92                213
        20002            АИ-95                213
        20003            АИ-98                213
        20010            ДТ                   213
        20011            АИ-80 среднее        225
        20012            АИ-92 среднее        225
        20013            АИ-95 среднее        225
        20014            АИ-98 среднее        225
        20015            ДТ среднее           225
"""

from __future__ import print_function

__authors__ = "Bogdanov Evgeniy"
__email__ = "evbogdanov@yandex-team.ru"

import unittest
import sys
from datetime import datetime, timedelta

sys.path.append(".")
import stocks3
from stocks3.core.source import SourceLoader
from stocks3.export.exporter import make_exporter


MAXIMUM_PERIOD = timedelta(7)


class QuoteEmulator(object):
    """
    Грязный хак, позволяющий за счет ducktyping'а  съэмулировать поведение котировок
    """
    def __init__(self, quote_id):
        self.quote_id = quote_id


class GasImportsTest(unittest.TestCase):
    def setUp(self):
        stocks3.load_modules()
        loader = SourceLoader()
        self.exporter = make_exporter(loader.get_active_sources())
        self.quotes_for_check = [(20001, 'АИ-92', 213),
                                 (20002, 'АИ-95', 213),
                                 (20003, 'АИ-98', 213),
                                 (20010, 'ДТ', 213),
                                 (20012, 'АИ-92 среднее', 225),
                                 (20013, 'АИ-95 среднее', 225),
                                 (20014, 'АИ-98 среднее', 225),
                                 (20015, 'ДТ среднее', 225)]

    def test_dates_from_db(self):
        if datetime.now().month == 1 and 1 <= datetime.now().day <= 10:
            return
        for quote_id, name, region in self.quotes_for_check:
            q = QuoteEmulator(quote_id)

            max_date = self.exporter.reader.db.select_max_unixtime(q, region)
            if max_date is None:
                continue
            max_date = datetime.fromtimestamp(max_date)
            self.assertTrue(max_date + MAXIMUM_PERIOD > datetime.now(), "Gas quotes is too old")


if __name__ == "__main__":
    suite = unittest.TestLoader().loadTestsFromTestCase(GasImportsTest)
    result = unittest.TestResult()
    suite.run(result)
    failure = 0 if len(result.errors) == 0 and len(result.failures) == 0 else 2
    message = 'Tests run: {0}, Errors: {1}, Failures: {2}'.format(result.testsRun, len(result.errors),
                                                                  len(result.failures))

    [print(err[0], err[1]) for err in result.errors]
    print('PASSIVE-CHECK:{0};{1};{2}'.format('StocksImportTests', failure, message))
