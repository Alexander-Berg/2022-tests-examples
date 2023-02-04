import unittest
from datetime import datetime, date, time

import json
from functools import partial
from .multic_json import CustomJSONEncoder

dumps = partial(json.dumps, cls=CustomJSONEncoder)


class TestsMulticJson(unittest.TestCase):

    def test_date(self):
        data = {'date': date(2012, 6, 10)}
        result = dumps(data)
        expected = '{"date": "2012-06-10"}'
        self.assertEqual(result, expected)

    def test_time_with_microseconds(self):
        data = {'time': time(12, 1, 36, 189952)}
        result = dumps(data)
        expected = '{"time": "12:01:36"}'
        self.assertEqual(result, expected)

    def test_time_wo_microseconds(self):
        data = {'time': time(12, 1, 36)}
        result = dumps(data)
        expected = '{"time": "12:01:36"}'
        self.assertEqual(result, expected)

    def test_datetime_with_microseconds(self):
        data = {'datetime': datetime(2012, 6, 10, 12, 1, 36, 189952)}
        result = dumps(data)
        expected = '{"datetime": "2012-06-10T12:01:36"}'
        self.assertEqual(result, expected)

    def test_datetime_wo_microseconds(self):
        data = {'datetime': datetime(2012, 6, 10, 12, 1, 36)}
        result = dumps(data)
        expected = '{"datetime": "2012-06-10T12:01:36"}'
        self.assertEqual(result, expected)


if __name__ == '__main__':
    unittest.main()
