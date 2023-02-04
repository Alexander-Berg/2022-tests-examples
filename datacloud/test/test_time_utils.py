# -*- coding: utf-8 -*-
import unittest
from freezegun import freeze_time
from datacloud.dev_utils.time import utils as time_utils, patterns


class TestTimeUtils(unittest.TestCase):
    def test_assert_date_str(self):
        correct_date = '2019-05-24'
        self.assertIsNone(time_utils.assert_date_str(correct_date))

        pattern_date = '2019-05'
        self.assertIsNone(time_utils.assert_date_str(pattern_date, patterns.RE_MONTH_LOG_FORMAT))

        with self.assertRaises(AssertionError):
            time_utils.assert_date_str(correct_date, patterns.RE_MONTH_LOG_FORMAT)

        wrong_date = '019-05-24'
        with self.assertRaises(AssertionError):
            time_utils.assert_date_str(wrong_date)

    @freeze_time('2019-05-24')
    def test_now_str(self):
        self.assertEqual(time_utils.now_str(), '2019-05-24 00:00:00')
        self.assertEqual(time_utils.now_str(patterns.FMT_DATE_YM), '2019-05')

    @freeze_time('2019-05-24 00:24:45')
    def test_now_floor_str(self):
        self.assertEqual(time_utils.now_floor_str(), '2019-05-24T00:20:00')

    @freeze_time('2019-05-24 00:01:53')
    def test_now_floor_str_2(self):
        self.assertEqual(time_utils.now_floor_str(), '2019-05-24T00:00:00')

    @freeze_time('2019-05-24 23:58:42')
    def test_now_floor_str_3(self):
        self.assertEqual(time_utils.now_floor_str(minutes=10), '2019-05-24T23:50:00')
