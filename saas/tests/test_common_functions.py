# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import sys
import six
import mock
import pytz
import random
import requests
import unittest
from datetime import datetime
from tzlocal import get_localzone


from saas.library.python.common_functions import wait_minute, random_hexadecimal_string, connection_error, round_up, \
    get_datetime_timestamp


class TestCommonFunctions(unittest.TestCase):
    @mock.patch('time.sleep')
    def test_wait_minute(self, m):
        wait_minute()
        m.assert_called_once_with(60)

    def test_random_hexadecimal_string(self):
        test_len = random.randint(1, 256)
        s = random_hexadecimal_string(test_len)
        self.assertIsInstance(s, six.string_types)
        self.assertEqual(len(s), test_len, '{} has invalid length'.format(s))
        self.assertRegexpMatches(s, r'^[0-9a-f]+$', '{} is not hexadecimal'.format(s))

        if sys.version_info > (3, 9):
            self.assertEqual(random_hexadecimal_string(0), '0')
        else:
            with self.assertRaises(ValueError):
                random_hexadecimal_string(0)

        with self.assertRaises(ValueError):
            random_hexadecimal_string(-4)

    def test_connection_error(self):
        ex = requests.exceptions.ConnectionError()
        self.assertTrue(connection_error(ex))
        self.assertFalse(connection_error(Exception()))

    def test_round_up(self):
        self.assertEqual(round_up(1/3, 3), 0.334)
        self.assertEqual(round_up(1/6, 5), 0.16667)
        self.assertEqual(round_up(1/7, 7), 0.1428572)

    def test_get_datetime_timestamp(self):
        naive_dt = datetime(2021, 4, 5, 16, 30)
        local_tz = get_localzone()
        local_offset = local_tz.utcoffset(naive_dt)
        self.assertEqual(get_datetime_timestamp(naive_dt), 1617640200 + local_offset.total_seconds())

        aware_utc_dt = datetime(2021, 4, 5, 12, 30, tzinfo=pytz.utc)
        self.assertEqual(get_datetime_timestamp(aware_utc_dt), 1617625800)

        aware_msk_dt = pytz.timezone('Europe/Moscow').localize(datetime(2021, 4, 5, 16, 00))
        self.assertEqual(get_datetime_timestamp(aware_msk_dt), 1617627600)
