import datetime
import unittest
from dateutil.relativedelta import relativedelta

from maps.analyzer.libs.data.lib.time_data import Time, TimePeriod


class TimeTest(unittest.TestCase):
    def test_time(self):
        time_str = "20111010T000000"
        for micro_suf, micro,  suffix in [
                ("", 0, ""),
                (".0", 0, ""),
                (".1", 100000, ".100000"),
                (".000001", 1, ".000001"),
                (".000000", 0, ""),
                (".500000", 500000, ".500000")]:
            string = time_str + micro_suf
            time = Time.from_string(string)
            self.assertEqual(time.year, 2011)
            self.assertEqual(time.month, 10)
            self.assertEqual(time.day, 10)
            self.assertEqual(time.hour, 0)
            self.assertEqual(time.minute, 0)
            self.assertEqual(time.second, 0)
            self.assertEqual(time.microsecond, micro)
            self.assertEqual(str(time), time_str + suffix)

        time = Time.from_timestamp(1)
        self.assertEqual(time.year, 1970)
        self.assertEqual(time.month, 1)
        self.assertEqual(time.day, 1)
        self.assertEqual(time.hour, 0)
        self.assertEqual(time.minute, 0)
        self.assertEqual(time.second, 1)
        self.assertEqual(time.microsecond, 0)

        self.assertRaises(TypeError, lambda x: Time.fromtimestamp(1))

        time = Time.relative_date_from_today(days=1, months=2, years=3)
        relative = relativedelta(time.date(), datetime.date.today())
        self.assertEqual(relative, relativedelta(days=1, months=2, years=3))

        time = Time.relative_date_from_today(months=-33)
        relative = relativedelta(time.date(), datetime.date.today())
        self.assertEqual(relative, relativedelta(months=-33))

    def test_to_timestamp(self):
        for timestamp in (1, 2, 3, 4, 5, 3600, 7200, 38923, 9292831, int(2e+9)):
            time = Time.from_timestamp(timestamp)
            self.assertEqual(time.to_timestamp(), timestamp)

    def test_to_timestamp_subseconds(self):
        TIME = '20170101T000000.1234'
        time = Time.from_string(TIME)
        self.assertEqual(time.to_timestamp(), 1483228800.1234)

    def test_add(self):
        for a, b in ((1, 2), (320, 760), (3783271, 316732)):
            self.assertEqual(
                (Time.from_timestamp(a) + datetime.timedelta(seconds=b)).to_timestamp(),
                a + b
            )
            self.assertEqual(
                (Time.from_timestamp(a) + datetime.timedelta(seconds=b, minutes=b)).to_timestamp(),
                a + b * 61
            )
            time = Time.from_timestamp(a)
            time += datetime.timedelta(seconds=b)
            self.assertEqual(time.to_timestamp(), a + b)

    def test_sub(self):
        for a, b in ((138192, 2), (123220, 760), (331783271, 316732)):
            self.assertEqual(
                (Time.from_timestamp(a) - datetime.timedelta(seconds=b)).to_timestamp(),
                a - b
            )
            self.assertEqual(
                (Time.from_timestamp(a) - datetime.timedelta(seconds=b, minutes=b)).to_timestamp(),
                a - b * 61
            )
            time = Time.from_timestamp(a)
            time -= datetime.timedelta(seconds=b)
            self.assertEqual(time.to_timestamp(), a - b)

            self.assertEqual(
                Time.from_timestamp(a) - Time.from_timestamp(b),
                datetime.timedelta(seconds=a - b)
            )


class TimePeriodTest(unittest.TestCase):
    def test_time_period(self):
        test_time_period = TimePeriod.from_string("19900611T102000 20111205T134901")
        self.assertEqual(test_time_period.begin, Time.from_string("19900611T102000"))
        self.assertEqual(test_time_period.end, Time.from_string("20111205T134901"))

        self.assertTrue(test_time_period.begin in test_time_period)
        self.assertTrue(test_time_period.end in test_time_period)

        self.assertTrue(Time.from_string("20001228T141516") in test_time_period)
        self.assertFalse(Time.from_string("19900223T112347") in test_time_period)
        self.assertFalse(Time.from_string("20120101T000000") in test_time_period)

if __name__ == "__main__":
    unittest.main()
