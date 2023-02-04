import pytest
from marshmallow import ValidationError

from smb.common.multiruntime.lib.basics import is_arcadia_python
from maps_adv.common.protomallow import PbStringEnumField
from maps_adv.common.protomallow.tests.test_fields.helpers import (
    reverse_parameters,
    serialize,
)

try:
    from maps_adv.common.protomallow.tests.proto import for_tests_pb2
except ImportError:
    if not is_arcadia_python:
        pytestmark = pytest.mark.skip(
            ".proto files in {} are not compiled. "
            "Compile them manually to run these tests.".format(
                "maps_adv.common.protomallow.tests.proto"
            )
        )
    else:
        raise

    for_tests_pb2 = None


test_data_serialize = [
    ("ELEVEN", 11),
    ("TWELVE", 12),
    ("THIRTEEN", 13),
]
test_data_deserialize = reverse_parameters(test_data_serialize)


@pytest.fixture
def field_instance():
    return PbStringEnumField(pb_enum=for_tests_pb2.Enum)


@pytest.mark.parametrize(("data", "expected"), test_data_deserialize)
def test_deserialize(field_instance, data, expected):
    result = field_instance.deserialize(data)

    assert result == expected


@pytest.mark.parametrize(("data", "expected"), test_data_serialize)
def test_serialize(field_instance, data, expected):
    result = serialize(field_instance, data)

    assert result == expected


def test_serialize_none(field_instance):
    assert serialize(field_instance, None) is None


def test_field_param_required():
    with pytest.raises(TypeError):
        PbStringEnumField()  # noqa


@pytest.mark.parametrize("value", [0, -1, (1, 2), "213", "", None])
def test_deserialize_raises_for_wrong_value(field_instance, value):
    with pytest.raises(ValidationError):
        field_instance.deserialize(value)


@pytest.mark.parametrize("value", [0, -1, (1, 2), "not_in_enum", ""])
def test_serialize_raises_for_wrong_value(field_instance, value):
    with pytest.raises(ValidationError):
        serialize(field_instance, value)
