from decimal import Decimal

import pytest
from marshmallow import ValidationError

from maps_adv.common.protomallow.fields import PbTruncatingDecimalField
from maps_adv.common.protomallow.tests.test_fields.helpers import serialize


@pytest.mark.parametrize(
    "places, expected_error, expected_err_message",
    [
        (None, TypeError, "\"places\" must be int, not <class 'NoneType'>"),
        ("123", TypeError, "\"places\" must be int, not <class 'str'>"),
        (1.2, TypeError, "\"places\" must be int, not <class 'float'>"),
        (
            Decimal(1),
            TypeError,
            "\"places\" must be int, not <class 'decimal.Decimal'>",
        ),
        (0, ValueError, '"places" must be positive'),
        (-1, ValueError, '"places" must be positive'),
    ],
)
def test_raises_for_wrong_init_value(places, expected_error, expected_err_message):
    with pytest.raises(expected_error) as exc_info:
        PbTruncatingDecimalField(places=places)

    assert exc_info.value.args == (expected_err_message,)


@pytest.mark.parametrize(
    "value, expected_err_message",
    [
        ("123", "Decimal type is required (got <class 'str'>)"),
        (123, "Decimal type is required (got <class 'int'>)"),
        (1.23, "Decimal type is required (got <class 'float'>)"),
    ],
)
def test_raises_for_wrong_serialization_value(value, expected_err_message):
    with pytest.raises(ValidationError) as exc_info:
        serialize(PbTruncatingDecimalField(places=2), value)

    assert exc_info.value.args == (expected_err_message,)


@pytest.mark.parametrize(
    "value, expected_err_message",
    [
        (None, "Field may not be null."),
        (123, "str type is required (got <class 'int'>)"),
        (1.23, "str type is required (got <class 'float'>)"),
        (Decimal("1.23"), "str type is required (got <class 'decimal.Decimal'>)"),
        ("abc", "'abc' value can't be converted to Decimal"),
    ],
)
def test_raises_for_wrong_deserialization_value(value, expected_err_message):
    with pytest.raises(ValidationError) as exc_info:
        PbTruncatingDecimalField(places=2).deserialize(value)

    assert exc_info.value.args == (expected_err_message,)


@pytest.mark.parametrize(
    "value, expected",
    [
        (None, None),
        (Decimal("1.23"), "1.23"),
        (Decimal("0.0"), "0"),
        (Decimal("1.00000"), "1"),
        (Decimal("-1.23"), "-1.23"),
        (Decimal("1234"), "1234"),
        (Decimal("1234.1"), "1234.1"),
        (Decimal("1234.12"), "1234.12"),
        (Decimal("1234.123456"), "1234.12"),
        (Decimal("1234.120"), "1234.12"),
        (Decimal("1234.129"), "1234.12"),
        (Decimal("1234.100000"), "1234.1"),
        (Decimal("1234.1000001"), "1234.1"),
    ],
)
def test_is_serialized_as_expected(value, expected):
    got = serialize(PbTruncatingDecimalField(places=2), value)

    assert got == expected


@pytest.mark.parametrize(
    "value, expected",
    [
        ("1.23", Decimal("1.23")),
        ("0.0", Decimal("0.0")),
        ("1.00000", Decimal("1.00000")),  # don't touch trailing zeroes after fraction
        ("-1.23", Decimal("-1.23")),
        ("1234", Decimal("1234")),
    ],
)
def test_is_deserialized_as_expected(value, expected):
    got = PbTruncatingDecimalField(places=2).deserialize(value)

    assert got == expected
