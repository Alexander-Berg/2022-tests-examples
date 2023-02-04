from unittest.mock import Mock

import pytest

from walle.util.jsonschema import (
    _Base,
    AnyOf,
    Enum,
    Integer,
    Number,
    OneOf,
    OneOfPartial,
    Null,
    Boolean,
    String,
    Object,
    Array,
    WithDefault,
    WithDescription,
)
from walle.util.misc import drop_none


def test_null():
    obj = Null()

    assert obj.get_schema() == {"type": "null"}


def test_boolean():
    obj = Boolean()

    assert obj.get_schema() == {"type": "boolean"}


@pytest.mark.parametrize("elements", (["a", "b"], [0, 1], []))
def test_enum(elements):
    obj = Enum(elements=elements)

    assert obj.get_schema() == {"enum": elements}


@pytest.mark.parametrize("minimum", (1.0, 100, None))
@pytest.mark.parametrize("maximum", (1.0, 100, None))
def test_number(minimum, maximum):
    obj = Number(**drop_none({"minimum": minimum, "maximum": maximum}))

    assert obj.get_schema() == drop_none({"type": "number", "minimum": minimum, "maximum": maximum})


@pytest.mark.parametrize("minimum", (100, None))
@pytest.mark.parametrize("maximum", (100, None))
def test_integer(minimum, maximum):
    obj = Integer(**drop_none({"minimum": minimum, "maximum": maximum}))

    assert obj.get_schema() == drop_none({"type": "integer", "minimum": minimum, "maximum": maximum})


@pytest.mark.parametrize("min_length", (10, None))
@pytest.mark.parametrize("max_length", (10, None))
@pytest.mark.parametrize("pattern", ("mock-pattern", None))
def test_string(min_length, max_length, pattern):
    obj = String(**drop_none({"min_length": min_length, "max_length": max_length, "pattern": pattern}))

    assert obj.get_schema() == drop_none(
        {"type": "string", "minLength": min_length, "maxLength": max_length, "pattern": pattern}
    )


@pytest.mark.parametrize("with_item", (True, False))
@pytest.mark.parametrize("min_items", (0, None))
@pytest.mark.parametrize("max_items", (10, None))
def test_array(with_item, min_items, max_items):
    contains_obj = _Base()
    contains_obj.get_schema = Mock(return_value=dict())

    obj = Array(
        items=contains_obj if with_item else None, **drop_none({"min_items": min_items, "max_items": max_items})
    )
    schema = obj.get_schema()

    if with_item:
        contains_obj.get_schema.assert_called_once()

    assert schema == drop_none(
        {
            "type": "array",
            "minItems": min_items,
            "maxItems": max_items,
            "items": dict() if with_item else None,
        }
    )


@pytest.mark.parametrize("required", (None, [], ["mock-1", "mock-2"]))
@pytest.mark.parametrize("additional_properties", (False, True))
def test_object(required, additional_properties):
    properties = {"mock-1": _Base(), "mock-2": _Base()}
    for prop in properties.values():
        prop.get_schema = Mock(return_value=dict())

    obj = Object(properties=properties.copy(), required=required, additional_properties=additional_properties)
    schema = obj.get_schema()

    for prop in properties.values():
        prop.get_schema.assert_called_once()

    assert schema == {
        "type": "object",
        "properties": {key: dict() for key in properties},
        "required": required or [],
        "additionalProperties": additional_properties,
    }


def test_any_of():
    schemes = {"mock-1": _Base(), "mock-2": _Base()}
    for key, schema in schemes.items():
        schema.get_schema = Mock(return_value=dict(type=key))

    obj = AnyOf(schemes=list(schemes.values()))
    schema = obj.get_schema()

    for value in schemes.values():
        value.get_schema.assert_called_once()

    assert schema == {"anyOf": [{"type": key} for key in schemes]}


def test_one_of():
    schemes = {"mock-1": _Base(), "mock-2": _Base()}
    for key, schema in schemes.items():
        schema.get_schema = Mock(return_value=dict(type=key))

    obj = OneOf(schemes=list(schemes.values()))
    schema = obj.get_schema()

    for value in schemes.values():
        value.get_schema.assert_called_once()

    assert schema == {"oneOf": [{"type": key} for key in schemes]}


@pytest.mark.parametrize(
    ("allowed_params", "request_params", "is_error"),
    (
        ([], [], False),
        ([], [{"param3": "data3-1"}, {"param3": "data3-2"}], True),
        (["param1", "param2", "param3"], [{"param3": "data3-1"}, {"param3": "data3-2"}], False),
        (["param1", "param2", "param3"], [{"param2": "data2-1"}, {"param2": "data2-2"}], True),
    ),
)
def test_one_of_patrial(allowed_params, request_params, is_error):
    parent_schema = {"type": "test", "param1": "data1", "param2": "data2"}
    parent_obj = _Base()
    parent_obj.get_schema = Mock(return_value=parent_schema)
    parent_obj.get_allowed_params = Mock(return_value=frozenset(allowed_params))

    obj = OneOfPartial(parent=parent_obj, params=request_params)

    if is_error:
        with pytest.raises(ValueError):
            obj.get_schema()
    else:
        schema = obj.get_schema()
        assert schema == {**parent_schema, "oneOf": request_params}

    parent_obj.get_schema.assert_called_once()
    parent_obj.get_allowed_params.assert_called_once()


@pytest.mark.parametrize("description", ("some description", None))
def test_description(description):
    parent_schema = drop_none({"type": "test", "param1": "data1", "param2": "data2", "description": description})
    parent_obj = _Base()
    parent_obj.get_schema = Mock(return_value=parent_schema)

    obj = WithDescription(parent=parent_obj, description="mock description")

    if description is not None:
        with pytest.raises(ValueError):
            obj.get_schema()
    else:
        schema = obj.get_schema()
        assert schema == {**parent_schema, "description": "mock description"}

    parent_obj.get_schema.assert_called_once()


@pytest.mark.parametrize("value", ("some value", None))
def test_default(value):
    parent_schema = drop_none({"type": "test", "param1": "data1", "param2": "data2", "default": value})
    parent_obj = _Base()
    parent_obj.get_schema = Mock(return_value=parent_schema)

    obj = WithDefault(parent=parent_obj, value="mock value")

    if value is not None:
        with pytest.raises(ValueError):
            obj.get_schema()
    else:
        schema = obj.get_schema()
        assert schema == {**parent_schema, "default": "mock value"}

    parent_obj.get_schema.assert_called_once()
