from datetime import datetime
import os
import unittest

import mock

from maps.pylibs.monitoring.lib import timetail


class TimetailTests(unittest.TestCase):

    def setUp(self):
        self._testFile = "test.log"
        self._callback = mock.MagicMock()
        self._expected_calls = []

    def tearDown(self):
        if os.path.exists(self._testFile):
            os.remove(self._testFile)

    def log_time(self, timestamp):
        """
        Formats a timestamp to a local time string.
        :param int timestamp: POSIX timestamp.
        :return: String of format 'YYYY-mm-dd HH:MM:SS'.
        """
        return datetime.fromtimestamp(timestamp).strftime("%Y-%m-%d %H:%M:%S")

    def expect_callback(self, lines):
        self._expected_calls.append(mock.call(lines))

    def check_timetail(self,
                       log_file_data,
                       span,
                       now,
                       interval):
        with open(self._testFile, "w+") as testFile:
            testFile.write(log_file_data)

        timetail.timetail(self._callback,
                          self._testFile,
                          span,
                          now,
                          interval)

        assert self._callback.mock_calls == self._expected_calls
        self._expected_calls = []
        self._callback.reset_mock()

    # General note: timetail returns lines in the range [now - span, now)

    def test_nothing_called_if_current_time_is_not_divisible_by_interval1(self):
        self.expect_callback([])
        self.check_timetail("[%s] Line 1" % self.log_time(30240 - 1),
                            span=900,
                            now=30240,
                            interval=300)

    def test_nothing_called_if_current_time_is_not_divisible_by_interval2(self):
        self.expect_callback([])
        self.check_timetail("[%s] Line 1" % self.log_time(30360 - 1),
                            span=900,
                            now=30360,
                            interval=300)

    def test_return_empty_list_if_no_matching_logs(self):
        line1 = "[%s] Line 1" % self.log_time(1454328000)
        self.expect_callback([])
        self.check_timetail(line1 + "\n",
                            span=10,
                            now=1454328000,
                            interval=300)

    def test_default_interval_forces_log_processing(self):
        line1 = "[%s] Line 1" % self.log_time(1454328000)
        self.expect_callback([line1])
        self.check_timetail(line1 + "\n",
                            span=10000,
                            now=1454329000,
                            interval=None)

    def test_return_matching_log_lines1(self):
        line1 = "[%s] Line 1" % self.log_time(1454328000)
        line2 = "[%s] Line 2" % self.log_time(1454328000 + 1)
        line3 = "[%s] Line 3" % self.log_time(1454328000 + 2)
        self.expect_callback([line2])
        self.check_timetail(line1 + "\n" + line2 + "\n" + line3 + "\n",
                            span=1,
                            now=1454328000 + 2,
                            interval=300)

    def test_return_matching_log_lines2(self):
        line1 = "[%s] Line 1" % self.log_time(1454328000)
        line2 = "[%s] Line 2" % self.log_time(1454328000 + 1)
        line3 = "[%s] Line 3" % self.log_time(1454328000 + 2)
        self.expect_callback([line1, line2])
        self.check_timetail(line1 + "\n" + line2 + "\n" + line3 + "\n",
                            span=3,
                            now=1454328000 + 2,
                            interval=300)
