import datetime

import pytz

from billing.apikeys.apikeys.tarifficator.base import PeriodMixin, TimezoneMixin


def test_make_cron_mothly_at_last_day():
    class Unit(PeriodMixin, TimezoneMixin):
        pass

    unit = Unit(period_mask='0 0 x * * *', time_zone='Europe/Moscow')

    dt = datetime.datetime(2014, 1, 30, 20, 0, tzinfo=pytz.utc)  # 2014-01-31
    period = unit.make_period(point_date=dt)
    next_dt = period.get_next_date(dt)
    assert next_dt == datetime.datetime(2014, 2, 27, 20, 0, tzinfo=pytz.utc)  # 2014-02-28
    next_dt = period.get_next_date(next_dt)
    assert next_dt == datetime.datetime(2014, 3, 30, 20, 0, tzinfo=pytz.utc)  # 2014-03-31

    dt = datetime.datetime(2020, 1, 30, 21, 0, tzinfo=pytz.utc)  # 2020-03-31
    period = unit.make_period(point_date=dt)
    next_dt = period.get_next_date(dt)
    assert next_dt == datetime.datetime(2020, 2, 28, 21, 0, tzinfo=pytz.utc)  # 2020-02-29
