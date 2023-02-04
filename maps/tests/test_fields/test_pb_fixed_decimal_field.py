from decimal import Decimal

import pytest
from marshmallow import ValidationError

from maps_adv.common.protomallow import PbFixedDecimalField
from maps_adv.common.protomallow.tests.test_fields.helpers import (
    reverse_parameters,
    serialize,
)

test_data_serialize = [
    ({"places": 1}, Decimal("123.0"), 1230),
    ({"places": 3}, Decimal("123.0"), 123000),
    ({"places": 3}, Decimal("-123.0"), -123000),
    ({"places": 2}, Decimal("1"), 100),
    ({"places": 2}, Decimal("2.0000000"), 200),
    ({"places": 3}, Decimal("0.123"), 123),
    ({"places": 3}, Decimal("321.654"), 321654),
]
test_data_deserialize = reverse_parameters(test_data_serialize)


@pytest.mark.parametrize(("field_params", "data", "expected"), test_data_deserialize)
def test_deserialize(field_params, data, expected):
    result = PbFixedDecimalField(**field_params).deserialize(data)

    assert result == expected


@pytest.mark.parametrize(("field_params", "data", "expected"), test_data_serialize)
def test_serialize(field_params, data, expected):
    result = serialize(PbFixedDecimalField(**field_params), data)

    assert result == expected


def test_serialize_none():
    assert serialize(PbFixedDecimalField(places=1), None) is None


@pytest.mark.parametrize("param", ["places"])
def test_field_params_required(param):
    all_valid_params = {"places": 4, "field": "smth"}
    del all_valid_params[param]

    with pytest.raises(TypeError):
        PbFixedDecimalField(**all_valid_params)


@pytest.mark.parametrize("places", [2.2, Decimal("4"), "8", "", None])
def test_field_raises_for_invalid_places_type(places):
    with pytest.raises(TypeError):
        PbFixedDecimalField(places=places)


@pytest.mark.parametrize("places", [0, -2])
def test_field_raises_for_invalid_places_value(places):
    with pytest.raises(ValueError):
        PbFixedDecimalField(places=places)


@pytest.mark.parametrize("value", [2.2, Decimal("4"), "8", "", None])
def test_deserialize_raises_for_non_int_in_dict(value):
    with pytest.raises(ValidationError):
        PbFixedDecimalField(places=3).deserialize(value)


@pytest.mark.parametrize("value", [4, 2.2, "8", ""])
def test_serialise_raises_for_non_decimal(value):
    with pytest.raises(ValidationError):
        serialize(PbFixedDecimalField(places=3), value)


def test_serialize_raises_for_decimal_to_long_for_places():
    with pytest.raises(ValidationError):
        serialize(PbFixedDecimalField(places=2), Decimal("1.123"))


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
    result = serialize(PbFixedDecimalField(places=places, quantize=True), data)

    assert result == expected
