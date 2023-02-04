from datetime import datetime, timedelta

import pytz

from billing.apikeys.apikeys.cron_mask.periond_cron_mask import PeriodCronMask
from billing.apikeys.apikeys.tarifficator.base import PeriodMixin, TimezoneMixin
from billing.apikeys.apikeys import cron_mask


class AbstractUnit(PeriodMixin, TimezoneMixin):
    pass


def test_ambiguous_time():
    mask = PeriodCronMask('0 1 * * *', time_zone='Europe/Moscow')
    dt = datetime(2014, 10, 25, tzinfo=pytz.utc)
    assert mask.get_next_date(dt) == datetime(2014, 10, 25, 22, tzinfo=pytz.utc)


def test_period_delta_stronger_than_period_mask():
    o = AbstractUnit(period_mask="0 0 x x * *", period_delta={'months': 3, 'day': 'x', 'month': 'x'}, time_zone="Europe/Moscow")
    mp = o.make_period(datetime(2020, 11, 1, tzinfo=pytz.UTC))
    assert mp.get_next_date(datetime(2019, 11, 2, tzinfo=pytz.UTC)) != datetime(2019, 11, 30, 21, tzinfo=pytz.UTC)
    assert mp.get_next_date(datetime(2019, 11, 2, tzinfo=pytz.UTC)) == datetime(2020, 1, 31, 21, tzinfo=pytz.UTC)


def test_known_bug_of_cron_quarter_planning():
    o = AbstractUnit(period_mask="0 0 x x/3 * *", time_zone="Europe/Moscow")
    mp = o.make_period(datetime(2020, 11, 1, tzinfo=pytz.UTC))
    assert mp.get_next_date(datetime(2019, 11, 2, tzinfo=pytz.UTC)) != datetime(2020, 1, 31, 21, tzinfo=pytz.UTC)
    assert mp.get_next_date(datetime(2019, 11, 2, tzinfo=pytz.UTC)) == datetime(2020, 10, 31, 21, tzinfo=pytz.UTC)


def test_relativedelta_period_ideal_date():
    o = AbstractUnit(period_delta={'months': 3, 'day': 'x', 'month': 'x'}, time_zone='Europe/Moscow')
    point_date = datetime(2020, 10, 31, 21, tzinfo=pytz.UTC)
    mp = o.make_period(point_date)
    assert mp.get_next_date(point_date) == datetime(2021, 1, 31, 21, tzinfo=pytz.UTC)
    assert mp.get_next_date(point_date - timedelta(days=1)) == datetime(2020, 10, 31, 21, tzinfo=pytz.UTC)
    assert mp.get_previous_date(point_date) == datetime(2020, 7, 31, 21, tzinfo=pytz.UTC)
    assert mp.get_next_date(datetime(2019, 11, 2, tzinfo=pytz.UTC)) == datetime(2020, 1, 31, 21, tzinfo=pytz.UTC)
    assert mp.get_previous_date(datetime(2019, 11, 2, tzinfo=pytz.UTC)) == datetime(2019, 10, 31, 21, tzinfo=pytz.UTC)
    """
    https://a.yandex-team.ru/review/1294520/details
    дата ативации - 15.01.2020
    повторяемся каждые три месяца
    1.
    сегодня - 20.02.2020
    целевая дата - 15.04.2020
    2.
    сегодня - 10.06.2020
    целевая дата - 15.07.2020
    :return:
    """
    point_date = datetime(2020, 1, 14, 21, tzinfo=pytz.UTC)
    mp = o.make_period(point_date)
    assert mp.get_next_date(datetime(2020, 2, 20, tzinfo=pytz.UTC)) == datetime(2020, 4, 14, 21, tzinfo=pytz.UTC)
    assert mp.get_next_date(datetime(2020, 6, 10, tzinfo=pytz.UTC)) == datetime(2020, 7, 14, 21, tzinfo=pytz.UTC)
    assert mp.get_next_date(datetime(2020, 4, 14, 21, tzinfo=pytz.UTC)) == datetime(2020, 7, 14, 21, tzinfo=pytz.UTC)
    assert mp.get_previous_date(datetime(2020, 4, 14, 21, tzinfo=pytz.UTC)) == datetime(2020, 1, 14, 21, tzinfo=pytz.UTC)


def test_relativedelta_period_random_date():
    o = AbstractUnit(period_delta={'months': 3, 'hour': 0, 'minute': 0}, time_zone='Europe/Moscow')
    point_date = datetime(2020, 2, 26, 20, 1, 8, 388000, tzinfo=pytz.UTC)
    mp = o.make_period(point_date)
    assert mp.get_previous_date(point_date) == datetime(2020, 2, 25, 21, tzinfo=pytz.UTC)
    assert mp.get_next_date(point_date) == datetime(2020, 5, 25, 21, tzinfo=pytz.UTC)

    point_date = datetime(2020, 2, 26, 22, 1, 8, 388000, tzinfo=pytz.UTC)
    mp = o.make_period(point_date)
    assert mp.get_previous_date(point_date) == datetime(2020, 2, 26, 21, tzinfo=pytz.UTC)
    assert mp.get_next_date(point_date) == datetime(2020, 5, 26, 21, tzinfo=pytz.UTC)


def test_initial_timezone_of_point_date_for_relative_delta_should_be_replaced():
    p = cron_mask.PeriodRelativeDelta(
        {'months': 3},
        'Europe/Moscow'
    )
    d = p.get_next_date(datetime(2020, 6, 8, 15, 20, tzinfo=pytz.UTC))
    assert d == datetime(2020, 9, 7, 21, tzinfo=pytz.UTC)


def test_relativedelta_period_from_long_to_short_month():
    o = AbstractUnit(period_delta={'months': 1, 'day': 'x', 'month': 'x'}, time_zone='UTC')
    point_date = datetime(2020, 1, 31, tzinfo=pytz.UTC)
    mp = o.make_period(point_date)
    assert mp.get_next_date(point_date) == datetime(2020, 2, 29, tzinfo=pytz.UTC)
    assert mp.get_next_date(datetime(2020, 4, 30, tzinfo=pytz.UTC)) == datetime(2020, 5, 31, tzinfo=pytz.UTC)
