from decimal import Decimal

import pytest
from marshmallow import ValidationError

from maps_adv.common.protomallow import PbFixedDecimalDictField
from maps_adv.common.protomallow.tests.proto import for_tests_pb2
from maps_adv.common.protomallow.tests.test_fields.helpers import serialize

test_data_serialize = [
    ({"places": 1, "field": "field"}, Decimal("123.0"), {"field": 1230}),
    ({"places": 3, "field": "field"}, Decimal("123.0"), {"field": 123000}),
    ({"places": 3, "field": "field"}, Decimal("-123.0"), {"field": -123000}),
    ({"places": 2, "field": "field"}, Decimal("1"), {"field": 100}),
    ({"places": 2, "field": "field"}, Decimal("2.0000000"), {"field": 200}),
    ({"places": 3, "field": "field"}, Decimal("0.123"), {"field": 123}),
    ({"places": 3, "field": "field"}, Decimal("321.654"), {"field": 321654}),
    ({"places": 3, "field": "smth"}, Decimal("321.654"), {"smth": 321654}),
]

test_data_deserialize = [
    (
        {"places": 1, "field": "field"},
        for_tests_pb2.DecimalMessage1(field=1230),
        Decimal("123.0"),
    ),
    (
        {"places": 3, "field": "field"},
        for_tests_pb2.DecimalMessage1(field=123000),
        Decimal("123.0"),
    ),
    (
        {"places": 3, "field": "field"},
        for_tests_pb2.DecimalMessage1(field=-123000),
        Decimal("-123.0"),
    ),
    (
        {"places": 2, "field": "field"},
        for_tests_pb2.DecimalMessage1(field=100),
        Decimal("1"),
    ),
    (
        {"places": 2, "field": "field"},
        for_tests_pb2.DecimalMessage1(field=200),
        Decimal("2.0000000"),
    ),
    (
        {"places": 3, "field": "field"},
        for_tests_pb2.DecimalMessage1(field=123),
        Decimal("0.123"),
    ),
    (
        {"places": 3, "field": "field"},
        for_tests_pb2.DecimalMessage1(field=321654),
        Decimal("321.654"),
    ),
    (
        {"places": 3, "field": "smth"},
        for_tests_pb2.DecimalMessage2(smth=321654),
        Decimal("321.654"),
    ),
]


@pytest.mark.parametrize(("field_params", "data", "expected"), test_data_deserialize)
def test_deserialize(field_params, data, expected):
    result = PbFixedDecimalDictField(**field_params).deserialize(data)
    assert result == expected


@pytest.mark.parametrize(("field_params", "data", "expected"), test_data_serialize)
def test_serialize(field_params, data, expected):
    result = serialize(PbFixedDecimalDictField(**field_params), data)

    assert result == expected


def test_serialize_none():
    assert serialize(PbFixedDecimalDictField(places=1, field="field"), None) is None


@pytest.mark.parametrize("param", ["places", "field"])
def test_field_params_required(param):
    all_valid_params = {"places": 4, "field": "smth"}
    del all_valid_params[param]

    with pytest.raises(TypeError):
        PbFixedDecimalDictField(**all_valid_params)


@pytest.mark.parametrize("places", [2.2, Decimal("4"), "8", "", None])
def test_field_raises_for_invalid_places_type(places):
    with pytest.raises(TypeError):
        PbFixedDecimalDictField(places=places, field="field")


@pytest.mark.parametrize("places", [0, -2])
def test_field_raises_for_invalid_places_value(places):
    with pytest.raises(ValueError):
        PbFixedDecimalDictField(places=places, field="field")


@pytest.mark.parametrize("field", [2.2, Decimal("4"), 8, None])
def test_field_raises_for_invalid_field_type(field):
    with pytest.raises(TypeError):
        PbFixedDecimalDictField(places=2, field=field)


@pytest.mark.parametrize("value", [2.2, Decimal("4"), "8", "", None, {"value": 123}])
def test_deserialize_raises_for_non_message(value):
    with pytest.raises(ValidationError):
        PbFixedDecimalDictField(places=3, field="field").deserialize(value)


@pytest.mark.parametrize("value", [2.2, Decimal("4"), "8", "", None])
def test_deserialize_raises_for_non_int_in_dict(value):
    with pytest.raises(ValidationError):
        PbFixedDecimalDictField(places=3, field="field").deserialize({"field": value})


@pytest.mark.parametrize(
    "value",
    [{"field": 4}, {"field": 2.2}, {"field": "8"}, {"field": ""}, {"field": None}],
)
def test_serialise_raises_for_non_decimal(value):
    with pytest.raises(ValidationError):
        serialize(PbFixedDecimalDictField(places=3, field="field"), value)


def test_serialize_raises_for_decimal_to_long_for_places():
    with pytest.raises(ValidationError):
        serialize(
            PbFixedDecimalDictField(places=2, field="field"),
            {"field": Decimal("1.123")},
        )


@pytest.mark.parametrize(
    ("places", "data", "expected"),
    [
        (1, Decimal("123.0"), 1230),
        (3, Decimal("123.0"), 123000),
        (3, Decimal("123.000000"), 123000),
        (3, Decimal("123.456789"), 123457),
        (4, Decimal("123.456789"), 1234568),
    ],
)
def test_serialize_quantize(places, data, expected):
    result = serialize(
        PbFixedDecimalDictField(places=places, field="field", quantize=True), data
    )

    assert result == {"field": expected}
