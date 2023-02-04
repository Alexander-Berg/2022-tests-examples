from datetime import datetime, timedelta, timezone

import pytest

from pay.lib.utils.datetime import normalize_datetime

_DT = datetime(2022, 2, 22, 12, 59, 25, 900505, tzinfo=timezone.utc)


@pytest.mark.parametrize(
    'dt,expected',
    [
        (None, None),
        ('2022-02-22 12:59:25.900505+00:00', _DT),
        (
            '2022-02-22T12:59:25.900505+03:00',
            _DT.replace(tzinfo=timezone(timedelta(hours=3))),
        ),
        (
            _DT.replace(tzinfo=timezone(timedelta(hours=-3))),
            _DT.replace(tzinfo=timezone(timedelta(hours=-3))),
        ),
    ],
)
def test_normalize_datetime(dt, expected):
    assert normalize_datetime(dt) == expected


@pytest.mark.parametrize('dt', ['2022-02-22T12:59:25.900505', _DT.replace(tzinfo=None)])
def test_normalize_datetime__naive_datetime_not_allowed(dt):
    with pytest.raises(AssertionError, match='Naive timestamps are not acceptable'):
        normalize_datetime(dt)
