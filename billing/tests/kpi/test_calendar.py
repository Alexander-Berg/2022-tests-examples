# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import datetime as dt

import pytest


@pytest.mark.parametrize('dt_, result', [
    (dt.datetime(2017, 1, 1), 0),
    (dt.datetime(2017, 1, 10), 1),
    (dt.datetime(2017, 1, 14), 0),
], ids=lambda p: p.strftime('%Y-%m-%d') if isinstance(p, dt.datetime) else str(bool(p)))
def test_is_workday(calendar, dt_, result):
    assert calendar.is_workday(dt_) == result


@pytest.mark.parametrize('interval, result', [
    ((dt.datetime(2017, 1, 1, 10), dt.datetime(2017, 1, 1, 11)), dt.timedelta(0)),
    ((dt.datetime(2017, 1, 10, 11), dt.datetime(2017, 1, 10, 12)), dt.timedelta(hours=1)),
    ((dt.datetime(2017, 1, 10, 9), dt.datetime(2017, 1, 10, 14)), dt.timedelta(hours=4)),
    ((dt.datetime(2017, 1, 10, 17), dt.datetime(2017, 1, 10, 23)), dt.timedelta(hours=2)),
    ((dt.datetime(2017, 1, 10, 9), dt.datetime(2017, 1, 10, 23)), dt.timedelta(hours=9)),
    ((dt.datetime(2017, 1, 10, 20), dt.datetime(2017, 1, 11, 5)), dt.timedelta(hours=0)),
    ((dt.datetime(2017, 1, 10, 11), dt.datetime(2017, 1, 13, 17)), dt.timedelta(hours=33)),
    ((dt.datetime(2017, 1, 13, 12), dt.datetime(2017, 1, 14, 12)), dt.timedelta(hours=7)),
    ((dt.datetime(2017, 1, 15, 9), dt.datetime(2017, 1, 16, 12, 13)), dt.timedelta(hours=2, minutes=13)),
    ((dt.datetime(2017, 1, 13, 12), dt.datetime(2017, 1, 16, 16)), dt.timedelta(hours=13)),
    ((dt.datetime(2017, 1, 12, 11), dt.datetime(2017, 1, 25, 13)), dt.timedelta(hours=83)),
    ((dt.datetime(2017, 1, 14, 12), dt.datetime(2017, 1, 22, 18)), dt.timedelta(hours=45)),
    ((dt.datetime(2016, 12, 20, 11), dt.datetime(2017, 1, 10, 13)), dt.timedelta(hours=92)),
    ((dt.datetime(2017, 1, 12), dt.datetime(2017, 1, 11, 13)), None),
    ((dt.datetime(2017, 1, 12, 14, 15, 16), dt.datetime(2017, 1, 12, 14, 15, 16)), dt.timedelta()),
], ids=[
    'weekend',
    'same_day_worktime',
    'same_day_early',
    'same_day_late',
    'same_day_all_day',
    'night',
    'multiple_days',
    'weekend_end',
    'weekend_start',
    'over_weekend',
    'multiple_weekends',
    'weekend2weekend',
    'over_newyear',
    'end_before_start',
    'zero_length'
])
def test_get_worktime(calendar, interval, result):
    assert calendar.get_worktime(*interval) == result
