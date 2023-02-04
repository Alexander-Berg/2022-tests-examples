#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Тестируем боевую конфигурацию :)
"""
import unittest
import sys
from datetime import datetime, timedelta
import json

sys.path.append(".")
import stocks3
from stocks3.core.source import SourceLoader
from stocks3.share.curl import load_url_data_retry
from stocks3.export.exporter import make_exporter


__authors__ = "Bogdanov Evgeny"
__email__ = "evbogdanov@yandex-team.ru"


# В качестве значения в данном случае допустимая разница между датой котировки и текущей.
# отрицательное значение говорит о том, что котировка должна быть установлена на будущую дату
quotes_list = {
    # 5: -1,  # Бивалютная корзина
    6: 0,  # EUR/USD кросс
    7: 0,  # USD/JPY кросс
    32: 0,  # GBP/USD кросс
    2000: 0,  # USD/RUB биржа
    2002: 0,  # EUR/RUB биржа

    10: 0,  # Золото Comex
    12: 2,  # Dow индекс
    17: 2,  # NASDAQ индекс
    29: 0,  # Акции GAZP
    42: 2,  # Акции YNDX
    43: 0,  # Акции YNDX(MOEX)
    50: 0,  # RTSI индекс
    54: 0,  # Акции Lukoil
    58: 0,  # Акции Rostelecom
    60: 0,  # Акции Сбербанк
    61: 0,  # Акции Сургутнефтегаз
    # 64: 0,  # Акции АвтоВАЗ
    68: 0,  # Акции Северсталь
    69: 0,  # Акции Корильский никель
    89: 0,  # Акции Аэрофлот
    183: 0,  # Акции ВТБ
    193: 0,  # Акции OGKE
    235: 0,  # Акции MTSI
    282: 0,  # Акции ROSN
    # 379: 0,  # Акции URKA
    1006: 0,  # Brent Oil
    1008: 0,  # Brent Oil
    # 1007: 0,  # Индекс Гонконгской биржи
    1013: 0,  # Индекс ММВБ

    1500: 0,  # Алюминий LME
    1501: 0,  # Медь LME
    1502: 0,  # Никель LME
    # 1504: 0,  # Палладий NYMEX
    # 1505: 0,  # Платина NYMEX
    # 1506: 0,  # Серебро COMEX
    # 1507: 1,  # Цинк LME
    2400: 0,  # Индекс ММВБ-10
    # 2503: 0,  # Индекс РТС-2 - выключен
    2505: 0,  # Индекс Nikkei
    2506: 2,  # Индекс SP500
    # 3001: 0,  # GBP/гривна НБУ
    3005: 0,  # USD/гривна НБУ
    # 3009: 0,  # KZT
    # 3015: 0,  # RUB
    # 3024: 0,  # CHF
    # 3026: 0,  # JPY
    # 3027: 0,  # EUR
    # 3033: 0,  # BYN
    4010: 0,  # EUR/BYN
    4011: 0,  # USD/BYN
    # 4012: 0,  # CHF/BYN
    # 4013: 0,  # GBP/BYN
    # 4014: 0,  # JPY/BYN
    # 4015: 0,  # UAH/BYN
    # 4016: 0,  # KZT/BYN
    4020: 0,  # RUB/BYN
    5000: 0,  # USD NBRK
    5001: 0,  # EUR
    5002: 0,  # RUB
    # 5003: 0,  # CHF
    # 5004: 0,  # GBP
    # 5005: 0,  # JPY
    # 5006: 0,  # UAH
    # 5009: 0,  # BYN
    5100: 2,  # KASE индекс
    10012: 0,  # UZS/RUB cbr
    10014: 0,  # TJS/RUB cbr
    10016: 0,
    10017: 0,
    10021: 0,
    10022: 0,
    10040: 0,
    # 20001: 0,  # АИ-92
    # 20002: 0,  # 95 бензин
    # 20010: 0,  # дизель
    # 40001: 0,  # GBP/UAH наличный
    # 40010: 0,
    # 40011: 0,
    # 40023: 0,
    # 40035: 0,
    # 40036: 0,
    # 40042: 0,
    # 40043: 0,
    # 40054: 0,
    # 40055: 0,
    # 40056: 0,
    # 40057: 0,
    # 40058: 0,  # Золото Турция
    # 40080: 0,  # Золото Турция dovis.com

    10000: -1,  # USD cbr
    10001: -1,  # EUR cbr
    # 10004: -1,  # GBP cbr
    # 10005: -1,  # JPY cbr
    # 10006: -1,  # UAH cbr
    # 10007: -1,
    # 10009: -1,
    # 10011: -1,
    # 10018: -1,
    # 10019: -1,
    # 10020: -1,
    40048: 0,
    40052: 0,
    40053: 0,
    40090: 0,
    40091: 0,
    40092: 0,
    71000: 1,
    71001: 1,
    71002: 1,

    # 40010: 0,
    # 40036: 0,
    # 40042: 0,
    # 40043: 0,
}


class CalendarChecker(object):
    def __init__(self):
        self._end = (datetime.now() + timedelta(days=15)).date()
        self._start = (datetime.now() - timedelta(days=15)).date()
        self._cache = {}

    def fetch(self, country):
        url = "https://api.calendar.yandex-team.ru/intapi/get-holidays?from={0}&to={1}&outMode=all" \
              "&for={2}&who_am_i=home.stocks".format(self._start, self._end, country)
        data = load_url_data_retry(url).decode("utf8")
        days = json.loads(data)

        self._cache[str(country)] = {}
        for day in days['holidays']:
            self._cache[str(country)][day['date']] = day['type'] in ["holiday", "weekend"]

    def get(self, country='225'):
        country = str(country)
        if country not in self._cache:
            self.fetch(country)
        return self._cache[country]

    def check(self, date, country='225'):
        return self.get(country)[date]


class ActualTest(unittest.TestCase):
    def setUp(self):
        stocks3.load_modules()
        loader = SourceLoader()
        self.sources = loader.get_active_sources()
        self.exporter = make_exporter(self.sources, [], [])
        self.calendar = CalendarChecker()

    def get_critical_date(self, now_datetime, date_factor):
        # До определенного часа не учиваем сегодняшний день.
        # Сама переменная указывает, сколько допустимо отсутствие данных.
        rest_days = 1 if now_datetime.time().hour < 13 else 0
        critical_day = now_datetime.date() - timedelta(days=rest_days)

        # Проверяем остальные прошедшие дни на то что они выходные
        while date_factor != 0 or self.calendar.check(str(critical_day)):
            if date_factor > 0 or self.calendar.check(str(critical_day)):
                if not self.calendar.check(str(critical_day)):
                    date_factor -= 1
                critical_day -= timedelta(days=1)
            else:
                date_factor += 1
                critical_day += timedelta(days=1)
        return critical_day

    def test_actual_quotes(self):
        fails = []

        data = self.exporter.reader.db.stocks.aggregate([
            {"$match": {"id": {"$in": list(quotes_list.keys())}, "disabled": {"$ne": True}}},
            {"$sort": {"date": 1}},
            {"$group": {"_id": {"id": "$id",
                                "region": "$region"},
                        "lastDate": {"$last": "$date"}}}
        ])

        if isinstance(data, dict):  # fallback для старых монг
            data = data["result"]

        for record in data:
            quote_id = int(record["_id"]["id"])
            quote_region = int(record["_id"]["region"])
            quote_date = datetime.strptime(record["lastDate"], '%Y-%m-%d').date()

            # Для котировок с датами "на завтра" или "на вчера" делаем смещение
            if quote_id in quotes_list:
                critical_date = self.get_critical_date(datetime.now(), quotes_list[quote_id])
            else:
                raise Exception('something happened!')

            if quote_date < critical_date:
                fails.append((quote_id, quote_region, quote_date))

        message = 'old stocks: ' + ' % '.join(['{0}:{1}'.format((fail[0]), (fail[1])) for fail in fails])
        self.assertEqual(len(fails), 0, message)


if __name__ == "__main__":
    unittest.main()
