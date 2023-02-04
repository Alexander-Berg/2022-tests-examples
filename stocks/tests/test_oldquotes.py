#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Тестируем боевую конфигурацию :)
"""
import unittest
import sys
from datetime import datetime, timedelta

__authors__ = "Bogdanov Evgeniy"
__email__ = "evbogdanov@yandex-team.ru"

sys.path.append(".")
import stocks3
from stocks3.core.source import SourceLoader
from stocks3.export.exporter import make_exporter


class OldquotesTest(unittest.TestCase):
    def setUp(self):
        stocks3.load_modules()
        loader = SourceLoader()
        self.exporter = make_exporter(loader.get_active_sources())

    def test_oldest_quotes(self):
        if datetime.now().month == 1 and 1 <= datetime.now().day <= 10:
            return

        fails = []
        quote_set = {}
        for quote in self.exporter.list_of_quotes_for_export():
            if not quote.has_flag('item') or quote.has_flag('ignore_dates'):
                continue
            quote_set[quote.quote_id] = quote
        data = self.exporter.reader.db.stocks.aggregate([
            {"$match": {"disabled": {"$ne": True}, "discontinued": {"$ne": True}}},
            {"$sort": {"date": 1}},
            {"$group": {"_id": {"id": "$id",
                                "region": "$region"},
                        "lastDate": {"$last": "$date"}}}
        ])

        if isinstance(data, dict):  # fallback для старых монг
            data = data["result"]

        for record in data:
            quote_id = record["_id"]["id"]
            quote_region = record["_id"]["region"]
            quote_date = record["lastDate"]
            if quote_id not in list(quote_set.keys()):  # or max_date is None:
                continue
            if datetime.now().date() - datetime.strptime(quote_date, '%Y-%m-%d').date() > timedelta(days=3):
                fails.append(
                    (quote_set[quote_id].name, quote_id, quote_region, quote_date, quote_set[quote_id].source.sourceId))

        message = 'old stocks: ' + ' % '.join(['{1}:{2}'.format((fail[0]), (fail[1]), (fail[2])) for fail in fails[0:]])
        self.assertEqual(len(fails), 0, message)


if __name__ == "__main__":
    unittest.main()
