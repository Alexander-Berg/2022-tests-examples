# coding: utf-8

import datetime

from copy import deepcopy


def iso_to_datetime(isoformat_time):
    return datetime.datetime.strptime(isoformat_time, u"%Y-%m-%dT%H:%M:%S")


def dict_to_str(d):
    """
    Because log with spaces is not one-click accept
    """
    return ",".join("{}={}".format(key, value) for key, value in d.iteritems())


def update_attr(d, attr, value):
    new_d = deepcopy(d)
    new_d[attr] = value
    return new_d


def del_attr(d, attr):
    new_d = deepcopy(d)
    del new_d[attr]
    return new_d


class MockDatetime(object):
    """
    Monkey-patch datetime for predictable results
    https://github.com/dbader/schedule/blob/master/test_schedule.py
    """
    def __init__(self, year, month, day, hour, minute):
        self.year = year
        self.month = month
        self.day = day
        self.hour = hour
        self.minute = minute

    @classmethod
    def from_datetime(cls, dt):
        return cls(dt.year, dt.month, dt.day, dt.hour, dt.minute)

    def __enter__(self):
        class MockDate(datetime.datetime):
            @classmethod
            def today(cls, *args, **kwargs):
                return cls(self.year, self.month, self.day)

            @classmethod
            def now(cls, *args, **kwargs):
                return cls(self.year, self.month, self.day,
                           self.hour, self.minute)
        self.original_datetime = datetime.datetime
        datetime.datetime = MockDate

    def __exit__(self, *args, **kwargs):
        datetime.datetime = self.original_datetime
