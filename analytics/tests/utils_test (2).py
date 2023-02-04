import datetime
from analytics.plotter_lib.utils import get_dts_delta, DATE_FORMAT, date_range


class Test_get_dts_delta():
    def test_base(self):
        assert 4 == get_dts_delta(
            datetime.date(2019, 10, 5).strftime(DATE_FORMAT),
            datetime.date(2019, 10, 1).strftime(DATE_FORMAT),
            DATE_FORMAT
        ).days


class Test_date_range():
    def test_base(self):
        assert 5 == len(list(date_range(
            datetime.date(2019, 10, 1),
            datetime.date(2019, 10, 5)
        )))
        assert 2 == len(list(date_range(
            datetime.date(2019, 10, 1),
            datetime.date(2019, 10, 2),
        )))
        assert 1 == len(list(date_range(
            datetime.date(2019, 10, 1),
            datetime.date(2019, 10, 1),
        )))
        assert 1 == len(list(date_range(
            datetime.date(2019, 10, 5),
        )))
        assert 1 == len(list(date_range(
            datetime.date(2019, 10, 1),
            datetime.date(2019, 10, 2),
            delta=1
        )))
        assert 1 == len(list(date_range(
            datetime.date(2019, 10, 1),
            datetime.date(2019, 10, 2),
            delta=3
        )))
        assert 1 == len(list(date_range(
            datetime.date(2019, 10, 1),
            datetime.date(2019, 10, 2),
            delta=5
        )))
        assert 2 == len(list(date_range(
            datetime.date(2019, 10, 1),
            datetime.date(2019, 10, 5),
            delta=3
        )))
