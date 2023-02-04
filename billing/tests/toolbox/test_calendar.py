from datetime import date

import pytest

from bcl.toolbox import calendars


@pytest.mark.xfail(reason='Вероятно Calendar временно недоступен')
def test_check_date_is_holiday():

    assert not calendars.Calendar.check_date_is_holiday('2017-03-29')
    assert not calendars.Calendar.check_date_is_holiday('2018-12-25')  # выходной в Беларуси
    assert not calendars.Calendar.check_date_is_holiday('2018-12-29')  # рабочая суббота в России
    assert calendars.Calendar.check_date_is_holiday('2017-01-03')


@pytest.mark.xfail(reason='Вероятно Calendar временно недоступен')
def test_get_holidays():

    result = calendars.Calendar.get_holidays('2018-11-25', date_till='2018-12-02')
    assert result == [date(2018, 11, 25), date(2018, 12, 1), date(2018, 12, 2)]


@pytest.mark.xfail(reason='Вероятно Calendar временно недоступен')
def test_get_non_holiday():

    result = calendars.Calendar.get_non_holiday(target_date='2018-11-05')
    assert result == (date(2018, 11, 2), True)

    result = calendars.Calendar.get_non_holiday(target_date='2018-11-06')
    assert result == (date(2018, 11, 2), False)

    result = calendars.Calendar.get_non_holiday(target_date='2018-11-07')
    assert result == (date(2018, 11, 6), False)
