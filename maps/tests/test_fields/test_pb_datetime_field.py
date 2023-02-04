from datetime import datetime, timezone
from decimal import Decimal

import pytest
from google.protobuf.timestamp_pb2 import Timestamp
from marshmallow import ValidationError

from maps_adv.common.protomallow import PbDateTimeField
from maps_adv.common.protomallow.tests.test_fields.helpers import serialize

test_data_serialize = [
    (
        datetime(2009, 2, 13, 23, 31, 30, 0, tzinfo=timezone.utc),
        {"seconds": 1234567890, "nanos": 0},
    ),
    (
        datetime(2009, 2, 13, 23, 31, 30, 0, tzinfo=timezone.utc),
        {"seconds": 1234567890, "nanos": 0},
    ),
    (
        datetime(2009, 2, 13, 23, 31, 30, 321, tzinfo=timezone.utc),
        {"seconds": 1234567890, "nanos": 321000},
    ),
]
test_data_deserialize = [
    (
        Timestamp(**{"seconds": 1234567890, "nanos": 0}),
        datetime(2009, 2, 13, 23, 31, 30, 0, tzinfo=timezone.utc),
    ),
    (
        Timestamp(**{"seconds": 1234567890, "nanos": 0}),
        datetime(2009, 2, 13, 23, 31, 30, 0, tzinfo=timezone.utc),
    ),
    (
        Timestamp(**{"seconds": 1234567890, "nanos": 321000}),
        datetime(2009, 2, 13, 23, 31, 30, 321, tzinfo=timezone.utc),
    ),
]


@pytest.mark.parametrize(("data", "expected"), test_data_deserialize)
def test_deserialize(data, expected):
    result = PbDateTimeField().deserialize(data)
    assert result == expected


@pytest.mark.parametrize(("data", "expected"), test_data_serialize)
def test_serialize(data, expected):
    result = serialize(PbDateTimeField(), data)

    assert result == expected


def test_serialize_none():
    assert serialize(PbDateTimeField(), None) is None


def test_serialize_raises_for_dt_with_no_tzinfo():
    with pytest.raises(ValidationError):
        serialize(PbDateTimeField(), datetime(2000, 2, 2, 3, 4, 5))


@pytest.mark.parametrize(
    "value",
    [
        2.2,
        Decimal("4"),
        "8",
        "",
        None,
        {"value": 123},
        {"seconds": 123, "nanos": 123456789},
    ],
)
def test_deserialize_raises_for_non_message(value):
    with pytest.raises(ValidationError):
        PbDateTimeField().deserialize(value)
