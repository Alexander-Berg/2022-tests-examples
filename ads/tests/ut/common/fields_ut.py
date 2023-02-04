import enum
import pytest
import marshmallow

from ads.watchman.timeline.api.lib.common import fields as co_fields


class TestEnum(enum.Enum):
    ONE = 1
    TWO = 2


class TestFieldModel(object):
    def __init__(self, event_type):
        self.event_type = event_type


def test_EnumField_valid_object_serialization():
    field = co_fields.EnumField(enum_=TestEnum)
    assert field.serialize('event_type', TestFieldModel(TestEnum.ONE)) == "ONE"


def test_EnumField_none_serialization():
    field = co_fields.EnumField(enum_=TestEnum)
    assert field.serialize('event_type', TestFieldModel(None)) is None


def test_EnumField_invalid_object_serialization_raise_exception():
    field = co_fields.EnumField(enum_=TestEnum)
    fake_event_type_name = "_".join(t.name for t in TestEnum)  # for uniqueness
    with pytest.raises(marshmallow.exceptions.ValidationError):
        field.serialize('event_type', TestFieldModel(fake_event_type_name))


def test_EnumField_valid_object_deserialization():
    field = co_fields.EnumField(enum_=TestEnum)
    assert field.deserialize('ONE') == TestEnum.ONE


def test_EnumField_none_deserialization_if_allow_none():
    field = co_fields.EnumField(allow_none=True, enum_=TestEnum)
    assert field.deserialize(None) is None


def test_EnumField_none_deserialization_if_not_allow_none_raise_exception():
    field = co_fields.EnumField(enum_=TestEnum)
    with pytest.raises(marshmallow.exceptions.ValidationError):
        field.deserialize(None)


def test_EnumField_missing_deserialization_if_required_raise_exception():
    field = co_fields.EnumField(required=True, enum_=TestEnum)
    with pytest.raises(marshmallow.exceptions.ValidationError):
        field.deserialize(marshmallow.utils.missing)


def test_EnumField_invalid_object_deserialization_raise_exception():
    field = co_fields.EnumField(enum_=TestEnum)
    with pytest.raises(marshmallow.exceptions.ValidationError):
        field.deserialize('string')
