# -*- coding: utf-8 -*-

import datetime as dt
import decimal
import unittest

import balance.money_format as moneyfmt
from balance import muzzle_util as ut
from balance.check_inn import check_inn_ru
from balance.xmlizer import xml_text


class TestMoneyFormat(unittest.TestCase):
    def testRUR(self):
        self.assertEqual(u'Ноль рублей 00 копеек', moneyfmt.written_money(decimal.Decimal('0')))
        self.assertEqual(u'Ноль рублей 10 копеек', moneyfmt.written_money(decimal.Decimal('0.1')))
        self.assertEqual(u'Сто один рубль 21 копейка', moneyfmt.written_money(decimal.Decimal('101.21')))
        self.assertEqual(u'Сто тринадцать рублей 33 копейки', moneyfmt.written_money(decimal.Decimal('113.33')))
        self.assertEqual(u'Сто двадцать пять рублей 45 копеек', moneyfmt.written_money(decimal.Decimal('125.45')))
        self.assertEqual(u'Сто тридцать семь рублей 57 копеек', moneyfmt.written_money(decimal.Decimal('137.57')))
        self.assertEqual(u'Десять тысяч пятьсот рублей 25 копеек', moneyfmt.written_money(decimal.Decimal('10500.25')))
        self.assertEqual(u'Сто тысяч пятьсот рублей 25 копеек', moneyfmt.written_money(decimal.Decimal('100500.25')))
        self.assertEqual(u'Три миллиона сто тысяч пятьсот рублей 25 копеек',
                         moneyfmt.written_money(decimal.Decimal('3100500.25')))

    def testUSD(self):
        self.assertEqual(u"Ноль долларов 00 центов", moneyfmt.written_money(decimal.Decimal('0'), 'usd'))
        self.assertEqual(u"Один доллар 01 цент", moneyfmt.written_money(decimal.Decimal('1.01'), 'usd'))
        self.assertEqual(u"Три доллара 03 цента", moneyfmt.written_money(decimal.Decimal('3.03'), 'usd'))
        self.assertEqual(u"Пять долларов 05 центов", moneyfmt.written_money(decimal.Decimal('5.05'), 'usd'))

    def testEUR(self):
        self.assertEqual(u"Ноль евро 00 центов", moneyfmt.written_money(decimal.Decimal('0'), 'eur'))
        self.assertEqual(u"Один евро 01 цент", moneyfmt.written_money(decimal.Decimal('1.01'), 'eur'))
        self.assertEqual(u"Три евро 03 цента", moneyfmt.written_money(decimal.Decimal('3.03'), 'eur'))
        self.assertEqual(u"Пять евро 05 центов", moneyfmt.written_money(decimal.Decimal('5.05'), 'eur'))

    def testGBP(self):
        self.assertEqual(u"Ноль фунтов стерлингов 00 пенсов", moneyfmt.written_money(decimal.Decimal('0'), 'gbp'))
        self.assertEqual(u"Один фунт стерлингов 01 пенни", moneyfmt.written_money(decimal.Decimal('1.01'), 'gbp'))
        self.assertEqual(u"Три фунта стерлингов 03 пенса", moneyfmt.written_money(decimal.Decimal('3.03'), 'gbp'))
        self.assertEqual(u"Пять фунтов стерлингов 05 пенсов", moneyfmt.written_money(decimal.Decimal('5.05'), 'gbp'))

    def testUAH(self):
        self.assertEqual(u"Нуль гривень 00 копійок", moneyfmt.written_money(decimal.Decimal('0'), 'uah'))
        self.assertEqual(u"Нуль гривень 10 копійок", moneyfmt.written_money(decimal.Decimal('0.1'), 'uah'))
        self.assertEqual(u"Сто одна гривна 21 копійка", moneyfmt.written_money(decimal.Decimal('101.21'), 'uah'))
        self.assertEqual(u"Сто тринадцять гривень 33 копійки", moneyfmt.written_money(decimal.Decimal('113.33'), 'uah'))
        self.assertEqual(u"Сто двадцять п'ять гривень 45 копійок",
                         moneyfmt.written_money(decimal.Decimal('125.45'), 'uah'))
        self.assertEqual(u"Сто тридцять сім гривень 57 копійок",
                         moneyfmt.written_money(decimal.Decimal('137.57'), 'uah'))
        self.assertEqual(u"Десять тисяч п'ятсот гривень 25 копійок",
                         moneyfmt.written_money(decimal.Decimal('10500.25'), 'uah'))
        self.assertEqual(u"Сто тисяч п'ятсот гривень 25 копійок",
                         moneyfmt.written_money(decimal.Decimal('100500.25'), 'uah'))
        self.assertEqual(u"Три мільйона сто тисяч п'ятсот гривень 25 копійок",
                         moneyfmt.written_money(decimal.Decimal('3100500.25'), 'uah'))

    def testYSTBN(self):
        self.assertEqual(u"Ноль бонусов", moneyfmt.written_money(decimal.Decimal('0'), 'ystbn'))
        self.assertRaises(ut.INVALID_PARAM, moneyfmt.written_money, decimal.Decimal('0.1'), 'ystbn')
        self.assertRaises(ut.INVALID_PARAM, moneyfmt.written_money, decimal.Decimal('0.01'), 'ystbn')
        self.assertEqual(u"Три бонуса", moneyfmt.written_money(decimal.Decimal('3'), 'ystbn'))
        self.assertEqual(u"Сорок два бонуса", moneyfmt.written_money(decimal.Decimal('42'), 'ystbn'))
        self.assertEqual(u"Сто один бонус", moneyfmt.written_money(decimal.Decimal('101'), 'ystbn'))
        self.assertEqual(u"Сто тринадцать бонусов", moneyfmt.written_money(decimal.Decimal('113'), 'ystbn'))
        self.assertEqual(u"Сто двадцать пять бонусов", moneyfmt.written_money(decimal.Decimal('125'), 'ystbn'))
        self.assertEqual(u"Одна тысяча три бонуса", moneyfmt.written_money(decimal.Decimal('1003'), 'ystbn'))
        self.assertEqual(u"Сто тысяч пятьсот бонусов", moneyfmt.written_money(decimal.Decimal('100500'), 'ystbn'))


