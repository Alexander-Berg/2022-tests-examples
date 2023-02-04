import pytest

from irt.utils import get_duration
from datetime import datetime, timedelta


def test_getting_durations():
    datetime_format = '%Y-%m-%d_%H:%M:%S'
    date1 = datetime.now()
    date2 = date1
    assert get_duration(date1.strftime(datetime_format), date2.strftime(datetime_format), datetime_format) == 0

    date3 = date2 + timedelta(seconds=27)
    assert get_duration(date1.strftime(datetime_format), date3.strftime(datetime_format), datetime_format) == 27

    other_datetime_format = '%Y-%m-%d %H:%M:%S'
    with pytest.raises(ValueError):
        get_duration(date1.strftime(datetime_format), date1.strftime(other_datetime_format), datetime_format)
