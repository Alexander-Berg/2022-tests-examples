# coding: utf-8

import unittest
import datetime

from agency_rewards.rewards.utils.dates import (
    get_last_dt_prev_month,
    get_first_dt_n_month_ago,
    get_first_dt_prev_month,
    format_short_dt,
    format_long_dt,
    format_yt,
    to_iso,
    from_iso,
    get_quarter_last_day_greg,
    get_previous_quarter_first_day_greg,
    get_quarter_first_day_greg,
    HYDateCtl,
    get_financial_quarter,
    get_financial_hy,
)


class TestGetLastDtPrevMonth(unittest.TestCase):
    def test_zero_seconds(self):
        dt = datetime.datetime(2018, 1, 1)
        self.assertEqual(get_last_dt_prev_month(dt), datetime.datetime(2017, 12, 31))

    def test_full_seconds(self):
        dt = datetime.datetime(2018, 1, 1)
        self.assertEqual(get_last_dt_prev_month(dt, zero_seconds=0), datetime.datetime(2017, 12, 31, 23, 59, 59))

    def test_n_months_ago(self):
        dt = datetime.datetime(2018, 12, 1)
        self.assertEqual(get_first_dt_n_month_ago(dt, 11), datetime.datetime(2018, 1, 1))


class TestGetFirstDtPrevMonth(unittest.TestCase):
    def test_func(self):
        dt = datetime.datetime(2018, 1, 1)
        self.assertEqual(get_first_dt_prev_month(dt), datetime.datetime(2017, 12, 1))


class TestFormatShortDt(unittest.TestCase):
    def test_func(self):
        dt = datetime.datetime(2018, 3, 9)
        self.assertEqual(format_short_dt(dt), '201803')


class TestFormatLongDt(unittest.TestCase):
    def test_func(self):
        dt = datetime.datetime(2018, 3, 9)
        self.assertEqual(format_long_dt(dt), '2018-03-09')


class TestToISO(unittest.TestCase):
    def test_func(self):
        dt = datetime.datetime(2018, 1, 1)
        self.assertEqual(to_iso(dt), "2017-12-31T21:00:00.000000Z")


class TestFromSO(unittest.TestCase):
    def test_func(self):
        dt = datetime.datetime(2018, 1, 1).astimezone(tz=None)
        self.assertEqual(from_iso("2017-12-31T21:00:00.000000Z"), dt)


class TestFormatYt(unittest.TestCase):
    def test_func(self):
        dt = datetime.datetime(2019, 3, 1)
        self.assertEqual(format_yt(dt), '2019-02-28T21:00:00Z')


class TestGregorianQuarter(unittest.TestCase):
    def test_get_quarter_last_day_g(self):
        """
        Последний день грегорианского квартала
        """
        tests = [
            dict(dt=datetime.datetime(2019, 3, 1), expected=datetime.datetime(2019, 3, 31, 23, 59, 59)),
            dict(dt=datetime.datetime(2019, 1, 1), expected=datetime.datetime(2019, 3, 31, 23, 59, 59)),
            dict(dt=datetime.datetime(2019, 4, 1), expected=datetime.datetime(2019, 6, 30, 23, 59, 59)),
            dict(dt=datetime.datetime(2019, 6, 30, 23, 59, 59), expected=datetime.datetime(2019, 6, 30, 23, 59, 59)),
            dict(dt=datetime.datetime(2019, 9, 30, 23, 59, 59), expected=datetime.datetime(2019, 9, 30, 23, 59, 59)),
            dict(dt=datetime.datetime(2019, 7, 1), expected=datetime.datetime(2019, 9, 30, 23, 59, 59)),
            dict(dt=datetime.datetime(2019, 11, 23, 23, 59, 59), expected=datetime.datetime(2019, 12, 31, 23, 59, 59)),
            dict(dt=datetime.datetime(2019, 10, 1), expected=datetime.datetime(2019, 12, 31, 23, 59, 59)),
        ]

        for test in tests:
            self.assertEqual(get_quarter_last_day_greg(test['dt']), test['expected'])

    def test_prev_quarter_first_day_g(self):
        """
        Первый день предыдущего грегорианского квартала
        """
        tests = [
            dict(dt=datetime.datetime(2019, 3, 1), expected=datetime.datetime(2018, 10, 1, 0, 0, 0)),
            dict(dt=datetime.datetime(2019, 4, 1), expected=datetime.datetime(2019, 1, 1, 0, 0, 0)),
            dict(dt=datetime.datetime(2019, 9, 30, 23, 59, 59), expected=datetime.datetime(2019, 4, 1, 0, 0, 0)),
            dict(dt=datetime.datetime(2019, 11, 23, 23, 59, 59), expected=datetime.datetime(2019, 7, 1, 0, 0, 0)),
        ]

        for test in tests:
            self.assertEqual(get_previous_quarter_first_day_greg(test['dt']), test['expected'])

    def test_get_quarter_first_day_g(self):
        """
        Первый день грегорианского квартала
        """
        tests = [
            dict(dt=datetime.datetime(2019, 3, 1), expected=datetime.datetime(2019, 1, 1)),
            dict(dt=datetime.datetime(2019, 1, 1), expected=datetime.datetime(2019, 1, 1)),
            dict(dt=datetime.datetime(2019, 4, 1), expected=datetime.datetime(2019, 4, 1)),
            dict(dt=datetime.datetime(2019, 6, 30, 23, 59, 59), expected=datetime.datetime(2019, 4, 1)),
            dict(dt=datetime.datetime(2019, 9, 30, 23, 59, 59), expected=datetime.datetime(2019, 7, 1)),
            dict(dt=datetime.datetime(2019, 7, 5, 23, 59, 59), expected=datetime.datetime(2019, 7, 1)),
            dict(dt=datetime.datetime(2019, 11, 23, 23, 59, 59), expected=datetime.datetime(2019, 10, 1)),
            dict(dt=datetime.datetime(2019, 12, 31, 23, 59, 59), expected=datetime.datetime(2019, 10, 1)),
        ]

        for test in tests:
            self.assertEqual(get_quarter_first_day_greg(test['dt']), test['expected'])


