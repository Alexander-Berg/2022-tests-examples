# coding: utf-8

from __future__ import unicode_literals

import pytest

from datetime import date

from cab.sources import gap


GAP_DATA_FROM_EXPORT = {
    u'comment': u'Взял билеты',
    u'workflow': u'vacation',
    u'work_in_absence': False,
    u'date_from': u'2015-10-16T00:00:00',
    u'date_to': u'2015-10-20T00:00:00',
    u'full_day': True,
    u'id': 388105,
}


GAP_DATA_FROM_NEED_FOR_ATTENTION = {
    u'comment': u'Взял билеты',
    u'workflow': u'vacation',
    u'work_in_absence': False,
    u'date_from': u'2015-10-16T00:00:00',
    u'date_to': u'2015-10-20T00:00:00',
    # в этой ручке нет full_day
    u'id': 388105,
}


def test_gap_wrapper_first_day():
    gap_data = gap.Gap(GAP_DATA_FROM_EXPORT)
    assert gap_data.first_day == date(2015, 10, 16)


@pytest.mark.parametrize('gap_data', [
    GAP_DATA_FROM_EXPORT,
    GAP_DATA_FROM_NEED_FOR_ATTENTION,
])
def test_gap_wrapper_last_day(gap_data):
    gap_data = gap.Gap(gap_data)
    fail_msg = 'Gap stores last day as 00:00 of next day'
    assert gap_data.last_day == date(2015, 10, 19), fail_msg


def test_gap_wrapper_duration():
    gap_data = gap.Gap(GAP_DATA_FROM_EXPORT)
    assert gap_data.duration == 4


@pytest.mark.parametrize('start,end', [
    (date(2015, 10, 17), date(2015, 10, 18)),
    (date(2015, 10, 17), date(2015, 10, 19)),
    (date(2015, 10, 17), date(2016, 1, 1)),
    (date(2015, 10, 16), date(2015, 10, 17)),
    (date(2015, 10, 15), date(2015, 10, 17)),
    (date(2015, 1, 1), date(2015, 10, 17)),
])
def test_gap_wrapper_intersects_yes(start, end):
    gap_data = gap.Gap(GAP_DATA_FROM_EXPORT)
    assert gap_data.intersects_with(start, end)


@pytest.mark.parametrize('start,end', [
    (date(2015, 10, 10), date(2015, 10, 15)),
    (date(2015, 10, 20), date(2015, 10, 25)),
])
def test_gap_wrapper_intersects_no(start, end):
    gap_data = gap.Gap(GAP_DATA_FROM_EXPORT)
    assert not gap_data.intersects_with(start, end)


@pytest.mark.parametrize('start,end,length', [
    (date(2015, 10, 17), date(2015, 10, 18), 2),
    (date(2015, 10, 17), date(2015, 10, 19), 3),
    (date(2015, 10, 17), date(2016, 1, 1), 3),
    (date(2015, 10, 16), date(2015, 10, 17), 2),
    (date(2015, 10, 16), date(2015, 10, 16), 1),
    (date(2015, 10, 15), date(2015, 10, 17), 2),
    (date(2015, 1, 1), date(2015, 10, 17), 2),
    (date(2015, 10, 10), date(2015, 10, 15), 0),
    (date(2015, 10, 20), date(2015, 10, 25), 0),
])
def test_gap_wrapper_intersection_range_length(start, end, length):
    gap_data = gap.Gap(GAP_DATA_FROM_EXPORT)
    assert gap_data.intersection_with(start, end) == length


@pytest.mark.parametrize('start,end,length', [
    (date(2015, 10, 15), None, 4),
    (date(2015, 10, 16), None, 4),
    (date(2015, 10, 17), None, 3),
    (date(2015, 10, 18), None, 2),
    (date(2015, 10, 19), None, 1),
    (date(2015, 10, 20), None, 0),
    (date(2015, 10, 21), None, 0),
    (None, date(2015, 10, 15), 0),
    (None, date(2015, 10, 16), 1),
    (None, date(2015, 10, 17), 2),
    (None, date(2015, 10, 18), 3),
    (None, date(2015, 10, 19), 4),
    (None, date(2015, 10, 20), 4),
    (None, date(2015, 10, 21), 4),
])
def test_gap_wrapper_intersection_half_range_length(start, end, length):
    gap_data = gap.Gap(GAP_DATA_FROM_EXPORT)
    assert gap_data.intersection_with(start, end) == length