class TestCheckInn(unittest.TestCase):
    def testInnFail(self):
        self.assert_(not check_inn_ru('abcdefghij'))
        self.assert_(not check_inn_ru('123'))
        self.assert_(not check_inn_ru('1234567890'))
        self.assert_(not check_inn_ru('1a234567890'))

    def testInnOk(self):
        self.assert_(check_inn_ru('7703599768'))
        self.assert_(check_inn_ru('773315815790'))


class TestMonths(unittest.TestCase):
    def testSameMonth(self):
        self.assert_(ut.is_same_month(dt.datetime(2001, 1, 10), dt.datetime(2001, 1, 11)))
        self.assert_(not ut.is_same_month(dt.datetime(2000, 1, 10), dt.datetime(2001, 1, 11)))
        self.assert_(not ut.is_same_month(dt.datetime(2001, 1, 10), dt.datetime(2001, 2, 11)))
        self.assert_(not ut.is_same_month(dt.datetime(2000, 1, 10), dt.datetime(2001, 2, 11)))

    def testNextMonth(self):
        self.assert_(ut.is_next_month(dt.datetime(2001, 1, 10), dt.datetime(2001, 2, 11)))
        self.assert_(ut.is_next_month(dt.datetime(2001, 12, 10), dt.datetime(2002, 1, 11)))
        self.assert_(not ut.is_next_month(dt.datetime(2001, 1, 10), dt.datetime(2001, 3, 11)))
        self.assert_(not ut.is_next_month(dt.datetime(2001, 12, 10), dt.datetime(2002, 2, 11)))

    def testAddMonth(self):
        self.assertEqual(ut.add_months_to_date(dt.datetime(1999, 12, 31), 2), dt.datetime(2000, 2, 29))
        self.assertEqual(ut.add_months_to_date(dt.datetime(1998, 12, 31), 2), dt.datetime(1999, 2, 28))
        self.assertEqual(ut.add_months_to_date(dt.datetime(1999, 12, 1), 31), dt.datetime(2002, 7, 1))
        self.assertEqual(ut.add_months_to_date(dt.datetime(2020, 1, 1), 4), dt.datetime(2020, 5, 1))
        self.assertEqual(ut.add_months_to_date(dt.datetime(2005, 5, 3), 1), dt.datetime(2005, 6, 3))
        self.assertEqual(ut.add_months_to_date(dt.datetime(2006, 7, 27), 15), dt.datetime(2007, 10, 27))
        self.assertEqual(ut.add_months_to_date(dt.datetime(2001, 3, 31), 1), dt.datetime(2001, 4, 30))

    def testSubstructMonth(self):
        self.assertEqual(ut.add_months_to_date(dt.datetime(2008, 2, 29), -12), dt.datetime(2007, 2, 28))
        self.assertEqual(ut.add_months_to_date(dt.datetime(2008, 2, 29), -24), dt.datetime(2006, 2, 28))
        self.assertEqual(ut.add_months_to_date(dt.datetime(2008, 2, 29), -48), dt.datetime(2004, 2, 29))
        self.assertEqual(ut.add_months_to_date(dt.datetime(2010, 5, 31), -1), dt.datetime(2010, 4, 30))


