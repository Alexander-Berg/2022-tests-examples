# coding=utf-8

import unittest
from mock import MagicMock

from infra.swatlib.util.retry import RetrySleeper


class TestRetrySleeper(unittest.TestCase):
    def test_max_delay(self):
        max_delay = 5
        max_jitter = 1
        sleep_func = MagicMock()
        sleeper = RetrySleeper(max_delay=max_delay, sleep_func=sleep_func, max_jitter=max_jitter)

        for i in range(10):
            sleeper.increment()

        for call in sleep_func.mock_calls:
            self.assertLessEqual(call[1][0], max_delay)

    def test_get_next_sleep_time(self):
        max_delay = 5
        max_jitter = 1
        sleep_func = MagicMock()
        sleeper = RetrySleeper(max_delay=max_delay, sleep_func=sleep_func, max_jitter=max_jitter)
        current_delay = sleeper.cur_delay

        for i in range(10):
            self.assertAlmostEqual(sleeper.get_next_time_to_sleep(), current_delay, delta=max_jitter)
            current_delay = min(sleeper.backoff * current_delay, max_delay)

        self.assertEqual(sleep_func.call_count, 0)
