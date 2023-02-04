import datetime
import pytest

from staff.lib.utils.date import parse_datetime


@pytest.mark.parametrize(
    'value,result', [
        ('', None),
        (None, None),
        ('2019-01-10T19:00', datetime.datetime(2019, 1, 10, 19, 0)),
        ('2019-01-10 19:00', datetime.datetime(2019, 1, 10, 19, 0)),
        ('2019-01-10', datetime.datetime(2019, 1, 10, 0, 0)),
    ]
)
def test_parse_datetime(value, result):
    assert parse_datetime(value) == result


def test_parse_datetime_w_int():
    with pytest.raises(TypeError):
        parse_datetime(1)