class TestHYDateCtl(unittest.TestCase):
    def test_get_prev_hy_range(self):
        year = 2016
        curr_dt = datetime.datetime(year=year, month=8, day=12)
        ranges = HYDateCtl.get_prev_hy_ranges(curr_dt)
        self.assertEqual(ranges['prev_hy_start'], datetime.datetime(2015, 9, 1))
        self.assertEqual(ranges['prev_hy_end'], datetime.datetime(2016, 2, 29, 23, 59, 59))

        curr_dt = datetime.datetime(year=year, month=2, day=20)
        ranges = HYDateCtl.get_prev_hy_ranges(curr_dt)
        self.assertEqual(ranges['prev_hy_start'], datetime.datetime(2015, 3, 1))
        self.assertEqual(ranges['prev_hy_end'], datetime.datetime(2015, 8, 31, 23, 59, 59))

        curr_dt = datetime.datetime(year=year, month=10, day=20)
        ranges = HYDateCtl.get_prev_hy_ranges(curr_dt)
        self.assertEqual(ranges['prev_hy_start'], datetime.datetime(2016, 3, 1))
        self.assertEqual(ranges['prev_hy_end'], datetime.datetime(2016, 8, 31, 23, 59, 59))

    def test_get_prev_hy_first_day_greg(self):
        curr_dt = datetime.datetime(year=2016, month=2, day=20)
        prev_hy_first_day_greg = HYDateCtl.get_prev_hy_first_day_greg(curr_dt)
        self.assertEqual(prev_hy_first_day_greg, datetime.datetime(2015, 7, 1))

        curr_dt = datetime.datetime(year=2016, month=10, day=20)
        prev_hy_first_day_greg = HYDateCtl.get_prev_hy_first_day_greg(curr_dt)
        self.assertEqual(prev_hy_first_day_greg, datetime.datetime(2016, 1, 1))

    def test_get_hy_last_day_greg(self):
        curr_dt = datetime.datetime(year=2016, month=2, day=20)
        hy_last_day_greg = HYDateCtl.get_hy_last_day_greg(curr_dt)
        self.assertEqual(hy_last_day_greg, datetime.datetime(2016, 6, 30, 23, 59, 59))

        curr_dt = datetime.datetime(year=2016, month=10, day=20)
        hy_last_day_greg = HYDateCtl.get_hy_last_day_greg(curr_dt)
        self.assertEqual(hy_last_day_greg, datetime.datetime(2016, 12, 31, 23, 59, 59))

    def test_get_hy_last_day(self):
        curr_dt = datetime.datetime(year=2016, month=5, day=20)
        hy_last_day_greg = HYDateCtl.get_hy_last_day(curr_dt)
        self.assertEqual(hy_last_day_greg, datetime.datetime(2016, 8, 31, 23, 59, 59))

        curr_dt = datetime.datetime(year=2015, month=10, day=20)
        hy_last_day_greg = HYDateCtl.get_hy_last_day(curr_dt)
        self.assertEqual(hy_last_day_greg, datetime.datetime(2016, 2, 29, 23, 59, 59))

        curr_dt = datetime.datetime(year=2015, month=2, day=20)
        hy_last_day_greg = HYDateCtl.get_hy_last_day(curr_dt)
        self.assertEqual(hy_last_day_greg, datetime.datetime(2015, 2, 28, 23, 59, 59))


class TestFinancialCalendar(unittest.TestCase):
    def test_get_financial_quarter(self):
        tests = [
            dict(dt=datetime.datetime(year=2019, month=2, day=20), expected=4),
            dict(dt=datetime.datetime(year=2019, month=4, day=1), expected=1),
            dict(dt=datetime.datetime(year=2019, month=6, day=30), expected=2),
            dict(dt=datetime.datetime(year=2019, month=10, day=2), expected=3),
        ]

        for test in tests:
            self.assertEqual(get_financial_quarter(test['dt']), test['expected'])

    def test_get_financial_hy(self):
        tests = [
            dict(dt=datetime.datetime(year=2019, month=2, day=20), expected=2),
            dict(dt=datetime.datetime(year=2019, month=8, day=22), expected=1),
        ]

        for test in tests:
            self.assertEqual(get_financial_hy(test['dt']), test['expected'])
