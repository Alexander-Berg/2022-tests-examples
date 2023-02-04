from datetime import date, datetime, timezone

import pytest
from google.protobuf import timestamp_pb2

from maps_adv.common.helpers import dt


@pytest.mark.parametrize(
    "value, expected",
    (
        [160, datetime(1970, 1, 1, 0, 2, 40, tzinfo=timezone.utc)],
        [100500, datetime(1970, 1, 2, 3, 55, tzinfo=timezone.utc)],
        ["1970-01-01 00:02:40", datetime(1970, 1, 1, 0, 2, 40, tzinfo=timezone.utc)],
        ["1970-01-02 03:55:00", datetime(1970, 1, 2, 3, 55, tzinfo=timezone.utc)],
    ),
)
def test_returns_datetime_fot_timestamp_or_datetime_like_input(value, expected):
    got = dt(value)

    assert got == expected


@pytest.mark.parametrize(
    "value, expected",
    (
        ["1970-01-01", date(1970, 1, 1)],
        ["2019-12-01", date(2019, 12, 1)],
        ["2020-07-14", date(2020, 7, 14)],
    ),
)
def test_returns_date_for_date_like_input(value, expected):
    got = dt(value)

    assert got == expected


@pytest.mark.parametrize(
    "value, expected",
    (
        [160, 160],
        [100500, 100500],
        ["1970-01-01 00:02:40", 160],
        ["1970-01-02 03:55:00", 100500],
        ["1970-01-01", 0],
        ["1970-01-02", 86400],
    ),
)
def test_returns_proto_message_if_requested(value, expected):
    got = dt(value, as_proto=True)

    assert got == timestamp_pb2.Timestamp(seconds=expected)
