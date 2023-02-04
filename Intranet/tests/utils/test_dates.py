# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import datetime

import pytest

from core.utils import dates


@pytest.fixture
def today():
    return datetime.datetime.today()


@pytest.fixture
def previous_year(today):
    if today.month == 1:
        return today.year - 1
    else:
        return today.year


@pytest.fixture
def previous_month(today):
    if today.month == 1:
        return 12
    else:
        return today.month - 1


def test_get_previous_month(previous_year, previous_month):
    month = dates.get_previous_month()
    assert month.year == previous_year
    assert month.month == previous_month
    assert month.day == 1
    assert month.hour == 0
    assert month.minute == 0
    assert month.second == 0
    assert month.microsecond == 0
    assert month.tzname()


@pytest.mark.parametrize('days', [0, 30])
def test_get_range_for_last(days):
    first, last = dates.get_range_for_last(days)
    assert (last - first) == datetime.timedelta(days=days)
