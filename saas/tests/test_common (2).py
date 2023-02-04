import time
import unittest

from mock import patch
from faker import Faker
from dateutil import tz
from datetime import datetime as dt
from datetime import timedelta


from saas.library.python.yasm.common import get_interval_begin_end, YasmPeriod


class TestYasmSignal(unittest.TestCase):
    fake = Faker()

    @patch('saas.library.python.yasm.common.get_current_timestamp', autospec=True)
    def test_interval_begin_end(self, timestamp_mock):
        interval_begin = self.fake.date_time(tzinfo=tz.UTC, end_datetime=(dt.now() - timedelta(seconds=900)))
        interval_begin_ts = int(time.mktime(interval_begin.utctimetuple()))

        interval_end = self.fake.date_time_between_dates(datetime_start=(interval_begin + timedelta(seconds=300)),
                                                         datetime_end=dt.now(tz=tz.UTC))
        interval_end_ts = int(time.mktime(interval_end.utctimetuple()))

        timestamp_mock.return_value = interval_end_ts
        interval_end_fixed = interval_end_ts - 25

        _, interval_begin_res, interval_end_res = get_interval_begin_end(
            YasmPeriod.five_seconds, interval_begin_ts, interval_end_ts)

        self.assertEqual(interval_end_fixed, interval_end_res)
        self.assertEqual(interval_begin_ts, interval_begin_res)
        timestamp_mock.assert_called_once()
        timestamp_mock.reset_mock()

        valid_interval_end = self.fake.date_time_between_dates(datetime_start=interval_begin,
                                                               datetime_end=(interval_end - timedelta(seconds=25)))
        valid_interval_end_ts = int(time.mktime(valid_interval_end.utctimetuple()))

        _, interval_begin_res, interval_end_res = get_interval_begin_end(
            YasmPeriod.five_seconds, int(interval_begin_ts), int(valid_interval_end_ts))

        timestamp_mock.assert_called_once()
        self.assertEqual(valid_interval_end_ts, interval_end_res)
        self.assertEqual(interval_begin_ts, interval_begin_res)