class TestGetNullable(unittest.TestCase):
    def test_int(self):
        self.assertEqual(None, ut.get_dict_nullable_int({'x': '', 'y': '-3'}, 'x'))
        self.assertEqual(None, ut.get_dict_nullable_int({'x': '', 'y': '-3'}, 'z'))
        self.assertEqual(-3, ut.get_dict_nullable_int({'x': '', 'y': '-3'}, 'y'))
        self.assertRaises(
            ut.INVALID_PARAM,
            lambda: ut.get_dict_nullable_int({'x': 'a', 'y': '-3'}, 'x'),
        )

    def test_dt(self):
        self.assertEqual(None, ut.get_dict_nullable_dt({'x': '', 'y': '2007-04-10T13:40:05'}, 'x'))
        self.assertEqual(None, ut.get_dict_nullable_dt({'x': '', 'y': '2007-04-10T13:40:05'}, 'z'))
        self.assertEqual(dt.datetime(2007, 04, 10, 13, 40, 05),
                         ut.get_dict_nullable_dt({'x': '', 'y': '2007-04-10T13:40:05'}, 'y'))
        self.assertRaises(
            ut.INVALID_PARAM,
            lambda: ut.get_dict_nullable_dt({'x': 'a', 'y': '2007-04-10T13:40:05'}, 'x'),
        )

    def test_decimal(self):
        self.assertEqual(None, ut.get_dict_nullable_decimal({'x': '', 'y': '-3.14'}, 'x'))
        self.assertEqual(None, ut.get_dict_nullable_decimal({'x': '', 'y': '-3.14'}, 'z'))
        self.assertEqual(decimal.Decimal('-3.14'), ut.get_dict_nullable_decimal({'x': '', 'y': '-3.14'}, 'y'))
        self.assertRaises(
            ut.INVALID_PARAM,
            lambda: ut.get_dict_nullable_decimal({'x': 'a', 'y': '-3.14'}, 'x'),
        )
        self.assertEqual(decimal.Decimal('-1003.14'), ut.get_dict_nullable_decimal({'x': 'a', 'y': '-1 003,14'}, 'y'))

    def test_xml_text(self):
        self.assertEqual('1', xml_text(1))
        self.assertEqual('2007-04-10T13:40:05',
                         xml_text(dt.datetime(2007, 4, 10, 13, 40, 5, 123)))
        self.assertEqual('1', xml_text(True))
        self.assertEqual('0', xml_text(False))
        self.assertEqual('', xml_text(None))
        self.assertEqual('1', xml_text(decimal.Decimal('1')))
        self.assertEqual('1.01000', xml_text(decimal.Decimal('1.01000')))
        self.assertEqual('1.01', xml_text(1.01))


class TestState2Dict(unittest.TestCase):
    def test_no_prefix(self):
        self.assertEqual(
            {'x': u'123', 'y': u'превед'},
            ut.filter_dict({'x': u'123', 'y': u'превед'}, '')
        )
        self.assertEqual(
            {'x': u'123', 'y': u'превед'},
            ut.filter_dict({'x': u'123', 'y': u'превед'}, None)
        )

    def test_prefix(self):
        self.assertEqual(
            {'x': u'123', 'z': u'1'},
            ut.filter_dict({'qx': u'123', 'q': u'0', 'zq': u'1', 'y': u'превед'}, 'q')
        )

    def test_re(self):
        self.assertEqual(
            {'qx': u'123', 'q': u'0'},
            ut.filter_dict({'qs-x': u'123', 'qs_': u'0', 'y': u'превед'}, 's[-_]')
        )
