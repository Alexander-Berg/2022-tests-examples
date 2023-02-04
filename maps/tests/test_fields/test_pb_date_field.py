from datetime import date, datetime, timezone
from decimal import Decimal

import pytest
from marshmallow import ValidationError

from maps_adv.common.protomallow import PbDateField
from maps_adv.common.protomallow.tests.proto import for_tests_pb2
from maps_adv.common.protomallow.tests.test_fields.helpers import serialize

test_data_serialize = [
    (date(2009, 2, 13), {"year": 2009, "month": 2, "day": 13}),
    (date(1900, 12, 6), {"year": 1900, "month": 12, "day": 6}),
    (None, None),
]
test_data_deserialize = [
    (for_tests_pb2.Date(year=2009, month=2, day=13), date(2009, 2, 13)),
    (for_tests_pb2.Date(year=1900, month=12, day=6), date(1900, 12, 6)),
]


@pytest.mark.parametrize(("data", "expected"), test_data_deserialize)
def test_deserialize(data, expected):
    result = PbDateField().deserialize(data)
    assert result == expected


@pytest.mark.parametrize(("data", "expected"), test_data_serialize)
def test_serialize(data, expected):
    result = serialize(PbDateField(), data)

    assert result == expected


@pytest.mark.parametrize(
    "value",
    [
        2.2,
        Decimal("4"),
        "8",
        "",
        {"value": 123},
        {"seconds": 123, "nanos": 123456789},
        datetime(2000, 3, 4),
        datetime(2000, 3, 4, 15, 0, 0),
        datetime(2000, 3, 4, 15, 0, 0, tzinfo=timezone.utc),
    ],
)
def test_serialize_raises_for_non_date(value):
    with pytest.raises(ValidationError):
        serialize(PbDateField(), value)


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
        PbDateField().deserialize(value)
