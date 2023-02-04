from datetime import date, datetime, time
from lib_monitoring import parse_date, validate_date, get_date


class TestParseDate:
    def test_yyyy_mm_dd(self):
        date_str = '2017-09-27'
        assert parse_date(date_str) == datetime.combine(date(2017, 9, 27), time(0, 0))


class TestValidateDate:
    def test_correct_date(self):
        date = '2017-09-27'
        assert validate_date(date)

    def test_incorrect_date(self):
        date = '2017-09-27T00:00:00'
        assert not validate_date(date)


class TestGetDate:
    def test_always_valid(self):
        for days_back in (0, 1, 10):
            assert validate_date(get_date(days_back=days_back))
        assert validate_date(get_date())
