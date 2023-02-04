"""Tests for custom validation objects."""
import itertools
import sys

import pytest

from object_validator import validate_object, DictScheme, String, ValidationError
from walle.util.validation import FlatableList, NumberToInteger, Number, PossiblyEmptyList


def assert_ok(obj, scheme):
    assert validate_object(obj, scheme)


def assert_error(obj, scheme):
    with pytest.raises(ValidationError):
        return validate_object(obj, scheme)


def test_number():
    scheme = Number()

    assert 0 == validate_object(0, scheme)
    assert 0.0 == validate_object(0.0, scheme)
    assert 0.9 == validate_object(0.9, scheme)
    assert -0.0 == validate_object(-0.0, scheme)
    assert -0.9 == validate_object(-0.9, scheme)

    assert 0 == validate_object(int(), scheme)
    assert 0.0 == validate_object(float(), scheme)
    assert float("inf") == validate_object(float("+INF"), scheme)
    assert float("-inf") == validate_object(float("-inf"), scheme)
    assert isinstance(validate_object(float("-NaN"), scheme), float)

    assert validate_object(sys.maxsize, scheme)

    assert_error("0", scheme)
    assert_error(object(), scheme)
    assert_error(int, scheme)


def test_number_to_integer():
    scheme = NumberToInteger()

    assert 0 == validate_object(0, scheme)
    assert 0 == validate_object(0.0, scheme)
    assert 0 == validate_object(0.9, scheme)
    assert 0 == validate_object(-0.0, scheme)
    assert 0 == validate_object(-0.9, scheme)

    assert 1 == validate_object(1, scheme)
    assert 1 == validate_object(1.0, scheme)
    assert 1 == validate_object(1.9, scheme)
    assert -1 == validate_object(-1, scheme)
    assert -1 == validate_object(-1.0, scheme)
    assert -1 == validate_object(-1.9, scheme)

    assert 0 == validate_object(int(), scheme)
    assert 0 == validate_object(float(), scheme)

    assert validate_object(sys.maxsize, scheme)

    assert_error(float("inf"), scheme)

    assert_error("0", scheme)
    assert_error(int, scheme)
    assert_error(object(), scheme)


def test_number_limits():
    keys = ("min_value", "max_value")
    values = (0, 0.0)

    def produce_kwargs():
        for keyword in itertools.product(keys, values):
            yield dict([keyword])

        for keywords in itertools.product(*[itertools.product([key], values) for key in keys]):
            yield dict(keywords)

    for kwargs in produce_kwargs():
        schema = Number(**kwargs)

        assert 0 == validate_object(0, schema), kwargs
        assert 0.0 == validate_object(0.0, schema), kwargs
        assert 0.0 == validate_object(-0.0, schema), kwargs

    for kwargs in produce_kwargs():
        schema = NumberToInteger(**kwargs)

        assert 0 == validate_object(0, schema), kwargs
        assert 0 == validate_object(0.0, schema), kwargs

    for kwargs in produce_kwargs():
        for validator in (Number, NumberToInteger):
            schema = validator(**kwargs)

            if "min_value" in kwargs:
                assert_error(-1, schema)
                assert_error(-1.0, schema)
                assert_error(-0.000000000000000000000000000001, schema)

            if "max_value" in kwargs:
                assert_error(1, schema)
                assert_error(1.0, schema)
                assert_error(0.000000000000000000000000000001, schema)


def test_flatable_list():
    scheme = DictScheme({"list": (FlatableList(String(min_length=3)))})
    assert_ok({"list": "string"}, scheme)
    assert_ok({"list": ["string"]}, scheme)
    assert_ok({"list": ["string", "other string"]}, scheme)
    assert_ok({"list": ["string", "other string"]}, scheme)

    assert {"list": ["string"]} == validate_object({"list": "string"}, scheme)
    assert {"list": ["string"]} == validate_object({"list": ["string"]}, scheme)
    assert {"list": ["string", "other string"]} == validate_object({"list": ["string", "other string"]}, scheme)

    assert_error({}, scheme)
    assert_error({"list": {"string"}}, scheme)
    assert_error({"list": [{"string"}]}, scheme)
    assert_error({"list": [{"string"}]}, scheme)

    assert_error({"list": "st"}, scheme)
    assert_error({"list": ["st"]}, scheme)
    assert_error({"list": ["st", "string"]}, scheme)


def test_possibly_empty_list():
    list_validator = PossiblyEmptyList(Number(), min_length=4, max_length=4)

    assert [1, 2, 3, 4] == validate_object([1, 2, 3, 4], list_validator)
    assert [] == validate_object([], list_validator)

    assert_error([1], list_validator)
    assert_error([1, 2, 3, 4, 5], list_validator)
    assert_error(["1", "2", "3", "4"], list_validator)
    assert_error(None, list_validator)
